package com.lsing.timego.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import com.lsing.timego.domain.formatCalisthenicsWeight
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.Spacing

/** [setDescriptions] holds one entry per set of this exercise (e.g. "60.0kg x 8"), grouped
 *  together under a single [exerciseName] row rather than repeating the name once per set --
 *  see [buildDayHistoryEntries]. */
data class DayHistoryEntry(val exerciseName: String, val setDescriptions: List<String>)

/** Shared by the Progress screen's tap-a-heatmap-day history and the logging landing page's
 *  last-session detail -- both need "every set logged in this session/day, one row per exercise
 *  with all its sets listed together" rather than one row per raw set (which repeated the
 *  exercise name for every set of a multi-set exercise). Order follows first-appearance order in
 *  [setLogs] (chronological, since callers pass sets already ordered by loggedAtEpochMillis). */
fun buildDayHistoryEntries(setLogs: List<SetLog>, exercisesById: Map<Long, Exercise>): List<DayHistoryEntry> =
    setLogs
        .mapNotNull { log ->
            val exercise = exercisesById[log.exerciseId] ?: return@mapNotNull null
            val description = when (exercise.loggingType) {
                LoggingType.DURATION_DISTANCE.name -> {
                    val distance = log.distanceKm?.let { " -- ${it}km" } ?: ""
                    "${log.durationMinutes ?: 0.0} min$distance"
                }
                LoggingType.HOLD.name -> "${log.holdSeconds ?: 0}s hold"
                else -> if (exercise.category == ExerciseCategory.CALISTHENICS.name && log.addedWeightKg != null) {
                    "${formatCalisthenicsWeight(log.addedWeightKg)} x ${log.reps}"
                } else {
                    "${log.weightKg}kg x ${log.reps}"
                }
            }
            exercise.name to description
        }
        .groupBy({ it.first }, { it.second })
        .map { (name, descriptions) -> DayHistoryEntry(name, descriptions) }

/** One row per exercise (not per set) -- shared between the Progress screen's tap-a-heatmap-day
 *  dialog (title = "Workout on <date>") and the logging landing page's last-session detail
 *  (title = "Last session"). [title] is caller-supplied rather than assuming a date, since the
 *  landing page's "last session" isn't itself date-keyed the way the heatmap's tap target is. */
@Composable
fun WorkoutHistoryDialog(
    title: String,
    entries: List<DayHistoryEntry>,
    onDismiss: () -> Unit,
    muscleGroups: Set<String> = emptySet(),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (muscleGroups.isNotEmpty()) {
                    FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        muscleGroups.sorted().forEach { group ->
                            AssistChip(
                                onClick = {},
                                label = { Text(formatEnumLabel(group)) },
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        }
                    }
                }
                if (entries.isEmpty()) {
                    Text("No sets logged.")
                } else {
                    entries.forEach { entry ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(entry.exerciseName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                entry.setDescriptions.joinToString(", "),
                                style = LedgerFigureValue.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
fun StatTile(label: String, value: String, caption: String? = null, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(Spacing.ExtraSmall),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = LedgerFigureValue, color = MaterialTheme.colorScheme.onSurface)
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
