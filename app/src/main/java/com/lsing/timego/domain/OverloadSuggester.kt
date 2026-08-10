package com.lsing.timego.domain

data class SetPerformance(val weightKg: Double, val reps: Int, val targetReps: Int)

data class OverloadSuggestion(val weightKg: Double, val reps: Int, val note: String, val plateauStatus: PlateauStatus)

interface OverloadSuggester {
    fun suggestNext(history: List<SetPerformance>): OverloadSuggestion?
}

/** Deterministic, on-device, no ML -- see the v1 spec's "Recommendation Engine" section for why,
 *  and the 2026-08-11 suggester-plateau-upgrade-design spec for why this is the base layer a
 *  future ML model sits on top of rather than the model itself. Plateau status is computed from
 *  a 5-set rolling window of estimated 1RM via [classifyPlateauStatus] -- REGRESSING (last two
 *  sets missed target) still triggers the same 10% deload as before; PROGRESSING keeps the
 *  original hit-target/missed-target branches; PLATEAUING is new -- holds weight and reps flat
 *  for one more session instead of blindly adding weight into a stall. */
class RuleBasedOverloadSuggester : OverloadSuggester {
    override fun suggestNext(history: List<SetPerformance>): OverloadSuggestion? {
        if (history.isEmpty()) return null
        val last = history.last()
        val oneRepMaxes = history.map { estimatedOneRepMax(it.weightKg, it.reps) }
        val hitFlags = history.map { it.reps >= it.targetReps }
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
        }
    }
}
