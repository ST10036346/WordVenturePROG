package com.st10036346.wordventure2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
// Add these required imports
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// Remove the extra "class" keyword from the import line
// class <-- This was causing a syntax error

class MainMenu : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "MainMenu"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        auth = Firebase.auth

        // --- Your existing user login check (This is good!) ---
        if (auth.currentUser == null) {
            Log.e(TAG, "User is not logged in. Redirecting to Login.")
            Toast.makeText(this, "Session expired, please log in again.", Toast.LENGTH_LONG).show()

            val intent = Intent(this, Login::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        val dailyButton: Button = findViewById(R.id.dailyButton)
        val playButton: Button = findViewById(R.id.playButton)
        val multiplayerButton: Button = findViewById(R.id.multiplayerButton)

        val profileIcon: ImageView = findViewById(R.id.profileIcon)
        val settingsIcon: ImageView = findViewById(R.id.settingsIcon)

        // --- UPDATED Daily Button Click Listener ---
        dailyButton.setOnClickListener {
            // Access SharedPreferences to check the last play date
            val prefs = getSharedPreferences("DailyChallenge", Context.MODE_PRIVATE)
            val lastPlayDate = prefs.getString("lastPlayDate", null)

            // Get today's date in the same format ("yyyy-MM-dd")
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            Log.d(TAG, "Daily button clicked. Today: $today, Last Play Date: $lastPlayDate")

            val intent = Intent(this, Daily1::class.java)

            // Compare dates to decide which mode to launch
            if (lastPlayDate == today) {
                // The user has already played today, launch in replay mode
                Log.d(TAG, "Launching Daily1 in replay mode.")
                intent.putExtra("IS_REPLAY", true)
            } else {
                // It's a new day or the first time playing, launch in normal mode
                Log.d(TAG, "Launching Daily1 in normal play mode.")
                intent.putExtra("IS_REPLAY", false)
            }
            startActivity(intent)
        }

        // --- The rest of your listeners remain the same ---
        playButton.setOnClickListener {
            Log.d(TAG, "Play button clicked. Navigating to Daily1.")
            // The regular play button should always start a fresh game
            val intent = Intent(this, Daily1::class.java)
            intent.putExtra("IS_REPLAY", false) // Explicitly set to false
            startActivity(intent)
        }

        multiplayerButton.setOnClickListener {
            // Changed this to navigate to Muliplayer activity
            startActivity(Intent(this, Muliplayer::class.java))
        }

        profileIcon.setOnClickListener {
            Log.d(TAG, "Profile icon clicked. Navigating to ProfileActivity.")
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        settingsIcon.setOnClickListener {
            Log.d(TAG, "Settings icon clicked. Navigating to SettingsActivity.")
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        Log.d(TAG, "User ${auth.currentUser?.email} successfully loaded Main Menu.")
    }
}
