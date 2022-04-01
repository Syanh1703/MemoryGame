package com.example.memorygame

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.models.BoardSize
import com.example.memorygame.models.MemoryCard
import com.example.memorygame.utils.ACTIVITY
import com.squareup.picasso.Picasso
import kotlin.math.min

class ItemsAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    private val cardClickListener: CardClickListener
) : RecyclerView.Adapter<ItemsAdapter.ViewHolder>() {//It represents one single memory card

    companion object {
        const val CARD_MARGIN_SIZE = 8
    }

    //Create the interface to check the state of the card
    interface CardClickListener {
        fun onCardClick(position: Int)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageBtn = view.findViewById<ImageButton>(R.id.imgbCard)
        fun bind(position: Int) {
            val cardPos = cards[position]
            //Render the user images to the Image Button
            if (cardPos.isFacedUp) {
                if (cardPos.cardUrl != null) {
                    Picasso.get().load(cardPos.cardUrl).into(imageBtn)
                } else {
                    imageBtn.setImageResource(cardPos.identifier)
                }
            } else {
                //If faced down, set launcher background
                imageBtn.setImageResource(R.drawable.riddler)
            }
            //Update the UI
            imageBtn.alpha = if (cardPos.isMatched)//set the opacity
            {
                .4f
            } else {
                1.0f
            }

            //Gray out the background if matched
            val colorState = if (cardPos.isMatched) {
                ContextCompat.getColorStateList(context, R.color.gray_background)
            } else {
                null
            }
            ViewCompat.setBackgroundTintList(imageBtn, colorState)

            imageBtn.setOnClickListener {
                Log.i(ACTIVITY, "Click on position: $position")
                //Invoke the method in the interface
                cardClickListener.onCardClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //Set the width automatically
        val cardWidth = parent.width / boardSize.getGameWidth() - (2 * CARD_MARGIN_SIZE)
        val cardHeight = parent.height / boardSize.getGameHeight() - (2 * CARD_MARGIN_SIZE)
        val cardSize = min(cardHeight, cardWidth)
        val inflater = LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)
        //Set the dimension to the card View
        val layoutParams =
            inflater.findViewById<CardView>(R.id.cvElement).layoutParams as ViewGroup.MarginLayoutParams
        //Assume the card is square
        layoutParams.height = cardSize
        layoutParams.width = cardSize
        layoutParams.setMargins(
            CARD_MARGIN_SIZE,
            CARD_MARGIN_SIZE,
            CARD_MARGIN_SIZE,
            CARD_MARGIN_SIZE
        )
        return ViewHolder(inflater)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return boardSize.numCards
    }
}