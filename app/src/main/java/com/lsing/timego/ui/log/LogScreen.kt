package com.lsing.timego.ui.log

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import com.lsing.timego.ui.theme.TimeGoMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.lsing.timego.domain.routinesForToday
import com.lsing.timego.domain.toggleExpandedExerciseIds
import com.lsing.timego.ui.common.AnimatedExpand
import com.lsing.timego.ui.common.CroppedMuscleDiagram
import com.lsing.timego.ui.common.ExerciseSections
import com.lsing.timego.ui.common.MuscleHeatLegend
import com.lsing.timego.ui.common.RadarChart
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.common.SurfaceCard
import com.lsing.timego.ui.common.TimerControls
import com.lsing.timego.ui.common.toFiniteDoubleOrNull
import com.lsing.timego.ui.common.toPositiveFiniteDoubleOrNull
import com.lsing.timego.ui.common.toPositiveIntOrNull
import com.lsing.timego.ui.common.TrainingPulse
import com.lsing.timego.ui.common.WorkoutHistoryDialog
import com.lsing.timego.ui.common.categoryVisual
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.common.formatMuscleGroupList
import com.lsing.timego.ui.common.timeframeLabel
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.Spacing
import java.time.LocalDate

private const val ACTIVE_LIBRARY_ITEM_INDEX = 4

@Composable
fun LogScreen(viewModel: LogViewModel = viewModel()) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val landingSummary by viewModel.landingSummary.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val landingBalanceTimeframe by viewModel.landingBalanceTimeframe.collectAsStateWithLifecycle()
    val landingMuscleBalance by viewModel.landingMuscleBalance.collectAsStateWithLifecycle()
    val routineLastCompleted by viewModel.routineLastCompleted.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = sessionState,
        transitionSpec = {
            val enter = slideInHorizontally(TimeGoMotion.navigationInOffset) { width -> width / 4 } + fadeIn(TimeGoMotion.contentEnter)
            val exit = slideOutHorizontally(TimeGoMotion.navigationOutOffset) { width -> -width / 4 } + fadeOut(TimeGoMotion.contentExit)
            enter togetherWith exit
        },
        label = "logSessionTransition",
    ) { state ->
        when (state) {
            is SessionUiState.Loading -> { /* nothing to render yet */ }
            is SessionUiState.NoActiveSession -> LogLandingContent(
                summary = landingSummary,
                routines = routines,
                isSessionActive = false,
                onStartOrContinue = viewModel::startSession,
                routineLastCompleted = routineLastCompleted,
                balanceTimeframe = landingBalanceTimeframe,
                muscleBalance = landingMuscleBalance,
                onSelectBalanceTimeframe = viewModel::selectLandingBalanceTimeframe,
            )
            is SessionUiState.Active -> {
                var peekingLanding by remember(state.sessionId) { mutableStateOf(false) }
                AnimatedContent(
                    targetState = peekingLanding,
                    transitionSpec = {
                        val enter = slideInHorizontally(TimeGoMotion.navigationInOffset) { width -> if (targetState) -width / 4 else width / 4 } + fadeIn(TimeGoMotion.contentEnter)
                        val exit = slideOutHorizontally(TimeGoMotion.navigationOutOffset) { width -> if (targetState) width / 4 else -width / 4 } + fadeOut(TimeGoMotion.contentExit)
                        enter togetherWith exit
                    },
                    label = "peekingLandingTransition",
                ) { peeking ->
                    if (peeking) {
                        LogLandingContent(
                            summary = landingSummary,
                            routines = routines,
                            isSessionActive = true,
                            onStartOrContinue = { peekingLanding = false },
                            routineLastCompleted = routineLastCompleted,
                            balanceTimeframe = landingBalanceTimeframe,
                            muscleBalance = landingMuscleBalance,
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
    }
}

/** Action-centric Command Center landing page: Hero next workout, last session recap, and routines carousel. */
@Composable
private fun LogLandingContent(
    summary: LandingSummary,
    routines: List<com.lsing.timego.data.Routine>,
    isSessionActive: Boolean,
    onStartOrContinue: (routineId: Long?) -> Unit,
    routineLastCompleted: Map<Long, LocalDate>,
    balanceTimeframe: ProgressTimeframe,
    muscleBalance: Map<String, Float>,
    onSelectBalanceTimeframe: (ProgressTimeframe) -> Unit,
) {
    var showLastSessionDetail by remember { mutableStateOf(false) }

    if (showLastSessionDetail && summary.lastSession != null) {
        WorkoutHistoryDialog(
            title = "Last session",
            entries = summary.lastSession.detail,
            onDismiss = { showLastSessionDetail = false },
            label = summary.lastSession.label,
            durationMinutes = summary.lastSession.durationMinutes.toDouble(),
        )
    }

    val todaysScheduledRoutine = remember(routines) {
        routinesForToday(routines, LocalDate.now().dayOfWeek).firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.Large),
    ) {
        if (isSessionActive) {
            SurfaceCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium),
                hero = true,
                riveted = true,
            ) {
                Column(modifier = Modifier.padding(Spacing.Medium)) {
                    Text(
                        "Session In Progress",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "You have an active workout open.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.ExtraSmall, bottom = Spacing.Small),
                    )
                    Button(
                        onClick = { onStartOrContinue(null) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Continue Workout")
                    }
                }
            }
        } else {
            // HERO: Today's Workout Command Center
            SectionHeader("Today's Workout", topPadding = Spacing.ExtraSmall)
            if (todaysScheduledRoutine != null) {
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium),
                    hero = true,
                    riveted = true,
                ) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        Text(
                            "Scheduled for Today",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            todaysScheduledRoutine.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 2.dp, bottom = Spacing.ExtraSmall),
                        )
                        val lastTrainedStr = formatDaysSince(routineLastCompleted[todaysScheduledRoutine.id], LocalDate.now())
                        Text(
                            "Last completed: $lastTrainedStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Spacing.Medium),
                        )
                        Button(
                            onClick = { onStartOrContinue(todaysScheduledRoutine.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Start ${todaysScheduledRoutine.name}")
                        }
                    }
                }
            } else {
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium),
                    hero = true,
                    riveted = true,
                ) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        Text(
                            "Recommended Focus",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (summary.recommendedMuscleGroups.isNotEmpty()) {
                            Text(
                                formatMuscleGroupList(summary.recommendedMuscleGroups),
                                style = LedgerFigureValue,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 2.dp),
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
                                muscleGroups = diagramGroupsForRecommendationCrop(
                                    summary.recommendedMuscleGroups.toSet(),
                                ),
                                accentColor = MaterialTheme.colorScheme.primary,
                                highlightGroups = summary.recommendedMuscleGroups.toSet(),
                                neutralizeUnhighlighted = true,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 130.dp).padding(vertical = Spacing.Small),
                            )
                        } else {
                            Text(
                                "All muscles recently trained.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = Spacing.ExtraSmall),
                            )
                        }
                        Button(
                            onClick = { onStartOrContinue(null) },
                            modifier = Modifier.fillMaxWidth().padding(top = Spacing.Small),
                        ) {
                            Text("Start Freeform Workout")
                        }
                    }
                }
            }

            // Quick Routines Carousel
            if (routines.isNotEmpty()) {
                SectionHeader("Your Routines", topPadding = Spacing.Small)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.Medium)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    Button(onClick = { onStartOrContinue(null) }) {
                        Text("Freeform")
                    }
                    routines.forEach { routine ->
                        OutlinedButton(onClick = { onStartOrContinue(routine.id) }) {
                            Text(routine.name)
                        }
                    }
                }
            }
        }

        // Secondary: Last Session Recap
        SectionHeader("Last Session Recap", topPadding = Spacing.Small)
        if (summary.lastSession == null) {
            Text("No sessions logged yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            SurfaceCard(
                modifier = Modifier.fillMaxWidth().clickable { showLastSessionDetail = true },
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
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.Small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LandingMetric("Sets", "${summary.lastSession.sets}", Modifier.weight(1f))
                        androidx.compose.material3.VerticalDivider(
                            modifier = Modifier.height(36.dp),
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
                        modifier = Modifier.fillMaxWidth().heightIn(max = 130.dp).padding(top = Spacing.Small),
                    )
                    if (summary.lastSession.muscleGroups.isNotEmpty()) {
                        MuscleHeatLegend(
                            detailColor = MaterialTheme.colorScheme.surfaceVariant,
                            periodLabel = "this session",
                            modifier = Modifier.padding(top = Spacing.ExtraSmall),
                        )
                    }
                }
            }
        }

        // Muscle Balance Radar Section on Landing
        SectionHeader("Muscle Balance (${timeframeLabel(balanceTimeframe)})", topPadding = Spacing.Small)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.ExtraSmall)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            ProgressTimeframe.entries.forEach { option ->
                FilterChip(
                    selected = balanceTimeframe == option,
                    onClick = { onSelectBalanceTimeframe(option) },
                    label = { Text(formatEnumLabel(option.name)) },
                )
            }
        }
        SurfaceCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.Small)) {
                RadarChart(
                    values = orderedMuscleDistributionForChart(muscleBalance)
                        .mapKeys { (group, _) -> formatEnumLabel(group) },
                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(vertical = Spacing.Small),
                )
                if (muscleBalance.isEmpty()) {
                    Text(
                        "No strength sets logged in the ${timeframeLabel(balanceTimeframe)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.ExtraSmall),
                    )
                }
            }
        }

        // Bottom spacer
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
    val exercises by viewModel.displayedExercises.collectAsStateWithLifecycle()
    val quickAddExercises by viewModel.quickAddExercises.collectAsStateWithLifecycle()
    val favoriteExerciseIds by viewModel.favoriteExerciseIds.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val holdSuggestions by viewModel.holdSuggestions.collectAsStateWithLifecycle()
    val lastWorkingSets by viewModel.lastWorkingSets.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val selectedRoutineId by viewModel.selectedRoutineId.collectAsStateWithLifecycle()
    val latestBodyWeightKg by viewModel.latestBodyWeightKg.collectAsStateWithLifecycle()
    val holdDelaySeconds by viewModel.holdDelaySeconds.collectAsStateWithLifecycle()
    val setLoggedPulse by viewModel.setLoggedPulse.collectAsStateWithLifecycle()
    val activeSessionSetsByExercise by viewModel.activeSessionSetsByExercise.collectAsStateWithLifecycle()
    var expandedExerciseIds by remember(sessionId) { mutableStateOf<List<Long>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEndSessionConfirmation by remember { mutableStateOf(false) }
    var librarySearchQuery by remember { mutableStateOf("") }
    val activeListState = rememberLazyListState()
    val activeScope = rememberCoroutineScope()

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
        LazyColumn(
            state = activeListState,
            modifier = Modifier.padding(Spacing.Large).padding(fabPadding),
        ) {
            item(key = "activeHeader") {
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
            item(key = "activeSummary") {
                // Live in-session summary of completed sets
                val exercisesById = remember(exercises) { exercises.associateBy { it.id } }
                ActiveWorkoutSection(
                    activeSetsByExercise = activeSessionSetsByExercise,
                    exercisesById = exercisesById,
                    selectedExerciseId = expandedExerciseIds.singleOrNull(),
                    onSelectExercise = { exerciseId ->
                        exercisesById[exerciseId]?.let { exercise ->
                            librarySearchQuery = exercise.name
                            expandedExerciseIds = listOf(exerciseId)
                            activeScope.launch {
                                activeListState.animateScrollToItem(ACTIVE_LIBRARY_ITEM_INDEX)
                            }
                        }
                    },
                )
            }
            item(key = "favorites") {
                val favoriteExercises = exercises.filter { it.id in favoriteExerciseIds }
                if (favoriteExercises.isNotEmpty()) {
                    SectionHeader("Favorites", topPadding = Spacing.Small)
                    FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small)) {
                        favoriteExercises.forEach { exercise ->
                            AnimatedAssistChip(
                                onClick = {
                                    librarySearchQuery = exercise.name
                                    expandedExerciseIds = listOf(exercise.id)
                                    activeScope.launch {
                                        activeListState.animateScrollToItem(ACTIVE_LIBRARY_ITEM_INDEX)
                                    }
                                },
                                label = { Text(exercise.name) },
                                modifier = Modifier.padding(end = Spacing.ExtraSmall, bottom = Spacing.ExtraSmall),
                            )
                        }
                    }
                }
            }
            item(key = "quickAdd") {
                if (quickAddExercises.isNotEmpty()) {
                    SectionHeader("Quick add", topPadding = Spacing.Small)
                    FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small)) {
                        quickAddExercises.forEach { exercise ->
                            AnimatedAssistChip(
                                onClick = {
                                    librarySearchQuery = exercise.name
                                    expandedExerciseIds = listOf(exercise.id)
                                    activeScope.launch {
                                        activeListState.animateScrollToItem(ACTIVE_LIBRARY_ITEM_INDEX)
                                    }
                                },
                                label = { Text(exercise.name) },
                                modifier = Modifier.padding(end = Spacing.ExtraSmall, bottom = Spacing.ExtraSmall),
                            )
                        }
                    }
                }
            }
            item(key = "exerciseLibrary") {
                SectionHeader("Exercise Library", topPadding = Spacing.Small)
                ExerciseSections(
                    exercises = exercises,
                    searchQuery = librarySearchQuery,
                    onSearchQueryChange = { librarySearchQuery = it },
                    favoriteExerciseIds = favoriteExerciseIds,
                ) { exercise ->
                    val currentExerciseSets = activeSessionSetsByExercise[exercise.id].orEmpty()
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
                            currentSessionSets = currentExerciseSets,
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
            item(key = "activeBottomSpacer") {
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
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "chevronRotation",
    )
    val starScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "starScale",
    )

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
                modifier = Modifier.graphicsLayer {
                    scaleX = starScale
                    scaleY = starScale
                },
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (expanded) "Collapse" else "Expand",
            modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
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
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(expanded) {
        if (expanded) {
            kotlinx.coroutines.delay(120)
            bringIntoViewRequester.bringIntoView()
            kotlinx.coroutines.delay(200)
            bringIntoViewRequester.bringIntoView()
        }
    }
    val accent = MaterialTheme.colorScheme.primary
    TrainingPulse(
        active = expanded,
        pulseId = pulseId,
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
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
private fun AnimatedAssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    AssistChip(
        onClick = {
            scope.launch {
                scale.animateTo(0.88f, spring(stiffness = Spring.StiffnessHigh))
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            }
            onClick()
        },
        label = label,
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
    )
}

@Composable
private fun StrengthLogRow(
    exerciseName: String,
    category: String,
    suggestion: com.lsing.timego.domain.OverloadSuggestion?,
    lastWorkingSet: SetLog?,
    currentSessionSets: List<SetLog> = emptyList(),
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
    val reps = repsText.toPositiveIntOrNull()
    val rpe = rpeText.toIntOrNull()?.takeIf { it in 1..10 }
    val validRpe = rpeText.isBlank() || rpe != null
    val enteredWeight = if (isBodyweight) {
        if (weightText.isBlank()) 0.0 else weightText.toFiniteDoubleOrNull()
    } else {
        weightText.toPositiveFiniteDoubleOrNull()
    }
    val bodyWeight = latestBodyWeightKg?.takeIf { it.isFinite() && it > 0.0 }
    val totalBodyweightLoad = if (isBodyweight && bodyWeight != null && enteredWeight != null) {
        (bodyWeight + enteredWeight).takeIf { it.isFinite() && it > 0.0 }
    } else {
        null
    }
    val canLog = reps != null && validRpe && if (isBodyweight) totalBodyweightLoad != null else enteredWeight != null
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
            if (currentSessionSets.isNotEmpty()) {
                val setsSummary = currentSessionSets.mapIndexed { idx, s ->
                    val w = if (isBodyweight && s.addedWeightKg != null) formatCalisthenicsWeight(s.addedWeightKg) else "${s.weightKg}kg"
                    "#${idx + 1}: $w x ${s.reps}"
                }.joinToString("  •  ")
                Text(
                    "Today: $setsSummary",
                    style = LedgerFigureValue.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall),
                )
            }
            if (isBodyweight && bodyWeight == null) {
                Text(
                    "Log a valid body weight on Progress before logging calisthenics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
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
                        isError = weightText.isNotBlank() && (enteredWeight == null || (isBodyweight && totalBodyweightLoad == null)),
                        textStyle = LedgerFigureValue.copy(fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).padding(end = Spacing.Small),
                    )
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text("reps") },
                        isError = repsText.isNotBlank() && reps == null,
                        textStyle = LedgerFigureValue.copy(fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                // Quick Stepper Chips for Fast In-Gym Adjustment
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.ExtraSmall),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                ) {
                    AnimatedAssistChip(
                        onClick = {
                            val cur = enteredWeight ?: 0.0
                            val next = (cur - 2.5).coerceAtLeast(0.0)
                            weightText = if (next == 0.0 && isBodyweight) "" else "%.1f".format(next).trimEnd('0').trimEnd('.')
                        },
                        label = { Text("-2.5kg", style = MaterialTheme.typography.labelSmall) },
                    )
                    AnimatedAssistChip(
                        onClick = {
                            val cur = enteredWeight ?: 0.0
                            val next = cur + 2.5
                            weightText = "%.1f".format(next).trimEnd('0').trimEnd('.')
                        },
                        label = { Text("+2.5kg", style = MaterialTheme.typography.labelSmall) },
                    )
                    AnimatedAssistChip(
                        onClick = {
                            val curReps = reps ?: 0
                            val next = (curReps - 1).coerceAtLeast(1)
                            repsText = next.toString()
                        },
                        label = { Text("-1 rep", style = MaterialTheme.typography.labelSmall) },
                    )
                    AnimatedAssistChip(
                        onClick = {
                            val curReps = reps ?: (suggestion?.reps ?: 0)
                            val next = curReps + 1
                            repsText = next.toString()
                        },
                        label = { Text("+1 rep", style = MaterialTheme.typography.labelSmall) },
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
                        isError = !validRpe,
                        singleLine = true,
                        textStyle = LedgerFigureValue.copy(fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(end = Spacing.Small),
                    )
                    Button(
                        enabled = canLog,
                        onClick = {
                            if (isBodyweight) {
                                if (reps != null && enteredWeight != null && totalBodyweightLoad != null) {
                                    onLog(totalBodyweightLoad, reps, suggestion?.reps ?: reps, isWarmup, enteredWeight, rpe, targetProvenanceFor(suggestion != null))
                                    weightText = ""
                                    repsText = ""
                                    rpeText = ""
                                    isWarmup = false
                                }
                            } else if (enteredWeight != null && reps != null) {
                                onLog(enteredWeight, reps, suggestion?.reps ?: reps, isWarmup, null, rpe, targetProvenanceFor(suggestion != null))
                                weightText = ""
                                repsText = ""
                                rpeText = ""
                                isWarmup = false
                            }
                        }
                    ) {
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
    val duration = durationText.toPositiveFiniteDoubleOrNull()
    val distance = distanceText.toPositiveFiniteDoubleOrNull()
    val validDistance = distanceText.isBlank() || distance != null
    val visual = categoryVisual(category)

    ExerciseCard(expanded, pulseId) {
        ExerciseRowHeader(exerciseName, visual.icon, visual.accent, null, expanded, isFavorite, onToggleFavorite, onToggle)
        AnimatedExpand(expanded) {
            if (duration != null) {
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
                isError = !validDistance,
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
                        isError = durationText.isNotBlank() && duration == null,
                        textStyle = LedgerFigureValue.copy(fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).padding(end = Spacing.Small),
                    )
                    Button(
                        enabled = duration != null && validDistance,
                        onClick = {
                            if (duration != null && validDistance) {
                                onLog(duration, distance)
                                durationText = ""
                                distanceText = ""
                            }
                        }
                    ) {
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
                    val manualDuration = manualDurationText.toPositiveIntOrNull()
                    OutlinedTextField(
                        value = manualDurationText,
                        onValueChange = { manualDurationText = it },
                        label = { Text("seconds") },
                        isError = manualDurationText.isNotBlank() && manualDuration == null,
                        textStyle = LedgerFigureValue.copy(fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).padding(end = Spacing.Small),
                    )
                    Button(
                        enabled = manualDuration != null,
                        onClick = {
                            if (manualDuration != null) {
                                onLog(manualDuration, suggestion?.targetDurationSeconds ?: manualDuration, isWarmup, targetProvenanceFor(suggestion != null))
                                manualDurationText = ""
                                isWarmup = false
                            }
                        }
                    ) { Text("Log") }
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
