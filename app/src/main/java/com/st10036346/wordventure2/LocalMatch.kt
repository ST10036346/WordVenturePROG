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
import kotlin.jvm.java

// Data class to hold the state of a single tile (its text and color)
data class TileState(val text: String, val backgroundColor: Int, val textColor: Int)

class LocalMatch : AppCompatActivity() {

    private lateinit var binding: ActivityLocalMatchBinding

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
    private val player1KeyboardStatus = mutableMapOf<Char, LetterStatus>()
    private val player2KeyboardStatus = mutableMapOf<Char, LetterStatus>()

    // We also need a variable to hold the CURRENT player's map easily.
    private var activeKeyboardStatus = player1KeyboardStatus
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

        // --- 1. Correctly load names and target words ONCE ---
        player1Name = intent.getStringExtra("PLAYER_1_NAME")?.uppercase() ?: "PLAYER 1"
        player2Name = intent.getStringExtra("PLAYER_2_NAME")?.uppercase() ?: "PLAYER 2"

        // The word FOR Player 1 (chosen by P2) is sent by the lobby under the key "PLAYER_1_TARGET_WORD"
        player1TargetWord = intent.getStringExtra("PLAYER_1_TARGET_WORD")?.uppercase() ?: "ERROR"

        // The word FOR Player 2 (chosen by P1) is sent by the lobby under the key "PLAYER_2_TARGET_WORD"
        player2TargetWord = intent.getStringExtra("PLAYER_2_TARGET_WORD")?.uppercase() ?: "ERROR"
        // -----------------------------------------------------------------

        setupBoard()
        setupKeyboard() // Make sure this is called AFTER letterButtonMap might be needed

        // --- 2. Set the Initial Game State ---
        activePlayer = 1
        targetWord = player1TargetWord // Player 1 starts by guessing THEIR target word

        // --- 3. Update the UI with the initial state ---
        updatePlayerIndicator() // This will now use the correct player name
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
    // In LocalMatch.kt

    private fun onEnterPressed() {
        if (currentCol != cols) {
            Toast.makeText(this, "Not enough letters", Toast.LENGTH_SHORT).show()
            return
        }

        val guess = (0 until cols).joinToString("") { j -> tiles[currentRow][j]?.text.toString() }

        // --- Grid and Keyboard Coloring Logic (This part is fine and remains) ---
        val availableTargetLettersForGrid = targetWord.toMutableList()
        val tileColors = Array(cols) { Color.parseColor("#787C7E") } // Gray
        for (j in 0 until cols) {
            if (guess[j] == targetWord[j]) {
                tileColors[j] = Color.parseColor("#6AAA64") // Green
                availableTargetLettersForGrid[j] = ' '
                activeKeyboardStatus[guess[j]] = LetterStatus.CORRECT
            }
        }
        for (j in 0 until cols) {
            if (tileColors[j] != Color.parseColor("#6AAA64")) {
                if (availableTargetLettersForGrid.contains(guess[j])) {
                    tileColors[j] = Color.parseColor("#C9B458") // Yellow
                    availableTargetLettersForGrid[availableTargetLettersForGrid.indexOf(guess[j])] = ' '
                    if (activeKeyboardStatus[guess[j]] != LetterStatus.CORRECT) {
                        activeKeyboardStatus[guess[j]] = LetterStatus.PRESENT
                    }
                } else {
                    if (activeKeyboardStatus[guess[j]] != LetterStatus.CORRECT && activeKeyboardStatus[guess[j]] != LetterStatus.PRESENT) {
                        activeKeyboardStatus[guess[j]] = LetterStatus.ABSENT
                    }
                }
            }
        }
        for (j in 0 until cols) {
            tiles[currentRow][j]?.apply { setBackgroundColor(tileColors[j]); setTextColor(Color.WHITE) }
        }
        updateKeyboardAppearance()
        // --- End Coloring Logic ---


        // --- NEW Win/Switch Logic ---
        if (guess.equals(targetWord, ignoreCase = true)) {
            // Player guessed correctly. End the game.
            binding.keyboardLayout.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({ showEndGamePanel(didWin = true) }, 1000)
            return // Stop further execution
        }

        // --- THIS IS THE KEY CHANGE ---
        // If it wasn't a win, immediately switch to the other player.
        binding.keyboardLayout.isEnabled = false // Disable keyboard during switch
        Handler(Looper.getMainLooper()).postDelayed({
            // Before switching, check if the board is full (a draw)
            if (currentRow == rows - 1) {
                showEndGamePanel(didWin = false) // Last guess was made, and no one won
            } else {
                switchPlayer() // Switch to the other player's turn
            }
        }, 1500) // Delay to show colors before switching
    }



    // --- NEW HELPER FUNCTION ---
    // In LocalMatch.kt
    // In LocalMatch.kt

    // In LocalMatch.kt

    private fun updateKeyboardAppearance() {
        for ((char, button) in letterButtonMap) {
            // --- THIS IS THE FIX ---
            // 1. Get a NEW, MUTABLE copy of the default background for every button.
            val defaultKeyBg = ContextCompat.getDrawable(this, R.drawable.key_background)?.mutate()

            // 2. Reset the button to its default state.
            button.background = defaultKeyBg
            button.setTextColor(Color.BLACK) // Or your default text color

            // 3. THEN, check the active player's map for a specific status.
            val status = activeKeyboardStatus[char]

            // 4. If a status exists, apply the corresponding color.
            if (status != null) {
                val color = when (status) {
                    LetterStatus.CORRECT -> Color.parseColor("#6AAA64") // Green
                    LetterStatus.PRESENT -> Color.parseColor("#C9B458") // Yellow
                    LetterStatus.ABSENT -> Color.parseColor("#787C7E")   // Gray
                }

                // Get the button's current background and apply a tint.
                // Since we created a mutable copy, this won't affect other buttons.
                val drawable = button.background
                DrawableCompat.setTint(drawable, color)
                button.setTextColor(Color.WHITE)
            }
        }
    }




    // In LocalMatch.kt
    // In LocalMatch.kt

    // In LocalMatch.kt

    private fun switchPlayer() {
        // 1. Save board state of the player who just finished.
        if (activePlayer == 1) {
            saveBoardState(player1BoardState)
        } else {
            saveBoardState(player2BoardState)
        }

        // 2. Switch the active player number.
        activePlayer = if (activePlayer == 1) 2 else 1

        // 3. Move to the next row if it's Player 1's turn again.
        if (activePlayer == 1) {
            currentRow++
        }

        // 4. Load the state for the NEW player.
        if (activePlayer == 1) {
            targetWord = player1TargetWord
            activeKeyboardStatus = player1KeyboardStatus // <--- Set P1's keyboard state as active
            loadBoardState(player1BoardState)
        } else { // activePlayer is 2
            targetWord = player2TargetWord
            activeKeyboardStatus = player2KeyboardStatus // <--- Set P2's keyboard state as active
            loadBoardState(player2BoardState)
        }

        // 5. Reset column for the new turn.
        currentCol = 0

        // 6. Update the UI. (NO MORE KEYBOARD RESET)
        updateKeyboardAppearance() // This now redraws the keyboard with the new active player's map
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
            binding.screenTitle.text = "$player1Name'S TURN"
        } else {
            binding.screenTitle.text = "$player2Name'S TURN"
        }
        // Removed toast messages as they are redundant with the title change
    }

    // In LocalMatch.kt

    private fun showEndGamePanel(didWin: Boolean) {
        binding.keyboardLayout.visibility = View.GONE
        val statsTitle = binding.statsPanel.statsTitle
        val statsBody = binding.statsPanel.statsBody // Assuming you have a TextView with this ID

        if (didWin) {
            if (activePlayer == 1) {
                MultiplayerScore.player1Wins++
                statsTitle.text = "$player1Name Wins!"
            } else {
                MultiplayerScore.player2Wins++
                statsTitle.text = "$player2Name Wins!"
            }
        } else {
            statsTitle.text = "It's a Draw!"
        }

        // --- NEW: Display the overall score ---
        statsBody.text = "SCORE\n$player1Name: ${MultiplayerScore.player1Wins}\n$player2Name: ${MultiplayerScore.player2Wins}"
        statsBody.visibility = View.VISIBLE

        binding.statsPanelContainer.visibility = View.VISIBLE
        binding.statsPanelContainer.animate().translationY(0f).setDuration(500).start()

        // --- NEW: Setup "Play Again" and "Main Menu" buttons ---
        val playAgainButton = binding.statsPanel.playAgainButton
        val mainMenuButton = binding.statsPanel.mainMenuButton

        playAgainButton.visibility = View.VISIBLE
        mainMenuButton.visibility = View.VISIBLE

        playAgainButton.setOnClickListener {
            // --- THIS IS THE FIX for names disappearing ---
            // When playing again, we must pass the player names back to the lobby.
            val intent = Intent(this, StartMultiplayerMatch::class.java).apply {
                putExtra("PLAYER_1_NAME", player1Name)
                putExtra("PLAYER_2_NAME", player2Name)
            }
            startActivity(intent)
            finish()
        }

        mainMenuButton.setOnClickListener {
            MultiplayerScore.reset()
            val intent = Intent(this, MainMenu::class.java)
            startActivity(intent)
            finish()
        }
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
