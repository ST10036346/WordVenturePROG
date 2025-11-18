package com.st10036346.wordventure2

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleManager {

    //Title: Locale
    //Name: Android Developers
    //Date: 2025
    //URL: https://developer.android.com/reference/java/util/Locale#obtaining-a-locale

    private const val PREFS_NAME = "AppLocaleSettings"
    private const val KEY_LANGUAGE = "language_code"
    const val DEFAULT_LANG = "en" // English as default language

    fun saveLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, DEFAULT_LANG) ?: DEFAULT_LANG
    }


    
    fun wrap(context: Context, languageCode: String): ContextWrapper {
        val config = context.resources.configuration
        val locale = Locale(languageCode)

        // Set the locale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        // Apply configuration update
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return ContextWrapper(context.createConfigurationContext(config))
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            return ContextWrapper(context)
        }
    }
}
