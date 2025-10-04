package com.st10036346.wordventure2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.st10036346.wordventure2.databinding.ActivityProfileBinding
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    // ADDED: StatsManager instance
    private lateinit var statsManager: StatsManager

    companion object {
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initialise Firebase Auth
        auth = Firebase.auth

        // check if user is logged in
        if (auth.currentUser == null) {
            Log.e(TAG, "User not authenticated. Redirecting to Login.")
            redirectToLogin()
            return
        }

        // initialise View Binding
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ADDED: Initialize StatsManager
        statsManager = StatsManager(this)

        // populate UI with user data
        displayUserProfile()

        // ADDED: Display game statistics
        displayGameStats()

        // set up listeners for navigation and actions
        setListeners()
    }

    /**
     * Populates the EditTexts with the current user's details (Email and Username).
     */
    private fun displayUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            // Display User Email (Read-only)
            binding.emailEditText.setText(user.email ?: "Email not available")
            binding.emailEditText.isFocusable = false
            binding.emailEditText.isFocusableInTouchMode = false

            // Display Username (Use existing display name or fallback)
            val username = user.displayName ?: user.email?.substringBefore('@') ?: "WordVenturePlayer"
            binding.usernameEditText.setText(username)
            // Ensure username is editable
            binding.usernameEditText.isFocusable = true
            binding.usernameEditText.isFocusableInTouchMode = true

            // Password field remains a placeholder for security (********)
            binding.passwordEditText.isFocusable = false
            binding.passwordEditText.isFocusableInTouchMode = false
        }
    }

    /**
     * Retrieves and displays the game statistics.
     * NOTE: You must have corresponding IDs in your activity_profile.xml (e.g., profile_games_played_value)
     */
    private fun displayGameStats() {
        val stats = statsManager.getStats()

        // ASSUMPTION: You have these TextViews in activity_profile.xml to display the stats
        try {
            // Update Basic Stats
            binding.gamesPlayedValue.text = stats.gamesPlayed.toString()
            binding.winStreakValue.text = stats.winStreak.toString()
            binding.maxStreakValue.text = stats.maxStreak.toString()

            // Update Graph
            updateGuessDistributionGraph(stats.guessDistribution)
        } catch (e: Exception) {
            Log.e(TAG, "Error binding profile stats views: Ensure activity_profile.xml contains games_played_value, win_streak_value, max_streak_value, and guess_distribution_chart_container.", e)
            // Show a user-friendly error or skip updating these views
        }
    }

    // NEW FUNCTION: Handles the bar graph drawing for the Profile Screen
    // This is a duplicate of the logic in Daily1.kt for convenience.
    private fun updateGuessDistributionGraph(guessDist: IntArray) {
        // ASSUMPTION: You have a LinearLayout with this ID in your activity_profile.xml
        val chartContainer = binding.guessDistributionChartContainer

        // Calculate the maximum count to determine bar scale
        val maxCount = guessDist.maxOrNull()?.coerceAtLeast(1) ?: 1 // Ensure maxCount is at least 1

        chartContainer.removeAllViews() // Clear any old views

        for (i in 0 until guessDist.size) {
            val count = guessDist[i]
            val guessNumber = i + 1

            val barWeight = if (count > 0) count.toFloat() / maxCount.toFloat() else 0.05f

            // Create a row for the bar (Guess #: [Bar] Count)
            val rowLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 4, 0, 4)
            }

            // 1. Guess Number Label
            val label = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 8
                }
                text = guessNumber.toString()
                textSize = 14f
                setTextColor(Color.parseColor("#051646"))
                setTypeface(null, Typeface.BOLD)
            }

            // 2. Bar Container
            val barContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.TRANSPARENT)
            }

            // The actual bar
            val bar = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    30, // Fixed height for the bar
                    barWeight * 0.9f
                ).apply {
                    marginEnd = 4
                }
                setBackgroundColor(Color.parseColor("#8058E5")) // Bar color
                text = ""
            }

            // 3. Count Label
            val countLabel = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = count.toString()
                textSize = 14f
                setTextColor(Color.parseColor("#051646"))
                setTypeface(null, Typeface.BOLD)
            }

            barContainer.addView(bar)
            rowLayout.addView(label)
            rowLayout.addView(barContainer)
            rowLayout.addView(countLabel)

            chartContainer.addView(rowLayout)
        }
    }

    /**
     * Sets up click listeners for the nav bar.
     */
    private fun setListeners() {
        // nav bar

        // Back Icon navigates to the previous screen (typically Main Menu)
        binding.backIcon.setOnClickListener {
            finish() // Standard back behavior
        }

        // Settings Icon navigates to the Settings Page
        binding.settingsIcon.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Logout button logs user out
        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // --- Username Save Listener ---
        // Save the username when the EditText loses focus (user is done editing).
        binding.usernameEditText.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                // If focus is lost, attempt to save the new username
                updateUsername()
            }
        }
    }

    /**
     * Updates the user's display name (username) in Firebase Authentication.
     */
    private fun updateUsername() {
        val newUsername = binding.usernameEditText.text.toString().trim()
        val user = auth.currentUser

        // Only proceed if the user is valid, the new username is not empty, and it's actually different.
        if (user != null && newUsername.isNotEmpty() && newUsername != user.displayName) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newUsername)
                .build()

            user.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "User display name updated to: $newUsername")
                        Toast.makeText(this, "Username updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "Failed to update display name: ${task.exception?.message}")
                        Toast.makeText(this, "Failed to update username. Try again.", Toast.LENGTH_LONG).show()
                        // Revert text field to the last known good value if update fails
                        binding.usernameEditText.setText(user.displayName ?: "")
                    }
                }
        } else if (newUsername.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty.", Toast.LENGTH_SHORT).show()
            // Revert text field to the last known good value if empty
            binding.usernameEditText.setText(user?.displayName ?: "WordVenturePlayer")
        }
    }

    /**
     * Shows an AlertDialog to confirm the user wants to log out.
     */
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to log out of WordVenture?")
            .setPositiveButton("Logout") { dialog, _ ->
                auth.signOut()
                Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
                redirectToLogin()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    /**
     * Helper function to clear the activity stack and redirect to the Login screen.
     */
    private fun redirectToLogin() {
        val intent = Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}