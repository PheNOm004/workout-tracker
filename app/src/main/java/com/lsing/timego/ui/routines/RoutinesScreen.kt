package com.lsing.timego.ui.routines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.data.TrainingLean
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.theme.LedgerFigureEmphasis
import com.lsing.timego.ui.theme.Spacing
import java.time.format.DateTimeFormatter

private val SESSION_HISTORY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun RoutinesScreen(viewModel: RoutinesViewModel = viewModel()) {
    val routines by viewModel.routines.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val untrainedGroups by viewModel.untrainedGroups.collectAsState()
    val holdDelaySeconds by viewModel.holdDelaySeconds.collectAsState()
    val trainingLean by viewModel.trainingLean.collectAsState()
    val sessionHistory by viewModel.sessionHistory.collectAsState()
    var showRoutineForm by remember { mutableStateOf(false) }
    var showSessionHistory by remember { mutableStateOf(false) }
    var pendingDeleteSessionId by remember { mutableStateOf<Long?>(null) }

    if (showRoutineForm) {
        RoutineFormDialog(
            exercises = exercises,
            onDismiss = { showRoutineForm = false },
            onCreate = viewModel::createRoutine,
        )
    }

    if (showSessionHistory) {
        SessionHistoryDialog(
            sessions = sessionHistory,
            onDismiss = { showSessionHistory = false },
            onDeleteRequest = { pendingDeleteSessionId = it },
        )
    }

    pendingDeleteSessionId?.let { sessionId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSessionId = null },
            title = { Text("Delete this session?") },
            text = { Text("This permanently removes the session and every set logged in it. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(sessionId)
                    pendingDeleteSessionId = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSessionId = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    LazyColumn(modifier = Modifier.padding(Spacing.Large)) {
        item {
            Text(
                "Routines",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = Spacing.ExtraSmall, bottom = Spacing.Small),
            )
            Text(
                "Prepare the next session before you step in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.Small),
            )
            SectionHeader(
                title = "Your routines",
                topPadding = Spacing.ExtraSmall,
                trailing = { Button(onClick = { showRoutineForm = true }) { Text("+ New routine") } },
            )
        }
        if (routines.isEmpty()) {
            item {
                Text(
                    "No routines yet. Create one to plan which days you train.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.Small),
                )
                Button(onClick = { showRoutineForm = true }) { Text("Create your first routine") }
            }
        }
        if (untrainedGroups.isNotEmpty()) {
            item {
                Text(
                    "Not trained in a while",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.Large, bottom = Spacing.ExtraSmall),
                )
                FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium)) {
                    untrainedGroups.forEach { group ->
                        AssistChip(
                            onClick = {},
                            label = { Text(formatEnumLabel(group)) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.error),
                            border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.padding(end = Spacing.ExtraSmall, bottom = Spacing.ExtraSmall),
                        )
                    }
                }
            }
        }
        item {
            SectionHeader(title = "Workout settings", topPadding = Spacing.Large)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium)) {
                Text(
                    "Hold-exercise start delay",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.setHoldDelaySeconds(holdDelaySeconds - 1) }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease delay")
                }
                Text("${holdDelaySeconds}s", style = LedgerFigureEmphasis)
                IconButton(onClick = { viewModel.setHoldDelaySeconds(holdDelaySeconds + 1) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase delay")
                }
            }
            Text(
                "Suggested exercise style",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = Spacing.ExtraSmall),
            )
            FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium)) {
                TrainingLean.entries.forEach { lean ->
                    FilterChip(
                        selected = trainingLean == lean,
                        onClick = { viewModel.setTrainingLean(lean) },
                        label = {
                            Text(
                                when (lean) {
                                    TrainingLean.STRENGTH -> "Strength"
                                    TrainingLean.BALANCED -> "Balanced"
                                    TrainingLean.CALISTHENICS -> "Calisthenics"
                                },
                            )
                        },
                        modifier = Modifier.padding(end = Spacing.ExtraSmall, bottom = Spacing.ExtraSmall),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        items(routines, key = { it.id }) { routine ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.ExtraSmall)
                    .then(if (routine == routines.firstOrNull()) Modifier.padding(Spacing.Small) else Modifier),
            ) {
                if (routine == routines.firstOrNull()) {
                    Text("Next routine", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        routine.name,
                        style = LedgerFigureEmphasis,
                        modifier = Modifier.weight(1f).padding(Spacing.Medium, Spacing.Medium, Spacing.Medium, Spacing.ExtraSmall),
                    )
                    IconButton(onClick = { viewModel.deleteRoutine(routine.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete ${routine.name}", tint = MaterialTheme.colorScheme.error)
                    }
                }
                if (routine.daysOfWeek.isEmpty()) {
                    Text(
                        "No days set",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.Medium, 0.dp, Spacing.Medium, Spacing.Medium),
                    )
                } else {
                    FlowRow(modifier = Modifier.padding(Spacing.Small, 0.dp, Spacing.Small, Spacing.Small)) {
                        routine.daysOfWeek.forEach { day ->
                            AssistChip(
                                onClick = {},
                                label = { Text(day.take(3).lowercase().replaceFirstChar(Char::uppercase)) },
                                modifier = Modifier.padding(Spacing.ExtraSmall),
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = Spacing.ExtraSmall))
            }
        }
        if (sessionHistory.isNotEmpty()) {
            item {
                SectionHeader(title = "Session history", topPadding = Spacing.Large)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium)) {
                    Text(
                        "Delete a mistaken or test session.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { showSessionHistory = true }) {
                        Text("View (${sessionHistory.size})")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

/** Popup listing every closed session with a per-row delete affordance -- kept out of the main
 *  Routines list (rather than inline) since it's a maintenance action, not something to see by
 *  default every time the page opens. [onDeleteRequest] hands the tapped session's id back to the
 *  caller rather than deleting directly, so the confirmation dialog stays owned by [RoutinesScreen]
 *  and can layer on top of this one. */
@Composable
private fun SessionHistoryDialog(
    sessions: List<SessionHistoryEntry>,
    onDismiss: () -> Unit,
    onDeleteRequest: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Session history") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                sessions.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f).padding(vertical = Spacing.ExtraSmall)) {
                            Text(entry.date.format(SESSION_HISTORY_DATE_FORMATTER), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${entry.setCount} set${if (entry.setCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDeleteRequest(entry.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete session on ${entry.date.format(SESSION_HISTORY_DATE_FORMATTER)}", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
