package com.st10036346.wordventure2

import android.content.Context
import android.content.SharedPreferences

// This data class holds all game statistics
data class GameStats(
    val gamesPlayed: Int,
    val winStreak: Int,
    val maxStreak: Int,
    val guessDistribution: IntArray
)

// 💡 UPDATE: StatsManager now requires the unique userId in the constructor
class StatsManager(context: Context, private val userId: String) {

    // 💡 FIX: Use the userId in the SharedPreferences file name
    // This creates a separate file for each user, making the stats user-specific.
    private val PREFS_FILE_NAME = "WordVentureStats_$userId"
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    companion object {
        // Keys for basic stats
        private const val KEY_GAMES_PLAYED = "games_played"
        private const val KEY_WIN_STREAK = "win_streak"
        private const val KEY_MAX_STREAK = "max_streak"

        // Keys for Guess Distribution (count of wins on guess 1 to 6)
        private val KEY_GUESS_DISTRIBUTION = (1..6).map { "guess_dist_$it" }.toTypedArray()
    }

    /**
     * Retrieves all current statistics.
     * @return A GameStats data class containing all tracked statistics.
     */
    fun getStats(): GameStats {
        val gamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0)
        val winStreak = prefs.getInt(KEY_WIN_STREAK, 0)
        val maxStreak = prefs.getInt(KEY_MAX_STREAK, 0)

        val guessDist = IntArray(6) { i ->
            prefs.getInt(KEY_GUESS_DISTRIBUTION[i], 0)
        }

        return GameStats(gamesPlayed, winStreak, maxStreak, guessDist)
    }

    /**
     * Updates statistics after a game is completed.
     * @param didWin True if the game was won, False otherwise.
     * @param guesses The number of guesses taken (1-6) if won, or 0 if lost.
     */
    fun updateStats(didWin: Boolean, guesses: Int) {
        val gamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0) + 1
        var winStreak = prefs.getInt(KEY_WIN_STREAK, 0)
        var maxStreak = prefs.getInt(KEY_MAX_STREAK, 0)

        if (didWin) {
            winStreak += 1
            if (winStreak > maxStreak) {
                maxStreak = winStreak
            }

            // Increment the count for the specific guess number (guesses is 1-6)
            if (guesses in 1..6) {
                val index = guesses - 1 // Array index is 0-5
                val currentCount = prefs.getInt(KEY_GUESS_DISTRIBUTION[index], 0)
                prefs.edit().putInt(KEY_GUESS_DISTRIBUTION[index], currentCount + 1).apply()
            }
        } else {
            winStreak = 0 // Reset win streak on a loss
        }

        // Update basic stats
        prefs.edit()
            .putInt(KEY_GAMES_PLAYED, gamesPlayed)
            .putInt(KEY_WIN_STREAK, winStreak)
            .putInt(KEY_MAX_STREAK, maxStreak)
            .apply()
    }
}