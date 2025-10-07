package com.st10036346.wordventure2

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth // ADDED: Firebase Auth import
import com.st10036346.wordventure2.R
import com.st10036346.wordventure2.databinding.ActivityLevelsBinding

class Levels : AppCompatActivity() {

    private lateinit var binding: ActivityLevelsBinding

    // Shared preference keys
    private val BASE_PREFS_NAME = "GameProgress" // Renamed for consistency
    private val KEY_UNLOCKED_LEVEL = "current_level_unlocked"

    // NEW: Authentication properties
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUserId: String

    private val MAX_DISPLAY_LEVEL = 9999 // Effectively unlimited cap for safety


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // 2. Check and assign user ID
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please log in to see levels.", Toast.LENGTH_LONG).show()
            // Assuming 'Login' is your login activity
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }
        currentUserId = user.uid // Store the authenticated user ID

        binding = ActivityLevelsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the level buttons every time the screen is displayed
        displayLevelButtons()
    }

    private fun setupListeners() {
        // Back icon goes to the previous screen (Main Menu, based on navigation flow)
        binding.backIcon.setOnClickListener { finish() }

        // Assuming ProfileActivity and SettingsActivity exist
        binding.profileIcon.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        binding.settingsIcon.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    private fun getCurrentUnlockedLevel(): Int {
        // FIX: Access the preference file using the user ID
        val prefsNameWithId = "${BASE_PREFS_NAME}_${currentUserId}"
        val prefs = getSharedPreferences(prefsNameWithId, Context.MODE_PRIVATE)
        // Default to 1 (Level 1 is always unlocked)
        return prefs.getInt(KEY_UNLOCKED_LEVEL, 1)
    }

    private fun displayLevelButtons() {
        val container = binding.levelsContainer
        container.removeAllViews() // Clear previous buttons

        val unlockedLevel = getCurrentUnlockedLevel()

        // 1. Add the main "Play Next Level" button
        if (unlockedLevel <= MAX_DISPLAY_LEVEL) {
            val nextButton = createLevelButton(unlockedLevel, isNext = true)
            nextButton.setOnClickListener {
                startLevel(unlockedLevel)
            }
            container.addView(nextButton)

            // Add a divider text below the main button (if at least one level is complete)
            if (unlockedLevel > 1) {
                val divider = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        // Set margins for spacing
                        setMargins(0, 48, 0, 16)
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    text = "--- COMPLETED LEVELS ---"
                    // Set Typeface for Monospace font
                    setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
                    textSize = 12f
                    setTextColor(Color.parseColor("#051646"))
                }
                container.addView(divider)
            }
        } else {
            // Player has completed the maximum displayable levels
            val finishedText = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "🎉 You have completed all known levels! 🎉"
                // Use custom font or fallback
                try {
                    val customFont = ResourcesCompat.getFont(context, R.font.barriecito) // Using barriecito for consistency
                    setTypeface(customFont, Typeface.BOLD)
                } catch (e: Exception) {
                    setTypeface(null, Typeface.BOLD)
                }
                textSize = 20f
                setTextColor(Color.parseColor("#051646"))
                gravity = Gravity.CENTER
            }
            container.addView(finishedText)
        }

        // 2. Add buttons for previously completed levels (Level 1 up to unlockedLevel - 1)
        for (i in 1 until unlockedLevel) {
            val completedButton = createLevelButton(i, isNext = false)
            completedButton.setOnClickListener {
                startLevel(i) // Allows replaying old levels
            }
            container.addView(completedButton)
        }
    }

    private fun createLevelButton(levelNum: Int, isNext: Boolean): Button {
        val buttonText = if (isNext) "PLAY NEXT LEVEL: $levelNum" else "REPLAY LEVEL $levelNum"

        val buttonColor = if (isNext)
            Color.parseColor("#8058E5") // Primary color for next level
        else
            Color.parseColor("#A999E0") // Secondary color for replay

        return Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // Ensure R.dimen.level_button_width is defined in res/values/dimens.xml
                width = resources.getDimensionPixelSize(R.dimen.level_button_width)
                setMargins(0, 16, 0, 16)
                gravity = Gravity.CENTER_HORIZONTAL // Center the button within the container
            }
            text = buttonText
            gravity = Gravity.CENTER
            textSize = 20f
            setBackgroundColor(buttonColor)
            setTextColor(Color.WHITE)

            // Correct way to set a custom font programmatically
            try {
                // R.font.barriecito is a common header font used in your prior XML
                val customFont = ResourcesCompat.getFont(context, R.font.barriecito)
                setTypeface(customFont, Typeface.BOLD)
            } catch (e: Exception) {
                // Fallback
                setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun startLevel(levelNum: Int) {
        Toast.makeText(this, "Starting Level $levelNum...", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, Play::class.java).apply {
            // Pass the level number to the game activity
            putExtra("LEVEL_NUMBER", levelNum)
        }
        startActivity(intent)
    }
}