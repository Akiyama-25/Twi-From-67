package Akari.Twitter.share.hook

import android.app.Activity
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

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        const val MODULE_PKG = "Akari.Twitter.share"
        const val TWITTER_PKG = "com.twitter.android"
        const val TWITTER_LITE_PKG = "com.twitter.android.lite"
        const val PREFS_NAME = "settings"
        const val TAG = "TwiFrom67"

        private var xSharedPreferences: XSharedPreferences? = null

        fun getXPreferences(): XSharedPreferences {
            val pref = xSharedPreferences ?: XSharedPreferences(MODULE_PKG, PREFS_NAME).apply {
                makeWorldReadable()
                reload()
            }
            xSharedPreferences = pref
            return pref
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
        hookTwitter(lpparam)
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
                        val config = loadConfig(null)
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
        // Priority 1: XSharedPreferences
        try {
            val pref = getXPreferences()
            pref.reload()
            val customS = pref.getString("custom_s", "67") ?: "67"
            val isEnabled = pref.getBoolean("is_enabled", true)
            val cleanTracking = pref.getBoolean("clean_tracking", false)
            return HookConfig(isEnabled, customS, cleanTracking)
        } catch (_: Throwable) {
            // fallback
        }

        // Priority 2: Remote ContentProvider query
        if (context != null) {
            try {
                val uri = Uri.parse("content://Akari.Twitter.share.provider/config")
                val bundle = context.contentResolver.call(uri, "getConfig", null, null)
                if (bundle != null) {
                    val customS = bundle.getString("custom_s", "67") ?: "67"
                    val isEnabled = bundle.getBoolean("is_enabled", true)
                    val cleanTracking = bundle.getBoolean("clean_tracking", false)
                    return HookConfig(isEnabled, customS, cleanTracking)
                }
            } catch (_: Throwable) {
                // Ignore
            }
        }

        // Priority 3: Default fallback
        return HookConfig(true, "67", false)
    }
}