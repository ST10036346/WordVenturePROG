package com.st10036346.wordventure2

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
    // ------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaily1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBoard()
        setupKeyboard() // Initializes letterButtonMap

        // Hide keyboard until word is fetched
        binding.keyboardLayout.visibility = View.INVISIBLE
        fetchWordleWord()

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
        binding.backIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Optional: finish Daily1 so the user can't go back to it
        }
    }

    private fun setupBoard() {
        val defaultTileBg = ContextCompat.getDrawable(this, R.drawable.tile_border)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val tile = TextView(this).apply {
                    layoutParams = android.widget.GridLayout.LayoutParams().apply {
                        width = 230; height = 230; setMargins(2, 2, 2, 2)
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

    private fun onEnterPressed() {
        if (currentCol != cols) {
            Toast.makeText(this, "Not enough letters", Toast.LENGTH_SHORT).show()
            return
        }

        val guess = (0 until cols).joinToString("") { j -> tiles[currentRow][j]?.text.toString() }

        // --- Grid Color Logic ---
        val availableTargetLettersForGrid = targetWord.toMutableList()
        val tileColors = Array(cols) { Color.parseColor("#787C7E") } // Gray
        for (j in 0 until cols) {
            if (guess[j] == targetWord[j]) {
                tileColors[j] = Color.parseColor("#6AAA64") // Green
                availableTargetLettersForGrid[j] = ' '
            }
        }
        for (j in 0 until cols) {
            if (tileColors[j] != Color.parseColor("#6AAA64")) {
                if (availableTargetLettersForGrid.contains(guess[j])) {
                    tileColors[j] = Color.parseColor("#C9B458") // Yellow
                    availableTargetLettersForGrid.indexOf(guess[j]).let { availableTargetLettersForGrid[it] = ' ' }
                }
            }
        }
        for (j in 0 until cols) {
            tiles[currentRow][j]?.apply { setBackgroundColor(tileColors[j]); setTextColor(Color.WHITE) }
        }

        // --- Keyboard Coloring Logic ---
        for (j in 0 until cols) {
            val letter = guess[j]
            if (letter == targetWord[j]) {
                keyboardLetterStatus[letter] = LetterStatus.CORRECT
            } else if (targetWord.contains(letter)) {
                // Only mark as PRESENT if not already CORRECT
                if (keyboardLetterStatus[letter] != LetterStatus.CORRECT) {
                    keyboardLetterStatus[letter] = LetterStatus.PRESENT
                }
            } else {
                // Only mark as ABSENT if we don't know its status yet
                if (!keyboardLetterStatus.containsKey(letter)) {
                    keyboardLetterStatus[letter] = LetterStatus.ABSENT
                }
            }
        }
        updateKeyboardAppearance()

        // --- Win/Loss Check ---
        if (guess.equals(targetWord, ignoreCase = true)) {
            // Delay showing stats panel to let user see winning colors
            Handler(Looper.getMainLooper()).postDelayed({
                showStatsPanel(didWin = true)
            }, 1000)
            return
        }

        currentRow++
        currentCol = 0

        if (currentRow >= rows) {
            // Delay showing stats panel to let user see final guess colors
            Handler(Looper.getMainLooper()).postDelayed({
                showStatsPanel(didWin = false)
            }, 1000)
        }
    }

    // --- New Keyboard Helper Function ---
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
    }

    private fun fetchWordleWord() {
        RetrofitClient.instance.getRandomWord().enqueue(object : Callback<WordResponse> {
            override fun onResponse(call: Call<WordResponse>, response: Response<WordResponse>) {
                binding.keyboardLayout.visibility = View.VISIBLE
                if (response.isSuccessful) {
                    targetWord = response.body()?.word?.uppercase() ?: "APPLE"
                } else {
                    targetWord = "ERROR"
                    Toast.makeText(this@Daily1, "Failed to get a word.", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<WordResponse>, t: Throwable) {
                binding.keyboardLayout.visibility = View.VISIBLE
                targetWord = "LOCAL" // Fallback for network failure
                Toast.makeText(this@Daily1, "Network Error. Using default word.", Toast.LENGTH_LONG).show()
            }
        })
    }

    // --- Updated setupKeyboard() ---
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
}
