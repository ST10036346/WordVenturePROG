package com.st10036346.wordventure2

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import androidx.core.graphics.drawable.DrawableCompat
import com.st10036346.wordventure2.databinding.ActivityLocalMatchBinding

// Data class to hold the state of a single tile (its text and color)
data class TileState(val text: String, val backgroundColor: Int, val textColor: Int)

class LocalMatch : AppCompatActivity() {

    private lateinit var binding: ActivityLocalMatchBinding

    // --- Game Board State ---
    private val rows = 6
    private val cols = 5
    private val tiles = Array(rows) { arrayOfNulls<TextView>(cols) }
    private var currentRow = 0
    private var currentCol = 0

    // --- Multiplayer State ---
    private var activePlayer = 1
    private var player1TargetWord = "" // Word Player 1 needs to guess (chosen by P2)
    private var player2TargetWord = "" // Word Player 2 needs to guess (chosen by P1)
    private var targetWord = "" // The word for the *active* player to guess

    // --- NEW: Keyboard State Management ---
    private enum class LetterStatus { CORRECT, PRESENT, ABSENT }
    private val keyboardLetterStatus = mutableMapOf<Char, LetterStatus>()
    private lateinit var letterButtonMap: Map<Char, Button>

    private var player1Name = "Player 1"
    private var player2Name = "Player 2"
    // ------------------------------------

    // --- Board states for each player ---
    private var player1BoardState = Array(rows) { Array(cols) { TileState("", 0, 0) } }
    private var player2BoardState = Array(rows) { Array(cols) { TileState("", 0, 0) } }
    private var player1CurrentRow = 0
    private var player2CurrentRow = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBoard()
        setupKeyboard() // This now initializes letterButtonMap

        player1Name = intent.getStringExtra("PLAYER_1_NAME") ?: "Player 1"
        player2Name = intent.getStringExtra("PLAYER_2_NAME") ?: "Player 2"
        player1TargetWord = intent.getStringExtra("PLAYER_1_TARGET_WORD")?.uppercase() ?: "ERROR"
        player2TargetWord = intent.getStringExtra("PLAYER_2_TARGET_WORD")?.uppercase() ?: "ERROR"


        // --- Multiplayer Mode Setup ---
        player1TargetWord = intent.getStringExtra("PLAYER_1_TARGET_WORD")?.uppercase() ?: "ERROR"
        player2TargetWord = intent.getStringExtra("PLAYER_2_TARGET_WORD")?.uppercase() ?: "ERROR"

        // Player 1 starts
        activePlayer = 1
        targetWord = player1TargetWord // P1 guesses P2's word
        updatePlayerIndicator()

        binding.keyboardLayout.visibility = View.VISIBLE





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

    // --- THIS IS THE UPDATED FUNCTION ---
    private fun onEnterPressed() {
        if (currentCol != cols) {
            Toast.makeText(this, "Not enough letters", Toast.LENGTH_SHORT).show()
            return
        }

        val guess = (0 until cols).joinToString("") { j -> tiles[currentRow][j]?.text.toString() }

        // --- Grid Color Logic (from your original code) ---
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
                    availableTargetLettersForGrid[availableTargetLettersForGrid.indexOf(guess[j])] = ' '
                }
            }
        }
        for (j in 0 until cols) {
            tiles[currentRow][j]?.apply { setBackgroundColor(tileColors[j]); setTextColor(Color.WHITE) }
        }

        // --- NEW: Keyboard Coloring Logic ---
        for (j in 0 until cols) {
            val letter = guess[j]
            // First pass for CORRECT (Green) letters to give them priority
            if (letter == targetWord[j]) {
                keyboardLetterStatus[letter] = LetterStatus.CORRECT
            }
            // Second pass for PRESENT (Yellow) and ABSENT (Gray) letters
            else {
                val isPresent = targetWord.contains(letter)
                // Only update if it's not already marked as CORRECT
                if (keyboardLetterStatus[letter] != LetterStatus.CORRECT) {
                    if (isPresent) {
                        // Don't downgrade from PRESENT to ABSENT
                        if (keyboardLetterStatus[letter] != LetterStatus.PRESENT) {
                            keyboardLetterStatus[letter] = LetterStatus.PRESENT
                        }
                    } else {
                        // Only mark as ABSENT if we haven't found it to be PRESENT or CORRECT before
                        if (!keyboardLetterStatus.containsKey(letter)) {
                            keyboardLetterStatus[letter] = LetterStatus.ABSENT
                        }
                    }
                }
            }
        }
        updateKeyboardAppearance() // Apply the new colors

        // --- Win condition check ---
        if (guess.equals(targetWord, ignoreCase = true)) {
            // Use a short delay so the player can see the winning colors
            Handler(Looper.getMainLooper()).postDelayed({
                showEndGamePanel(didWin = true)
            }, 1000)
            return
        }

        // --- Turn switching logic (from your original code) ---
        binding.keyboardLayout.isEnabled = false
        Handler(Looper.getMainLooper()).postDelayed({
            currentRow++
            currentCol = 0

            if (currentRow >= rows) {
                if ((activePlayer == 1 && player2CurrentRow >= rows) || (activePlayer == 2 && player1CurrentRow >= rows)) {
                    showEndGamePanel(didWin = false) // Both players lost (draw)
                } else {
                    switchPlayer()
                }
            } else {
                switchPlayer()
            }
            binding.keyboardLayout.isEnabled = true
        }, 2000)
    }

    // --- NEW HELPER FUNCTION ---
    private fun updateKeyboardAppearance() {
        for ((char, button) in letterButtonMap) {
            val status = keyboardLetterStatus[char] ?: continue // Get status for this character

            val color = when (status) {
                LetterStatus.CORRECT -> Color.parseColor("#6AAA64") // Green
                LetterStatus.PRESENT -> Color.parseColor("#C9B458") // Yellow
                LetterStatus.ABSENT -> Color.parseColor("#787C7E")   // Gray
            }

            // Use DrawableCompat to safely change the background tint of the key
            val drawable = button.background
            DrawableCompat.setTint(drawable, color)
            button.setTextColor(Color.WHITE) // Change text to white for better contrast
        }
    }

    private fun switchPlayer() {
        if (activePlayer == 1) {
            player1CurrentRow = currentRow
            saveBoardState(player1BoardState)
        } else {
            player2CurrentRow = currentRow
            saveBoardState(player2BoardState)
        }

        activePlayer = if (activePlayer == 1) 2 else 1

        if (activePlayer == 1) {
            currentRow = player1CurrentRow
            targetWord = player1TargetWord
            loadBoardState(player1BoardState)
        } else {
            currentRow = player2CurrentRow
            targetWord = player2TargetWord
            loadBoardState(player2BoardState)
        }
        currentCol = 0
        updatePlayerIndicator()
        binding.keyboardLayout.isEnabled = true
    }

    private fun saveBoardState(boardState: Array<Array<TileState>>) {
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val tile = tiles[i][j]
                val text = tile?.text.toString()
                val bgColor = (tile?.background as? ColorDrawable)?.color ?: 0
                val textColor = tile?.currentTextColor ?: Color.BLACK
                boardState[i][j] = TileState(text, bgColor, textColor)
            }
        }
    }

    private fun loadBoardState(boardState: Array<Array<TileState>>) {
        val defaultBg = ContextCompat.getDrawable(this, R.drawable.tile_border)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val state = boardState[i][j]
                tiles[i][j]?.apply {
                    text = state.text
                    if (state.backgroundColor == 0) {
                        background = defaultBg
                        setTextColor(Color.BLACK)
                    } else {
                        setBackgroundColor(state.backgroundColor)
                        setTextColor(state.textColor)
                    }
                }
            }
        }
    }

    private fun updatePlayerIndicator() {
        if (activePlayer == 1) {
            binding.screenTitle.text = "$player1Name 'S TURN"
        } else {
            binding.screenTitle.text = "$player2Name 'S TURN"
        }
        // Removed toast messages as they are redundant with the title change
    }

    private fun showEndGamePanel(didWin: Boolean) {
        binding.keyboardLayout.visibility = View.GONE
        val statsTitle = binding.statsPanel.statsTitle

        if (didWin) {
            statsTitle.text = "Player $activePlayer Wins!"
            val otherPlayer = if (activePlayer == 1) 2 else 1
            val wordToReveal = if (activePlayer == 1) player2TargetWord else player1TargetWord
            Toast.makeText(this, "Player $otherPlayer's word was: $wordToReveal", Toast.LENGTH_LONG).show()
        } else {
            statsTitle.text = "It's a Draw!"
            Toast.makeText(this, "P1 Word: $player2TargetWord | P2 Word: $player1TargetWord", Toast.LENGTH_LONG).show()
        }
        binding.statsPanelContainer.visibility = View.VISIBLE
        binding.statsPanelContainer.animate().translationY(0f).setDuration(500).start()
    }

    // --- UPDATED setupKeyboard() ---
    private fun setupKeyboard() {
        // This links the character to the button view.
        // Make sure your button IDs in XML match this (e.g., binding.buttonQ)
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
