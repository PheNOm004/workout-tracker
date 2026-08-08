package com.lsing.timego.ui.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import com.lsing.timego.ui.common.formatEnumLabel

@Composable
fun AddExerciseDialog(onDismiss: () -> Unit, onAdd: (name: String, muscleGroups: List<String>, category: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExerciseCategory.STRENGTH) }
    val selectedGroups = remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Exercise") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Category", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ExerciseCategory.entries.forEach { entry ->
                        FilterChip(
                            selected = category == entry,
                            onClick = { category = entry },
                            label = { Text(formatEnumLabel(entry.name)) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
                Text("Muscle groups", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                MuscleGroup.entries.forEach { group ->
                    val checked = group.name in selectedGroups.value
                    Row {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                selectedGroups.value = if (isChecked) {
                                    selectedGroups.value + group.name
                                } else {
                                    selectedGroups.value - group.name
                                }
                            },
                        )
                        Text(formatEnumLabel(group.name))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && selectedGroups.value.isNotEmpty()) {
                    onAdd(name, selectedGroups.value.toList(), category.name)
                    onDismiss()
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
