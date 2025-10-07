package com.st10036346.wordventure2

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.content.Context
import android.content.SharedPreferences

class MainActivity : AppCompatActivity() {

    // Constants to match the keys used in SettingsActivity
    private val PREFS_NAME = "GameSettings"
    private val KEY_SOUND = "isSoundEnabled"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Retrieves the saved sound state. Default is the sound on
        val isSoundEnabled = prefs.getBoolean(KEY_SOUND, true)

        // Sets the initial volume of the BackgroundMusicService
        BackgroundMusicService.setVolume(!isSoundEnabled)

        // Create and Start the service.
        val musicIntent = Intent(this, BackgroundMusicService::class.java)
        startService(musicIntent)



        val registerButton: Button = findViewById(R.id.register_button)
        val loginButton: Button = findViewById(R.id.login_button)

        registerButton.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }
}

/**
 * GeeksforGeeks, 2025.
 * How to Build a Wordle Game in Android?
 * Available at: https://www.geeksforgeeks.org/android/how-to-build-a-wordle-game-application-in-android/
 */