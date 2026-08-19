package com.lsing.timego.domain

const val MAX_EXPANDED_EXERCISE_ROWS = 3

fun toggleExpandedExerciseIds(
    expandedIds: List<Long>,
    exerciseId: Long,
    maxExpanded: Int = MAX_EXPANDED_EXERCISE_ROWS,
): List<Long> =
    if (exerciseId in expandedIds) {
        expandedIds.filterNot { it == exerciseId }
    } else {
        (expandedIds + exerciseId).takeLast(maxExpanded)
    }
