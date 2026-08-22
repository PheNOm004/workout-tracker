package com.lsing.timego.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.TargetProvenance
import com.lsing.timego.data.targetProvenanceFor
import com.lsing.timego.domain.HoldSuggestion
import com.lsing.timego.domain.MET_CARDIO
import com.lsing.timego.domain.MET_WARMUP
import com.lsing.timego.domain.ProgressTimeframe
import com.lsing.timego.domain.averagePaceMinPerKm
import com.lsing.timego.domain.diagramGroupsForRecommendationCrop
import com.lsing.timego.domain.estimatedCalorieBurn
import com.lsing.timego.domain.formatCalisthenicsWeight
import com.lsing.timego.domain.formatDaysSince
import com.lsing.timego.domain.orderedMuscleDistributionForChart
import com.lsing.timego.domain.toggleExpandedExerciseIds
import com.lsing.timego.ui.common.AnimatedExpand
import com.lsing.timego.ui.common.CroppedMuscleDiagram
import com.lsing.timego.ui.common.ExerciseSections
import com.lsing.timego.ui.common.MuscleHeatLegend
import com.lsing.timego.ui.common.RadarChart
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.common.SurfaceCard
import com.lsing.timego.ui.common.TimerControls
import com.lsing.timego.ui.common.TrainingPulse
import com.lsing.timego.ui.common.WorkoutHistoryDialog
import com.lsing.timego.ui.common.categoryVisual
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.common.formatMuscleGroupList
import com.lsing.timego.ui.common.timeframeLabel
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.Spacing
import java.time.LocalDate

@Composable
fun LogScreen(viewModel: LogViewModel = viewModel()) {
    val sessionState by viewModel.sessionState.collectAsState()
    val landingSummary by viewModel.landingSummary.collectAsState()
    val routines by viewModel.routines.collectAsState()
    val landingBalanceTimeframe by viewModel.landingBalanceTimeframe.collectAsState()
    val landingMuscleBalance by viewModel.landingMuscleBalance.collectAsState()
    val routineLastCompleted by viewModel.routineLastCompleted.collectAsState()

    when (val state = sessionState) {
        is SessionUiState.Loading -> { /* nothing to render yet -- first frame only, resolves on the next recomposition */ }
        is SessionUiState.NoActiveSession -> LogLandingContent(
            summary = landingSummary,
            routines = routines,
            isSessionActive = false,
            onStartOrContinue = viewModel::startSession,
            balanceTimeframe = landingBalanceTimeframe,
            muscleBalance = landingMuscleBalance,
            routineLastCompleted = routineLastCompleted,
            onSelectBalanceTimeframe = viewModel::selectLandingBalanceTimeframe,
        )
        is SessionUiState.Active -> {
            // Keyed on sessionId, not just a bare remember{}: this resets to false whenever a
            // *new* active session starts, but persists correctly across recompositions of the
            // same session (e.g. suggestion updates after logging a set).
            var peekingLanding by remember(state.sessionId) { mutableStateOf(false) }
            if (peekingLanding) {
                LogLandingContent(
                    summary = landingSummary,
                    routines = routines,
                    isSessionActive = true,
                    onStartOrContinue = { peekingLanding = false },
                    balanceTimeframe = landingBalanceTimeframe,
                    muscleBalance = landingMuscleBalance,
                    routineLastCompleted = routineLastCompleted,
                    onSelectBalanceTimeframe = viewModel::selectLandingBalanceTimeframe,
                )
            } else {
                LoggingContent(
                    sessionId = state.sessionId,
                    viewModel = viewModel,
                    onEndSession = viewModel::endActiveSession,
                    onBackToLanding = { peekingLanding = true },
                )
            }
        }
    }
}

/** Shared between the real landing page (no active session) and the "peek back" view from an
 *  in-progress session -- same last-session summary and recommendation either way; only the
 *  bottom action row changes ([isSessionActive] swaps Freeform/Routine start buttons for a single
 *  "Continue Session" button, since [onStartOrContinue] resumes the existing session rather than
 *  creating a new one when a session is already active). */
@Composable
private fun LogLandingContent(
    summary: LandingSummary,
    routines: List<com.lsing.timego.data.Routine>,
    isSessionActive: Boolean,
    onStartOrContinue: (routineId: Long?) -> Unit,
    balanceTimeframe: ProgressTimeframe,
    muscleBalance: Map<String, Float>,
    routineLastCompleted: Map<Long, LocalDate>,
    onSelectBalanceTimeframe: (ProgressTimeframe) -> Unit,
) {
    var showLastSessionDetail by remember { mutableStateOf(false) }

    if (showLastSessionDetail && summary.lastSession != null) {
        WorkoutHistoryDialog(
            title = "Last session",
            entries = summary.lastSession.detail,
            onDismiss = { showLastSessionDetail = false },
            label = summary.lastSession.label,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.Large),
    ) {
        SectionHeader("Last session", topPadding = Spacing.ExtraSmall)
        if (summary.lastSession == null) {
            Text("No sessions logged yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            SurfaceCard(
                modifier = Modifier.fillMaxWidth().clickable { showLastSessionDetail = true },
                hero = true,
            ) {
                Column(modifier = Modifier.padding(Spacing.Medium)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            summary.lastSession.label,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "View last session details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LandingMetric("Sets", "${summary.lastSession.sets}", Modifier.weight(1f))
                        androidx.compose.material3.VerticalDivider(
                            modifier = Modifier.height(40.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        LandingMetric(
                            "Duration",
                            "${summary.lastSession.durationMinutes} min",
                            Modifier.weight(1f),
                        )
                    }
                    CroppedMuscleDiagram(
                        muscleGroups = summary.lastSession.muscleGroups,
                        intensities = summary.lastSession.muscleIntensities,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth().height(148.dp).padding(top = Spacing.Medium),
                    )
                    if (summary.lastSession.muscleGroups.isNotEmpty()) {
                        MuscleHeatLegend(
                            detailColor = MaterialTheme.colorScheme.surfaceVariant,
                            periodLabel = "this session",
                            modifier = Modifier.padding(top = Spacing.Small),
                        )
                    }
                }
            }
        }

        SectionHeader("Recommended")
        if (summary.recommendedMuscleGroups.isEmpty()) {
            Text("Everything's been trained recently -- nice balance.", style = MaterialTheme.typography.bodyMedium)
        } else {
            SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(Spacing.Medium)) {
                    Text(
                        formatMuscleGroupList(summary.recommendedMuscleGroups),
                        style = LedgerFigureValue,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "A fresh focus based on what has gone longest without training.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.ExtraSmall),
                    )
                    summary.suggestedExercise?.let { exercise ->
                        Text(
                            "Try: ${exercise.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.ExtraSmall),
                        )
                    }
                    CroppedMuscleDiagram(
                        // Size the artwork from the relevant body region only; the recommendation
                        // remains the only highlighted group set.
                        muscleGroups = diagramGroupsForRecommendationCrop(
                            summary.recommendedMuscleGroups.toSet(),
                        ),
                        accentColor = MaterialTheme.colorScheme.primary,
                        highlightGroups = summary.recommendedMuscleGroups.toSet(),
                        neutralizeUnhighlighted = true,
                        modifier = Modifier.fillMaxWidth().height(148.dp).padding(top = Spacing.Small),
                    )
                }
            }
        }

        SectionHeader("Muscle Balance (${timeframeLabel(balanceTimeframe)})")
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall)) {
            ProgressTimeframe.entries.forEach { option ->
                FilterChip(
                    selected = balanceTimeframe == option,
                    onClick = { onSelectBalanceTimeframe(option) },
                    label = { Text(formatEnumLabel(option.name)) },
                    modifier = Modifier.padding(end = Spacing.ExtraSmall),
                )
            }
        }
        if (muscleBalance.isNotEmpty()) {
            RadarChart(
                values = orderedMuscleDistributionForChart(muscleBalance)
                    .mapKeys { (group, _) -> formatEnumLabel(group) },
                modifier = Modifier.fillMaxWidth().height(220.dp).padding(vertical = Spacing.Small),
            )
        }
        if (routines.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = Spacing.Small)) {
                routines
                    .sortedWith(compareBy(nullsFirst()) { routine -> routineLastCompleted[routine.id] })
                    .forEach { routine ->
                        Text(
                            "${routine.name} — ${formatDaysSince(routineLastCompleted[routine.id], LocalDate.now())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
            }
        }

        if (isSessionActive) {
            SectionHeader("Session in progress")
            Button(onClick = { onStartOrContinue(null) }) {
                Text("Continue Session")
            }
        } else {
            SectionHeader("Start a session")
            if (routines.isEmpty()) {
                SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        Text("No routines yet", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Start freeform today. Build a reusable plan in Routines when your session repeats.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.ExtraSmall, bottom = Spacing.Small),
                        )
                        Button(onClick = { onStartOrContinue(null) }) { Text("Start freeform") }
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    Button(onClick = { onStartOrContinue(null) }, modifier = Modifier.padding(end = Spacing.Small)) {
                        Text("Freeform")
                    }
                    routines.forEach { routine ->
                        Button(onClick = { onStartOrContinue(routine.id) }, modifier = Modifier.padding(end = Spacing.Small)) {
                            Text(routine.name)
                        }
                    }
                }
            }
        }

        // Leave enough scrollable room for the final landing card to clear the persistent bottom
        // navigation bar when the user scrolls to the recommendation artwork.
        Spacer(modifier = Modifier.height(Spacing.ExtraLarge + 64.dp))
    }
}

@Composable
private fun LandingMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = Spacing.Small)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = LedgerFigureValue, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LoggingContent(
    sessionId: Long,
    viewModel: LogViewModel,
    onEndSession: () -> Unit,
    onBackToLanding: () -> Unit,
) {
    val exercises by viewModel.displayedExercises.collectAsState()
    val quickAddExercises by viewModel.quickAddExercises.collectAsState()
    val favoriteExerciseIds by viewModel.favoriteExerciseIds.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val holdSuggestions by viewModel.holdSuggestions.collectAsState()
    val lastWorkingSets by viewModel.lastWorkingSets.collectAsState()
    val routines by viewModel.routines.collectAsState()
    val selectedRoutineId by viewModel.selectedRoutineId.collectAsState()
    val latestBodyWeightKg by viewModel.latestBodyWeightKg.collectAsState()
    val holdDelaySeconds by viewModel.holdDelaySeconds.collectAsState()
    val setLoggedPulse by viewModel.setLoggedPulse.collectAsState()
    var expandedExerciseIds by remember(sessionId) { mutableStateOf<List<Long>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEndSessionConfirmation by remember { mutableStateOf(false) }
    var librarySearchQuery by remember { mutableStateOf("") }

    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onAdd = viewModel::addCustomExercise,
        )
    }

    if (showEndSessionConfirmation) {
        AlertDialog(
            onDismissRequest = { showEndSessionConfirmation = false },
            title = { Text("End workout?") },
            text = { Text("Your logged sets are saved. End this session when you are finished adding sets.") },
            dismissButton = {
                TextButton(onClick = { showEndSessionConfirmation = false }) { Text("Keep logging") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndSessionConfirmation = false
                        onEndSession()
                    },
                ) { Text("End session") }
            },
        )
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add custom exercise")
            }
        },
    ) { fabPadding ->
        LazyColumn(modifier = Modifier.padding(Spacing.Large).padding(fabPadding)) {
            item {
                Text(
                    "Log your next set",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = Spacing.ExtraSmall, bottom = Spacing.Small),
                )
                SectionHeader(
                    "Active workout",
                    topPadding = Spacing.ExtraSmall,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBackToLanding) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to landing")
                            }
                            TextButton(onClick = { showEndSessionConfirmation = true }) {
                                Text("End session", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                )
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small).horizontalScroll(rememberScrollState())) {
                    FilterChip(
                        selected = selectedRoutineId == null,
                        onClick = { viewModel.selectRoutine(null) },
                        label = { Text("Freeform") },
                        modifier = Modifier.padding(end = Spacing.Small),
                    )
                    routines.forEach { routine ->
                        FilterChip(
                            selected = selectedRoutineId == routine.id,
                            onClick = { viewModel.selectRoutine(routine.id) },
                            label = { Text(routine.name) },
                            modifier = Modifier.padding(end = Spacing.Small),
                        )
                    }
                }
            }
            item {
                val favoriteExercises = exercises.filter { it.id in favoriteExerciseIds }
                if (favoriteExercises.isNotEmpty()) {
                    SectionHeader("Favorites", topPadding = Spacing.Small)
                    FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small)) {
                        favoriteExercises.forEach { exercise ->
                            AssistChip(
                                onClick = {
                                    librarySearchQuery = exercise.name
                                    expandedExerciseIds = listOf(exercise.id)
                                },
                                label = { Text(exercise.name) },
                                modifier = Modifier.padding(end = Spacing.ExtraSmall, bottom = Spacing.ExtraSmall),
                            )
                        }
                    }
                }
                if (quickAddExercises.isNotEmpty()) {
                    SectionHeader("Quick add", topPadding = Spacing.Small)
                    FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small)) {
                        quickAddExercises.forEach { exercise ->
                            AssistChip(
                                onClick = {
                                    librarySearchQuery = exercise.name
                                    expandedExerciseIds = listOf(exercise.id)
                                },
                                label = { Text(exercise.name) },
                                modifier = Modifier.padding(end = Spacing.ExtraSmall, bottom = Spacing.ExtraSmall),
                            )
                        }
                    }
                }
                ExerciseSections(
                    exercises = exercises,
                    searchQuery = librarySearchQuery,
                    onSearchQueryChange = { librarySearchQuery = it },
                ) { exercise ->
                    when (exercise.loggingType) {
                        LoggingType.HOLD.name -> HoldLogRow(
                            exerciseName = exercise.name,
                            category = exercise.category,
                            suggestion = holdSuggestions[exercise.id],
                            delaySeconds = holdDelaySeconds,
                            isFavorite = exercise.id in favoriteExerciseIds,
                            onToggleFavorite = { viewModel.toggleFavoriteExercise(exercise.id) },
                            pulseId = setLoggedPulse?.takeIf { it.exerciseId == exercise.id }?.eventId ?: 0L,
                            expanded = exercise.id in expandedExerciseIds,
                            onToggle = {
                                expandedExerciseIds = toggleExpandedExerciseIds(expandedExerciseIds, exercise.id)
                            },
                            onLog = { duration, target, isWarmup, targetProvenance -> viewModel.logHoldSet(exercise.id, duration, target, isWarmup, targetProvenance) },
                        )
                        LoggingType.DURATION_DISTANCE.name -> CardioLogRow(
                            exerciseName = exercise.name,
                            category = exercise.category,
                            met = if (exercise.category == ExerciseCategory.CARDIO.name) MET_CARDIO else MET_WARMUP,
                            bodyWeightKg = latestBodyWeightKg,
                            delaySeconds = holdDelaySeconds,
                            isFavorite = exercise.id in favoriteExerciseIds,
                            onToggleFavorite = { viewModel.toggleFavoriteExercise(exercise.id) },
                            pulseId = setLoggedPulse?.takeIf { it.exerciseId == exercise.id }?.eventId ?: 0L,
                            expanded = exercise.id in expandedExerciseIds,
                            onToggle = {
                                expandedExerciseIds = toggleExpandedExerciseIds(expandedExerciseIds, exercise.id)
                            },
                            onLog = { duration, distance -> viewModel.logCardioSet(exercise.id, duration, distance) },
                        )
                        else -> StrengthLogRow(
                            exerciseName = exercise.name,
                            category = exercise.category,
                            suggestion = suggestions[exercise.id],
                            lastWorkingSet = lastWorkingSets[exercise.id],
                            isBodyweight = exercise.category == ExerciseCategory.CALISTHENICS.name,
                            latestBodyWeightKg = latestBodyWeightKg,
                            isFavorite = exercise.id in favoriteExerciseIds,
                            onToggleFavorite = { viewModel.toggleFavoriteExercise(exercise.id) },
                            expanded = exercise.id in expandedExerciseIds,
                            pulseId = setLoggedPulse?.takeIf { it.exerciseId == exercise.id }?.eventId ?: 0L,
                            onToggle = {
                                expandedExerciseIds = toggleExpandedExerciseIds(expandedExerciseIds, exercise.id)
                            },
                            onLog = { weight, reps, target, isWarmup, addedWeightKg, rpe, targetProvenance ->
                                viewModel.logSet(exercise.id, weight, reps, target, isWarmup, addedWeightKg, rpe, targetProvenance)
                            },
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

/** A one-line header (icon + exercise name, plus the suggested target already "penciled in" when
 *  one exists) that expands into the actual logging inputs when tapped. Defaults to collapsed --
 *  rendering full input rows for every exercise in a 119-strong library at once was both visually
 *  overwhelming and wasteful, per user feedback. [suggestionSummary] is shown collapsed, not
 *  gated behind expand: a ledger page has today's target already written on the line, not hidden
 *  until you tap it open. The leading icon is decorative (contentDescription = null): category is
 *  carried by icon shape alone (see categoryVisual), and naming it here would be redundant with
 *  what TalkBack already reads from the exercise name and expand/collapse icon. */
@Composable
private fun ExerciseRowHeader(
    exerciseName: String,
    icon: ImageVector,
    iconTint: Color,
    suggestionSummary: String?,
    expanded: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(Spacing.Medium),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.padding(end = Spacing.Small))
        Text(exerciseName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (suggestionSummary != null) {
            Text(
                suggestionSummary,
                style = LedgerFigureValue.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = Spacing.Small),
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
            contentDescription = if (expanded) "Collapse" else "Expand",
        )
    }
}

/** Replaces the elevated-card-with-permanent-accent-bar treatment: a plain surface with a
 *  hairline bottom rule (ledger row divider) and the brand accent (the red margin rule) shown
 *  only on the active row, not permanently on every row -- restraint is the point of the
 *  direction. Category no longer carries its own color (see categoryVisual); the rule's accent
 *  is always the theme's one committed brand color. */
@Composable
private fun ExerciseCard(expanded: Boolean, pulseId: Long = 0L, content: @Composable () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    TrainingPulse(
        active = expanded,
        pulseId = pulseId,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.Large, end = Spacing.Small)
            .then(if (expanded) Modifier.background(MaterialTheme.colorScheme.surfaceContainer) else Modifier),
    ) {
        Column {
            content()
            HorizontalDivider(
                color = if (expanded) accent else MaterialTheme.colorScheme.outlineVariant,
                thickness = if (expanded) 2.dp else 1.dp,
            )
        }
    }
}

@Composable
private fun StrengthLogRow(
    exerciseName: String,
    category: String,
    suggestion: com.lsing.timego.domain.OverloadSuggestion?,
    lastWorkingSet: SetLog?,
    isBodyweight: Boolean,
    latestBodyWeightKg: Double?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    expanded: Boolean,
    pulseId: Long,
    onToggle: () -> Unit,
    onLog: (weightKg: Double, reps: Int, targetReps: Int, isWarmup: Boolean, addedWeightKg: Double?, rpe: Int?, targetProvenance: TargetProvenance) -> Unit,
) {
    // Bodyweight exercises (Pull-Up, Push-Up, Dip, ...) ask for just the added weight k (e.g. a
    // weighted vest) -- blank/0 means bodyweight-only, not "no weight logged." weightKg (the
    // absolute bodyweight+k total 1RM/PR/suggester math needs) is computed at log time, not typed.
    var weightText by remember(exerciseName) { mutableStateOf("") }
    var repsText by remember(exerciseName) { mutableStateOf("") }
    var rpeText by remember(exerciseName) { mutableStateOf("") }
    var isWarmup by remember(exerciseName) { mutableStateOf(false) }
    val visual = categoryVisual(category)
    val suggestionHint = suggestion?.let {
        if (isBodyweight) {
            val addedK = latestBodyWeightKg?.let { bw -> it.weightKg - bw } ?: it.weightKg
            "${formatCalisthenicsWeight(addedK)} x ${it.reps}"
        } else {
            "${it.weightKg}kg x ${it.reps}"
        }
    }
    val lastSetHint = lastWorkingSet?.let { set ->
        val weight = if (isBodyweight && set.addedWeightKg != null) {
            formatCalisthenicsWeight(set.addedWeightKg)
        } else {
            "${set.weightKg}kg"
        }
        "Last time: $weight x ${set.reps}"
    }

    ExerciseCard(expanded, pulseId) {
        ExerciseRowHeader(
            exerciseName,
            visual.icon,
            visual.accent,
            suggestionHint,
            expanded,
            isFavorite,
            onToggleFavorite,
        ) { onToggle() }
        AnimatedExpand(expanded) {
            if (suggestion != null) {
                Text(
                    suggestion.note,
                    style = LedgerFigureValue.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Spacing.Medium),
                )
            }
            lastSetHint?.let { hint ->
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Spacing.Medium),
            ) {
                Checkbox(checked = isWarmup, onCheckedChange = { isWarmup = it })
                Text("Warmup set", style = MaterialTheme.typography.bodySmall)
            }
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.Medium)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text(if (isBodyweight) "added kg" else "kg") },
                        placeholder = if (isBodyweight) { { Text("0") } } else null,
                        textStyle = LedgerFigureValue.copy(fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).padding(end = Spacing.Small),
                    )
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text("reps") },
                        textStyle = LedgerFigureValue.copy(fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = rpeText,
                        onValueChange = { rpeText = it },
                        label = { Text("RPE") },
                        placeholder = { Text("1-10") },
                        singleLine = true,
                        textStyle = LedgerFigureValue.copy(fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(end = Spacing.Small),
                    )
                    Button(onClick = {
                        val reps = repsText.toIntOrNull()
                        val rpe = rpeText.toIntOrNull()?.coerceIn(1, 10)
                        if (isBodyweight) {
                            val addedWeight = weightText.toDoubleOrNull() ?: 0.0
                            if (reps != null) {
                                val totalWeight = (latestBodyWeightKg ?: 0.0) + addedWeight
                                onLog(totalWeight, reps, suggestion?.reps ?: reps, isWarmup, addedWeight, rpe, targetProvenanceFor(suggestion != null))
                                weightText = ""
                                repsText = ""
                                rpeText = ""
                                isWarmup = false
                            }
                        } else {
                            val weight = weightText.toDoubleOrNull()
                            if (weight != null && reps != null) {
                                onLog(weight, reps, suggestion?.reps ?: reps, isWarmup, null, rpe, targetProvenanceFor(suggestion != null))
                                weightText = ""
                                repsText = ""
                                rpeText = ""
                                isWarmup = false
                            }
                        }
                    }) {
                        Text("Log set")
                    }
                }
            }
        }
    }
}

@Composable
private fun CardioLogRow(
    exerciseName: String,
    category: String,
    met: Double,
    bodyWeightKg: Double?,
    delaySeconds: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    expanded: Boolean,
    pulseId: Long,
    onToggle: () -> Unit,
    onLog: (durationMinutes: Double, distanceKm: Double?) -> Unit,
) {
    var useTimer by remember(exerciseName) { mutableStateOf(false) }
    var durationText by remember(exerciseName) { mutableStateOf("") }
    var distanceText by remember(exerciseName) { mutableStateOf("") }
    val duration = durationText.toDoubleOrNull()
    val distance = distanceText.toDoubleOrNull()
    val visual = categoryVisual(category)

    ExerciseCard(expanded, pulseId) {
        ExerciseRowHeader(exerciseName, visual.icon, visual.accent, null, expanded, isFavorite, onToggleFavorite, onToggle)
        AnimatedExpand(expanded) {
            if (duration != null && duration > 0) {
                val pace = distance?.let { averagePaceMinPerKm(duration, it) }
                val calories = bodyWeightKg?.let { estimatedCalorieBurn(met, it, duration) }
                val details = listOfNotNull(
                    pace?.let { "Pace: ${"%.1f".format(it)} min/km" },
                    calories?.let { "~${it.toInt()} kcal" },
                ).joinToString(" -- ")
                if (details.isNotEmpty()) {
                    Text(
                        details,
                        style = LedgerFigureValue.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = Spacing.Medium),
                    )
                }
            }
            OutlinedTextField(
                value = distanceText,
                onValueChange = { distanceText = it },
                label = { Text("km (optional)") },
                textStyle = LedgerFigureValue.copy(fontSize = 16.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.padding(horizontal = Spacing.Medium),
            )
            if (!useTimer) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("minutes") },
                        textStyle = LedgerFigureValue.copy(fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).padding(end = Spacing.Small),
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
                OutlinedButton(
                    onClick = { useTimer = true },
                    modifier = Modifier.padding(horizontal = Spacing.Medium),
                ) { Text("Use timer") }
            } else {
                TimerControls(
                    delaySeconds = delaySeconds,
                    formatElapsed = ::formatElapsedSeconds,
                    onEnterManually = { useTimer = false },
                    onStop = { elapsedSeconds ->
                        onLog(elapsedSeconds / 60.0, distance)
                        useTimer = false
                        distanceText = ""
                    },
                )
            }
        }
    }
}

private fun formatElapsedSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun HoldLogRow(
    exerciseName: String,
    category: String,
    suggestion: HoldSuggestion?,
    delaySeconds: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    expanded: Boolean,
    pulseId: Long,
    onToggle: () -> Unit,
    onLog: (durationSeconds: Int, targetDurationSeconds: Int, isWarmup: Boolean, targetProvenance: TargetProvenance) -> Unit,
) {
    var isWarmup by remember(exerciseName) { mutableStateOf(false) }
    var useTimer by remember(exerciseName) { mutableStateOf(true) }
    var manualDurationText by remember(exerciseName) { mutableStateOf("") }
    val visual = categoryVisual(category)

    ExerciseCard(expanded, pulseId) {
        ExerciseRowHeader(
            exerciseName,
            visual.icon,
            visual.accent,
            suggestion?.let { "${it.targetDurationSeconds}s" },
            expanded,
            isFavorite,
            onToggleFavorite,
        ) { onToggle() }
        AnimatedExpand(expanded) {
            if (suggestion != null) {
                Text(
                    suggestion.note,
                    style = LedgerFigureValue.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Spacing.Medium),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Spacing.Medium),
            ) {
                Checkbox(checked = isWarmup, onCheckedChange = { isWarmup = it })
                Text("Warmup set", style = MaterialTheme.typography.bodySmall)
            }
            if (!useTimer) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val manualDuration = manualDurationText.toIntOrNull()
                    OutlinedTextField(
                        value = manualDurationText,
                        onValueChange = { manualDurationText = it },
                        label = { Text("seconds") },
                        textStyle = LedgerFigureValue.copy(fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(end = Spacing.Small),
                    )
                    Button(onClick = {
                        if (manualDuration != null && manualDuration > 0) {
                            onLog(manualDuration, suggestion?.targetDurationSeconds ?: manualDuration, isWarmup, targetProvenanceFor(suggestion != null))
                            manualDurationText = ""
                            isWarmup = false
                        }
                    }) { Text("Log") }
                }
                TextButton(
                    onClick = { useTimer = true },
                    modifier = Modifier.padding(horizontal = Spacing.Medium),
                ) { Text("Use timer instead") }
            } else {
                TimerControls(
                    delaySeconds = delaySeconds,
                    formatElapsed = { "${it}s" },
                    onEnterManually = { useTimer = false },
                    onStop = { seconds ->
                        onLog(seconds, suggestion?.targetDurationSeconds ?: seconds, isWarmup, targetProvenanceFor(suggestion != null))
                        isWarmup = false
                    },
                )
            }
        }
    }
}
