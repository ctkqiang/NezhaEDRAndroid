package xin.ctkqiang.nezhaedr.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    private const val PREFS_NAME = "nezha_edr_prefs"
    private const val KEY_LANGUAGE = "app_language_code"

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "zh") ?: "zh"
    }

    fun applyLanguage(context: Context, languageCode: String): Context {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()

        val locale = when (languageCode) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "en" -> Locale.ENGLISH
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            "th" -> Locale.Builder().setLanguage("th").build()
            else -> Locale.SIMPLIFIED_CHINESE
        }

        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        EDRLogger.i("LocaleHelper", "Applied application locale: $languageCode")
        return context.createConfigurationContext(config)
    }
}
