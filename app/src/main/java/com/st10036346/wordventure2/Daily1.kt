package com.st10036346.wordventure2

import kotlin.text.uppercase

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.st10036346.wordventure2.databinding.ActivityDaily1Binding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.jvm.java

class Daily1 : AppCompatActivity() {

    private lateinit var binding: ActivityDaily1Binding

    private val rows = 6
    private val cols = 5
    private val tiles = Array(rows) { arrayOfNulls<TextView>(cols) }
    private var currentRow = 0
    private var currentCol = 0
    private var targetWord = "" // Will be fetched from API

    // --- Keyboard State Management ---
    private enum class LetterStatus {
        CORRECT, // Green
        PRESENT, // Yellow
        ABSENT   // Gray
    }
    private val keyboardLetterStatus = mutableMapOf<Char, LetterStatus>()
    private lateinit var letterButtonMap: Map<Char, Button>
    private var isReplayMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaily1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        isReplayMode = intent.getBooleanExtra("IS_REPLAY", false)

        // --- CORRECTED SETUP LOGIC ---
        // The if/else block now correctly handles all setup scenarios without duplication.
        if (isReplayMode) {
            // SCENARIO 1: Replay a completed game
            loadAndReplayGame()
        } else {
            // SCENARIO 2: Start a fresh game
            setupBoard()
            setupKeyboard()
            binding.keyboardLayout.visibility = View.INVISIBLE // Hide keyboard until word is ready
            fetchWordleWord()
        }
        // The duplicate calls that were here have been removed.

        binding.profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.settingsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.backIcon.setOnClickListener {
            // Assuming this should go to MainMenu
            val intent = Intent(this, MainMenu::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupBoard() {
        // Clear previous views if any, to be safe
        binding.boardGrid.removeAllViews()
        val defaultTileBg = ContextCompat.getDrawable(this, R.drawable.tile_border)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val tile = TextView(this).apply {
                    layoutParams = android.widget.GridLayout.LayoutParams().apply {
                        width = 150
                        height = 150
                        setMargins(8, 8, 8, 8)
                    }
                    gravity = Gravity.CENTER
                    textSize = 32f
                    setTypeface(null, Typeface.BOLD)
                    background = defaultTileBg
                }
                binding.boardGrid.addView(tile)
                tiles[i][j] = tile
            }
        }
    }

    private fun onLetterPressed(letter: Char) {
        if (currentCol < cols && binding.keyboardLayout.isEnabled) {
            tiles[currentRow][currentCol]?.text = letter.toString()
            currentCol++
        }
    }

    private fun onBackPressedCustom() {
        if (currentCol > 0 && binding.keyboardLayout.isEnabled) {
            currentCol--
            tiles[currentRow][currentCol]?.text = ""
        }
    }

    // --- THIS IS THE FULLY UPDATED onEnterPressed FUNCTION ---
    // In Daily1.kt

    // --- THIS IS THE FULLY UPDATED onEnterPressed FUNCTION ---
    // In Daily1.kt

    // --- THIS IS THE FULLY UPDATED and STABILIZED onEnterPressed FUNCTION ---
    private fun onEnterPressed() {
        if (currentCol != cols) {
            Toast.makeText(this, "Not enough letters", Toast.LENGTH_SHORT).show()
            return
        }

        val guess = (0 until cols).joinToString("") { j -> tiles[currentRow][j]?.text.toString() }.uppercase()
        val safeContext = this
        val guessBody = WordGuess(guess) // Create the body object
        // Disable UI to prevent multiple submissions
        binding.keyboardLayout.isEnabled = false

        // Call the API to check if the word is valid
        RetrofitClient.instance.checkWord(guessBody).enqueue(object : Callback<CheckWordResponse> {
            override fun onResponse(call: Call<CheckWordResponse>, response: Response<CheckWordResponse>) {
                // Check if the network call itself was successful (e.g., status 200 OK)
                if (response.isSuccessful && response.body()?.valid == true) {
                    // Word is VALID and response was successful
                    runOnUiThread {
                        proceedWithGuess(guess)
                    }
                } else {
                    // Word is INVALID or response was unsuccessful (e.g., 404)
                    // Use a Handler to post the UI update safely.
                    Handler(Looper.getMainLooper()).postDelayed({
                        Toast.makeText(applicationContext, "Not in word list", Toast.LENGTH_SHORT).show()
                        binding.keyboardLayout.isEnabled = true // Re-enable keyboard
                    }, 50) // A small 50ms delay is enough to prevent the crash
                }
            }

            override fun onFailure(call: Call<CheckWordResponse>, t: Throwable) {
                // This block runs for network errors (e.g., no internet)
                android.util.Log.e("API_FAILURE", "Retrofit call failed", t)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(safeContext, "Could not verify word", Toast.LENGTH_SHORT).show()
                    binding.keyboardLayout.isEnabled = true // Re-enable keyboard
                }
            }
        })
    }



    // --- NEW HELPER FUNCTION TO CONTAIN LOGIC AFTER VALIDATION ---
    private fun proceedWithGuess(guess: String) {
        // 1. Color the grid row
        colorGridRow(guess, currentRow)

        // 2. Update keyboard colors
        for (j in 0 until cols) {
            val letter = guess[j]
            if (letter == targetWord[j]) {
                keyboardLetterStatus[letter] = LetterStatus.CORRECT
            } else if (targetWord.contains(letter)) {
                if (keyboardLetterStatus[letter] != LetterStatus.CORRECT) {
                    keyboardLetterStatus[letter] = LetterStatus.PRESENT
                }
            } else {
                if (!keyboardLetterStatus.containsKey(letter)) {
                    keyboardLetterStatus[letter] = LetterStatus.ABSENT
                }
            }
        }
        updateKeyboardAppearance()

        // 3. Check for a win
        if (guess.equals(targetWord, ignoreCase = true)) {
            binding.keyboardLayout.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({
                showStatsPanel(didWin = true)
            }, 1000)
            return
        }

        // 4. Move to the next row or end the game if it's a loss
        currentRow++
        currentCol = 0

        if (currentRow >= rows) {
            binding.keyboardLayout.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({
                showStatsPanel(didWin = false)
            }, 1000)
        } else {
            // Re-enable keyboard for the next turn if the game is not over
            binding.keyboardLayout.isEnabled = true
        }
    }

    private fun updateKeyboardAppearance() {
        for ((char, button) in letterButtonMap) {
            val status = keyboardLetterStatus[char] ?: continue
            val color = when (status) {
                LetterStatus.CORRECT -> Color.parseColor("#6AAA64")
                LetterStatus.PRESENT -> Color.parseColor("#C9B458")
                LetterStatus.ABSENT -> Color.parseColor("#787C7E")
            }
            val drawable = button.background
            DrawableCompat.setTint(drawable, color)
            button.setTextColor(Color.WHITE)
        }
    }

    private fun showStatsPanel(didWin: Boolean) {
        binding.keyboardLayout.visibility = View.GONE
        val statsTitle = binding.statsPanel.statsTitle

        if (didWin) {
            statsTitle.text = "CONGRATULATIONS!"
        } else {
            statsTitle.text = "NEXT TIME!"
            Toast.makeText(this, "The word was: $targetWord", Toast.LENGTH_LONG).show()
        }
        binding.statsPanelContainer.visibility = View.VISIBLE
        binding.statsPanelContainer.animate().translationY(0f).setDuration(500).start()
        if (!isReplayMode) {
            saveCompletedGame(didWin)
        }
    }

    private fun fetchWordleWord() {
        RetrofitClient.instance.getRandomWord().enqueue(object : Callback<WordResponse> {
            override fun onResponse(call: Call<WordResponse>, response: Response<WordResponse>) {
                if (response.isSuccessful) {
                    targetWord = response.body()?.word?.uppercase() ?: "APPLE"
                    binding.keyboardLayout.visibility = View.VISIBLE
                } else {
                    targetWord = "ERROR"
                    Toast.makeText(this@Daily1, "Failed to get a word.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<WordResponse>, t: Throwable) {
                targetWord = "LOCAL" // Fallback for network failure
                binding.keyboardLayout.visibility = View.VISIBLE
                Toast.makeText(this@Daily1, "Network Error. Using default word.", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setupKeyboard() {
        letterButtonMap = mapOf(
            'Q' to binding.buttonQ, 'W' to binding.buttonW, 'E' to binding.buttonE,
            'R' to binding.buttonR, 'T' to binding.buttonT, 'Y' to binding.buttonY,
            'U' to binding.buttonU, 'I' to binding.buttonI, 'O' to binding.buttonO,
            'P' to binding.buttonP, 'A' to binding.buttonA, 'S' to binding.buttonS,
            'D' to binding.buttonD, 'F' to binding.buttonF, 'G' to binding.buttonG,
            'H' to binding.buttonH, 'J' to binding.buttonJ, 'K' to binding.buttonK,
            'L' to binding.buttonL, 'Z' to binding.buttonZ, 'X' to binding.buttonX,
            'C' to binding.buttonC, 'V' to binding.buttonV, 'B' to binding.buttonB,
            'N' to binding.buttonN, 'M' to binding.buttonM
        )
        for ((letter, button) in letterButtonMap) { button.setOnClickListener { onLetterPressed(letter) } }
        binding.buttonEnter.setOnClickListener { onEnterPressed() }
        binding.buttonDelete.setOnClickListener { onBackPressedCustom() }
    }

    private fun saveCompletedGame(didWin: Boolean) {
        val prefs = getSharedPreferences("DailyChallenge", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val gridState = (0 until currentRow + 1).joinToString(";") { i ->
            (0 until cols).joinToString(",") { j ->
                tiles[i][j]?.text.toString()
            }
        }
        editor.putString("lastPlayDate", today)
        editor.putString("savedGridState", gridState)
        editor.putString("savedTargetWord", targetWord)
        editor.putBoolean("didWin", didWin)
        editor.apply()
    }

    private fun loadAndReplayGame() {
        val prefs = getSharedPreferences("DailyChallenge", Context.MODE_PRIVATE)
        targetWord = prefs.getString("savedTargetWord", "") ?: ""
        val savedGridState = prefs.getString("savedGridState", "") ?: ""
        val didWin = prefs.getBoolean("didWin", false)

        if (targetWord.isEmpty() || savedGridState.isEmpty()) {
            setupBoard()
            setupKeyboard()
            fetchWordleWord()
            return
        }

        setupBoard()

        val savedRows = savedGridState.split(";")
        savedRows.forEachIndexed { i, rowString ->
            if (i < rows) { // Check to prevent index out of bounds
                currentRow = i
                val letters = rowString.split(",")
                letters.forEachIndexed { j, letter ->
                    if (j < cols) {
                        tiles[i][j]?.text = letter
                    }
                }
                val guess = (0 until cols).joinToString("") { j -> tiles[i][j]?.text.toString() }
                colorGridRow(guess, i)
            }
        }

        showStatsPanel(didWin)
        binding.keyboardLayout.visibility = View.GONE
    }

    private fun colorGridRow(guess: String, row: Int) {
        val availableTargetLetters = targetWord.toMutableList()
        val tileColors = Array(cols) { Color.parseColor("#787C7E") } // Default Gray

        for (j in 0 until cols) {
            if (guess.length > j && targetWord.length > j && guess[j] == targetWord[j]) {
                tileColors[j] = Color.parseColor("#6AAA64") // Green
                availableTargetLetters[j] = ' '
            }
        }
        for (j in 0 until cols) {
            if (tileColors[j] != Color.parseColor("#6AAA64")) {
                if (guess.length > j && availableTargetLetters.contains(guess[j])) {
                    tileColors[j] = Color.parseColor("#C9B458") // Yellow
                    val index = availableTargetLetters.indexOf(guess[j])
                    if (index != -1) {
                        availableTargetLetters[index] = ' '
                    }
                }
            }
        }
        for (j in 0 until cols) {
            if (row < rows && j < cols) {
                tiles[row][j]?.apply {
                    setBackgroundColor(tileColors[j])
                    setTextColor(Color.WHITE)
                }
            }
        }
    }
}
