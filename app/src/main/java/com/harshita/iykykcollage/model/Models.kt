package com.harshita.iykykcollage.model

import android.graphics.Bitmap

data class FaceObservation(
    val timestampMs: Long,
    val portraitCandidate: Bitmap,
    val embedding: FloatArray,
    val trackingId: Int?,
    val yaw: Float,
    val pitch: Float,
    val roll: Float,
    val leftEyeOpen: Float?,
    val rightEyeOpen: Float?,
    val smile: Float?,
    val quality: Float
)

data class PersonResult(
    val id: Int,
    val appearanceCount: Int,
    val representative: Bitmap,
    val observations: Int
)

data class ProcessingResult(
    val people: List<PersonResult>,
    val collage: Bitmap,
    val totalAppearances: Int,
    val framesScanned: Int
)

data class ProcessingProgress(
    val fraction: Float,
    val stage: String
)
