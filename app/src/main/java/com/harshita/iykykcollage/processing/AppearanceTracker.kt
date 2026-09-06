package com.harshita.iykykcollage.processing

import com.harshita.iykykcollage.model.FaceObservation

object AppearanceTracker {
    // At 5 fps, allow two missed frames without splitting one continuous appearance.
    const val MAX_GAP_MS = 650L

    fun count(observations: List<FaceObservation>): Int {
        val times = observations.map { it.timestampMs }.distinct().sorted()
        if (times.isEmpty()) return 0
        return 1 + times.zipWithNext().count { (previous, next) -> next - previous > MAX_GAP_MS }
    }
}
