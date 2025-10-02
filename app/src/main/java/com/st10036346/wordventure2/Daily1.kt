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

class Daily1 : AppCompatActivity() {

    // 1. Declare the binding variable. This will replace findViewById for all layout views.
    private lateinit var binding: ActivityDaily1Binding

    // 2. Class-level properties for game state.
    private val rows = 6
    private val cols = 5
    private val tiles = Array(rows) { arrayOfNulls<TextView>(cols) }
    private var currentRow = 0
    private var currentCol = 0
    private var targetWord = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 3. Inflate the layout using View Binding and set the content view.
        binding = ActivityDaily1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.keyboardLayout.visibility = View.INVISIBLE // Use INVISIBLE to keep layout space

        // 4. Call the setup functions.
        fetchWordleWord()
        setupBoard()
        setupKeyboard()
    }

    private fun setupBoard() {
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val tile = TextView(this).apply {
                    layoutParams = android.widget.GridLayout.LayoutParams().apply {
                        width = 230 // Consider moving this to a dimens.xml file
                        height = 230
                        setMargins(2, 2, 2, 2)
                    }
                    text = ""
                    gravity = Gravity.CENTER
                    textSize = 32f
                    setTextColor(Color.BLACK)
                    setTypeface(null, Typeface.BOLD)
                    background = ContextCompat.getDrawable(this@Daily1, R.drawable.tile_border)
                }

                // Use the binding object to access the boardGrid
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

        val guess = (0 until cols).joinToString("") { j ->
            tiles[currentRow][j]?.text.toString()
        }

        val availableTargetLetters = targetWord.toMutableList()
        val tileColors = Array(cols) { Color.parseColor("#787C7E") } // Gray

        // First Pass: Greens
        for (j in 0 until cols) {
            if (guess[j] == targetWord[j]) {
                tileColors[j] = Color.parseColor("#6AAA64") // Green
                availableTargetLetters[j] = ' '
            }
        }

        // Second Pass: Yellows
        for (j in 0 until cols) {
            if (tileColors[j] != Color.parseColor("#6AAA64")) {
                if (availableTargetLetters.contains(guess[j])) {
                    tileColors[j] = Color.parseColor("#C9B458") // Yellow
                    availableTargetLetters[availableTargetLetters.indexOf(guess[j])] = ' '
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

        // Check for win/loss
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

    private fun setupKeyboard() {
        // A map of Button Views to the character they represent, using View Binding
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

        for ((button, letter) in buttonMap) {
            button.setOnClickListener { onLetterPressed(letter) }
        }

        // Set listeners for Enter and Delete using View Binding
        binding.buttonEnter.setOnClickListener { onEnterPressed() }
        binding.buttonDelete.setOnClickListener { onBackPressedCustom() }
    }

    private fun showStatsPanel(didWin: Boolean) {
        // Hide the keyboard
        binding.keyboardLayout.visibility = View.GONE

        // Get the title TextView from the included layout via the binding object.
        // For this to work, you must have an id on your <include> tag in activity_daily1.xml.
        // I am assuming it is: android:id="@+id/stats_panel"
        val statsTitle = binding.statsPanel.statsTitle

        if (didWin) {
            statsTitle.text = "CONGRATULATIONS!"
        } else {
            statsTitle.text = "NEXT TIME!"
            Toast.makeText(this@Daily1, "WORD WAS: " + targetWord, Toast.LENGTH_LONG).show()

        }

        // TODO: Update the rest of the stats values using binding.statsPanel.your_view_id

        // Animate the panel into view
        binding.statsPanelContainer.visibility = View.VISIBLE
        binding.statsPanelContainer.animate()
            .translationY(0f)
            .setDuration(500)
            .start()
    }

    // This function is not currently used, but is good practice to keep if needed later.
    private fun setTile(row: Int, col: Int, letter: Char, color: Int) {
        tiles[row][col]?.apply {
            text = if (letter == ' ') "" else letter.toString()
            setBackgroundColor(color)
        }
    }

    private fun fetchWordleWord() {
        RetrofitClient.instance.getRandomWord().enqueue(object : retrofit2.Callback<WordResponse> {
            override fun onResponse(call: retrofit2.Call<WordResponse>, response: retrofit2.Response<WordResponse>) {


                binding.keyboardLayout.visibility = View.VISIBLE

                if (response.isSuccessful) {
                    // Update the targetWord with the fetched word
                    // Use a fallback word like "APPLE" if the response is null
                    val randomWord = response.body()!!.word
                    targetWord = randomWord.uppercase() // IMPORTANT: Ensure the word is uppercase

                }
                else {
                    // Handle cases where the server gives an error (e.g., 404, 500)
                    Toast.makeText(this@Daily1, "Error: Could not fetch a word from the server.", Toast.LENGTH_LONG).show()
                    targetWord = "ERROR" // Set a fallback word only on server error
                }
            }

            override fun onFailure(call: retrofit2.Call<WordResponse>, t: Throwable) {

                binding.keyboardLayout.visibility = View.VISIBLE
                // Handle network failure (no internet, server down, etc.)
                Toast.makeText(this@Daily1, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
