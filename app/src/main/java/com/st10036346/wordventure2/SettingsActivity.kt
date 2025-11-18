package com.st10036346.wordventure2

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import android.content.ClipData
import com.st10036346.wordventure2.databinding.ActivitySettingsBinding


class SettingsActivity : BaseActivity() { 

    private lateinit var binding: ActivitySettingsBinding

    /**
     * Android Developers, 2025.
     * Save Key-Value Data
     * Available at: https://developer.android.com/training/data-storage/shared-preferences
     */
    private val PREFS_NAME = "GameSettings" 
    private val KEY_SOUND = "isSoundEnabled"

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // initialise SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // set initial state of switches based on saved preferences
        setInitialState()

        // set up all interactive elements
        setListeners()
    }

    //loads saved preferences
    private fun setInitialState() {
        val isSound = prefs.getBoolean(KEY_SOUND, true) // Default to sound on

        binding.soundSwitch.isChecked = isSound

    }

    
    private fun setListeners() {
        binding.bookIcon.setOnClickListener {
            // navigate to the Main Menu.
            startActivity(Intent(this, MainMenu::class.java))
            finish()
        }

        binding.profileIcon.setOnClickListener {
            // navigate to the Profile Page
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // soundSwitch Listener
        binding.soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            // save the new preference
            prefs.edit().putBoolean(KEY_SOUND, isChecked).apply()

            // call function in BackgroundMusicService to control volume
            BackgroundMusicService.setVolume(!isChecked)

            val toastMessage = if (isChecked) "Sound On" else "Sound Off"
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

    private fun showHelpSupportDialog() {
        val emailAddress = "wordventuresupport@gmail.com"

        AlertDialog.Builder(this)
            .setTitle("Help & Support")
            .setMessage("For any questions or issues, please email us at:\n\n$emailAddress")
            .setPositiveButton("Copy Email") { dialog, _ ->
                // copy email to clipboard
                /**
                 * Android Developers, 2025.
                 * Copy and Paste
                 * Available at:https://developer.android.com/develop/ui/views/touch-and-input/copy-paste
                 */
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
