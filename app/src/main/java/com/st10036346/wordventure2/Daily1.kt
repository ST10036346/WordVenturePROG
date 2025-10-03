package com.st10036346.wordventure2

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.st10036346.wordventure2.databinding.ActivityDaily1Binding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Daily1 : AppCompatActivity() {

    private lateinit var binding: ActivityDaily1Binding

    private val rows = 6
    private val cols = 5
    private val tiles = Array(rows) { arrayOfNulls<TextView>(cols) }
    private var currentRow = 0
    private var currentCol = 0
    private var targetWord = "" // Will be fetched from API

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaily1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBoard()
        setupKeyboard()

        // Hide keyboard and show loader until word is fetched
        binding.keyboardLayout.visibility = View.INVISIBLE

        fetchWordleWord()
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
            showStatsPanel(didWin = true)
            return
        }

        currentRow++
        currentCol = 0

        if (currentRow >= rows) {
            showStatsPanel(didWin = false)
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
        binding.statsPanelContainer.visibility = View.VISIBLE
        binding.statsPanelContainer.animate().translationY(0f).setDuration(500).start()
    }

    private fun fetchWordleWord() {
        RetrofitClient.instance.getRandomWord().enqueue(object : Callback<WordResponse> {
            override fun onResponse(call: Call<WordResponse>, response: Response<WordResponse>) {

                binding.keyboardLayout.visibility = View.VISIBLE
                if (response.isSuccessful) {
                    targetWord = response.body()?.word?.uppercase() ?: "APPLE"
                } else {
                    targetWord = "ERROR"
                    Toast.makeText(this@Daily1, "Failed to get a word.", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<WordResponse>, t: Throwable) {

                binding.keyboardLayout.visibility = View.VISIBLE
                targetWord = "LOCAL"
                Toast.makeText(this@Daily1, "Network Error. Using default word.", Toast.LENGTH_LONG).show()
            }
        })
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
