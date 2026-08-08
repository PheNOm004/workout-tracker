package com.lsing.timego.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.ui.common.HeatmapGrid

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = viewModel()) {
    val volumeRatios by viewModel.volumeRatios.collectAsState()
    val records by viewModel.records.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val selectedExerciseId by viewModel.selectedExerciseId.collectAsState()
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
        items(records) { record ->
            Text("${record.type}: ${record.value} on ${record.achievedOn}", modifier = Modifier.padding(vertical = 2.dp))
        }
        item {
            Text("Strength Curve", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        }
        items(exercises, key = { it.id }) { exercise ->
            Text(
                exercise.name,
                style = if (exercise.id == selectedExerciseId) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                color = if (exercise.id == selectedExerciseId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectExercise(exercise.id) }
                    .padding(vertical = 4.dp),
            )
        }
        item {
            if (strengthCurve.isEmpty()) {
                Text("No logged sets yet for this exercise.", style = MaterialTheme.typography.bodySmall)
            } else {
                Column {
                    strengthCurve.forEach { (date, oneRepMax) ->
                        Text("$date: est. 1RM ${"%.1f".format(oneRepMax)}kg")
                    }
                }
            }
        }
        item {
            Text("Body Metrics", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.padding(end = 8.dp),
                )
                OutlinedTextField(
                    value = waistText,
                    onValueChange = { waistText = it },
                    label = { Text("Waist (cm)") },
                    modifier = Modifier.padding(end = 8.dp),
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
