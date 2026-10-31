package Akari.Twitter.share.data

import android.content.Context
import android.content.SharedPreferences
import Akari.Twitter.share.util.DigitConverter
import java.io.File

object PrefManager {

    const val PREFS_NAME = "settings"
    const val KEY_CUSTOM_S = "custom_s"
    const val KEY_IS_ENABLED = "is_enabled"
    const val KEY_CLEAN_TRACKING = "clean_tracking"
    const val KEY_LANGUAGE = "language"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCustomS(context: Context): String {
        val s = getPrefs(context).getString(KEY_CUSTOM_S, DigitConverter.DEFAULT_S)
        return if (!s.isNullOrEmpty() && DigitConverter.isValidTwoDigits(s)) s else DigitConverter.DEFAULT_S
    }

    fun setCustomS(context: Context, value: String) {
        val sanitized = DigitConverter.sanitizeToTwoDigits(value)
        val finalVal = if (sanitized.isNotEmpty()) sanitized else DigitConverter.DEFAULT_S
        getPrefs(context).edit().putString(KEY_CUSTOM_S, finalVal).apply()
        makeWorldReadable(context)
    }

    fun isEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_ENABLED, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_ENABLED, enabled).apply()
        makeWorldReadable(context)
    }

    fun isCleanTracking(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CLEAN_TRACKING, false)
    }

    fun setCleanTracking(context: Context, clean: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CLEAN_TRACKING, clean).apply()
        makeWorldReadable(context)
    }

    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, "system") ?: "system"
    }

    fun setLanguage(context: Context, langCode: String) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, langCode).apply()
    }

    private fun makeWorldReadable(context: Context) {
        try {
            val parent = context.filesDir.parentFile ?: return
            val sharedPrefsDir = File(parent, "shared_prefs")
            val prefsFile = File(sharedPrefsDir, "$PREFS_NAME.xml")
            if (parent.exists()) {
                parent.setReadable(true, false)
                parent.setExecutable(true, false)
            }
            if (sharedPrefsDir.exists()) {
                sharedPrefsDir.setReadable(true, false)
                sharedPrefsDir.setExecutable(true, false)
            }
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false)
            }
        } catch (_: Throwable) {
            // Ignore security exceptions on locked environments
        }
    }
}