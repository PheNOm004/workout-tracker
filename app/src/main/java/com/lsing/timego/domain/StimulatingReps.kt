package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import java.time.LocalDate

/** Stimulating reps in one set. [estimated] is true when no RPE was logged and a neutral guess stood in. */
data class StimulatingReps(val reps: Double, val estimated: Boolean)

/** Reps counted only within five reps of failure (the stimulating-reps model: a rep far from failure adds little
 *  hypertrophy stimulus). With RIR = 10 - RPE the set earns min(reps, 5 - RIR), kept within 0 to 5. This is a
 *  theory, not a settled fact, so every screen that shows it calls it an estimate. Warm-ups, holds and cardio
 *  return null; a missing RPE counts as two reps in reserve (min(reps, 3)) and is flagged as estimated. */
fun stimulatingReps(log: SetLog, exercise: Exercise): StimulatingReps? {
    if (log.isWarmup) return null
    if (exercise.category == ExerciseCategory.WARMUP.name || exercise.category == ExerciseCategory.CARDIO.name) return null
    if (exercise.loggingType != LoggingType.WEIGHT_REPS.name) return null
    val reps = log.reps.coerceAtLeast(0).toDouble()
    val rpe = log.rpe ?: return StimulatingReps(minOf(reps, 3.0), estimated = true)
    val repsInReserve = 10 - rpe.coerceIn(1, 10)
    return StimulatingReps((5 - repsInReserve).coerceIn(0, 5).toDouble().coerceAtMost(reps), estimated = false)
}

data class StimulusDistribution(
    val repsByGroup: Map<String, Double>,
    /** Share (0 to 1) of the counted sets whose effort was estimated because no RPE was logged. */
    val estimatedShare: Double,
)

/** Stimulating reps per muscle group on or after [since]. A set credits every muscle in the exercise's
 *  muscleGroups, scaled by its muscleWeights entry (100 when unlisted), like [muscleGroupEffectiveSetDistribution]. */
fun muscleGroupStimulatingReps(
    history: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sessionDateById: Map<Long, LocalDate>,
    since: LocalDate,
    until: LocalDate = LocalDate.MAX,
): StimulusDistribution {
    val repsByGroup = mutableMapOf<String, Double>()
    var counted = 0
    var estimated = 0
    for (log in history) {
        val exercise = exercisesById[log.exerciseId] ?: continue
        val date = sessionDateById[log.sessionId] ?: continue
        if (date.isBefore(since) || date.isAfter(until)) continue
        val stim = stimulatingReps(log, exercise) ?: continue
        counted++
        if (stim.estimated) estimated++
        for (group in exercise.muscleGroups) {
            repsByGroup[group] = (repsByGroup[group] ?: 0.0) + stim.reps * (exercise.muscleWeights[group] ?: 100) / 100.0
        }
    }
    return StimulusDistribution(repsByGroup, if (counted == 0) 0.0 else estimated.toDouble() / counted)
}

/** Heat intensities for the body map, from stimulating reps instead of effort-weighted sets. */
fun muscleStimulusHeatForTimeframe(
    timeframe: ProgressTimeframe,
    sessions: List<com.lsing.timego.data.WorkoutSession>,
    sets: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    today: LocalDate,
): Pair<Map<String, Float>, Double> {
    val since = timeframe.sinceDate(today)
    val distribution = muscleGroupStimulatingReps(sets, exercisesById, sessions.associate { it.id to it.date }, since)
    return normalizedHeat(distribution.repsByGroup) to distribution.estimatedShare
}
