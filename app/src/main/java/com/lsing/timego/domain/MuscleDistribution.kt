package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
    val since = timeframe.sinceDate(today)
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

/** Weighted effective-set count per muscle group, mirroring [muscleGroupVolumeDistribution]'s
 *  filtering and [Exercise.muscleWeights] partial-credit conventions but counting RPE-weighted
 *  sets instead of load -- sets are the unit fitness research treats as comparable across muscle
 *  groups, unlike kg-moved. Deliberately a separate function: [muscleGroupVolumeDistribution] and
 *  [muscleDistributionForTimeframe] remain unchanged and continue to drive the body-diagram
 *  heatmap, which must not change. */
fun muscleGroupEffectiveSetDistribution(
    history: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sessionDateById: Map<Long, LocalDate>,
    since: LocalDate,
    until: LocalDate = LocalDate.MAX,
): Map<String, Double> {
    val effectiveSetsByGroup = mutableMapOf<String, Double>()
    for (log in history) {
        val exercise = exercisesById[log.exerciseId] ?: continue
        if (log.isWarmup || exercise.category == ExerciseCategory.WARMUP.name || exercise.category == ExerciseCategory.CARDIO.name) continue
        if (exercise.loggingType != LoggingType.WEIGHT_REPS.name && exercise.loggingType != LoggingType.HOLD.name) continue
        val date = sessionDateById[log.sessionId] ?: continue
        if (date.isBefore(since) || date.isAfter(until)) continue
        val weight = effortWeight(log.rpe)
        for (group in exercise.muscleGroups) {
            val credit = weight * (exercise.muscleWeights[group] ?: 100) / 100.0
            effectiveSetsByGroup[group] = (effectiveSetsByGroup[group] ?: 0.0) + credit
        }
    }
    return effectiveSetsByGroup
}

private const val TARGET_EFFECTIVE_SETS_PER_WEEK = 10.0

/** [muscleGroupEffectiveSetDistribution] normalized against a fixed weekly target (evidence-
 *  grounded per the design spec: ~10 effective sets/muscle/week) rather than the period's own max
 *  group -- unlike the reverted frequency-vs-own-baseline attempt, this target is external and
 *  fixed, so genuine under-training still reads as genuinely low instead of flattening toward 1.0.
 *  Reuses the same [ProgressTimeframe]/[timeframe.sinceDate] the rest of the Progress screen
 *  already uses -- Year/Lifetime naturally read as "average weekly rate over that period," the
 *  same semantics every other Progress stat already has for those tabs. Inclusive day-counting
 *  (`+ 1`) guarantees at least a one-day window whenever [since] is on or before [today], so this
 *  never divides by zero even on someone's very first day of use. */
fun muscleBalanceForTimeframe(
    timeframe: ProgressTimeframe,
    sessions: List<WorkoutSession>,
    sets: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    today: LocalDate,
): Map<String, Float> {
    val since = timeframe.sinceDate(today)
    val sessionDateById = sessions.associate { it.id to it.date }
    return muscleBalanceForDateRange(sets, exercisesById, sessionDateById, since, today)
}

/** Scores a closed date range against the same fixed weekly effective-set target as the primary
 * Muscle Balance chart. The range is explicit so a prior equal-length period can be compared
 * without accidentally including today's sets. */
fun muscleBalanceForDateRange(
    sets: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sessionDateById: Map<Long, LocalDate>,
    since: LocalDate,
    until: LocalDate,
): Map<String, Float> {
    val effectiveSets = muscleGroupEffectiveSetDistribution(sets, exercisesById, sessionDateById, since, until)
    if (effectiveSets.isEmpty()) return emptyMap()
    val daysInWindow = ChronoUnit.DAYS.between(since, until) + 1
    val weeksInWindow = daysInWindow / 7.0
    val target = TARGET_EFFECTIVE_SETS_PER_WEEK * weeksInWindow
    return effectiveSets.mapValues { (_, value) -> (value / target).toFloat().coerceIn(0f, 1f) }
}

/** The exact, immediately preceding calendar window for a bounded timeframe. */
fun previousMuscleBalanceForTimeframe(
    timeframe: ProgressTimeframe,
    sessions: List<WorkoutSession>,
    sets: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    today: LocalDate,
): Map<String, Float> {
    val currentStart = timeframe.sinceDate(today)
    val windowDays = ChronoUnit.DAYS.between(currentStart, today) + 1
    val previousEnd = currentStart.minusDays(1)
    val previousStart = previousEnd.minusDays(windowDays - 1)
    return muscleBalanceForDateRange(
        sets = sets,
        exercisesById = exercisesById,
        sessionDateById = sessions.associate { it.id to it.date },
        since = previousStart,
        until = previousEnd,
    )
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

data class MuscleSetSummary(
    val bestWeightKg: Double,
    val bestReps: Int,
    val setCount: Int,
)

/** Per-muscle-group "best set + how many sets" summary backing the long-press popup on the
 *  Progress body diagram. Mirrors [muscleGroupVolumeDistribution]'s filtering (WEIGHT_REPS only,
 *  cardio/warmup excluded, a set credits every muscle group its exercise is tagged with) but
 *  reports the raw logged best set -- the one with the highest weightKg*reps, the same rule
 *  [personalRecords] uses for BEST_SET -- instead of aggregated volume. Weight/reps are the values
 *  as logged, not [Exercise.muscleWeights]-scaled: the popup answers "what was the hardest set
 *  that hit this muscle", not "how much of it counted toward the heatmap". HOLD sets carry no
 *  weight*reps and are left out entirely, so a HOLD-only group is simply absent from the map. */
fun muscleGroupSetSummaryForTimeframe(
    timeframe: ProgressTimeframe,
    sessions: List<WorkoutSession>,
    sets: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    today: LocalDate,
): Map<String, MuscleSetSummary> {
    val since = timeframe.sinceDate(today)
    val sessionDateById = sessions.associate { it.id to it.date }

    class Acc {
        var bestVolume = -1.0
        var bestWeightKg = 0.0
        var bestReps = 0
        var setCount = 0
    }

    val byGroup = mutableMapOf<String, Acc>()
    for (log in sets) {
        val exercise = exercisesById[log.exerciseId] ?: continue
        if (log.isWarmup || exercise.category == ExerciseCategory.WARMUP.name || exercise.category == ExerciseCategory.CARDIO.name) continue
        if (exercise.loggingType != LoggingType.WEIGHT_REPS.name) continue
        val date = sessionDateById[log.sessionId] ?: continue
        if (date.isBefore(since)) continue

        val volume = log.weightKg * log.reps
        for (group in exercise.muscleGroups) {
            val acc = byGroup.getOrPut(group) { Acc() }
            acc.setCount++
            if (volume > acc.bestVolume) {
                acc.bestVolume = volume
                acc.bestWeightKg = log.weightKg
                acc.bestReps = log.reps
            }
        }
    }
    return byGroup.mapValues { (_, a) -> MuscleSetSummary(a.bestWeightKg, a.bestReps, a.setCount) }
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

data class DayTrainingStats(
    val date: LocalDate,
    val workouts: Int,
    val durationMinutes: Double,
    val volumeKg: Double,
    val sets: Int,
)

/** [trainingStats] broken out by calendar day -- backs the tap-a-stat-tile breakdown on Progress,
 *  same duration-estimate convention (span of that day's logged-set timestamps, per session).
 *  Only days with at least one session appear; a day with zero training isn't a zero-row, it's
 *  absent, same convention [HeatmapGrid] uses. Newest day first. */
fun trainingStatsByDay(sessions: List<WorkoutSession>, sets: List<SetLog>, since: LocalDate): List<DayTrainingStats> {
    val sessionsSince = sessions.filter { !it.date.isBefore(since) }
    val sessionIdsSince = sessionsSince.mapTo(hashSetOf()) { it.id }
    // Index the history once. Filtering the complete set list separately for every training day
    // made this O(days * sets), which becomes visible after several years of workout history.
    val setsBySession = sets
        .asSequence()
        .filter { it.sessionId in sessionIdsSince }
        .groupBy { it.sessionId }

    return sessionsSince
        .groupBy { it.date }
        .map { (date, daySessions) ->
            val daySets = daySessions.flatMap { setsBySession[it.id].orEmpty() }
            val durationMinutes = daySessions.sumOf { session ->
                val timestamps = setsBySession[session.id].orEmpty().map { it.loggedAtEpochMillis }
                if (timestamps.isEmpty()) 0.0 else (timestamps.max() - timestamps.min()) / 60_000.0
            }
            DayTrainingStats(
                date = date,
                workouts = daySessions.size,
                durationMinutes = durationMinutes,
                volumeKg = daySets.sumOf { it.weightKg * it.reps },
                sets = daySets.size,
            )
        }
        .sortedByDescending { it.date }
}
