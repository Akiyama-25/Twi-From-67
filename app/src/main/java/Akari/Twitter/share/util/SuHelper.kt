package Akari.Twitter.share.util

import android.content.Context
import android.util.Log
import java.io.DataOutputStream
import java.io.File

object SuHelper {

    private const val TAG = "TwiFrom67_SU"
    const val TWITTER_PKG = "com.twitter.android"
    const val TWITTER_LITE_PKG = "com.twitter.android.lite"
    const val MODULE_PKG = "Akari.Twitter.share"

    /**
     * Executes shell commands as root (su).
     */
    fun execRoot(vararg commands: String): Boolean {
        var process: Process? = null
        var os: DataOutputStream? = null
        return try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            for (cmd in commands) {
                os.writeBytes("$cmd\n")
            }
            os.writeBytes("exit\n")
            os.flush()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (t: Throwable) {
            Log.e(TAG, "execRoot failed: ${t.message}")
            false
        } finally {
            try {
                os?.close()
            } catch (_: Throwable) {}
            try {
                process?.destroy()
            } catch (_: Throwable) {}
        }
    }

    /**
     * Checks if SU binary is accessible and root is granted.
     */
    fun isRootAvailable(): Boolean {
        return execRoot("id")
    }

    /**
     * Synchronizes configuration files directly into Twitter's data directories
     * and sets open permissions on the module's shared_prefs to overcome SELinux/DAC blocks.
     */
    fun syncConfig(
        context: Context,
        customS: String,
        isEnabled: Boolean,
        cleanTracking: Boolean
    ): Boolean {
        val jsonPayload = "{\\\"custom_s\\\":\\\"$customS\\\",\\\"is_enabled\\\":$isEnabled,\\\"clean_tracking\\\":$cleanTracking}"
        val moduleDataDir = context.applicationInfo.dataDir

        val cmds = mutableListOf<String>()

        // 1. Relax module permissions for XSharedPreferences direct read
        cmds.add("chmod 777 $moduleDataDir 2>/dev/null || true")
        cmds.add("chmod 777 $moduleDataDir/shared_prefs 2>/dev/null || true")
        cmds.add("chmod 666 $moduleDataDir/shared_prefs/* 2>/dev/null || true")

        // 2. Write direct config into Twitter package's files directory (if directory exists)
        cmds.add("mkdir -p /data/data/$TWITTER_PKG/files 2>/dev/null || true")
        cmds.add("echo '$jsonPayload' > /data/data/$TWITTER_PKG/files/twi_from_67.json 2>/dev/null || true")
        cmds.add("chmod 666 /data/data/$TWITTER_PKG/files/twi_from_67.json 2>/dev/null || true")
        cmds.add("chown --reference=/data/data/$TWITTER_PKG/files /data/data/$TWITTER_PKG/files/twi_from_67.json 2>/dev/null || true")

        // 3. Write direct config into Twitter Lite if present
        cmds.add("if [ -d /data/data/$TWITTER_LITE_PKG ]; then " +
                "mkdir -p /data/data/$TWITTER_LITE_PKG/files && " +
                "echo '$jsonPayload' > /data/data/$TWITTER_LITE_PKG/files/twi_from_67.json && " +
                "chmod 666 /data/data/$TWITTER_LITE_PKG/files/twi_from_67.json && " +
                "chown --reference=/data/data/$TWITTER_LITE_PKG/files /data/data/$TWITTER_LITE_PKG/files/twi_from_67.json 2>/dev/null; fi")

        return execRoot(*cmds.toTypedArray())
    }

    /**
     * Force-stops X (Twitter) process via SU and relaunches it.
     */
    fun restartTwitter(context: Context): Boolean {
        // Also perform sync before restarting
        val customS = PrefManager.getCustomS(context)
        val isEnabled = PrefManager.isEnabled(context)
        val cleanTracking = PrefManager.isCleanTracking(context)
        syncConfig(context, customS, isEnabled, cleanTracking)

        val cmds = listOf(
            // Force-stop Twitter and Twitter Lite
            "am force-stop $TWITTER_PKG",
            "am force-stop $TWITTER_LITE_PKG",
            "sleep 0.3",
            // Relaunch Twitter
            "monkey -p $TWITTER_PKG -c android.intent.category.LAUNCHER 1 2>/dev/null || " +
                    "monkey -p $TWITTER_LITE_PKG -c android.intent.category.LAUNCHER 1 2>/dev/null"
        )

        return execRoot(*cmds.toTypedArray())
    }
}