package com.lsing.timego.domain.ai

import com.lsing.timego.data.ExerciseCategory
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

/**
 * Tracks user modality affinity (Calisthenics vs Strength vs Cardio) using
 * exponential recency decay and reinforcement rewards from session logs.
 */
class CategoryPreferenceLearner(
    private val halfLifeSessions: Double = 8.0,
    private val minFloor: Double = 0.2
) {
    private val affinities: MutableMap<ExerciseCategory, Double> = mutableMapOf(
        ExerciseCategory.CALISTHENICS to 1.0,
        ExerciseCategory.STRENGTH to 1.0,
        ExerciseCategory.CARDIO to 0.6
    )

    private val decayRate: Double = exp(-ln(2.0) / halfLifeSessions)

    /**
     * Ingests a logged exercise or an accepted recommendation.
     */
    fun recordInteraction(category: ExerciseCategory, reward: Double = 1.0) {
        // Only track trainable training categories
        if (category !in affinities.keys) return

        // Decay all categories
        for (cat in affinities.keys) {
            val decayed = (affinities[cat] ?: 1.0) * decayRate
            affinities[cat] = max(minFloor, decayed)
        }

        // Apply positive feedback reward
        val current = affinities[category] ?: 1.0
        affinities[category] = current + max(0.1, reward)
    }

    /**
     * Ingests a user swap (e.g. user dismissed a Barbell Bench suggestion and chose a Push-Up).
     */
    fun recordSwap(rejected: ExerciseCategory, accepted: ExerciseCategory) {
        if (rejected in affinities.keys) {
            val current = affinities[rejected] ?: 1.0
            affinities[rejected] = max(minFloor, current * 0.85)
        }
        recordInteraction(accepted, reward = 0.8)
    }

    /**
     * Normalized category distribution (sums to 1.0).
     */
    fun getProbabilities(): Map<ExerciseCategory, Double> {
        val total = affinities.values.sum()
        if (total <= 0.0) {
            return affinities.mapValues { 1.0 / affinities.size }
        }
        return affinities.mapValues { it.value / total }
    }

    fun getDominantLean(): ExerciseCategory {
        return affinities.maxByOrNull { it.value }?.key ?: ExerciseCategory.STRENGTH
    }

    fun getAffinity(category: ExerciseCategory): Double {
        return affinities[category] ?: 1.0
    }

    fun exportState(): Map<String, Double> {
        return affinities.mapKeys { it.key.name }
    }

    fun loadState(saved: Map<String, Double>) {
        saved.forEach { (catName, value) ->
            try {
                val cat = ExerciseCategory.valueOf(catName)
                if (cat in affinities.keys) {
                    affinities[cat] = max(minFloor, value)
                }
            } catch (_: IllegalArgumentException) {
                // Ignore unknown categories
            }
        }
    }
}
