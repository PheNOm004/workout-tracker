package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.TrainingLean

/** Picks a least-used exercise for [targetGroups], applying [lean] as a soft category filter.
 *  Requires that at least one of [targetGroups] is a primary mover (weight >= 70) for the exercise,
 *  so incidental synergist/stabilizer tags (e.g. Lats on Ab Wheel Rollout) are not suggested. */
fun suggestedExerciseFor(
    targetGroups: Set<String>,
    exercises: List<Exercise>,
    lean: TrainingLean,
    usageCounts: Map<Long, Int>,
): Exercise? {
    val matching = exercises.filter { exercise -> primaryMuscleGroups(exercise).any { it in targetGroups } }
    if (matching.isEmpty()) return null

    val leaned = when (lean) {
        TrainingLean.STRENGTH -> matching.filter { it.category != ExerciseCategory.CALISTHENICS.name }
        TrainingLean.CALISTHENICS -> matching.filter { it.category == ExerciseCategory.CALISTHENICS.name }
        TrainingLean.BALANCED -> matching
    }

    return leaned
        .ifEmpty { matching }
        .minWithOrNull(compareBy<Exercise> { usageCounts[it.id] ?: 0 }.thenBy { it.name })
}
