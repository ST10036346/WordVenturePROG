package com.st10036346.wordventure2

object MultiplayerScore {
    var player1Wins: Int = 0
    var player2Wins: Int = 0

    // A function to reset the score when the user goes back to the main menu.
    fun reset() {
        player1Wins = 0
        player2Wins = 0
    }
}