package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.SetLog

/** Counts non-warmup working sets per exercise across the full log history for picker ranking. */
fun exerciseUsageFrequency(
    setLogs: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
): Map<Long, Int> =
    setLogs
        .filter { log ->
            !log.isWarmup && exercisesById[log.exerciseId]?.category !in setOf(
                ExerciseCategory.WARMUP.name,
                ExerciseCategory.CARDIO.name,
            )
        }
        .groupingBy { it.exerciseId }
        .eachCount()

/** Sorts exercises by usage count descending, then by name for a stable order. */
fun exercisesRankedByFrequency(
    exercises: List<Exercise>,
    usageCounts: Map<Long, Int>,
): List<Exercise> =
    exercises.sortedWith(
        compareByDescending<Exercise> { usageCounts[it.id] ?: 0 }
            .thenBy { it.name },
    )

/** The small, familiar subset that sits above the full library during an active workout. A zero
 * count means no personal evidence yet, so it belongs in the browser rather than Quick Add. */
fun quickAddExercises(
    exercises: List<Exercise>,
    usageCounts: Map<Long, Int>,
    limit: Int = 6,
): List<Exercise> = exercisesRankedByFrequency(exercises, usageCounts)
    .filter { (usageCounts[it.id] ?: 0) > 0 }
    .take(limit)
