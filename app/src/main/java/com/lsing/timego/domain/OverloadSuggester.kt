package com.lsing.timego.domain

data class SetPerformance(val weightKg: Double, val reps: Int, val targetReps: Int)

data class OverloadSuggestion(val weightKg: Double, val reps: Int, val note: String)

interface OverloadSuggester {
    fun suggestNext(history: List<SetPerformance>): OverloadSuggestion?
}

/** Deterministic, on-device, no ML -- see the v1 spec's "Recommendation Engine" section for why.
 *  Deload triggers only on the last TWO logged sets both missing target reps, so a single off day
 *  doesn't force a weight drop. */
class RuleBasedOverloadSuggester : OverloadSuggester {
    override fun suggestNext(history: List<SetPerformance>): OverloadSuggestion? {
        if (history.isEmpty()) return null
        val last = history.last()
        val lastTwo = history.takeLast(2)
        val missedLastTwo = lastTwo.size == 2 && lastTwo.all { it.reps < it.targetReps }
        return when {
            missedLastTwo -> OverloadSuggestion(
                weightKg = last.weightKg * 0.9,
                reps = last.targetReps,
                note = "Deload: missed target reps twice in a row",
            )
            last.reps >= last.targetReps -> OverloadSuggestion(
                weightKg = last.weightKg + 2.5,
                reps = last.targetReps,
                note = "Increase weight: hit target reps last time",
            )
            else -> OverloadSuggestion(
                weightKg = last.weightKg,
                reps = last.reps + 1,
                note = "Same weight, aim for one more rep",
            )
        }
    }
}
