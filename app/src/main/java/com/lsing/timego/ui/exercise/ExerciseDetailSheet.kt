package com.lsing.timego.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lsing.timego.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailSheet(
    model: ExerciseDetailModel,
    easierName: String?,
    onShowEasier: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            item {
                Text(model.title, style = MaterialTheme.typography.headlineSmall)
                Text(model.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            model.personalSummary?.let { summary -> item { AssistChip(onClick = {}, label = { Text(summary) }) } }
            if (model.equipment.isNotEmpty()) item { DetailSection("Equipment", model.equipment) }
            model.purpose?.let { purpose -> item { DetailSection("Purpose", listOf(purpose)) } }
            if (model.isReviewed) {
                item { DetailSection("Set up", model.setup) }
                item { DetailSection("How to do it", model.steps, numbered = true) }
                item { DetailSection("Useful cues", model.cues) }
                item { DetailSection("Avoid", model.mistakes) }
            } else {
                item { Text("Full instructions are still being reviewed. You can keep logging this exercise normally.") }
            }
            if (easierName != null) {
                item { TextButton(onClick = onShowEasier) { Text("Start with $easierName") } }
            }
            item { Text("Stop if a movement causes pain, dizziness, or loss of control.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = Spacing.Large)) }
        }
    }
}

@Composable
private fun DetailSection(title: String, lines: List<String>, numbered: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        lines.forEachIndexed { index, line -> Text(if (numbered) "${index + 1}. $line" else "• $line") }
    }
}
