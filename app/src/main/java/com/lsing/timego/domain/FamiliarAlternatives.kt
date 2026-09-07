package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.TrainingLean

/**
 * Ordered list of familiar exercises the user could pick instead of the current landing
 * recommendation, for the session-local "Choose another" action (Coach Memory Phase 1 -- see the
 * vault note "TimeGo/18 Coach Memory Product and Technical Design").
 *
 * This is a deterministic ranked filter, not a learner: identical inputs always produce an identical
 * list, nothing is trained, and nothing is predicted. It decides *candidacy* only -- it makes no
 * claim that a candidate is easier, equivalent, or safer.
 *
 * Favorites are deliberately not a parameter: the durable project decision (vault note
 * "TimeGo/06 Decisions and Historical Record") fixes them as cosmetic shortcuts that must never feed
 * recommendation ranking.
 *
 * Hard eligibility gates, applied before ordering:
 *  - not [rejectedId] and not in [excludedIds] (exercises the user has already clicked past this
 *    session);
 *  - a primary mover (`muscleWeights >= 70`, via [primaryMuscleGroups]) overlaps at least one
 *    [targetGroups] entry -- an incidental synergist tag does not qualify;
 *  - category is STRENGTH or CALISTHENICS, so a Cardio or Warmup exercise is never injected into the
 *    strength/calisthenics landing flow;
 *  - [lean] is applied as a soft filter exactly like [suggestedExerciseFor]: the off-lean category is
 *    dropped only when an on-lean candidate survives.
 *
 * Ordering tiers (earlier tier wins; each is a tie-break for the previous):
 *  1. saved-routine member before non-member;
 *  2. previously logged before never logged;
 *  3. anything except [lastShownId] before [lastShownId] itself (rotate away from what was just shown);
 *  4. lower [usageCounts] value;
 *  5. name, for a stable final tie-break.
 */
fun familiarAlternativesFor(
    targetGroups: Set<String>,
    exercises: List<Exercise>,
    lean: TrainingLean,
    usageCounts: Map<Long, Int>,
    loggedExerciseIds: Set<Long>,
    routineExerciseIds: Set<Long>,
    excludedIds: Set<Long>,
    rejectedId: Long?,
    lastShownId: Long?,
): List<Exercise> {
    if (targetGroups.isEmpty()) return emptyList()

    val eligible = exercises.filter { exercise ->
        exercise.id != rejectedId &&
            exercise.id !in excludedIds &&
            (exercise.category == ExerciseCategory.STRENGTH.name ||
                exercise.category == ExerciseCategory.CALISTHENICS.name) &&
            primaryMuscleGroups(exercise).any { it in targetGroups }
    }
    if (eligible.isEmpty()) return emptyList()

    val leaned = when (lean) {
        TrainingLean.STRENGTH -> eligible.filter { it.category != ExerciseCategory.CALISTHENICS.name }
        TrainingLean.CALISTHENICS -> eligible.filter { it.category == ExerciseCategory.CALISTHENICS.name }
        TrainingLean.BALANCED -> eligible
    }.ifEmpty { eligible }

    return leaned.sortedWith(
        compareByDescending<Exercise> { it.id in routineExerciseIds }
            .thenByDescending { it.id in loggedExerciseIds }
            .thenBy { it.id == lastShownId }
            .thenBy { usageCounts[it.id] ?: 0 }
            .thenBy { it.name },
    )
}
