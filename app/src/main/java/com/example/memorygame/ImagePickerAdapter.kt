package com.example.memorygame

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(
    private val context: Context,
    private val choseImageUri: List<Uri>,
    private val intentBoardSize: BoardSize,
    private val imagePicker: PickImage
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    interface PickImage {
        fun onPlaceHolderClick()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //Scale the image
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        val cardWidth = parent.width / intentBoardSize.getGameWidth()
        val cardHeight = parent.height / intentBoardSize.getGameHeight()
        val cardSize = min(cardHeight, cardWidth)
        val layoutParams =
            view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams as ViewGroup.LayoutParams
        layoutParams.width = cardSize
        layoutParams.height = cardSize
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        if (position < choseImageUri.size) {
            holder.bindAtThePresentPosition(choseImageUri[position])//The user has chosen the image
        } else {
            holder.bind() //The user did not choose the image
        }
    }

    override fun getItemCount(): Int {
        return intentBoardSize.getGamePairs()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val customImage = view.findViewById<ImageView>(R.id.ivCustomImage)
        fun bind() {
            customImage.setOnClickListener {
                /**
                 * Launch an intent for user to select photos
                 */
                imagePicker.onPlaceHolderClick()
            }
        }

        fun bindAtThePresentPosition(position: Uri) {
            customImage.setImageURI(position)
            customImage.setOnClickListener(null)//Unclickable
        }
    }
}
