package com.lsing.timego.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.domain.MET_CARDIO
import com.lsing.timego.domain.MET_WARMUP
import com.lsing.timego.domain.averagePaceMinPerKm
import com.lsing.timego.domain.estimatedCalorieBurn
import com.lsing.timego.ui.common.ExerciseSections

@Composable
fun LogScreen(viewModel: LogViewModel = viewModel()) {
    val exercises by viewModel.displayedExercises.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val routines by viewModel.routines.collectAsState()
    val selectedRoutineId by viewModel.selectedRoutineId.collectAsState()
    val latestBodyWeightKg by viewModel.latestBodyWeightKg.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onAdd = viewModel::addCustomExercise,
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add custom exercise")
            }
        },
    ) { fabPadding ->
        LazyColumn(modifier = Modifier.padding(16.dp).padding(fabPadding)) {
            item {
                Text("Session type", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).horizontalScroll(rememberScrollState())) {
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
            item {
                ExerciseSections(exercises = exercises) { exercise ->
                    if (exercise.category == ExerciseCategory.CARDIO.name || exercise.category == ExerciseCategory.WARMUP.name) {
                        CardioLogRow(
                            exerciseName = exercise.name,
                            met = if (exercise.category == ExerciseCategory.CARDIO.name) MET_CARDIO else MET_WARMUP,
                            bodyWeightKg = latestBodyWeightKg,
                            onLog = { duration, distance -> viewModel.logCardioSet(exercise.id, duration, distance) },
                        )
                    } else {
                        StrengthLogRow(
                            exerciseName = exercise.name,
                            suggestion = suggestions[exercise.id],
                            isBodyweight = exercise.category == ExerciseCategory.CALISTHENICS.name,
                            latestBodyWeightKg = latestBodyWeightKg,
                            onLog = { weight, reps, target -> viewModel.logSet(exercise.id, weight, reps, target) },
                        )
                    }
                }
            }
            item {
                // Bottom spacer so the last exercise card isn't hidden behind the FAB.
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

/** A one-line header (just the exercise name) that expands into the actual logging inputs when
 *  tapped. Defaults to collapsed -- rendering full input rows for every exercise in a 119-strong
 *  library at once was both visually overwhelming and wasteful, per user feedback. */
@Composable
private fun ExerciseRowHeader(exerciseName: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(12.dp),
    ) {
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
            contentDescription = if (expanded) "Collapse" else "Expand",
            modifier = Modifier.padding(end = 4.dp),
        )
        Text(exerciseName, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun StrengthLogRow(
    exerciseName: String,
    suggestion: com.lsing.timego.domain.OverloadSuggestion?,
    isBodyweight: Boolean,
    latestBodyWeightKg: Double?,
    onLog: (weightKg: Double, reps: Int, targetReps: Int) -> Unit,
) {
    var expanded by remember(exerciseName) { mutableStateOf(false) }
    // Bodyweight exercises (Pull-Up, Push-Up, Dip, ...) pre-fill kg with the user's latest logged
    // body weight rather than leaving it blank/0 -- an unedited bodyweight set is still real load,
    // and 0 would flatten estimatedOneRepMax to zero forever regardless of actual rep progress.
    var weightText by remember(exerciseName) {
        mutableStateOf(if (isBodyweight) latestBodyWeightKg?.toString().orEmpty() else "")
    }
    var repsText by remember(exerciseName) { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp)) {
        ExerciseRowHeader(exerciseName, expanded) { expanded = !expanded }
        if (expanded) {
            if (suggestion != null) {
                Text(
                    "Suggested: ${suggestion.weightKg}kg x ${suggestion.reps} -- ${suggestion.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("kg") },
                    placeholder = if (isBodyweight) { { Text("BW") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it },
                    label = { Text("reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Button(onClick = {
                    val weight = weightText.toDoubleOrNull()
                    val reps = repsText.toIntOrNull()
                    if (weight != null && reps != null) {
                        onLog(weight, reps, suggestion?.reps ?: reps)
                        weightText = ""
                        repsText = ""
                    }
                }) {
                    Text("Log set")
                }
            }
        }
    }
}

@Composable
private fun CardioLogRow(
    exerciseName: String,
    met: Double,
    bodyWeightKg: Double?,
    onLog: (durationMinutes: Double, distanceKm: Double?) -> Unit,
) {
    var expanded by remember(exerciseName) { mutableStateOf(false) }
    var durationText by remember(exerciseName) { mutableStateOf("") }
    var distanceText by remember(exerciseName) { mutableStateOf("") }
    val duration = durationText.toDoubleOrNull()
    val distance = distanceText.toDoubleOrNull()

    Card(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp)) {
        ExerciseRowHeader(exerciseName, expanded) { expanded = !expanded }
        if (expanded) {
            if (duration != null && duration > 0) {
                val pace = distance?.let { averagePaceMinPerKm(duration, it) }
                val calories = bodyWeightKg?.let { estimatedCalorieBurn(met, it, duration) }
                val details = listOfNotNull(
                    pace?.let { "Pace: ${"%.1f".format(it)} min/km" },
                    calories?.let { "~${it.toInt()} kcal" },
                ).joinToString(" -- ")
                if (details.isNotEmpty()) {
                    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = { distanceText = it },
                    label = { Text("km (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Button(onClick = {
                    if (duration != null && duration > 0) {
                        onLog(duration, distance)
                        durationText = ""
                        distanceText = ""
                    }
                }) {
                    Text("Log")
                }
            }
        }
    }
}
