package com.lsing.timego.ui.progress

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.domain.ProgressTimeframe
import com.lsing.timego.domain.PrType
import com.lsing.timego.domain.BmiCategory
import com.lsing.timego.domain.bmiCategory
import com.lsing.timego.domain.formatCalisthenicsWeight
import com.lsing.timego.domain.orderedMuscleDistributionForChart
import com.lsing.timego.ui.common.DayHistoryEntry
import com.lsing.timego.ui.common.HeatmapGrid
import com.lsing.timego.ui.common.HorizontalWheelPicker
import com.lsing.timego.ui.common.MuscleBalanceBars
import com.lsing.timego.ui.common.MuscleBodyDiagram
import com.lsing.timego.ui.common.RadarChart
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.common.SparklineChart
import com.lsing.timego.ui.common.StatTile
import com.lsing.timego.ui.common.SurfaceCard
import com.lsing.timego.ui.common.WorkoutHistoryDialog
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.common.rankedMuscleBalanceBars
import com.lsing.timego.ui.common.timeframeLabel
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.NightAmber
import com.lsing.timego.ui.theme.NightCoral
import com.lsing.timego.ui.theme.NightCoralShade
import com.lsing.timego.ui.theme.NightMint
import com.lsing.timego.ui.theme.TimeGoMotion
import com.lsing.timego.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class BalanceChartMode { BARS, RADAR }

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = viewModel()) {
    val volumeRatios by viewModel.volumeRatios.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val selectedExerciseId by viewModel.selectedExerciseId.collectAsStateWithLifecycle()
    val curveMode by viewModel.curveMode.collectAsStateWithLifecycle()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsStateWithLifecycle()
    val strengthCurve by viewModel.strengthCurve.collectAsStateWithLifecycle()
    val bodyMetrics by viewModel.bodyMetrics.collectAsStateWithLifecycle()
    val weightCurve by viewModel.weightCurve.collectAsStateWithLifecycle()
    val currentBmi by viewModel.currentBmi.collectAsStateWithLifecycle()
    val muscleDistribution by viewModel.muscleDistribution.collectAsStateWithLifecycle()
    val muscleBalance by viewModel.muscleBalance.collectAsStateWithLifecycle()
    val previousMuscleBalance by viewModel.previousMuscleBalance.collectAsStateWithLifecycle()
    val trainingStats by viewModel.trainingStats.collectAsStateWithLifecycle()
    val timeframe by viewModel.timeframe.collectAsStateWithLifecycle()

    var weightText by remember { mutableStateOf("") }
    var waistText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var selectedPrExerciseId by remember { mutableStateOf<Long?>(null) }
    var balanceChartMode by remember { mutableStateOf(BalanceChartMode.BARS) }

    val selectedHistoryDate by viewModel.selectedHistoryDate.collectAsStateWithLifecycle()
    val historyForSelectedDate by viewModel.historyForSelectedDate.collectAsStateWithLifecycle()
    val historyLabel by viewModel.historyLabel.collectAsStateWithLifecycle()

    if (selectedHistoryDate != null) {
        WorkoutHistoryDialog(
            title = "Workout on ${selectedHistoryDate!!}",
            entries = historyForSelectedDate,
            onDismiss = { viewModel.selectHistoryDate(null) },
            label = historyLabel,
        )
    }

    LazyColumn(modifier = Modifier.padding(Spacing.Large)) {
        item {
            Text(
                "Progress",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = Spacing.ExtraSmall, bottom = Spacing.Small),
            )
            Text(
                "A clear read on how your training is moving.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.Small),
            )
        }
        item {
            SectionHeader("Consistency", topPadding = Spacing.ExtraSmall)
            HeatmapGrid(
                ratios = volumeRatios,
                lightColor = NightCoral,
                darkColor = NightCoralShade,
                onDateClick = { date -> viewModel.selectHistoryDate(date) },
            )
        }
        item {
            SectionHeader("Muscle Balance (${timeframeLabel(timeframe)})")
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                ProgressTimeframe.entries.forEach { option ->
                    FilterChip(
                        selected = timeframe == option,
                        onClick = { viewModel.selectTimeframe(option) },
                        label = { Text(formatEnumLabel(option.name)) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }
            Text(
                "Bars rank progress toward the weekly target. Δ compares the prior equal period; the radar remains available for the overall shape.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            if (muscleDistribution.isEmpty()) {
                Text(
                    "No strength sets logged (${timeframeLabel(timeframe)}) yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Small),
                ) {
                    BalanceChartMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = balanceChartMode == mode,
                            onClick = { balanceChartMode = mode },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = BalanceChartMode.entries.size),
                            label = { Text(if (mode == BalanceChartMode.BARS) "Bars" else "Radar") },
                        )
                    }
                }
                when (balanceChartMode) {
                    BalanceChartMode.BARS -> MuscleBalanceBars(
                        entries = rankedMuscleBalanceBars(muscleBalance, previousMuscleBalance),
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Small),
                    )
                    BalanceChartMode.RADAR -> RadarChart(
                        values = orderedMuscleDistributionForChart(muscleBalance)
                            .mapKeys { (group, _) -> formatEnumLabel(group) },
                        comparisonValues = orderedMuscleDistributionForChart(previousMuscleBalance)
                            .mapKeys { (group, _) -> formatEnumLabel(group) },
                        modifier = Modifier.fillMaxWidth().height(220.dp).padding(vertical = 8.dp),
                    )
                }
                MuscleBodyDiagram(
                    intensities = muscleDistribution,
                    periodLabel = timeframeLabel(timeframe),
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
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
                    hero = true,
                ) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        // Exercise name dropped -- it's already the centered item on the wheel
                        // picker directly above this card, repeating it here was redundant.
                        AnimatedContent(
                            targetState = selectedExercise.id,
                            transitionSpec = {
                                fadeIn(TimeGoMotion.contentEnter) togetherWith fadeOut(TimeGoMotion.contentExit)
                            },
                        ) { exerciseId ->
                            val exerciseRecords = recordsByExercise[exerciseId].orEmpty()
                            Row(modifier = Modifier.fillMaxWidth()) {
                                if (selectedExercise.loggingType == LoggingType.HOLD.name) {
                                    val record = exerciseRecords.firstOrNull { it.type == PrType.LONGEST_HOLD }
                                    StatTile(
                                        label = "Longest Hold",
                                        value = record?.let { "${it.value.toInt()}s" } ?: "--",
                                        caption = record?.achievedOn?.format(PR_DATE_FORMATTER),
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    // All three tiles read off the same best-set record (weight, reps,
                                    // weight*reps) -- they used to be independently maximized across
                                    // different sets, which could report a heavy triple's weight next to
                                    // a lighter high-rep set's reps as if one set did both.
                                    val record = exerciseRecords.firstOrNull { it.type == PrType.BEST_SET }
                                    val caption = record?.achievedOn?.format(PR_DATE_FORMATTER)
                                    val isBodyweight = selectedExercise.category == ExerciseCategory.CALISTHENICS.name
                                    StatTile(
                                        label = "Weight",
                                        value = record?.let {
                                            if (isBodyweight && it.addedWeightKg != null) {
                                                formatCalisthenicsWeight(it.addedWeightKg)
                                            } else {
                                                "%.1fkg".format(it.value)
                                            }
                                        } ?: "--",
                                        caption = caption,
                                        modifier = Modifier.weight(1f),
                                    )
                                    StatTile(
                                        label = "Reps",
                                        value = record?.secondaryValue?.let { "${it.toInt()}" } ?: "--",
                                        caption = caption,
                                        modifier = Modifier.weight(1f),
                                    )
                                    StatTile(
                                        label = "Total Weight",
                                        value = record?.let { "%.1fkg".format(it.value * (it.secondaryValue ?: 0.0)) } ?: "--",
                                        caption = caption,
                                        modifier = Modifier.weight(1f),
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
                    fadeIn(TimeGoMotion.contentEnter) togetherWith fadeOut(TimeGoMotion.contentExit)
                },
            ) { targetCurveKey ->
                key(targetCurveKey) {
                    if (strengthCurve.isEmpty()) {
                        Text("No logged sets yet for this selection.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        SparklineChart(strengthCurve, modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp))
                    }
                }
            }
        }
        item {
            SectionHeader("Body Metrics")
            if (currentBmi != null) {
                val category = bmiCategory(currentBmi!!)
                val bmiColor = when (category) {
                    BmiCategory.NORMAL -> NightMint
                    BmiCategory.OVERWEIGHT -> NightAmber
                    BmiCategory.UNDERWEIGHT, BmiCategory.OBESE -> MaterialTheme.colorScheme.error
                }
                Text(
                    "BMI: %.1f (${formatEnumLabel(category.name)})".format(currentBmi),
                    style = LedgerFigureValue.copy(fontSize = 14.sp),
                    color = bmiColor,
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

private val PR_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d")
