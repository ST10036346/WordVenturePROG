package com.st10036346.wordventure2

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class Daily1 : AppCompatActivity() {
    private lateinit var boardGrid: GridLayout
    private val rows = 6
    private val cols = 5
    private val tiles = Array(rows) { Array<TextView?>(cols) { null } }

    private var currentRow = 0
    private var currentCol = 0

    private val targetWord = "EARTH"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily1)

        boardGrid = findViewById(R.id.boardGrid)
        setupBoard()
        setupKeyboard()



    }


    private fun setupBoard() {
        // A single, corrected loop to create exactly 30 tiles
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val tile = TextView(this).apply {
                    // Set the size and margins for each tile
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 230 // You can adjust this size as needed
                        height = 230
                        setMargins(2, 2, 2, 2)
                    }

                    // Set the visual properties of the tile
                    text = ""
                    gravity = Gravity.CENTER
                    textSize = 32f // Use a consistent text size
                    setTextColor(Color.BLACK)
                    setTypeface(null, Typeface.BOLD)

                    // Apply the border drawable you created
                    background = ContextCompat.getDrawable(this@Daily1, R.drawable.tile_border)
                }

                // Add the fully configured tile to the grid
                boardGrid.addView(tile)
                // Store the reference to THIS tile in the array
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

    // In Daily1.kt
    private fun onEnterPressed() {
        if (currentCol != cols) {
            Toast.makeText(this, "Not enough letters", Toast.LENGTH_SHORT).show()
            return
        }

        var guess = ""
        for (j in 0 until cols) {
            guess += tiles[currentRow][j]?.text.toString()
        }

        // --- Start of New Logic ---

        // A mutable list of the letters in the target word that are available for yellow matches.
        val availableTargetLetters = targetWord.toMutableList()
        // An array to store the final color of each tile. Initialize all to gray.
        val tileColors = Array(cols) { Color.parseColor("#787C7E") } // Gray

        // First Pass: Find all correct letters in the correct position (Greens)
        for (j in 0 until cols) {
            val tile = tiles[currentRow][j]
            val letter = guess[j]

            if (letter == targetWord[j]) {
                tileColors[j] = Color.parseColor("#6AAA64") // Green
                // Mark this letter as "used" so it can't be used for a yellow match.
                // Replacing with a placeholder character is a simple way to do this.
                availableTargetLetters[j] = ' '
            }
        }

        // Second Pass: Find correct letters in the wrong position (Yellows)
        for (j in 0 until cols) {
            val tile = tiles[currentRow][j]
            val letter = guess[j]

            // Only check tiles that are not already green
            if (tileColors[j] != Color.parseColor("#6AAA64")) {
                if (availableTargetLetters.contains(letter)) {
                    tileColors[j] = Color.parseColor("#C9B458") // Yellow
                    // Mark this letter as "used" from the available list.
                    availableTargetLetters[availableTargetLetters.indexOf(letter)] = ' '
                }
            }
        }

        // Apply the colors determined in the two passes
        for (j in 0 until cols) {
            val tile = tiles[currentRow][j]
            tile?.setBackgroundColor(tileColors[j])
            tile?.setTextColor(Color.WHITE)
        }

        // --- End of New Logic ---

        // 4. Check for win/loss and move to the next row (this part remains the same)
        if (guess.equals(targetWord, ignoreCase = true)) {
            Toast.makeText(this, "You won!", Toast.LENGTH_LONG).show()
            // You could disable the keyboard here
            return
        }

        // Move to the next row
        currentRow++
        currentCol = 0

        if (currentRow >= rows) {
            Toast.makeText(this, "Game Over! The word was $targetWord", Toast.LENGTH_LONG).show()
            // You could disable the keyboard here
        }
    }



    private fun setTile(row: Int, col: Int, letter: Char, color: Int) {
        tiles[row][col]?.apply {
            text = if (letter == ' ') "" else letter.toString()
            setBackgroundColor(color)
        }
    }

    private fun setupKeyboard() {
        // A map of Button IDs to the character they represent
        val buttonIdToChar = mapOf(
            R.id.button_q to 'Q', R.id.button_w to 'W', R.id.button_e to 'E',
            R.id.button_r to 'R', R.id.button_t to 'T', R.id.button_y to 'Y',
            R.id.button_u to 'U', R.id.button_i to 'I', R.id.button_o to 'O',
            R.id.button_p to 'P', R.id.button_a to 'A', R.id.button_s to 'S',
            R.id.button_d to 'D', R.id.button_f to 'F', R.id.button_g to 'G',
            R.id.button_h to 'H', R.id.button_j to 'J', R.id.button_k to 'K',
            R.id.button_l to 'L', R.id.button_z to 'Z', R.id.button_x to 'X',
            R.id.button_c to 'C', R.id.button_v to 'V', R.id.button_b to 'B',
            R.id.button_n to 'N', R.id.button_m to 'M'
        )

        // Loop through the map to set up each button
        for ((buttonId, letter) in buttonIdToChar) {
            findViewById<Button>(buttonId).setOnClickListener {
                onLetterPressed(letter)
            }
        }

        findViewById<Button>(R.id.button_enter).setOnClickListener { onEnterPressed() }
        findViewById<Button>(R.id.button_delete).setOnClickListener { onBackPressedCustom()}

        // You will also need to handle the Enter and Backspace buttons
        // findViewById<Button>(R.id.button_enter).setOnClickListener { onEnterPressed() }
        // findViewById<Button>(R.id.button_backspace).setOnClickListener { onBackPressedCustom() }
    }
}
