package com.st10036346.wordventure2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest // Added for updating display name
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.st10036346.wordventure2.databinding.ActivityProfileBinding
import android.app.AlertDialog

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth

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

        // populate UI with user data
        displayUserProfile()

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
            // incorporate biometrics in part 3
            binding.passwordEditText.isFocusable = false
            binding.passwordEditText.isFocusableInTouchMode = false
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
