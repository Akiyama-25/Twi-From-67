package Akari.Twitter.share.util

import android.content.Context
import android.util.Log
import Akari.Twitter.share.data.PrefManager
import java.io.DataOutputStream
import java.io.File

object SuHelper {

    private const val TAG = "TwiFrom67_SU"
    const val TWITTER_PKG = "com.twitter.android"
    const val TWITTER_LITE_PKG = "com.twitter.android.lite"
    const val MODULE_PKG = "Akari.Twitter.share"
    const val ACTION_UPDATE_CONFIG = "Akari.Twitter.share.ACTION_UPDATE_CONFIG"

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
     * with matching permissions, ownership and SELinux contexts (chcon/restorecon)
     * so Twitter's untrusted_app domain can access it without restriction.
     * Writes both a simple single-value file (custom_s.txt) and a JSON config.
     */
    fun syncConfig(
        context: Context,
        customS: String,
        isEnabled: Boolean,
        cleanTracking: Boolean
    ): Boolean {
        val jsonPayload = "{\"custom_s\":\"$customS\",\"is_enabled\":$isEnabled,\"clean_tracking\":$cleanTracking}"
        val moduleDataDir = context.applicationInfo.dataDir

        val cmds = mutableListOf<String>()

        // 1. Relax module permissions and restore SELinux labels
        cmds.add("chmod 777 $moduleDataDir 2>/dev/null || true")
        cmds.add("chmod 777 $moduleDataDir/shared_prefs 2>/dev/null || true")
        cmds.add("chmod 666 $moduleDataDir/shared_prefs/* 2>/dev/null || true")
        cmds.add("restorecon -R $moduleDataDir 2>/dev/null || true")

        // 2. Direct write into Twitter package's files directory (both JSON and plain text number)
        for (baseDir in listOf("/data/data/$TWITTER_PKG", "/data/user/0/$TWITTER_PKG")) {
            cmds.add("mkdir -p $baseDir/files 2>/dev/null || true")
            // Plain text number file
            cmds.add("echo -n '$customS' > $baseDir/files/custom_s.txt 2>/dev/null || true")
            cmds.add("chmod 666 $baseDir/files/custom_s.txt 2>/dev/null || true")
            cmds.add("chown --reference=$baseDir/files $baseDir/files/custom_s.txt 2>/dev/null || true")
            cmds.add("chcon --reference=$baseDir/files $baseDir/files/custom_s.txt 2>/dev/null || true")
            cmds.add("restorecon $baseDir/files/custom_s.txt 2>/dev/null || true")

            // JSON file
            cmds.add("echo '$jsonPayload' > $baseDir/files/twi_from_67.json 2>/dev/null || true")
            cmds.add("chmod 666 $baseDir/files/twi_from_67.json 2>/dev/null || true")
            cmds.add("chown --reference=$baseDir/files $baseDir/files/twi_from_67.json 2>/dev/null || true")
            cmds.add("chcon --reference=$baseDir/files $baseDir/files/twi_from_67.json 2>/dev/null || true")
            cmds.add("restorecon $baseDir/files/twi_from_67.json 2>/dev/null || true")
        }

        // 3. Twitter Lite if present
        for (baseDir in listOf("/data/data/$TWITTER_LITE_PKG", "/data/user/0/$TWITTER_LITE_PKG")) {
            cmds.add("if [ -d $baseDir ]; then " +
                    "mkdir -p $baseDir/files && " +
                    "echo -n '$customS' > $baseDir/files/custom_s.txt && " +
                    "chmod 666 $baseDir/files/custom_s.txt && " +
                    "echo '$jsonPayload' > $baseDir/files/twi_from_67.json && " +
                    "chmod 666 $baseDir/files/twi_from_67.json && " +
                    "chown --reference=$baseDir/files $baseDir/files/custom_s.txt 2>/dev/null && " +
                    "chown --reference=$baseDir/files $baseDir/files/twi_from_67.json 2>/dev/null && " +
                    "restorecon $baseDir/files/custom_s.txt 2>/dev/null && " +
                    "restorecon $baseDir/files/twi_from_67.json 2>/dev/null; fi")
        }

        return execRoot(*cmds.toTypedArray())
    }

    /**
     * Force-stops X (Twitter) process via SU, updates configuration, and relaunches it.
     */
    fun restartTwitter(context: Context, newCustomS: String? = null): Boolean {
        val customS = newCustomS ?: PrefManager.getCustomS(context)
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