package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import java.time.LocalDate

/** Epley formula -- standard estimated-1RM approximation, good enough for a trend line without
 *  needing a real max-effort test every session. */
fun estimatedOneRepMax(weightKg: Double, reps: Int): Double = weightKg * (1 + reps / 30.0)

fun strengthCurve(history: List<SetLog>, sessionDateById: Map<Long, LocalDate>): List<Pair<LocalDate, Double>> =
    history.mapNotNull { log ->
        sessionDateById[log.sessionId]?.let { date -> date to estimatedOneRepMax(log.weightKg, log.reps) }
    }.sortedBy { it.first }

/** Feeds HeatmapGrid's `ratios` param directly -- each day's total volume (weight * reps summed
 *  across sets) normalized against the single highest-volume day in the data, so the heatmap
 *  reads as relative training load rather than a plain yes/no consistency mark. */
fun workoutVolumeRatios(sessions: List<WorkoutSession>, sets: List<SetLog>): Map<LocalDate, Float> {
    val sessionDateById = sessions.associate { it.id to it.date }
    val volumeByDate = sets.mapNotNull { set -> sessionDateById[set.sessionId]?.let { date -> date to set.weightKg * set.reps } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, volumes) -> volumes.sum() }
    val maxVolume = volumeByDate.values.maxOrNull() ?: return emptyMap()
    if (maxVolume <= 0.0) return emptyMap()
    return volumeByDate.mapValues { (_, volume) -> (volume / maxVolume).toFloat().coerceIn(0f, 1f) }
}

enum class PrType { HEAVIEST_WEIGHT, MOST_REPS, BEST_VOLUME }

data class PersonalRecord(val type: PrType, val value: Double, val achievedOn: LocalDate)

/** Computed fresh from full history each time (not incrementally tracked) -- simpler and correct
 *  by construction; history sizes here are small enough (one person's own lifts) that this is
 *  cheap even called on every Progress screen load. */
fun personalRecords(history: List<SetLog>, sessionDateById: Map<Long, LocalDate>): List<PersonalRecord> {
    if (history.isEmpty()) return emptyList()
    val heaviest = history.maxBy { it.weightKg }
    val mostReps = history.maxBy { it.reps }
    val bestVolume = history.maxBy { it.weightKg * it.reps }
    return listOfNotNull(
        sessionDateById[heaviest.sessionId]?.let { PersonalRecord(PrType.HEAVIEST_WEIGHT, heaviest.weightKg, it) },
        sessionDateById[mostReps.sessionId]?.let { PersonalRecord(PrType.MOST_REPS, mostReps.reps.toDouble(), it) },
        sessionDateById[bestVolume.sessionId]?.let { PersonalRecord(PrType.BEST_VOLUME, bestVolume.weightKg * bestVolume.reps, it) },
    )
}

/** Best estimated-1RM per date among sets logged for any exercise tagged with [muscleGroup] --
 *  an aggregate view across e.g. every QUADS exercise, not just one. */
fun muscleGroupStrengthCurve(
    history: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sessionDateById: Map<Long, LocalDate>,
    muscleGroup: String,
): List<Pair<LocalDate, Double>> {
    val bestByDate = mutableMapOf<LocalDate, Double>()
    for (log in history) {
        val exercise = exercisesById[log.exerciseId] ?: continue
        if (muscleGroup !in exercise.muscleGroups) continue
        val date = sessionDateById[log.sessionId] ?: continue
        val oneRepMax = estimatedOneRepMax(log.weightKg, log.reps)
        val current = bestByDate[date]
        if (current == null || oneRepMax > current) bestByDate[date] = oneRepMax
    }
    return bestByDate.entries.sortedBy { it.key }.map { it.key to it.value }
}
