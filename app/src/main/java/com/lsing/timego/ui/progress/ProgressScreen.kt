package com.lsing.timego.ui.progress

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
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
import com.lsing.timego.ui.common.MuscleBodyDiagram
import com.lsing.timego.ui.common.RadarChart
import com.lsing.timego.ui.common.PeriodBreakdownDialog
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.common.SparklineChart
import com.lsing.timego.ui.common.StatTile
import com.lsing.timego.ui.common.SurfaceCard
import com.lsing.timego.ui.common.WorkoutHistoryDialog
import com.lsing.timego.ui.common.toPositiveFiniteDoubleOrNull
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.common.timeframeLabel
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.NightAmber
import com.lsing.timego.ui.theme.NightCoral
import com.lsing.timego.ui.theme.NightCoralShade
import com.lsing.timego.ui.theme.NightMint
import com.lsing.timego.ui.theme.TimeGoMotion
import com.lsing.timego.ui.theme.Spacing
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class BalanceChartMode { DIAGRAM, RADAR }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProgressScreen(viewModel: ProgressViewModel = viewModel()) {
    val selectedSegment by viewModel.selectedSegment.collectAsStateWithLifecycle()
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
    val muscleSetSummaries by viewModel.muscleSetSummaries.collectAsStateWithLifecycle()
    val trainingStats by viewModel.trainingStats.collectAsStateWithLifecycle()
    val timeframe by viewModel.timeframe.collectAsStateWithLifecycle()

    val muscleBalance by viewModel.muscleBalance.collectAsStateWithLifecycle()
    val previousMuscleBalance by viewModel.previousMuscleBalance.collectAsStateWithLifecycle()
    var balanceChartMode by remember { mutableStateOf(BalanceChartMode.DIAGRAM) }

    var weightText by remember { mutableStateOf("") }
    var waistText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    val enteredWeight = weightText.toPositiveFiniteDoubleOrNull()
    val enteredWaist = waistText.toPositiveFiniteDoubleOrNull()
    val enteredHeight = heightText.toPositiveFiniteDoubleOrNull()
    val bodyMetricInputValid =
        (weightText.isBlank() || enteredWeight != null) &&
            (waistText.isBlank() || enteredWaist != null) &&
            (heightText.isBlank() || enteredHeight != null) &&
            (enteredWeight != null || enteredWaist != null || enteredHeight != null)
    var showPeriodBreakdown by remember { mutableStateOf(false) }

    val selectedHistoryDate by viewModel.selectedHistoryDate.collectAsStateWithLifecycle()
    val historyForSelectedDate by viewModel.historyForSelectedDate.collectAsStateWithLifecycle()
    val historyLabel by viewModel.historyLabel.collectAsStateWithLifecycle()
    val historyDurationMinutes by viewModel.historyDurationMinutes.collectAsStateWithLifecycle()
    val periodBreakdown by viewModel.periodBreakdown.collectAsStateWithLifecycle()

    val progressListState = rememberLazyListState(
        cacheWindow = LazyLayoutCacheWindow(aheadFraction = 1.5f, behindFraction = 1f),
    )

    if (selectedHistoryDate != null) {
        WorkoutHistoryDialog(
            title = "Workout on ${selectedHistoryDate!!}",
            entries = historyForSelectedDate,
            onDismiss = { viewModel.selectHistoryDate(null) },
            label = historyLabel,
            durationMinutes = historyDurationMinutes,
        )
    }
    if (showPeriodBreakdown) {
        PeriodBreakdownDialog(
            periodLabel = timeframeLabel(timeframe),
            days = periodBreakdown,
            onDismiss = { showPeriodBreakdown = false },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = progressListState,
            modifier = Modifier.padding(horizontal = Spacing.Large),
        ) {
            item {
                Text(
                    "Progress",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = Spacing.Large, bottom = Spacing.ExtraSmall),
                )
            }

        if (selectedSegment == ProgressSegment.TRAINING) {
            // ==================== TRAINING SEGMENT ====================
            item {
                SectionHeader("Consistency", topPadding = Spacing.ExtraSmall)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                ) {
                    HeatmapGrid(
                        ratios = volumeRatios,
                        lightColor = NightCoral,
                        darkColor = NightCoralShade,
                        onDateClick = { date -> viewModel.selectHistoryDate(date) },
                    )
                }
            }
            item {
                SectionHeader("Muscle Balance (${timeframeLabel(timeframe)})", topPadding = Spacing.Small)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                ) {
                    ProgressTimeframe.entries.forEach { option ->
                        FilterChip(
                            selected = timeframe == option,
                            onClick = { viewModel.selectTimeframe(option) },
                            label = { Text(formatEnumLabel(option.name)) },
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                ) {
                    FilterChip(
                        selected = balanceChartMode == BalanceChartMode.DIAGRAM,
                        onClick = { balanceChartMode = BalanceChartMode.DIAGRAM },
                        label = { Text("Anatomical Map") },
                    )
                    FilterChip(
                        selected = balanceChartMode == BalanceChartMode.RADAR,
                        onClick = { balanceChartMode = BalanceChartMode.RADAR },
                        label = { Text("Radar Chart") },
                    )
                }
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
                ) {
                    Column(modifier = Modifier.padding(Spacing.Small)) {
                        if (muscleDistribution.isEmpty()) {
                            Text(
                                "No strength sets logged in the ${timeframeLabel(timeframe)} yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = Spacing.ExtraSmall),
                            )
                        }
                        AnimatedContent(
                            targetState = balanceChartMode,
                            transitionSpec = { fadeIn(TimeGoMotion.contentEnter) togetherWith fadeOut(TimeGoMotion.contentExit) },
                            label = "balanceChartMode",
                        ) { mode ->
                            when (mode) {
                                BalanceChartMode.DIAGRAM -> {
                                    MuscleBodyDiagram(
                                        intensities = muscleDistribution,
                                        setSummaries = muscleSetSummaries,
                                        periodLabel = timeframeLabel(timeframe),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                                    )
                                }
                                BalanceChartMode.RADAR -> {
                                    RadarChart(
                                        values = orderedMuscleDistributionForChart(muscleDistribution)
                                            .mapKeys { (group, _) -> formatEnumLabel(group) },
                                        comparisonValues = orderedMuscleDistributionForChart(previousMuscleBalance)
                                            .mapKeys { (group, _) -> formatEnumLabel(group) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(260.dp)
                                            .padding(vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    val tapToBreakdown = Modifier.clickable { showPeriodBreakdown = true }
                    StatTile("Workouts", trainingStats.workouts.toString(), modifier = tapToBreakdown)
                    StatTile("Duration", "${trainingStats.totalDurationMinutes.toInt()} min", modifier = tapToBreakdown)
                    StatTile("Volume", "${trainingStats.totalVolumeKg.toInt()} kg", modifier = tapToBreakdown)
                    StatTile("Sets", trainingStats.totalSets.toString(), modifier = tapToBreakdown)
                }
            }

            // UNIFIED EXERCISE PERFORMANCE (PRs + Strength Progression Curve)
            item {
                SectionHeader("Exercise Performance", topPadding = Spacing.Small)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    FilterChip(
                        selected = curveMode == CurveMode.EXERCISE,
                        onClick = { selectedExerciseId?.let { viewModel.selectExercise(it) } },
                        label = { Text("By Exercise") },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    FilterChip(
                        selected = curveMode == CurveMode.MUSCLE_GROUP,
                        onClick = { viewModel.selectMuscleGroup(selectedMuscleGroup ?: MuscleGroup.entries.first().name) },
                        label = { Text("By Muscle Group") },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            item {
                AnimatedContent(
                    targetState = curveMode,
                    transitionSpec = { fadeIn(TimeGoMotion.contentEnter) togetherWith fadeOut(TimeGoMotion.contentExit) },
                    label = "curveModeTransition",
                ) { mode ->
                    when (mode) {
                        CurveMode.EXERCISE -> {
                            Column {
                                if (exercises.isNotEmpty()) {
                                    val selectedIndex = exercises.indexOfFirst { it.id == selectedExerciseId }.coerceAtLeast(0)
                                    HorizontalWheelPicker(
                                        items = exercises.map { it.name },
                                        selectedIndex = selectedIndex,
                                        onSelectedIndexChange = { index -> viewModel.selectExercise(exercises[index].id) },
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )
                                    val exercisesById = remember(exercises) { exercises.associateBy { it.id } }
                                    val recordsByExercise = remember(records) { records.groupBy { it.exerciseId } }

                                    AnimatedContent(
                                        targetState = exercises[selectedIndex].id,
                                        transitionSpec = { fadeIn(TimeGoMotion.contentEnter) togetherWith fadeOut(TimeGoMotion.contentExit) },
                                        label = "exercisePerformanceTransition",
                                    ) { exerciseId ->
                                        val selectedExercise = exercisesById[exerciseId] ?: return@AnimatedContent
                                        val exerciseRecords = recordsByExercise[exerciseId].orEmpty()
                                        SurfaceCard(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
                                            hero = true,
                                            riveted = true,
                                        ) {
                                            Column(modifier = Modifier.padding(Spacing.Medium)) {
                                                Text(
                                                    "${selectedExercise.name} Records",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(bottom = Spacing.ExtraSmall),
                                                )
                                                if (exerciseRecords.isEmpty()) {
                                                    Text(
                                                        "No personal records yet for this exercise.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                } else if (selectedExercise.loggingType == LoggingType.HOLD.name) {
                                                    val record = exerciseRecords.firstOrNull { it.type == PrType.LONGEST_HOLD }
                                                    Row(modifier = Modifier.fillMaxWidth()) {
                                                        StatTile(
                                                            label = "Longest Hold",
                                                            value = record?.let { "${it.value.toInt()}s" } ?: "--",
                                                            caption = record?.achievedOn?.format(PR_DATE_FORMATTER),
                                                            modifier = Modifier.weight(1f),
                                                        )
                                                    }
                                                } else {
                                                    val record = exerciseRecords.firstOrNull { it.type == PrType.BEST_SET }
                                                    val caption = record?.achievedOn?.format(PR_DATE_FORMATTER)
                                                    val isBodyweight = selectedExercise.category == ExerciseCategory.CALISTHENICS.name
                                                    Row(modifier = Modifier.fillMaxWidth()) {
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

                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = Spacing.Medium),
                                                    color = MaterialTheme.colorScheme.outlineVariant,
                                                )

                                                Text(
                                                    "Strength Progression Curve",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(bottom = Spacing.ExtraSmall),
                                                )
                                                if (strengthCurve.isEmpty()) {
                                                    Text(
                                                        "No logged sets yet to draw progression curve.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                } else {
                                                    SparklineChart(
                                                        strengthCurve,
                                                        modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 4.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        CurveMode.MUSCLE_GROUP -> {
                            Column {
                                val groupNames = MuscleGroup.entries.map { it.name }
                                val selectedIndex = groupNames.indexOf(selectedMuscleGroup).coerceAtLeast(0)
                                HorizontalWheelPicker(
                                    items = groupNames.map { formatEnumLabel(it) },
                                    selectedIndex = selectedIndex,
                                    onSelectedIndexChange = { index -> viewModel.selectMuscleGroup(groupNames[index]) },
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                                AnimatedContent(
                                    targetState = selectedMuscleGroup,
                                    transitionSpec = { fadeIn(TimeGoMotion.contentEnter) togetherWith fadeOut(TimeGoMotion.contentExit) },
                                    label = "muscleGroupPerformanceTransition",
                                ) { muscleGroup ->
                                    SurfaceCard(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
                                        hero = true,
                                        riveted = true,
                                    ) {
                                        Column(modifier = Modifier.padding(Spacing.Medium)) {
                                            Text(
                                                "${muscleGroup?.let { formatEnumLabel(it) } ?: "Muscle Group"} Strength Progression",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(bottom = Spacing.ExtraSmall),
                                            )
                                            if (strengthCurve.isEmpty()) {
                                                Text(
                                                    "No logged sets yet for this muscle group.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            } else {
                                                SparklineChart(
                                                    strengthCurve,
                                                    modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 4.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(76.dp))
            }
        } else {
            // ==================== BODY SEGMENT ====================
            item {
                SectionHeader("BMI & Composition", topPadding = Spacing.ExtraSmall)
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
                    hero = true,
                    riveted = true,
                ) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        if (currentBmi != null) {
                            val category = bmiCategory(currentBmi!!)
                            val bmiColor = when (category) {
                                BmiCategory.NORMAL -> NightMint
                                BmiCategory.OVERWEIGHT -> NightAmber
                                BmiCategory.UNDERWEIGHT, BmiCategory.OBESE -> MaterialTheme.colorScheme.error
                            }
                            Text(
                                "Current BMI",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "%.1f".format(currentBmi),
                                style = LedgerFigureValue.copy(fontSize = 32.sp),
                                color = bmiColor,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Text(
                                "Category: ${formatEnumLabel(category.name)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = bmiColor,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        } else {
                            Text(
                                "Log a weight and height below to calculate your BMI and track body composition trends.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (weightCurve.isNotEmpty()) {
                item {
                    SectionHeader("Weight Trend", topPadding = Spacing.Small)
                    SurfaceCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
                    ) {
                        Column(modifier = Modifier.padding(Spacing.Medium)) {
                            SparklineChart(
                                weightCurve,
                                modifier = Modifier.fillMaxWidth().height(140.dp).padding(vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader("Log Body Metrics", topPadding = Spacing.Small)
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
                ) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it },
                                label = { Text("Weight (kg)") },
                                isError = weightText.isNotBlank() && enteredWeight == null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                            )
                            OutlinedTextField(
                                value = waistText,
                                onValueChange = { waistText = it },
                                label = { Text("Waist (cm)") },
                                isError = waistText.isNotBlank() && enteredWaist == null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = heightText,
                                onValueChange = { heightText = it },
                                label = { Text("Height (cm)") },
                                isError = heightText.isNotBlank() && enteredHeight == null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                            )
                            Button(
                                enabled = bodyMetricInputValid,
                                onClick = {
                                    viewModel.logBodyMetric(enteredWeight, enteredWaist, enteredHeight)
                                    weightText = ""
                                    waistText = ""
                                    heightText = ""
                                },
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }

            if (bodyMetrics.isNotEmpty()) {
                item {
                    SectionHeader("Metric History", topPadding = Spacing.Small)
                }
                items(bodyMetrics, key = { it.id }) { metric ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = Spacing.Small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${metric.date}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val w = metric.weightKg?.let { "%.1f kg".format(it) } ?: "--"
                        val waist = metric.waistCm?.let { "%.1f cm".format(it) } ?: "--"
                        Text(
                            "$w  •  Waist: $waist",
                            style = LedgerFigureValue.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(76.dp))
            }
        }
    }

    // Floating thumb-friendly Segment Selector at bottom of Progress screen
    Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(32.dp),
                ),
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                ProgressSegment.entries.forEachIndexed { index, segment ->
                    SegmentedButton(
                        selected = selectedSegment == segment,
                        onClick = { viewModel.selectSegment(segment) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ProgressSegment.entries.size),
                    ) {
                        Text(segment.label)
                    }
                }
            }
        }
    }
}

private val PR_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d")
