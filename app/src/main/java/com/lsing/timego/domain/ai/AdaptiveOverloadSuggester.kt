package com.lsing.timego.domain.ai

import com.lsing.timego.domain.DEFAULT_WEIGHT_INCREMENT_KG
import com.lsing.timego.domain.EscalationTier
import com.lsing.timego.domain.OverloadSuggester
import com.lsing.timego.domain.OverloadSuggestion
import com.lsing.timego.domain.PlateauStatus
import com.lsing.timego.domain.RepRange
import com.lsing.timego.domain.SetPerformance
import com.lsing.timego.domain.classifyPlateauStatus
import com.lsing.timego.domain.escalationTierForRpe
import com.lsing.timego.domain.estimatedOneRepMax
import com.lsing.timego.domain.roundDownToIncrement
import kotlin.math.abs

/**
 * Adaptive overload engine utilizing exponential smoothing (EMA) over historical performance,
 * RPE-adjusted capacity estimates, and mastery ceiling detection for progressive movement transitions.
 */
class AdaptiveOverloadSuggester(
    private val smoothingAlpha: Double = 0.35
) : OverloadSuggester {

    override fun suggestNext(
        sessionHistory: List<SetPerformance>,
        currentSessionWorkingSets: List<SetPerformance>,
        weightIncrementKg: Double?,
        repRange: RepRange?
    ): OverloadSuggestion? {
        // In-session lock: never escalate between working sets within the same session
        if (currentSessionWorkingSets.isNotEmpty()) {
            val locked = currentSessionWorkingSets.first()
            return OverloadSuggestion(
                weightKg = locked.weightKg,
                reps = locked.targetReps,
                note = "Repeating today's working weight",
                plateauStatus = PlateauStatus.REPEATING
            )
        }

        if (sessionHistory.isEmpty()) return null

        val last = sessionHistory.last()
        val oneRepMaxes = sessionHistory.map { estimatedOneRepMax(it.weightKg, it.reps) }
        val hitFlags = sessionHistory.map { it.reps >= it.targetReps }
        val status = classifyPlateauStatus(oneRepMaxes, hitFlags)

        // 1. Check for genuine regression / deload requirement
        if (status == PlateauStatus.REGRESSING) {
            val deloadFactor = 0.90
            val deloaded = (last.weightKg * deloadFactor).let {
                if (weightIncrementKg == null) it else roundDownToIncrement(it, weightIncrementKg)
            }
            return OverloadSuggestion(
                weightKg = deloaded,
                reps = last.targetReps,
                note = "Deload: recovery dip detected across consecutive sessions",
                plateauStatus = status
            )
        }

        // 2. Compute Exponential Moving Average of capacity to smooth out single-session noise
        var ema1RM = oneRepMaxes.first()
        for (i in 1 until oneRepMaxes.size) {
            ema1RM = (smoothingAlpha * oneRepMaxes[i]) + ((1.0 - smoothingAlpha) * ema1RM)
        }

        val last1RM = oneRepMaxes.last()
        val trendSlope = last1RM - ema1RM

        // 3. Check for Plateau status
        if (status == PlateauStatus.PLATEAUING || (sessionHistory.size >= 4 && abs(trendSlope) < 0.25 && !hitFlags.last())) {
            return OverloadSuggestion(
                weightKg = last.weightKg,
                reps = last.targetReps,
                note = "Performance holding steady; maintain load for adaptation",
                plateauStatus = PlateauStatus.PLATEAUING
            )
        }

        // 4. Progressing: evaluate overload vector (reps vs weight vs mastery)
        val stepKg = weightIncrementKg ?: DEFAULT_WEIGHT_INCREMENT_KG

        if (repRange == null) {
            return if (last.reps >= last.targetReps) {
                OverloadSuggestion(
                    weightKg = last.weightKg + stepKg,
                    reps = last.targetReps,
                    note = "Target reps achieved -- increasing load",
                    plateauStatus = status
                )
            } else {
                OverloadSuggestion(
                    weightKg = last.weightKg,
                    reps = last.reps + 1,
                    note = "Same load, aim for one more rep",
                    plateauStatus = status
                )
            }
        }

        // With repRange: Check if below ceiling
        if (last.reps < repRange.ceiling) {
            return OverloadSuggestion(
                weightKg = last.weightKg,
                reps = last.reps + 1,
                note = "Building volume: target one more rep toward ceiling (${repRange.ceiling})",
                plateauStatus = status
            )
        }

        // At or above ceiling: check RPE
        return when (escalationTierForRpe(last.rpe)) {
            EscalationTier.FULL -> {
                val isBodyweight = weightIncrementKg == null
                val nextWeight = if (isBodyweight) last.weightKg else last.weightKg + stepKg
                val nextReps = if (isBodyweight) repRange.ceiling + 1 else repRange.ceiling
                val note = if (isBodyweight) {
                    "Ceiling hit with reserve: push volume (+1 rep) or ready for progression upgrade"
                } else {
                    "Ceiling hit with reserve: escalating weight +$stepKg kg"
                }
                OverloadSuggestion(
                    weightKg = nextWeight,
                    reps = nextReps,
                    note = note,
                    plateauStatus = status
                )
            }
            EscalationTier.PARTIAL -> {
                val halfStep = if (weightIncrementKg == null) stepKg / 2.0 else roundDownToIncrement(stepKg / 2.0, DEFAULT_WEIGHT_INCREMENT_KG)
                if (halfStep <= 0.0) {
                    OverloadSuggestion(
                        weightKg = last.weightKg,
                        reps = repRange.ceiling,
                        note = "Ceiling reached near limit: solidify at this weight",
                        plateauStatus = status
                    )
                } else {
                    OverloadSuggestion(
                        weightKg = last.weightKg + halfStep,
                        reps = repRange.ceiling,
                        note = "Ceiling reached with moderate effort: small load increase",
                        plateauStatus = status
                    )
                }
            }
            EscalationTier.HOLD -> OverloadSuggestion(
                weightKg = last.weightKg,
                reps = repRange.ceiling,
                note = "Ceiling hit at high effort: consolidate form before escalating",
                plateauStatus = status
            )
        }
    }
}
