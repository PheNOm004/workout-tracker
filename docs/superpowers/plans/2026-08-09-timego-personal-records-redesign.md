# TimeGo Personal Records Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Personal Records vertical card list in `ProgressScreen.kt` with a `HorizontalWheelPicker` exercise selector and a single `StatTile`-based card, matching the strength-curve section's existing pattern.

**Architecture:** Pure Compose UI change confined to `ProgressScreen.kt`. `ProgressViewModel.records` already holds every PR up front, so exercise selection is local composable state (mirroring the existing `selectedExerciseId` pattern for the strength curve) rather than a ViewModel addition. `StatTile` gains one new optional parameter so its 4 other call sites (Workouts/Duration/Volume/Sets) are unaffected.

**Tech Stack:** Kotlin, Jetpack Compose (`HorizontalWheelPicker`, `Card`, `Row`, `Column` — all already used elsewhere in this file).

## Global Constraints

- No `ProgressViewModel` changes — `records: StateFlow<List<PersonalRecord>>` already contains every PR; this task only changes how `ProgressScreen.kt` renders it.
- No domain-logic changes — `personalRecords()` in `ProgressMath.kt` and `PersonalRecord`/`PrType` are untouched.
- `StatTile`'s 3 existing non-PR call sites (Workouts, Duration, Volume, Sets in the training-stats row) must render identically after this change — the new parameter is optional and defaults to unused.
- On-device verification is handed off to the user directly (per established preference) rather than the agent taking screenshots — build, install, and report what to check.

---

### Task 1: Wheel-picker Personal Records + StatTile date caption

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt`

**Interfaces:**
- Consumes: `ProgressViewModel.records: StateFlow<List<PersonalRecord>>`, `ProgressViewModel.exercises: StateFlow<List<Exercise>>` (both already exposed, unchanged), `HorizontalWheelPicker(items: List<String>, selectedIndex: Int, onSelectedIndexChange: (Int) -> Unit, modifier: Modifier)` (existing component, already used for the strength-curve selector in this same file).
- Produces: `StatTile(label: String, value: String, caption: String? = null)` — new optional third parameter, consumed by both the new PR card and unchanged by the 4 existing training-stats call sites.

- [ ] **Step 1: Add the date formatter and extend `StatTile` with an optional caption**

Add the import (alongside the other `java.time` import already in this file):

```kotlin
import java.time.format.DateTimeFormatter
```

Add a private formatter constant just above `formatRecordValue` (near the bottom of the file, line 285 in the current version):

```kotlin
private val PR_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d")
```

Change the `StatTile` composable (currently the last function in the file) from:

```kotlin
@Composable
private fun StatTile(label: String, value: String) {
    Card(modifier = Modifier.padding(4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
```

to:

```kotlin
@Composable
private fun StatTile(label: String, value: String, caption: String? = null) {
    Card(modifier = Modifier.padding(4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
```

- [ ] **Step 2: Add local wheel-selection state**

In `ProgressScreen`, alongside the other `var ... by remember { mutableStateOf(...) }` declarations near the top of the function (after `var heightText by remember { mutableStateOf("") }`, currently line 62), add:

```kotlin
    var selectedPrExerciseId by remember { mutableStateOf<Long?>(null) }
```

- [ ] **Step 3: Replace the Personal Records section**

Replace this entire block (currently lines 111-138):

```kotlin
        item {
            Text("Personal Records", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        }
        if (records.isEmpty()) {
            item {
                Text(
                    "No personal records yet -- log a few sets to see them here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
        val recordsByExercise = records.groupBy { it.exerciseId }
        items(recordsByExercise.entries.toList(), key = { it.key }) { (exerciseId, exerciseRecords) ->
            val exerciseName = exercises.firstOrNull { it.id == exerciseId }?.name ?: "Unknown exercise"
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(exerciseName, style = MaterialTheme.typography.titleSmall)
                    exerciseRecords.forEach { record ->
                        Text(
                            "${formatEnumLabel(record.type.name)}: ${formatRecordValue(record)} on ${record.achievedOn}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
```

with:

```kotlin
        item {
            Text("Personal Records", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        }
        val recordsByExercise = records.groupBy { it.exerciseId }
        val exercisesWithRecords = exercises.filter { it.id in recordsByExercise.keys }.sortedBy { it.name }
        if (exercisesWithRecords.isEmpty()) {
            item {
                Text(
                    "No personal records yet -- log a few sets to see them here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        } else {
            item {
                val selectedIndex = exercisesWithRecords.indexOfFirst { it.id == selectedPrExerciseId }.coerceAtLeast(0)
                HorizontalWheelPicker(
                    items = exercisesWithRecords.map { it.name },
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = { index -> selectedPrExerciseId = exercisesWithRecords[index].id },
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                val selectedExercise = exercisesWithRecords[selectedIndex]
                val exerciseRecords = recordsByExercise[selectedExercise.id].orEmpty()
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(selectedExercise.name, style = MaterialTheme.typography.titleSmall)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            PrType.entries.forEach { type ->
                                val record = exerciseRecords.firstOrNull { it.type == type }
                                StatTile(
                                    label = formatEnumLabel(type.name),
                                    value = record?.let { formatRecordValue(it) } ?: "--",
                                    caption = record?.achievedOn?.format(PR_DATE_FORMATTER),
                                )
                            }
                        }
                    }
                }
            }
        }
```

- [ ] **Step 4: Compile**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo" ; .\gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Run the full unit test suite**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo" ; .\gradlew.bat testDebugUnitTest`
Expected: PASS — this is a pure Compose UI change with no domain-logic edits, so all existing tests should be unaffected.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt
git commit -m "Redesign Personal Records as a wheel-picker with StatTile cards"
```

- [ ] **Step 7: On-device verification**

Build and install:

```bash
cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"
./gradlew.bat assembleDebug
"/c/Users/lsing/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
```

Hand off to the user to check on-device directly (per established preference — no agent screenshots unless asked): confirm the wheel scrolls through exercises that have PRs (alphabetical order), the selected card shows 3 tiles (Heaviest Weight/Most Reps/Best Volume) with correct values and a readable date caption (e.g. "Aug 9"), and — if there's currently an exercise logged with only some PR types populated (e.g. only ever logged one set, so heaviest/most-reps/best-volume might coincide or a type might be genuinely missing) — that the missing case renders "--" rather than crashing.
