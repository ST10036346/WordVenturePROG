package com.st10036346.wordventure2

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

// BaseActivity must extend AppCompatActivity or a derivative
abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        //Retrieves the saved language code from LocaleManager
        val savedLangCode = LocaleManager.getSavedLanguage(newBase)

        val context = updateResources(newBase, savedLangCode)

        super.attachBaseContext(context)
    }

    private fun updateResources(context: Context, languageCode: String): ContextWrapper {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = context.resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            return ContextWrapper(context.createConfigurationContext(config))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            return ContextWrapper(context)
        }
    }
}