package com.st10036346.wordventure2

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import android.content.ClipData // Required for clipboard copying
import com.st10036346.wordventure2.databinding.ActivitySettingsBinding

/**
 * Activity for managing game settings like Dark Mode, Sound, and navigation to other settings screens.
 * Uses View Binding for cleaner and safer view access, matching the Daily1 activity structure.
 */
class SettingsActivity : AppCompatActivity() {

    // 1. Declare the binding variable.
    private lateinit var binding: ActivitySettingsBinding

    // SharedPreferences name for saving user preferences
    private val PREFS_NAME = "GameSettings"
    private val KEY_DARK_MODE = "isDarkModeEnabled"
    private val KEY_SOUND = "isSoundEnabled"

    // SharedPreferences instance
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Inflate the layout using View Binding and set the content view.
        // This line requires viewBinding = true in your build.gradle (module: app)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Set initial state of switches based on saved preferences
        setInitialState()

        // Set up all interactive elements
        setListeners()
    }

    /**
     * Loads saved preferences and sets the initial state of the UI elements.
     */
    private fun setInitialState() {
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false) // Default to light mode
        val isSound = prefs.getBoolean(KEY_SOUND, true) // Default to sound on

        // Use binding to access the switches
        binding.darkModeSwitch.isChecked = isDarkMode
        binding.soundSwitch.isChecked = isSound

        // Apply the theme immediately on creation if necessary
        applyTheme(isDarkMode)
    }

    /**
     * Sets up all click and change listeners for navigation and settings toggles.
     */
    private fun setListeners() {
        // --- Header Navigation (Using binding) ---
        binding.bookIcon.setOnClickListener {
            // Navigate back to the Main Menu.
            startActivity(Intent(this, MainMenu::class.java))
            finish()
        }

        binding.profileIcon.setOnClickListener {
            // Navigate to the Profile Page
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // --- Settings Toggles (Using binding) ---

        // Dark Mode Switch Listener
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply()

            applyTheme(isChecked)

            val toastMessage = if (isChecked) "Dark Mode On" else "Dark Mode Off"
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }

        // Sound Switch Listener
        binding.soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_SOUND, isChecked).apply()

            val toastMessage = if (isChecked) "Sound On" else "Sound Off (Muted)"
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }

        binding.editProfileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.languageButton.setOnClickListener {
            startActivity(Intent(this, LanguageActivity::class.java))
        }

        binding.helpSupportButton.setOnClickListener {
            showHelpSupportDialog()
        }
    }

    /**
     * Applies the selected theme mode (Dark or Light) to the entire application.
     */
    private fun applyTheme(isDark: Boolean) {
        val mode = if (isDark) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Shows a custom dialog with the contact email for Help & Support.
     */
    private fun showHelpSupportDialog() {
        val emailAddress = "wordventuresupport@gmail.com"

        AlertDialog.Builder(this)
            .setTitle("Help & Support")
            .setMessage("For any questions or issues, please email us at:\n\n$emailAddress")
            .setPositiveButton("Copy Email") { dialog, _ ->
                // Copy email address to clipboard
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = ClipData.newPlainText("Support Email", emailAddress)
                clipboard.setPrimaryClip(clip)

                val toast = Toast.makeText(this, "Email copied to clipboard!", Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()

                dialog.dismiss()
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}
