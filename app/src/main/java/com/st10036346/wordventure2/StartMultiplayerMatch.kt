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

    // Tracks current player and words
    private var currentPlayer = 1
    private var player1Word: String? = null

    // Default player names
    private var player1Name = "Player 1"
    private var player2Name = "Player 2"

    companion object {
        const val EXTRA_CURRENT_PLAYER = "EXTRA_CURRENT_PLAYER"
        const val EXTRA_PLAYER_1_WORD = "EXTRA_PLAYER_1_WORD"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartMultiplayerMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get player names from previous screen
        player1Name = intent.getStringExtra("PLAYER_1_NAME") ?: "Player 1"
        player2Name = intent.getStringExtra("PLAYER_2_NAME") ?: "Player 2"

        // Get current player and word from intent
        currentPlayer = intent.getIntExtra(EXTRA_CURRENT_PLAYER, 1)
        player1Word = intent.getStringExtra(EXTRA_PLAYER_1_WORD)

        // Update label for whose turn it is
        updateTurnIndicator()

        // Show message for whose turn it is
        if (currentPlayer == 2) {
            Toast.makeText(this, "Player 2, choose your word!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Player 1, choose your word!", Toast.LENGTH_SHORT).show()
        }

        // Show loading animation
        binding.loadingIndicator.visibility = View.VISIBLE

        // Open profile screen
        binding.profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Open settings screen
        binding.settingsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Go back to player name entry screen
        binding.backIcon.setOnClickListener {
            startActivity(Intent(this, EnterPlayerNamesActivity::class.java))
            finish()
        }

        // Get random words for selection
        fetchWords()
    }

    // Updates which player's turn it is
    private fun updateTurnIndicator() {
        val turnText = if (currentPlayer == 1) {
            "$player1Name's Turn"
        } else {
            "$player2Name's Turn"
        }
        binding.playerTurnIndicator.text = turnText
    }

    // Fetches random words using API
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

        // Wait until all words are fetched
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

    // Shows error if word fetch fails
    private fun handleFetchFailure() {
        Toast.makeText(this, "Failed to load words. Please check your connection and try again.", Toast.LENGTH_LONG).show()
    }

    // Displays all fetched words
    private fun displayWords(words: List<String>) {
        binding.wordsContainer.removeAllViews()
        for (word in words) {
            val wordLayout = createWordView(word)
            binding.wordsContainer.addView(wordLayout)
        }
    }

    // Creates clickable word box
    private fun createWordView(word: String): View {
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

        // Create letter tiles
        for (letter in word) {
            val letterTile = TextView(this).apply {
                text = letter.toString()
                textSize = 28f
                setTextColor(Color.parseColor("#051646"))
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setBackgroundColor(Color.parseColor("#9CCC65"))
                layoutParams = LinearLayout.LayoutParams(120, 120).also {
                    it.setMargins(8, 8, 8, 8)
                }
            }
            wordContainer.addView(letterTile)
        }

        // Handles when a word is selected
        wordContainer.setOnClickListener {
            if (currentPlayer == 1) {
                // Switch to player 2 selection
                val intent = Intent(this, StartMultiplayerMatch::class.java).apply {
                    putExtra(EXTRA_CURRENT_PLAYER, 2)
                    putExtra(EXTRA_PLAYER_1_WORD, word)
                    putExtra("PLAYER_1_NAME", player1Name)
                    putExtra("PLAYER_2_NAME", player2Name)
                }
                startActivity(intent)
                finish()
            } else {
                // Player 2 chose word, start match
                val player2Word = word
                val gameIntent = Intent(this, LocalMatch::class.java).apply {
                    putExtra("PLAYER_1_TARGET_WORD", player2Word)
                    putExtra("PLAYER_2_TARGET_WORD", player1Word)
                    putExtra("PLAYER_1_NAME", player1Name)
                    putExtra("PLAYER_2_NAME", player2Name)
                }
                startActivity(gameIntent)
                finish()
            }
        }
        return wordContainer
    }
}
