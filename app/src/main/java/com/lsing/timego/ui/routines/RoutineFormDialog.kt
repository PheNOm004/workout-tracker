package com.lsing.timego.ui.routines

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lsing.timego.data.Exercise
import com.lsing.timego.ui.common.ExerciseSections
import com.lsing.timego.ui.common.LocalTimeGoDialogDismiss
import com.lsing.timego.ui.common.TimeGoDialog
import com.lsing.timego.ui.theme.Spacing
import java.time.DayOfWeek

/** Dedicated hardware-plated dialog for creating a routine with Gauge Panel styling,
 *  scroll-to-fit layout, and symmetric EaseInOut enter/exit animations. */
@Composable
fun RoutineFormDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onCreate: (name: String, exerciseIds: List<Long>, daysOfWeek: List<String>) -> Unit,
) {
    var routineName by remember { mutableStateOf("") }
    val selectedExerciseIds = remember { mutableStateOf<List<Long>>(emptyList()) }
    val selectedDays = remember { mutableStateOf(setOf<String>()) }

    TimeGoDialog(
        onDismissRequest = onDismiss,
        eyebrow = "ROUTINES",
        title = "New Routine",
        dismissButton = {
            val dismiss = LocalTimeGoDialogDismiss.current
            TextButton(onClick = dismiss) { Text("Cancel") }
        },
        confirmButton = {
            val dismiss = LocalTimeGoDialogDismiss.current
            Button(
                onClick = {
                    if (routineName.isNotBlank() && selectedExerciseIds.value.isNotEmpty()) {
                        onCreate(routineName, selectedExerciseIds.value.toList(), selectedDays.value.toList())
                        dismiss()
                    }
                },
                enabled = routineName.isNotBlank() && selectedExerciseIds.value.isNotEmpty(),
            ) {
                Text("Create routine")
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = routineName,
                onValueChange = { routineName = it },
                label = { Text("Routine name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Days",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = Spacing.Large, bottom = Spacing.ExtraSmall),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                DayOfWeek.entries.forEach { day ->
                    val checked = day.name in selectedDays.value
                    FilterChip(
                        selected = checked,
                        onClick = {
                            selectedDays.value = if (checked) selectedDays.value - day.name else selectedDays.value + day.name
                        },
                        label = { Text(day.name.take(3).lowercase().replaceFirstChar(Char::uppercase)) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }
            Text(
                "Exercises",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.Large, bottom = Spacing.ExtraSmall),
            )
            Text(
                "Tap in workout order",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.ExtraSmall),
            )
            ExerciseSections(exercises = exercises) { exercise ->
                val checked = exercise.id in selectedExerciseIds.value
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            selectedExerciseIds.value = if (isChecked) {
                                selectedExerciseIds.value + exercise.id
                            } else {
                                selectedExerciseIds.value - exercise.id
                            }
                        },
                    )
                    Text(
                        exercise.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = Spacing.ExtraSmall),
                    )
                }
            }
        }
    }
}
