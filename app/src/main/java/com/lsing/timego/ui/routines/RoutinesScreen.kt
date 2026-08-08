package com.lsing.timego.ui.routines

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.ui.common.ExerciseSections
import com.lsing.timego.ui.common.formatEnumLabel
import java.time.DayOfWeek

@Composable
fun RoutinesScreen(viewModel: RoutinesViewModel = viewModel()) {
    val routines by viewModel.routines.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val untrainedGroups by viewModel.untrainedGroups.collectAsState()

    var routineName by remember { mutableStateOf("") }
    val selectedExerciseIds = remember { mutableStateOf(setOf<Long>()) }
    val selectedDays = remember { mutableStateOf(setOf<String>()) }

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
            Text("Your Routines", style = MaterialTheme.typography.titleMedium)
        }
        items(routines, key = { it.id }) { routine ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    "${routine.name} -- ${if (routine.daysOfWeek.isEmpty()) "no days set" else routine.daysOfWeek.joinToString(", ") { formatEnumLabel(it) }}",
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        item {
            Text("New Routine", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            OutlinedTextField(
                value = routineName,
                onValueChange = { routineName = it },
                label = { Text("Routine name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Days", modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                DayOfWeek.entries.forEach { day ->
                    val checked = day.name in selectedDays.value
                    FilterChip(
                        selected = checked,
                        onClick = {
                            selectedDays.value = if (checked) selectedDays.value - day.name else selectedDays.value + day.name
                        },
                        label = { Text(day.name.take(3).lowercase().replaceFirstChar(Char::uppercase)) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }
        }
        item {
            ExerciseSections(exercises = exercises) { exercise ->
                val checked = exercise.id in selectedExerciseIds.value
                Row {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            selectedExerciseIds.value = if (isChecked) {
                                selectedExerciseIds.value + exercise.id
                            } else {
                                selectedExerciseIds.value - exercise.id
                            }
                        },
                    )
                    Text(exercise.name)
                }
            }
        }
        item {
            Button(
                onClick = {
                    if (routineName.isNotBlank() && selectedExerciseIds.value.isNotEmpty()) {
                        viewModel.createRoutine(routineName, selectedExerciseIds.value.toList(), selectedDays.value.toList())
                        routineName = ""
                        selectedExerciseIds.value = emptySet()
                        selectedDays.value = emptySet()
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Create routine")
            }
        }
    }
}
