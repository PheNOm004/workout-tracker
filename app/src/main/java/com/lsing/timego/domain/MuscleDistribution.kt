package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import java.time.LocalDate

/** RPE >=7 (0-3 reps in reserve, "effective rep" territory per hypertrophy research) gets full
 *  credit toward the Muscle Balance chart's weekly target. RPE 5-6 ramps linearly (light-but-not-
 *  trivial effort). RPE <=4 gets low but nonzero credit -- very light work still counts a little,
 *  just not as a real stimulus set. Missing RPE gets full credit, same convention as every other
 *  RPE-gated behavior in this app (see escalationTierForRpe): never penalize a value the user
 *  simply didn't log. */
fun effortWeight(rpe: Int?): Double = when {
    rpe == null -> 1.0
    rpe >= 7 -> 1.0
    rpe >= 5 -> 0.3 + (rpe - 5) / 2.0 * 0.7
    else -> 0.15
}

fun muscleDistributionForTimeframe(
    timeframe: ProgressTimeframe,
    sessions: List<WorkoutSession>,
    sets: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    today: LocalDate,
): Map<String, Float> {
    val since = timeframe.sinceDate(sessions.minOfOrNull { it.date }, today)
    val sessionDateById = sessions.associate { it.id to it.date }
    val rawDistribution = muscleGroupVolumeDistribution(sets, exercisesById, sessionDateById, since)
    val maxVolume = rawDistribution.values.maxOrNull() ?: return emptyMap()
    if (maxVolume <= 0.0) return emptyMap()
    return rawDistribution.mapValues { (_, volume) -> (volume / maxVolume).toFloat() }
}

/** Total volume per muscle group across WEIGHT_REPS/HOLD sets logged on or after [since] -- an
 *  exercise contributes its volume to every muscle group it's tagged with, scaled by that
 *  group's entry in [Exercise.muscleWeights] (defaulting to 100% when unspecified) -- e.g. a
 *  squat set counts its full volume toward QUADS but a partial-credit fraction toward GLUTES if
 *  weighted lower. Backs the "muscle distribution" radar chart and the muscle-body heatmap.
 *  CARDIO/WARMUP excluded, same reasoning as personalRecords/muscleGroupStrengthCurve -- their
 *  weightKg/reps are 0.0/0 sentinels, not real values. HOLD exercises use holdSeconds directly as
 *  their "volume" figure before weighting: a rough proxy, not weight-equivalent. */
fun muscleGroupVolumeDistribution(
    history: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sessionDateById: Map<Long, LocalDate>,
    since: LocalDate,
): Map<String, Double> {
    val volumeByGroup = mutableMapOf<String, Double>()
    for (log in history) {
        val exercise = exercisesById[log.exerciseId] ?: continue
        if (log.isWarmup || exercise.category == ExerciseCategory.WARMUP.name || exercise.category == ExerciseCategory.CARDIO.name) continue
        val volume = when (exercise.loggingType) {
            LoggingType.WEIGHT_REPS.name -> log.weightKg * log.reps
            LoggingType.HOLD.name -> (log.holdSeconds ?: 0).toDouble()
            else -> continue
        }
        val date = sessionDateById[log.sessionId] ?: continue
        if (date.isBefore(since)) continue
        for (group in exercise.muscleGroups) {
            val weightedVolume = volume * (exercise.muscleWeights[group] ?: 100) / 100.0
            volumeByGroup[group] = (volumeByGroup[group] ?: 0.0) + weightedVolume
        }
    }
    return volumeByGroup
}

/** Keeps the detailed radar axes stable as the user logs different exercise sequences. The raw
 * distribution map follows set/exercise insertion order, which would otherwise make the same
 * muscle jump to a different spoke between recompositions. */
fun orderedMuscleDistributionForChart(distribution: Map<String, Float>): Map<String, Float> =
    linkedMapOf<String, Float>().apply {
        MuscleGroup.entries
            .filterNot { it == MuscleGroup.FULL_BODY }
            .forEach { group ->
                distribution[group.name]?.let { put(group.name, it) }
            }
    }

/** Relative per-muscle contribution for one session, using the same weighted-volume calculation
 *  as the Progress tab. The strongest group in the session is 1.0; secondary groups retain their
 *  lower weighted intensity instead of receiving the same flat highlight as primary groups. */
fun muscleGroupIntensityForSession(
    sessionId: Long,
    setLogs: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
): Map<String, Float> {
    val sessionSets = setLogs.filter { it.sessionId == sessionId }
    val rawDistribution = muscleGroupVolumeDistribution(
        history = sessionSets,
        exercisesById = exercisesById,
        sessionDateById = sessionSets.associate { it.sessionId to LocalDate.MIN },
        since = LocalDate.MIN,
    )
    val maxVolume = rawDistribution.values.maxOrNull() ?: return emptyMap()
    if (maxVolume <= 0.0) return emptyMap()
    return rawDistribution.mapValues { (_, volume) -> (volume / maxVolume).toFloat() }
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
