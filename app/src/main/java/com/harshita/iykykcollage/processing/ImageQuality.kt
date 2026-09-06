package com.harshita.iykykcollage.processing

import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face
import kotlin.math.abs

object ImageQuality {
    fun score(frame: Bitmap, face: Face): Float {
        val b = face.boundingBox
        val margin = minOf(b.left, b.top, frame.width - b.right, frame.height - b.bottom)
        val visibility = (margin / (b.width() * 0.25f)).coerceIn(0f, 1f)
        val frontality = (1f - (abs(face.headEulerAngleY) / 35f + abs(face.headEulerAngleX) / 30f) / 2f).coerceIn(0f, 1f)
        val eyeValues = listOfNotNull(face.leftEyeOpenProbability, face.rightEyeOpenProbability)
        val eyes = if (eyeValues.isEmpty()) 0.7f else eyeValues.average().toFloat()
        val smile = face.smilingProbability ?: 0.5f
        val sharpness = laplacianLikeVariance(frame, b.left, b.top, b.right, b.bottom)
        return 0.30f * frontality + 0.25f * sharpness + 0.20f * eyes + 0.10f * smile + 0.15f * visibility
    }

    private fun laplacianLikeVariance(bitmap: Bitmap, l: Int, t: Int, r: Int, b: Int): Float {
        val left = l.coerceIn(0, bitmap.width - 1)
        val top = t.coerceIn(0, bitmap.height - 1)
        val right = r.coerceIn(left + 1, bitmap.width)
        val bottom = b.coerceIn(top + 1, bitmap.height)
        val step = maxOf(2, minOf(right - left, bottom - top) / 40)
        var sum = 0.0; var sumSq = 0.0; var count = 0
        for (y in top + step until bottom - step step step) for (x in left + step until right - step step step) {
            fun gray(px: Int): Int = (((px shr 16) and 255) * 30 + ((px shr 8) and 255) * 59 + (px and 255) * 11) / 100
            val c = gray(bitmap.getPixel(x, y))
            val edge = 4 * c - gray(bitmap.getPixel(x-step,y)) - gray(bitmap.getPixel(x+step,y)) - gray(bitmap.getPixel(x,y-step)) - gray(bitmap.getPixel(x,y+step))
            sum += edge; sumSq += edge * edge; count++
        }
        if (count == 0) return 0f
        val variance = sumSq / count - (sum / count) * (sum / count)
        return (variance / 1200.0).toFloat().coerceIn(0f, 1f)
    }
}
