package Akari.Twitter.share.hook

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.File

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        const val MODULE_PKG = "Akari.Twitter.share"
        const val TWITTER_PKG = "com.twitter.android"
        const val TWITTER_LITE_PKG = "com.twitter.android.lite"
        const val PREFS_NAME = "settings"
        const val TAG = "TwiFrom67"

        @Volatile
        private var xSharedPreferences: XSharedPreferences? = null

        @Volatile
        private var twitterAppContext: Context? = null

        fun getXPreferences(): XSharedPreferences {
            val pref = xSharedPreferences ?: XSharedPreferences(MODULE_PKG, PREFS_NAME)
            xSharedPreferences = pref
            return pref
        }

        fun getEffectiveContext(providedContext: Context?): Context? {
            if (providedContext != null) return providedContext
            if (twitterAppContext != null) return twitterAppContext
            return try {
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val currentApplication = activityThreadClass.getDeclaredMethod("currentApplication")
                currentApplication.isAccessible = true
                currentApplication.invoke(null) as? Context
            } catch (_: Throwable) {
                null
            }
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        try {
            xSharedPreferences = XSharedPreferences(MODULE_PKG, PREFS_NAME)
        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] Zygote init prefs exception: ${t.message}")
        }
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // 1. Self activation verification
        if (lpparam.packageName == MODULE_PKG) {
            try {
                XposedHelpers.findAndHookMethod(
                    "Akari.Twitter.share.util.ModuleStatus",
                    lpparam.classLoader,
                    "isModuleActive",
                    XC_MethodReplacement.returnConstant(true)
                )
            } catch (t: Throwable) {
                XposedBridge.log("[$TAG] Failed to hook ModuleStatus: ${t.message}")
            }
            return
        }

        // 2. Hook Twitter / X
        if (lpparam.packageName != TWITTER_PKG && lpparam.packageName != TWITTER_LITE_PKG) {
            return
        }

        XposedBridge.log("[$TAG] Initializing hooks for Twitter package: ${lpparam.packageName}")
        hookApplicationLifecycle(lpparam)
        hookTwitter(lpparam)
    }

    private fun hookApplicationLifecycle(lpparam: LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "attach",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as? Context
                        if (app != null) {
                            twitterAppContext = app
                            XposedBridge.log("[$TAG] Captured Twitter ApplicationContext via Application.attach")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] Error hooking Application.attach: ${t.message}")
        }
    }

    private fun hookTwitter(lpparam: LoadPackageParam) {
        // Hook 1: ClipboardManager.setPrimaryClip (Captures "Copy link" action)
        try {
            XposedHelpers.findAndHookMethod(
                ClipboardManager::class.java,
                "setPrimaryClip",
                ClipData::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val clip = param.args[0] as? ClipData ?: return
                        val ctx = try {
                            XposedHelpers.getObjectField(param.thisObject, "mContext") as? Context
                        } catch (_: Throwable) {
                            null
                        }
                        val config = loadConfig(ctx)
                        if (!config.isEnabled) return

                        var modified = false
                        val newItems = mutableListOf<ClipData.Item>()

                        for (i in 0 until clip.itemCount) {
                            val item = clip.getItemAt(i)
                            val text = item.text?.toString()
                            if (text != null && TweetUrlModifier.containsTweetUrl(text)) {
                                val rewritten = TweetUrlModifier.modify(
                                    text,
                                    config.customS,
                                    config.cleanTracking
                                )
                                if (rewritten != text) {
                                    newItems.add(ClipData.Item(rewritten))
                                    modified = true
                                    XposedBridge.log("[$TAG] Modified clipboard URL: $text -> $rewritten")
                                } else {
                                    newItems.add(item)
                                }
                            } else {
                                newItems.add(item)
                            }
                        }

                        if (modified && newItems.isNotEmpty()) {
                            val rebuiltClip = ClipData(clip.description, newItems[0])
                            for (i in 1 until newItems.size) {
                                rebuiltClip.addItem(newItems[i])
                            }
                            param.args[0] = rebuiltClip
                        }
                    }
                }
            )
            XposedBridge.log("[$TAG] Hooked ClipboardManager.setPrimaryClip successfully")
        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] Error hooking ClipboardManager: ${t.message}")
        }

        // Hook 2: Activity.startActivity (Captures "Share via..." chooser and system sheet)
        try {
            XposedHelpers.findAndHookMethod(
                Activity::class.java,
                "startActivity",
                Intent::class.java,
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity
                        val intent = param.args[0] as? Intent ?: return
                        interceptShareIntent(intent, activity)
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                Activity::class.java,
                "startActivityForResult",
                Intent::class.java,
                Int::class.javaPrimitiveType,
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity
                        val intent = param.args[0] as? Intent ?: return
                        interceptShareIntent(intent, activity)
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] Error hooking Activity.startActivity: ${t.message}")
        }

        // Hook 3: ContextWrapper.startActivity
        try {
            XposedHelpers.findAndHookMethod(
                ContextWrapper::class.java,
                "startActivity",
                Intent::class.java,
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val ctx = param.thisObject as? Context
                        val intent = param.args[0] as? Intent ?: return
                        interceptShareIntent(intent, ctx)
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] Error hooking ContextWrapper.startActivity: ${t.message}")
        }

        // Hook 4: Intent.putExtra(String, String)
        try {
            XposedHelpers.findAndHookMethod(
                Intent::class.java,
                "putExtra",
                String::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val name = param.args[0] as? String ?: return
                        if (name == Intent.EXTRA_TEXT || name == Intent.EXTRA_SUBJECT) {
                            val value = param.args[1] as? String ?: return
                            if (TweetUrlModifier.containsTweetUrl(value)) {
                                val config = loadConfig(null)
                                if (config.isEnabled) {
                                    val modified = TweetUrlModifier.modify(
                                        value,
                                        config.customS,
                                        config.cleanTracking
                                    )
                                    param.args[1] = modified
                                    XposedBridge.log("[$TAG] Modified Intent.putExtra ($name): $value -> $modified")
                                }
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] Error hooking Intent.putExtra: ${t.message}")
        }

        // Hook 5: Uri.Builder.appendQueryParameter(String, String)
        try {
            XposedHelpers.findAndHookMethod(
                Uri.Builder::class.java,
                "appendQueryParameter",
                String::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as? String ?: return
                        if (key == "s") {
                            val config = loadConfig(null)
                            if (config.isEnabled) {
                                param.args[1] = config.customS
                                XposedBridge.log("[$TAG] Uri.Builder appendQueryParameter 's' replaced: ${config.customS}")
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] Error hooking Uri.Builder: ${t.message}")
        }
    }

    private fun interceptShareIntent(intent: Intent, context: Context?) {
        val config = loadConfig(context)
        if (!config.isEnabled) return

        if (intent.action == Intent.ACTION_CHOOSER) {
            val targetIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            if (targetIntent != null) {
                modifyIntentText(targetIntent, config)
            }
        } else if (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE) {
            modifyIntentText(intent, config)
        }
    }

    private fun modifyIntentText(intent: Intent, config: HookConfig) {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (text != null && TweetUrlModifier.containsTweetUrl(text)) {
            val modified = TweetUrlModifier.modify(text, config.customS, config.cleanTracking)
            intent.putExtra(Intent.EXTRA_TEXT, modified)
            XposedBridge.log("[$TAG] Modified share intent EXTRA_TEXT: $text -> $modified")
        }

        val clip = intent.clipData
        if (clip != null && clip.itemCount > 0) {
            for (i in 0 until clip.itemCount) {
                val item = clip.getItemAt(i)
                val clipText = item.text?.toString()
                if (clipText != null && TweetUrlModifier.containsTweetUrl(clipText)) {
                    val modified = TweetUrlModifier.modify(clipText, config.customS, config.cleanTracking)
                    intent.clipData = ClipData.newPlainText(clip.description?.label ?: "text", modified)
                    break
                }
            }
        }
    }

    data class HookConfig(
        val isEnabled: Boolean = true,
        val customS: String = "67",
        val cleanTracking: Boolean = false
    )

    private fun loadConfig(context: Context?): HookConfig {
        val effectiveContext = getEffectiveContext(context)

        // Tier 1: Direct file in Twitter package's own files directory (100% accessible to Twitter)
        val twitterFilesDirCandidates = listOf(
            File("/data/data/$TWITTER_PKG/files/twi_from_67.json"),
            File("/data/data/$TWITTER_LITE_PKG/files/twi_from_67.json"),
            if (effectiveContext != null) File(effectiveContext.filesDir, "twi_from_67.json") else null
        )
        for (targetFile in twitterFilesDirCandidates) {
            if (targetFile != null && targetFile.exists() && targetFile.canRead()) {
                try {
                    val content = targetFile.readText()
                    val customS = Regex("\"custom_s\"\\s*:\\s*\"([^\"]+)\"").find(content)?.groupValues?.get(1)
                    val isEnabled = !content.contains("\"is_enabled\":false")
                    val cleanTracking = content.contains("\"clean_tracking\":true")
                    if (!customS.isNullOrEmpty()) {
                        XposedBridge.log("[$TAG] Loaded config from Twitter files directory: customS=$customS")
                        return HookConfig(isEnabled, customS, cleanTracking)
                    }
                } catch (t: Throwable) {
                    XposedBridge.log("[$TAG] Read Twitter directTargetFile error: ${t.message}")
                }
            }
        }

        // Tier 2: Direct file from module's shared_prefs (if readable)
        val modulePrefCandidates = listOf(
            File("/data/data/$MODULE_PKG/shared_prefs/$PREFS_NAME.xml"),
            File("/data/user/0/$MODULE_PKG/shared_prefs/$PREFS_NAME.xml"),
            File("/data/user_de/0/$MODULE_PKG/shared_prefs/$PREFS_NAME.xml")
        )
        for (prefFile in modulePrefCandidates) {
            if (prefFile.exists() && prefFile.canRead()) {
                try {
                    val text = prefFile.readText()
                    val match = Regex("<string name=\"custom_s\">(.*?)</string>").find(text)
                    if (match != null) {
                        val customS = match.groupValues[1]
                        val isEnabled = !text.contains("<boolean name=\"is_enabled\" value=\"false\"")
                        val cleanTracking = text.contains("<boolean name=\"clean_tracking\" value=\"true\"")
                        XposedBridge.log("[$TAG] Loaded config from module XML file directly: customS=$customS")
                        return HookConfig(isEnabled, customS, cleanTracking)
                    }
                } catch (_: Throwable) {}
            }
        }

        // Tier 3: LSPosed XSharedPreferences
        try {
            val pref = getXPreferences()
            pref.reload()
            if (pref.contains("custom_s")) {
                val customS = pref.getString("custom_s", null)
                if (!customS.isNullOrEmpty()) {
                    val isEnabled = pref.getBoolean("is_enabled", true)
                    val cleanTracking = pref.getBoolean("clean_tracking", false)
                    XposedBridge.log("[$TAG] Loaded config from XSharedPreferences: customS=$customS")
                    return HookConfig(isEnabled, customS, cleanTracking)
                }
            }
        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] XSharedPreferences error: ${t.message}")
        }

        // Tier 4: ContentProvider query
        if (effectiveContext != null) {
            try {
                val uri = Uri.parse("content://Akari.Twitter.share.provider/config")
                val bundle = effectiveContext.contentResolver.call(uri, "getConfig", null, null)
                if (bundle != null && bundle.containsKey("custom_s")) {
                    val customS = bundle.getString("custom_s", null)
                    if (!customS.isNullOrEmpty()) {
                        val isEnabled = bundle.getBoolean("is_enabled", true)
                        val cleanTracking = bundle.getBoolean("clean_tracking", false)
                        XposedBridge.log("[$TAG] Loaded config from ContentProvider: customS=$customS")
                        return HookConfig(isEnabled, customS, cleanTracking)
                    }
                }
            } catch (t: Throwable) {
                XposedBridge.log("[$TAG] ContentProvider query error: ${t.message}")
            }
        }

        // Tier 5: Default fallback
        XposedBridge.log("[$TAG] All config sources missed, using baseline default 67")
        return HookConfig(true, "67", false)
    }
}