package com.example.memorygame.models

import com.example.memorygame.utils.DEFAULT_ICONS

class MemoryGame(private val boardSize: BoardSize)
{
    val cards :List<MemoryCard>
    var numPairsFound = 0
    var foundMatch :Boolean = false

    private var indexOfSingleSelectedCards :Int? = null
    private var cardsFlipped = 0

    init {
        //Randomize the icons to the cars according to the pairs
        val chooseImages:List<Int> = DEFAULT_ICONS.shuffled().take(boardSize.getGamePairs())
        val randomizedImages = (chooseImages + chooseImages).shuffled()
        //Create a list of Memory Card
       cards  = randomizedImages.map { MemoryCard(it) }
    }

    fun flipCard(position :Int):Boolean
    {
        var cardFace = cards[position]
        /**
         * Three cases:
         * 0 cards previously flipped over => Flip over two cards if unmatched (restore cards) + flip the selected card
         * 1 card previously flipped over => Flip over the selected card + check if they have the same image
         * 2 cards previously flipped over => Flip over two cards if unmatched (restore cards) + flip the selected card
         */
        cardsFlipped++
        //Change the value of isFaceUp
        if(indexOfSingleSelectedCards == null)
        {
            //0 or 2 cards previously flipped over
            restoreCards()
            indexOfSingleSelectedCards = position//Save the id of the pressed card
        }
        else
        {
            //exactly 1 card previously flipped over
            foundMatch = checkIfMatched(indexOfSingleSelectedCards!!, position)
            indexOfSingleSelectedCards = null
        }
        cardFace.isFacedUp = !cardFace.isFacedUp
        return foundMatch
    }

    private fun restoreCards()
    {
        for(card in cards)
        {
            if(!card.isMatched)
            {
                card.isFacedUp = false
            }
        }
    }

    private fun checkIfMatched(pos1 :Int, pos2: Int):Boolean
    {
        if(cards[pos1].identifier != cards[pos2].identifier)
        {
            //Two cards are not matched
            return false
        }
        cards[pos1].isMatched = true
        cards[pos2].isMatched = true
        numPairsFound++
        return true
    }

    fun hasWonGame():Boolean
    {
        return numPairsFound == boardSize.getGamePairs()
    }

    fun isCardFacedUp(position: Int):Boolean
    {
        return cards[position].isFacedUp
    }

    fun getNumMoves():Int
    {
        //Moves counts up when two cards flipped => have number of cards flipped
        return cardsFlipped/2
    }
}