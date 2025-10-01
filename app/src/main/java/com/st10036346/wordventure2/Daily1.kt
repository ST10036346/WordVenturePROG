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
            setTile(currentRow, currentCol, letter, Color.BLACK)
            currentCol++
        }
    }

    private fun onBackPressedCustom() {
        if (currentCol > 0) {
            currentCol--
            setTile(currentRow, currentCol, ' ', Color.WHITE)
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
}
