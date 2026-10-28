package com.lsing.timego.ui.routines

import com.lsing.timego.ui.common.TimeGoBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import com.lsing.timego.ui.theme.RaisedFill
import com.lsing.timego.ui.theme.EyebrowStyle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import com.lsing.timego.profile.TrainingProfile
import com.lsing.timego.ui.common.formatEnumLabel
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lsing.timego.data.TrainingLean
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.theme.LedgerFigureEmphasis
import com.lsing.timego.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    holdDelaySeconds: Int,
    onSetHoldDelaySeconds: (Int) -> Unit,
    restTimerSeconds: Int,
    onSetRestTimerSeconds: (Int) -> Unit,
    restAdaptive: Boolean,
    onSetRestAdaptive: (Boolean) -> Unit,
    themeMode: com.lsing.timego.data.ThemeMode,
    onSetThemeMode: (com.lsing.timego.data.ThemeMode) -> Unit,
    trainingLean: TrainingLean,
    onSetTrainingLean: (TrainingLean) -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    trainingProfile: TrainingProfile,
    onEditTrainingProfile: () -> Unit,
    onOpenAccount: () -> Unit,
    sessionHistoryCount: Int,
    onViewSessionHistory: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var actionInFlight by remember { mutableStateOf(false) }

    fun dismissThen(action: () -> Unit) {
        if (actionInFlight) return
        actionInFlight = true
        scope.launch {
            sheetState.hide()
            action()
            onDismiss()
        }
    }

    TimeGoBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Spacing.Large)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.ExtraLarge + 24.dp),
        ) {
            Text(
                "SETTINGS",
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 2.dp),
            )
            Text(
                "Preferences & Data",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Spacing.Small),
            )

            // Workout: the controls used mid-session, ordered by how often they change
            SectionHeader(title = "Workout", topPadding = Spacing.Small)
            SettingStepper(
                label = "Rest timer between sets",
                value = if (restTimerSeconds == 0) "Off" else "${restTimerSeconds}s",
                onMinus = { onSetRestTimerSeconds((restTimerSeconds - 15).coerceAtLeast(0)) },
                onPlus = { onSetRestTimerSeconds(restTimerSeconds + 15) },
                minusDescription = "Decrease rest time",
                plusDescription = "Increase rest time",
            )
            SettingSwitch(
                label = "Adaptive rest",
                supporting = if (restAdaptive) {
                    "Uses the program's rest and your last RPE; the time above is the fallback."
                } else {
                    "Always the fixed time above."
                },
                checked = restAdaptive,
                onChange = onSetRestAdaptive,
            )
            SettingStepper(
                label = "Hold-exercise start delay",
                value = "${holdDelaySeconds}s",
                onMinus = { onSetHoldDelaySeconds((holdDelaySeconds - 1).coerceAtLeast(0)) },
                onPlus = { onSetHoldDelaySeconds(holdDelaySeconds + 1) },
                minusDescription = "Decrease delay",
                plusDescription = "Increase delay",
            )
            Text(
                "Suggested exercise style",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Spacing.Small, bottom = Spacing.ExtraSmall),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                TrainingLean.entries.forEach { lean ->
                    FilterChip(
                        selected = trainingLean == lean,
                        onClick = { onSetTrainingLean(lean) },
                        label = {
                            Text(
                                when (lean) {
                                    TrainingLean.STRENGTH -> "Strength"
                                    TrainingLean.BALANCED -> "Balanced"
                                    TrainingLean.CALISTHENICS -> "Calisthenics"
                                },
                            )
                        },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Small), color = MaterialTheme.colorScheme.outlineVariant)

            SectionHeader(title = "Appearance", topPadding = Spacing.Small)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                com.lsing.timego.data.ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onSetThemeMode(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Small), color = MaterialTheme.colorScheme.outlineVariant)

            // Local backup & restore
            SectionHeader(title = "Local Backup & Restore", topPadding = Spacing.Small)
            Text(
                "TimeGo stores all data strictly on your device. Export a SQLite database backup or restore previously saved data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.Small),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Button(
                    onClick = { dismissThen(onExportBackup) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Export Backup")
                }
                OutlinedButton(
                    onClick = { dismissThen(onRestoreBackup) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Restore")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Small), color = MaterialTheme.colorScheme.outlineVariant)

            SectionHeader(title = "Training Profile", topPadding = Spacing.Small)
            Text(
                "The preferences TimeGo uses to filter and explain recommendations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.Small),
            )
            TrainingProfileSummary(trainingProfile)
            OutlinedButton(
                onClick = { dismissThen(onEditTrainingProfile) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Edit training profile") }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Small), color = MaterialTheme.colorScheme.outlineVariant)

            SectionHeader(title = "Account & Reports", topPadding = Spacing.Small)
            Text(
                "Optional email sign-in, cloud backup, and report preferences. Guest mode stays fully available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.Small),
            )
            OutlinedButton(onClick = { dismissThen(onOpenAccount) }, modifier = Modifier.fillMaxWidth()) { Text("Account and email reports") }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Small), color = MaterialTheme.colorScheme.outlineVariant)

            // Session history & management
            SectionHeader(title = "Session Management", topPadding = Spacing.Small)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.Small),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Workout History", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "$sessionHistoryCount total recorded sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = { dismissThen(onViewSessionHistory) }) {
                    Text("Manage")
                }
            }
        }
    }
}

/** Label on the left, "- value +" stepper on the right; one row shape for every numeric preference. */
@Composable
private fun SettingStepper(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusDescription: String,
    plusDescription: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onMinus) { Icon(Icons.Filled.Remove, contentDescription = minusDescription) }
        Text(value, style = LedgerFigureEmphasis, modifier = Modifier.widthIn(min = 44.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = onPlus) { Icon(Icons.Filled.Add, contentDescription = plusDescription) }
    }
}

/** Label with a one-line explanation of the current state and a switch. */
@Composable
private fun SettingSwitch(label: String, supporting: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = Spacing.Small)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Read-only view of what TimeGo knows from the training profile (the private limitation note stays out of sight). */
@Composable
private fun TrainingProfileSummary(profile: TrainingProfile) {
    fun label(value: Enum<*>) = formatEnumLabel(value.name.removePrefix("AGE_"))
    val rows = listOfNotNull(
        profile.goal?.let { "Goal" to label(it) },
        profile.experience?.let { "Experience" to label(it) },
        profile.modality?.let { "Training style" to label(it) },
        profile.equipment.takeIf { it.isNotEmpty() }?.let { "Equipment" to it.joinToString(", ") { e -> label(e) } },
        profile.trainingDaysPerWeek?.let { "Days per week" to it.toString() },
        profile.sessionDuration?.let { "Session length" to label(it).replace("Minutes ", "").replace(" To ", "-") + " min" },
        profile.ageRange?.takeIf { it != com.lsing.timego.profile.AgeRange.PREFER_NOT_TO_SAY }?.let { "Age" to label(it) },
        profile.heightCm?.let { "Height" to "${it.toInt()} cm" },
        profile.weightKg?.let { "Weight" to "${"%.1f".format(it)} kg" },
        profile.movementLimitations.takeIf { it.isNotEmpty() }?.let { "Avoids" to it.joinToString(", ") { m -> label(m) } },
    )
    if (rows.isEmpty()) {
        Text(
            "Not set up yet.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = Spacing.Small),
        )
        return
    }
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small)) {
        rows.forEach { (name, value) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
                Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f))
            }
        }
    }
}
