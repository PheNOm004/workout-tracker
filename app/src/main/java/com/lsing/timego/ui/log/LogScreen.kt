package com.lsing.timego.ui.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import com.lsing.timego.data.MuscleGroup

@Composable
fun LogScreen(viewModel: LogViewModel = viewModel()) {
    val exercises by viewModel.displayedExercises.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val routines by viewModel.routines.collectAsState()
    val selectedRoutineId by viewModel.selectedRoutineId.collectAsState()

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text("Session type", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                FilterChip(
                    selected = selectedRoutineId == null,
                    onClick = { viewModel.selectRoutine(null) },
                    label = { Text("Freeform") },
                    modifier = Modifier.padding(end = 8.dp),
                )
                routines.forEach { routine ->
                    FilterChip(
                        selected = selectedRoutineId == routine.id,
                        onClick = { viewModel.selectRoutine(routine.id) },
                        label = { Text(routine.name) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }
        items(exercises, key = { it.id }) { exercise ->
            var weightText by remember(exercise.id) { mutableStateOf("") }
            var repsText by remember(exercise.id) { mutableStateOf("") }
            val suggestion = suggestions[exercise.id]

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                if (suggestion != null) {
                    Text(
                        "Suggested: ${suggestion.weightKg}kg x ${suggestion.reps} -- ${suggestion.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("kg") },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text("reps") },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Button(onClick = {
                        val weight = weightText.toDoubleOrNull()
                        val reps = repsText.toIntOrNull()
                        if (weight != null && reps != null) {
                            val target = suggestion?.reps ?: reps
                            viewModel.logSet(exercise.id, weight, reps, target)
                            weightText = ""
                            repsText = ""
                        }
                    }) {
                        Text("Log set")
                    }
                }
            }
        }
        item {
            AddCustomExerciseForm(onAdd = viewModel::addCustomExercise)
        }
    }
}

@Composable
private fun AddCustomExerciseForm(onAdd: (String, List<String>) -> Unit) {
    var name by remember { mutableStateOf("") }
    val selectedGroups = remember { mutableStateOf(setOf<String>()) }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text("Add Custom Exercise", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Exercise name") },
            modifier = Modifier.fillMaxWidth(),
        )
        MuscleGroup.entries.forEach { group ->
            val checked = group.name in selectedGroups.value
            Row {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { isChecked ->
                        selectedGroups.value = if (isChecked) {
                            selectedGroups.value + group.name
                        } else {
                            selectedGroups.value - group.name
                        }
                    },
                )
                Text(group.name)
            }
        }
        Button(
            onClick = {
                if (name.isNotBlank() && selectedGroups.value.isNotEmpty()) {
                    onAdd(name, selectedGroups.value.toList())
                    name = ""
                    selectedGroups.value = emptySet()
                }
            },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text("Add exercise")
        }
    }
}
