package com.lsing.timego.domain

data class SetPerformance(val weightKg: Double, val reps: Int, val targetReps: Int, val rpe: Int? = null)

data class OverloadSuggestion(val weightKg: Double, val reps: Int, val note: String, val plateauStatus: PlateauStatus)

interface OverloadSuggester {
    fun suggestNext(
        sessionHistory: List<SetPerformance>,
        currentSessionWorkingSets: List<SetPerformance>,
        weightIncrementKg: Double? = DEFAULT_WEIGHT_INCREMENT_KG,
        repRange: RepRange? = null,
    ): OverloadSuggestion?
}

/** Smallest loadable step on a barbell: one 1.25 kg plate per side. */
const val DEFAULT_WEIGHT_INCREMENT_KG = 2.5

/** Rounds a suggested load down to something actually loadable. Down, not nearest, so a deload
 *  never rounds back up into the weight that was just missed. */
fun roundDownToIncrement(weightKg: Double, incrementKg: Double): Double =
    if (incrementKg <= 0.0) weightKg else Math.floor(weightKg / incrementKg) * incrementKg

/** Deterministic, on-device, no ML -- see the v1 spec's "Recommendation Engine" section for why,
 *  and the 2026-08-11 suggester-plateau-upgrade-design spec for why this is the base layer a
 *  future ML model sits on top of rather than the model itself. [sessionHistory] is one
 *  representative (last working) set per past session (see [sessionWorkingSetHistory]), not every
 *  raw set -- overload is a between-session decision. [currentSessionWorkingSets] non-empty means
 *  a working set has already been logged for this exercise this session: the suggestion locks to
 *  that session's *first* working set's weight/target (2026-08-12 warmup-session-aware-suggester
 *  design) rather than re-running the decision table, so a second/third set of the same exercise
 *  doesn't escalate further mid-session even if you deviate (e.g. a drop set) on a later one.
 *  [weightIncrementKg] null disables plate rounding entirely -- for CALISTHENICS the stored
 *  weightKg is bodyweight + added k, and bodyweight is not a loadable quantity, so snapping the
 *  total to a plate multiple would silently distort the added-k the user actually sees. */
class RuleBasedOverloadSuggester : OverloadSuggester {
    override fun suggestNext(
        sessionHistory: List<SetPerformance>,
        currentSessionWorkingSets: List<SetPerformance>,
        weightIncrementKg: Double?,
        repRange: RepRange?,
    ): OverloadSuggestion? {
        if (currentSessionWorkingSets.isNotEmpty()) {
            val locked = currentSessionWorkingSets.first()
            return OverloadSuggestion(
                weightKg = locked.weightKg,
                reps = locked.targetReps,
                note = "Repeating today's working weight",
                plateauStatus = PlateauStatus.REPEATING,
            )
        }
        if (sessionHistory.isEmpty()) return null
        val last = sessionHistory.last()
        val oneRepMaxes = sessionHistory.map { estimatedOneRepMax(it.weightKg, it.reps) }
        val hitFlags = sessionHistory.map { it.reps >= it.targetReps }
        val status = classifyPlateauStatus(oneRepMaxes, hitFlags)

        return when (status) {
            PlateauStatus.REGRESSING -> OverloadSuggestion(
                weightKg = (last.weightKg * 0.9).let { deloaded ->
                    if (weightIncrementKg == null) deloaded else roundDownToIncrement(deloaded, weightIncrementKg)
                },
                reps = last.targetReps,
                note = "Deload: missed target reps twice in a row",
                plateauStatus = status,
            )
            PlateauStatus.PLATEAUING -> OverloadSuggestion(
                weightKg = last.weightKg,
                reps = last.targetReps,
                note = "Plateau: performance has been flat for several sessions -- hold steady one more session before deciding",
                plateauStatus = status,
            )
            PlateauStatus.PROGRESSING -> when {
                repRange == null -> if (last.reps >= last.targetReps) {
                    fullEscalation(last, weightIncrementKg, status, "Increase weight: hit target reps last time", last.targetReps)
                } else {
                    addOneRep(last, status)
                }
                last.reps < repRange.ceiling -> addOneRep(last, status)
                else -> when (escalationTierForRpe(last.rpe)) {
                    EscalationTier.FULL -> fullEscalation(last, weightIncrementKg, status, "Ceiling hit with reps in reserve -- increasing weight.", repRange.ceiling)
                    EscalationTier.PARTIAL -> partialEscalation(last, weightIncrementKg, status, repRange)
                    EscalationTier.HOLD -> holdAtCeiling(last, status, repRange)
                }
            }
            PlateauStatus.REPEATING -> error("classifyPlateauStatus never returns REPEATING")
        }
    }
}

private fun fullEscalation(last: SetPerformance, weightIncrementKg: Double?, status: PlateauStatus, note: String, reps: Int): OverloadSuggestion =
    OverloadSuggestion(
        weightKg = last.weightKg + (weightIncrementKg ?: DEFAULT_WEIGHT_INCREMENT_KG),
        reps = reps,
        note = note,
        plateauStatus = status,
    )

private fun addOneRep(last: SetPerformance, status: PlateauStatus): OverloadSuggestion =
    OverloadSuggestion(
        weightKg = last.weightKg,
        reps = last.reps + 1,
        note = "Same weight, aim for one more rep",
        plateauStatus = status,
    )

private fun holdAtCeiling(last: SetPerformance, status: PlateauStatus, repRange: RepRange): OverloadSuggestion =
    OverloadSuggestion(
        weightKg = last.weightKg,
        reps = repRange.ceiling,
        note = "Ceiling hit at max effort -- hold before adding load.",
        plateauStatus = status,
    )

/** Half of the smallest loadable barbell step (2.5kg) rounds down to zero, so PARTIAL collapses
 *  into a HOLD outcome for any exercise using the default increment -- correct real-world
 *  behavior, not a bug: you cannot load half of the smallest available plate jump. Calisthenics
 *  (weightIncrementKg == null) has no plate constraint, so its half-step is used unrounded, same
 *  opt-out convention as deload rounding. */
private fun partialEscalation(last: SetPerformance, weightIncrementKg: Double?, status: PlateauStatus, repRange: RepRange): OverloadSuggestion {
    val fullStep = weightIncrementKg ?: DEFAULT_WEIGHT_INCREMENT_KG
    val halfStep = if (weightIncrementKg == null) fullStep / 2 else roundDownToIncrement(fullStep / 2, DEFAULT_WEIGHT_INCREMENT_KG)
    return if (halfStep <= 0.0) {
        holdAtCeiling(last, status, repRange)
    } else {
        OverloadSuggestion(
            weightKg = last.weightKg + halfStep,
            reps = repRange.ceiling,
            note = "Ceiling hit but close to your limit -- small increase.",
            plateauStatus = status,
        )
    }
}
