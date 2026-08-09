package com.lsing.timego.ui.routines

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lsing.timego.data.Exercise
import com.lsing.timego.ui.common.ExerciseSections
import java.time.DayOfWeek

/** Full-screen dialog for creating a routine -- previously an always-visible form at the bottom
 *  of the Routines screen, which forced scrolling past the whole exercise library just to see
 *  the existing routines list. A dedicated dialog (same pattern as AddExerciseDialog) gives the
 *  name/days/exercise-picking form room to breathe and keeps the main screen to just a list. */
@Composable
fun RoutineFormDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onCreate: (name: String, exerciseIds: List<Long>, daysOfWeek: List<String>) -> Unit,
) {
    var routineName by remember { mutableStateOf("") }
    val selectedExerciseIds = remember { mutableStateOf(setOf<Long>()) }
    val selectedDays = remember { mutableStateOf(setOf<String>()) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text("New Routine", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    item {
                        OutlinedTextField(
                            value = routineName,
                            onValueChange = { routineName = it },
                            label = { Text("Routine name") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("Days", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
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
                        Text("Exercises", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                    }
                    item {
                        ExerciseSections(exercises = exercises) { exercise ->
                            val checked = exercise.id in selectedExerciseIds.value
                            Row {
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
                                Text(exercise.name)
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = {
                            if (routineName.isNotBlank() && selectedExerciseIds.value.isNotEmpty()) {
                                onCreate(routineName, selectedExerciseIds.value.toList(), selectedDays.value.toList())
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Create routine")
                    }
                }
            }
        }
    }
}
