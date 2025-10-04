package com.st10036346.wordventure2

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.st10036346.wordventure2.databinding.ActivityLanguageBinding

class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show "Coming Soon" message
        Toast.makeText(this, "Language settings feature coming soon!", Toast.LENGTH_LONG).show()

        try {
            binding.backIcon.setOnClickListener {
                finish() // Close the current activity and return to the previous one
            }
        } catch (e: Exception) {
        }
    }
}
