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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

import com.st10036346.wordventure2.databinding.PlayScreenBinding 
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.st10036346.wordventure2.StatsManager

class Play : AppCompatActivity() {

    private lateinit var binding: PlayScreenBinding

    private val rows = 6
    private val cols = 5
    private val tiles = Array(rows) { arrayOfNulls<TextView>(cols) }
    private var currentRow = 0
    private var currentCol = 0
    private var targetWord = ""

    // List of fallback words to use when the API is unreachable
    private val FALLBACK_WORDS = listOf(
        "TABLE", "CHAIR", "DREAM", "LIGHT", "FLUID",
        "SHAKE", "FROST", "GHOST", "BLANK", "STORY",
        "BRICK", "POWER", "QUICK", "VALVE", "YIELD"
    )
    // Key for tracking the index of the last used fallback word
    private val KEY_FALLBACK_INDEX = "fallback_word_index"

    //  LEVEL TRACKING 
    private var currentLevelNumber: Int = 1
    private val BASE_PREFS_NAME = "GameProgress"
    private val KEY_UNLOCKED_LEVEL = "current_level_unlocked"
    // Stores the user ID obtained in onCreate
    private lateinit var currentUserId: String

    private lateinit var statsManager: StatsManager
    private lateinit var auth: FirebaseAuth

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

        binding = PlayScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Initialize Firebase Auth
        auth = Firebase.auth

        // 2. Get the user's UID
        val userId = auth.currentUser?.uid

        // 3. Safety check: Ensure user is logged in
        if (userId == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Store the user ID globally for this activity
        currentUserId = userId

        // 4. Initialize StatsManager correctly with the unique user ID
        statsManager = StatsManager(this, currentUserId)

        // Get the level number passed from the Levels screen
        currentLevelNumber = intent.getIntExtra("LEVEL_NUMBER", 1)
        Toast.makeText(this, "Level $currentLevelNumber Loaded", Toast.LENGTH_SHORT).show()


        // Starts fresh game for each level
        setupBoard()
        setupKeyboard()
        binding.keyboardLayout.visibility = View.INVISIBLE
        fetchWordleWord()

        binding.profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.settingsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Back icon goes back to the Levels screen for this mode
        binding.backIcon.setOnClickListener {
            val intent = Intent(this, Levels::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun saveLevelProgress(levelCompleted: Int) {
        // Append the currentUserId to the preference file name to save progress uniquely
        val prefsNameWithId = "${BASE_PREFS_NAME}_${currentUserId}"
        val prefs = getSharedPreferences(prefsNameWithId, Context.MODE_PRIVATE)
        val nextLevel = levelCompleted + 1

        // Only saves if the completed level is the one that unlocks the next one
        if (nextLevel > prefs.getInt(KEY_UNLOCKED_LEVEL, 1)) {
            prefs.edit().putInt(KEY_UNLOCKED_LEVEL, nextLevel).apply()
            Toast.makeText(this, "Level $levelCompleted complete! Level $nextLevel unlocked.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupBoard() {
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

    // Logic to fall back to proceedWithGuess on network failure
    private fun onEnterPressed() {
        if (currentCol != cols) {
            Toast.makeText(this, "Not enough letters", Toast.LENGTH_SHORT).show()
            return
        }

        val guess = (0 until cols).joinToString("") { j -> tiles[currentRow][j]?.text.toString() }.uppercase(Locale.ROOT)
        val safeContext = this
        val guessBody = WordGuess(guess)

        binding.keyboardLayout.isEnabled = false // Disable keyboard while processing

        // API call to validate word
        RetrofitClient.instance.checkWord(guessBody).enqueue(object : Callback<CheckWordResponse> {
            override fun onResponse(call: Call<CheckWordResponse>, response: Response<CheckWordResponse>) {
                if (response.isSuccessful && response.body()?.valid == true) {
                    // ONLINE SUCCESS: Word is valid, proceed with game logic
                    runOnUiThread {
                        proceedWithGuess(guess)
                    }
                } else {
                    // ONLINE FAILURE: Word is not in the dictionary (or bad API response)
                    Handler(Looper.getMainLooper()).postDelayed({
                        Toast.makeText(applicationContext, "Not in word list (Online Check)", Toast.LENGTH_SHORT).show()
                        binding.keyboardLayout.isEnabled = true
                    }, 50)
                }
            }

            override fun onFailure(call: Call<CheckWordResponse>, t: Throwable) {
                // OFFLINE MODE: Network error occurred. Assume word is valid and proceed.
                android.util.Log.e("API_FAILURE", "Retrofit call failed - Assuming valid word for offline play", t)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(safeContext, "Offline Mode: Guessing without dictionary check.", Toast.LENGTH_SHORT).show()
                    // Proceed with guess, allowing the user to continue the game
                    proceedWithGuess(guess)
                }
            }
        })
    }

    private fun proceedWithGuess(guess: String) {
        // Color the grid row
        colorGridRow(guess, currentRow)

        // Update keyboard colors
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

        // Check for a win
        if (guess.equals(targetWord, ignoreCase = true)) {
            binding.keyboardLayout.isEnabled = false

            // Level progress after a win
            saveLevelProgress(currentLevelNumber)

            Handler(Looper.getMainLooper()).postDelayed({
                showStatsPanel(didWin = true) // Shows win panel
            }, 1000)
            return
        }

        // Moves to the next row or end the game if it's a loss
        currentRow++
        currentCol = 0

        if (currentRow >= rows) {
            binding.keyboardLayout.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({
                showStatsPanel(didWin = false) // Shows loss panel
            }, 1000)
        } else {
            // Re-enables keyboard for the next turn if the game is not over
            binding.keyboardLayout.isEnabled = true
        }
    }

    private fun updateKeyboardAppearance() {
        for ((char, button) in letterButtonMap) {
            val status = keyboardLetterStatus[char] ?: continue
            val color = when (status) { // Determine colour based on status
                LetterStatus.CORRECT -> Color.parseColor("#6AAA64") //Green
                LetterStatus.PRESENT -> Color.parseColor("#C9B458") //Yellow
                LetterStatus.ABSENT -> Color.parseColor("#787C7E") //Grey
            }
            val drawable = button.background
            DrawableCompat.setTint(drawable, color)
            button.setTextColor(Color.WHITE)
        }
    }

    private fun showStatsPanel(didWin: Boolean) {
        binding.keyboardLayout.visibility = View.GONE

        // 1. Set the Title
        if (didWin) {
            binding.statsPanel.statsTitle.text = "LEVEL $currentLevelNumber COMPLETED!" //Stats panel for a win
        } else {
            binding.statsPanel.statsTitle.text = "LEVEL FAILED! RETRY $currentLevelNumber" //Stats panel for a loss
            Toast.makeText(this, "The word was: $targetWord", Toast.LENGTH_LONG).show()
        }

        // 2. HIDE ALL STATS ELEMENTS 
        binding.statsPanel.statsMetricsContainer.visibility = View.GONE
        binding.statsPanel.guessDistributionLabel.visibility = View.GONE
        binding.statsPanel.guessDistributionChartContainer.visibility = View.GONE

        // 3. Set the Button Text
        binding.statsPanel.playMoreButton.text = if (didWin) "PLAY NEXT LEVEL" else "RETRY LEVEL"
        binding.statsPanel.mainMenuButton.text = "BACK TO LEVELS MENU"

        // 4. Set Button Listeners
        binding.statsPanel.playMoreButton.setOnClickListener {
            // If you win, you go to the next level. If you lose, retry the current level
            val nextLevel = if (didWin) currentLevelNumber + 1 else currentLevelNumber
            val intent = Intent(this, Play::class.java).apply {
                putExtra("LEVEL_NUMBER", nextLevel)
            }
            startActivity(intent)
            finish()
        }

        binding.statsPanel.mainMenuButton.setOnClickListener {
            val intent = Intent(this, Levels::class.java) //Navigates to Levels menu
            startActivity(intent)
            finish()
        }

        // 5. Stat updates (Still track in background)
        val guesses = currentRow + 1 //SHows the guesses made
        statsManager.updateStats(didWin, if (didWin) guesses else 0) //Updates overall user stats


        // 6. Show the Panel
        binding.statsPanelContainer.visibility = View.VISIBLE //Makes stats panel visible
        binding.statsPanelContainer.animate().translationY(0f).setDuration(500).start()
    }

    // Logic to use fallback word on API failure
    private fun fetchWordleWord() {
        // Attempt API call first for any level
        RetrofitClient.instance.getRandomWord().enqueue(object : Callback<WordResponse> {
            override fun onResponse(call: Call<WordResponse>, response: Response<WordResponse>) {
                if (response.isSuccessful) {
                    // SUCCESS: Use the word fetched from the API (Online Mode)
                    targetWord = response.body()?.word?.uppercase(Locale.ROOT) ?: "APPLE"
                    binding.keyboardLayout.visibility = View.VISIBLE
                    Toast.makeText(this@Play, "Online Word Loaded.", Toast.LENGTH_SHORT).show()
                } else {
                    // FAILURE: API was reached, but response was bad (Server Error). Use fallback word.
                    useFallbackWord()
                    Toast.makeText(this@Play, "API Error. Using offline word.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<WordResponse>, t: Throwable) {
                // FAILURE: Network error occurred (Offline Mode). Use fallback word.
                android.util.Log.e("API_FAILURE", "Network Error. Using fallback word.", t)
                useFallbackWord()
                Toast.makeText(this@Play, "Network Error. Using offline word.", Toast.LENGTH_LONG).show()
            }
        })
    }

    // Logic to select the next word from the local list
    private fun useFallbackWord() {
        val prefsNameWithId = "${BASE_PREFS_NAME}_${currentUserId}"
        val prefs = getSharedPreferences(prefsNameWithId, Context.MODE_PRIVATE)

        // 1. Get the current index. Default is 0.
        val currentIndex = prefs.getInt(KEY_FALLBACK_INDEX, 0)

        // 2. Select the word using the current index.
        // Use the modulus operator (%) to loop back to the start if the list is exhausted.
        val wordIndexToUse = currentIndex % FALLBACK_WORDS.size
        targetWord = FALLBACK_WORDS[wordIndexToUse].uppercase(Locale.ROOT)

        // 3. Save the index for the NEXT word
        val nextIndex = (currentIndex + 1) % FALLBACK_WORDS.size
        prefs.edit().putInt(KEY_FALLBACK_INDEX, nextIndex).apply()

        // Show the keyboard
        binding.keyboardLayout.visibility = View.VISIBLE
    }

    //Keyboard setup
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

    private fun colorGridRow(guess: String, row: Int) {
        val availableTargetLetters = targetWord.toMutableList()
        val tileColors = Array(cols) { Color.parseColor("#787C7E") } // Grey

        //Checks for correct letter and shows Green
        for (j in 0 until cols) {
            if (guess.length > j && targetWord.length > j && guess[j] == targetWord[j]) {
                tileColors[j] = Color.parseColor("#6AAA64") // Green
                availableTargetLetters[j] = ' '
            }
        }
        for (j in 0 until cols) {
            //If the tile was not Green, it checks for Yellow
            if (tileColors[j] != Color.parseColor("#6AAA64"))  {
                if (guess.length > j && availableTargetLetters.contains(guess[j])) {
                    tileColors[j] = Color.parseColor("#C9B458") // Yellow
                    val index = availableTargetLetters.indexOf(guess[j])
                    if (index != -1) {
                        availableTargetLetters[index] = ' '
                    }
                }
            }
        }
        //Applies colour to the tiles
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

        //Iteration for 6 possible guesses
        for (i in 0 until guessDist.size.coerceAtMost(6)) {
            val count = guessDist[i]
            val guessNumber = i + 1

            //Calculate bar length ratio
            val barPercentage = count.toFloat() / maxCount.toFloat()
            val barWeight = barPercentage * 0.7f

            //Layout for the entire row
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

            // Guess Number Label
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

            // Bar Container
            val barContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f)
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START
            }

            // Actual coloured bar
            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, BAR_HEIGHT_DP.dpToPx(), barWeight.coerceAtLeast(0.01f))
                setBackgroundColor(Color.parseColor("#8058E5"))
            }
            barContainer.addView(bar)

            val spacer = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, BAR_HEIGHT_DP.dpToPx(), (0.8f - barWeight).coerceAtLeast(0.01f))
            }
            barContainer.addView(spacer)


            //Count Label
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
