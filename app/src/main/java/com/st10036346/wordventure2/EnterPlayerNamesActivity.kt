package com.st10036346.wordventure2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.st10036346.wordventure2.databinding.ActivityEnterPlayerNamesBinding

class EnterPlayerNamesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnterPlayerNamesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnterPlayerNamesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.continueButton.setOnClickListener {
            val player1Name = binding.player1NameInput.text.toString().trim()
            val player2Name = binding.player2NameInput.text.toString().trim()

            // Check if names are entered
            if (player1Name.isNotEmpty() && player2Name.isNotEmpty()) {
                // Create an Intent to start the next activity
                val intent = Intent(this, StartMultiplayerMatch::class.java).apply {
                    // Put the player names as extras
                    putExtra("PLAYER_1_NAME", player1Name)
                    putExtra("PLAYER_2_NAME", player2Name)
                }
                startActivity(intent)
            } else {
                // Show an error if names are missing
                Toast.makeText(this, "Please enter names for both players.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
