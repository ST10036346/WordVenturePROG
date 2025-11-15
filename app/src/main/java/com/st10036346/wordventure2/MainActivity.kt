package com.st10036346.wordventure2

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.util.Log
class MainActivity : AppCompatActivity() {

    // Constants to match the keys used in SettingsActivity
    private val PREFS_NAME = "GameSettings"
    private val KEY_SOUND = "isSoundEnabled"

    // Register the activity result launcher for the permission request
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("FCM_PERMISSION", "Notification permission granted.")
        } else {
            Log.d("FCM_PERMISSION", "Notification permission denied. User will not receive push notifications.")
        }
    }
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


        askNotificationPermission()

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

    private fun askNotificationPermission() {
        // This is only needed for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // Request the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

/**
 * GeeksforGeeks, 2025.
 * How to Build a Wordle Game in Android?
 * Available at: https://www.geeksforgeeks.org/android/how-to-build-a-wordle-game-application-in-android/
 */