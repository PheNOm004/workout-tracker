package com.lsing.timego.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.lsing.timego.domain.DayTrainingStats
import com.lsing.timego.ui.theme.LedgerFigureValue
import java.time.format.DateTimeFormatter

private val BREAKDOWN_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d")

/** Per-day breakdown behind tapping any of the Consistency stat tiles (Workouts/Duration/Volume/
 *  Sets) -- the same underlying [DayTrainingStats] list regardless of which tile was tapped, since
 *  seeing all four together is strictly more useful than a single-metric view, and it avoids four
 *  near-identical dialogs. Days with zero training don't appear -- [days] only ever contains days
 *  that had a session, same absence-not-zero convention the heatmap and [WorkoutHistoryDialog] use. */
@Composable
fun PeriodBreakdownDialog(periodLabel: String, days: List<DayTrainingStats>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Breakdown")
                Text(periodLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (days.isEmpty()) {
                    Text("No sessions logged in this period.")
                } else {
                    days.forEach { day ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(day.date.format(BREAKDOWN_DATE_FORMATTER), style = MaterialTheme.typography.bodyMedium)
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                                Text(
                                    "${day.workouts} workout${if (day.workouts == 1) "" else "s"} · " +
                                        "${formatHistoryDuration(day.durationMinutes)} · " +
                                        "${day.volumeKg.toInt()} kg · " +
                                        "${day.sets} set${if (day.sets == 1) "" else "s"}",
                                    style = LedgerFigureValue.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
