package com.lsing.timego.ui.log

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Session type", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Button(onClick = { showAddDialog = true }) { Text("+ Add exercise") }
            }
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
                        onLog = { weight, reps, target -> viewModel.logSet(exercise.id, weight, reps, target) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StrengthLogRow(
    exerciseName: String,
    suggestion: com.lsing.timego.domain.OverloadSuggestion?,
    onLog: (weightKg: Double, reps: Int, targetReps: Int) -> Unit,
) {
    var weightText by remember(exerciseName) { mutableStateOf("") }
    var repsText by remember(exerciseName) { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp)) {
        Text(exerciseName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(12.dp, 12.dp, 12.dp, 0.dp))
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

@Composable
private fun CardioLogRow(
    exerciseName: String,
    met: Double,
    bodyWeightKg: Double?,
    onLog: (durationMinutes: Double, distanceKm: Double?) -> Unit,
) {
    var durationText by remember(exerciseName) { mutableStateOf("") }
    var distanceText by remember(exerciseName) { mutableStateOf("") }
    val duration = durationText.toDoubleOrNull()
    val distance = distanceText.toDoubleOrNull()

    Card(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp)) {
        Text(exerciseName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(12.dp, 12.dp, 12.dp, 0.dp))
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
