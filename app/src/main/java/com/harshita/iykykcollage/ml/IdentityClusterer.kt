package com.harshita.iykykcollage.ml

import com.harshita.iykykcollage.model.FaceObservation

class IdentityClusterer(private val threshold: Float = 0.60f) {

    data class Cluster(
        val observations: MutableList<FaceObservation>,
        var centroid: FloatArray
    )

    fun cluster(observations: List<FaceObservation>): List<Cluster> {
        val clusters = mutableListOf<Cluster>()

        observations.sortedBy { it.timestampMs }.forEach { observation ->
            val best = clusters
                .filterNot { cluster ->
                    cluster.observations.any {
                        it.timestampMs == observation.timestampMs
                    }
                }
                .map { it to cosine(it.centroid, observation.embedding) }
                .maxByOrNull { it.second }

            if (best == null || best.second < threshold) {
                clusters += Cluster(
                    mutableListOf(observation),
                    observation.embedding.copyOf()
                )
            } else {
                best.first.observations += observation
                best.first.centroid = meanEmbedding(best.first.observations)
            }
        }

        return mergeNearDuplicates(clusters)
    }

    private fun mergeNearDuplicates(
        input: MutableList<Cluster>
    ): List<Cluster> {
        var changed = true

        while (changed) {
            changed = false

            loop@ for (i in input.indices) {
                for (j in i + 1 until input.size) {
                    val appearTogether = input[i].observations.any { first ->
                        input[j].observations.any { second ->
                            first.timestampMs == second.timestampMs
                        }
                    }

                    if (
                        !appearTogether &&
                        cosine(input[i].centroid, input[j].centroid) >= 0.495f
                    ) {
                        input[i].observations += input[j].observations
                        input[i].centroid =
                            meanEmbedding(input[i].observations)
                        input.removeAt(j)
                        changed = true
                        break@loop
                    }
                }
            }
        }

        return input
    }

    private fun meanEmbedding(
        items: List<FaceObservation>
    ): FloatArray {
        val mean = FloatArray(items.first().embedding.size)

        items.forEach { item ->
            item.embedding.forEachIndexed { index, value ->
                mean[index] += value
            }
        }

        for (index in mean.indices) {
            mean[index] /= items.size
        }

        return mean
    }

    private fun cosine(
        first: FloatArray,
        second: FloatArray
    ): Float {
        return first.indices
            .sumOf { (first[it] * second[it]).toDouble() }
            .toFloat()
    }
}