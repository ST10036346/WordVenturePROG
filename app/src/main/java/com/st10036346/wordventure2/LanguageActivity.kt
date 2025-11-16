package com.st10036346.wordventure2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.st10036346.wordventure2.BaseActivity
import com.st10036346.wordventure2.databinding.ActivityLanguageBinding
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class LanguageActivity : BaseActivity() {

    private lateinit var binding: ActivityLanguageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLanguageSelection()

        try {
            binding.backIcon.setOnClickListener {
                finish() // Close the current activity and return to the previous one
            }
        } catch (e: Exception) {
        }
    }

    private fun setupLanguageSelection() {
        // Find buttons by their assumed IDs and map them to their language codes
        binding.root.findViewById<Button>(R.id.btnEnglish)?.setOnClickListener {
            setNewLocale("en")
        }
        binding.root.findViewById<Button>(R.id.btnAfrikaans)?.setOnClickListener {
            setNewLocale("af")
        }
        binding.root.findViewById<Button>(R.id.btnZulu)?.setOnClickListener {
            setNewLocale("zu")
        }
    }

    //applies language to application based on users choice
    private fun setNewLocale(languageCode: String) {
        //Save the new language
        LocaleManager.saveLanguage(this, languageCode)

        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)

        // Applies the locale change globally
        AppCompatDelegate.setApplicationLocales(appLocale)

        Toast.makeText(this, "Language updated successfully!", Toast.LENGTH_SHORT).show()

        finish()
    }
}