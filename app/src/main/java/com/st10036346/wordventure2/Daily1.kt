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
import com.google.firebase.firestore.DocumentReference
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
import kotlin.io.path.exists
import kotlin.text.uppercase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class Daily1 : AppCompatActivity() {

    private lateinit var binding: ActivityDaily1Binding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var statsManager: StatsManager
    // Variable to hold the non-null user ID
    private lateinit var currentUserId: String

    // Game State
    private val rows = 6
    private val cols = 5
    private val tiles = Array(rows) { arrayOfNulls<TextView>(cols) }
    private var currentRow = 0
    private var currentCol = 0
    private var targetWord = ""
    private var isReplayMode = false

    // Keyboard State
    private enum class LetterStatus { CORRECT, PRESENT, ABSENT }
    private val keyboardLetterStatus = mutableMapOf<Char, LetterStatus>()
    private lateinit var letterButtonMap: Map<Char, Button>
    private var isGameReady = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaily1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Initialization ---
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Redirect to main menu if no user is logged in
        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, MainMenu::class.java))
            finish()
            return
        }

        // 💡 FIX 1: Retrieve the user ID
        currentUserId = user.uid

        // 💡 FIX 2: Initialize StatsManager correctly with the user ID
        statsManager = StatsManager(this, currentUserId)

        // --- Main Game Flow ---
        checkIfPlayedTodayAndFetchWord()

        // --- UI Listeners ---
        binding.backIcon.setOnClickListener { finish() }
        binding.profileIcon.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        binding.settingsIcon.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    // --- Core Logic: Step 1 - Check local state first ---
    // In Daily1.kt

    private fun checkIfPlayedTodayAndFetchWord() {
        // Use the new class property for the user ID
        val userId = currentUserId
        val prefs = getSharedPreferences("DailyChallenge_$userId", Context.MODE_PRIVATE)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastPlayDate = prefs.getString("lastPlayDate", "")

        // Check for a saved in-progress game from today
        val hasSavedState = prefs.getBoolean("hasSavedState", false)

        if (lastPlayDate == todayStr && !hasSavedState) {
            // Condition 1: User has already FINISHED their game today.
            isReplayMode = true
            loadAndReplayGame()
        } else if (lastPlayDate == todayStr && hasSavedState) {
            // Condition 2: User has an IN-PROGRESS game from today.
            isReplayMode = false
            loadInProgressGame() // We will create this function next.
        } else {
            // Condition 3: User has not played at all today.
            isReplayMode = false
            fetchDailyWordFromFirestore(todayStr)
        }
    }
    // In Daily1.kt

    override fun onPause() {
        super.onPause()
        // Save progress only if the game is ready and not in replay mode.
        if (isGameReady && !isReplayMode) {
            saveCurrentProgress()
        }
    }
// In Daily1.kt

    private fun saveCurrentProgress() {
        val userId = auth.currentUser!!.uid
        val prefs = getSharedPreferences("DailyChallenge_$userId", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Save grid state: e.g., "A,U,D,I,O;S,L,A,T,E;,,,,;"
        val gridState = (0 until rows).joinToString(";") { i ->
            (0 until cols).joinToString(",") { j ->
                tiles[i][j]?.text?.toString() ?: ""
            }
        }

        // Save keyboard state: e.g., "A:ABSENT;U:PRESENT;D:CORRECT;"
        val keyboardState = keyboardLetterStatus.map { "${it.key}:${it.value.name}" }.joinToString(";")

        // --- Save all data to SharedPreferences ---
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        editor.putBoolean("hasSavedState", true) // Mark that there is an in-progress game
        editor.putString("lastPlayDate", today)
        editor.putString("savedTargetWord", targetWord)
        editor.putInt("currentRow", currentRow)
        editor.putString("savedGridState", gridState)
        editor.putString("savedKeyboardState", keyboardState)

        editor.apply() // Save the changes asynchronously
    }


    // In Daily1.kt

    private fun loadInProgressGame() {
        val userId = auth.currentUser!!.uid
        val prefs = getSharedPreferences("DailyChallenge_$userId", Context.MODE_PRIVATE)

        // Load all the necessary data
        targetWord = prefs.getString("savedTargetWord", "") ?: ""
        currentRow = prefs.getInt("currentRow", 0)
        val savedGridState = prefs.getString("savedGridState", "") ?: ""
        val savedKeyboardState = prefs.getString("savedKeyboardState", "") ?: ""

        if (targetWord.isEmpty()) {
            // If something is wrong, just start a fresh game.
            fetchDailyWordFromFirestore(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
            return
        }

        // Now, setup the UI
        finishGameSetup() // This calls setupBoard() and setupKeyboard()

        // Use a post block to ensure the board is ready before we modify it
        binding.boardGrid.post {
            // Restore the keyboard colors first
            savedKeyboardState.split(";").forEach { state ->
                if (state.contains(":")) {
                    val parts = state.split(":")
                    val char = parts[0].first()
                    val status = LetterStatus.valueOf(parts[1])
                    keyboardLetterStatus[char] = status
                }
            }
            updateKeyboardAppearance()

            // --- THIS IS THE FIX ---
            // Restore the grid state, including text AND colors
            val savedRows = savedGridState.split(";")
            savedRows.forEachIndexed { i, rowString ->
                if (i < currentRow) { // Only process completed rows
                    val letters = rowString.split(",")
                    val guess = letters.joinToString("")
                    if (guess.length == cols) {
                        // Re-apply the text to the tiles
                        letters.forEachIndexed { j, letter ->
                            if (j < cols) tiles[i][j]?.text = letter
                        }
                        // Re-color the row based on the guess
                        colorGridRow(guess, i)
                    }
                } else if (i == currentRow) { // Restore text for the current, unsubmitted row
                    val letters = rowString.split(",")
                    var colIdx = 0
                    letters.forEachIndexed { j, letter ->
                        if (j < cols && letter.isNotEmpty()) {
                            tiles[i][j]?.text = letter
                            colIdx++
                        }
                    }
                    currentCol = colIdx // Set the current column to the next empty space
                }
            }
        }
    }



    // --- Core Logic: Step 2 - Fetch word from Firestore ---
    private fun fetchDailyWordFromFirestore(dateStr: String) {
        // Launch a coroutine to do network and database work in the background
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dailyWordDocRef = db.collection("daily_words").document(dateStr)
                val document = dailyWordDocRef.get().await() // .await() is the non-blocking coroutine version

                if (document.exists()) {
                    // The word for today already exists, use it.
                    targetWord = document.getString("word")?.uppercase() ?: "FALLBACK"
                } else {
                    // This is the FIRST player of the day. Their device creates the word.
                    generateAndSetNewDailyWord(dateStr)
                }

                // After all background work is done, switch back to the Main thread to update the UI
                withContext(Dispatchers.Main) {
                    finishGameSetup()
                }

            } catch (e: Exception) {
                // If anything fails (network error, transaction failure), show an error on the Main thread
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Daily1, "Error fetching daily word. Using fallback.", Toast.LENGTH_LONG).show()
                    targetWord = "NETWORK" // Fallback word
                    finishGameSetup()
                }
            }
        }
    }

    // --- Core Logic: Step 3 - Generate word if it doesn't exist for today ---
    private suspend fun generateAndSetNewDailyWord(dateStr: String) {
        // This is a 'suspend' function, meaning it can be paused without blocking a thread.
        // It runs entirely on the background thread (Dispatchers.IO) from where it was called.
        val dailyWordDocRef = db.collection("daily_words").document(dateStr)
        val metadataRef = db.collection("daily_words").document("metadata")

        try {
            db.runTransaction { transaction ->
                val metadataDoc = transaction.get(metadataRef)
                val usedWords = metadataDoc.get("usedWords") as? List<String> ?: listOf()

                var newWord: String? = null
                var attempts = 0
                while (newWord == null && attempts < 10) { // Safety break
                    // .execute() is now SAFE because we are on a background thread
                    val response = RetrofitClient.instance.getRandomWord().execute()
                    val fetchedWord = response.body()?.word?.uppercase()

                    if (fetchedWord != null && fetchedWord.length == 5 && !usedWords.contains(fetchedWord)) {
                        newWord = fetchedWord
                    }
                    attempts++
                }

                if (newWord == null) {
                    // If we couldn't get a unique word, the transaction will fail.
                    throw Exception("Failed to fetch a unique word from API.")
                }

                targetWord = newWord!!
                transaction.set(dailyWordDocRef, hashMapOf("word" to targetWord))
                transaction.set(metadataRef, mapOf("usedWords" to (usedWords + targetWord)))

                null // Transactions require a return value, null is fine here
            }.await() // .await() waits for the entire transaction to complete

        } catch (e: Exception) {
            // If the transaction fails, re-throw the exception so the calling function's catch block can handle it.
            throw e
        }
    }

    private fun finishGameSetup() {
        if (targetWord.length == 5) {
            isGameReady = true
            binding.keyboardLayout.visibility = View.VISIBLE
            setupBoard()
            setupKeyboard()
        } else {
            // This is a failsafe. If we get here, something went wrong with the word fetching.
            Toast.makeText(this, "Failed to initialize game board. Please try again.", Toast.LENGTH_LONG).show()
            // Optionally, navigate the user back or show a retry button.
            finish() // Go back to the previous screen
        }
    }

    // --- All your existing game functions remain the same ---

    private fun setupBoard() {
        binding.boardGrid.removeAllViews()

        // 1. Get the margin value in pixels from dimens.xml.
        val marginSizeInPixels = resources.getDimensionPixelSize(R.dimen.tile_margin)

        // 2. Add a GlobalLayoutListener to wait until the GridLayout has been measured.
        binding.boardGrid.post {
            // This 'post' block runs after the layout has been measured, so getWidth() is reliable.
            val gridWidth = binding.boardGrid.width

            // 3. Calculate the total space taken by margins.
            // Each tile has a left and right margin.
            val totalMarginSpace = (marginSizeInPixels * 2) * cols

            // 4. Subtract the margin space to find the net width available for tiles.
            val netWidthForTiles = gridWidth - totalMarginSpace

            // 5. Calculate the size for a single tile.
            val tileSize = netWidthForTiles / cols

            // Now that we have the correct size, create and add the tiles.
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    val rowSpec = GridLayout.spec(i)
                    val colSpec = GridLayout.spec(j)

                    val params = GridLayout.LayoutParams(rowSpec, colSpec).apply {
                        width = tileSize
                        height = tileSize
                        setMargins(marginSizeInPixels, marginSizeInPixels, marginSizeInPixels, marginSizeInPixels)
                    }

                    val tile = TextView(this@Daily1).apply {
                        layoutParams = params
                        gravity = Gravity.CENTER
                        textSize = 32f
                        setTextColor(Color.BLACK)
                        setTypeface(null, Typeface.BOLD)
                        setBackgroundResource(R.drawable.tile_border)
                    }

                    binding.boardGrid.addView(tile)
                    tiles[i][j] = tile
                }
            }
        }
    }

    private fun setupKeyboard() {
        letterButtonMap = mapOf(
            'Q' to binding.buttonQ, 'W' to binding.buttonW, 'E' to binding.buttonE, 'R' to binding.buttonR, 'T' to binding.buttonT, 'Y' to binding.buttonY,
            'U' to binding.buttonU, 'I' to binding.buttonI, 'O' to binding.buttonO, 'P' to binding.buttonP, 'A' to binding.buttonA, 'S' to binding.buttonS,
            'D' to binding.buttonD, 'F' to binding.buttonF, 'G' to binding.buttonG, 'H' to binding.buttonH, 'J' to binding.buttonJ, 'K' to binding.buttonK,
            'L' to binding.buttonL, 'Z' to binding.buttonZ, 'X' to binding.buttonX, 'C' to binding.buttonC, 'V' to binding.buttonV, 'B' to binding.buttonB,
            'N' to binding.buttonN, 'M' to binding.buttonM
        )
        for ((letter, button) in letterButtonMap) { button.setOnClickListener { onLetterPressed(letter) } }
        binding.buttonEnter.setOnClickListener { onEnterPressed() }
        binding.buttonDelete.setOnClickListener { onBackPressedCustom() }
    }

    private fun onLetterPressed(letter: Char) {
        if (!isGameReady) return

        if (currentCol < cols && binding.keyboardLayout.isEnabled) {
            tiles[currentRow][currentCol]?.text = letter.toString()
            currentCol++
        }
    }

    private fun onBackPressedCustom() {
        if (!isGameReady) return

        if (currentCol > 0 && binding.keyboardLayout.isEnabled) {
            currentCol--
            tiles[currentRow][currentCol]?.text = ""
        }
    }

    private fun onEnterPressed() {
        if (!isGameReady) return
        if (currentCol != cols) {
            Toast.makeText(this, "Not enough letters", Toast.LENGTH_SHORT).show()
            return
        }
        val guess = (0 until cols).joinToString("") { j -> tiles[currentRow][j]?.text.toString() }.uppercase()
        binding.keyboardLayout.isEnabled = false

        // Check if word is valid
        RetrofitClient.instance.checkWord(WordGuess(guess)).enqueue(object : Callback<CheckWordResponse> {
            override fun onResponse(call: Call<CheckWordResponse>, response: Response<CheckWordResponse>) {
                if (response.isSuccessful && response.body()?.valid == true) {
                    runOnUiThread { proceedWithGuess(guess) }
                } else {
                    Toast.makeText(applicationContext, "Not in word list", Toast.LENGTH_SHORT).show()
                    binding.keyboardLayout.isEnabled = true
                }
            }
            override fun onFailure(call: Call<CheckWordResponse>, t: Throwable) {
                Toast.makeText(this@Daily1, "Could not verify word", Toast.LENGTH_SHORT).show()
                binding.keyboardLayout.isEnabled = true
            }
        })
    }

    private fun proceedWithGuess(guess: String) {
        colorGridRow(guess, currentRow)
        updateKeyboardLetterStatus(guess)
        updateKeyboardAppearance()

        if (guess.equals(targetWord, ignoreCase = true)) {
            binding.keyboardLayout.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({ showStatsPanel(didWin = true) }, 1000)
            return
        }

        currentRow++
        currentCol = 0

        if (currentRow >= rows) {
            binding.keyboardLayout.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({ showStatsPanel(didWin = false) }, 1000)
        } else {
            binding.keyboardLayout.isEnabled = true
        }
    }

    private fun updateKeyboardLetterStatus(guess: String) {
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
    }

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

    private fun showStatsPanel(didWin: Boolean) {
        binding.keyboardLayout.visibility = View.GONE
        val statsTitle = binding.statsPanel.statsTitle

        if (didWin) {
            statsTitle.text = "CONGRATULATIONS!"
        } else {
            statsTitle.text = "NEXT TIME!"
            Toast.makeText(this, "The word was: $targetWord", Toast.LENGTH_LONG).show()
        }

        if (!isReplayMode) {
            val guesses = currentRow + 1
            statsManager.updateStats(didWin, if (didWin) guesses else 0)
            saveCompletedGame(didWin)
        }

        val currentStats = statsManager.getStats()
        val stats = binding.statsPanel
        stats.gamesPlayedValue.text = currentStats.gamesPlayed.toString()
        stats.winStreakValue.text = currentStats.winStreak.toString()
        stats.maxStreakValue.text = currentStats.maxStreak.toString()
        updateStatsPanelGraph(stats.guessDistributionChartContainer, currentStats.guessDistribution)
        binding.statsPanelContainer.visibility = View.VISIBLE
        binding.statsPanelContainer.animate().translationY(0f).setDuration(500).start()
        binding.statsPanel.playMoreButton.visibility = View.GONE
        binding.statsPanel.mainMenuButton.setOnClickListener {
            startActivity(Intent(this, MainMenu::class.java))
            finish()
        }
    }

    private fun saveCompletedGame(didWin: Boolean) {
        val userId = currentUserId // Use the class property
        val prefsName = "DailyChallenge_$userId"
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        editor.putBoolean("hasSavedState", false)
        // We can also clear the keyboard state as it's not needed for replay.
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

    private fun loadAndReplayGame() {
        val userId = currentUserId // Use the class property
        val prefsName = "DailyChallenge_$userId"
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        targetWord = prefs.getString("savedTargetWord", "") ?: ""
        val savedGridState = prefs.getString("savedGridState", "") ?: ""
        val didWin = prefs.getBoolean("didWin", false)

        if (targetWord.isEmpty() || savedGridState.isEmpty()) {
            // This case should ideally not be reached if logic is correct, but as a fallback:
            isReplayMode = false
            checkIfPlayedTodayAndFetchWord() // Restart the flow
            return
        }

        setupBoard()
        setupKeyboard() // Important: setup keyboard to color the keys later

        val savedRows = savedGridState.split(";")
        savedRows.forEachIndexed { i, rowString ->
            if (i < rows) {
                val guess = rowString.replace(",", "")
                colorGridRow(guess, i)
                updateKeyboardLetterStatus(guess)
                val letters = rowString.split(",")
                letters.forEachIndexed { j, letter ->
                    if (j < cols) tiles[i][j]?.text = letter
                }
            }
        }

        updateKeyboardAppearance()
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
                    if (index != -1) availableTargetLetters[index] = ' '
                }
            }
        }
        for (j in 0 until cols) {
            if (row < rows && j < cols) {
                val tile = tiles[row][j]
                // Get the tile's background drawable
                val drawable = tile?.background?.mutate()
                // Apply the correct color tint
                drawable?.let {
                    DrawableCompat.setTint(it, tileColors[j])
                    tile.background = it // Re-assign the tinted drawable
                }
                tile?.setTextColor(Color.WHITE)
            }
        }
    }

    private fun updateStatsPanelGraph(container: LinearLayout, guessDist: IntArray) {
        val maxCount = guessDist.maxOrNull()?.coerceAtLeast(1) ?: 1
        container.removeAllViews()
        for (i in 0 until guessDist.size.coerceAtMost(6)) {
            val count = guessDist[i]
            val guessNumber = i + 1
            // Inflate your stats_bar_item.xml here and populate it
            // This is a simplified placeholder, assuming you have a layout for this.
            val barLayout = LinearLayout(this).apply {
                //... setup layout
            }
            val label = TextView(this).apply { text = "$guessNumber:" }
            val value = TextView(this).apply { text = count.toString() }
            barLayout.addView(label)
            barLayout.addView(value)
            container.addView(barLayout)
        }
    }

    // Helper extension function for dp to pixels
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}