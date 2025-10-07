package com.st10036346.wordventure2

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
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.setMargins
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.st10036346.wordventure2.databinding.ActivityDaily1Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class Daily1 : AppCompatActivity() {

    private lateinit var binding: ActivityDaily1Binding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var statsManager: StatsManager
    private lateinit var currentUserId: String // Stores logged-in user ID

    // --- Game state variables ---
    private val rows = 6
    private val cols = 5
    private val tiles = Array(rows) { arrayOfNulls<TextView>(cols) }
    private var currentRow = 0
    private var currentCol = 0
    private var targetWord = ""
    private var isReplayMode = false

    // --- Keyboard state ---
    private enum class LetterStatus { CORRECT, PRESENT, ABSENT }

    private val keyboardLetterStatus = mutableMapOf<Char, LetterStatus>()
    private lateinit var letterButtonMap: Map<Char, Button>
    private var isGameReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaily1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase and user data
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Redirect if no user is logged in
        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, MainMenu::class.java))
            finish()
            return
        }

        currentUserId = user.uid
        statsManager = StatsManager(this, currentUserId)

        // Check if daily challenge was played today
        checkIfPlayedTodayAndFetchWord()

        // Navigation icons
        binding.backIcon.setOnClickListener { finish() }
        binding.profileIcon.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ProfileActivity::class.java
                )
            )
        }
        binding.settingsIcon.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    SettingsActivity::class.java
                )
            )
        }
    }

    // Checks if the player already played today
    private fun checkIfPlayedTodayAndFetchWord() {
        val userId = currentUserId
        val prefs = getSharedPreferences("DailyChallenge_$userId", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastPlayDate = prefs.getString("lastPlayDate", "")
        val hasSavedState = prefs.getBoolean("hasSavedState", true)

        // Block if game was completed today
        if (lastPlayDate == todayStr && !hasSavedState) {
            Toast.makeText(
                this,
                "You have already completed the daily challenge today!",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        // Reset old game data
        if (lastPlayDate != todayStr) prefs.edit().clear().apply()
        isReplayMode = false
        fetchDailyWordFromFirestore(todayStr)
    }

    override fun onPause() {
        super.onPause()
        // Save progress when leaving the screen
        if (isGameReady && !isReplayMode) saveCurrentProgress()
    }

    // Saves current board and keyboard state
    private fun saveCurrentProgress() {
        val userId = auth.currentUser!!.uid
        val prefs = getSharedPreferences("DailyChallenge_$userId", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val gridState = (0 until rows).joinToString(";") { i ->
            (0 until cols).joinToString(",") { j -> tiles[i][j]?.text?.toString() ?: "" }
        }

        val keyboardState =
            keyboardLetterStatus.map { "${it.key}:${it.value.name}" }.joinToString(";")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        editor.putBoolean("hasSavedState", true)
        editor.putString("lastPlayDate", today)
        editor.putString("savedTargetWord", targetWord)
        editor.putInt("currentRow", currentRow)
        editor.putString("savedGridState", gridState)
        editor.putString("savedKeyboardState", keyboardState)
        editor.apply()
    }

    // Loads an unfinished game
    private fun loadInProgressGame() {
        val userId = auth.currentUser!!.uid
        val prefs = getSharedPreferences("DailyChallenge_$userId", Context.MODE_PRIVATE)
        targetWord = prefs.getString("savedTargetWord", "") ?: ""
        currentRow = prefs.getInt("currentRow", 0)

        if (targetWord.isEmpty()) {
            fetchDailyWordFromFirestore(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                    Date()
                )
            )
            return
        }

        finishGameSetup()

        // Restore saved letters and colors
        binding.boardGrid.post {
            val savedKeyboardState = prefs.getString("savedKeyboardState", "") ?: ""
            savedKeyboardState.split(";").forEach {
                if (it.contains(":")) {
                    val parts = it.split(":")
                    val char = parts[0].first()
                    val status = LetterStatus.valueOf(parts[1])
                    keyboardLetterStatus[char] = status
                }
            }
            updateKeyboardAppearance()

            val savedGridState = prefs.getString("savedGridState", "") ?: ""
            val savedRows = savedGridState.split(";")
            savedRows.forEachIndexed { i, rowString ->
                if (i < currentRow) {
                    val letters = rowString.split(",")
                    val guess = letters.joinToString("")
                    if (guess.length == cols) {
                        letters.forEachIndexed { j, letter -> tiles[i][j]?.text = letter }
                        colorGridRow(guess, i)
                    }
                }
            }
        }
    }

    // Gets the daily word from Firestore or generates one
    private fun fetchDailyWordFromFirestore(dateStr: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val doc = db.collection("daily_words").document(dateStr).get().await()
                targetWord = if (doc.exists()) doc.getString("word")?.uppercase() ?: "FALLBACK"
                else {
                    generateAndSetNewDailyWord(dateStr)
                    targetWord
                }
                withContext(Dispatchers.Main) { finishGameSetup() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Daily1,
                        "Error fetching daily word. Using fallback.",
                        Toast.LENGTH_LONG
                    ).show()
                    targetWord = "NETWORK"
                    finishGameSetup()
                }
            }
        }
    }

    // Creates a new daily word if none exists
    private suspend fun generateAndSetNewDailyWord(dateStr: String) {
        val dailyWordDocRef = db.collection("daily_words").document(dateStr)
        val metadataRef = db.collection("daily_words").document("metadata")

        db.runTransaction { transaction ->
            val metadataDoc = transaction.get(metadataRef)
            val usedWords = metadataDoc.get("usedWords") as? List<String> ?: listOf()

            var newWord: String? = null
            var attempts = 0
            while (newWord == null && attempts < 10) {
                val response = RetrofitClient.instance.getRandomWord().execute()
                val fetchedWord = response.body()?.word?.uppercase()
                if (fetchedWord != null && fetchedWord.length == 5 && !usedWords.contains(
                        fetchedWord
                    )
                ) {
                    newWord = fetchedWord
                }
                attempts++
            }

            targetWord = newWord ?: throw Exception("Failed to fetch unique word.")
            transaction.set(dailyWordDocRef, mapOf("word" to targetWord))
            transaction.set(metadataRef, mapOf("usedWords" to (usedWords + targetWord)))
            null
        }.await()
    }

    // Sets up board and keyboard
    private fun finishGameSetup() {
        if (targetWord.length == 5) {
            setupBoard()
            if (!isReplayMode) setupKeyboard()
            binding.keyboardLayout.visibility = if (isReplayMode) View.GONE else View.VISIBLE
        } else {
            Toast.makeText(this, "Failed to initialize game board.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // Builds the 6x5 letter grid
    private fun setupBoard() {
        binding.boardGrid.removeAllViews()
        val marginSize = resources.getDimensionPixelSize(R.dimen.tile_margin)
        binding.boardGrid.post {
            val gridWidth = binding.boardGrid.width
            val tileSize = (gridWidth - (marginSize * 2) * cols) / cols
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    val tile = TextView(this@Daily1).apply {
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = tileSize
                            height = tileSize
                            setMargins(marginSize)
                        }
                        setBackgroundResource(R.drawable.tile_border)
                        gravity = Gravity.CENTER
                        setTextColor(Color.BLACK)
                        textSize = 32f
                        isAllCaps = true
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    binding.boardGrid.addView(tile)
                    tiles[i][j] = tile
                }
            }
            if (!isReplayMode) isGameReady = true
        }
    }

    // Sets up the on-screen keyboard
    private fun setupKeyboard() {
        letterButtonMap = mapOf(
            'Q' to binding.buttonQ,
            'W' to binding.buttonW,
            'E' to binding.buttonE,
            'R' to binding.buttonR,
            'T' to binding.buttonT,
            'Y' to binding.buttonY,
            'U' to binding.buttonU,
            'I' to binding.buttonI,
            'O' to binding.buttonO,
            'P' to binding.buttonP,
            'A' to binding.buttonA,
            'S' to binding.buttonS,
            'D' to binding.buttonD,
            'F' to binding.buttonF,
            'G' to binding.buttonG,
            'H' to binding.buttonH,
            'J' to binding.buttonJ,
            'K' to binding.buttonK,
            'L' to binding.buttonL,
            'Z' to binding.buttonZ,
            'X' to binding.buttonX,
            'C' to binding.buttonC,
            'V' to binding.buttonV,
            'B' to binding.buttonB,
            'N' to binding.buttonN,
            'M' to binding.buttonM
        )
        for ((letter, button) in letterButtonMap) button.setOnClickListener { onLetterPressed(letter) }
        binding.buttonEnter.setOnClickListener { onEnterPressed() }
        binding.buttonDelete.setOnClickListener { onBackPressedCustom() }
    }

    // Adds a letter to the current guess
    private fun onLetterPressed(letter: Char) {
        if (!isGameReady) return
        if (currentCol < cols) {
            tiles[currentRow][currentCol]?.text = letter.toString()
            currentCol++
        }
    }

    // Deletes a letter
    private fun onBackPressedCustom() {
        if (!isGameReady) return
        if (currentCol > 0) {
            currentCol--
            tiles[currentRow][currentCol]?.text = ""
        }
    }

    // Handles Enter button press
    private fun onEnterPressed() {
        if (!isGameReady) return
        if (currentCol != cols) {
            Toast.makeText(this, "Not enough letters", Toast.LENGTH_SHORT).show()
            return
        }
        val guess = (0 until cols).joinToString("") { j -> tiles[currentRow][j]?.text.toString() }
            .uppercase()
        binding.keyboardLayout.isEnabled = false

        RetrofitClient.instance.checkWord(WordGuess(guess))
            .enqueue(object : Callback<CheckWordResponse> {
                override fun onResponse(
                    call: Call<CheckWordResponse>,
                    response: Response<CheckWordResponse>
                ) {
                    if (response.isSuccessful && response.body()?.valid == true) runOnUiThread {
                        proceedWithGuess(
                            guess
                        )
                    }
                    else {
                        Toast.makeText(applicationContext, "Not in word list", Toast.LENGTH_SHORT)
                            .show()
                        binding.keyboardLayout.isEnabled = true
                    }
                }

                override fun onFailure(call: Call<CheckWordResponse>, t: Throwable) {
                    Toast.makeText(this@Daily1, "Could not verify word", Toast.LENGTH_SHORT).show()
                    binding.keyboardLayout.isEnabled = true
                }
            })
    }

    // Evaluates and colors the guessed word
    private fun proceedWithGuess(guess: String) {
        colorGridRow(guess, currentRow)
        updateKeyboardLetterStatus(guess)
        updateKeyboardAppearance()

        if (guess.equals(targetWord, ignoreCase = true)) {
            binding.keyboardLayout.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({ showStatsPanel(true) }, 1000)
            return
        }

        currentRow++
        currentCol = 0

        if (currentRow >= rows) {
            binding.keyboardLayout.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({ showStatsPanel(false) }, 1000)
        } else binding.keyboardLayout.isEnabled = true
    }

    // Updates keyboard letter color states
    private fun updateKeyboardLetterStatus(guess: String) {
        for (j in 0 until cols) {
            val letter = guess[j]
            when {
                letter == targetWord[j] -> keyboardLetterStatus[letter] = LetterStatus.CORRECT
                targetWord.contains(letter) -> if (keyboardLetterStatus[letter] != LetterStatus.CORRECT)
                    keyboardLetterStatus[letter] = LetterStatus.PRESENT

                else -> if (!keyboardLetterStatus.containsKey(letter))
                    keyboardLetterStatus[letter] = LetterStatus.ABSENT
            }
        }
    }

    // Refreshes keyboard UI colors
    private fun updateKeyboardAppearance() {
        for ((char, button) in letterButtonMap) {
            val status = keyboardLetterStatus[char] ?: continue
            val color = when (status) {
                LetterStatus.CORRECT -> Color.parseColor("#6AAA64")
                LetterStatus.PRESENT -> Color.parseColor("#C9B458")
                LetterStatus.ABSENT -> Color.parseColor("#787C7E")
            }
            DrawableCompat.setTint(button.background, color)
            button.setTextColor(Color.WHITE)
        }
    }

    // Displays end-game stats and results
    private fun showStatsPanel(didWin: Boolean) {
        binding.keyboardLayout.visibility = View.GONE
        binding.statsPanel.statsTitle.text = if (didWin) "CONGRATULATIONS!" else "NEXT TIME!"
        if (!didWin) Toast.makeText(this, "The word was: $targetWord", Toast.LENGTH_LONG).show()

        if (!isReplayMode) {
            val guesses = currentRow + 1
            statsManager.updateStats(didWin, if (didWin) guesses else 0)
            saveCompletedGame(didWin)
        }

        val stats = statsManager.getStats()
        val s = binding.statsPanel
        s.gamesPlayedValue.text = stats.gamesPlayed.toString()
        s.winStreakValue.text = stats.winStreak.toString()
        s.maxStreakValue.text = stats.maxStreak.toString()
        updateStatsPanelGraph(s.guessDistributionChartContainer, stats.guessDistribution)
        binding.statsPanelContainer.visibility = View.VISIBLE
        binding.statsPanel.mainMenuButton.setOnClickListener {
            startActivity(Intent(this, MainMenu::class.java))
            finish()
        }
    }

    // Saves completed game data
    private fun saveCompletedGame(didWin: Boolean) {
        val prefs = getSharedPreferences("DailyChallenge_$currentUserId", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("hasSavedState", false)
        editor.remove("savedKeyboardState")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val gridState = (0..currentRow).joinToString(";") { i ->
            (0 until cols).joinToString(",") { j -> tiles[i][j]?.text.toString() }
        }
        editor.putString("lastPlayDate", today)
        editor.putString("savedGridState", gridState)
        editor.putString("savedTargetWord", targetWord)
        editor.putBoolean("didWin", didWin)
        editor.apply()
    }

    // Colors grid tiles after a guess
    private fun colorGridRow(guess: String, row: Int) {
        val availableTargetLetters = targetWord.toMutableList()
        val tileColors = Array(cols) { Color.parseColor("#787C7E") }

        for (j in 0 until cols)
            if (guess[j] == targetWord[j]) {
                tileColors[j] = Color.parseColor("#6AAA64")
                availableTargetLetters[j] = ' '
            }

        for (j in 0 until cols)
            if (tileColors[j] != Color.parseColor("#6AAA64") && availableTargetLetters.contains(
                    guess[j]
                )
            ) {
                tileColors[j] = Color.parseColor("#C9B458")
                availableTargetLetters[availableTargetLetters.indexOf(guess[j])] = ' '
            }

        for (j in 0 until cols) {
            val tile = tiles[row][j]
            val drawable = tile?.background?.mutate()
            drawable?.let { DrawableCompat.setTint(it, tileColors[j]) }
            tile?.background = drawable
            tile?.setTextColor(Color.WHITE)
        }
    }

    // Builds the stats graph
    private fun updateStatsPanelGraph(container: LinearLayout, guessDist: IntArray) {
        val maxCount = guessDist.maxOrNull()?.coerceAtLeast(1) ?: 1
        container.removeAllViews()
        for (i in 0 until guessDist.size.coerceAtMost(6)) {
            val count = guessDist[i]
            val label = TextView(this).apply { text = "${i + 1}:" }
            val value = TextView(this).apply { text = count.toString() }
            val barLayout = LinearLayout(this)
            barLayout.addView(label)
            barLayout.addView(value)
            container.addView(barLayout)
        }
    }

}

