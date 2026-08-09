package com.lsing.timego.ui.progress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.domain.PersonalRecord
import com.lsing.timego.domain.PrType
import com.lsing.timego.domain.BmiCategory
import com.lsing.timego.domain.bmiCategory
import com.lsing.timego.ui.common.HeatmapGrid
import com.lsing.timego.ui.common.HorizontalWheelPicker
import com.lsing.timego.ui.common.MuscleBodyDiagram
import com.lsing.timego.ui.common.RadarChart
import com.lsing.timego.ui.common.SparklineChart
import com.lsing.timego.ui.common.formatEnumLabel
import java.time.LocalDate

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = viewModel()) {
    val volumeRatios by viewModel.volumeRatios.collectAsState()
    val records by viewModel.records.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val selectedExerciseId by viewModel.selectedExerciseId.collectAsState()
    val curveMode by viewModel.curveMode.collectAsState()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val strengthCurve by viewModel.strengthCurve.collectAsState()
    val bodyMetrics by viewModel.bodyMetrics.collectAsState()
    val weightCurve by viewModel.weightCurve.collectAsState()
    val currentBmi by viewModel.currentBmi.collectAsState()
    val muscleDistribution by viewModel.muscleDistribution.collectAsState()
    val trainingStats by viewModel.trainingStats.collectAsState()

    var weightText by remember { mutableStateOf("") }
    var waistText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }

    val selectedHistoryDate by viewModel.selectedHistoryDate.collectAsState()
    val historyForSelectedDate by viewModel.historyForSelectedDate.collectAsState()

    if (selectedHistoryDate != null) {
        DayHistoryDialog(
            date = selectedHistoryDate!!,
            entries = historyForSelectedDate,
            onDismiss = { viewModel.selectHistoryDate(null) },
        )
    }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text("Consistency", style = MaterialTheme.typography.titleMedium)
            HeatmapGrid(
                ratios = volumeRatios,
                lightColor = Color(0xFF7FD8A0),
                darkColor = Color(0xFF1B5E3A),
                onDateClick = { date -> viewModel.selectHistoryDate(date) },
            )
        }
        item {
            Text("Muscle Distribution (last 30 days)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            if (muscleDistribution.isEmpty()) {
                Text(
                    "No strength sets logged in the last 30 days yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                RadarChart(
                    values = muscleDistribution.mapKeys { (group, _) -> formatEnumLabel(group) },
                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(vertical = 8.dp),
                )
                MuscleBodyDiagram(
                    intensities = muscleDistribution,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                StatTile("Workouts", trainingStats.workouts.toString())
                StatTile("Duration", "${trainingStats.totalDurationMinutes.toInt()} min")
                StatTile("Volume", "${trainingStats.totalVolumeKg.toInt()} kg")
                StatTile("Sets", trainingStats.totalSets.toString())
            }
        }
        item {
            Text("Personal Records", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        }
        if (records.isEmpty()) {
            item {
                Text(
                    "No personal records yet -- log a few sets to see them here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
        val recordsByExercise = records.groupBy { it.exerciseId }
        items(recordsByExercise.entries.toList(), key = { it.key }) { (exerciseId, exerciseRecords) ->
            val exerciseName = exercises.firstOrNull { it.id == exerciseId }?.name ?: "Unknown exercise"
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(exerciseName, style = MaterialTheme.typography.titleSmall)
                    exerciseRecords.forEach { record ->
                        Text(
                            "${formatEnumLabel(record.type.name)}: ${formatRecordValue(record)} on ${record.achievedOn}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        item {
            Text("Strength Curve", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                FilterChip(
                    selected = curveMode == CurveMode.EXERCISE,
                    onClick = { selectedExerciseId?.let { viewModel.selectExercise(it) } },
                    label = { Text("This exercise") },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = curveMode == CurveMode.MUSCLE_GROUP,
                    onClick = { viewModel.selectMuscleGroup(selectedMuscleGroup ?: MuscleGroup.entries.first().name) },
                    label = { Text("Muscle group") },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        if (curveMode == CurveMode.EXERCISE) {
            item {
                if (exercises.isNotEmpty()) {
                    val selectedIndex = exercises.indexOfFirst { it.id == selectedExerciseId }.coerceAtLeast(0)
                    HorizontalWheelPicker(
                        items = exercises.map { it.name },
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = { index -> viewModel.selectExercise(exercises[index].id) },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        } else {
            item {
                val groupNames = MuscleGroup.entries.map { it.name }
                val selectedIndex = groupNames.indexOf(selectedMuscleGroup).coerceAtLeast(0)
                HorizontalWheelPicker(
                    items = groupNames.map { formatEnumLabel(it) },
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = { index -> viewModel.selectMuscleGroup(groupNames[index]) },
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
        item {
            if (strengthCurve.isEmpty()) {
                Text("No logged sets yet for this selection.", style = MaterialTheme.typography.bodySmall)
            } else {
                SparklineChart(strengthCurve, modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp))
            }
        }
        item {
            Text("Body Metrics", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            if (currentBmi != null) {
                val category = bmiCategory(currentBmi!!)
                Text(
                    "BMI: %.1f (${formatEnumLabel(category.name)})".format(currentBmi),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (category == BmiCategory.NORMAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text(
                    "Log a weight and height to see your BMI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (weightCurve.isNotEmpty()) {
                Text("Weight trend", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                SparklineChart(weightCurve, modifier = Modifier.fillMaxWidth().height(140.dp).padding(vertical = 8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                OutlinedTextField(
                    value = waistText,
                    onValueChange = { waistText = it },
                    label = { Text("Waist (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it },
                    label = { Text("Height (cm, optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Button(onClick = {
                    viewModel.logBodyMetric(weightText.toDoubleOrNull(), waistText.toDoubleOrNull(), heightText.toDoubleOrNull())
                    weightText = ""
                    waistText = ""
                    heightText = ""
                }) {
                    Text("Log")
                }
            }
        }
        items(bodyMetrics, key = { it.id }) { metric ->
            Text("${metric.date}: ${metric.weightKg?.let { "${it}kg" } ?: "--"} / ${metric.waistCm?.let { "${it}cm" } ?: "--"}")
        }
    }
}

/** Set/Name/Reps/Weight table, one row per logged set that day -- inspired by the set-log table
 *  pattern common in workout-tracking apps. No muscle-diagram illustration (would need real
 *  per-muscle-group vector art, a bigger asset investment than this pass); the exercise's own
 *  muscle-group tags are already visible elsewhere (Log/Routines section headers). */
@Composable
private fun DayHistoryDialog(date: LocalDate, entries: List<DayHistoryEntry>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Workout on $date") },
        text = {
            if (entries.isEmpty()) {
                Text("No sets logged on this day.")
            } else {
                Column {
                    entries.forEachIndexed { index, entry ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text("${index + 1}", modifier = Modifier.padding(end = 12.dp))
                            Text(entry.exerciseName, modifier = Modifier.weight(1f))
                            Text(entry.description)
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

private fun formatRecordValue(record: PersonalRecord): String = when (record.type) {
    PrType.HEAVIEST_WEIGHT -> "${record.value}kg"
    PrType.MOST_REPS -> "${record.value.toInt()} reps"
    PrType.BEST_VOLUME -> "${record.value}kg total"
}

@Composable
private fun StatTile(label: String, value: String) {
    Card(modifier = Modifier.padding(4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
