package com.st10036346.wordventure2

import android.content.Context
import android.content.SharedPreferences

// Holds all game stats
data class GameStats(
    val gamesPlayed: Int,
    val winStreak: Int,
    val maxStreak: Int,
    val guessDistribution: IntArray
)

// Manages stats for a specific user
class StatsManager(context: Context, private val userId: String) {

    // Separate prefs file per user
    private val PREFS_FILE_NAME = "WordVentureStats_$userId"
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GAMES_PLAYED = "games_played"
        private const val KEY_WIN_STREAK = "win_streak"
        private const val KEY_MAX_STREAK = "max_streak"
        private val KEY_GUESS_DISTRIBUTION = (1..6).map { "guess_dist_$it" }.toTypedArray()
    }

    // Get all stats
    fun getStats(): GameStats {
        val gamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0)
        val winStreak = prefs.getInt(KEY_WIN_STREAK, 0)
        val maxStreak = prefs.getInt(KEY_MAX_STREAK, 0)
        val guessDist = IntArray(6) { i -> prefs.getInt(KEY_GUESS_DISTRIBUTION[i], 0) }
        return GameStats(gamesPlayed, winStreak, maxStreak, guessDist)
    }

    // Update stats after a game
    fun updateStats(didWin: Boolean, guesses: Int) {
        val editor = prefs.edit()

        val gamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0) + 1
        var winStreak = prefs.getInt(KEY_WIN_STREAK, 0)
        var maxStreak = prefs.getInt(KEY_MAX_STREAK, 0)

        if (didWin) {
            winStreak += 1
            if (winStreak > maxStreak) maxStreak = winStreak

            if (guesses in 1..6) {
                val index = guesses - 1
                val currentCount = prefs.getInt(KEY_GUESS_DISTRIBUTION[index], 0)
                editor.putInt(KEY_GUESS_DISTRIBUTION[index], currentCount + 1)
            }
        } else {
            winStreak = 0 // reset streak on loss
        }

        editor.putInt(KEY_GAMES_PLAYED, gamesPlayed)
        editor.putInt(KEY_WIN_STREAK, winStreak)
        editor.putInt(KEY_MAX_STREAK, maxStreak)
        editor.apply()
    }
}
