package com.lsing.timego.ui.log

import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import com.lsing.timego.ui.common.rememberDismissKeyboard
import com.lsing.timego.ui.common.rememberAdvanceKeyboardActions
import androidx.compose.ui.text.input.ImeAction
import com.lsing.timego.ui.common.LocalRestClearance
import com.lsing.timego.ui.common.regionLabelsFor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import com.lsing.timego.ui.common.RegionChipRow
import com.lsing.timego.ui.common.exerciseDisplayRegion
import com.lsing.timego.ui.common.LocalDockHeight
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.lsing.timego.ui.theme.Hairline
import com.lsing.timego.ui.common.TimeGoDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.lsing.timego.ui.common.TimeGoBottomSheet
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalWindowInfo
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
import com.lsing.timego.domain.diagramGroupsForBodyRegionCrop
import com.lsing.timego.domain.expandMuscleGroupRegions
import com.lsing.timego.domain.estimatedCalorieBurn
import com.lsing.timego.domain.formatCalisthenicsWeight
import com.lsing.timego.domain.formatDaysSince
import com.lsing.timego.domain.flexibleRoutines
import com.lsing.timego.domain.nextFlexibleRoutineByRecovery
import com.lsing.timego.domain.primaryMuscleGroups
import com.lsing.timego.domain.orderedMuscleDistributionForChart
import com.lsing.timego.domain.routinesForToday
import com.lsing.timego.domain.toggleExpandedExerciseIds
import com.lsing.timego.ui.common.AnimatedExpand
import com.lsing.timego.ui.common.CroppedMuscleDiagram
import com.lsing.timego.ui.common.ExerciseSections
import com.lsing.timego.ui.common.ExerciseListOrder
import com.lsing.timego.ui.common.RadarChart
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.common.SurfaceCard
import com.lsing.timego.ui.common.TimerControls
import com.lsing.timego.ui.common.toFiniteDoubleOrNull
import com.lsing.timego.ui.common.toPositiveFiniteDoubleOrNull
import com.lsing.timego.ui.common.toPositiveIntOrNull
import com.lsing.timego.ui.common.TrainingPulse
import com.lsing.timego.ui.common.tactilePress
import com.lsing.timego.ui.common.WorkoutHistoryDialog
import com.lsing.timego.ui.common.ShimmerLine
import com.lsing.timego.ui.common.categoryVisual
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.common.formatMuscleGroupList
import com.lsing.timego.ui.common.timeframeLabel
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.PlateBlue
import com.lsing.timego.ui.theme.PlateGreen
import com.lsing.timego.ui.theme.Spacing
import com.lsing.timego.ui.exercise.ExerciseDetailSheet
import com.lsing.timego.ui.exercise.buildExerciseDetail
import java.time.LocalDate

private enum class ActiveLogPage { SESSION, EXERCISE_PICKER }

/** Per-exercise entry conventions, provided once at the top of LogScreen so every logging row (session list
 *  and picker) reads and toggles them without threading parameters through each layer. */
private class SideModes(
    val combinedEntryIds: Set<Long>,
    val bothSidesIds: Set<Long>,
    val toggleCombinedEntry: (Long) -> Unit,
    val toggleBothSides: (Long) -> Unit,
    val weightSteps: Map<Long, Double> = emptyMap(),
    val setWeightStep: (Long, Double) -> Unit = { _, _ -> },
)

private val LocalSideModes = androidx.compose.runtime.compositionLocalOf {
    SideModes(emptySet(), emptySet(), {}, {})
}

/** Opens the exercise detail sheet by exercise name; provided once at the top of LogScreen so every row header
 *  (session list and picker) gets the info button without threading a callback through each row. */
private val LocalRpeEntryMode = androidx.compose.runtime.compositionLocalOf { com.lsing.timego.data.RpeEntryMode.AFTER_SET }
private val LocalShowExerciseDetails = androidx.compose.runtime.compositionLocalOf<(String) -> Unit> { {} }

/** Items LoggingContent's list renders before the first exercise card: activeHeader, activeSummary,
 *  favorites, quickAdd, logExerciseAction, exercisesHeader. Keep in sync when adding items above. */
private const val FIRST_EXERCISE_ITEM_INDEX = 6

/** "How ready are you?" shown once at the top of a new session when the readiness setting is on. */
@Composable
private fun ReadinessPrompt(onRate: (Int) -> Unit, onSkip: () -> Unit) {
    SurfaceCard(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small)) {
        Column(modifier = Modifier.padding(Spacing.Medium), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text("How ready do you feel today?", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), modifier = Modifier.fillMaxWidth()) {
                (1..5).forEach { value ->
                    OutlinedButton(
                        onClick = { onRate(value) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.weight(1f).height(48.dp).semantics { contentDescription = "Readiness $value of 5" },
                    ) { Text("$value") }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "1 drained, 5 great. On a 1 or 2 day TimeGo holds weights and adds 15 s of rest.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSkip) { Text("Skip") }
            }
        }
    }
}

/** What switching to a program would change: weekly sets per muscle against the last 30 days of training. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ProgramDiffSheet(programName: String, diff: com.lsing.timego.domain.ProgramDiff, onDismiss: () -> Unit) {
    TimeGoBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Large).padding(bottom = Spacing.ExtraLarge + 24.dp)) {
            Text("COMPARE", style = com.lsing.timego.ui.theme.EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$programName vs your last 30 days", style = MaterialTheme.typography.titleLarge)
            Text(
                "Weekly sets per muscle, projected from the program as written (3 sets per exercise, each routine once a week).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.ExtraSmall, bottom = Spacing.Small),
            )
            if (diff.gainers.isEmpty() && diff.drops.isEmpty()) {
                Text("No notable change: the weekly volume per muscle would stay within half a set.", style = MaterialTheme.typography.bodyMedium)
            }
            listOf("Gains" to diff.gainers, "Drops" to diff.drops).forEach { (title, rows) ->
                if (rows.isNotEmpty()) {
                    SectionHeader(title, topPadding = Spacing.Small)
                    rows.forEach { row ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall)) {
                            Text(formatEnumLabel(row.muscle), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(
                                "${if (row.delta > 0) "+" else ""}${"%.1f".format(row.delta)} sets/wk",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (row.delta > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                            )
                            Text(
                                "  ${"%.1f".format(row.currentWeekly)} to ${"%.1f".format(row.projectedWeekly)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Per-exercise notes, left/right state and asymmetry flags, handed to every strength row without threading parameters. */
private data class LogExtras(
    val routineNotes: Map<Long, String> = emptyMap(),
    val draftNotes: Map<Long, String> = emptyMap(),
    val onDraftNoteChange: ((Long, String) -> Unit)? = null,
    val nextSides: Map<Long, String> = emptyMap(),
    val onToggleSide: ((Long) -> Unit)? = null,
    val asymmetries: Map<Long, com.lsing.timego.domain.AsymmetryInsight> = emptyMap(),
    val holdModifiers: Map<Long, com.lsing.timego.domain.LeverageModifier?> = emptyMap(),
    val onHoldModifierChange: ((Long, com.lsing.timego.domain.LeverageModifier?) -> Unit)? = null,
)
private val LocalLogExtras = androidx.compose.runtime.compositionLocalOf { LogExtras() }
private const val EXPAND_FOLLOW_NANOS = 1_000_000_000L
private const val EXPAND_FOLLOW_FRACTION = 0.25f

/** ExerciseCard recentres itself in its scroll parent when expanded; the active-session list scrolls to
 *  each card itself (see LoggingContent), so it turns this off. */
private val LocalAutoScrollExpandedCard = androidx.compose.runtime.compositionLocalOf { true }

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LogScreen(viewModel: LogViewModel = viewModel()) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val rpeEntryMode by viewModel.rpeEntryMode.collectAsStateWithLifecycle()
    val landingSummary by viewModel.landingSummary.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val routineExercisesById by viewModel.routineExercisesById.collectAsStateWithLifecycle()
    val landingBalanceTimeframe by viewModel.landingBalanceTimeframe.collectAsStateWithLifecycle()
    val landingMuscleBalance by viewModel.landingMuscleBalance.collectAsStateWithLifecycle()
    val routineLastCompleted by viewModel.routineLastCompleted.collectAsStateWithLifecycle()
    val activeTimer by viewModel.activeTimer.collectAsStateWithLifecycle()
    val guidanceByKey by viewModel.guidanceByKey.collectAsStateWithLifecycle()
    val exerciseLibrary by viewModel.exerciseLibrary.collectAsStateWithLifecycle()
    val activeProgramId by viewModel.activeProgramId.collectAsStateWithLifecycle()
    val calisthenicsTier by viewModel.calisthenicsTier.collectAsStateWithLifecycle()
    val combinedEntryIds by viewModel.combinedEntryExerciseIds.collectAsStateWithLifecycle()
    val bothSidesIds by viewModel.bothSidesExerciseIds.collectAsStateWithLifecycle()
    val weightSteps by viewModel.weightSteps.collectAsStateWithLifecycle()
    val sideModes = SideModes(combinedEntryIds, bothSidesIds, viewModel::toggleCombinedEntry, viewModel::toggleBothSides, weightSteps, viewModel::setWeightStep)
    val routineNotes by viewModel.routineNotes.collectAsStateWithLifecycle()
    val draftSetNotes by viewModel.draftSetNotes.collectAsStateWithLifecycle()
    val nextSides by viewModel.nextSide.collectAsStateWithLifecycle()
    val asymmetries by viewModel.asymmetryByExerciseId.collectAsStateWithLifecycle()
    val holdModifiers by viewModel.holdModifiers.collectAsStateWithLifecycle()
    val logExtras = LogExtras(routineNotes, draftSetNotes, viewModel::setDraftSetNote, nextSides, viewModel::toggleNextSide, asymmetries, holdModifiers, viewModel::setHoldModifier)
    var peekingLanding by rememberSaveable { mutableStateOf(false) }
    var expandedExerciseIds by rememberSaveable { mutableStateOf(listOf<Long>()) }
    var librarySearchQuery by rememberSaveable { mutableStateOf("") }
    var exerciseListOrderName by rememberSaveable { mutableStateOf(ExerciseListOrder.CURRENT.name) }
    var draftSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var activeLogPageName by rememberSaveable { mutableStateOf(ActiveLogPage.SESSION.name) }
    var selectedExerciseIds by rememberSaveable { mutableStateOf(listOf<Long>()) }
    var detailExercise by remember { mutableStateOf<com.lsing.timego.data.Exercise?>(null) }
    // Set just before starting a routine session (see onStartOrContinue below) and consumed by the
    // LaunchedEffect once the new session actually exists -- setting selectedExerciseIds directly in
    // the click handler would be clobbered by this same effect's own reset when sessionState updates.
    var pendingRoutineExerciseIds by remember { mutableStateOf<List<Long>?>(null) }

    // Back unwinds the active-session layers one at a time (picker -> logging -> landing); at the landing
    // level it is disabled so the root handler in TimeGoNavHost owns exit.
    BackHandler(enabled = sessionState is SessionUiState.Active && !peekingLanding) {
        if (activeLogPageName == ActiveLogPage.EXERCISE_PICKER.name) {
            activeLogPageName = ActiveLogPage.SESSION.name
        } else {
            peekingLanding = true
        }
    }

    LaunchedEffect(sessionState) {
        val sessionId = (sessionState as? SessionUiState.Active)?.sessionId
        if (sessionId != draftSessionId) {
            draftSessionId = sessionId
            expandedExerciseIds = emptyList()
            librarySearchQuery = ""
            peekingLanding = false
            activeLogPageName = ActiveLogPage.SESSION.name
            selectedExerciseIds = pendingRoutineExerciseIds ?: emptyList()
            pendingRoutineExerciseIds = null
        }
    }

    val imeVisible by androidx.compose.runtime.rememberUpdatedState(WindowInsets.isImeVisible)
    LaunchedEffect(Unit) {
        viewModel.advanceToExercise.collect { request ->
            // Rest ran out on its own while the keyboard is open: the user is typing, so leave the screen alone.
            if (!request.userInitiated && imeVisible) return@collect
            val exerciseId = request.exerciseId
            if (exerciseId !in selectedExerciseIds) selectedExerciseIds = selectedExerciseIds + exerciseId
            expandedExerciseIds = listOf(exerciseId)
        }
    }

    val continuePrompt by viewModel.continuePrompt.collectAsStateWithLifecycle()
    if (continuePrompt != null) {
        TimeGoDialog(
            onDismissRequest = viewModel::dismissContinuePrompt,
            eyebrow = "SESSION",
            title = "Continue today's session?",
            confirmButton = { TextButton(onClick = viewModel::continueFinishedSession) { Text("Continue it") } },
            dismissButton = { TextButton(onClick = viewModel::startNewInstead) { Text("Start new") } },
        ) {
            Text("You finished this workout earlier today. Continue it to add a forgotten exercise, or start a separate session.", style = MaterialTheme.typography.bodyMedium)
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalSideModes provides sideModes,
        LocalLogExtras provides logExtras,
        LocalRpeEntryMode provides rpeEntryMode,
        LocalShowExerciseDetails provides { name -> detailExercise = exerciseLibrary.firstOrNull { it.name == name } },
    ) {
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
            is SessionUiState.Loading -> LogLoadingContent()
            is SessionUiState.NoActiveSession -> LogLandingContent(
                summary = landingSummary,
                routines = routines,
                routineExercisesById = routineExercisesById,
                isSessionActive = false,
                onStartOrContinue = { routineId ->
                    pendingRoutineExerciseIds = routineId?.let { id -> routineExercisesById[id]?.map { it.id } }.orEmpty()
                    viewModel.startSession(routineId)
                },
                onChooseAnother = viewModel::chooseAnotherSuggestion,
                activeProgramId = activeProgramId,
                onSetActiveProgramId = viewModel::setActiveProgramId,
                programDiffFor = viewModel::programDiff,
                calisthenicsTier = calisthenicsTier,
                onSetCalisthenicsTier = viewModel::setCalisthenicsTier,
                onOpenExerciseDetails = { detailExercise = it },
                routineLastCompleted = routineLastCompleted,
                balanceTimeframe = landingBalanceTimeframe,
                muscleBalance = landingMuscleBalance,
                onSelectBalanceTimeframe = viewModel::selectLandingBalanceTimeframe,
            )
            is SessionUiState.Active -> {
                val activeViewStateHolder = rememberSaveableStateHolder()
                AnimatedContent(
                    targetState = if (peekingLanding) "LANDING" else activeLogPageName,
                    transitionSpec = {
                        val enteringLanding = targetState == "LANDING"
                        val enter = slideInHorizontally(TimeGoMotion.navigationInOffset) { width -> if (enteringLanding) -width / 4 else width / 4 } + fadeIn(TimeGoMotion.contentEnter)
                        val exit = slideOutHorizontally(TimeGoMotion.navigationOutOffset) { width -> if (enteringLanding) width / 4 else -width / 4 } + fadeOut(TimeGoMotion.contentExit)
                        enter togetherWith exit
                    },
                    label = "activeLogPageTransition",
                ) { page ->
                    if (page == "LANDING") {
                        activeViewStateHolder.SaveableStateProvider("activeLanding") {
                            LogLandingContent(
                                summary = landingSummary,
                                routines = routines,
                                routineExercisesById = routineExercisesById,
                                isSessionActive = true,
                                onStartOrContinue = { peekingLanding = false },
                                onChooseAnother = {},
                                activeProgramId = activeProgramId,
                                onSetActiveProgramId = viewModel::setActiveProgramId,
                                programDiffFor = viewModel::programDiff,
                                calisthenicsTier = calisthenicsTier,
                                onSetCalisthenicsTier = viewModel::setCalisthenicsTier,
                                onOpenExerciseDetails = { detailExercise = it },
                                routineLastCompleted = routineLastCompleted,
                                balanceTimeframe = landingBalanceTimeframe,
                                muscleBalance = landingMuscleBalance,
                                onSelectBalanceTimeframe = viewModel::selectLandingBalanceTimeframe,
                            )
                        }
                    } else if (page == ActiveLogPage.EXERCISE_PICKER.name) {
                        activeViewStateHolder.SaveableStateProvider("exercisePicker") {
                            ExercisePickerContent(
                                exercises = viewModel.exerciseLibrary.collectAsStateWithLifecycle().value,
                                viewModel = viewModel,
                                activeTimer = activeTimer,
                                listOrder = ExerciseListOrder.valueOf(exerciseListOrderName),
                                onListOrderChange = { exerciseListOrderName = it.name },
                                searchQuery = librarySearchQuery,
                                onSearchQueryChange = { librarySearchQuery = it },
                                favoriteExerciseIds = viewModel.favoriteExerciseIds.collectAsStateWithLifecycle().value,
                                onToggleFavorite = viewModel::toggleFavoriteExercise,
                                onAddCustomExercise = viewModel::addCustomExercise,
                                onBack = { activeLogPageName = ActiveLogPage.SESSION.name },
                                // First set from the picker: back to the session page with that exercise
                                // expanded, where the next set, rest timer and Log button already live.
                                onSetLogged = { exerciseId ->
                                    if (exerciseId !in selectedExerciseIds) selectedExerciseIds = selectedExerciseIds + exerciseId
                                    expandedExerciseIds = listOf(exerciseId)
                                    activeLogPageName = ActiveLogPage.SESSION.name
                                },
                            )
                        }
                    } else {
                        activeViewStateHolder.SaveableStateProvider("activeLogging") {
                            LoggingContent(
                                sessionId = state.sessionId,
                                viewModel = viewModel,
                                activeTimer = activeTimer,
                                expandedExerciseIds = expandedExerciseIds,
                                onExpandedExerciseIdsChange = { expandedExerciseIds = it },
                                selectedExerciseIds = selectedExerciseIds,
                                onOpenExercisePicker = { activeLogPageName = ActiveLogPage.EXERCISE_PICKER.name },
                                onSelectExercise = { exerciseId ->
                                    if (exerciseId !in selectedExerciseIds) selectedExerciseIds = selectedExerciseIds + exerciseId
                                    expandedExerciseIds = listOf(exerciseId)
                                },
                                onEndSession = viewModel::endActiveSession,
                                onBackToLanding = { peekingLanding = true },
                            )
                        }
                    }
                }
            }
        }
    }
    }
    detailExercise?.let { exercise ->
        val guidance = exercise.catalogueKey?.let(guidanceByKey::get)
        val model = buildExerciseDetail(exercise, guidance)
        val easierExercise = model.easierVariationKey?.let { key ->
            exerciseLibrary.firstOrNull { it.catalogueKey == key }
        }
        ExerciseDetailSheet(
            model = model,
            easierName = easierExercise?.name,
            onShowEasier = { easierExercise?.let { detailExercise = it } },
            onDismiss = { detailExercise = null },
        )
    }
}

@Composable
private fun LogLoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.Large),
    ) {
        Text("Today's Workout", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Preparing your session…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.ExtraSmall, bottom = Spacing.Medium),
        )
        SurfaceCard(modifier = Modifier.fillMaxWidth(), hero = true) {
            Column(modifier = Modifier.padding(Spacing.Medium)) {
                ShimmerLine(width = 112.dp, height = 12.dp)
                Spacer(modifier = Modifier.height(Spacing.Small))
                ShimmerLine(width = 210.dp, height = 22.dp)
                Spacer(modifier = Modifier.height(Spacing.Medium))
                ShimmerLine(width = 170.dp, height = 12.dp)
                Spacer(modifier = Modifier.height(Spacing.Medium))
                ShimmerLine(width = 260.dp, height = 44.dp)
            }
        }
        Spacer(modifier = Modifier.height(Spacing.Medium))
        ShimmerLine(width = 150.dp, height = 16.dp)
        ShimmerLine(width = 280.dp, height = 88.dp, modifier = Modifier.padding(top = Spacing.Small))
    }
}

/** Action-centric Command Center landing page: Hero next workout, last session recap, and routines carousel. */
@Composable
private fun LogLandingContent(
    summary: LandingSummary,
    routines: List<com.lsing.timego.data.Routine>,
    routineExercisesById: Map<Long, List<com.lsing.timego.data.Exercise>>,
    isSessionActive: Boolean,
    onStartOrContinue: (routineId: Long?) -> Unit,
    onChooseAnother: () -> Unit,
    activeProgramId: String?,
    onSetActiveProgramId: (String?) -> Unit,
    programDiffFor: (String) -> com.lsing.timego.domain.ProgramDiff,
    calisthenicsTier: com.lsing.timego.data.CalisthenicsTier,
    onSetCalisthenicsTier: (com.lsing.timego.data.CalisthenicsTier) -> Unit,
    routineLastCompleted: Map<Long, LocalDate>,
    balanceTimeframe: ProgressTimeframe,
    muscleBalance: Map<String, Float>,
    onSelectBalanceTimeframe: (ProgressTimeframe) -> Unit,
    onOpenExerciseDetails: (com.lsing.timego.data.Exercise) -> Unit,
) {
    var showLastSessionDetail by remember { mutableStateOf(false) }

    if (showLastSessionDetail && summary.lastSession != null) {
        WorkoutHistoryDialog(
            title = "Last session",
            entries = summary.lastSession.detail,
            groupedEntries = summary.lastSession.groupedDetail,
            onDismiss = { showLastSessionDetail = false },
            label = summary.lastSession.label,
            durationMinutes = summary.lastSession.durationMinutes.toDouble(),
        )
    }

    // Program picker is a filter over already-seeded routines (see SeedRoutines.kt), not a live
    // recommender: "None" shows only user-created routines (programId == null); a seeded program
    // shows just its own routines, narrowed to the selected tier for Calisthenics Progression.
    val filteredRoutines = remember(routines, activeProgramId, calisthenicsTier) {
        routines.filter { routine ->
            when {
                activeProgramId == null -> routine.programId == null
                routine.programId != activeProgramId -> false
                activeProgramId == "calisthenics_progression" -> routine.tier == calisthenicsTier.name
                else -> true
            }
        }
    }
    val todaysScheduledRoutine = remember(filteredRoutines) {
        routinesForToday(filteredRoutines, LocalDate.now().dayOfWeek).firstOrNull()
    }
    val flexibleRoutinesList = remember(filteredRoutines) {
        flexibleRoutines(filteredRoutines)
    }
    // One selectable list for the Today's Workout card: today's scheduled routine wins, otherwise the
    // flexible routine whose muscles are most recovered, otherwise the first routine of the program.
    val defaultSuggestedRoutine = remember(filteredRoutines, todaysScheduledRoutine, flexibleRoutinesList, routineLastCompleted, routineExercisesById, summary.lastWorkedByGroup) {
        todaysScheduledRoutine ?: nextFlexibleRoutineByRecovery(
            flexibleRoutinesList,
            routinePrimaryGroups = routineExercisesById.mapValues { (_, exercises) -> exercises.flatMapTo(mutableSetOf()) { primaryMuscleGroups(it) } },
            lastWorkedByGroup = summary.lastWorkedByGroup,
            routineLastCompleted = routineLastCompleted,
            today = LocalDate.now(),
        ) ?: filteredRoutines.firstOrNull()
    }
    var userSelectedRoutineId by remember(filteredRoutines) {
        mutableStateOf<Long?>(null)
    }
    val activeRoutine = remember(filteredRoutines, defaultSuggestedRoutine, userSelectedRoutineId) {
        userSelectedRoutineId?.let { id -> filteredRoutines.firstOrNull { it.id == id } }
            ?: defaultSuggestedRoutine
    }

    // A routine's own exercises are the ground truth for its target muscles -- no recommender
    // needed, unlike the generic Recommended Focus diagram below.
    fun regionsForRoutine(routineId: Long): Set<String> =
        expandMuscleGroupRegions(routineExercisesById[routineId].orEmpty().flatMap { primaryMuscleGroups(it) }.toSet())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = LocalDockHeight.current + LocalRestClearance.current)
            .padding(Spacing.Large),
    ) {
        if (isSessionActive) {
            SurfaceCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium),
                hero = true,
            ) {
                Column(modifier = Modifier.padding(Spacing.Medium)) {
                    Text(
                        "Session In Progress",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
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
            if (activeRoutine != null) {
                val isScheduledToday = activeRoutine.id == todaysScheduledRoutine?.id
                val isFlexible = activeRoutine.daysOfWeek.isEmpty()
                SurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = TimeGoMotion.expandEnter)
                        .padding(bottom = Spacing.Medium),
                    hero = true,
                ) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)) {
                                Text(
                                    when {
                                        isScheduledToday -> "Scheduled for today"
                                        !isFlexible -> "Scheduled · " + activeRoutine.daysOfWeek.joinToString(", ") { it.take(3).lowercase().replaceFirstChar(Char::uppercase) }
                                        activeRoutine.id == defaultSuggestedRoutine?.id && flexibleRoutinesList.size > 1 -> "Next in rotation"
                                        else -> "Flexible routine"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                )
                            }
                            if (isFlexible) {
                                Icon(
                                    Icons.Filled.AllInclusive,
                                    contentDescription = "Flexible routine",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Text(
                            activeRoutine.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = Spacing.Small, bottom = Spacing.ExtraSmall),
                        )
                        val lastTrainedStr = formatDaysSince(routineLastCompleted[activeRoutine.id], LocalDate.now())
                        Text(
                            "Last completed: $lastTrainedStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Spacing.Small),
                        )
                        val routineRegions = regionsForRoutine(activeRoutine.id)
                        val regionLabels = remember(activeRoutine.id, routineExercisesById) {
                            routineExercisesById[activeRoutine.id].orEmpty().map { exerciseDisplayRegion(it) }.distinct()
                        }
                        if (regionLabels.isNotEmpty()) {
                            RegionChipRow(regionLabels, modifier = Modifier.padding(bottom = Spacing.Medium))
                        }
                        if (routineRegions.isNotEmpty()) {
                            CroppedMuscleDiagram(
                                muscleGroups = diagramGroupsForBodyRegionCrop(routineRegions),
                                accentColor = MaterialTheme.colorScheme.primary,
                                highlightGroups = routineRegions,
                                showNeutralContext = true,
                                colorByRegion = true,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 130.dp).padding(bottom = Spacing.Small),
                            )
                        }
                        if (filteredRoutines.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(bottom = Spacing.Medium),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                            ) {
                                filteredRoutines.forEach { routine ->
                                    FilterChip(
                                        selected = routine.id == activeRoutine.id,
                                        onClick = { userSelectedRoutineId = routine.id },
                                        label = { Text(routine.name) },
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = { onStartOrContinue(activeRoutine.id) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) {
                            Text("Start ${activeRoutine.name}", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(
                            onClick = { onStartOrContinue(null) },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 2.dp),
                        ) {
                            Text(
                                "Or start freeform workout",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                SurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = TimeGoMotion.expandEnter)
                        .padding(bottom = Spacing.Medium),
                    hero = true,
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
                                TextButton(
                                    onClick = { onOpenExerciseDetails(exercise) },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.padding(top = Spacing.ExtraSmall).heightIn(min = 48.dp),
                                ) { Text("Try: ${exercise.name}", style = MaterialTheme.typography.bodySmall) }
                                summary.recommendationNote?.let { note ->
                                    Text(
                                        note,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                                if (!isSessionActive && summary.canChooseAnother) {
                                    TextButton(
                                        onClick = onChooseAnother,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = Spacing.ExtraSmall),
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) {
                                        Text("Choose another", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                            if (!isSessionActive && summary.noAlternativesLeft) {
                                Text(
                                    "No other familiar matches found. Start freeform and pick any exercise.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = Spacing.ExtraSmall),
                                )
                            }
                            CroppedMuscleDiagram(
                                muscleGroups = diagramGroupsForBodyRegionCrop(
                                    summary.recommendedMuscleGroups.toSet(),
                                ),
                                accentColor = MaterialTheme.colorScheme.primary,
                                highlightGroups = summary.recommendedMuscleGroups.toSet(),
                                highlightStrengths = summary.recommendationHighlightStrengths,
                                showNeutralContext = true,
                                colorByRegion = true,
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

            // Program filters the seeded routines below. Keep the choice in one compact control so
            // the landing page remains scannable on smaller phones and with larger text settings.
            val selectedProgramName = activeProgramId?.let { id ->
                com.lsing.timego.data.PROGRAM_DISPLAY_NAMES.entries.firstOrNull { it.key == id }?.value
            } ?: "Custom routines"
            var programMenuExpanded by rememberSaveable { mutableStateOf(false) }
            SectionHeader("Program", topPadding = Spacing.Small)
            SurfaceCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small),
                hero = false,
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(Spacing.Medium)) {
                    Text(
                        text = "Choose a program",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                    Box {
                        OutlinedButton(
                            onClick = { programMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = Spacing.Medium),
                        ) {
                            Text(
                                text = selectedProgramName,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Icon(
                                imageVector = if (programMenuExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.ChevronRight,
                                contentDescription = if (programMenuExpanded) "Collapse program list" else "Expand program list",
                            )
                        }
                        DropdownMenu(
                            expanded = programMenuExpanded,
                            onDismissRequest = { programMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.92f),
                        ) {
                            val programItem: @Composable (String?, String) -> Unit = { programId, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        onSetActiveProgramId(programId)
                                        programMenuExpanded = false
                                    },
                                    leadingIcon = if (activeProgramId == programId) {
                                        { Icon(Icons.Filled.Check, contentDescription = null) }
                                    } else null,
                                )
                            }
                            if (routines.any { it.programId == com.lsing.timego.data.RIR_PROGRAM_ID }) {
                                programItem(com.lsing.timego.data.RIR_PROGRAM_ID, "Upper/Lower AB (RIR) - my program")
                            }
                            programItem(null, "Custom routines")
                            HorizontalDivider()
                            com.lsing.timego.data.PROGRAM_DISPLAY_NAMES
                                .filterKeys { it != com.lsing.timego.data.RIR_PROGRAM_ID }
                                .forEach { (programId, name) -> programItem(programId, name) }
                        }
                    }
                    Text(
                        text = if (activeProgramId == null) {
                            "Showing your custom routines"
                        } else {
                            "Showing routines from this program"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.ExtraSmall),
                    )
                    var showProgramDiff by rememberSaveable { mutableStateOf(false) }
                    if (activeProgramId != null && com.lsing.timego.data.SEED_ROUTINES.any { it.programId == activeProgramId }) {
                        TextButton(onClick = { showProgramDiff = true }) { Text("Compare vs current") }
                    }
                    val diffProgramId = activeProgramId
                    if (showProgramDiff && diffProgramId != null) {
                        ProgramDiffSheet(
                            programName = selectedProgramName,
                            diff = remember(diffProgramId) { programDiffFor(diffProgramId) },
                            onDismiss = { showProgramDiff = false },
                        )
                    }
                }
            }
            if (activeProgramId == "calisthenics_progression") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    com.lsing.timego.data.CalisthenicsTier.entries.forEach { tier ->
                        FilterChip(
                            selected = calisthenicsTier == tier,
                            onClick = { onSetCalisthenicsTier(tier) },
                            label = { Text(formatEnumLabel(tier.name)) },
                        )
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
                        muscleGroups = diagramGroupsForBodyRegionCrop(summary.lastSession.muscleGroups),
                        intensities = summary.lastSession.muscleIntensities,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        highlightGroups = summary.lastSession.muscleGroups,
                        showNeutralContext = true,
                        colorByRegion = true,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 130.dp).padding(top = Spacing.Small),
                    )
                    if (summary.lastSession.muscleGroups.isNotEmpty()) {
                        RegionChipRow(
                            regionLabelsFor(summary.lastSession.muscleGroups),
                            modifier = Modifier.padding(top = Spacing.Small),
                        )
                        Text(
                            "Brighter means more work this session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.Small),
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
    activeTimer: ActiveTimer?,
    expandedExerciseIds: List<Long>,
    onExpandedExerciseIdsChange: (List<Long>) -> Unit,
    selectedExerciseIds: List<Long>,
    onOpenExercisePicker: () -> Unit,
    onSelectExercise: (Long) -> Unit,
    onEndSession: () -> Unit,
    onBackToLanding: () -> Unit,
) {
    val exercises by viewModel.displayedExercises.collectAsStateWithLifecycle()
    val libraryExercises by viewModel.exerciseLibrary.collectAsStateWithLifecycle()
    val quickAddExercises by viewModel.quickAddExercises.collectAsStateWithLifecycle()
    val favoriteExerciseIds by viewModel.favoriteExerciseIds.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val holdSuggestions by viewModel.holdSuggestions.collectAsStateWithLifecycle()
    val lastWorkingSets by viewModel.lastWorkingSets.collectAsStateWithLifecycle()
    val latestBodyWeightKg by viewModel.latestBodyWeightKg.collectAsStateWithLifecycle()
    val holdDelaySeconds by viewModel.holdDelaySeconds.collectAsStateWithLifecycle()
    val setLoggedPulse by viewModel.setLoggedPulse.collectAsStateWithLifecycle()
    val activeSessionSetsByExercise by viewModel.activeSessionSetsByExercise.collectAsStateWithLifecycle()
    val prescriptions by viewModel.activePrescriptions.collectAsStateWithLifecycle()
    val restTimer by viewModel.restTimer.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) {}
    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    // Ask once, the first time a rest timer starts, so the end-of-rest alert can reach a backgrounded app.
    LaunchedEffect(restTimer != null) {
        if (restTimer != null && !askedNotificationPermission &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            askedNotificationPermission = true
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var showEndSessionConfirmation by remember { mutableStateOf(false) }
    val activeListState = rememberLazyListState()
    val exercisesById = remember(libraryExercises) { libraryExercises.associateBy { it.id } }
    val selectedExercises = remember(selectedExerciseIds, activeSessionSetsByExercise, exercisesById) {
        (selectedExerciseIds + activeSessionSetsByExercise.keys).distinct().mapNotNull { exercisesById[it] }
    }
    val focusedExerciseId = expandedExerciseIds.singleOrNull()
    val density = LocalDensity.current
    // Each exercise is its own lazy item, so the expanded one can be scrolled to exactly: top of the
    // viewport with a small gap. Scrolled twice because the previously expanded card is still collapsing
    // during the first scroll and shifts everything below it.
    var focusAppliedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(focusedExerciseId) {
        val index = selectedExercises.indexOfFirst { it.id == focusedExerciseId }
        if (index < 0) return@LaunchedEffect
        val target = FIRST_EXERCISE_ITEM_INDEX + index
        val topGap = -with(density) { Spacing.Small.roundToPx() }
        if (!focusAppliedOnce) {
            // First arrival (e.g. back from the picker after logging): the page is already sliding in, so
            // place the card instantly rather than layering a second scroll animation on top of it.
            focusAppliedOnce = true
            activeListState.scrollToItem(target, topGap)
        }
        // The opening card keeps growing and the previously open one shrinks above it, so its position moves for
        // about a second. A one-shot animated scroll aims at where the card is now and overshoots (it leaves the
        // screen, then snaps back). Instead follow it: every frame close a fraction of the remaining gap, which
        // stays smooth while the target moves and settles on its own.
        val endNanos = System.nanoTime() + EXPAND_FOLLOW_NANOS
        while (System.nanoTime() < endNanos) {
            withFrameNanos { }
            val offset = activeListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == target }?.offset
            if (offset == null) {
                activeListState.scrollToItem(target, topGap)
            } else {
                val gap = (offset - topGap).toFloat()
                if (kotlin.math.abs(gap) > 1f) activeListState.scrollBy(gap * EXPAND_FOLLOW_FRACTION)
            }
        }
    }

    if (showEndSessionConfirmation) {
        TimeGoDialog(
            onDismissRequest = { showEndSessionConfirmation = false },
            eyebrow = "SESSION",
            title = "End workout?",
            dismissButton = {
                val dismiss = com.lsing.timego.ui.common.LocalTimeGoDialogDismiss.current
                TextButton(onClick = dismiss) { Text("Keep logging") }
            },
            confirmButton = {
                val dismiss = com.lsing.timego.ui.common.LocalTimeGoDialogDismiss.current
                Button(
                    onClick = {
                        dismiss()
                        onEndSession()
                    },
                ) { Text("End session") }
            },
        ) {
            Text(
                "Your logged sets are saved. End this session when you are finished adding sets.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalAutoScrollExpandedCard provides false) {
    Box(modifier = Modifier.fillMaxSize().imePadding()) {
    LazyColumn(
        state = activeListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.Large),
        contentPadding = PaddingValues(bottom = Spacing.ExtraLarge + LocalDockHeight.current + LocalRestClearance.current),
    ) {
            item(key = "activeHeader") {
                val readinessEnabled by viewModel.readinessEnabled.collectAsStateWithLifecycle()
                val activeReadiness by viewModel.activeReadiness.collectAsStateWithLifecycle()
                var readinessSkipped by rememberSaveable { mutableStateOf(false) }
                if (readinessEnabled && activeReadiness == null && !readinessSkipped) {
                    ReadinessPrompt(onRate = viewModel::setReadiness, onSkip = { readinessSkipped = true })
                }
                Text(
                    "Current session",
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
            }
            item(key = "activeSummary") {
                // Live in-session summary of completed sets
                val exercisesById = remember(libraryExercises) { libraryExercises.associateBy { it.id } }
                ActiveWorkoutSection(
                    activeSetsByExercise = activeSessionSetsByExercise,
                    exercisesById = exercisesById,
                    selectedExerciseId = expandedExerciseIds.singleOrNull(),
                    onSelectExercise = { exerciseId ->
                        exercisesById[exerciseId]?.let { onSelectExercise(exerciseId) }
                    },
                )
            }
            item(key = "favorites") {
                val favoriteExercises = libraryExercises.filter { it.id in favoriteExerciseIds }
                SectionHeader("Favorites", topPadding = Spacing.Small)
                if (favoriteExercises.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                    ) {
                        favoriteExercises.forEach { exercise ->
                            AnimatedAssistChip(
                                onClick = {
                                    onSelectExercise(exercise.id)
                                },
                                label = { Text(exercise.name) },
                            )
                        }
                    }
                } else {
                    Text(
                        "Your starred exercises will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.Small),
                    )
                }
            }
            item(key = "quickAdd") {
                SectionHeader("Quick add", topPadding = Spacing.Small)
                if (quickAddExercises.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                    ) {
                        quickAddExercises.forEach { exercise ->
                            AnimatedAssistChip(
                                onClick = {
                                    onSelectExercise(exercise.id)
                                },
                                label = { Text(exercise.name) },
                            )
                        }
                    }
                } else {
                    val hasLoggedStrength = activeSessionSetsByExercise.keys.any { id ->
                        libraryExercises.firstOrNull { it.id == id }?.category in setOf(
                            ExerciseCategory.STRENGTH.name,
                            ExerciseCategory.CALISTHENICS.name,
                        )
                    }
                    Text(
                        if (hasLoggedStrength) "No familiar strength exercises to add yet." else "Log a strength or calisthenics set to unlock session-aware quick add.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.Small),
                    )
                }
            }
            item(key = "logExerciseAction") {
                Button(
                    onClick = onOpenExercisePicker,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.ExtraSmall, bottom = Spacing.Small),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = Spacing.ExtraSmall))
                    Text("Log exercise")
                }
            }
            item(key = "exercisesHeader") {
                if (selectedExercises.isNotEmpty()) {
                    SectionHeader("Exercises", topPadding = Spacing.Small)
                }
            }
            items(selectedExercises, key = { "exercise-${it.id}" }) { exercise ->
                run {
                    val currentExerciseSets = activeSessionSetsByExercise[exercise.id].orEmpty()
                    when (exercise.loggingType) {
                        LoggingType.HOLD.name -> HoldLogRow(
                            exerciseId = exercise.id,
                            exerciseName = exercise.name,
                            category = exercise.category,
                            suggestion = holdSuggestions[exercise.id],
                            delaySeconds = holdDelaySeconds,
                            isFavorite = exercise.id in favoriteExerciseIds,
                            onToggleFavorite = { viewModel.toggleFavoriteExercise(exercise.id) },
                            pulseId = setLoggedPulse?.takeIf { it.exerciseId == exercise.id }?.eventId ?: 0L,
                            activeTimer = activeTimer,
                            expanded = exercise.id in expandedExerciseIds,
                            onToggle = {
                                onExpandedExerciseIdsChange(toggleExpandedExerciseIds(expandedExerciseIds, exercise.id))
                            },
                            onLog = { duration, target, isWarmup, targetProvenance -> viewModel.logHoldSet(exercise.id, duration, target, isWarmup, targetProvenance) },
                            onStartTimer = { viewModel.startTimer(exercise.id) },
                            onCancelTimer = { viewModel.clearActiveTimer(exercise.id) },
                            todaySetCount = currentExerciseSets.count { !it.isWarmup },
                        )
                        LoggingType.DURATION_DISTANCE.name -> CardioLogRow(
                            exerciseId = exercise.id,
                            exerciseName = exercise.name,
                            category = exercise.category,
                            met = if (exercise.category == ExerciseCategory.CARDIO.name) MET_CARDIO else MET_WARMUP,
                            bodyWeightKg = latestBodyWeightKg,
                            delaySeconds = holdDelaySeconds,
                            isFavorite = exercise.id in favoriteExerciseIds,
                            onToggleFavorite = { viewModel.toggleFavoriteExercise(exercise.id) },
                            pulseId = setLoggedPulse?.takeIf { it.exerciseId == exercise.id }?.eventId ?: 0L,
                            activeTimer = activeTimer,
                            expanded = exercise.id in expandedExerciseIds,
                            onToggle = {
                                onExpandedExerciseIdsChange(toggleExpandedExerciseIds(expandedExerciseIds, exercise.id))
                            },
                            onLog = { duration, distance -> viewModel.logCardioSet(exercise.id, duration, distance) },
                            onStartTimer = { viewModel.startTimer(exercise.id) },
                            onCancelTimer = { viewModel.clearActiveTimer(exercise.id) },
                            todaySetCount = currentExerciseSets.size,
                        )
                        else -> StrengthLogRow(
                            exerciseId = exercise.id,
                            exerciseName = exercise.name,
                            category = exercise.category,
                            suggestion = suggestions[exercise.id],
                            lastWorkingSet = lastWorkingSets[exercise.id],
                            currentSessionSets = currentExerciseSets,
                            prescription = prescriptions[exercise.name],
                            isBodyweight = exercise.category == ExerciseCategory.CALISTHENICS.name,
                            latestBodyWeightKg = latestBodyWeightKg,
                            isFavorite = exercise.id in favoriteExerciseIds,
                            onToggleFavorite = { viewModel.toggleFavoriteExercise(exercise.id) },
                            expanded = exercise.id in expandedExerciseIds,
                            pulseId = setLoggedPulse?.takeIf { it.exerciseId == exercise.id }?.eventId ?: 0L,
                            onToggle = {
                                onExpandedExerciseIdsChange(toggleExpandedExerciseIds(expandedExerciseIds, exercise.id))
                            },
                            onLog = { weight, reps, target, isWarmup, addedWeightKg, rpe, targetProvenance ->
                                viewModel.logSet(exercise.id, weight, reps, target, isWarmup, addedWeightKg, rpe, targetProvenance)
                            },
                        )
                    }
                }
            }
            item(key = "activeBottomSpacer") {
                Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
            }
        }
    }
    }
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun ExercisePickerContent(
    exercises: List<com.lsing.timego.data.Exercise>,
    viewModel: LogViewModel,
    activeTimer: ActiveTimer?,
    listOrder: ExerciseListOrder,
    onListOrderChange: (ExerciseListOrder) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    favoriteExerciseIds: Set<Long>,
    onToggleFavorite: (Long) -> Unit,
    onAddCustomExercise: (name: String, muscleGroups: List<String>, category: String) -> Unit,
    onBack: () -> Unit,
    onSetLogged: (exerciseId: Long) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val holdSuggestions by viewModel.holdSuggestions.collectAsStateWithLifecycle()
    val lastWorkingSets by viewModel.lastWorkingSets.collectAsStateWithLifecycle()
    val latestBodyWeightKg by viewModel.latestBodyWeightKg.collectAsStateWithLifecycle()
    val holdDelaySeconds by viewModel.holdDelaySeconds.collectAsStateWithLifecycle()
    val setLoggedPulse by viewModel.setLoggedPulse.collectAsStateWithLifecycle()
    val activeSessionSetsByExercise by viewModel.activeSessionSetsByExercise.collectAsStateWithLifecycle()
    val muscleContext = expandedExerciseId?.let { id ->
        exercises.firstOrNull { it.id == id }?.muscleGroups?.let(::formatMuscleGroupList)
    }
    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, muscleGroups, category ->
                onAddCustomExercise(name, muscleGroups, category)
                showAddDialog = false
            },
        )
    }
    var showOrderMenu by remember { mutableStateOf(false) }
    // The dock hides behind the keyboard, so its clearance is dead space while the keyboard is up.
    val bottomClearance = if (WindowInsets.isImeVisible) 0.dp else LocalDockHeight.current + LocalRestClearance.current
    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = Spacing.Large)
                .padding(top = Spacing.Small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to current session")
                }
                Box {
                    IconButton(
                        onClick = { showOrderMenu = true },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Change exercise order")
                    }
                    DropdownMenu(
                        expanded = showOrderMenu,
                        onDismissRequest = { showOrderMenu = false },
                    ) {
                        ExerciseListOrder.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    onListOrderChange(option)
                                    showOrderMenu = false
                                },
                                leadingIcon = if (listOrder == option) {
                                    { Icon(Icons.Filled.Check, contentDescription = null) }
                                } else null,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!muscleContext.isNullOrBlank()) {
                        Text(
                            muscleContext,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                ExerciseSections(
                    exercises = exercises,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    showSearchField = false,
                    listOrder = listOrder,
                    favoriteExerciseIds = favoriteExerciseIds,
                ) { exercise ->
                    ExercisePickerRow(
                        exercise = exercise,
                        expanded = expandedExerciseId == exercise.id,
                        suggestion = suggestions[exercise.id],
                        holdSuggestion = holdSuggestions[exercise.id],
                        lastWorkingSet = lastWorkingSets[exercise.id],
                        currentSessionSets = activeSessionSetsByExercise[exercise.id].orEmpty(),
                        latestBodyWeightKg = latestBodyWeightKg,
                        holdDelaySeconds = holdDelaySeconds,
                        activeTimer = activeTimer,
                        isFavorite = exercise.id in favoriteExerciseIds,
                        pulseId = setLoggedPulse?.takeIf { it.exerciseId == exercise.id }?.eventId ?: 0L,
                        onToggleFavorite = { onToggleFavorite(exercise.id) },
                        onToggle = {
                            expandedExerciseId = if (expandedExerciseId == exercise.id) null else exercise.id
                        },
                        onLogStrength = { weight, reps, target, isWarmup, addedWeightKg, rpe, provenance ->
                            viewModel.logSet(exercise.id, weight, reps, target, isWarmup, addedWeightKg, rpe, provenance)
                            onSetLogged(exercise.id)
                        },
                        onLogCardio = { duration, distance ->
                            viewModel.logCardioSet(exercise.id, duration, distance)
                            onSetLogged(exercise.id)
                        },
                        onLogHold = { duration, target, isWarmup, provenance ->
                            viewModel.logHoldSet(exercise.id, duration, target, isWarmup, provenance)
                            onSetLogged(exercise.id)
                        },
                        onStartTimer = { viewModel.startTimer(exercise.id) },
                        onCancelTimer = { viewModel.clearActiveTimer(exercise.id) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.ExtraLarge + 72.dp + bottomClearance))
        }
        SurfaceCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // LocalDockHeight already includes the system navigation-bar inset; the rest bar docks
                // at the same spot, so lift the search field above it while a rest is showing.
                .padding(bottom = bottomClearance)
                .padding(horizontal = Spacing.Large, vertical = Spacing.Small),
            hero = true,
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search exercises") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.ExtraSmall, vertical = Spacing.ExtraSmall),
                singleLine = true,
                shape = RoundedCornerShape(20.dp), // concentric with the 24dp card minus its 4dp inset
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
            )
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.Large, bottom = bottomClearance + 88.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add custom exercise")
        }
    }
}

@Composable
private fun ExercisePickerRow(
    exercise: com.lsing.timego.data.Exercise,
    expanded: Boolean,
    suggestion: com.lsing.timego.domain.OverloadSuggestion?,
    holdSuggestion: HoldSuggestion?,
    lastWorkingSet: SetLog?,
    currentSessionSets: List<SetLog>,
    latestBodyWeightKg: Double?,
    holdDelaySeconds: Int,
    activeTimer: ActiveTimer?,
    isFavorite: Boolean,
    pulseId: Long,
    onToggleFavorite: () -> Unit,
    onToggle: () -> Unit,
    onLogStrength: (Double, Int, Int, Boolean, Double?, Int?, TargetProvenance) -> Unit,
    onLogCardio: (Double, Double?) -> Unit,
    onLogHold: (Int, Int, Boolean, TargetProvenance) -> Unit,
    onStartTimer: () -> Unit,
    onCancelTimer: () -> Unit,
) {
    Column {
    when (exercise.loggingType) {
        LoggingType.HOLD.name -> HoldLogRow(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            category = exercise.category,
            suggestion = holdSuggestion,
            delaySeconds = holdDelaySeconds,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            expanded = expanded,
            pulseId = pulseId,
            activeTimer = activeTimer,
            onToggle = onToggle,
            onLog = onLogHold,
            onStartTimer = onStartTimer,
            onCancelTimer = onCancelTimer,
            todaySetCount = currentSessionSets.count { !it.isWarmup },
        )
        LoggingType.DURATION_DISTANCE.name -> CardioLogRow(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            category = exercise.category,
            met = if (exercise.category == ExerciseCategory.CARDIO.name) MET_CARDIO else MET_WARMUP,
            bodyWeightKg = latestBodyWeightKg,
            delaySeconds = holdDelaySeconds,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            expanded = expanded,
            pulseId = pulseId,
            activeTimer = activeTimer,
            onToggle = onToggle,
            onLog = onLogCardio,
            onStartTimer = onStartTimer,
            onCancelTimer = onCancelTimer,
            todaySetCount = currentSessionSets.size,
        )
        else -> StrengthLogRow(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            category = exercise.category,
            suggestion = suggestion,
            lastWorkingSet = lastWorkingSet,
            currentSessionSets = currentSessionSets,
            isBodyweight = exercise.category == ExerciseCategory.CALISTHENICS.name,
            latestBodyWeightKg = latestBodyWeightKg,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            expanded = expanded,
            pulseId = pulseId,
            onToggle = onToggle,
            onLog = onLogStrength,
        )
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
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    todaySummary: String? = null,
    todaySetCount: Int = 0,
) {
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
        // Two states, told apart by label and colour: green TODAY once a set is logged, blue TARGET
        // (derived from last session) before that. The badge sits under the name so a long name never fights it.
        val (badgeLabel, badgeValue, badgeColor) = when {
            todaySummary != null -> Triple("TODAY \u00b7 $todaySetCount ${if (todaySetCount == 1) "SET" else "SETS"}", todaySummary, PlateGreen)
            suggestionSummary != null -> Triple("TARGET", suggestionSummary, PlateBlue)
            else -> Triple(null, null, Color.Unspecified)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(exerciseName, style = MaterialTheme.typography.titleSmall)
            if (badgeValue != null) {
                // FlowRow so the figure drops to its own line at large text sizes instead of being squeezed into a column.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalArrangement = Arrangement.Center) {
                    Text(badgeLabel!!, style = MaterialTheme.typography.labelSmall, color = badgeColor.copy(alpha = 0.75f))
                    Text(badgeValue, style = LedgerFigureValue.copy(fontSize = 14.sp), color = badgeColor)
                }
            }
        }
        val showDetails = LocalShowExerciseDetails.current
        IconButton(onClick = { showDetails(exerciseName) }, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "Exercise details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(48.dp)) {
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
        com.lsing.timego.ui.common.ExpandChevron(expanded)
    }
}

/**
 * Modern Engine-Room Gauge Panel card for active exercise logging:
 * Mounted console plate (SurfaceCard, 10.dp rounded corners, CardFill, hairline border)
 * when collapsed, elevating to an active glowing instrument panel (RaisedFill, hero brass
 * sheen) when expanded, with TrainingPulse on set log.
 */
@Composable
private fun ExerciseCard(expanded: Boolean, pulseId: Long = 0L, content: @Composable () -> Unit) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var cardHeight by remember { mutableStateOf(0) }
    var cardWidth by remember { mutableStateOf(0) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val topGapPx = with(density) { 12.dp.toPx() }
    val bottomClearancePx = with(density) { (LocalDockHeight.current + LocalRestClearance.current + 112.dp).toPx() }
    val topChromePx = with(density) { 220.dp.toPx() }
    val windowHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()

    val autoScroll = LocalAutoScrollExpandedCard.current
    LaunchedEffect(expanded, cardHeight, cardWidth, bottomClearancePx, windowHeightPx) {
        if (autoScroll && expanded && cardHeight > 0 && cardWidth > 0) {
            // Recenter after every animated size change using the actual window viewport. This
            // avoids a second fixed-delay correction and keeps the expanded exercise in the
            // visual middle while the keyboard/insets are changing.
            withFrameNanos { }
            // Keep the whole card clear of what floats over the list (search bar, rest bar, dock), and show
            // its top first when it is taller than the space left.
            val visiblePx = (windowHeightPx - bottomClearancePx - topChromePx).coerceAtLeast(1f)
            val top = if (cardHeight > visiblePx) cardHeight - visiblePx else -topGapPx
            bringIntoViewRequester.bringIntoView(
                androidx.compose.ui.geometry.Rect(
                    left = 0f,
                    top = top,
                    right = cardWidth.toFloat(),
                    bottom = cardHeight + bottomClearancePx,
                ),
            )
        }
    }

    SurfaceCard(
        hero = expanded,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = TimeGoMotion.expandEnter)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onSizeChanged {
                cardWidth = it.width
                cardHeight = it.height
            }
            .padding(vertical = 4.dp),
    ) {
        TrainingPulse(
            active = expanded,
            pulseId = pulseId,
        ) {
            Column {
                content()
            }
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
    exerciseId: Long,
    exerciseName: String,
    category: String,
    suggestion: com.lsing.timego.domain.OverloadSuggestion?,
    lastWorkingSet: SetLog?,
    currentSessionSets: List<SetLog> = emptyList(),
    prescription: com.lsing.timego.data.Prescription? = null,
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
    val sideModes = LocalSideModes.current
    // Notes and left/right state come from a local so the active list and the exercise picker share them.
    val extras = LocalLogExtras.current
    val routineNote = extras.routineNotes[exerciseId]
    val draftNote = extras.draftNotes[exerciseId].orEmpty()
    val onDraftNoteChange = extras.onDraftNoteChange?.let { change -> { text: String -> change(exerciseId, text) } }
    val nextSide = if (exerciseId in sideModes.bothSidesIds) extras.nextSides[exerciseId] ?: com.lsing.timego.domain.Side.LEFT else null
    val onToggleSide = extras.onToggleSide?.let { toggle -> { toggle(exerciseId) } }
    val asymmetry = extras.asymmetries[exerciseId]
    // Combined entry only applies to loaded exercises; bodyweight moves keep their added-kg convention.
    val combined = !isBodyweight && exerciseId in sideModes.combinedEntryIds
    fun displayKg(stored: Double) = com.lsing.timego.domain.storedToCombinedEntry(stored, combined)
    // Steps are per hand; the box shows both dumbbells in combined entry, so it moves twice as far.
    val handStepKg = com.lsing.timego.domain.weightStepKg(exerciseName, combined, sideModes.weightSteps[exerciseId])
    val stepKg = displayKg(handStepKg)
    var weightText by rememberSaveable(exerciseName) { mutableStateOf("") }
    var repsText by rememberSaveable(exerciseName) { mutableStateOf("") }
    var rpeText by rememberSaveable(exerciseName) { mutableStateOf("") }
    var isWarmup by rememberSaveable(exerciseName) { mutableStateOf(false) }
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
    // With a target on screen and nothing typed, one tap logs the target as shown.
    val useTarget = suggestion != null && weightText.isBlank() && repsText.isBlank()
    val canLog = validRpe && (useTarget || (reps != null && if (isBodyweight) totalBodyweightLoad != null else enteredWeight != null))
    val dismissKeyboard = rememberDismissKeyboard()
    val weightFocus = remember { FocusRequester() }
    val repsFocus = remember { FocusRequester() }
    val rpeFocus = remember { FocusRequester() }
    val logSet: () -> Unit = {
        if (useTarget && suggestion != null) {
            val addedKg = if (isBodyweight) latestBodyWeightKg?.let { bw -> suggestion.weightKg - bw } ?: suggestion.weightKg else null
            onLog(suggestion.weightKg, suggestion.reps, suggestion.reps, isWarmup, addedKg, rpe, targetProvenanceFor(true))
            rpeText = ""
            isWarmup = false
            dismissKeyboard()
        } else if (isBodyweight) {
            if (reps != null && enteredWeight != null && totalBodyweightLoad != null) {
                onLog(totalBodyweightLoad, reps, suggestion?.reps ?: reps, isWarmup, enteredWeight, rpe, targetProvenanceFor(suggestion != null))
                weightText = ""
                repsText = ""
                rpeText = ""
                isWarmup = false
                dismissKeyboard()
            }
        } else if (enteredWeight != null && reps != null) {
            onLog(com.lsing.timego.domain.combinedEntryToStored(enteredWeight, combined), reps, suggestion?.reps ?: reps, isWarmup, null, rpe, targetProvenanceFor(suggestion != null))
            weightText = ""
            repsText = ""
            rpeText = ""
            isWarmup = false
            dismissKeyboard()
        }
    }
    val visual = categoryVisual(category)
    val suggestionHint = suggestion?.let {
        if (isBodyweight) {
            val addedK = latestBodyWeightKg?.let { bw -> it.weightKg - bw } ?: it.weightKg
            "${formatCalisthenicsWeight(addedK)} x ${it.reps}"
        } else {
            "${displayKg(it.weightKg)}kg x ${it.reps}"
        }
    }
    val lastSetHint = lastWorkingSet?.let { set ->
        val weight = if (isBodyweight && set.addedWeightKg != null) {
            formatCalisthenicsWeight(set.addedWeightKg)
        } else {
            "${displayKg(set.weightKg)}kg"
        }
        "Last time: $weight x ${set.reps}"
    }

    val workingToday = currentSessionSets.filter { !it.isWarmup }
    val todayHint = workingToday.lastOrNull()?.let { set ->
        if (isBodyweight && set.addedWeightKg != null) "${formatCalisthenicsWeight(set.addedWeightKg)} x ${set.reps}" else "${displayKg(set.weightKg)}kg x ${set.reps}"
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
            onToggle = onToggle,
            todaySummary = todayHint,
            todaySetCount = workingToday.size,
        )
        AnimatedExpand(expanded) {
            HorizontalDivider(
                color = Hairline.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall),
            )
            if (suggestion != null) {
                Text(
                    suggestion.note,
                    style = LedgerFigureValue.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = Spacing.Medium),
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
            val rpeMode = LocalRpeEntryMode.current
            val workingDone = currentSessionSets.count { !it.isWarmup }
            val lastDisplayKg = lastWorkingSet?.let {
                if (isBodyweight && it.addedWeightKg != null) it.addedWeightKg else displayKg(it.weightKg)
            }
            val targetKg = suggestion?.let {
                if (isBodyweight) latestBodyWeightKg?.let { bw -> it.weightKg - bw } ?: it.weightKg else displayKg(it.weightKg)
            }
            fun formatKg(value: Double) = "%.1f".format(value).trimEnd('0').trimEnd('.')
            val shownKg = enteredWeight ?: targetKg
            val deltaKg = if (shownKg != null && lastDisplayKg != null) shownKg - lastDisplayKg else null
            val weightCaption = when {
                deltaKg == null -> null
                kotlin.math.abs(deltaKg) < 0.05 -> "same as last"
                deltaKg > 0 -> "+${formatKg(deltaKg)} vs last"
                else -> "${formatKg(deltaKg)} vs last"
            }
            val repsCaption = if (repsText.isBlank()) null else (suggestion?.let { "target ${it.reps}" }
                ?: prescription?.let { "target ${it.repMin}-${it.repMax}" })
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            ) {
                SetTimeline(done = workingDone, planned = prescription?.sets)
                var selectedField by rememberSaveable(exerciseName) { mutableStateOf(EntryField.WEIGHT) }
                val tileCaption = listOfNotNull(weightCaption, suggestion?.let { "target ${it.reps}" } ?: prescription?.let { "target ${it.repMin}-${it.repMax}" })
                    .joinToString("  \u00b7  ").ifEmpty { if (suggestion == null && prescription == null) "No history yet, enter your first set" else null }
                SetEntryTile(
                    weightUnit = if (isBodyweight) "added kg" else if (combined) "kg (both)" else "kg",
                    weightText = weightText,
                    onWeightChange = { weightText = it },
                    weightPlaceholder = if (isBodyweight) "0" else targetKg?.let(::formatKg) ?: "\u2013",
                    weightError = weightText.isNotBlank() && (enteredWeight == null || (isBodyweight && totalBodyweightLoad == null)),
                    repsText = repsText,
                    onRepsChange = { repsText = it },
                    repsPlaceholder = suggestion?.reps?.toString() ?: prescription?.repMin?.toString() ?: "\u2013",
                    placeholderIsTarget = suggestion != null || prescription != null,
                    repsError = repsText.isNotBlank() && reps == null,
                    selected = selectedField,
                    onSelect = { selectedField = it },
                    caption = tileCaption,
                    captionColor = if ((deltaKg ?: 0.0) > 0.05) PlateGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    weightKeyboardActions = rememberAdvanceKeyboardActions(onNext = { repsFocus.requestFocus() }),
                    repsKeyboardActions = rememberAdvanceKeyboardActions(onDone = { if (canLog) logSet() }),
                    weightFocus = weightFocus,
                    repsFocus = repsFocus,
                )
                StepperStrip(
                    adjusting = if (selectedField == EntryField.WEIGHT) "Adjusting kg" else "Adjusting reps",
                    step = if (selectedField == EntryField.WEIGHT) "${formatKg(stepKg)} kg" else "1 rep",
                    onStepClick = if (selectedField == EntryField.WEIGHT) {
                        { sideModes.setWeightStep(exerciseId, com.lsing.timego.domain.nextWeightStep(handStepKg)) }
                    } else null,
                    onMinus = {
                        if (selectedField == EntryField.WEIGHT) {
                            val next = ((enteredWeight ?: targetKg ?: 0.0) - stepKg).coerceAtLeast(0.0)
                            weightText = if (next == 0.0 && isBodyweight) "" else formatKg(next)
                        } else {
                            repsText = ((reps ?: suggestion?.reps ?: 1) - 1).coerceAtLeast(1).toString()
                        }
                    },
                    onPlus = {
                        if (selectedField == EntryField.WEIGHT) weightText = formatKg((enteredWeight ?: targetKg ?: 0.0) + stepKg)
                        else repsText = ((reps ?: suggestion?.reps ?: 0) + 1).toString()
                    },
                    minusDescription = if (selectedField == EntryField.WEIGHT) "Decrease weight by ${formatKg(stepKg)} kilograms" else "Decrease reps by one",
                    plusDescription = if (selectedField == EntryField.WEIGHT) "Increase weight by ${formatKg(stepKg)} kilograms" else "Increase reps by one",
                )
                // Slider mode: the RPE strip is part of the card (it slides in with it). It stays optional.
                androidx.compose.animation.AnimatedVisibility(
                    visible = rpeMode == com.lsing.timego.data.RpeEntryMode.SLIDER,
                    enter = androidx.compose.animation.expandVertically(TimeGoMotion.expandEnter) + androidx.compose.animation.fadeIn(TimeGoMotion.contentEnter),
                    exit = androidx.compose.animation.shrinkVertically(TimeGoMotion.expandExit) + androidx.compose.animation.fadeOut(TimeGoMotion.contentExit),
                ) {
                    RpeSlider(
                        value = rpe,
                        defaultValue = (prescription?.rpeLow ?: 8).coerceIn(5, 10),
                        onValueChange = { rpeText = it.toString() },
                        onClear = { rpeText = "" },
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    FilterChip(
                        selected = isWarmup,
                        onClick = { isWarmup = !isWarmup },
                        label = { Text("Warmup") },
                    )
                    if (!isBodyweight && (combined || com.lsing.timego.domain.supportsCombinedEntry(exerciseName))) {
                        FilterChip(
                            selected = combined,
                            onClick = { sideModes.toggleCombinedEntry(exerciseId) },
                            label = { Text("Both dumbbells") },
                        )
                    }
                    if (onToggleSide != null && (nextSide != null || com.lsing.timego.domain.supportsBothSides(exerciseName))) {
                        FilterChip(
                            selected = nextSide != null,
                            onClick = { sideModes.toggleBothSides(exerciseId) },
                            label = { Text("Left / right") },
                        )
                    }
                    if (nextSide != null && onToggleSide != null) {
                        FilterChip(
                            selected = true,
                            onClick = onToggleSide,
                            label = { Text("Next: ${com.lsing.timego.domain.Side.label(nextSide)}") },
                        )
                    }
                }
                asymmetry?.let {
                    Text(
                        text = "${it.message}. Start with ${com.lsing.timego.domain.Side.label(it.laggingSide)} first and match the other side.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                // The routine's own note for this exercise wins over the built-in program note.
                val noteLine = routineNote?.takeIf { it.isNotBlank() } ?: prescription?.note
                if (prescription != null || noteLine != null) {
                    Text(
                        text = listOfNotNull(prescription?.summary, noteLine).joinToString(separator = "\u000A"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onDraftNoteChange != null) {
                    var noteOpen by rememberSaveable(exerciseName) { mutableStateOf(false) }
                    if (noteOpen || draftNote.isNotBlank()) {
                        OutlinedTextField(
                            value = draftNote,
                            onValueChange = onDraftNoteChange,
                            label = { Text("Note for the next set") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        TextButton(onClick = { noteOpen = true }) { Text("Add a note to this set") }
                    }
                }
                val logInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val workingSetNumber = workingDone + 1
                Button(
                    enabled = canLog,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    interactionSource = logInteractionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .tactilePress(logInteractionSource, pressedScale = 0.97f),
                    onClick = logSet,
                ) {
                    Text(
                        when {
                            !canLog -> if (isBodyweight) "Enter reps" else "Enter weight and reps"
                            isWarmup -> "Log warmup set"
                            useTarget && suggestion != null -> "Log set $workingSetNumber \u00b7 ${formatKg(targetKg ?: 0.0)} x ${suggestion.reps}"
                            else -> "Log set $workingSetNumber"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardioLogRow(
    exerciseId: Long,
    exerciseName: String,
    category: String,
    met: Double,
    bodyWeightKg: Double?,
    delaySeconds: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    expanded: Boolean,
    pulseId: Long,
    activeTimer: ActiveTimer?,
    onToggle: () -> Unit,
    onLog: (durationMinutes: Double, distanceKm: Double?) -> Unit,
    onStartTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    todaySetCount: Int = 0,
) {
    var useTimer by rememberSaveable(exerciseName) { mutableStateOf(false) }
    var durationText by rememberSaveable(exerciseName) { mutableStateOf("") }
    var distanceText by rememberSaveable(exerciseName) { mutableStateOf("") }
    val duration = durationText.toPositiveFiniteDoubleOrNull()
    val distance = distanceText.toPositiveFiniteDoubleOrNull()
    val validDistance = distanceText.isBlank() || distance != null
    // Real cardio needs a distance (pace and speed come from it); a warm-up like skipping does not.
    val distanceRequired = category == ExerciseCategory.CARDIO.name
    val distanceOk = validDistance && (!distanceRequired || distance != null)
    val dismissKeyboardCardio = rememberDismissKeyboard()
    val minutesFocus = remember { FocusRequester() }
    val logCardio: () -> Unit = {
        if (duration != null && distanceOk) {
            onLog(duration, distance)
            durationText = ""
            distanceText = ""
            dismissKeyboardCardio()
        }
    }
    val visual = categoryVisual(category)
    val timerOwnsExercise = activeTimer?.exerciseId == exerciseId
    val timerVisible = useTimer || timerOwnsExercise

    ExerciseCard(expanded, pulseId) {
        ExerciseRowHeader(exerciseName, visual.icon, visual.accent, null, expanded, isFavorite, onToggleFavorite, onToggle)
        AnimatedExpand(expanded) {
            HorizontalDivider(
                color = Hairline.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall),
            )
            var cardioSelected by rememberSaveable(exerciseName) { mutableStateOf(EntryField.WEIGHT) }
            val kmFocus = remember { FocusRequester() }
            val cardioDetails = duration?.let { minutes ->
                val pace = distance?.let { averagePaceMinPerKm(minutes, it) }
                val calories = bodyWeightKg?.let { estimatedCalorieBurn(met, it, minutes) }
                listOfNotNull(
                    pace?.let { "Pace ${"%.1f".format(it)} min/km" },
                    calories?.let { "~${it.toInt()} kcal" },
                ).joinToString("  \u00b7  ").ifEmpty { null }
            }
            fun fmt(value: Double) = "%.1f".format(value).trimEnd('0').trimEnd('.')
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            ) {
                SetTimeline(done = todaySetCount, planned = null)
                if (!timerVisible) {
                    SetEntryTile(
                        weightUnit = "minutes",
                        weightText = durationText,
                        onWeightChange = { durationText = it },
                        weightPlaceholder = "\u2013",
                        weightError = durationText.isNotBlank() && duration == null,
                        repsText = distanceText,
                        onRepsChange = { distanceText = it },
                        repsPlaceholder = "\u2013",
                        placeholderIsTarget = false,
                        repsError = !validDistance,
                        selected = cardioSelected,
                        onSelect = { cardioSelected = it },
                        caption = cardioDetails ?: if (distanceRequired) "Add distance to get your pace" else "Distance is optional",
                        captionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        weightKeyboardActions = rememberAdvanceKeyboardActions(onNext = { kmFocus.requestFocus() }),
                        repsKeyboardActions = rememberAdvanceKeyboardActions(onDone = logCardio),
                        weightFocus = minutesFocus,
                        repsFocus = kmFocus,
                        secondUnit = "km",
                        secondDecimal = true,
                        separator = "\u00b7",
                    )
                    StepperStrip(
                        adjusting = if (cardioSelected == EntryField.WEIGHT) "Adjusting minutes" else "Adjusting km",
                        step = if (cardioSelected == EntryField.WEIGHT) "1 min" else "0.5 km",
                        onMinus = {
                            if (cardioSelected == EntryField.WEIGHT) durationText = fmt(((duration ?: 0.0) - 1.0).coerceAtLeast(0.0)).let { if (it == "0") "" else it }
                            else distanceText = fmt(((distance ?: 0.0) - 0.5).coerceAtLeast(0.0)).let { if (it == "0") "" else it }
                        },
                        onPlus = {
                            if (cardioSelected == EntryField.WEIGHT) durationText = fmt((duration ?: 0.0) + 1.0)
                            else distanceText = fmt((distance ?: 0.0) + 0.5)
                        },
                        minusDescription = if (cardioSelected == EntryField.WEIGHT) "Decrease minutes by one" else "Decrease distance by half a kilometre",
                        plusDescription = if (cardioSelected == EntryField.WEIGHT) "Increase minutes by one" else "Increase distance by half a kilometre",
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        FilterChip(selected = false, onClick = { useTimer = true }, label = { Text("Use timer") })
                    }
                    val cardioLogInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Button(
                        enabled = duration != null && distanceOk,
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        interactionSource = cardioLogInteractionSource,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).tactilePress(cardioLogInteractionSource, pressedScale = 0.97f),
                        onClick = logCardio,
                    ) {
                        Text(
                            when {
                                duration == null -> "Enter minutes"
                                !distanceOk -> if (validDistance) "Enter distance" else "Check distance"
                                else -> "Log set ${todaySetCount + 1} \u00b7 ${fmt(duration)} min${distance?.let { " \u00b7 ${fmt(it)} km" } ?: ""}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        )
                    }
                } else {
                    SingleEntryTile(
                        unit = if (distanceRequired) "km" else "km (optional)",
                        text = distanceText,
                        onTextChange = { distanceText = it },
                        placeholder = "\\u2013",
                        placeholderIsTarget = false,
                        caption = null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = rememberAdvanceKeyboardActions(onDone = {}),
                        focusRequester = kmFocus,
                        isError = !validDistance,
                        filterInput = ::filterDecimal,
                    )
                TimerControls(
                    timerKey = exerciseId,
                    activeTimerKey = activeTimer?.exerciseId,
                    startedAtEpochMillis = activeTimer?.startedAtEpochMillis,
                    delaySeconds = delaySeconds,
                    formatElapsed = ::formatElapsedSeconds,
                    onEnterManually = {
                        useTimer = false
                        onCancelTimer()
                    },
                    onStart = onStartTimer,
                    onCancel = {
                        useTimer = false
                        onCancelTimer()
                    },
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
}

private fun formatElapsedSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun HoldLogRow(
    exerciseId: Long,
    exerciseName: String,
    category: String,
    suggestion: HoldSuggestion?,
    delaySeconds: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    expanded: Boolean,
    pulseId: Long,
    activeTimer: ActiveTimer?,
    onToggle: () -> Unit,
    onLog: (durationSeconds: Int, targetDurationSeconds: Int, isWarmup: Boolean, targetProvenance: TargetProvenance) -> Unit,
    onStartTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    todaySetCount: Int = 0,
) {
    val sideModes = LocalSideModes.current
    val bothSides = exerciseId in sideModes.bothSidesIds
    // 0 = first side, 1 = second side; justStopped lets onCancel (fired right after onStop) know the timer
    // ended by logging rather than by the user cancelling.
    var sideIndex by rememberSaveable(exerciseName) { mutableStateOf(0) }
    var justStopped by remember { mutableStateOf(false) }
    var isWarmup by rememberSaveable(exerciseName) { mutableStateOf(false) }
    var useTimer by rememberSaveable(exerciseName) { mutableStateOf(true) }
    var manualDurationText by rememberSaveable(exerciseName) { mutableStateOf("") }
    val visual = categoryVisual(category)
    val timerOwnsExercise = activeTimer?.exerciseId == exerciseId
    val timerVisible = useTimer || timerOwnsExercise

    ExerciseCard(expanded, pulseId) {
        ExerciseRowHeader(
            exerciseName,
            visual.icon,
            visual.accent,
            suggestion?.let { "${it.targetDurationSeconds}s" },
            expanded,
            isFavorite,
            onToggleFavorite,
            onToggle = onToggle,
        )
        AnimatedExpand(expanded) {
            HorizontalDivider(
                color = Hairline.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall),
            )
            if (suggestion != null) {
                Text(
                    suggestion.note,
                    style = LedgerFigureValue.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = Spacing.Medium),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Spacing.Medium),
            ) {
                FilterChip(selected = isWarmup, onClick = { isWarmup = !isWarmup }, label = { Text("Warmup") })
                val holdExtras = LocalLogExtras.current
                val onHoldModifierChange = holdExtras.onHoldModifierChange
                if (onHoldModifierChange != null) {
                    val currentModifier = holdExtras.holdModifiers[exerciseId]
                    var modifierMenuOpen by remember { mutableStateOf(false) }
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Box {
                        FilterChip(
                            selected = currentModifier != null,
                            onClick = { modifierMenuOpen = true },
                            label = { Text(currentModifier?.label ?: "+ Modifier") },
                        )
                        DropdownMenu(expanded = modifierMenuOpen, onDismissRequest = { modifierMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("None (clean hold)") },
                                onClick = { onHoldModifierChange(exerciseId, null); modifierMenuOpen = false },
                            )
                            com.lsing.timego.domain.LEVERAGE_MODIFIER_PRESETS.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.label) },
                                    onClick = { onHoldModifierChange(exerciseId, preset); modifierMenuOpen = false },
                                )
                            }
                        }
                    }
                }
                if (bothSides || com.lsing.timego.domain.supportsBothSides(exerciseName)) {
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    FilterChip(
                        selected = bothSides,
                        onClick = { sideModes.toggleBothSides(exerciseId); sideIndex = 0 },
                        label = { Text("Both sides") },
                    )
                }
            }
            if (bothSides && timerVisible) {
                Text(
                    if (sideIndex == 0) "Side 1 of 2: timer covers one side" else "Side 2 of 2: switch sides now",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall),
                )
            }
            if (!timerVisible) {
                val manualDuration = manualDurationText.toPositiveIntOrNull()
                val dismissKeyboardHold = rememberDismissKeyboard()
                val holdTarget = suggestion?.targetDurationSeconds
                val useHoldTarget = holdTarget != null && manualDurationText.isBlank()
                val canLogHold = useHoldTarget || manualDuration != null
                val holdFocus = remember { FocusRequester() }
                val logHold: () -> Unit = {
                    val seconds = if (useHoldTarget) holdTarget else manualDuration
                    if (seconds != null) {
                        onLog(seconds, holdTarget ?: seconds, isWarmup, targetProvenanceFor(suggestion != null))
                        manualDurationText = ""
                        isWarmup = false
                        dismissKeyboardHold()
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    SetTimeline(done = todaySetCount, planned = null)
                    SingleEntryTile(
                        unit = "seconds",
                        text = manualDurationText,
                        onTextChange = { manualDurationText = it },
                        placeholder = holdTarget?.toString() ?: "\u2013",
                        placeholderIsTarget = holdTarget != null,
                        caption = holdTarget?.let { "target ${it}s" } ?: "No history yet, enter your first hold",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = rememberAdvanceKeyboardActions(onDone = logHold),
                        focusRequester = holdFocus,
                        isError = manualDurationText.isNotBlank() && manualDuration == null,
                        filterInput = { filterWholeNumber(it) },
                    )
                    StepperStrip(
                        adjusting = "Adjusting seconds",
                        step = "5 s",
                        onMinus = { manualDurationText = ((manualDuration ?: holdTarget ?: 0) - 5).coerceAtLeast(5).toString() },
                        onPlus = { manualDurationText = ((manualDuration ?: holdTarget ?: 0) + 5).toString() },
                        minusDescription = "Decrease by five seconds",
                        plusDescription = "Increase by five seconds",
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        FilterChip(selected = false, onClick = { useTimer = true }, label = { Text("Use timer instead") })
                    }
                    val holdLogInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Button(
                        enabled = canLogHold,
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        interactionSource = holdLogInteractionSource,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).tactilePress(holdLogInteractionSource, pressedScale = 0.97f),
                        onClick = logHold,
                    ) {
                        val seconds = if (useHoldTarget) holdTarget else manualDuration
                        Text(
                            if (seconds != null) "Log set ${todaySetCount + 1} \u00b7 ${seconds}s" else "Enter seconds",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        )
                    }
                }
            } else {
                TimerControls(
                    timerKey = exerciseId,
                    activeTimerKey = activeTimer?.exerciseId,
                    startedAtEpochMillis = activeTimer?.startedAtEpochMillis,
                    delaySeconds = delaySeconds,
                    formatElapsed = { "${it}s" },
                    onEnterManually = {
                        useTimer = false
                        onCancelTimer()
                    },
                    onStart = onStartTimer,
                    onCancel = {
                        if (justStopped) {
                            // Stop & Log finished a side: start the other side (its start delay is the switch
                            // time), or end the exercise if both are done.
                            justStopped = false
                            if (bothSides && sideIndex == 0) {
                                sideIndex = 1
                                onStartTimer()
                            } else {
                                sideIndex = 0
                                onCancelTimer()
                            }
                        } else {
                            useTimer = false
                            sideIndex = 0
                            onCancelTimer()
                        }
                    },
                    onStop = { seconds ->
                        onLog(seconds, suggestion?.targetDurationSeconds ?: seconds, isWarmup, targetProvenanceFor(suggestion != null))
                        justStopped = true
                        if (!bothSides || sideIndex == 1) isWarmup = false
                    },
                )
            }
        }
    }
}
