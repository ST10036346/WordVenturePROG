package com.st10036346.wordventure2

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.st10036346.wordventure2.databinding.ActivityStartMultiplayerMatchBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Collections
import java.util.concurrent.CountDownLatch

class StartMultiplayerMatch : AppCompatActivity() {

    private lateinit var binding: ActivityStartMultiplayerMatchBinding

    // --- NEW: State Management Variables ---
    private var currentPlayer = 1
    private var player1Word: String? = null

    companion object {
        const val EXTRA_CURRENT_PLAYER = "EXTRA_CURRENT_PLAYER"
        const val EXTRA_PLAYER_1_WORD = "EXTRA_PLAYER_1_WORD"
    }
    // ------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartMultiplayerMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- NEW: Check intent for current player state ---
        currentPlayer = intent.getIntExtra(EXTRA_CURRENT_PLAYER, 1) // Default to Player 1
        player1Word = intent.getStringExtra(EXTRA_PLAYER_1_WORD)

        if (currentPlayer == 2) {
            Toast.makeText(this, "Player 2, choose your word!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Player 1, choose your word!", Toast.LENGTH_SHORT).show()
        }
        // -----------------------------------------------

        binding.loadingIndicator.visibility = View.VISIBLE

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
        fetchWords()
    }

    private fun fetchWords() {
        val wordsToFetch = 4
        val fetchedWords = Collections.synchronizedList(mutableListOf<String>())
        val latch = CountDownLatch(wordsToFetch)

        for (i in 1..wordsToFetch) {
            RetrofitClient.instance.getRandomWord().enqueue(object : Callback<WordResponse> {
                override fun onResponse(call: Call<WordResponse>, response: Response<WordResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.word?.let {
                            if (!fetchedWords.contains(it.uppercase())) {
                                fetchedWords.add(it.uppercase())
                            }
                        }
                    }
                    latch.countDown()
                }

                override fun onFailure(call: Call<WordResponse>, t: Throwable) {
                    latch.countDown()
                }
            })
        }

        Thread {
            latch.await()
            runOnUiThread {
                binding.loadingIndicator.visibility = View.GONE
                if (fetchedWords.isNotEmpty()) {
                    displayWords(fetchedWords)
                } else {
                    handleFetchFailure()
                }
            }
        }.start()
    }

    private fun handleFetchFailure() {
        Toast.makeText(this, "Failed to load words. Please check your connection and try again.", Toast.LENGTH_LONG).show()
    }

    private fun displayWords(words: List<String>) {
        binding.wordsContainer.removeAllViews()
        for (word in words) {
            val wordLayout = createWordView(word)
            binding.wordsContainer.addView(wordLayout)
        }
    }

    private fun createWordView(word: String): View {
        // (This function remains the same as before)
        val wordContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(0, 0, 0, 24)
            }
            setBackgroundResource(R.drawable.rounded_purple_background)
            setPadding(16, 16, 16, 16)
        }

        for (letter in word) {
            val letterTile = TextView(this).apply {
                text = letter.toString()
                textSize = 28f
                setTextColor(Color.parseColor("#051646"))
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setBackgroundColor(Color.parseColor("#9CCC65")) // Light green
                layoutParams = LinearLayout.LayoutParams(120, 120).also {
                    it.setMargins(8, 8, 8, 8)
                }
            }
            wordContainer.addView(letterTile)
        }

        // --- NEW: Updated Click Listener Logic ---
        wordContainer.setOnClickListener {
            if (currentPlayer == 1) {
                // --- Player 1 chose a word ---
                // Relaunch this same activity for Player 2
                val intent = Intent(this, StartMultiplayerMatch::class.java).apply {
                    putExtra(EXTRA_CURRENT_PLAYER, 2) // It's now Player 2's turn
                    putExtra(EXTRA_PLAYER_1_WORD, word)   // Pass Player 1's chosen word
                }
                startActivity(intent)
                finish() // Close Player 1's screen

            } else {
                // --- Player 2 chose a word ---
                val player2Word = word

                // Now we have both words, start the actual game
                val gameIntent = Intent(this, LocalMatch::class.java).apply { // <-- CHANGE THIS to LocalMatch
                    putExtra("PLAYER_1_TARGET_WORD", player2Word) // P1 guesses P2's word
                    putExtra("PLAYER_2_TARGET_WORD", player1Word) // P2 guesses P1's word
                }
                startActivity(gameIntent)
                finish() // Close the word selection screen
            }
        }
        // -----------------------------------------
        return wordContainer
    }
}
