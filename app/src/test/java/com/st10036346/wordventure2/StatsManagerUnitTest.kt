package com.st10036346.wordventure2

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

/**
 * Unit tests for StatsManager logic, using Mockito to simulate SharedPreferences behavior.
 * This test uses the new key structure and update sequence defined in the current StatsManager.kt.
 */
class StatsManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var statsManager: StatsManager

    // CRITICAL FIX: Define a mock user ID, as the StatsManager constructor now requires it.
    private val MOCK_USER_ID = "test_user_id_123"

    // Replicate keys used in StatsManager.kt for mocking
    private val KEY_GAMES_PLAYED = "games_played"
    private val KEY_WIN_STREAK = "win_streak"
    private val KEY_MAX_STREAK = "max_streak"

    // Keys for the first two distribution slots (for focused testing)
    private val KEY_GUESS_DISTRIBUTION_1 = "guess_dist_1"
    private val KEY_GUESS_DISTRIBUTION_2 = "guess_dist_2"

    @Before
    fun setUp() {
        // 1. Mock the Android dependencies (Context, SharedPreferences, and Editor)
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        // 2. Define the behavior of the mocks
        // When getSharedPreferences is called, return our mockPrefs
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        // When edit() is called on the preferences, return our mockEditor
        `when`(mockPrefs.edit()).thenReturn(mockEditor)

        // Make the editor mock chainable for all put operations (putInt, putString, etc.)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        // Stubbing apply() so it does nothing but allows the call to complete
        `when`(mockEditor.apply()).then { }

        // 3. Set up the default initial state for 'getInt' calls (simulating fresh start)
        `when`(mockPrefs.getInt(anyString(), anyInt())).thenReturn(0)

        // 4. Initialize the StatsManager with the mocked Context AND the required userId.
        statsManager = StatsManager(mockContext, MOCK_USER_ID)
    }

    // --- TEST SUITE FOR updateStats ---

    /**
     * Test a game win on the first guess (guesses = 1).
     * This verifies streak increase, max streak update, and distribution update.
     */
    @Test
    fun updateStats_winOnFirstGuess_updatesCorrectly() {
        // Setup initial state: all 0
        `when`(mockPrefs.getInt(KEY_GAMES_PLAYED, 0)).thenReturn(0)
        `when`(mockPrefs.getInt(KEY_WIN_STREAK, 0)).thenReturn(0)
        `when`(mockPrefs.getInt(KEY_MAX_STREAK, 0)).thenReturn(0)
        `when`(mockPrefs.getInt(KEY_GUESS_DISTRIBUTION_1, 0)).thenReturn(0)

        // Act: Win the game in 1 guess
        statsManager.updateStats(didWin = true, guesses = 1)

        // Verify DISTRIBUTION update first (uses its own apply)
        verify(mockEditor).putInt(KEY_GUESS_DISTRIBUTION_1, 1) // 0 + 1

        // Verify BASIC STATS update (uses the second apply)
        verify(mockEditor).putInt(KEY_GAMES_PLAYED, 1)
        verify(mockEditor).putInt(KEY_WIN_STREAK, 1)
        verify(mockEditor).putInt(KEY_MAX_STREAK, 1)

        // Ensure apply() was called at least twice (once for dist, once for stats)
        verify(mockEditor, times(2)).apply()
    }

    /**
     * Test a game loss.
     * This verifies game count increase and streak reset.
     */
    @Test
    fun updateStats_loss_resetsWinStreakAndIncreasesPlayed() {
        // Setup initial state: non-zero streak
        `when`(mockPrefs.getInt(KEY_GAMES_PLAYED, 0)).thenReturn(5)
        `when`(mockPrefs.getInt(KEY_WIN_STREAK, 0)).thenReturn(3)
        `when`(mockPrefs.getInt(KEY_MAX_STREAK, 0)).thenReturn(5)

        // Act: Lose the game
        statsManager.updateStats(didWin = false, guesses = 0)

        // Verify BASIC STATS update
        verify(mockEditor).putInt(KEY_GAMES_PLAYED, 6) // 5 + 1
        verify(mockEditor).putInt(KEY_WIN_STREAK, 0)  // Streak reset
        verify(mockEditor).putInt(KEY_MAX_STREAK, 5)  // Max streak remains the same

        // Ensure the distribution key was NOT updated when losing
        verify(mockEditor, never()).putInt(matches("guess_dist_\\d"), anyInt())
    }

    /**
     * Test increasing the win streak beyond the current max streak.
     */
    @Test
    fun updateStats_increasesMaxStreak() {
        // Setup initial state
        `when`(mockPrefs.getInt(KEY_WIN_STREAK, 0)).thenReturn(4)
        `when`(mockPrefs.getInt(KEY_MAX_STREAK, 0)).thenReturn(4)

        // Act: Win the game
        statsManager.updateStats(didWin = true, guesses = 2)

        // Verify BASIC STATS update: Both streak and max streak should become 5
        verify(mockEditor).putInt(KEY_WIN_STREAK, 5)
        verify(mockEditor).putInt(KEY_MAX_STREAK, 5)
    }

    /**
     * Test a win in 2 guesses with existing stats.
     */
    @Test
    fun updateStats_winOnSecondGuess_updatesDistributionCorrectly() {
        // Setup initial distribution for 2 guesses
        `when`(mockPrefs.getInt(KEY_GUESS_DISTRIBUTION_2, 0)).thenReturn(10)

        // Act: Win the game in 2 guesses
        statsManager.updateStats(didWin = true, guesses = 2)

        // Verify DISTRIBUTION update
        verify(mockEditor).putInt(KEY_GUESS_DISTRIBUTION_2, 11) // 10 + 1
    }

    // --- TEST SUITE FOR getStats ---

    /**
     * Test that getStats returns the correct GameStats object based on SharedPreferences.
     */
    @Test
    fun getStats_loadsCorrectData() {
        // Setup SharedPreferences to return specific values
        `when`(mockPrefs.getInt(KEY_GAMES_PLAYED, 0)).thenReturn(10)
        `when`(mockPrefs.getInt(KEY_WIN_STREAK, 0)).thenReturn(5)
        `when`(mockPrefs.getInt(KEY_MAX_STREAK, 0)).thenReturn(7)
        `when`(mockPrefs.getInt(KEY_GUESS_DISTRIBUTION_1, 0)).thenReturn(2)
        `when`(mockPrefs.getInt(KEY_GUESS_DISTRIBUTION_2, 0)).thenReturn(3)
        // All other distribution slots default to 0, which is set in setUp()

        // Act
        val stats = statsManager.getStats()

        // Assert
        assertEquals(10, stats.gamesPlayed)
        assertEquals(5, stats.winStreak)
        assertEquals(7, stats.maxStreak)
        assertArrayEquals(intArrayOf(2, 3, 0, 0, 0, 0), stats.guessDistribution)
    }
}
