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

        // --- MUSIC INITIALIZATION LOGIC ---

        // 1. Initialize SharedPreferences
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 2. Retrieve the saved sound state. Default is 'true' (Sound ON).
        val isSoundEnabled = prefs.getBoolean(KEY_SOUND, true)

        // 3. Set the initial volume of the BackgroundMusicService
        // If sound is enabled (true), Muted must be false. Hence, use !isSoundEnabled.
        BackgroundMusicService.setVolume(!isSoundEnabled)

        // 4. Create and Start the service.
        val musicIntent = Intent(this, BackgroundMusicService::class.java)
        startService(musicIntent)

        // --- END MUSIC INITIALIZATION LOGIC ---


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