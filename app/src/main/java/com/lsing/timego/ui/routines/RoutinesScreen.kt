package com.lsing.timego.ui.routines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.theme.LedgerFigureEmphasis
import com.lsing.timego.ui.theme.Spacing

@Composable
fun RoutinesScreen(viewModel: RoutinesViewModel = viewModel()) {
    val routines by viewModel.routines.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val untrainedGroups by viewModel.untrainedGroups.collectAsState()
    val holdDelaySeconds by viewModel.holdDelaySeconds.collectAsState()
    var showRoutineForm by remember { mutableStateOf(false) }

    if (showRoutineForm) {
        RoutineFormDialog(
            exercises = exercises,
            onDismiss = { showRoutineForm = false },
            onCreate = viewModel::createRoutine,
        )
    }

    LazyColumn(modifier = Modifier.padding(Spacing.Large)) {
        item {
            SectionHeader(title = "Settings", topPadding = Spacing.ExtraSmall)
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        if (untrainedGroups.isNotEmpty()) {
            item {
                Text(
                    "Not trained in a while",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.ExtraSmall),
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        item {
            SectionHeader(
                title = "Your Routines",
                topPadding = Spacing.ExtraSmall,
                trailing = { Button(onClick = { showRoutineForm = true }) { Text("+ New routine") } },
            )
        }
        if (routines.isEmpty()) {
            item {
                Text(
                    "No routines yet -- create one to plan which days you train which exercises.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(routines, key = { it.id }) { routine ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        routine.name,
                        style = LedgerFigureEmphasis,
                        modifier = Modifier.weight(1f).padding(Spacing.Medium, Spacing.Medium, Spacing.Medium, Spacing.ExtraSmall),
                    )
                    IconButton(onClick = { viewModel.deleteRoutine(routine.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete ${routine.name}")
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
