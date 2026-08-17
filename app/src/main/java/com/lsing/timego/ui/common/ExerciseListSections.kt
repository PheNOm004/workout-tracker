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
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.ui.theme.Spacing

/** Title-Case display label for an ExerciseCategory or MuscleGroup enum name, e.g. "FULL_BODY" ->
 *  "Full Body". Shared here since both category and muscle-group headers need it. */
fun formatEnumLabel(rawName: String): String =
    rawName.lowercase().split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

/** Formats a set of muscle-group names into a short display label, e.g. {CHEST, TRICEPS} ->
 *  "Chest & Triceps" -- shared by the logging landing page's last-session/recommended sections
 *  and the workout-history dialog. Ordered by [MuscleGroup]'s own declaration order (front-to-back
 *  anatomical grouping) rather than alphabetically, so e.g. "Chest & Triceps" reads naturally
 *  instead of "Chest & Quads & Triceps" in an arbitrary order. Four or more distinct groups
 *  collapses to "Full Body" instead of a long list -- past that point naming every group stops
 *  being useful summary information. Empty input returns "" so callers can supply their own
 *  fallback copy (e.g. "Everything's trained" vs. "Cardio"). */
fun formatMuscleGroupList(groups: Collection<String>): String {
    val ordered = groups.distinct().sortedBy { name -> MuscleGroup.entries.indexOfFirst { it.name == name } }
    return when {
        ordered.isEmpty() -> ""
        ordered.size <= 3 -> ordered.map(::formatEnumLabel).let { names ->
            if (names.size == 1) names[0] else names.dropLast(1).joinToString(", ") + " & " + names.last()
        }
        else -> "Full Body"
    }
}

/** Human-readable "what kind of day was this" label for a session, e.g. "Chest & Triceps". Falls
 *  back to "Cardio" when [isCardioOnly] (no primary-mover muscle groups because every set was
 *  duration/distance-based), or "Light Session" when there's simply nothing above the
 *  primary-mover threshold (e.g. only synergist/stabilizer work was logged). Never invents a
 *  muscle group that wasn't actually a primary mover -- see [formatMuscleGroupList]. */
fun sessionDayLabel(muscleGroups: Set<String>, isCardioOnly: Boolean): String {
    val listLabel = formatMuscleGroupList(muscleGroups)
    if (listLabel.isNotEmpty()) return listLabel
    return if (isCardioOnly) "Cardio" else "Light Session"
}

/** Strips hyphens/spaces and lowercases so a search for "pull up" or "pullup" matches an exercise
 *  named "Pull-Up" -- exercise names keep their real punctuation (no renaming), only the search
 *  comparison ignores it. Applied to both the query and the candidate name. */
private fun normalizeForSearch(text: String): String =
    text.lowercase().filterNot { it == '-' || it == ' ' }

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
            exercises.filter { normalizeForSearch(it.name).contains(normalizeForSearch(query)) }
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
                    .padding(top = Spacing.Large, bottom = Spacing.ExtraSmall)
                    .clickable { expanded = !expanded },
            ) {
                val visual = categoryVisual(category)
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = visual.accent,
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                )
                Text(
                    formatEnumLabel(category.name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
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
                                .padding(top = Spacing.Small)
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
