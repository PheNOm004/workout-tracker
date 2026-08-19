package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.data.SetLog
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A muscle group only counts as genuinely "worked" by an exercise if it's a primary mover
 *  (weight >= 70 on the exercise's EMG-grounded 0-100 scale, same convention as
 *  [muscleGroupVolumeDistribution]'s weighting) -- a synergist or stabilizer tag (e.g. Triceps on
 *  Bench Press, weight ~65) shouldn't silently satisfy "trained today" for that muscle. A group
 *  missing from [Exercise.muscleWeights] defaults to 100 (unweighted = fully primary), matching
 *  the field's own documented default. */
private const val PRIMARY_MOVER_THRESHOLD = 70

private val ANATOMICAL_MUSCLE_GROUPS = MuscleGroup.entries
    .filterNot { it == MuscleGroup.FULL_BODY }
    .map { it.name }

private val DISPLAY_REGION_GROUPS = listOf(
    setOf(MuscleGroup.CHEST.name),
    setOf(MuscleGroup.LATS.name, MuscleGroup.UPPER_BACK.name, MuscleGroup.LOWER_BACK.name),
    setOf(
        MuscleGroup.FRONT_DELTS.name,
        MuscleGroup.SIDE_DELTS.name,
        MuscleGroup.REAR_DELTS.name,
        MuscleGroup.TRAPS.name,
    ),
    setOf(MuscleGroup.BICEPS.name, MuscleGroup.TRICEPS.name, MuscleGroup.FOREARMS.name),
    setOf(
        MuscleGroup.QUADS.name,
        MuscleGroup.HAMSTRINGS.name,
        MuscleGroup.GLUTES.name,
        MuscleGroup.ADDUCTORS.name,
        MuscleGroup.CALVES.name,
    ),
    setOf(MuscleGroup.ABS.name, MuscleGroup.OBLIQUES.name),
)

private fun isTrainingSet(log: SetLog, exercise: Exercise): Boolean =
    !log.isWarmup && exercise.category != ExerciseCategory.WARMUP.name && exercise.category != ExerciseCategory.CARDIO.name

fun primaryMuscleGroups(exercise: Exercise): Set<String> =
    exercise.muscleGroups.filter { group -> (exercise.muscleWeights[group] ?: 100) >= PRIMARY_MOVER_THRESHOLD }.toSet()

fun lastTrainedDatesByMuscleGroup(
    setLogs: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sessionDateById: Map<Long, LocalDate>,
): Map<String, LocalDate> {
    val result = mutableMapOf<String, LocalDate>()
    for (log in setLogs) {
        val exercise = exercisesById[log.exerciseId] ?: continue
        if (!isTrainingSet(log, exercise)) continue
        val date = sessionDateById[log.sessionId] ?: continue
        val groups = primaryMuscleGroups(exercise).flatMap { group ->
            if (group == MuscleGroup.FULL_BODY.name) ANATOMICAL_MUSCLE_GROUPS else listOf(group)
        }
        for (group in groups) {
            val current = result[group]
            if (current == null || date.isAfter(current)) result[group] = date
        }
    }
    return result
}

fun untrainedMuscleGroups(
    allGroups: List<String>,
    lastTrainedByGroup: Map<String, LocalDate>,
    today: LocalDate,
    thresholdDays: Int = 7,
): List<String> = allGroups.filter { group ->
    val last = lastTrainedByGroup[group]
    last == null || ChronoUnit.DAYS.between(last, today) >= thresholdDays
}

/** Same neglect signal as [untrainedMuscleGroups] but returns every group ranked by staleness
 *  (most-neglected first) instead of a threshold-filtered flag list -- backs the logging landing
 *  page's "recommended muscle group" pick (top of this list = best candidate for balanced
 *  growth). Never-trained groups (absent from [lastTrainedByGroup]) sort first, ahead of any
 *  trained-but-stale group, since "never" is more neglected than any finite number of days. */
fun rankUntrainedMuscleGroups(
    allGroups: List<String>,
    lastTrainedByGroup: Map<String, LocalDate>,
    today: LocalDate,
): List<String> = allGroups.sortedByDescending { group ->
    val last = lastTrainedByGroup[group] ?: return@sortedByDescending Long.MAX_VALUE
    ChronoUnit.DAYS.between(last, today)
}

/** Which muscle groups a session actually trained, derived from its logged sets -- shared by the
 *  logging landing page's last-session summary card (this spec) and, later, the Progress screen's
 *  heatmap workout-summary feature. Deliberately session-scoped rather than date-scoped: two
 *  sessions can share a calendar date now that WorkoutSession isn't date-unique, and this should
 *  answer "what did THIS session train," not "what was trained that whole day." Only counts
 *  [primaryMuscleGroups] per exercise -- a chest session that also lightly loads triceps/delts as
 *  synergists shouldn't report having "trained" those groups. Warm-up and cardio-category
 *  exercises are excluded because they are not strength-session targets. */
fun muscleGroupsWorkedInSession(
    sessionId: Long,
    setLogs: List<SetLog>,
    exercises: List<Exercise>,
): Set<String> {
    val exercisesById = exercises.associateBy { it.id }
    return setLogs
        .filter { it.sessionId == sessionId }
        .mapNotNull { log -> exercisesById[log.exerciseId]?.takeIf { isTrainingSet(log, it) }?.let(::primaryMuscleGroups) }
        .flatten()
        .toSet()
}

/** Detailed affected groups for a session's user-facing heatmap and muscle chips. Unlike
 *  [muscleGroupsWorkedInSession], this intentionally keeps secondary-but-real tags such as
 *  BICEPS on a pull-up and UPPER_BACK on a row. Volume/recommendation math continues to use the
 *  primary-mover function above. Warm-up and cardio-category exercises are excluded. */
fun muscleGroupsAffectedInSession(
    sessionId: Long,
    setLogs: List<SetLog>,
    exercises: List<Exercise>,
): Set<String> {
    val exercisesById = exercises.associateBy { it.id }
    return setLogs
        .filter { it.sessionId == sessionId }
        .flatMap { log ->
            exercisesById[log.exerciseId]
                ?.takeIf { isTrainingSet(log, it) }
                ?.muscleGroups
                .orEmpty()
        }
        .toSet()
}

/** Expands a compact recommendation such as UPPER_BACK + BICEPS into every detailed group that
 *  the displayed regions represent. This is display-only: it does not mark every expanded group
 *  as trained in volume/recommendation calculations. */
fun expandMuscleGroupRegions(groups: Collection<String>): Set<String> {
    val expanded = groups.toMutableSet()
    DISPLAY_REGION_GROUPS.forEach { regionGroups ->
        if (groups.any { it in regionGroups }) expanded += regionGroups
    }
    return expanded
}

/** True when a session (or day) has no primary-mover muscle groups to name because every
 *  non-warmup set logged was cardio/duration-based (no meaningful primary mover) -- distinguishes
 *  an actual cardio day from a session that just happened to hit nothing above the primary-mover
 *  threshold. Warm-up sets are excluded from the check, same convention as the recommendation
 *  baselines. A session with no non-warmup sets at all counts as cardio-only vacuously; callers
 *  needing a distinct "nothing logged" case should check [setLogs] emptiness themselves. */
fun isCardioOnlySession(setLogs: List<SetLog>, exercisesById: Map<Long, Exercise>): Boolean {
    val trainingLogs = setLogs.filter { log ->
        val exercise = exercisesById[log.exerciseId] ?: return@filter false
        !log.isWarmup && exercise.category != ExerciseCategory.WARMUP.name
    }
    return trainingLogs.isNotEmpty() && trainingLogs.all { log ->
        exercisesById[log.exerciseId]?.loggingType == LoggingType.DURATION_DISTANCE.name
    }
}
