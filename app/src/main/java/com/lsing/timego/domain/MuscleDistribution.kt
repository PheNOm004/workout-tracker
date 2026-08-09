package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import java.time.LocalDate

/** Total volume (weight * reps) per muscle group across STRENGTH/CALISTHENICS sets logged on or
 *  after [since] -- an exercise contributes its full volume to every muscle group it's tagged
 *  with (e.g. a squat set counts toward both QUADS and GLUTES). Backs the "muscle distribution"
 *  radar chart. CARDIO/WARMUP excluded, same reasoning as personalRecords/muscleGroupStrengthCurve
 *  -- their weightKg/reps are 0.0/0 sentinels, not real values. */
fun muscleGroupVolumeDistribution(
    history: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sessionDateById: Map<Long, LocalDate>,
    since: LocalDate,
): Map<String, Double> {
    val volumeByGroup = mutableMapOf<String, Double>()
    for (log in history) {
        val exercise = exercisesById[log.exerciseId] ?: continue
        if (exercise.category !in STRENGTH_CATEGORIES) continue
        val date = sessionDateById[log.sessionId] ?: continue
        if (date.isBefore(since)) continue
        val volume = log.weightKg * log.reps
        for (group in exercise.muscleGroups) {
            volumeByGroup[group] = (volumeByGroup[group] ?: 0.0) + volume
        }
    }
    return volumeByGroup
}

data class TrainingStats(
    val workouts: Int,
    val totalDurationMinutes: Double,
    val totalVolumeKg: Double,
    val totalSets: Int,
)

/** [totalDurationMinutes] is an estimate -- sessions have no explicit start/end time, so it's the
 *  span between each session's first and last logged set timestamp, summed across sessions. A
 *  session with only one set contributes 0 (no span to measure). */
fun trainingStats(sessions: List<WorkoutSession>, sets: List<SetLog>, since: LocalDate): TrainingStats {
    val sessionIdsSince = sessions.filter { !it.date.isBefore(since) }.map { it.id }.toSet()
    val setsSince = sets.filter { it.sessionId in sessionIdsSince }
    val durationMinutes = setsSince.groupBy { it.sessionId }
        .values
        .sumOf { sessionSets ->
            val timestamps = sessionSets.map { it.loggedAtEpochMillis }
            ((timestamps.max() - timestamps.min()) / 60_000.0)
        }
    return TrainingStats(
        workouts = sessionIdsSince.size,
        totalDurationMinutes = durationMinutes,
        totalVolumeKg = setsSince.sumOf { it.weightKg * it.reps },
        totalSets = setsSince.size,
    )
}
