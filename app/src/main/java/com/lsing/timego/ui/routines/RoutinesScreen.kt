package com.lsing.timego.ui.routines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.lsing.timego.ui.common.formatEnumLabel

@Composable
fun RoutinesScreen(viewModel: RoutinesViewModel = viewModel()) {
    val routines by viewModel.routines.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val untrainedGroups by viewModel.untrainedGroups.collectAsState()
    var showRoutineForm by remember { mutableStateOf(false) }

    if (showRoutineForm) {
        RoutineFormDialog(
            exercises = exercises,
            onDismiss = { showRoutineForm = false },
            onCreate = viewModel::createRoutine,
        )
    }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        if (untrainedGroups.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text(
                        "Not trained in a while: ${untrainedGroups.joinToString(", ") { formatEnumLabel(it) }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Text("Your Routines", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Button(onClick = { showRoutineForm = true }) { Text("+ New routine") }
            }
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
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column {
                    Text(routine.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(12.dp, 12.dp, 12.dp, 4.dp))
                    if (routine.daysOfWeek.isEmpty()) {
                        Text(
                            "No days set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp, 0.dp, 12.dp, 12.dp),
                        )
                    } else {
                        FlowRow(modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 8.dp)) {
                            routine.daysOfWeek.forEach { day ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(day.take(3).lowercase().replaceFirstChar(Char::uppercase)) },
                                    modifier = Modifier.padding(4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
