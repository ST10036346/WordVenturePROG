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
import com.st10036346.wordventure2.R // Ensure R is imported
import com.st10036346.wordventure2.databinding.ActivityLevelsBinding

class Levels : AppCompatActivity() {

    private lateinit var binding: ActivityLevelsBinding

    // Levels are now unlimited (limited only by the word file size)
    private val MAX_DISPLAY_LEVEL = 9999 // Effectively unlimited cap for safety

    // Shared preference keys
    private val PREFS_NAME = "GameProgress"
    private val KEY_UNLOCKED_LEVEL = "current_level_unlocked"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        binding.backIcon.setOnClickListener { finish() }
        // Assuming ProfileActivity and SettingsActivity exist
        binding.profileIcon.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        binding.settingsIcon.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    private fun getCurrentUnlockedLevel(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Default to 1 (Level 1 is always unlocked)
        return prefs.getInt(KEY_UNLOCKED_LEVEL, 1)
    }

    // NOTE: This function is for illustration; you'll implement the actual save logic
    // in your game activity (Daily1.kt) upon a win.
    /*
    fun saveProgress(levelCompleted: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nextLevel = levelCompleted + 1
        if (nextLevel <= MAX_DISPLAY_LEVEL) {
             prefs.edit().putInt(KEY_UNLOCKED_LEVEL, nextLevel).apply()
        }
    }
    */

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
                    // FIX: Use setTypeface for Monospace font
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
                // FIX: Use setTypeface for custom font or fallback
                try {
                    val customFont = ResourcesCompat.getFont(context, R.font.comic_relief)
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

            // FIX: Correct way to set a custom font programmatically
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
        val intent = Intent(this, Daily1::class.java).apply {
            // Pass the level number to the game activity
            putExtra("LEVEL_NUMBER", levelNum)
        }
        startActivity(intent)
    }
}