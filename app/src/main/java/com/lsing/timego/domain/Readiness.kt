package com.lsing.timego.domain

/** Self-rated readiness for a session, 1 (drained) to 5 (great). Only 1 and 2 change anything. */
fun readinessIsLow(readiness: Int?): Boolean = readiness != null && readiness <= 2

private const val LOW_READINESS_EXTRA_REST_SECONDS = 15

/** On a low-readiness day a suggestion never raises the weight: it is held at the last working weight. Higher
 *  readiness, no rating or no history leave the suggestion alone. The note says so, so the nudge is never silent. */
fun applyReadiness(suggestion: OverloadSuggestion, lastWorkingKg: Double?, readiness: Int?): OverloadSuggestion {
    if (!readinessIsLow(readiness) || lastWorkingKg == null || suggestion.weightKg <= lastWorkingKg) return suggestion
    return suggestion.copy(weightKg = lastWorkingKg, note = "Low readiness today: holding the weight. ${suggestion.note}")
}

/** A little longer rest on a low-readiness day. */
fun readinessRestSeconds(seconds: Int, readiness: Int?): Int =
    if (readinessIsLow(readiness)) seconds + LOW_READINESS_EXTRA_REST_SECONDS else seconds
