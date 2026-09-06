package com.harshita.iykykcollage.processing

import android.graphics.Bitmap
import android.graphics.Rect

object BitmapCrops {
    fun faceInput(frame: Bitmap, face: Rect): Bitmap {
        val side = (maxOf(face.width(), face.height()) * 1.10f).toInt()
        val cx = face.centerX(); val cy = face.centerY()
        val left = (cx - side / 2).coerceIn(0, maxOf(0, frame.width - side))
        val top = (cy - side / 2).coerceIn(0, maxOf(0, frame.height - side))
        val width = minOf(side, frame.width - left); val height = minOf(side, frame.height - top)
        return Bitmap.createBitmap(frame, left, top, width, height)
    }

    fun generousPortrait(frame: Bitmap, face: Rect): Bitmap {
        val width = (face.width() * 2.2f).toInt().coerceAtMost(frame.width)
        val height = (width * 1.28f).toInt().coerceAtMost(frame.height)
        val left = (face.centerX() - width / 2).coerceIn(0, frame.width - width)
        val top = (face.centerY() - height * 0.38f).toInt().coerceIn(0, frame.height - height)
        val crop = Bitmap.createBitmap(frame, left, top, width, height)
        val targetHeight = minOf(540, crop.height)
        val targetWidth = (crop.width * (targetHeight.toFloat() / crop.height)).toInt()
        return if (crop.height > targetHeight) Bitmap.createScaledBitmap(crop, targetWidth, targetHeight, true) else crop
    }
}
