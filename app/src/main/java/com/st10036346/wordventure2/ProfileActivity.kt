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
import com.st10036346.wordventure2.ProfilePhotoActivity.Companion.getProfilePicResId

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var statsManager: StatsManager
    // NEW PROPERTY: To hold the authenticated user's ID
    private lateinit var currentUserId: String

    companion object {
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // initialise Firebase Auth
        auth = Firebase.auth

        val user = auth.currentUser

        // check if user is logged in
        if (user == null) {
            Log.e(TAG, "User not authenticated. Redirecting to Login.")
            redirectToLogin()
            return
        }

        // FIX: Store the current user's ID
        currentUserId = user.uid

        // initialise View Binding
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // FIX: initialise StatsManager, passing the required userId
        statsManager = StatsManager(this, currentUserId)

        // populate UI with user data
        displayUserProfile()

        loadProfilePicture()

        displayGameStats()

        // set up listeners for navigation and actions
        setListeners()
    }

    /**
     * Ensures the profile picture is reloaded every time the user navigates back to this activity.
     */
    override fun onResume() {
        super.onResume()
        loadProfilePicture()
    }

    /**
     * Loads the saved profile picture resource ID from SharedPreferences and displays it
     */
    private fun loadProfilePicture() {
        val savedResId = getProfilePicResId(this)

        binding.profilePicture.setImageResource(savedResId)
    }

    /**
     * Populates the EditTexts with the current user's details (Email and Username).
     */
    private fun displayUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            // display User Email (unable to edit)
            binding.emailEditText.setText(user.email ?: "Email not available")
            binding.emailEditText.isFocusable = false
            binding.emailEditText.isFocusableInTouchMode = false

            // display Username (use existing display name)
            val username = user.displayName ?: user.email?.substringBefore('@') ?: "WordVenturePlayer"
            binding.usernameEditText.setText(username)
            // username is editable
            binding.usernameEditText.isFocusable = true
            binding.usernameEditText.isFocusableInTouchMode = true

            // password field is a placeholder for security
            binding.passwordEditText.isFocusable = false
            binding.passwordEditText.isFocusableInTouchMode = false
        }
    }

    /**
     * Retrieves and displays the game statistics.
     */
    private fun displayGameStats() {
        val stats = statsManager.getStats()

        try {
            // update stats
            binding.gamesPlayedValue.text = stats.gamesPlayed.toString()
            binding.winStreakValue.text = stats.winStreak.toString()
            binding.maxStreakValue.text = stats.maxStreak.toString()

            // update graph
            updateGuessDistributionGraph(stats.guessDistribution)
        } catch (e: Exception) {
            Log.e(TAG, "Error binding profile stats views: Ensure activity_profile.xml contains required TextViews.", e)
        }
    }

    // bar graph drawing for Profile Screen
    /**
     * GeeksforGeeks, 2021.
     * How to Create a BarChart in Android?
     * Available at: https://www.geeksforgeeks.org/android/how-to-create-a-barchart-in-android/
     */
    private fun updateGuessDistributionGraph(guessDist: IntArray) {
        val chartContainer = binding.guessDistributionChartContainer

        // calculate maximum count to determine bar scale
        val maxCount = guessDist.maxOrNull()?.coerceAtLeast(1) ?: 1 // Ensure maxCount is at least 1

        chartContainer.removeAllViews()

        for (i in 0 until guessDist.size) {
            val count = guessDist[i]
            val guessNumber = i + 1

            val barWeight = if (count > 0) count.toFloat() / maxCount.toFloat() else 0.05f

            // create a row for the bar
            val rowLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 4, 0, 4)
            }

            // guess number label
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

            // bar container
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

            // actual bar
            val bar = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    30, // fixed height
                    barWeight * 0.9f
                ).apply {
                    marginEnd = 4
                }
                setBackgroundColor(Color.parseColor("#8058E5"))
                text = ""
            }

            // count label
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
     * Set up click listeners for nav bar and profile actions.
     */
    private fun setListeners() {
        // navigates to previous screen
        binding.backIcon.setOnClickListener {
            finish()
        }

        // navigates to Settings Page
        binding.settingsIcon.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // logout button
        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // click on profile photo card to navigate to selection activity
        binding.profilePhotoCard.setOnClickListener {
            startActivity(Intent(this, ProfilePhotoActivity::class.java))
        }

        // save username when the EditText loses focus (when user is done editing)
        binding.usernameEditText.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
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
        // Assuming 'Login' is the name of your login activity class
        val intent = Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}