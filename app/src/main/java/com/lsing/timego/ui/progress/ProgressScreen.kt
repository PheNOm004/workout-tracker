package com.lsing.timego.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.domain.PersonalRecord
import com.lsing.timego.domain.PrType
import com.lsing.timego.ui.common.HeatmapGrid
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

    var weightText by remember { mutableStateOf("") }
    var waistText by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text("Consistency", style = MaterialTheme.typography.titleMedium)
            HeatmapGrid(
                ratios = volumeRatios,
                lightColor = Color(0xFF7FD8A0),
                darkColor = Color(0xFF1B5E3A),
            )
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
        items(records) { record ->
            val exerciseName = exercises.firstOrNull { it.id == record.exerciseId }?.name ?: "Unknown exercise"
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(exerciseName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${formatEnumLabel(record.type.name)}: ${formatRecordValue(record)} on ${record.achievedOn}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    exercises.forEach { exercise ->
                        FilterChip(
                            selected = exercise.id == selectedExerciseId,
                            onClick = { viewModel.selectExercise(exercise.id) },
                            label = { Text(exercise.name) },
                            modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
                        )
                    }
                }
            }
        } else {
            item {
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    MuscleGroup.entries.forEach { group ->
                        FilterChip(
                            selected = selectedMuscleGroup == group.name,
                            onClick = { viewModel.selectMuscleGroup(group.name) },
                            label = { Text(formatEnumLabel(group.name)) },
                            modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
                        )
                    }
                }
            }
        }
        item {
            if (strengthCurve.isEmpty()) {
                Text("No logged sets yet for this selection.", style = MaterialTheme.typography.bodySmall)
            } else {
                StrengthCurveChart(strengthCurve, modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp))
            }
        }
        item {
            Text("Body Metrics", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
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
                Button(onClick = {
                    viewModel.logBodyMetric(weightText.toDoubleOrNull(), waistText.toDoubleOrNull())
                    weightText = ""
                    waistText = ""
                }) {
                    Text("Log")
                }
            }
        }
        items(bodyMetrics) { metric ->
            Text("${metric.date}: ${metric.weightKg?.let { "${it}kg" } ?: "--"} / ${metric.waistCm?.let { "${it}cm" } ?: "--"}")
        }
    }
}

private fun formatRecordValue(record: PersonalRecord): String = when (record.type) {
    PrType.HEAVIEST_WEIGHT -> "${record.value}kg"
    PrType.MOST_REPS -> "${record.value.toInt()} reps"
    PrType.BEST_VOLUME -> "${record.value}kg total"
}

/** Plain Canvas line chart -- no charting library dependency, same "draw it yourself" approach
 *  HeatP used for its WeeklyBarChart. Normalizes [points]' values to the canvas height. */
@Composable
private fun StrengthCurveChart(points: List<Pair<LocalDate, Double>>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (points.size < 2) {
            return@Canvas
        }
        val maxValue = points.maxOf { it.second }
        val minValue = points.minOf { it.second }
        val range = (maxValue - minValue).coerceAtLeast(1.0)
        val stepX = size.width / (points.size - 1)
        val path = Path()
        points.forEachIndexed { index, (_, value) ->
            val x = stepX * index
            val y = size.height - ((value - minValue) / range * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 4f))
        points.forEachIndexed { index, (_, value) ->
            val x = stepX * index
            val y = size.height - ((value - minValue) / range * size.height).toFloat()
            drawCircle(color = lineColor, radius = 6f, center = Offset(x, y))
        }
    }
}
