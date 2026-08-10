package com.lsing.timego.domain

data class HoldPerformance(val durationSeconds: Int, val targetDurationSeconds: Int)

data class HoldSuggestion(val targetDurationSeconds: Int, val note: String, val plateauStatus: PlateauStatus)

interface HoldSuggester {
    fun suggestNext(history: List<HoldPerformance>, exerciseName: String): HoldSuggestion?
}

/** Static, hand-curated progression chain for the tiered HOLD-type calisthenics families added in
 *  the 2026-08-11 library import (Planche/Front Lever/Back Lever/Human Flag) -- not derived from
 *  muscle tags or any naming heuristic, since most of the library isn't tiered. Every HOLD
 *  exercise is CALISTHENICS (enforced by SeedExercisesTest's "every HOLD exercise is CALISTHENICS"
 *  invariant), so no separate category check is needed before consulting this map. */
private val CALISTHENICS_PROGRESSIONS: Map<String, String> = mapOf(
    "Tuck Planche Hold" to "Advanced Tuck Planche Hold",
    "Advanced Tuck Planche Hold" to "Straddle Planche Hold",
    "Straddle Planche Hold" to "Full Planche Hold",
    "Tuck Front Lever" to "Advanced Tuck Front Lever",
    "Advanced Tuck Front Lever" to "Straddle Front Lever",
    "Straddle Front Lever" to "Front Lever Hold",
    "Tuck Back Lever" to "Straddle Back Lever",
    "Straddle Back Lever" to "Back Lever Hold",
    "Human Flag Tuck" to "Human Flag Straddle",
    "Human Flag Straddle" to "Human Flag Hold",
)

private const val PROGRESSION_CEILING_RATIO = 1.5

/** Same deterministic, no-ML philosophy as [RuleBasedOverloadSuggester], applied to timed holds.
 *  Plateau status is computed from a 5-hold rolling window via [classifyPlateauStatus]. When the
 *  last hold clears [PROGRESSION_CEILING_RATIO]x its target and [exerciseName] has a known next
 *  tier in [CALISTHENICS_PROGRESSIONS], suggests switching to that tier instead of a duration bump
 *  -- the user rarely adds external load to calisthenics, so a duration/weight increase isn't the
 *  natural ceiling-buster here the way it is for STRENGTH exercises. */
class RuleBasedHoldSuggester : HoldSuggester {
    override fun suggestNext(history: List<HoldPerformance>, exerciseName: String): HoldSuggestion? {
        if (history.isEmpty()) return null
        val last = history.last()
        val durations = history.map { it.durationSeconds.toDouble() }
        val hitFlags = history.map { it.durationSeconds >= it.targetDurationSeconds }
        val status = classifyPlateauStatus(durations, hitFlags)

        val nextTier = CALISTHENICS_PROGRESSIONS[exerciseName]
        val clearedCeiling = last.durationSeconds >= last.targetDurationSeconds * PROGRESSION_CEILING_RATIO
        if (status != PlateauStatus.REGRESSING && clearedCeiling && nextTier != null) {
            return HoldSuggestion(
                targetDurationSeconds = last.targetDurationSeconds,
                note = "Consistently hitting target -- try $nextTier next",
                plateauStatus = status,
            )
        }

        return when (status) {
            PlateauStatus.REGRESSING -> HoldSuggestion(
                targetDurationSeconds = (last.targetDurationSeconds * 0.9).toInt(),
                note = "Deload: missed target hold twice in a row",
                plateauStatus = status,
            )
            PlateauStatus.PLATEAUING -> HoldSuggestion(
                targetDurationSeconds = last.targetDurationSeconds,
                note = "Plateau: hold duration has been flat for several sessions -- hold steady one more session before deciding",
                plateauStatus = status,
            )
            PlateauStatus.PROGRESSING -> if (last.durationSeconds >= last.targetDurationSeconds) {
                HoldSuggestion(
                    targetDurationSeconds = last.targetDurationSeconds + 5,
                    note = "Increase hold: hit target last time",
                    plateauStatus = status,
                )
            } else {
                HoldSuggestion(
                    targetDurationSeconds = last.targetDurationSeconds,
                    note = "Same target, aim to hold longer",
                    plateauStatus = status,
                )
            }
        }
    }
}
