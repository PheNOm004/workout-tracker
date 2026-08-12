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
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.Spacing

data class DayHistoryEntry(val exerciseName: String, val description: String)

/** Set/Name/Reps-or-Duration table, one row per logged set -- shared between the Progress
 *  screen's tap-a-heatmap-day dialog (title = "Workout on <date>") and the logging landing page's
 *  last-session detail (title = "Last session"). [title] is caller-supplied rather than assuming
 *  a date, since the landing page's "last session" isn't itself date-keyed the way the heatmap's
 *  tap target is. */
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
                    entries.forEachIndexed { index, entry ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text("${index + 1}", style = LedgerFigureValue.copy(fontSize = 14.sp), modifier = Modifier.padding(end = 12.dp))
                            Text(entry.exerciseName, modifier = Modifier.weight(1f))
                            Text(entry.description, style = LedgerFigureValue.copy(fontSize = 14.sp))
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
