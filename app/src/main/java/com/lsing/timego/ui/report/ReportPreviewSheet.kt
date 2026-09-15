package com.lsing.timego.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lsing.timego.report.WorkoutReport
import com.lsing.timego.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportPreviewSheet(report: WorkoutReport, title: String, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.Large), verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text("${report.period.start} to ${report.period.endInclusive}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Metric("Sessions", report.sessionCount.toString())
            Metric("Active days", report.activeDays.toString())
            Metric("Training time", "${report.durationMinutes} min")
            Metric("Working sets", report.workingSets.toString())
            Metric("Strength volume", "${report.strengthVolumeKg.current.toInt()} kg")
            Metric("Holds", "${report.holdSeconds} sec")
            Metric("Cardio", "${report.cardioMinutes.toInt()} min • ${"%.1f".format(report.cardioDistanceKm)} km")
            Metric("Muscles logged", report.muscleGroups.size.toString())
            Text(report.suggestion, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spacing.Small, bottom = Spacing.Large))
        }
    }
}

@Composable private fun Metric(label: String, value: String) {
    Column { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.titleMedium) }
}
