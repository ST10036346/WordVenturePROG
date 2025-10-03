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

class MainMenu : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "MainMenu"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        auth = Firebase.auth

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

        dailyButton.setOnClickListener {
            Log.d(TAG, "Daily button clicked. Navigating to Daily1.")
            startActivity(Intent(this, Daily1::class.java))
        }

        playButton.setOnClickListener {
            Log.d(TAG, "Play button clicked. Navigating to Daily1.")
            startActivity(Intent(this, Daily1::class.java))
        }

        multiplayerButton.setOnClickListener {
            Toast.makeText(this, "Multiplayer coming soon!", Toast.LENGTH_SHORT).show()
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
