package com.lsing.timego.ui.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.ui.common.LocalTimeGoDialogDismiss
import com.lsing.timego.ui.common.TimeGoDialog
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.theme.Spacing

@Composable
fun AddExerciseDialog(onDismiss: () -> Unit, onAdd: (name: String, muscleGroups: List<String>, category: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var nameTouched by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(ExerciseCategory.STRENGTH) }
    val selectedGroups = remember { mutableStateOf(setOf<String>()) }
    val canAdd = name.isNotBlank() && selectedGroups.value.isNotEmpty()

    TimeGoDialog(
        onDismissRequest = onDismiss,
        eyebrow = "EXERCISE",
        title = "Add Custom Exercise",
        confirmButton = {
            val dismiss = LocalTimeGoDialogDismiss.current
            TextButton(onClick = {
                if (canAdd) {
                    onAdd(name, selectedGroups.value.toList(), category.name)
                    dismiss()
                }
            }, enabled = canAdd) {
                Text("Add")
            }
        },
        dismissButton = {
            val dismiss = LocalTimeGoDialogDismiss.current
            TextButton(onClick = dismiss) { Text("Cancel") }
        },
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameTouched = true },
                label = { Text("Exercise name") },
                supportingText = {
                    Text(
                        if (nameTouched && name.isBlank()) "Exercise name is required" else " ",
                        color = if (nameTouched && name.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                isError = nameTouched && name.isBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Category", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = Spacing.Large, bottom = Spacing.ExtraSmall))
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                ExerciseCategory.entries.forEach { entry ->
                    FilterChip(
                        selected = category == entry,
                        onClick = { category = entry },
                        label = { Text(formatEnumLabel(entry.name)) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }
            Text("Muscle groups", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = Spacing.Large, bottom = Spacing.ExtraSmall))
            Text(
                "Choose at least one muscle group to enable Add.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.ExtraSmall),
            )
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                MuscleGroup.entries.forEach { group ->
                    FilterChip(
                        selected = group.name in selectedGroups.value,
                        onClick = {
                            selectedGroups.value = if (group.name in selectedGroups.value) {
                                selectedGroups.value - group.name
                            } else {
                                selectedGroups.value + group.name
                            }
                        },
                        label = { Text(formatEnumLabel(group.name)) },
                        modifier = Modifier.padding(end = Spacing.ExtraSmall, bottom = Spacing.ExtraSmall),
                    )
                }
            }
        }
    }
}
