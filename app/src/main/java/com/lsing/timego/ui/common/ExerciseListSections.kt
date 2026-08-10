package com.lsing.timego.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory

/** Title-Case display label for an ExerciseCategory or MuscleGroup enum name, e.g. "FULL_BODY" ->
 *  "Full Body". Shared here since both category and muscle-group headers need it. */
fun formatEnumLabel(rawName: String): String =
    rawName.lowercase().split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

/** Renders [exercises] with a search box, then grouped by category (collapsible, defaults to
 *  expanded) and sub-headed by muscle group within each category. [itemContent] renders one
 *  exercise's row -- this component owns only the search/grouping/collapse chrome so Log and
 *  Routines can each supply their own row UI (weight/reps inputs vs a selection checkbox)
 *  without duplicating that structure. A search query flattens the results out of the
 *  category/muscle-group grouping (a plain filtered list), since narrowing to a handful of
 *  matches makes the grouping chrome noise rather than useful navigation. */
@Composable
fun ExerciseSections(exercises: List<Exercise>, itemContent: @Composable (Exercise) -> Unit) {
    var query by remember { mutableStateOf("") }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Search exercises") },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        singleLine = true,
    )

    if (query.isNotBlank()) {
        val matches = remember(exercises, query) {
            exercises.filter { it.name.contains(query, ignoreCase = true) }
        }
        matches.forEach { exercise -> itemContent(exercise) }
        return
    }

    val byCategory = remember(exercises) { exercises.groupBy { it.category } }
    ExerciseCategory.entries.forEach { category ->
        val inCategory = byCategory[category.name].orEmpty()
        if (inCategory.isEmpty()) return@forEach
        key(category) {
            var expanded by remember(category) { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clickable { expanded = !expanded },
            ) {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                )
                Text(formatEnumLabel(category.name), style = MaterialTheme.typography.titleMedium)
            }
            AnimatedExpand(expanded) {
                // Sorted so iteration order is deterministic across recompositions -- a plain
                // HashMap's order isn't guaranteed, and combined with key() below, an unstable
                // order would still churn which composable slot each group lands in.
                val byMuscleGroup = remember(inCategory) {
                    inCategory.groupBy { it.muscleGroups.firstOrNull() ?: "OTHER" }.toSortedMap()
                }
                byMuscleGroup.forEach { (group, groupExercises) ->
                    key(group) {
                        var groupExpanded by remember(category, group) { mutableStateOf(false) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clickable { groupExpanded = !groupExpanded },
                        ) {
                            Icon(
                                if (groupExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                                contentDescription = if (groupExpanded) "Collapse" else "Expand",
                                modifier = Modifier.padding(start = 32.dp, end = 4.dp),
                            )
                            Text(
                                formatEnumLabel(group),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AnimatedExpand(groupExpanded) {
                            groupExercises.forEach { exercise -> itemContent(exercise) }
                        }
                    }
                }
            }
        }
    }
}
