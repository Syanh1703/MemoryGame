package com.example.memorygame.models

//List out all the attributes of the cards
data class MemoryCard(
    //Capture the identifier of the card
    val identifier: Int,
    val cardUrl: String? = null,
    var isFacedUp: Boolean = false, //the card can be flipped, so it's var
    var isMatched: Boolean = false
)