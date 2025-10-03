package com.st10036346.wordventure2

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import com.st10036346.wordventure2.databinding.ActivityLocalMatchBinding // IMPORTANT: Use ActivityLocalMatchBinding

// Data class to hold the state of a single tile (its text and color)
data class TileState(val text: String, val backgroundColor: Int, val textColor: Int)

class LocalMatch : AppCompatActivity() {

    // Use the binding class for activity_local_match.xml
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

    // --- Board states for each player ---
    private var player1BoardState = Array(rows) { Array(cols) { TileState("", 0, 0) } }
    private var player2BoardState = Array(rows) { Array(cols) { TileState("", 0, 0) } }
    private var player1CurrentRow = 0
    private var player2CurrentRow = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the correct layout
        binding = ActivityLocalMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBoard()
        setupKeyboard()

        // --- Multiplayer Mode Setup ---
        player1TargetWord = intent.getStringExtra("PLAYER_1_TARGET_WORD")?.uppercase() ?: "ERROR"
        player2TargetWord = intent.getStringExtra("PLAYER_2_TARGET_WORD")?.uppercase() ?: "ERROR"

        // Player 1 starts
        activePlayer = 1
        targetWord = player1TargetWord // P1 guesses P2's word
        updatePlayerIndicator()

        binding.keyboardLayout.visibility = View.VISIBLE
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
                binding.boardGrid.addView(tile) // Assumes boardGrid ID exists in activity_local_match.xml
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

        val guess = (0 until cols).joinToString("") { j -> tiles[currentRow][j]?.text.toString() }

        // Color logic
        val availableTargetLetters = targetWord.toMutableList()
        val tileColors = Array(cols) { Color.parseColor("#787C7E") } // Gray
        for (j in 0 until cols) {
            if (guess[j] == targetWord[j]) {
                tileColors[j] = Color.parseColor("#6AAA64"); availableTargetLetters[j] = ' '
            }
        }
        for (j in 0 until cols) {
            if (tileColors[j] != Color.parseColor("#6AAA64")) {
                if (availableTargetLetters.contains(guess[j])) {
                    tileColors[j] = Color.parseColor("#C9B458")
                    availableTargetLetters[availableTargetLetters.indexOf(guess[j])] = ' '
                }
            }
        }
        for (j in 0 until cols) {
            tiles[currentRow][j]?.apply { setBackgroundColor(tileColors[j]); setTextColor(Color.WHITE) }
        }

        if (guess.equals(targetWord, ignoreCase = true)) {
            showEndGamePanel(didWin = true)
            return
        }

        binding.keyboardLayout.isEnabled = false

        Handler(Looper.getMainLooper()).postDelayed({
            // This code will run after a 2-second delay
            currentRow++
            currentCol = 0

            if (currentRow >= rows) {
                if ((activePlayer == 1 && player2CurrentRow >= rows) || (activePlayer == 2 && player1CurrentRow >= rows)) {
                    showEndGamePanel(didWin = false) // Both players lost (draw)
                } else {
                    switchPlayer() // Switch turns
                }
            } else {
                switchPlayer() // Switch turns
            }

            // Re-enable the keyboard for the next player
            binding.keyboardLayout.isEnabled = true

        }, 2000)
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
        // Assumes player1_turn_indicator and player2_turn_indicator IDs exist in the layout
        if (activePlayer == 1) {
            binding.screenTitle.text = "PLAYER 1'S TURN"
            Toast.makeText(this, "Player 1's Turn!", Toast.LENGTH_SHORT).show()
        } else {
            binding.screenTitle.text = "PLAYER 2'S TURN"
            Toast.makeText(this, "Player 2's Turn!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEndGamePanel(didWin: Boolean) {
        binding.keyboardLayout.visibility = View.GONE
        // Assumes statsPanel.statsTitle ID exists via an <include> in the layout
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

    private fun setupKeyboard() {
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
        for ((button, letter) in buttonMap) { button.setOnClickListener { onLetterPressed(letter) } }
        binding.buttonEnter.setOnClickListener { onEnterPressed() }
        binding.buttonDelete.setOnClickListener { onBackPressedCustom() }
    }
}
