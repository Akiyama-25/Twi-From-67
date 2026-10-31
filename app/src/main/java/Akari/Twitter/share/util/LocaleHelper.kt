package Akari.Twitter.share.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import Akari.Twitter.share.data.PrefManager

object LocaleHelper {

    const val LANG_SYSTEM = "system"
    const val LANG_ZH_HANS = "zh-Hans"
    const val LANG_ZH_HANT = "zh-Hant"
    const val LANG_YUE = "zh-yue"
    const val LANG_EN = "en"
    const val LANG_JA = "ja"

    fun applyLanguage(context: Context, langCode: String) {
        PrefManager.setLanguage(context, langCode)
        val localeList = when (langCode) {
            LANG_ZH_HANS -> LocaleListCompat.forLanguageTags("zh-Hans")
            LANG_ZH_HANT -> LocaleListCompat.forLanguageTags("zh-Hant")
            LANG_YUE -> LocaleListCompat.forLanguageTags("yue-Hant,yue,zh-yue")
            LANG_EN -> LocaleListCompat.forLanguageTags("en")
            LANG_JA -> LocaleListCompat.forLanguageTags("ja")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getCurrentLanguage(context: Context): String {
        return PrefManager.getLanguage(context)
    }
}