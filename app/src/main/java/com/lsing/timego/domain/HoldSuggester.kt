package com.lsing.timego.domain

data class HoldPerformance(val durationSeconds: Int, val targetDurationSeconds: Int)

data class HoldSuggestion(val targetDurationSeconds: Int, val note: String)

interface HoldSuggester {
    fun suggestNext(history: List<HoldPerformance>): HoldSuggestion?
}

/** Same deterministic, on-device, no-ML philosophy as [RuleBasedOverloadSuggester], applied to
 *  timed holds instead of weight+reps. Deload triggers only on the last TWO logged holds both
 *  missing target duration, so a single off day doesn't force a target drop. */
class RuleBasedHoldSuggester : HoldSuggester {
    override fun suggestNext(history: List<HoldPerformance>): HoldSuggestion? {
        if (history.isEmpty()) return null
        val last = history.last()
        val lastTwo = history.takeLast(2)
        val missedLastTwo = lastTwo.size == 2 && lastTwo.all { it.durationSeconds < it.targetDurationSeconds }
        return when {
            missedLastTwo -> HoldSuggestion(
                targetDurationSeconds = (last.targetDurationSeconds * 0.9).toInt(),
                note = "Deload: missed target hold twice in a row",
            )
            last.durationSeconds >= last.targetDurationSeconds -> HoldSuggestion(
                targetDurationSeconds = last.targetDurationSeconds + 5,
                note = "Increase hold: hit target last time",
            )
            else -> HoldSuggestion(
                targetDurationSeconds = last.targetDurationSeconds,
                note = "Same target, aim to hold longer",
            )
        }
    }
}
