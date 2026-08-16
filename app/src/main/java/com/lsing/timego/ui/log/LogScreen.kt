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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.lsing.timego.domain.HoldSuggestion
import com.lsing.timego.domain.HoldTimerPhase
import com.lsing.timego.domain.MET_CARDIO
import com.lsing.timego.domain.MET_WARMUP
import com.lsing.timego.domain.averagePaceMinPerKm
import com.lsing.timego.domain.estimatedCalorieBurn
import com.lsing.timego.domain.formatCalisthenicsWeight
import com.lsing.timego.domain.tick
import com.lsing.timego.ui.common.AnimatedExpand
import com.lsing.timego.ui.common.ExerciseSections
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.common.StatTile
import com.lsing.timego.ui.common.TrainingPulse
import com.lsing.timego.ui.common.WorkoutHistoryDialog
import com.lsing.timego.ui.common.categoryVisual
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.theme.LedgerFigureValue
import kotlinx.coroutines.delay
import com.lsing.timego.ui.theme.Spacing

@Composable
fun LogScreen(viewModel: LogViewModel = viewModel()) {
    val sessionState by viewModel.sessionState.collectAsState()
    val landingSummary by viewModel.landingSummary.collectAsState()
    val routines by viewModel.routines.collectAsState()

    when (val state = sessionState) {
        is SessionUiState.Loading -> { /* nothing to render yet -- first frame only, resolves on the next recomposition */ }
        is SessionUiState.NoActiveSession -> LogLandingContent(
            summary = landingSummary,
            routines = routines,
            isSessionActive = false,
            onStartOrContinue = viewModel::startSession,
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
                )
            } else {
                LoggingContent(
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
) {
    var showLastSessionDetail by remember { mutableStateOf(false) }

    if (showLastSessionDetail && summary.lastSession != null) {
        WorkoutHistoryDialog(
            title = "Last session",
            entries = summary.lastSession.detail,
            onDismiss = { showLastSessionDetail = false },
        )
    }

    Column(modifier = Modifier.padding(Spacing.Large)) {
        SectionHeader("Last session", topPadding = Spacing.ExtraSmall)
        if (summary.lastSession == null) {
            Text("No sessions logged yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Column(modifier = Modifier.fillMaxWidth().clickable { showLastSessionDetail = true }) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatTile("Sets", "${summary.lastSession.sets}", modifier = Modifier.weight(1f))
                    StatTile("Duration", "${summary.lastSession.durationMinutes} min", modifier = Modifier.weight(1f))
                }
                Text(
                    "Trained",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.Small),
                )
                if (summary.lastSession.muscleGroups.isEmpty()) {
                    Text(
                        "--",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                } else {
                    FlowRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        summary.lastSession.muscleGroups.sorted().forEach { group ->
                            AssistChip(
                                onClick = {},
                                label = { Text(formatEnumLabel(group)) },
                                modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        SectionHeader("Recommended")
        if (summary.recommendedMuscleGroups.isEmpty()) {
            Text("Everything's been trained recently -- nice balance.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                summary.recommendedMuscleGroups.joinToString(", ") { formatEnumLabel(it) },
                style = LedgerFigureValue,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (isSessionActive) {
            SectionHeader("Session in progress")
            Button(onClick = { onStartOrContinue(null) }) {
                Text("Continue Session")
            }
        } else {
            SectionHeader("Start a session")
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
}

@Composable
private fun LoggingContent(viewModel: LogViewModel, onEndSession: () -> Unit, onBackToLanding: () -> Unit) {
    val exercises by viewModel.displayedExercises.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val holdSuggestions by viewModel.holdSuggestions.collectAsState()
    val routines by viewModel.routines.collectAsState()
    val selectedRoutineId by viewModel.selectedRoutineId.collectAsState()
    val latestBodyWeightKg by viewModel.latestBodyWeightKg.collectAsState()
    val holdDelaySeconds by viewModel.holdDelaySeconds.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onAdd = viewModel::addCustomExercise,
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
                            Button(onClick = onEndSession) { Text("End Session") }
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
                ExerciseSections(exercises = exercises) { exercise ->
                    when (exercise.loggingType) {
                        LoggingType.HOLD.name -> HoldLogRow(
                            exerciseName = exercise.name,
                            category = exercise.category,
                            suggestion = holdSuggestions[exercise.id],
                            delaySeconds = holdDelaySeconds,
                            onLog = { duration, target, isWarmup -> viewModel.logHoldSet(exercise.id, duration, target, isWarmup) },
                        )
                        LoggingType.DURATION_DISTANCE.name -> CardioLogRow(
                            exerciseName = exercise.name,
                            category = exercise.category,
                            met = if (exercise.category == ExerciseCategory.CARDIO.name) MET_CARDIO else MET_WARMUP,
                            bodyWeightKg = latestBodyWeightKg,
                            onLog = { duration, distance -> viewModel.logCardioSet(exercise.id, duration, distance) },
                        )
                        else -> StrengthLogRow(
                            exerciseName = exercise.name,
                            category = exercise.category,
                            suggestion = suggestions[exercise.id],
                            isBodyweight = exercise.category == ExerciseCategory.CALISTHENICS.name,
                            latestBodyWeightKg = latestBodyWeightKg,
                            onLog = { weight, reps, target, isWarmup, addedWeightKg ->
                                viewModel.logSet(exercise.id, weight, reps, target, isWarmup, addedWeightKg)
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
private fun ExerciseCard(expanded: Boolean, content: @Composable () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    TrainingPulse(
        active = expanded,
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
    isBodyweight: Boolean,
    latestBodyWeightKg: Double?,
    onLog: (weightKg: Double, reps: Int, targetReps: Int, isWarmup: Boolean, addedWeightKg: Double?) -> Unit,
) {
    var expanded by remember(exerciseName) { mutableStateOf(false) }
    // Bodyweight exercises (Pull-Up, Push-Up, Dip, ...) ask for just the added weight k (e.g. a
    // weighted vest) -- blank/0 means bodyweight-only, not "no weight logged." weightKg (the
    // absolute bodyweight+k total 1RM/PR/suggester math needs) is computed at log time, not typed.
    var weightText by remember(exerciseName) { mutableStateOf("") }
    var repsText by remember(exerciseName) { mutableStateOf("") }
    var isWarmup by remember(exerciseName) { mutableStateOf(false) }
    val visual = categoryVisual(ExerciseCategory.valueOf(category))
    val suggestionHint = suggestion?.let {
        if (isBodyweight) {
            val addedK = latestBodyWeightKg?.let { bw -> it.weightKg - bw } ?: it.weightKg
            "${formatCalisthenicsWeight(addedK)} x ${it.reps}"
        } else {
            "${it.weightKg}kg x ${it.reps}"
        }
    }

    ExerciseCard(expanded) {
        ExerciseRowHeader(
            exerciseName,
            visual.icon,
            visual.accent,
            suggestionHint,
            expanded,
        ) { expanded = !expanded }
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                    modifier = Modifier.weight(1f).padding(end = Spacing.Small),
                )
                Button(onClick = {
                    val reps = repsText.toIntOrNull()
                    if (isBodyweight) {
                        val addedWeight = weightText.toDoubleOrNull() ?: 0.0
                        if (reps != null) {
                            val totalWeight = (latestBodyWeightKg ?: 0.0) + addedWeight
                            onLog(totalWeight, reps, suggestion?.reps ?: reps, isWarmup, addedWeight)
                            weightText = ""
                            repsText = ""
                            isWarmup = false
                        }
                    } else {
                        val weight = weightText.toDoubleOrNull()
                        if (weight != null && reps != null) {
                            onLog(weight, reps, suggestion?.reps ?: reps, isWarmup, null)
                            weightText = ""
                            repsText = ""
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

@Composable
private fun CardioLogRow(
    exerciseName: String,
    category: String,
    met: Double,
    bodyWeightKg: Double?,
    onLog: (durationMinutes: Double, distanceKm: Double?) -> Unit,
) {
    var expanded by remember(exerciseName) { mutableStateOf(false) }
    var durationText by remember(exerciseName) { mutableStateOf("") }
    var distanceText by remember(exerciseName) { mutableStateOf("") }
    val duration = durationText.toDoubleOrNull()
    val distance = distanceText.toDoubleOrNull()
    val visual = categoryVisual(ExerciseCategory.valueOf(category))

    ExerciseCard(expanded) {
        ExerciseRowHeader(exerciseName, visual.icon, visual.accent, null, expanded) { expanded = !expanded }
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
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = { distanceText = it },
                    label = { Text("km (optional)") },
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
        }
    }
}

@Composable
private fun HoldLogRow(
    exerciseName: String,
    category: String,
    suggestion: HoldSuggestion?,
    delaySeconds: Int,
    onLog: (durationSeconds: Int, targetDurationSeconds: Int, isWarmup: Boolean) -> Unit,
) {
    var expanded by remember(exerciseName) { mutableStateOf(false) }
    var isWarmup by remember(exerciseName) { mutableStateOf(false) }
    var phase by remember(exerciseName) { mutableStateOf<HoldTimerPhase?>(null) }
    val visual = categoryVisual(ExerciseCategory.valueOf(category))

    LaunchedEffect(phase) {
        val current = phase ?: return@LaunchedEffect
        delay(1000)
        phase = current.tick()
    }

    ExerciseCard(expanded) {
        ExerciseRowHeader(
            exerciseName,
            visual.icon,
            visual.accent,
            suggestion?.let { "${it.targetDurationSeconds}s" },
            expanded,
        ) { expanded = !expanded }
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
            when (val currentPhase = phase) {
                null -> Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = {
                        phase = if (delaySeconds <= 0) HoldTimerPhase.Running(0) else HoldTimerPhase.CountingDown(delaySeconds)
                    }) {
                        Text("Start")
                    }
                }
                is HoldTimerPhase.CountingDown -> Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Starting in ${currentPhase.secondsRemaining}s...",
                        style = LedgerFigureValue,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { phase = null }) {
                        Text("Cancel")
                    }
                }
                is HoldTimerPhase.Running -> Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${currentPhase.elapsedSeconds}s",
                        style = LedgerFigureValue,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = {
                        val seconds = currentPhase.elapsedSeconds
                        onLog(seconds, suggestion?.targetDurationSeconds ?: seconds, isWarmup)
                        phase = null
                        isWarmup = false
                    }) {
                        Text("Stop & Log")
                    }
                }
            }
        }
    }
}
