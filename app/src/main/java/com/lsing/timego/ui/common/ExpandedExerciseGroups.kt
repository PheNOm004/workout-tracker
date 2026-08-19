package com.lsing.timego.ui.common

const val MAX_EXPANDED_EXERCISE_GROUPS = 1

fun toggleExpandedExerciseGroupKeys(
    expandedKeys: List<String>,
    groupKey: String,
    maxExpanded: Int = MAX_EXPANDED_EXERCISE_GROUPS,
): List<String> =
    if (groupKey in expandedKeys) {
        expandedKeys.filterNot { it == groupKey }
    } else {
        (expandedKeys + groupKey).takeLast(maxExpanded)
    }
