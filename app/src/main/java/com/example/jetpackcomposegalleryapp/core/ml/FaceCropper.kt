package com.example.jetpackcomposegalleryapp.core.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face
import androidx.core.graphics.scale

object FaceCropper {

    fun cropFace(originalBitmap: Bitmap, face: Face,margin:Float = .2f): Bitmap? {
        val boundingBox = face.boundingBox

        val width = boundingBox.width()
        val height = boundingBox.height()

        val paddingX = (width*margin).toInt()
        val paddingY = (height*margin).toInt()



        val left = (boundingBox.left-paddingX).coerceIn(0, originalBitmap.width - 1)
        val top = (boundingBox.top-paddingY).coerceIn(0, originalBitmap.height - 1)
        val right = (boundingBox.right+paddingX).coerceIn(left + 1, originalBitmap.width)
        val bottom = (boundingBox.bottom+paddingY).coerceIn(top + 1, originalBitmap.height)

        return try {
            Bitmap.createBitmap(
                originalBitmap,
                left,
                top,
                right - left,
                bottom - top
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun preprocessForEmbedding(faceBitmap: Bitmap): Bitmap {
        return faceBitmap.scale(112, 112)
    }
}