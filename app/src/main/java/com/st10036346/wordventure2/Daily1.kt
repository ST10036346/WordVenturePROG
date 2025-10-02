package com.st10036346.wordventure2

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Daily1 : AppCompatActivity() {
    private lateinit var boardGrid: GridLayout
    private val rows = 6
    private val cols = 5
    private val tiles = Array(rows) { Array<TextView?>(cols) { null } }

    private var currentRow = 0
    private var currentCol = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily1)

        boardGrid = findViewById(R.id.boardGrid)
        setupBoard()
        setupKeyboard()



    }

    private fun setupBoard() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val tile = TextView(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 230
                        height = 230
                        setMargins(2, 2, 2, 2)
                    }
                    textSize = 28f  // Bigger text
                    setTypeface(typeface, Typeface.BOLD) // Bold for contrast
                    gravity = Gravity.CENTER
                    setBackgroundColor(Color.WHITE)
                    setTextColor(Color.BLACK)
                    setBackgroundResource(android.R.drawable.alert_light_frame) // subtle border
                }

                boardGrid.addView(tile)
                tiles[r][c] = tile
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
        if (currentCol == cols) {
            // For now just move to next row
            currentRow++
            currentCol = 0
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
