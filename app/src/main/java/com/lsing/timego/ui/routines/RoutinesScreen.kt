package com.lsing.timego.ui.routines

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.lsing.timego.ui.common.ExpandableCard
import com.lsing.timego.ui.common.ExpandableHeader
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Settings
import com.lsing.timego.ui.common.TimeGoDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lsing.timego.data.TrainingLean
import com.lsing.timego.data.TIMEGO_BACKUP_MIME_TYPE
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.guidance.CatalogueRepository
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.common.SurfaceCard
import com.lsing.timego.ui.common.RoutineCardSkeleton
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.theme.LedgerFigureEmphasis
import com.lsing.timego.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.lsing.timego.ui.onboarding.OnboardingScreen
import com.lsing.timego.ui.onboarding.OnboardingViewModel
import androidx.compose.ui.platform.LocalContext
import com.lsing.timego.account.AccountViewModel
import com.lsing.timego.account.FirebaseAuthRepository
import com.lsing.timego.ui.account.AccountScreen
import com.lsing.timego.account.AuthState
import com.lsing.timego.report.ReportViewModel
import com.lsing.timego.ui.report.ReportSettingsScreen
import com.lsing.timego.sync.CloudBackupViewModel
import com.lsing.timego.ui.account.CloudBackupScreen
import com.lsing.timego.ui.exercise.ExerciseDetailSheet
import com.lsing.timego.ui.exercise.buildExerciseDetail

private val SESSION_HISTORY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun RoutinesScreen(viewModel: RoutinesViewModel = viewModel()) {
    val isHydrated by viewModel.isHydrated.collectAsStateWithLifecycle()
    val routines = viewModel.routines.collectAsStateWithLifecycle().value
    val routineExercisesById by viewModel.routineExercisesById.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val untrainedGroups by viewModel.untrainedGroups.collectAsStateWithLifecycle()
    val holdDelaySeconds by viewModel.holdDelaySeconds.collectAsStateWithLifecycle()
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsStateWithLifecycle()
    val trainingLean by viewModel.trainingLean.collectAsStateWithLifecycle()
    val sessionHistory by viewModel.sessionHistory.collectAsStateWithLifecycle()
    val backupResult by viewModel.backupResult.collectAsStateWithLifecycle()
    var showRoutineForm by remember { mutableStateOf(false) }
    var editingRoutine by remember { mutableStateOf<com.lsing.timego.data.Routine?>(null) }
    var expandedProgramIds by remember { mutableStateOf(setOf<String>()) }
    var showPreinstalled by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = showPreinstalled) { showPreinstalled = false }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSessionHistory by remember { mutableStateOf(false) }
    var pendingDeleteSessionId by remember { mutableStateOf<Long?>(null) }
    var showTrainingProfile by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    var showReports by remember { mutableStateOf(false) }
    var showCloudBackup by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val accountRepository = remember(context) { FirebaseAuthRepository.createIfConfigured(context) }
    val accountViewModel: AccountViewModel = viewModel(key = "account", factory = AccountViewModel.factory(accountRepository))
    val cloudBackupViewModel: CloudBackupViewModel = viewModel(key = "cloud_backup")
    val cloudConsent by cloudBackupViewModel.consent.collectAsStateWithLifecycle()
    val accountState by accountViewModel.uiState.collectAsStateWithLifecycle()
    val catalogueRepository = remember(context) { CatalogueRepository(TimeGoDatabase.getInstance(context)) }
    val guidanceByKey by catalogueRepository.guidanceByKey.collectAsStateWithLifecycle(initialValue = emptyMap())
    var detailExercise by remember { mutableStateOf<Exercise?>(null) }

    if (showCloudBackup) {
        CloudBackupScreen(
            viewModel = cloudBackupViewModel,
            emailVerified = (accountState.authState as? AuthState.SignedIn)?.verified == true,
            onBack = { showCloudBackup = false },
        )
        return
    }

    if (showReports) {
        val reportViewModel: ReportViewModel = viewModel(key = "reports")
        ReportSettingsScreen(
            viewModel = reportViewModel,
            emailVerified = (accountState.authState as? AuthState.SignedIn)?.verified == true,
            cloudBackupEnabled = cloudConsent.enabled,
            onBack = { showReports = false },
        )
        return
    }

    if (showAccount) {
        AccountScreen(accountViewModel, onOpenCloudBackup = { showCloudBackup = true }, onOpenReports = { showReports = true }, onDeleteLocalData = { viewModel.clearLocalWorkoutData() }, onBack = { showAccount = false })
        return
    }

    if (showTrainingProfile) {
        val profileViewModel: OnboardingViewModel = viewModel(key = "settings_onboarding")
        OnboardingScreen(
            viewModel = profileViewModel,
            onFinished = { showTrainingProfile = false },
        )
        return
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TIMEGO_BACKUP_MIME_TYPE)) { uri ->
        uri?.let(viewModel::exportBackup)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::restoreBackup)
    }

    if (!isHydrated) {
        LazyColumn(modifier = Modifier.padding(Spacing.Large)) {
            item {
                Text(
                    "Programs & Routines",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = Spacing.ExtraSmall, bottom = Spacing.Small),
                )
                Text(
                    "Loading your training plan…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RoutineCardSkeleton()
                RoutineCardSkeleton()
            }
        }
        return
    }

    backupResult?.let { result ->
        TimeGoDialog(
            onDismissRequest = viewModel::clearBackupResult,
            eyebrow = "DATABASE",
            title = if (result.isError) "Backup problem" else "Backup complete",
            confirmButton = {
                val dismiss = com.lsing.timego.ui.common.LocalTimeGoDialogDismiss.current
                TextButton(onClick = {
                    dismiss()
                }) { Text("OK") }
            },
        ) {
            Text(result.message, style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (showRoutineForm) {
        RoutineFormDialog(
            exercises = exercises,
            onDismiss = { showRoutineForm = false },
            onCreate = viewModel::createRoutine,
        )
    }

    editingRoutine?.let { routine ->
        RoutineFormDialog(
            exercises = exercises,
            onDismiss = { editingRoutine = null },
            onCreate = { name, exerciseIds, days -> viewModel.updateRoutine(routine.id, name, exerciseIds, days) },
            initial = RoutineFormInitial(
                name = routine.name,
                exerciseIds = routineExercisesById[routine.id].orEmpty().map { it.id },
                days = routine.daysOfWeek.toSet(),
                nameEditable = routine.programId == null || routine.programId == com.lsing.timego.data.RIR_PROGRAM_ID,
            ),
        )
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            holdDelaySeconds = holdDelaySeconds,
            onSetHoldDelaySeconds = viewModel::setHoldDelaySeconds,
            restTimerSeconds = restTimerSeconds,
            onSetRestTimerSeconds = viewModel::setRestTimerSeconds,
            trainingLean = trainingLean,
            onSetTrainingLean = viewModel::setTrainingLean,
            onExportBackup = {
                exportLauncher.launch("timego-backup-${LocalDate.now()}.db")
            },
            onRestoreBackup = {
                restoreLauncher.launch(arrayOf("*/*"))
            },
            onEditTrainingProfile = {
                showTrainingProfile = true
            },
            onOpenAccount = { showAccount = true },
            sessionHistoryCount = sessionHistory.size,
            onViewSessionHistory = {
                showSessionHistory = true
            },
            onDismiss = { showSettingsSheet = false },
        )
    }

    if (showSessionHistory) {
        SessionHistoryDialog(
            sessions = sessionHistory,
            onDismiss = { showSessionHistory = false },
            onDeleteRequest = { pendingDeleteSessionId = it },
        )
    }

    pendingDeleteSessionId?.let { sessionId ->
        TimeGoDialog(
            onDismissRequest = { pendingDeleteSessionId = null },
            eyebrow = "HISTORY",
            title = "Delete this session?",
            confirmButton = {
                val dismiss = com.lsing.timego.ui.common.LocalTimeGoDialogDismiss.current
                TextButton(onClick = {
                    viewModel.deleteSession(sessionId)
                    dismiss()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                val dismiss = com.lsing.timego.ui.common.LocalTimeGoDialogDismiss.current
                TextButton(onClick = dismiss) {
                    Text("Cancel")
                }
            },
        ) {
            Text(
                "This permanently removes the session and every set logged in it. This can't be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    // Same horizontal slide the Log pages use, so drilling into the preinstalled list reads as going deeper.
    androidx.compose.animation.AnimatedContent(
        targetState = showPreinstalled,
        transitionSpec = {
            val deeper = targetState
            val enter = androidx.compose.animation.slideInHorizontally(com.lsing.timego.ui.theme.TimeGoMotion.navigationInOffset) { width -> if (deeper) width / 4 else -width / 4 } +
                androidx.compose.animation.fadeIn(com.lsing.timego.ui.theme.TimeGoMotion.contentEnter)
            val exit = androidx.compose.animation.slideOutHorizontally(com.lsing.timego.ui.theme.TimeGoMotion.navigationOutOffset) { width -> if (deeper) -width / 4 else width / 4 } +
                androidx.compose.animation.fadeOut(com.lsing.timego.ui.theme.TimeGoMotion.contentExit)
            enter togetherWith exit
        },
        label = "programsPageTransition",
    ) { onPreinstalledPage ->
        LazyColumn(modifier = Modifier.padding(Spacing.Large)) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.ExtraSmall, bottom = Spacing.Small),
                ) {
                    Text(
                        "Programs & Routines",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings & Preferences",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "Prepare the next session before you step in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.Small),
                )
                if (onPreinstalledPage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.ExtraSmall),
                    ) {
                        IconButton(onClick = { showPreinstalled = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Programs & Routines")
                        }
                        Text("Preinstalled programs", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    SectionHeader(title = "My programs", topPadding = Spacing.ExtraSmall)
                }
            }
            val programCardCallbacks = ProgramCardCallbacks(
                onToggle = { expandedProgramIds = expandedProgramIds.toggle(it) },
                onOpenDetails = { detailExercise = it },
                onEdit = { editingRoutine = it },
            )
            if (onPreinstalledPage) {
                com.lsing.timego.data.PROGRAM_DISPLAY_NAMES.filterKeys { it != com.lsing.timego.data.RIR_PROGRAM_ID }.forEach { (programId, name) ->
                    item(key = "program-$programId") {
                        ProgramCard(
                            title = name,
                            programRoutines = routines.filter { it.programId == programId },
                            expanded = programId in expandedProgramIds,
                            programKey = programId,
                            routineExercisesById = routineExercisesById,
                            callbacks = programCardCallbacks,
                            onDelete = null,
                            onReset = { viewModel.resetSeededRoutine(it.id) },
                        )
                    }
                }
            } else {
                item(key = "program-${com.lsing.timego.data.RIR_PROGRAM_ID}") {
                    ProgramCard(
                        title = "Upper/Lower AB (RIR)",
                        programRoutines = routines.filter { it.programId == com.lsing.timego.data.RIR_PROGRAM_ID },
                        expanded = com.lsing.timego.data.RIR_PROGRAM_ID in expandedProgramIds,
                        programKey = com.lsing.timego.data.RIR_PROGRAM_ID,
                        routineExercisesById = routineExercisesById,
                        callbacks = programCardCallbacks,
                        onDelete = { viewModel.deleteRoutine(it.id) },
                        onReset = null,
                    )
                }
                item(key = "program-custom") {
                    ProgramCard(
                        title = "Custom routines",
                        programRoutines = routines.filter { it.programId == null },
                        expanded = "custom" in expandedProgramIds,
                        programKey = "custom",
                        routineExercisesById = routineExercisesById,
                        callbacks = programCardCallbacks,
                        onDelete = { viewModel.deleteRoutine(it.id) },
                        onReset = null,
                        onNewRoutine = { showRoutineForm = true },
                    )
                }
                item(key = "preinstalledEntry") {
                    SurfaceCard(modifier = Modifier.fillMaxWidth().padding(top = Spacing.Large, bottom = 4.dp), cornerRadius = 10.dp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPreinstalled = true }
                                .padding(Spacing.Medium),
                        ) {
                            Text("Preinstalled programs", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(
                                "${com.lsing.timego.data.PROGRAM_DISPLAY_NAMES.size - 1} programs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = Spacing.Small),
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
            }
            if (!onPreinstalledPage && untrainedGroups.isNotEmpty()) {
                item {
                    Text(
                        "Not trained in a while",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.Large, bottom = Spacing.ExtraSmall),
                    )
                    FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Medium)) {
                        untrainedGroups.take(6).forEach { group ->
                            AssistChip(
                                onClick = {},
                                label = { Text(formatEnumLabel(group)) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.error),
                                border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.padding(end = Spacing.ExtraSmall, bottom = Spacing.ExtraSmall),
                            )
                        }
                    }
                    if (untrainedGroups.size > 6) {
                        Text(
                            "Showing the six longest-neglected groups.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Spacing.Medium),
                        )
                    }
                }
            }
        }
    }
    detailExercise?.let { exercise ->
        val model = buildExerciseDetail(exercise, exercise.catalogueKey?.let(guidanceByKey::get))
        val easierExercise = model.easierVariationKey?.let { key -> exercises.firstOrNull { it.catalogueKey == key } }
        ExerciseDetailSheet(
            model = model,
            easierName = easierExercise?.name,
            onShowEasier = { easierExercise?.let { detailExercise = it } },
            onDismiss = { detailExercise = null },
        )
    }
}

@Composable
private fun RoutineSteps(exercises: List<Exercise>, onOpenDetails: (Exercise) -> Unit) {
    Column(modifier = Modifier.padding(Spacing.Medium, 0.dp, Spacing.Medium, Spacing.Medium)) {
        Text(
            "Workout order",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.ExtraSmall),
        )
        exercises.forEachIndexed { index, exercise ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onOpenDetails(exercise) }
                    .padding(vertical = Spacing.ExtraSmall),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = Spacing.Small),
                )
            }
        }
    }
}

/** Popup listing every closed session with a per-row delete affordance -- kept out of the main
 *  Routines list (rather than inline) since it's a maintenance action, not something to see by
 *  default every time the page opens. [onDeleteRequest] hands the tapped session's id back to the
 *  caller rather than deleting directly, so the confirmation dialog stays owned by [RoutinesScreen]
 *  and can layer on top of this one. */
@Composable
private fun SessionHistoryDialog(
    sessions: List<SessionHistoryEntry>,
    onDismiss: () -> Unit,
    onDeleteRequest: (Long) -> Unit,
) {
    TimeGoDialog(
        onDismissRequest = onDismiss,
        eyebrow = "ROUTINES",
        title = "Session history",
        subtitle = if (sessions.isNotEmpty()) "${sessions.size} session${if (sessions.size == 1) "" else "s"} recorded" else null,
        confirmButton = {
            val dismiss = com.lsing.timego.ui.common.LocalTimeGoDialogDismiss.current
            TextButton(onClick = dismiss) { Text("Close") }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (sessions.isEmpty()) {
                Text(
                    "No sessions recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                sessions.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f).padding(vertical = Spacing.ExtraSmall)) {
                            Text(entry.date.format(SESSION_HISTORY_DATE_FORMATTER), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${entry.setCount} set${if (entry.setCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDeleteRequest(entry.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete session on ${entry.date.format(SESSION_HISTORY_DATE_FORMATTER)}", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

private fun Set<String>.toggle(key: String) = if (key in this) this - key else this + key

private class ProgramCardCallbacks(
    val onToggle: (String) -> Unit,
    val onOpenDetails: (Exercise) -> Unit,
    val onEdit: (com.lsing.timego.data.Routine) -> Unit,
)

/** One program as an [ExpandableCard] (same look as the Log exercise rows): the header shows the name and
 *  routine count, the body lists its routines with edit plus delete (own routines) or reset (preinstalled). */
@Composable
private fun ProgramCard(
    title: String,
    programRoutines: List<com.lsing.timego.data.Routine>,
    expanded: Boolean,
    programKey: String,
    routineExercisesById: Map<Long, List<Exercise>>,
    callbacks: ProgramCardCallbacks,
    onDelete: ((com.lsing.timego.data.Routine) -> Unit)?,
    onReset: ((com.lsing.timego.data.Routine) -> Unit)?,
    onNewRoutine: (() -> Unit)? = null,
) {
    ExpandableCard(
        expanded = expanded,
        header = {
            ExpandableHeader(
                title = title,
                expanded = expanded,
                onToggle = { callbacks.onToggle(programKey) },
                summary = "${programRoutines.size} routine${if (programRoutines.size == 1) "" else "s"}",
            )
        },
    ) {
        onNewRoutine?.let {
            Button(onClick = it, modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall)) { Text("+ New routine") }
        }
        if (programRoutines.isEmpty()) {
            Text(
                "No routines here yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Spacing.Medium),
            )
        }
        programRoutines.forEach { routine ->
            RoutineBody(
                routine = routine,
                steps = routineExercisesById[routine.id].orEmpty(),
                onOpenDetails = callbacks.onOpenDetails,
                onEdit = { callbacks.onEdit(routine) },
                onDelete = onDelete?.let { { it(routine) } },
                onReset = onReset?.let { { it(routine) } },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineBody(
    routine: com.lsing.timego.data.Routine,
    steps: List<Exercise>,
    onOpenDetails: (Exercise) -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?,
    onReset: (() -> Unit)?,
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                routine.name,
                style = LedgerFigureEmphasis,
                modifier = Modifier.weight(1f).padding(Spacing.Medium, Spacing.Medium, Spacing.Medium, Spacing.ExtraSmall),
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit ${routine.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            onReset?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reset ${routine.name} to default", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            onDelete?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${routine.name}", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        FlowRow(modifier = Modifier.padding(Spacing.Small, 0.dp, Spacing.Small, Spacing.Small)) {
            when {
                routine.daysOfWeek.isEmpty() -> AssistChip(
                    onClick = {},
                    leadingIcon = {
                        Icon(
                            Icons.Filled.AllInclusive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    label = { Text("Flexible · Any day") },
                    colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.primary),
                    border = AssistChipDefaults.assistChipBorder(
                        enabled = true,
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    ),
                    modifier = Modifier.padding(Spacing.ExtraSmall),
                )
                routine.daysOfWeek.size >= 7 -> AssistChip(
                    onClick = {},
                    label = { Text("Every day (Mon–Sun)") },
                    modifier = Modifier.padding(Spacing.ExtraSmall),
                )
                else -> routine.daysOfWeek.forEach { day ->
                    AssistChip(
                        onClick = {},
                        label = { Text(day.take(3).lowercase().replaceFirstChar(Char::uppercase)) },
                        modifier = Modifier.padding(Spacing.ExtraSmall),
                    )
                }
            }
        }
        if (steps.isNotEmpty()) RoutineSteps(steps, onOpenDetails = onOpenDetails)
    }
}
