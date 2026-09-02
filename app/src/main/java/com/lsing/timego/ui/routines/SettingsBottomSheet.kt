package com.lsing.timego.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lsing.timego.data.TrainingLean
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.theme.LedgerFigureEmphasis
import com.lsing.timego.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    holdDelaySeconds: Int,
    onSetHoldDelaySeconds: (Int) -> Unit,
    trainingLean: TrainingLean,
    onSetTrainingLean: (TrainingLean) -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    sessionHistoryCount: Int,
    onViewSessionHistory: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Large)
                .padding(bottom = Spacing.ExtraLarge + 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Preferences & Data",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Spacing.Small),
            )

            // Hold exercise start delay
            SectionHeader(title = "Workout Settings", topPadding = Spacing.Small)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
            ) {
                Text(
                    "Hold-exercise start delay",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onSetHoldDelaySeconds((holdDelaySeconds - 1).coerceAtLeast(0)) }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease delay")
                }
                Text("${holdDelaySeconds}s", style = LedgerFigureEmphasis)
                IconButton(onClick = { onSetHoldDelaySeconds(holdDelaySeconds + 1) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase delay")
                }
            }

            // Training lean
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
                    onClick = onExportBackup,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Export Backup")
                }
                OutlinedButton(
                    onClick = onRestoreBackup,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Restore")
                }
            }

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
                OutlinedButton(onClick = onViewSessionHistory) {
                    Text("Manage")
                }
            }
        }
    }
}
