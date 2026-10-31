package Akari.Twitter.share

import android.app.Application
import com.google.android.material.color.DynamicColors
import Akari.Twitter.share.util.LocaleHelper

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        // Enable Material 3 Dynamic Colors (Monet) across all activities
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Apply saved language preferences
        val savedLang = LocaleHelper.getCurrentLanguage(this)
        if (savedLang != LocaleHelper.LANG_SYSTEM) {
            LocaleHelper.applyLanguage(this, savedLang)
        }
    }
}