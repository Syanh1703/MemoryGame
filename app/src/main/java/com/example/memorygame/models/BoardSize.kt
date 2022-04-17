package com.example.memorygame.models

import android.os.CountDownTimer

enum class BoardSize(val numCards: Int) {
    //Define number of cards in each level
    /**
     * Implement type-safe
     */
    EASY(8),
    MEDIUM(18),
    HARD(24),
    EXTREMELY_HARD(40);

    companion object {
        fun getCardsByValue(value: Any) = values().first { it.numCards == value }
    }

    //Adjust the number of columns depend on the level
    fun getGameWidth(): Int {
        return when (this) {
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4
            EXTREMELY_HARD -> 5
        }
    }

    //Adjust the number of rows depend on the level
    fun getGameHeight(): Int {
        return numCards / getGameWidth()
    }

    //Determine the pairs of card depend on each level
    fun getGamePairs(): Int {
        return numCards / 2
    }

}