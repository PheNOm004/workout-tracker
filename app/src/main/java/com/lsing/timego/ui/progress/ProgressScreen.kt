package com.lsing.timego.ui.progress

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.domain.PrType
import com.lsing.timego.domain.BmiCategory
import com.lsing.timego.domain.bmiCategory
import com.lsing.timego.ui.common.HeatmapGrid
import com.lsing.timego.ui.common.HorizontalWheelPicker
import com.lsing.timego.ui.common.MuscleBodyDiagram
import com.lsing.timego.ui.common.RadarChart
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.common.SparklineChart
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    var selectedPrExerciseId by remember { mutableStateOf<Long?>(null) }

    val selectedHistoryDate by viewModel.selectedHistoryDate.collectAsState()
    val historyForSelectedDate by viewModel.historyForSelectedDate.collectAsState()

    if (selectedHistoryDate != null) {
        DayHistoryDialog(
            date = selectedHistoryDate!!,
            entries = historyForSelectedDate,
            onDismiss = { viewModel.selectHistoryDate(null) },
        )
    }

    LazyColumn(modifier = Modifier.padding(Spacing.Large)) {
        item {
            SectionHeader("Consistency", topPadding = Spacing.ExtraSmall)
            HeatmapGrid(
                ratios = volumeRatios,
                lightColor = Color(0xFFE12D23),
                darkColor = Color(0xFF5C1A14),
                onDateClick = { date -> viewModel.selectHistoryDate(date) },
            )
        }
        item {
            SectionHeader("Muscle Distribution (last 30 days)")
            Text(
                "Colors show volume relative to your most-trained muscle group this period",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
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
            SectionHeader("Personal Records")
        }
        val recordsByExercise = records.groupBy { it.exerciseId }
        val exercisesWithRecords = exercises.filter { it.id in recordsByExercise.keys }.sortedBy { it.name }
        if (exercisesWithRecords.isEmpty()) {
            item {
                Text(
                    "No personal records yet -- log a few sets to see them here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        } else {
            item {
                val selectedIndex = exercisesWithRecords.indexOfFirst { it.id == selectedPrExerciseId }.coerceAtLeast(0)
                HorizontalWheelPicker(
                    items = exercisesWithRecords.map { it.name },
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = { index -> selectedPrExerciseId = exercisesWithRecords[index].id },
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                val selectedExercise = exercisesWithRecords[selectedIndex]
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        Text(selectedExercise.name, style = MaterialTheme.typography.titleSmall)
                        AnimatedContent(
                            targetState = selectedExercise.id,
                            transitionSpec = {
                                (fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                                    scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))) togetherWith
                                    (fadeOut(spring(stiffness = Spring.StiffnessHigh)) +
                                        scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessHigh)))
                            },
                            modifier = Modifier.padding(top = 8.dp),
                        ) { exerciseId ->
                            val exerciseRecords = recordsByExercise[exerciseId].orEmpty()
                            Row(modifier = Modifier.fillMaxWidth()) {
                                if (selectedExercise.loggingType == LoggingType.HOLD.name) {
                                    val record = exerciseRecords.firstOrNull { it.type == PrType.LONGEST_HOLD }
                                    StatTile(
                                        label = "Longest Hold",
                                        value = record?.let { "${it.value.toInt()}s" } ?: "--",
                                        caption = record?.achievedOn?.format(PR_DATE_FORMATTER),
                                    )
                                } else {
                                    // All three tiles read off the same best-set record (weight, reps,
                                    // weight*reps) -- they used to be independently maximized across
                                    // different sets, which could report a heavy triple's weight next to
                                    // a lighter high-rep set's reps as if one set did both.
                                    val record = exerciseRecords.firstOrNull { it.type == PrType.BEST_SET }
                                    val caption = record?.achievedOn?.format(PR_DATE_FORMATTER)
                                    StatTile(
                                        label = "Weight",
                                        value = record?.let { "${it.value}kg" } ?: "--",
                                        caption = caption,
                                    )
                                    StatTile(
                                        label = "Reps",
                                        value = record?.secondaryValue?.let { "${it.toInt()}" } ?: "--",
                                        caption = caption,
                                    )
                                    StatTile(
                                        label = "Total Weight",
                                        value = record?.let { "${it.value * (it.secondaryValue ?: 0.0)}kg" } ?: "--",
                                        caption = caption,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionHeader("Strength Curve")
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
            val curveKey = if (curveMode == CurveMode.EXERCISE) selectedExerciseId else selectedMuscleGroup
            AnimatedContent(
                targetState = curveKey,
                transitionSpec = {
                    (fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                        scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))) togetherWith
                        (fadeOut(spring(stiffness = Spring.StiffnessHigh)) +
                            scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessHigh)))
                },
            ) {
                if (strengthCurve.isEmpty()) {
                    Text("No logged sets yet for this selection.", style = MaterialTheme.typography.bodySmall)
                } else {
                    SparklineChart(strengthCurve, modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp))
                }
            }
        }
        item {
            SectionHeader("Body Metrics")
            if (currentBmi != null) {
                val category = bmiCategory(currentBmi!!)
                Text(
                    "BMI: %.1f (${formatEnumLabel(category.name)})".format(currentBmi),
                    style = LedgerFigureValue.copy(fontSize = 14.sp),
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
            Text(
                "${metric.date}: ${metric.weightKg?.let { "${it}kg" } ?: "--"} / ${metric.waistCm?.let { "${it}cm" } ?: "--"}",
                style = LedgerFigureValue.copy(fontSize = 13.sp),
            )
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
                            Text("${index + 1}", style = LedgerFigureValue.copy(fontSize = 14.sp), modifier = Modifier.padding(end = 12.dp))
                            Text(entry.exerciseName, modifier = Modifier.weight(1f))
                            Text(entry.description, style = LedgerFigureValue.copy(fontSize = 14.sp))
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

private val PR_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d")

@Composable
private fun StatTile(label: String, value: String, caption: String? = null) {
    Card(
        modifier = Modifier.padding(Spacing.ExtraSmall),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = LedgerFigureValue)
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
