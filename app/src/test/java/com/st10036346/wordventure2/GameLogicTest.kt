package com.st10036346.wordventure2

import org.junit.Assert.assertEquals
import org.junit.Test

// define LetterStatus to make the test self-contained
private enum class LetterStatus {
    CORRECT, // Green: Right letter, right position
    PRESENT, // Yellow: Right letter, wrong position
    ABSENT   // Gray: Letter is not in the word
}

/**
 * Helper function that mirrors the complex word-matching logic found in the Activities' colorGridRow
 * methods. This allows us to unit test the logic without requiring an Android Activity instance.
 */
private fun evaluateGuess(target: String, guess: String): List<LetterStatus> {
    if (target.length != guess.length) {
        return List(guess.length) { LetterStatus.ABSENT }
    }

    val length = target.length
    val results = MutableList(length) { LetterStatus.ABSENT }
    val targetLetters = target.toMutableList()

    // Pass 1: Find CORRECT (Green) letters and consume the target letter
    for (i in 0 until length) {
        if (guess[i] == target[i]) {
            results[i] = LetterStatus.CORRECT
            targetLetters[i] = ' ' // Mark this target letter as used
        }
    }

    // Pass 2: Find PRESENT (Yellow) letters
    for (i in 0 until length) {
        // Only process if not already marked CORRECT
        if (results[i] != LetterStatus.CORRECT) {
            val letter = guess[i]
            val indexInTarget = targetLetters.indexOf(letter)
            if (indexInTarget != -1) {
                // Found a match, mark as PRESENT and consume the target letter
                results[i] = LetterStatus.PRESENT
                targetLetters[indexInTarget] = ' '
            }
        }
    }

    return results
}

/**
 * Unit tests for the core Wordle-style game logic, validating the complex duplicate letter handling.
 */
class GameLogicUnitTest {

    // Helper constants for easy reading of expected results
    private val C = LetterStatus.CORRECT // Green
    private val P = LetterStatus.PRESENT // Yellow
    private val A = LetterStatus.ABSENT  // Gray

    /**
     * Test Case 1: Perfect Match
     * Target: APPLE, Guess: APPLE
     */
    @Test
    fun evaluateGuess_perfectMatch_returnsAllCorrect() {
        val target = "APPLE"
        val guess = "APPLE"
        val expected = listOf(C, C, C, C, C)
        val actual = evaluateGuess(target, guess)
        assertEquals("Perfect match failed.", expected, actual)
    }

    /**
     * Test Case 2: Complete Miss
     * Target: APPLE, Guess: MOUND
     */
    @Test
    fun evaluateGuess_completeMiss_returnsAllAbsent() {
        val target = "APPLE"
        val guess = "MOUND"
        val expected = listOf(A, A, A, A, A)
        val actual = evaluateGuess(target, guess)
        assertEquals("Complete miss failed.", expected, actual)
    }

    /**
     * Test Case 3: Mixed Results (Known letters in wrong spots)
     * Target: HEART, Guess: EARTH
     */
    @Test
    fun evaluateGuess_mixedResults() {
        val target = "HEART"
        val guess = "EARTH"
        // CORRECTED EXPECTED VALUE: All letters are present but none are in the correct spot.
        val expected = listOf(P, P, P, P, P)
        val actual = evaluateGuess(target, guess)
        assertEquals("Mixed results failed.", expected, actual)
    }

    /**
     * Test Case 4: Handling Duplicate Letters in Guess and Target (ROBOT vs BLOOM)
     * Target: ROBOT, Guess: BLOOM
     * The 'O' at index 3 is CORRECT. The 'O' at index 2 in guess is PRESENT.
     * Expected: B(P) L(A) O(P) O(C) M(A)
     */
    @Test
    fun evaluateGuess_duplicatesInGuess_correctlyHandlesConsumption() {
        val target = "ROBOT"
        val guess = "BLOOM"
        val expected = listOf(P, A, P, C, A)
        val actual = evaluateGuess(target, guess)
        assertEquals("Duplicate guess letters failed.", expected, actual)
    }
}
