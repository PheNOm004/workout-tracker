package com.lsing.timego.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import kotlin.math.roundToInt

/** [setDescriptions] holds one entry per set of this exercise (e.g. "60.0kg x 8"), grouped
 *  together under a single [exerciseName] row rather than repeating the name once per set --
 *  see [buildDayHistoryEntries]. */
data class DayHistoryEntry(val exerciseName: String, val setDescriptions: List<String>)

internal fun formatHistoryDuration(durationMinutes: Double?): String {
    val totalSeconds = ((durationMinutes ?: 0.0).coerceAtLeast(0.0) * 60).roundToInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return when {
        minutes == 0 -> "${seconds}s"
        seconds == 0 -> "$minutes min"
        else -> "${minutes}m ${seconds}s"
    }
}

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
                    "${formatHistoryDuration(log.durationMinutes)}$distance"
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
    label: String? = null,
    durationMinutes: Double? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(title)
                if (label != null) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (durationMinutes != null) {
                    Text(
                        "Duration: ${formatHistoryDuration(durationMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
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
    SurfaceCard(
        modifier = modifier.padding(Spacing.ExtraSmall),
        cornerRadius = 4.dp,
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    val enter = slideInVertically { height -> height / 2 } + fadeIn()
                    val exit = slideOutVertically { height -> -height / 2 } + fadeOut()
                    enter togetherWith exit
                },
                label = "statTileValueTransition",
            ) { targetValue ->
                Text(targetValue, style = LedgerFigureValue, color = MaterialTheme.colorScheme.onSurface)
            }
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
