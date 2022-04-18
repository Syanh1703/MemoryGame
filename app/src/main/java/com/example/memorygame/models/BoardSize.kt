package com.example.memorygame.models

import com.example.memorygame.utils.TIMER_EASY
import com.example.memorygame.utils.TIMER_EX_HARD
import com.example.memorygame.utils.TIMER_HARD
import com.example.memorygame.utils.TIMER_MED

enum class BoardSize(val numCards: Int, val timeLimit:Long) {
    //Define number of cards in each level
    /**
     * Implement type-safe
     */
    EASY(8, TIMER_EASY),
    MEDIUM(18, TIMER_MED),
    HARD(24, TIMER_HARD),
    EXTREMELY_HARD(40, TIMER_EX_HARD);

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

    fun getTimer():Long
    {
        return timeLimit
    }

}