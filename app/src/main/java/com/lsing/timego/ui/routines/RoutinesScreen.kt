package com.lsing.timego.ui.routines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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

@Composable
fun RoutinesScreen(viewModel: RoutinesViewModel = viewModel()) {
    val routines by viewModel.routines.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val untrainedGroups by viewModel.untrainedGroups.collectAsState()

    var routineName by remember { mutableStateOf("") }
    val selectedExerciseIds = remember { mutableStateOf(setOf<Long>()) }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        if (untrainedGroups.isNotEmpty()) {
            item {
                Text(
                    "Not trained in a while: ${untrainedGroups.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
        item {
            Text("Your Routines", style = MaterialTheme.typography.titleMedium)
        }
        items(routines, key = { it.id }) { routine ->
            Text(routine.name, modifier = Modifier.padding(vertical = 4.dp))
        }
        item {
            Text("New Routine", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            OutlinedTextField(
                value = routineName,
                onValueChange = { routineName = it },
                label = { Text("Routine name") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        items(exercises, key = { it.id }) { exercise ->
            val checked = exercise.id in selectedExerciseIds.value
            Column(modifier = Modifier.fillMaxWidth()) {
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
        item {
            Button(
                onClick = {
                    if (routineName.isNotBlank() && selectedExerciseIds.value.isNotEmpty()) {
                        viewModel.createRoutine(routineName, selectedExerciseIds.value.toList())
                        routineName = ""
                        selectedExerciseIds.value = emptySet()
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Create routine")
            }
        }
    }
}
