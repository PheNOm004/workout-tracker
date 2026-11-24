package com.lsing.timego.ui.routines

import com.lsing.timego.ui.common.TimeGoBottomSheet
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.lsing.timego.ui.common.SurfaceCard
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
    readinessTapEnabled: Boolean,
    onSetReadinessTapEnabled: (Boolean) -> Unit,
    rpeEntryMode: com.lsing.timego.data.RpeEntryMode,
    onSetRpeEntryMode: (com.lsing.timego.data.RpeEntryMode) -> Unit,
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
                .padding(horizontal = Spacing.Large)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.ExtraLarge + 24.dp),
        ) {
            Text(
                "SETTINGS",
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp),
            )
            Text(
                "Preferences & Data",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Spacing.Small),
            )

            SettingsGroup("Workout", Icons.Filled.FitnessCenter) {
                SettingStepper(
                    label = "Rest timer between sets",
                    value = if (restTimerSeconds == 0) "Off" else "${restTimerSeconds}s",
                    onMinus = { onSetRestTimerSeconds((restTimerSeconds - 15).coerceAtLeast(0)) },
                    onPlus = { onSetRestTimerSeconds(restTimerSeconds + 15) },
                    minusDescription = "Decrease rest time",
                    plusDescription = "Increase rest time",
                )
                SettingsDivider()
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
                SettingsDivider()
                SettingSwitch(
                    label = "Readiness check",
                    supporting = if (readinessTapEnabled) {
                        "Asks how ready you feel (1 to 5) when a session starts. On a 1 or 2 day, weights are held and rests run 15 s longer."
                    } else {
                        "Off. Turn on to rate how ready you feel at the start of each session."
                    },
                    checked = readinessTapEnabled,
                    onChange = onSetReadinessTapEnabled,
                )
                SettingsDivider()
                SettingStepper(
                    label = "Hold-exercise start delay",
                    value = "${holdDelaySeconds}s",
                    onMinus = { onSetHoldDelaySeconds((holdDelaySeconds - 1).coerceAtLeast(0)) },
                    onPlus = { onSetHoldDelaySeconds(holdDelaySeconds + 1) },
                    minusDescription = "Decrease delay",
                    plusDescription = "Increase delay",
                )
                SettingsDivider()
                SettingChoice(label = "Suggested exercise style") {
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
                SettingsDivider()
                SettingChoice(
                    label = "Rate effort (RPE)",
                    supporting = when (rpeEntryMode) {
                        com.lsing.timego.data.RpeEntryMode.AFTER_SET -> "After each working set the rest timer asks how many reps you had left. One tap, or ignore it."
                        com.lsing.timego.data.RpeEntryMode.SLIDER -> "Open an RPE slider on the entry card before you log the set."
                        com.lsing.timego.data.RpeEntryMode.OFF -> "RPE is not asked for. Adaptive rest uses the program rest only."
                    },
                ) {
                    com.lsing.timego.data.RpeEntryMode.entries.forEach { mode ->
                        FilterChip(
                            selected = rpeEntryMode == mode,
                            onClick = { onSetRpeEntryMode(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        com.lsing.timego.data.RpeEntryMode.AFTER_SET -> "After the set"
                                        com.lsing.timego.data.RpeEntryMode.SLIDER -> "Slider"
                                        com.lsing.timego.data.RpeEntryMode.OFF -> "Off"
                                    },
                                )
                            },
                        )
                    }
                }
            }

            SettingsGroup("Appearance", Icons.Filled.Palette) {
                SettingChoice(label = "Theme") {
                    com.lsing.timego.data.ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) },
                        )
                    }
                }
            }

            SettingsGroup("Backup & restore", Icons.Filled.Backup) {
                Text(
                    "TimeGo stores all data strictly on your device. Export a SQLite database backup or restore previously saved data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.Small),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    Button(onClick = { dismissThen(onExportBackup) }, modifier = Modifier.weight(1f)) { Text("Export backup") }
                    OutlinedButton(onClick = { dismissThen(onRestoreBackup) }, modifier = Modifier.weight(1f)) { Text("Restore") }
                }
            }

            SettingsGroup("Training profile", Icons.Filled.Tune) {
                Text(
                    "The preferences TimeGo uses to filter and explain recommendations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.Small),
                )
                TrainingProfileSummary(trainingProfile)
                SettingsDivider()
                SettingsLinkRow("Edit training profile", null) { dismissThen(onEditTrainingProfile) }
            }

            SettingsGroup("Account & reports", Icons.Filled.AccountCircle) {
                SettingsLinkRow(
                    "Account and email reports",
                    "Optional email sign-in, cloud backup, and report preferences. Guest mode stays fully available.",
                ) { dismissThen(onOpenAccount) }
            }

            SettingsGroup("Session management", Icons.Filled.History) {
                SettingsLinkRow(
                    "Workout history",
                    "$sessionHistoryCount total recorded sessions",
                ) { dismissThen(onViewSessionHistory) }
            }
        }
    }
}

/** A titled card holding one group of related settings; groups are spaced apart instead of divided by rules. */
@Composable
private fun SettingsGroup(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = Spacing.Large, bottom = Spacing.Small, start = Spacing.ExtraSmall),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.Small),
        )
    }
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Medium, vertical = Spacing.Small), content = content)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.ExtraSmall), color = MaterialTheme.colorScheme.outlineVariant)
}

/** A label, optional explanation of the current choice, and a wrapping row of chips. */
@Composable
private fun SettingChoice(label: String, supporting: String? = null, chips: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = Spacing.ExtraSmall))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) { chips() }
        if (supporting != null) {
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.ExtraSmall),
            )
        }
    }
}

/** A whole-row tap target with a chevron, for settings that open another screen. */
@Composable
private fun SettingsLinkRow(title: String, supporting: String?, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.ExtraSmall),
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = Spacing.Small)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
