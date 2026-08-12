package com.lsing.timego.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
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
                else -> "${log.weightKg}kg x ${log.reps}"
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
fun WorkoutHistoryDialog(title: String, entries: List<DayHistoryEntry>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (entries.isEmpty()) {
                Text("No sets logged.")
            } else {
                Column {
                    entries.forEach { entry ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(entry.exerciseName, modifier = Modifier.weight(1f))
                            Text(
                                entry.setDescriptions.joinToString(", "),
                                style = LedgerFigureValue.copy(fontSize = 14.sp),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = LedgerFigureValue)
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
