package com.st10036346.wordventure2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.st10036346.wordventure2.databinding.ActivityMuliplayerBinding

private lateinit var binding: ActivityMuliplayerBinding

class Muliplayer : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_muliplayer)
        binding = ActivityMuliplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Opens profile screen
        binding.profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Opens settings screen
        binding.settingsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Returns to home screen
        binding.bookIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // closes current activity
        }

        // Opens player name entry screen
        binding.localMatchButton.setOnClickListener {
            startActivity(Intent(this, EnterPlayerNamesActivity::class.java))
        }
    }
}
