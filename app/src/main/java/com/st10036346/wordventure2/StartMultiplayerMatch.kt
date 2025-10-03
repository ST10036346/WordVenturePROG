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

class StartMultiplayerMatch : AppCompatActivity() {

    private lateinit var binding: ActivityStartMultiplayerMatchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartMultiplayerMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show loading indicator and fetch words
        binding.loadingIndicator.visibility = View.VISIBLE
        fetchWords()
    }

    private fun fetchWords() {
        // We need to call the API multiple times to get 4 different words.
        val wordsToFetch = 4
        val fetchedWords = mutableListOf<String>()

        for (i in 1..wordsToFetch) {
            RetrofitClient.instance.getRandomWord().enqueue(object : Callback<WordResponse> {
                override fun onResponse(call: Call<WordResponse>, response: Response<WordResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.word?.let { fetchedWords.add(it.uppercase()) }

                        // When all words have been fetched, display them
                        if (fetchedWords.size == wordsToFetch) {
                            binding.loadingIndicator.visibility = View.GONE
                            displayWords(fetchedWords)
                        }
                    } else {
                        handleFetchFailure()
                    }
                }

                override fun onFailure(call: Call<WordResponse>, t: Throwable) {
                    handleFetchFailure()
                }
            })
        }
    }

    private fun handleFetchFailure() {
        binding.loadingIndicator.visibility = View.GONE
        Toast.makeText(this, "Failed to load words. Please try again.", Toast.LENGTH_LONG).show()
    }

    private fun displayWords(words: List<String>) {
        // Clear any previous views in the container
        binding.wordsContainer.removeAllViews()

        // Create a UI element for each word
        for (word in words) {
            val wordLayout = createWordView(word)
            binding.wordsContainer.addView(wordLayout)
        }
    }

    private fun createWordView(word: String): View {
        // Create a horizontal LinearLayout to hold the letter tiles
        val wordContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(0, 0, 0, 24) // Margin between words
            }
            // Set a rounded background
            setBackgroundResource(R.drawable.rounded_purple_background) // You need to create this drawable
            setPadding(16, 16, 16, 16)
        }

        // Create a TextView for each letter in the word
        for (letter in word) {
            val letterTile = TextView(this).apply {
                text = letter.toString()
                textSize = 28f
                setTextColor(Color.parseColor("#051646"))
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setBackgroundColor(Color.parseColor("#9CCC65")) // Light green color
                layoutParams = LinearLayout.LayoutParams(120, 120).also {
                    it.setMargins(8, 8, 8, 8)
                }
            }
            wordContainer.addView(letterTile)
        }

        wordContainer.setOnClickListener {
            // TODO: Handle what happens when a player chooses a word
            Toast.makeText(this, "You chose: $word", Toast.LENGTH_SHORT).show()
        }

        return wordContainer
    }
}
