package com.st10036346.wordventure2

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.st10036346.wordventure2.databinding.ActivityDaily1Binding

class Daily1 : AppCompatActivity() {

    // 1. Declare the binding variable.
    private lateinit var binding: ActivityDaily1Binding

    // 2. Class-level properties for game state.
    private val rows = 6
    private val cols = 5
    private val tiles = Array(rows) { arrayOfNulls<TextView>(cols) }
    private var currentRow = 0 // Tracks number of guesses (0-indexed)
    private var currentCol = 0
    private var targetWord = ""

    // ADDED: StatsManager instance
    private lateinit var statsManager: StatsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 3. Inflate the layout using View Binding and set the content view.
        binding = ActivityDaily1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // ADDED: Initialize StatsManager
        statsManager = StatsManager(this)

        binding.keyboardLayout.visibility = View.INVISIBLE // Use INVISIBLE to keep layout space

        // 4. Call the setup functions.
        fetchWordleWord()
        setupBoard()
        setupKeyboard()
    }

    private fun setupBoard() {
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val tile = TextView(this).apply {
                    layoutParams = android.widget.GridLayout.LayoutParams().apply {
                        width = 230 // Consider moving this to a dimens.xml file
                        height = 230
                        setMargins(2, 2, 2, 2)
                    }
                    text = ""
                    gravity = Gravity.CENTER
                    textSize = 32f
                    setTextColor(Color.BLACK)
                    setTypeface(null, Typeface.BOLD)
                    background = ContextCompat.getDrawable(this@Daily1, R.drawable.tile_border)
                }

                // Use the binding object to access the boardGrid
                binding.boardGrid.addView(tile)
                tiles[i][j] = tile
            }
        }
    }

    private fun onLetterPressed(letter: Char) {
        if (currentCol < cols) {
            tiles[currentRow][currentCol]?.text = letter.toString()
            currentCol++
        }
    }

    private fun onBackPressedCustom() {
        if (currentCol > 0) {
            currentCol--
            tiles[currentRow][currentCol]?.text = ""
        }
    }

    private fun onEnterPressed() {
        if (currentCol != cols) {
            Toast.makeText(this, "Not enough letters", Toast.LENGTH_SHORT).show()
            return
        }

        val guess = (0 until cols).joinToString("") { j ->
            tiles[currentRow][j]?.text.toString()
        }

        val availableTargetLetters = targetWord.toMutableList()
        val tileColors = Array(cols) { Color.parseColor("#787C7E") } // Gray

        // First Pass: Greens
        for (j in 0 until cols) {
            if (guess[j] == targetWord[j]) {
                tileColors[j] = Color.parseColor("#6AAA64") // Green
                availableTargetLetters[j] = ' '
            }
        }

        // Second Pass: Yellows
        for (j in 0 until cols) {
            if (tileColors[j] != Color.parseColor("#6AAA64")) {
                if (availableTargetLetters.contains(guess[j])) {
                    tileColors[j] = Color.parseColor("#C9B458") // Yellow
                    availableTargetLetters[availableTargetLetters.indexOf(guess[j])] = ' '
                }
            }
        }

        // Apply colors
        for (j in 0 until cols) {
            tiles[currentRow][j]?.apply {
                setBackgroundColor(tileColors[j])
                setTextColor(Color.WHITE)
            }
        }

        // Check for win/loss
        if (guess.equals(targetWord, ignoreCase = true)) {
            // currentRow is 0-indexed, so add 1 for the guess count (1-6)
            showStatsPanel(didWin = true, guessesTaken = currentRow + 1)
            return
        }

        currentRow++
        currentCol = 0

        if (currentRow >= rows) {
            // Loss: guessesTaken is 0 (StatsManager handles this)
            showStatsPanel(didWin = false, guessesTaken = 0)
        }
    }

    private fun setupKeyboard() {
        // ... (keyboard setup remains the same) ...
        val buttonMap = mapOf(
            binding.buttonQ to 'Q', binding.buttonW to 'W', binding.buttonE to 'E',
            binding.buttonR to 'R', binding.buttonT to 'T', binding.buttonY to 'Y',
            binding.buttonU to 'U', binding.buttonI to 'I', binding.buttonO to 'O',
            binding.buttonP to 'P', binding.buttonA to 'A', binding.buttonS to 'S',
            binding.buttonD to 'D', binding.buttonF to 'F', binding.buttonG to 'G',
            binding.buttonH to 'H', binding.buttonJ to 'J', binding.buttonK to 'K',
            binding.buttonL to 'L', binding.buttonZ to 'Z', binding.buttonX to 'X',
            binding.buttonC to 'C', binding.buttonV to 'V', binding.buttonB to 'B',
            binding.buttonN to 'N', binding.buttonM to 'M'
        )

        for ((button, letter) in buttonMap) {
            button.setOnClickListener { onLetterPressed(letter) }
        }

        // Set listeners for Enter and Delete using View Binding
        binding.buttonEnter.setOnClickListener { onEnterPressed() }
        binding.buttonDelete.setOnClickListener { onBackPressedCustom() }
    }

    // MODIFIED: Added guessesTaken parameter
    private fun showStatsPanel(didWin: Boolean, guessesTaken: Int) {

        // 1. UPDATE STATS
        statsManager.updateStats(didWin, guessesTaken)

        // 2. RETRIEVE UPDATED STATS
        val stats = statsManager.getStats()

        // Hide the keyboard
        binding.keyboardLayout.visibility = View.GONE

        // 3. POPULATE BASIC STATS
        // Assuming your <include> tag for the stats popup is: <include android:id="@+id/statsPanel" layout="@layout/popup_stats" />
        binding.statsPanel.statsTitle.text = if (didWin) "CONGRATULATIONS!" else "NEXT TIME!"

        binding.statsPanel.gamesPlayedValue.text = stats.gamesPlayed.toString()
        binding.statsPanel.winStreakValue.text = stats.winStreak.toString()
        binding.statsPanel.maxStreakValue.text = stats.maxStreak.toString()

        // Show the correct word on a loss
        if (!didWin) {
            Toast.makeText(this@Daily1, "WORD WAS: $targetWord", Toast.LENGTH_LONG).show()
        }

        // 4. POPULATE GUESS DISTRIBUTION GRAPH
        updateGuessDistributionGraph(stats.guessDistribution)

        // 5. SET BUTTON LISTENERS
        binding.statsPanel.playMoreButton.setOnClickListener {
            // Start a new game by recreating the activity
            recreate()
        }

        binding.statsPanel.mainMenuButton.setOnClickListener {
            finish() // Go back to the previous activity (Main Menu)
        }

        // Animate the panel into view
        binding.statsPanelContainer.visibility = View.VISIBLE
        binding.statsPanelContainer.animate()
            .translationY(0f)
            .setDuration(500)
            .start()
    }

    // NEW FUNCTION: Handles the bar graph drawing using LinearLayout weights
    private fun updateGuessDistributionGraph(guessDist: IntArray) {
        // ASSUMPTION: You've added android:id="@+id/guess_distribution_chart_container" 
        // to a vertical LinearLayout in your popup XML.
        val chartContainer = binding.statsPanel.guessDistributionChartContainer

        // Calculate the maximum count to determine bar scale
        val maxCount = guessDist.maxOrNull()?.coerceAtLeast(1) ?: 1 // Ensure maxCount is at least 1

        chartContainer.removeAllViews() // Clear any old views

        for (i in 0 until guessDist.size) {
            val count = guessDist[i]
            val guessNumber = i + 1

            // Calculate bar weight (relative width)
            val barWeight = if (count > 0) count.toFloat() / maxCount.toFloat() else 0.05f

            // Create a row for the bar (Guess #: [Bar] Count)
            val rowLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 4, 0, 4)
            }

            // 1. Guess Number Label (e.g., '1', '2', etc.)
            val label = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 8 // Small margin after the number
                }
                text = guessNumber.toString()
                textSize = 14f
                setTextColor(Color.parseColor("#051646"))
                setTypeface(null, Typeface.BOLD)
            }

            // 2. Bar Container (takes up the space for the bar and leaves space for the count)
            val barContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f // Takes up all remaining width (excluding the guess number and count)
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.TRANSPARENT)
            }

            // The actual bar whose width is controlled by weight
            val bar = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    30, // Fixed height for the bar
                    // The weight determines the bar's length (up to 90% of the barContainer)
                    barWeight * 0.9f
                ).apply {
                    marginEnd = 4
                }
                setBackgroundColor(Color.parseColor("#8058E5")) // Bar color
                text = ""
            }

            // 3. Count Label (next to the bar)
            val countLabel = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = count.toString()
                textSize = 14f
                setTextColor(Color.parseColor("#051646"))
                setTypeface(null, Typeface.BOLD)
            }

            barContainer.addView(bar)
            rowLayout.addView(label)
            rowLayout.addView(barContainer)
            rowLayout.addView(countLabel)

            chartContainer.addView(rowLayout)
        }
    }

    // This function is not currently used, but is good practice to keep if needed later.
    private fun setTile(row: Int, col: Int, letter: Char, color: Int) {
        tiles[row][col]?.apply {
            text = if (letter == ' ') "" else letter.toString()
            setBackgroundColor(color)
        }
    }

    private fun fetchWordleWord() {
        RetrofitClient.instance.getRandomWord().enqueue(object : retrofit2.Callback<WordResponse> {
            override fun onResponse(call: retrofit2.Call<WordResponse>, response: retrofit2.Response<WordResponse>) {
                binding.keyboardLayout.visibility = View.VISIBLE

                if (response.isSuccessful) {
                    val randomWord = response.body()!!.word
                    targetWord = randomWord.uppercase()

                }
                else {
                    Toast.makeText(this@Daily1, "Error: Could not fetch a word from the server.", Toast.LENGTH_LONG).show()
                    targetWord = "ERROR"
                }
            }

            override fun onFailure(call: retrofit2.Call<WordResponse>, t: Throwable) {
                binding.keyboardLayout.visibility = View.VISIBLE
                Toast.makeText(this@Daily1, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                targetWord = "APPLE" // Set a fallback word on network failure
            }
        })
    }
}