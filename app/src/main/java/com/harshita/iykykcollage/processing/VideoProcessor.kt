package com.harshita.iykykcollage.processing

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.harshita.iykykcollage.ml.FaceEmbedder
import com.harshita.iykykcollage.ml.IdentityClusterer
import com.harshita.iykykcollage.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class VideoProcessor(private val context: Context) {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .setMinFaceSize(0.08f)
            .build()
    )

    suspend fun process(uri: Uri, onProgress: (ProcessingProgress) -> Unit): ProcessingResult = withContext(Dispatchers.Default) {
        onProgress(ProcessingProgress(0f, "Opening video"))
        val retriever = MediaMetadataRetriever().apply { setDataSource(context, uri) }
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            ?: error("Could not read video duration")
        val intervalMs = 200L // 5 fps; enough for short portrait clips.
        val timestamps = (0L until durationMs step intervalMs).toList()
        val observations = mutableListOf<FaceObservation>()

        FaceEmbedder(context).use { embedder ->
            timestamps.forEachIndexed { index, timestamp ->
                val frame = retriever.getFrameAtTime(timestamp * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                if (frame != null) {
                    val faces = detector.process(InputImage.fromBitmap(frame, 0)).await()
                    faces.forEach { face ->
                        val bounds = face.boundingBox
                        if (bounds.width() >= 70 && bounds.height() >= 70 && isMostlyInside(frame, bounds)) {
                            val embeddingCrop = BitmapCrops.faceInput(frame, bounds)
                            observations += FaceObservation(
                                timestampMs = timestamp,
                                portraitCandidate = BitmapCrops.generousPortrait(frame, bounds),
                                embedding = embedder.embed(embeddingCrop),
                                trackingId = face.trackingId,
                                yaw = face.headEulerAngleY,
                                pitch = face.headEulerAngleX,
                                roll = face.headEulerAngleZ,
                                leftEyeOpen = face.leftEyeOpenProbability,
                                rightEyeOpen = face.rightEyeOpenProbability,
                                smile = face.smilingProbability,
                                quality = ImageQuality.score(frame, face)
                            )
                        }
                    }
                }
                if (index % 3 == 0) onProgress(ProcessingProgress((index + 1f) / timestamps.size * 0.82f, "Finding faces • ${index + 1}/${timestamps.size}"))
            }
        }
        retriever.release()
        require(observations.isNotEmpty()) { "No clear faces were found in this video" }
        onProgress(ProcessingProgress(0.87f, "Grouping the same people"))
        val clusters = IdentityClusterer().cluster(observations)
        val people = clusters.mapIndexed { index, cluster ->
            val best = cluster.observations.maxBy { it.quality }
            PersonResult(index, AppearanceTracker.count(cluster.observations), best.portraitCandidate, cluster.observations.size)
        }.sortedByDescending { it.observations }.mapIndexed { index, person -> person.copy(id = index) }
        onProgress(ProcessingProgress(0.95f, "Designing collage"))
        val collage = CollageGenerator.create(people)
        onProgress(ProcessingProgress(1f, "Done"))
        ProcessingResult(people, collage, people.sumOf { it.appearanceCount }, timestamps.size)
    }

    fun close() = detector.close()

    private fun isMostlyInside(frame: Bitmap, bounds: android.graphics.Rect): Boolean {
        val marginX = bounds.width() * 0.08f; val marginY = bounds.height() * 0.08f
        return bounds.left >= -marginX && bounds.top >= -marginY && bounds.right <= frame.width + marginX && bounds.bottom <= frame.height + marginY
    }
}
