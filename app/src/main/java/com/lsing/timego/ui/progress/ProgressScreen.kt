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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.domain.PersonalRecord
import com.lsing.timego.domain.PrType
import com.lsing.timego.ui.common.HeatmapGrid
import com.lsing.timego.ui.common.formatEnumLabel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
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
                val selectedExerciseName = exercises.firstOrNull { it.id == selectedExerciseId }?.name ?: "Choose an exercise"
                var dropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedExerciseName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Exercise") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                    ) {
                        exercises.forEach { exercise ->
                            DropdownMenuItem(
                                text = { Text(exercise.name) },
                                onClick = {
                                    viewModel.selectExercise(exercise.id)
                                    dropdownExpanded = false
                                },
                            )
                        }
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

/** Plain Canvas line chart -- no charting library dependency, same "draw it yourself" approach
 *  HeatP used for its WeeklyBarChart. A dotted average-value reference line (sparkline style,
 *  inspired by mobile strength-tracking apps' per-muscle trend rows) gives the curve context
 *  without needing full axis chrome; min/max/date labels anchor it further. Padding keeps the
 *  end points and labels from clipping against the canvas edge. */
@Composable
private fun StrengthCurveChart(points: List<Pair<LocalDate, Double>>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val averageLineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = androidx.compose.ui.platform.LocalDensity.current
    val labelTextSizePx = with(density) { 12.sp.toPx() }
    val horizontalPaddingPx = with(density) { 8.dp.toPx() }
    val topPaddingPx = with(density) { 12.dp.toPx() }
    val bottomPaddingPx = with(density) { 28.dp.toPx() }

    Canvas(modifier = modifier) {
        if (points.size < 2) {
            return@Canvas
        }
        val maxValue = points.maxOf { it.second }
        val minValue = points.minOf { it.second }
        val average = points.map { it.second }.average()
        val range = (maxValue - minValue).coerceAtLeast(1.0)
        val plotWidth = size.width - horizontalPaddingPx * 2
        val plotHeight = size.height - topPaddingPx - bottomPaddingPx
        val stepX = plotWidth / (points.size - 1)

        fun xFor(index: Int) = horizontalPaddingPx + stepX * index
        fun yFor(value: Double) = topPaddingPx + plotHeight - ((value - minValue) / range * plotHeight).toFloat()

        val averageY = yFor(average)
        drawLine(
            color = averageLineColor.copy(alpha = 0.4f),
            start = Offset(horizontalPaddingPx, averageY),
            end = Offset(size.width - horizontalPaddingPx, averageY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
        )

        val path = Path()
        points.forEachIndexed { index, (_, value) ->
            val x = xFor(index)
            val y = yFor(value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        points.forEachIndexed { index, (_, value) ->
            drawCircle(color = lineColor, radius = 5f, center = Offset(xFor(index), yFor(value)))
        }

        val nativePaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = labelTextSizePx
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.apply {
            nativePaint.textAlign = android.graphics.Paint.Align.LEFT
            drawText(points.first().first.toString(), horizontalPaddingPx, size.height - 8f, nativePaint)
            nativePaint.textAlign = android.graphics.Paint.Align.RIGHT
            drawText(points.last().first.toString(), size.width - horizontalPaddingPx, size.height - 8f, nativePaint)
            nativePaint.textAlign = android.graphics.Paint.Align.LEFT
            drawText("avg %.1f".format(average), horizontalPaddingPx, averageY - 8f, nativePaint)
        }
    }
}
