/*
 * Author: Ethan Pillay
 * Source: GeeksforGeeks - "How to Build a Wordle Game Application in Android"
 * URL: https://www.geeksforgeeks.org/android/how-to-build-a-wordle-game-application-in-android/
 * Description: Handles the local multiplayer gameplay. Manages the game board, player turns,
 *              letter input, tile coloring, keyboard status, win/loss checking, and displays
 *              the game result panel with scores.
 */


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
import android.widget.GridLayout
import androidx.core.graphics.drawable.DrawableCompat
import com.st10036346.wordventure2.databinding.ActivityLocalMatchBinding
import kotlin.jvm.java

// Holds text and color info for each tile
data class TileState(val text: String, val backgroundColor: Int, val textColor: Int)

class LocalMatch : AppCompatActivity() {

    private lateinit var binding: ActivityLocalMatchBinding

    private val rows = 6
    private val cols = 5
    private val tiles = Array(rows) { arrayOfNulls<TextView>(cols) }
    private var currentRow = 0
    private var currentCol = 0

    // Multiplayer variables
    private var activePlayer = 1
    private var player1TargetWord = ""
    private var player2TargetWord = ""
    private var targetWord = ""

    // Keyboard tracking
    private enum class LetterStatus { CORRECT, PRESENT, ABSENT }
    private val player1KeyboardStatus = mutableMapOf<Char, LetterStatus>()
    private val player2KeyboardStatus = mutableMapOf<Char, LetterStatus>()
    private var activeKeyboardStatus = player1KeyboardStatus
    private lateinit var letterButtonMap: Map<Char, Button>

    private var player1Name = "Player 1"
    private var player2Name = "Player 2"

    // Board states for both players
    private var player1BoardState = Array(rows) { Array(cols) { TileState("", 0, 0) } }
    private var player2BoardState = Array(rows) { Array(cols) { TileState("", 0, 0) } }
    private var player1CurrentRow = 0
    private var player2CurrentRow = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocalMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load player names and target words
        player1Name = intent.getStringExtra("PLAYER_1_NAME")?.uppercase() ?: "PLAYER 1"
        player2Name = intent.getStringExtra("PLAYER_2_NAME")?.uppercase() ?: "PLAYER 2"
        player1TargetWord = intent.getStringExtra("PLAYER_1_TARGET_WORD")?.uppercase() ?: "ERROR"
        player2TargetWord = intent.getStringExtra("PLAYER_2_TARGET_WORD")?.uppercase() ?: "ERROR"

        setupBoard()

        // Set initial state
        activePlayer = 1
        targetWord = player1TargetWord
        updatePlayerIndicator()

        // Navigation buttons
        binding.profileIcon.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.settingsIcon.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.bookIcon.setOnClickListener {
            startActivity(Intent(this, MainMenu::class.java))
            finish()
        }
    }

    // Sets up the board layout
    private fun setupBoard() {
        binding.boardGrid.removeAllViews()
        val marginSizeInPixels = resources.getDimensionPixelSize(R.dimen.tile_margin)
        binding.keyboardLayout.visibility = View.INVISIBLE

        binding.boardGrid.post {
            val gridWidth = binding.boardGrid.width
            val totalMarginSpace = (marginSizeInPixels * 2) * cols
            val netWidthForTiles = gridWidth - totalMarginSpace
            val tileSize = netWidthForTiles / cols

            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    val tile = TextView(this@LocalMatch).apply {
                        val rowSpec = GridLayout.spec(i)
                        val colSpec = GridLayout.spec(j)
                        layoutParams = GridLayout.LayoutParams(rowSpec, colSpec).apply {
                            width = tileSize
                            height = tileSize
                            setMargins(marginSizeInPixels, marginSizeInPixels, marginSizeInPixels, marginSizeInPixels)
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

            setupKeyboard()
            binding.keyboardLayout.visibility = View.VISIBLE
        }
    }

    // Handles letter input
    private fun onLetterPressed(letter: Char) {
        if (currentCol < cols) {
            tiles[currentRow][currentCol]?.text = letter.toString()
            currentCol++
        }
    }

    // Handles backspace
    private fun onBackPressedCustom() {
        if (currentCol > 0) {
            currentCol--
            tiles[currentRow][currentCol]?.text = ""
        }
    }

    // Handles Enter button
    private fun onEnterPressed() {
        if (currentCol != cols) {
            Toast.makeText(this, "Not enough letters", Toast.LENGTH_SHORT).show()
            return
        }

        val guess = (0 until cols).joinToString("") { j -> tiles[currentRow][j]?.text.toString() }

        // Check letters and color tiles
        val availableTargetLetters = targetWord.toMutableList()
        val tileColors = Array(cols) { Color.parseColor("#787C7E") } // Default gray

        // Check correct positions
        for (j in 0 until cols) {
            if (guess[j] == targetWord[j]) {
                tileColors[j] = Color.parseColor("#6AAA64") // Green
                availableTargetLetters[j] = ' '
                activeKeyboardStatus[guess[j]] = LetterStatus.CORRECT
            }
        }

        // Check letters that exist but in wrong spot
        for (j in 0 until cols) {
            if (tileColors[j] != Color.parseColor("#6AAA64")) {
                if (availableTargetLetters.contains(guess[j])) {
                    tileColors[j] = Color.parseColor("#C9B458") // Yellow
                    availableTargetLetters[availableTargetLetters.indexOf(guess[j])] = ' '
                    if (activeKeyboardStatus[guess[j]] != LetterStatus.CORRECT) {
                        activeKeyboardStatus[guess[j]] = LetterStatus.PRESENT
                    }
                } else {
                    if (activeKeyboardStatus[guess[j]] != LetterStatus.CORRECT &&
                        activeKeyboardStatus[guess[j]] != LetterStatus.PRESENT) {
                        activeKeyboardStatus[guess[j]] = LetterStatus.ABSENT
                    }
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

        updateKeyboardAppearance()

        // Check for win
        if (guess.equals(targetWord, ignoreCase = true)) {
            binding.keyboardLayout.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({ showEndGamePanel(true) }, 1000)
            return
        }

        // Switch player if not win
        binding.keyboardLayout.isEnabled = false
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentRow == rows - 1) {
                showEndGamePanel(false)
            } else {
                switchPlayer()
            }
        }, 1500)
    }

    // Updates keyboard button colors
    private fun updateKeyboardAppearance() {
        for ((char, button) in letterButtonMap) {
            val defaultBg = ContextCompat.getDrawable(this, R.drawable.key_background)?.mutate()
            button.background = defaultBg
            button.setTextColor(Color.BLACK)

            val status = activeKeyboardStatus[char]
            if (status != null) {
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
    }

    // Switches active player
    private fun switchPlayer() {
        if (activePlayer == 1) saveBoardState(player1BoardState)
        else saveBoardState(player2BoardState)

        activePlayer = if (activePlayer == 1) 2 else 1

        if (activePlayer == 1) currentRow++

        if (activePlayer == 1) {
            targetWord = player1TargetWord
            activeKeyboardStatus = player1KeyboardStatus
            loadBoardState(player1BoardState)
        } else {
            targetWord = player2TargetWord
            activeKeyboardStatus = player2KeyboardStatus
            loadBoardState(player2BoardState)
        }

        currentCol = 0
        updateKeyboardAppearance()
        updatePlayerIndicator()
        binding.keyboardLayout.isEnabled = true
    }

    // Saves current board
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

    // Loads board for active player
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

    // Updates the player turn label
    private fun updatePlayerIndicator() {
        binding.screenTitle.text =
            if (activePlayer == 1) "$player1Name'S TURN" else "$player2Name'S TURN"
    }

    // Shows game result screen
    private fun showEndGamePanel(didWin: Boolean) {
        binding.keyboardLayout.visibility = View.GONE
        val statsTitle = binding.statsPanel.statsTitle
        val statsBody = binding.statsPanel.statsBody

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

        // Show scores
        statsBody.text = "SCORE\n$player1Name: ${MultiplayerScore.player1Wins}\n$player2Name: ${MultiplayerScore.player2Wins}"
        statsBody.visibility = View.VISIBLE

        binding.statsPanelContainer.visibility = View.VISIBLE
        binding.statsPanelContainer.animate().translationY(0f).setDuration(500).start()

        // Play Again button
        binding.statsPanel.playAgainButton.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                val intent = Intent(this@LocalMatch, StartMultiplayerMatch::class.java).apply {
                    putExtra("PLAYER_1_NAME", player1Name)
                    putExtra("PLAYER_2_NAME", player2Name)
                }
                startActivity(intent)
                finish()
            }
        }

        // Main Menu button
        binding.statsPanel.mainMenuButton.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                MultiplayerScore.reset()
                startActivity(Intent(this@LocalMatch, MainMenu::class.java))
                finish()
            }
        }
    }

    // Sets up the keyboard
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

        for ((letter, button) in letterButtonMap) {
            button.setOnClickListener { onLetterPressed(letter) }
        }

        binding.buttonEnter.setOnClickListener { onEnterPressed() }
        binding.buttonDelete.setOnClickListener { onBackPressedCustom() }
    }
}
