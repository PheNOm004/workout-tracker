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

/** Minimum recovery time (48 hours) recommended by sports science for muscle protein synthesis
 *  and neuromuscular recovery before re-training the same prime mover. */
const val MIN_RECOVERY_DAYS = 2

/** Threshold for secondary movers / synergists (EMG weight >= 40) that incur meaningful fatigue
 *  during compound movements (e.g. Triceps on Bench Press, Hamstrings on Deadlift). */
const val SYNERGIST_RECOVERY_THRESHOLD = 40

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

/** Tracks the most recent date each muscle group was loaded as either a primary mover or a
 *  heavy synergist (EMG weight >= [threshold]). Used by the recommendation engine to monitor
 *  collateral synergist fatigue (e.g. Triceps loaded during heavy bench press) so secondary movers
 *  aren't programmed as fresh primary targets the following day. */
fun lastWorkedDatesByMuscleGroup(
    setLogs: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sessionDateById: Map<Long, LocalDate>,
    threshold: Int = SYNERGIST_RECOVERY_THRESHOLD,
): Map<String, LocalDate> {
    val result = mutableMapOf<String, LocalDate>()
    for (log in setLogs) {
        val exercise = exercisesById[log.exerciseId] ?: continue
        if (!isTrainingSet(log, exercise)) continue
        val date = sessionDateById[log.sessionId] ?: continue
        val groups = exercise.muscleGroups.filter { (exercise.muscleWeights[it] ?: 100) >= threshold }.flatMap { group ->
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

/** Evidence-based staleness ranking with a 48-hour recovery guardrail.
 *  1. Never-trained groups rank first (infinite staleness).
 *  2. Fully rested groups (days >= [minRecoveryDays]) rank next, sorted by staleness (most neglected first).
 *  3. Groups in active recovery (days < [minRecoveryDays], e.g. trained today or yesterday) are heavily
 *     demoted so fully recovered groups always take priority. If all groups are recovering, the one
 *     with the longest rest is favored. */
fun rankUntrainedMuscleGroups(
    allGroups: List<String>,
    lastTrainedByGroup: Map<String, LocalDate>,
    today: LocalDate,
    minRecoveryDays: Int = MIN_RECOVERY_DAYS,
): List<String> = allGroups.sortedByDescending { group ->
    val last = lastTrainedByGroup[group] ?: return@sortedByDescending Long.MAX_VALUE
    val days = ChronoUnit.DAYS.between(last, today)
    if (days >= minRecoveryDays) {
        days
    } else {
        days - 10_000L
    }
}

/** Synergistic movement clusters and biomechanical pairing rules. */
val SYNERGISTIC_MUSCLE_CLUSTERS: List<Set<String>> = listOf(
    // PULL / Posterior Chain: Back + Biceps + Forearms + Traps
    setOf(
        MuscleGroup.LATS.name,
        MuscleGroup.UPPER_BACK.name,
        MuscleGroup.LOWER_BACK.name,
        MuscleGroup.TRAPS.name,
        MuscleGroup.REAR_DELTS.name,
        MuscleGroup.BICEPS.name,
        MuscleGroup.FOREARMS.name,
    ),
    // PUSH / Anterior Upper: Chest + Anterior/Lateral Delts + Triceps
    setOf(
        MuscleGroup.CHEST.name,
        MuscleGroup.FRONT_DELTS.name,
        MuscleGroup.SIDE_DELTS.name,
        MuscleGroup.TRICEPS.name,
    ),
    // LEGS / Lower Body: Quads + Hamstrings + Glutes + Calves + Adductors
    setOf(
        MuscleGroup.QUADS.name,
        MuscleGroup.HAMSTRINGS.name,
        MuscleGroup.GLUTES.name,
        MuscleGroup.CALVES.name,
        MuscleGroup.ADDUCTORS.name,
    ),
    // CORE: Abs + Obliques
    setOf(
        MuscleGroup.ABS.name,
        MuscleGroup.OBLIQUES.name,
    ),
)

fun synergisticPartnersFor(primaryGroup: String): Set<String> {
    val partners = mutableSetOf<String>()
    SYNERGISTIC_MUSCLE_CLUSTERS.forEach { cluster ->
        if (primaryGroup in cluster) partners += cluster
    }
    // Evidence-based movement archetypes (PPL, Arnold Split, Posterior Chain, Legs & Core)
    if (primaryGroup == MuscleGroup.CHEST.name) {
        // Push day: Triceps, Front/Side Delts; Arnold Split: Chest + Back antagonist
        partners += setOf(
            MuscleGroup.TRICEPS.name,
            MuscleGroup.FRONT_DELTS.name,
            MuscleGroup.SIDE_DELTS.name,
            MuscleGroup.LATS.name,
            MuscleGroup.UPPER_BACK.name,
        )
    } else if (primaryGroup == MuscleGroup.LATS.name || primaryGroup == MuscleGroup.UPPER_BACK.name || primaryGroup == MuscleGroup.LOWER_BACK.name) {
        // Biomechanically optimal: Back + Biceps (classic Pull), Back + Legs (Posterior chain / Deadlift day), or Back + Chest (Arnold)
        partners += setOf(
            MuscleGroup.BICEPS.name,
            MuscleGroup.HAMSTRINGS.name,
            MuscleGroup.GLUTES.name,
            MuscleGroup.FOREARMS.name,
            MuscleGroup.TRAPS.name,
            MuscleGroup.REAR_DELTS.name,
            MuscleGroup.CHEST.name,
        )
        // Ensure push delts (Front/Side) and triceps do not contaminate Back day
        partners.remove(MuscleGroup.FRONT_DELTS.name)
        partners.remove(MuscleGroup.SIDE_DELTS.name)
        partners.remove(MuscleGroup.TRICEPS.name)
    } else if (primaryGroup == MuscleGroup.FRONT_DELTS.name) {
        // Push day / Overhead pressing
        partners += setOf(MuscleGroup.CHEST.name, MuscleGroup.SIDE_DELTS.name, MuscleGroup.TRICEPS.name)
    } else if (primaryGroup == MuscleGroup.SIDE_DELTS.name) {
        // Push day or Arnold Shoulders & Arms
        partners += setOf(MuscleGroup.FRONT_DELTS.name, MuscleGroup.TRICEPS.name, MuscleGroup.BICEPS.name, MuscleGroup.REAR_DELTS.name)
    } else if (primaryGroup == MuscleGroup.REAR_DELTS.name) {
        // Pull day or Arnold Shoulders & Arms
        partners += setOf(MuscleGroup.UPPER_BACK.name, MuscleGroup.LATS.name, MuscleGroup.TRAPS.name, MuscleGroup.BICEPS.name, MuscleGroup.SIDE_DELTS.name)
    } else if (primaryGroup == MuscleGroup.BICEPS.name) {
        // Pull day (Back + Biceps) or Arnold Arms day (Biceps + Triceps)
        partners += setOf(MuscleGroup.LATS.name, MuscleGroup.UPPER_BACK.name, MuscleGroup.TRICEPS.name, MuscleGroup.FOREARMS.name)
    } else if (primaryGroup == MuscleGroup.TRICEPS.name) {
        // Push day (Chest + Triceps) or Arnold Arms day (Triceps + Biceps)
        partners += setOf(MuscleGroup.CHEST.name, MuscleGroup.BICEPS.name, MuscleGroup.FRONT_DELTS.name)
    } else if (primaryGroup == MuscleGroup.QUADS.name) {
        // Complete Leg day or Legs & Core
        partners += setOf(MuscleGroup.HAMSTRINGS.name, MuscleGroup.GLUTES.name, MuscleGroup.CALVES.name, MuscleGroup.ADDUCTORS.name, MuscleGroup.ABS.name)
    } else if (primaryGroup == MuscleGroup.HAMSTRINGS.name || primaryGroup == MuscleGroup.GLUTES.name) {
        // Leg day, Posterior Chain / Deadlift day, or Legs & Core
        partners += setOf(MuscleGroup.QUADS.name, MuscleGroup.LATS.name, MuscleGroup.LOWER_BACK.name, MuscleGroup.CALVES.name, MuscleGroup.ABS.name)
    } else if (primaryGroup == MuscleGroup.ABS.name || primaryGroup == MuscleGroup.OBLIQUES.name) {
        // Core always pairs with Legs (bracing) or Pull (hanging work/calisthenics)
        partners += setOf(MuscleGroup.QUADS.name, MuscleGroup.HAMSTRINGS.name, MuscleGroup.GLUTES.name, MuscleGroup.LATS.name)
    }
    partners.remove(primaryGroup)
    return partners
}

/** Evidence-based recommendation algorithm that selects the most neglected, recovered muscle group
 *  as the primary focus, and pairs it with its most stale synergistic partner.
 *  Uses [lastWorkedByGroup] to protect against collateral synergist fatigue (e.g. Triceps loaded
 *  during heavy chest pressing). */
fun recommendSynergisticMuscleGroups(
    allGroups: List<String>,
    lastTrainedByGroup: Map<String, LocalDate>,
    today: LocalDate,
    lastWorkedByGroup: Map<String, LocalDate> = lastTrainedByGroup,
): List<String> {
    val ranked = rankUntrainedMuscleGroups(allGroups, lastWorkedByGroup, today)
    if (ranked.isEmpty()) return emptyList()
    val primary = ranked.first()
    val partners = synergisticPartnersFor(primary)

    val eligiblePartners = partners.intersect(allGroups.toSet())
    val rankedPartners = ranked.filter { it in eligiblePartners && it != primary }

    val secondary = if (primary == MuscleGroup.LATS.name || primary == MuscleGroup.UPPER_BACK.name || primary == MuscleGroup.LOWER_BACK.name) {
        // For Back: prioritize Biceps and Posterior Chain (Hamstrings/Glutes) over delts/chest
        val preferredBackPartners = listOf(
            MuscleGroup.BICEPS.name,
            MuscleGroup.HAMSTRINGS.name,
            MuscleGroup.GLUTES.name,
            MuscleGroup.FOREARMS.name,
        )
        preferredBackPartners.firstOrNull { it in rankedPartners }
            ?: rankedPartners.firstOrNull()
    } else {
        rankedPartners.firstOrNull()
    } ?: ranked.firstOrNull { it != primary }

    return if (secondary != null) listOf(primary, secondary) else listOf(primary)
}

/** Resolves precise exercise targeting groups for a recommended workout split, preserving kinetic
 *  purity so that push muscles never leak into pull workouts (e.g. Triceps on Back + Biceps day)
 *  and vice versa. */
fun workoutTargetGroups(recommendedSeeds: Collection<String>): Set<String> {
    val targets = mutableSetOf<String>()
    for (seed in recommendedSeeds) {
        targets += seed
        when (seed) {
            MuscleGroup.CHEST.name -> {
                targets += setOf(MuscleGroup.CHEST.name)
            }
            MuscleGroup.LATS.name, MuscleGroup.UPPER_BACK.name, MuscleGroup.LOWER_BACK.name -> {
                targets += setOf(MuscleGroup.LATS.name, MuscleGroup.UPPER_BACK.name, MuscleGroup.LOWER_BACK.name, MuscleGroup.TRAPS.name, MuscleGroup.REAR_DELTS.name)
            }
            MuscleGroup.FRONT_DELTS.name, MuscleGroup.SIDE_DELTS.name -> {
                targets += setOf(MuscleGroup.FRONT_DELTS.name, MuscleGroup.SIDE_DELTS.name)
            }
            MuscleGroup.REAR_DELTS.name -> {
                targets += setOf(MuscleGroup.REAR_DELTS.name, MuscleGroup.UPPER_BACK.name)
            }
            MuscleGroup.BICEPS.name -> {
                targets += setOf(MuscleGroup.BICEPS.name, MuscleGroup.FOREARMS.name)
            }
            MuscleGroup.TRICEPS.name -> {
                targets += setOf(MuscleGroup.TRICEPS.name)
            }
            MuscleGroup.FOREARMS.name -> {
                targets += setOf(MuscleGroup.FOREARMS.name, MuscleGroup.BICEPS.name)
            }
            MuscleGroup.QUADS.name -> {
                targets += setOf(MuscleGroup.QUADS.name, MuscleGroup.ADDUCTORS.name)
            }
            MuscleGroup.HAMSTRINGS.name, MuscleGroup.GLUTES.name -> {
                targets += setOf(MuscleGroup.HAMSTRINGS.name, MuscleGroup.GLUTES.name)
            }
            MuscleGroup.CALVES.name -> {
                targets += setOf(MuscleGroup.CALVES.name)
            }
            MuscleGroup.ABS.name, MuscleGroup.OBLIQUES.name -> {
                targets += setOf(MuscleGroup.ABS.name, MuscleGroup.OBLIQUES.name)
            }
        }
    }
    return targets
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
