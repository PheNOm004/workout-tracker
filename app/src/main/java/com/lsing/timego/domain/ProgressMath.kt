package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.LoggingType
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

enum class PrType { BEST_SET, LONGEST_HOLD }

/** [secondaryValue] carries reps alongside [value]'s weight for a BEST_SET record (null for
 *  LONGEST_HOLD, which is single-valued) -- a "best set" is inherently a weight+reps pair, not a
 *  single number, so both travel together rather than being reported as separate records. */
data class PersonalRecord(val exerciseId: Long, val type: PrType, val value: Double, val secondaryValue: Double? = null, val achievedOn: LocalDate)

/** Computed fresh from full history each time (not incrementally tracked) -- simpler and correct
 *  by construction; history sizes here are small enough (one person's own lifts) that this is
 *  cheap even called on every Progress screen load. Grouped per exercise -- a global "heaviest
 *  weight across everything" would mix e.g. a squat and a bicep curl together, which is
 *  meaningless. CARDIO/WARMUP sets are excluded entirely via the loggingType check: their
 *  weightKg/reps are 0.0/0 sentinels (see SetLog), not real values worth ranking. HOLD exercises
 *  get their own LONGEST_HOLD record computed separately, since weightKg/reps are sentinels for
 *  them too but holdSeconds is real.
 *
 *  BEST_SET replaces the earlier three-way HEAVIEST_WEIGHT/MOST_REPS/BEST_VOLUME split -- those
 *  independently-maximized numbers usually didn't come from the same set at all (a heavy triple
 *  and a light-weight high-rep set could both "win" separately), which didn't answer "what's the
 *  best I've actually done." One set wins by estimated 1RM (already the app's standard strength
 *  metric, used for the strength curve too), and both its weight and reps are reported together. */
fun personalRecords(
    history: List<SetLog>,
    sessionDateById: Map<Long, LocalDate>,
    exercisesById: Map<Long, Exercise>,
): List<PersonalRecord> {
    val weightRepsRecords = history
        .filter { log -> exercisesById[log.exerciseId]?.loggingType == LoggingType.WEIGHT_REPS.name }
        .groupBy { it.exerciseId }
        .mapNotNull { (exerciseId, sets) ->
            val bestSet = sets.maxBy { estimatedOneRepMax(it.weightKg, it.reps) }
            sessionDateById[bestSet.sessionId]?.let {
                PersonalRecord(exerciseId, PrType.BEST_SET, bestSet.weightKg, bestSet.reps.toDouble(), it)
            }
        }
    val holdRecords = history
        .filter { log -> exercisesById[log.exerciseId]?.loggingType == LoggingType.HOLD.name }
        .groupBy { it.exerciseId }
        .flatMap { (exerciseId, sets) ->
            val longest = sets.maxBy { it.holdSeconds ?: 0 }
            listOfNotNull(
                sessionDateById[longest.sessionId]?.let { PersonalRecord(exerciseId, PrType.LONGEST_HOLD, (longest.holdSeconds ?: 0).toDouble(), achievedOn = it) },
            )
        }
    return weightRepsRecords + holdRecords
}

/** Best estimated-1RM per date among sets logged for any STRENGTH/CALISTHENICS exercise tagged
 *  with [muscleGroup] -- an aggregate view across e.g. every QUADS exercise, not just one.
 *  CARDIO/WARMUP exercises are excluded even if tagged with the group (e.g. Cycling is tagged
 *  QUADS for the muscle-nudge feature) since their weightKg/reps are 0.0/0 sentinels, not real
 *  values -- including them would corrupt the curve with fake near-zero 1RM points. */
fun muscleGroupStrengthCurve(
    history: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sessionDateById: Map<Long, LocalDate>,
    muscleGroup: String,
): List<Pair<LocalDate, Double>> {
    val bestByDate = mutableMapOf<LocalDate, Double>()
    for (log in history) {
        val exercise = exercisesById[log.exerciseId] ?: continue
        if (exercise.loggingType != LoggingType.WEIGHT_REPS.name) continue
        if (muscleGroup !in exercise.muscleGroups) continue
        val date = sessionDateById[log.sessionId] ?: continue
        val oneRepMax = estimatedOneRepMax(log.weightKg, log.reps)
        val current = bestByDate[date]
        if (current == null || oneRepMax > current) bestByDate[date] = oneRepMax
    }
    return bestByDate.entries.sortedBy { it.key }.map { it.key to it.value }
}
