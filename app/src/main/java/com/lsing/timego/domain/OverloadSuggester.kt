package com.lsing.timego.domain

data class SetPerformance(val weightKg: Double, val reps: Int, val targetReps: Int)

data class OverloadSuggestion(val weightKg: Double, val reps: Int, val note: String, val plateauStatus: PlateauStatus)

interface OverloadSuggester {
    fun suggestNext(sessionHistory: List<SetPerformance>, currentSessionWorkingSets: List<SetPerformance>): OverloadSuggestion?
}

/** Deterministic, on-device, no ML -- see the v1 spec's "Recommendation Engine" section for why,
 *  and the 2026-08-11 suggester-plateau-upgrade-design spec for why this is the base layer a
 *  future ML model sits on top of rather than the model itself. [sessionHistory] is one
 *  representative (last working) set per past session (see [sessionWorkingSetHistory]), not every
 *  raw set -- overload is a between-session decision. [currentSessionWorkingSets] non-empty means
 *  a working set has already been logged for this exercise this session: the suggestion locks to
 *  that session's *first* working set's weight/target (2026-08-12 warmup-session-aware-suggester
 *  design) rather than re-running the decision table, so a second/third set of the same exercise
 *  doesn't escalate further mid-session even if you deviate (e.g. a drop set) on a later one. */
class RuleBasedOverloadSuggester : OverloadSuggester {
    override fun suggestNext(sessionHistory: List<SetPerformance>, currentSessionWorkingSets: List<SetPerformance>): OverloadSuggestion? {
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
                weightKg = last.weightKg * 0.9,
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
            PlateauStatus.PROGRESSING -> if (last.reps >= last.targetReps) {
                OverloadSuggestion(
                    weightKg = last.weightKg + 2.5,
                    reps = last.targetReps,
                    note = "Increase weight: hit target reps last time",
                    plateauStatus = status,
                )
            } else {
                OverloadSuggestion(
                    weightKg = last.weightKg,
                    reps = last.reps + 1,
                    note = "Same weight, aim for one more rep",
                    plateauStatus = status,
                )
            }
            PlateauStatus.REPEATING -> error("classifyPlateauStatus never returns REPEATING")
        }
    }
}
