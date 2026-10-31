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

    /**
     * Retrieve shared preferences.
     * Attempts Context.MODE_WORLD_READABLE first so LSPosed's xposedsharedprefs hook
     * can redirect and expose the preference file across app boundaries.
     */
    @Suppress("DEPRECATION")
    fun getPrefs(context: Context): SharedPreferences {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
        } catch (_: Throwable) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getCustomS(context: Context): String {
        val s = getPrefs(context).getString(KEY_CUSTOM_S, DigitConverter.DEFAULT_S)
        return if (!s.isNullOrEmpty() && DigitConverter.isValidDigits(s)) s else DigitConverter.DEFAULT_S
    }

    fun setCustomS(context: Context, value: String) {
        val sanitized = DigitConverter.sanitizeDigits(value)
        val finalVal = if (sanitized.isNotEmpty()) sanitized else DigitConverter.DEFAULT_S

        // Write synchronously via commit() to both WORLD_READABLE and PRIVATE
        try {
            @Suppress("DEPRECATION")
            context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
                .edit().putString(KEY_CUSTOM_S, finalVal).commit()
        } catch (_: Throwable) {}

        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_CUSTOM_S, finalVal).commit()
        } catch (_: Throwable) {}

        makeWorldReadable(context)
    }

    fun isEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_ENABLED, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        try {
            @Suppress("DEPRECATION")
            context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
                .edit().putBoolean(KEY_IS_ENABLED, enabled).commit()
        } catch (_: Throwable) {}

        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_IS_ENABLED, enabled).commit()
        } catch (_: Throwable) {}

        makeWorldReadable(context)
    }

    fun isCleanTracking(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CLEAN_TRACKING, false)
    }

    fun setCleanTracking(context: Context, clean: Boolean) {
        try {
            @Suppress("DEPRECATION")
            context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
                .edit().putBoolean(KEY_CLEAN_TRACKING, clean).commit()
        } catch (_: Throwable) {}

        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_CLEAN_TRACKING, clean).commit()
        } catch (_: Throwable) {}

        makeWorldReadable(context)
    }

    fun getLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "system") ?: "system"
    }

    fun setLanguage(context: Context, langCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, langCode).apply()
    }

    private fun makeWorldReadable(context: Context) {
        try {
            val dataDir = context.applicationInfo.dataDir
            val prefsDir = File(dataDir, "shared_prefs")
            val prefsFile = File(prefsDir, "$PREFS_NAME.xml")
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false)
                prefsFile.setWritable(true, false)
            }
            if (prefsDir.exists()) {
                prefsDir.setReadable(true, false)
                prefsDir.setExecutable(true, false)
            }
            File(dataDir).setReadable(true, false)
            File(dataDir).setExecutable(true, false)
        } catch (_: Throwable) {
            // Ignore security exceptions on locked environments
        }
    }
}