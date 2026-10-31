package Akari.Twitter.share.hook

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
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
        const val INTERNAL_PREF = "twi_from_67_internal"
        const val ACTION_UPDATE_CONFIG = "Akari.Twitter.share.ACTION_UPDATE_CONFIG"
        const val TAG = "TwiFrom67"

        @Volatile
        private var xSharedPreferences: XSharedPreferences? = null

        @Volatile
        private var twitterAppContext: Context? = null

        @Volatile
        private var isReceiverRegistered = false

        @Volatile
        private var cachedConfig: HookConfig = HookConfig(true, "67", false)

        fun getXPreferences(): XSharedPreferences {
            val pref = xSharedPreferences ?: XSharedPreferences(MODULE_PKG, PREFS_NAME)
            try {
                pref.makeWorldReadable()
            } catch (_: Throwable) {}
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

        fun registerConfigReceiverIfNeeded(context: Context?) {
            if (context == null || isReceiverRegistered) return
            synchronized(MainHook::class.java) {
                if (isReceiverRegistered) return
                try {
                    val filter = IntentFilter(ACTION_UPDATE_CONFIG)
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context?, intent: Intent?) {
                            if (intent == null) return
                            val customS = intent.getStringExtra("custom_s")
                            val isEnabled = intent.getBooleanExtra("is_enabled", true)
                            val cleanTracking = intent.getBooleanExtra("clean_tracking", false)

                            if (!customS.isNullOrEmpty() && customS.all { it in '0'..'9' }) {
                                val cfg = HookConfig(isEnabled, customS, cleanTracking)
                                cachedConfig = cfg
                                XposedBridge.log("[$TAG] Broadcast received config update: customS=$customS")

                                // Persist to Twitter's own internal storage
                                ctx?.let { c ->
                                    try {
                                        c.getSharedPreferences(INTERNAL_PREF, Context.MODE_PRIVATE)
                                            .edit()
                                            .putString("custom_s", customS)
                                            .putBoolean("is_enabled", isEnabled)
                                            .putBoolean("clean_tracking", cleanTracking)
                                            .commit()
                                        File(c.filesDir, "custom_s.txt").writeText(customS)
                                    } catch (_: Throwable) {}
                                }
                            }
                        }
                    }

                    // Register with Exported flag support across Android versions
                    try {
                        context.registerReceiver(receiver, filter)
                    } catch (_: Throwable) {
                        try {
                            val method = Context::class.java.getMethod(
                                "registerReceiver",
                                BroadcastReceiver::class.java,
                                IntentFilter::class.java,
                                Int::class.javaPrimitiveType
                            )
                            // 2 = Context.RECEIVER_EXPORTED
                            method.invoke(context, receiver, filter, 2)
                        } catch (t: Throwable) {
                            XposedBridge.log("[$TAG] Failed to register broadcast receiver: ${t.message}")
                        }
                    }
                    isReceiverRegistered = true
                    XposedBridge.log("[$TAG] Successfully registered live BroadcastReceiver in Twitter process")
                } catch (t: Throwable) {
                    XposedBridge.log("[$TAG] Exception registering receiver: ${t.message}")
                }
            }
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        try {
            xSharedPreferences = XSharedPreferences(MODULE_PKG, PREFS_NAME)
            xSharedPreferences?.makeWorldReadable()
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
        // Hook Application.attach
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
                            registerConfigReceiverIfNeeded(app)
                            loadConfig(app)
                            XposedBridge.log("[$TAG] Application.attach: Context acquired and initialized")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] Error hooking Application.attach: ${t.message}")
        }

        // Hook Activity.onCreate as secondary Context capturer
        try {
            XposedHelpers.findAndHookMethod(
                Activity::class.java,
                "onCreate",
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val activity = param.thisObject as? Activity
                        if (activity != null) {
                            if (twitterAppContext == null) {
                                twitterAppContext = activity.applicationContext
                            }
                            registerConfigReceiverIfNeeded(activity)
                            loadConfig(activity)
                        }
                    }
                }
            )
        } catch (_: Throwable) {}
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
                        // Prevent intercepting our own URL rewriter calls
                        if (TweetUrlModifier.isRewriting.get() == true) {
                            return
                        }
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
        registerConfigReceiverIfNeeded(effectiveContext)

        // Tier 1: Plain text file in Twitter's own files directory (e.g. custom_s.txt)
        val textCandidates = listOf(
            File("/data/data/$TWITTER_PKG/files/custom_s.txt"),
            File("/data/user/0/$TWITTER_PKG/files/custom_s.txt"),
            File("/data/data/$TWITTER_LITE_PKG/files/custom_s.txt"),
            File("/data/user/0/$TWITTER_LITE_PKG/files/custom_s.txt"),
            if (effectiveContext != null) File(effectiveContext.filesDir, "custom_s.txt") else null
        )
        for (f in textCandidates) {
            if (f != null && f.exists() && f.canRead()) {
                try {
                    val textVal = f.readText().trim()
                    if (textVal.isNotEmpty() && textVal.all { it in '0'..'9' }) {
                        val cfg = HookConfig(true, textVal, cachedConfig.cleanTracking)
                        cachedConfig = cfg
                        XposedBridge.log("[$TAG] Tier 1 loaded custom_s.txt: $textVal")
                        return cfg
                    }
                } catch (_: Throwable) {}
            }
        }

        // Tier 2: Direct JSON file in Twitter's own files directory
        val jsonCandidates = listOf(
            File("/data/data/$TWITTER_PKG/files/twi_from_67.json"),
            File("/data/user/0/$TWITTER_PKG/files/twi_from_67.json"),
            File("/data/data/$TWITTER_LITE_PKG/files/twi_from_67.json"),
            File("/data/user/0/$TWITTER_LITE_PKG/files/twi_from_67.json"),
            if (effectiveContext != null) File(effectiveContext.filesDir, "twi_from_67.json") else null
        )
        for (targetFile in jsonCandidates) {
            if (targetFile != null && targetFile.exists() && targetFile.canRead()) {
                try {
                    val content = targetFile.readText()
                    val customS = Regex("\"custom_s\"\\s*:\\s*\"([0-9]+)\"").find(content)?.groupValues?.get(1)
                    val isEnabled = !content.contains("\"is_enabled\":false")
                    val cleanTracking = content.contains("\"clean_tracking\":true")
                    if (!customS.isNullOrEmpty()) {
                        val cfg = HookConfig(isEnabled, customS, cleanTracking)
                        cachedConfig = cfg
                        XposedBridge.log("[$TAG] Tier 2 loaded twi_from_67.json: customS=$customS")
                        return cfg
                    }
                } catch (t: Throwable) {
                    XposedBridge.log("[$TAG] Tier 2 JSON error: ${t.message}")
                }
            }
        }

        // Tier 3: Twitter internal SharedPreferences (persisted via Broadcast)
        if (effectiveContext != null) {
            try {
                val sp = effectiveContext.getSharedPreferences(INTERNAL_PREF, Context.MODE_PRIVATE)
                val customS = sp.getString("custom_s", null)
                if (!customS.isNullOrEmpty() && customS.all { it in '0'..'9' }) {
                    val isEnabled = sp.getBoolean("is_enabled", true)
                    val cleanTracking = sp.getBoolean("clean_tracking", false)
                    val cfg = HookConfig(isEnabled, customS, cleanTracking)
                    cachedConfig = cfg
                    XposedBridge.log("[$TAG] Tier 3 loaded internal SharedPreferences: customS=$customS")
                    return cfg
                }
            } catch (_: Throwable) {}
        }

        // Tier 4: Direct file from module's shared_prefs (if readable)
        val modulePrefCandidates = listOf(
            File("/data/data/$MODULE_PKG/shared_prefs/$PREFS_NAME.xml"),
            File("/data/user/0/$MODULE_PKG/shared_prefs/$PREFS_NAME.xml"),
            File("/data/user_de/0/$MODULE_PKG/shared_prefs/$PREFS_NAME.xml")
        )
        for (prefFile in modulePrefCandidates) {
            if (prefFile.exists() && prefFile.canRead()) {
                try {
                    val text = prefFile.readText()
                    val match = Regex("<string name=\"custom_s\">([0-9]+)</string>").find(text)
                    if (match != null) {
                        val customS = match.groupValues[1]
                        val isEnabled = !text.contains("<boolean name=\"is_enabled\" value=\"false\"")
                        val cleanTracking = text.contains("<boolean name=\"clean_tracking\" value=\"true\"")
                        val cfg = HookConfig(isEnabled, customS, cleanTracking)
                        cachedConfig = cfg
                        XposedBridge.log("[$TAG] Tier 4 loaded module XML: customS=$customS")
                        return cfg
                    }
                } catch (_: Throwable) {}
            }
        }

        // Tier 5: LSPosed XSharedPreferences
        try {
            val pref = getXPreferences()
            pref.reload()
            if (pref.contains("custom_s")) {
                val customS = pref.getString("custom_s", null)
                if (!customS.isNullOrEmpty() && customS.all { it in '0'..'9' }) {
                    val isEnabled = pref.getBoolean("is_enabled", true)
                    val cleanTracking = pref.getBoolean("clean_tracking", false)
                    val cfg = HookConfig(isEnabled, customS, cleanTracking)
                    cachedConfig = cfg
                    XposedBridge.log("[$TAG] Tier 5 loaded XSharedPreferences: customS=$customS")
                    return cfg
                }
            }
        } catch (t: Throwable) {
            XposedBridge.log("[$TAG] Tier 5 XSharedPreferences error: ${t.message}")
        }

        // Tier 6: ContentProvider query
        if (effectiveContext != null) {
            try {
                val uri = Uri.parse("content://Akari.Twitter.share.provider/config")
                val bundle = effectiveContext.contentResolver.call(uri, "getConfig", null, null)
                if (bundle != null && bundle.containsKey("custom_s")) {
                    val customS = bundle.getString("custom_s", null)
                    if (!customS.isNullOrEmpty() && customS.all { it in '0'..'9' }) {
                        val isEnabled = bundle.getBoolean("is_enabled", true)
                        val cleanTracking = bundle.getBoolean("clean_tracking", false)
                        val cfg = HookConfig(isEnabled, customS, cleanTracking)
                        cachedConfig = cfg
                        XposedBridge.log("[$TAG] Tier 6 loaded ContentProvider: customS=$customS")
                        return cfg
                    }
                }
            } catch (t: Throwable) {
                XposedBridge.log("[$TAG] Tier 6 ContentProvider query error: ${t.message}")
            }
        }

        // Tier 7: Return cachedConfig if previously loaded, else default 67
        return cachedConfig
    }
}