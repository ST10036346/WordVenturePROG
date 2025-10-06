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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

// 💡 CRITICAL FIX 1: Import the correct binding class for 'play_screen.xml'
// The correct binding name is derived from your XML filename: play_screen -> PlayScreenBinding
import com.st10036346.wordventure2.databinding.PlayScreenBinding // <--- NEW CORRECT BINDING IMPORT
// import com.st10036346.wordventure2.databinding.ActivityDaily1Binding // <-- OLD, INCORRECT BINDING IMPORT (REMOVED)

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.st10036346.wordventure2.StatsManager

class Play : AppCompatActivity() {

    // 💡 CRITICAL FIX 2: Change the binding type to the correct class
    private lateinit var binding: PlayScreenBinding // <--- NEW CORRECT BINDING TYPE

    private val rows = 6
    private val cols = 5
    private val tiles = Array(rows) { arrayOfNulls<TextView>(cols) }
    private var currentRow = 0
    private var currentCol = 0
    private var targetWord = ""

    // --- LEVEL TRACKING ---
    private var currentLevelNumber: Int = 1
    private val PREFS_NAME = "GameProgress"
    private val KEY_UNLOCKED_LEVEL = "current_level_unlocked"
    // ----------------------

    private lateinit var statsManager: StatsManager

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

        // 💡 CRITICAL FIX 3: Inflate the correct layout binding
        binding = PlayScreenBinding.inflate(layoutInflater) // <--- NEW CORRECT BINDING INFLATION
        setContentView(binding.root)

        statsManager = StatsManager(this)

        // Get the level number passed from the Levels screen
        currentLevelNumber = intent.getIntExtra("LEVEL_NUMBER", 1)
        Toast.makeText(this, "Level $currentLevelNumber Loaded", Toast.LENGTH_SHORT).show()


        // --- Always start a fresh game for Levels mode ---
        setupBoard()
        setupKeyboard()
        binding.keyboardLayout.visibility = View.INVISIBLE // Assuming this ID exists in play_screen.xml
        fetchWordleWord()
        // -------------------------------------------------

        binding.profileIcon.setOnClickListener { // Assuming this ID exists in play_screen.xml
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.settingsIcon.setOnClickListener { // Assuming this ID exists in play_screen.xml
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Back icon goes back to the Levels screen for this mode
        binding.backIcon.setOnClickListener { // Assuming this ID exists in play_screen.xml
            val intent = Intent(this, Levels::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun saveLevelProgress(levelCompleted: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nextLevel = levelCompleted + 1

        // Only save if the completed level is the one that unlocks the next one
        if (nextLevel > prefs.getInt(KEY_UNLOCKED_LEVEL, 1)) {
            prefs.edit().putInt(KEY_UNLOCKED_LEVEL, nextLevel).apply()
            Toast.makeText(this, "Level $levelCompleted complete! Level $nextLevel unlocked.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupBoard() {
        binding.boardGrid.removeAllViews() // Assuming this ID exists in play_screen.xml
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
        if (currentCol < cols && binding.keyboardLayout.isEnabled) { // Assuming this ID exists in play_screen.xml
            tiles[currentRow][currentCol]?.text = letter.toString()
            currentCol++
        }
    }

    private fun onBackPressedCustom() {
        if (currentCol > 0 && binding.keyboardLayout.isEnabled) { // Assuming this ID exists in play_screen.xml
            currentCol--
            tiles[currentRow][currentCol]?.text = ""
        }
    }

    private fun onEnterPressed() {
        if (currentCol != cols) {
            Toast.makeText(this, "Not enough letters", Toast.LENGTH_SHORT).show()
            return
        }

        val guess = (0 until cols).joinToString("") { j -> tiles[currentRow][j]?.text.toString() }.uppercase()
        val safeContext = this
        val guessBody = WordGuess(guess)

        binding.keyboardLayout.isEnabled = false // Assuming this ID exists in play_screen.xml

        RetrofitClient.instance.checkWord(guessBody).enqueue(object : Callback<CheckWordResponse> {
            override fun onResponse(call: Call<CheckWordResponse>, response: Response<CheckWordResponse>) {
                if (response.isSuccessful && response.body()?.valid == true) {
                    runOnUiThread {
                        proceedWithGuess(guess)
                    }
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        Toast.makeText(applicationContext, "Not in word list", Toast.LENGTH_SHORT).show()
                        binding.keyboardLayout.isEnabled = true // Assuming this ID exists in play_screen.xml
                    }, 50)
                }
            }

            override fun onFailure(call: Call<CheckWordResponse>, t: Throwable) {
                android.util.Log.e("API_FAILURE", "Retrofit call failed", t)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(safeContext, "Could not verify word", Toast.LENGTH_SHORT).show()
                    binding.keyboardLayout.isEnabled = true // Assuming this ID exists in play_screen.xml
                }
            }
        })
    }

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
            binding.keyboardLayout.isEnabled = false // Assuming this ID exists in play_screen.xml

            // --- LEVEL PROGRESS SAVE ON WIN ---
            saveLevelProgress(currentLevelNumber)
            // ----------------------------------

            Handler(Looper.getMainLooper()).postDelayed({
                showStatsPanel(didWin = true) // Show win panel
            }, 1000)
            return
        }

        // 4. Move to the next row or end the game if it's a loss
        currentRow++
        currentCol = 0

        if (currentRow >= rows) {
            binding.keyboardLayout.isEnabled = false // Assuming this ID exists in play_screen.xml
            Handler(Looper.getMainLooper()).postDelayed({
                showStatsPanel(didWin = false) // Show loss panel
            }, 1000)
        } else {
            // Re-enable keyboard for the next turn if the game is not over
            binding.keyboardLayout.isEnabled = true // Assuming this ID exists in play_screen.xml
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
        binding.keyboardLayout.visibility = View.GONE // Assuming this ID exists in play_screen.xml
        binding.statsPanel.statsTitle // Assuming statsPanel is an <include> in play_screen.xml

        if (didWin) {
            binding.statsPanel.statsTitle.text = "LEVEL $currentLevelNumber COMPLETED!"
        } else {
            binding.statsPanel.statsTitle.text = "LEVEL FAILED! RETRY $currentLevelNumber"
            Toast.makeText(this, "The word was: $targetWord", Toast.LENGTH_LONG).show()
        }

        // --- CORRECTED CODE FOR HIDING STATS (Used for Levels) ---

        // 1. Hide the horizontal container holding the 3 main metrics
        binding.statsPanel.statsMetricsContainer.visibility = View.GONE

        // 2. Hide the "Guess Distribution" label
        binding.statsPanel.guessDistributionLabel.visibility = View.GONE

        // 3. Hide the chart container
        binding.statsPanel.guessDistributionChartContainer.visibility = View.GONE
        // ---------------------------------------

        // Change button text for Levels mode
        binding.statsPanel.playMoreButton.text = if (didWin) "PLAY NEXT LEVEL" else "RETRY LEVEL"
        // Note: Using mainMenuButton based on your XML ID
        binding.statsPanel.mainMenuButton.text = "BACK TO LEVELS MENU"

        // Button Listeners
        binding.statsPanel.playMoreButton.setOnClickListener {
            // If win, go to next level. If loss, retry current level.
            val nextLevel = if (didWin) currentLevelNumber + 1 else currentLevelNumber
            val intent = Intent(this, Play::class.java).apply {
                putExtra("LEVEL_NUMBER", nextLevel)
            }
            startActivity(intent)
            finish()
        }

        binding.statsPanel.mainMenuButton.setOnClickListener {
            val intent = Intent(this, Levels::class.java)
            startActivity(intent)
            finish()
        }

        // Stat updates
        val guesses = currentRow + 1
        statsManager.updateStats(didWin, if (didWin) guesses else 0)


        binding.statsPanelContainer.visibility = View.VISIBLE // Assuming this ID exists in play_screen.xml
        binding.statsPanelContainer.animate().translationY(0f).setDuration(500).start() // Assuming this ID exists in play_screen.xml
    }

    private fun fetchWordleWord() {
        RetrofitClient.instance.getRandomWord().enqueue(object : Callback<WordResponse> {
            override fun onResponse(call: Call<WordResponse>, response: Response<WordResponse>) {
                if (response.isSuccessful) {
                    targetWord = response.body()?.word?.uppercase() ?: "APPLE"
                    binding.keyboardLayout.visibility = View.VISIBLE // Assuming this ID exists in play_screen.xml
                } else {
                    targetWord = "ERROR"
                    Toast.makeText(this@Play, "Failed to get a word.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<WordResponse>, t: Throwable) {
                targetWord = "LOCAL" // Fallback for network failure
                binding.keyboardLayout.visibility = View.VISIBLE // Assuming this ID exists in play_screen.xml
                Toast.makeText(this@Play, "Network Error. Using default word.", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setupKeyboard() {
        // Assuming all these button IDs (buttonQ, buttonW, etc.) exist in play_screen.xml
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

    private fun updateStatsPanelGraph(container: LinearLayout, guessDist: IntArray) {
        val BAR_HEIGHT_DP = 18
        val TEXT_COLOR = Color.parseColor("#051646")

        val maxCount = guessDist.maxOrNull()?.coerceAtLeast(1) ?: 1
        container.removeAllViews()

        for (i in 0 until guessDist.size.coerceAtMost(6)) {
            val count = guessDist[i]
            val guessNumber = i + 1

            val barPercentage = count.toFloat() / maxCount.toFloat()
            val barWeight = barPercentage * 0.7f

            val rowLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 4.dpToPx()
                }
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            // 1. Guess Number Label (10% width)
            val label = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.1f)
                text = guessNumber.toString()
                textSize = 12f
                setTextColor(TEXT_COLOR)
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 0, 8, 0)
            }
            rowLayout.addView(label)

            // 2. Bar Container (80% width)
            val barContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f)
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START
            }

            // The actual bar (dynamic width)
            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, BAR_HEIGHT_DP.dpToPx(), barWeight.coerceAtLeast(0.01f))
                setBackgroundColor(Color.parseColor("#8058E5"))
            }
            barContainer.addView(bar)

            // Add a spacer to push the count label to the end
            val spacer = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, BAR_HEIGHT_DP.dpToPx(), (0.8f - barWeight).coerceAtLeast(0.01f))
            }
            barContainer.addView(spacer)


            // 3. Count Label (10% width)
            val countLabel = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.1f)
                text = count.toString()
                textSize = 12f
                setTextColor(TEXT_COLOR)
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                setTypeface(null, Typeface.BOLD)
                setPadding(8, 0, 0, 0)
            }

            rowLayout.addView(barContainer)
            rowLayout.addView(countLabel)

            container.addView(rowLayout)
        }
    }
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}