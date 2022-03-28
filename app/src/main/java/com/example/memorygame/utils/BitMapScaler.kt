package com.example.memorygame.utils

import android.graphics.Bitmap

object BitMapScaler
{
    fun scaleWidth(bitmap: Bitmap, width:Int):Bitmap
    {
        val factor = (width/bitmap.width).toFloat()
        return Bitmap.createScaledBitmap(bitmap,width, (bitmap.height*factor).toInt(),true)
    }

    fun scaleHeight(bitmap: Bitmap, height: Int): Bitmap {
        val factor = height / bitmap.height.toFloat()
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * factor).toInt(), height, true)
    }
}