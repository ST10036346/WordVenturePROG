package com.st10036346.wordventure2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.st10036346.wordventure2.databinding.ActivityDaily1Binding
import com.st10036346.wordventure2.databinding.ActivityMuliplayerBinding
import com.st10036346.wordventure2.databinding.ActivityStartMultiplayerMatchBinding

private lateinit var binding: ActivityMuliplayerBinding
class Muliplayer : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_muliplayer)
        binding = ActivityMuliplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.profileIcon.setOnClickListener {
            // Create an Intent to start ProfileActivity
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // 2. Settings Icon Click Listener
        binding.settingsIcon.setOnClickListener {
            // Create an Intent to start SettingsActivity
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // You can also add one for the book icon to go home if you wish
        binding.bookIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Optional: finish Daily1 so the user can't go back to it
        }

        val localGameButton = findViewById<Button>(R.id.localMatchButton) // Or use View Binding

        localGameButton.setOnClickListener {
            val intent = Intent(this, StartMultiplayerMatch::class.java)
            startActivity(intent)
        }
    }
}