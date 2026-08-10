# TimeGo Visual-Identity Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the app's already-distinctive theme (Onyx palette, Manrope/Fraunces type scale) actually show up on screen — category icon/accent cues on exercise rows, consistent card elevation and spacing, and real jobs for the Fraunces serif (PR stat values, routine names) — across Log, Routines, and Progress.

**Architecture:** A small shared visual-language kit built once in `ui/common`/`ui/theme` (category→icon/accent map, named spacing constants, a `SectionHeader` composable, two new Fraunces text styles), then applied screen by screen: Log → Routines → Progress touch-ups.

**Tech Stack:** Kotlin, Jetpack Compose, Material3. `androidx.compose.material:material-icons-extended` is already a dependency (`app/build.gradle.kts:49`) — no new dependencies needed.

## Global Constraints

- No new domain logic in this pass — pure UI. No new unit tests required; verify via `./gradlew assembleDebug` and the existing `testDebugUnitTest` suite staying green (per the spec, Section 4).
- No proactive screenshots — the user verifies on-device themselves once their phone reconnects (established project preference).
- Spacing scale: `Spacing.ExtraSmall = 4.dp`, `Small = 8.dp`, `Medium = 12.dp`, `Large = 16.dp`, `ExtraLarge = 24.dp` (from `docs/superpowers/specs/2026-08-10-timego-visual-identity-pass-design.md`, Section 2).
- Card elevation: `CardDefaults.cardElevation(defaultElevation = 2.dp)` on every `Card` touched in this pass.
- Category → icon/accent mapping (spec Section 1): STRENGTH → `Icons.Filled.FitnessCenter` / `OnyxPrimary`; CALISTHENICS → `Icons.Filled.Accessibility` / `OnyxTertiary`; CARDIO → `Icons.Filled.MonitorHeart` / `OnyxSecondary`; WARMUP → `Icons.Filled.Whatshot` / `MaterialTheme.colorScheme.outline` (theme-adaptive, deliberately muted).

---

### Task 1: Spacing tokens + Fraunces text styles

**Files:**
- Create: `app/src/main/java/com/lsing/timego/ui/theme/Spacing.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/theme/Type.kt` (append after the `Typography` val, i.e. after line 86)

**Interfaces:**
- Produces: `object Spacing { val ExtraSmall, Small, Medium, Large, ExtraLarge: Dp }`; `val FrauncesStatValue: TextStyle`; `val FrauncesEmphasis: TextStyle` — all consumed by Tasks 2–6.

- [ ] **Step 1: Create `Spacing.kt`**

```kotlin
package com.lsing.timego.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Named spacing scale so screens stop guessing at ad-hoc padding values. */
object Spacing {
    val ExtraSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val ExtraLarge: Dp = 24.dp
}
```

- [ ] **Step 2: Append the two Fraunces text styles to `Type.kt`**

Add after line 86 (the closing `)` of the `Typography` val), still inside `package com.lsing.timego.ui.theme`:

```kotlin

/** Fraunces treatment for PR stat-tile values -- TimeGo's "moment" numbers. */
val FrauncesStatValue = TextStyle(
    fontFamily = FrauncesFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 24.sp,
)

/** Fraunces treatment for short identity-bearing labels (routine names). */
val FrauncesEmphasis = TextStyle(
    fontFamily = FrauncesFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
)
```

- [ ] **Step 3: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (both new files compile; nothing else references them yet so no other code should break).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/theme/Spacing.kt app/src/main/java/com/lsing/timego/ui/theme/Type.kt
git commit -m "Add spacing scale and Fraunces stat/emphasis text styles"
```

---

### Task 2: Category icon/accent mapping

**Files:**
- Create: `app/src/main/java/com/lsing/timego/ui/common/CategoryVisuals.kt`

**Interfaces:**
- Consumes: `com.lsing.timego.data.ExerciseCategory` (existing enum: `STRENGTH, CALISTHENICS, CARDIO, WARMUP`); `OnyxPrimary`, `OnyxSecondary`, `OnyxTertiary` (existing, `ui/theme/Color.kt`).
- Produces: `data class CategoryVisual(val icon: ImageVector, val accent: Color)`; `@Composable fun categoryVisual(category: ExerciseCategory): CategoryVisual` — consumed by Task 4 (LogScreen).

- [ ] **Step 1: Create `CategoryVisuals.kt`**

```kotlin
package com.lsing.timego.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.ui.theme.OnyxPrimary
import com.lsing.timego.ui.theme.OnyxSecondary
import com.lsing.timego.ui.theme.OnyxTertiary

/** Icon + accent color shown per [ExerciseCategory] so the exercise list is scannable at a
 *  glance instead of a wall of identical rows. WARMUP uses the theme's neutral outline color
 *  (not an Onyx accent) -- deliberately muted, since warmups aren't the main event. */
data class CategoryVisual(val icon: ImageVector, val accent: Color)

@Composable
fun categoryVisual(category: ExerciseCategory): CategoryVisual = when (category) {
    ExerciseCategory.STRENGTH -> CategoryVisual(Icons.Filled.FitnessCenter, OnyxPrimary)
    ExerciseCategory.CALISTHENICS -> CategoryVisual(Icons.Filled.Accessibility, OnyxTertiary)
    ExerciseCategory.CARDIO -> CategoryVisual(Icons.Filled.MonitorHeart, OnyxSecondary)
    ExerciseCategory.WARMUP -> CategoryVisual(Icons.Filled.Whatshot, MaterialTheme.colorScheme.outline)
}
```

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (If `MonitorHeart` or `Whatshot` aren't resolvable, search `material-icons-extended`'s actual bundled icon set for the closest equivalent — e.g. `Icons.Filled.DirectionsRun` for CARDIO or `Icons.Filled.LocalFireDepartment` for WARMUP — and use that instead; the exact icon choice is not load-bearing elsewhere.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/common/CategoryVisuals.kt
git commit -m "Add category icon/accent visual mapping"
```

---

### Task 3: `SectionHeader` shared composable

**Files:**
- Create: `app/src/main/java/com/lsing/timego/ui/common/SectionHeader.kt`

**Interfaces:**
- Consumes: `Spacing` (Task 1).
- Produces: `@Composable fun SectionHeader(title: String, modifier: Modifier = Modifier, topPadding: Dp = Spacing.Large, trailing: (@Composable () -> Unit)? = null)` — consumed by Tasks 4, 5, 6.

- [ ] **Step 1: Create `SectionHeader.kt`**

```kotlin
package com.lsing.timego.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.lsing.timego.ui.theme.Spacing

/** Shared section-title row used across Log/Routines/Progress, with an optional trailing slot
 *  for an inline action (e.g. Routines' "+ New routine" button) -- replaces one-off
 *  Row+Text+Button combos doing the same job on each screen. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    topPadding: Dp = Spacing.Large,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(top = topPadding, bottom = Spacing.Small),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}
```

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/common/SectionHeader.kt
git commit -m "Add shared SectionHeader composable"
```

---

### Task 4: Apply to LogScreen

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt` (entire file — every composable in it changes)

**Interfaces:**
- Consumes: `Spacing` (Task 1), `categoryVisual`/`CategoryVisual` (Task 2), `SectionHeader` (Task 3).
- Produces: `ExerciseRowHeader`, `StrengthLogRow`, `CardioLogRow` now take a category-derived `CategoryVisual` — no other file calls these (private), so no downstream interface change.

- [ ] **Step 1: Replace the full contents of `LogScreen.kt`**

```kotlin
package com.lsing.timego.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.domain.MET_CARDIO
import com.lsing.timego.domain.MET_WARMUP
import com.lsing.timego.domain.averagePaceMinPerKm
import com.lsing.timego.domain.estimatedCalorieBurn
import com.lsing.timego.ui.common.ExerciseSections
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.common.categoryVisual
import com.lsing.timego.ui.theme.Spacing

@Composable
fun LogScreen(viewModel: LogViewModel = viewModel()) {
    val exercises by viewModel.displayedExercises.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val routines by viewModel.routines.collectAsState()
    val selectedRoutineId by viewModel.selectedRoutineId.collectAsState()
    val latestBodyWeightKg by viewModel.latestBodyWeightKg.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onAdd = viewModel::addCustomExercise,
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add custom exercise")
            }
        },
    ) { fabPadding ->
        LazyColumn(modifier = Modifier.padding(Spacing.Large).padding(fabPadding)) {
            item {
                SectionHeader("Session type", topPadding = Spacing.ExtraSmall)
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Small).horizontalScroll(rememberScrollState())) {
                    FilterChip(
                        selected = selectedRoutineId == null,
                        onClick = { viewModel.selectRoutine(null) },
                        label = { Text("Freeform") },
                        modifier = Modifier.padding(end = Spacing.Small),
                    )
                    routines.forEach { routine ->
                        FilterChip(
                            selected = selectedRoutineId == routine.id,
                            onClick = { viewModel.selectRoutine(routine.id) },
                            label = { Text(routine.name) },
                            modifier = Modifier.padding(end = Spacing.Small),
                        )
                    }
                }
            }
            item {
                ExerciseSections(exercises = exercises) { exercise ->
                    if (exercise.category == ExerciseCategory.CARDIO.name || exercise.category == ExerciseCategory.WARMUP.name) {
                        CardioLogRow(
                            exerciseName = exercise.name,
                            category = exercise.category,
                            met = if (exercise.category == ExerciseCategory.CARDIO.name) MET_CARDIO else MET_WARMUP,
                            bodyWeightKg = latestBodyWeightKg,
                            onLog = { duration, distance -> viewModel.logCardioSet(exercise.id, duration, distance) },
                        )
                    } else {
                        StrengthLogRow(
                            exerciseName = exercise.name,
                            category = exercise.category,
                            suggestion = suggestions[exercise.id],
                            isBodyweight = exercise.category == ExerciseCategory.CALISTHENICS.name,
                            latestBodyWeightKg = latestBodyWeightKg,
                            onLog = { weight, reps, target -> viewModel.logSet(exercise.id, weight, reps, target) },
                        )
                    }
                }
            }
            item {
                // Bottom spacer so the last exercise card isn't hidden behind the FAB.
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

/** A one-line header (icon + exercise name) that expands into the actual logging inputs when
 *  tapped. Defaults to collapsed -- rendering full input rows for every exercise in a 119-strong
 *  library at once was both visually overwhelming and wasteful, per user feedback. The leading
 *  icon is decorative (contentDescription = null): the category is also conveyed by the card's
 *  left accent bar, and naming it here would be redundant with what TalkBack already reads from
 *  the exercise name and expand/collapse icon. */
@Composable
private fun ExerciseRowHeader(
    exerciseName: String,
    icon: ImageVector,
    accent: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(Spacing.Medium),
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(end = Spacing.Small))
        Text(exerciseName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
            contentDescription = if (expanded) "Collapse" else "Expand",
        )
    }
}

/** Wraps [content] in a card with a category-accent-colored left bar, matching the visual
 *  language used by both [StrengthLogRow] and [CardioLogRow]. */
@Composable
private fun ExerciseCard(accent: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.Large, end = Spacing.Small, bottom = Spacing.Small),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(accent))
            Column(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}

@Composable
private fun StrengthLogRow(
    exerciseName: String,
    category: String,
    suggestion: com.lsing.timego.domain.OverloadSuggestion?,
    isBodyweight: Boolean,
    latestBodyWeightKg: Double?,
    onLog: (weightKg: Double, reps: Int, targetReps: Int) -> Unit,
) {
    var expanded by remember(exerciseName) { mutableStateOf(false) }
    // Bodyweight exercises (Pull-Up, Push-Up, Dip, ...) pre-fill kg with the user's latest logged
    // body weight rather than leaving it blank/0 -- an unedited bodyweight set is still real load,
    // and 0 would flatten estimatedOneRepMax to zero forever regardless of actual rep progress.
    var weightText by remember(exerciseName) {
        mutableStateOf(if (isBodyweight) latestBodyWeightKg?.toString().orEmpty() else "")
    }
    var repsText by remember(exerciseName) { mutableStateOf("") }
    val visual = categoryVisual(ExerciseCategory.valueOf(category))

    ExerciseCard(visual.accent) {
        ExerciseRowHeader(exerciseName, visual.icon, visual.accent, expanded) { expanded = !expanded }
        if (expanded) {
            if (suggestion != null) {
                Text(
                    "Suggested: ${suggestion.weightKg}kg x ${suggestion.reps} -- ${suggestion.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Spacing.Medium),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("kg") },
                    placeholder = if (isBodyweight) { { Text("BW") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).padding(end = Spacing.Small),
                )
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it },
                    label = { Text("reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).padding(end = Spacing.Small),
                )
                Button(onClick = {
                    val weight = weightText.toDoubleOrNull()
                    val reps = repsText.toIntOrNull()
                    if (weight != null && reps != null) {
                        onLog(weight, reps, suggestion?.reps ?: reps)
                        weightText = ""
                        repsText = ""
                    }
                }) {
                    Text("Log set")
                }
            }
        }
    }
}

@Composable
private fun CardioLogRow(
    exerciseName: String,
    category: String,
    met: Double,
    bodyWeightKg: Double?,
    onLog: (durationMinutes: Double, distanceKm: Double?) -> Unit,
) {
    var expanded by remember(exerciseName) { mutableStateOf(false) }
    var durationText by remember(exerciseName) { mutableStateOf("") }
    var distanceText by remember(exerciseName) { mutableStateOf("") }
    val duration = durationText.toDoubleOrNull()
    val distance = distanceText.toDoubleOrNull()
    val visual = categoryVisual(ExerciseCategory.valueOf(category))

    ExerciseCard(visual.accent) {
        ExerciseRowHeader(exerciseName, visual.icon, visual.accent, expanded) { expanded = !expanded }
        if (expanded) {
            if (duration != null && duration > 0) {
                val pace = distance?.let { averagePaceMinPerKm(duration, it) }
                val calories = bodyWeightKg?.let { estimatedCalorieBurn(met, it, duration) }
                val details = listOfNotNull(
                    pace?.let { "Pace: ${"%.1f".format(it)} min/km" },
                    calories?.let { "~${it.toInt()} kcal" },
                ).joinToString(" -- ")
                if (details.isNotEmpty()) {
                    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = Spacing.Medium))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).padding(end = Spacing.Small),
                )
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = { distanceText = it },
                    label = { Text("km (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).padding(end = Spacing.Small),
                )
                Button(onClick = {
                    if (duration != null && duration > 0) {
                        onLog(duration, distance)
                        durationText = ""
                        distanceText = ""
                    }
                }) {
                    Text("Log")
                }
            }
        }
    }
}
```

Note: this adds `import androidx.compose.foundation.layout.Column` implicitly via `Column(modifier = ...)` used inside `ExerciseCard` — add `import androidx.compose.foundation.layout.Column` to the import list above (it was not needed in the original file since it had no `Column` usage).

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If `Column` is reported unresolved, confirm the import from Step 1's note was added.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt
git commit -m "Apply category icon/accent cues and spacing/elevation to LogScreen"
```

---

### Task 5: Apply to RoutinesScreen

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/routines/RoutinesScreen.kt` (entire file)

**Interfaces:**
- Consumes: `Spacing`, `FrauncesEmphasis` (Task 1), `SectionHeader` (Task 3).

- [ ] **Step 1: Replace the full contents of `RoutinesScreen.kt`**

```kotlin
package com.lsing.timego.ui.routines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.theme.FrauncesEmphasis
import com.lsing.timego.ui.theme.Spacing

@Composable
fun RoutinesScreen(viewModel: RoutinesViewModel = viewModel()) {
    val routines by viewModel.routines.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val untrainedGroups by viewModel.untrainedGroups.collectAsState()
    var showRoutineForm by remember { mutableStateOf(false) }

    if (showRoutineForm) {
        RoutineFormDialog(
            exercises = exercises,
            onDismiss = { showRoutineForm = false },
            onCreate = viewModel::createRoutine,
        )
    }

    LazyColumn(modifier = Modifier.padding(Spacing.Large)) {
        if (untrainedGroups.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.Large),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Text(
                        "Not trained in a while: ${untrainedGroups.joinToString(", ") { formatEnumLabel(it) }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(Spacing.Medium),
                    )
                }
            }
        }
        item {
            SectionHeader(
                title = "Your Routines",
                topPadding = Spacing.ExtraSmall,
                trailing = { Button(onClick = { showRoutineForm = true }) { Text("+ New routine") } },
            )
        }
        if (routines.isEmpty()) {
            item {
                Text(
                    "No routines yet -- create one to plan which days you train which exercises.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(routines, key = { it.id }) { routine ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            routine.name,
                            style = FrauncesEmphasis,
                            modifier = Modifier.weight(1f).padding(Spacing.Medium, Spacing.Medium, Spacing.Medium, Spacing.ExtraSmall),
                        )
                        IconButton(onClick = { viewModel.deleteRoutine(routine.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete ${routine.name}")
                        }
                    }
                    if (routine.daysOfWeek.isEmpty()) {
                        Text(
                            "No days set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(Spacing.Medium, 0.dp, Spacing.Medium, Spacing.Medium),
                        )
                    } else {
                        FlowRow(modifier = Modifier.padding(Spacing.Small, 0.dp, Spacing.Small, Spacing.Small)) {
                            routine.daysOfWeek.forEach { day ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(day.take(3).lowercase().replaceFirstChar(Char::uppercase)) },
                                    modifier = Modifier.padding(Spacing.ExtraSmall),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/routines/RoutinesScreen.kt
git commit -m "Apply SectionHeader, Fraunces routine names, and card elevation to RoutinesScreen"
```

---

### Task 6: Apply touch-ups to ProgressScreen

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt`

**Interfaces:**
- Consumes: `Spacing`, `FrauncesStatValue` (Task 1), `SectionHeader` (Task 3).
- No signature changes to `StatTile`, `DayHistoryDialog`, or `ProgressScreen` itself — same call sites, same parameters, only internal styling changes.

- [ ] **Step 1: Add imports**

At the top of `ProgressScreen.kt`, add these three imports (alongside the existing `androidx.compose.material3.*` and `com.lsing.timego.ui.common.*` imports):

```kotlin
import androidx.compose.material3.CardDefaults
import com.lsing.timego.ui.common.SectionHeader
import com.lsing.timego.ui.theme.FrauncesStatValue
import com.lsing.timego.ui.theme.Spacing
```

- [ ] **Step 2: Swap the `LazyColumn` padding and the five section-title `Text` calls for `SectionHeader`**

Change (`ProgressScreen.kt:77`):
```kotlin
    LazyColumn(modifier = Modifier.padding(16.dp)) {
```
to:
```kotlin
    LazyColumn(modifier = Modifier.padding(Spacing.Large)) {
```

Change (`ProgressScreen.kt:79`):
```kotlin
            Text("Consistency", style = MaterialTheme.typography.titleMedium)
```
to:
```kotlin
            SectionHeader("Consistency", topPadding = Spacing.ExtraSmall)
```

Change (`ProgressScreen.kt:88`):
```kotlin
            Text("Muscle Distribution (last 30 days)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
```
to:
```kotlin
            SectionHeader("Muscle Distribution (last 30 days)")
```

Change (`ProgressScreen.kt:114`):
```kotlin
            Text("Personal Records", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
```
to:
```kotlin
            SectionHeader("Personal Records")
```

Change (`ProgressScreen.kt:156`):
```kotlin
            Text("Strength Curve", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
```
to:
```kotlin
            SectionHeader("Strength Curve")
```

Change (`ProgressScreen.kt:204`):
```kotlin
            Text("Body Metrics", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
```
to:
```kotlin
            SectionHeader("Body Metrics")
```

- [ ] **Step 3: Add elevation to the Personal Records card**

Change (`ProgressScreen.kt:138`):
```kotlin
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
```
to:
```kotlin
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
```

- [ ] **Step 4: Give `StatTile` Fraunces treatment + elevation**

Change (`ProgressScreen.kt:309-320`):
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
to:
```kotlin
@Composable
private fun StatTile(label: String, value: String, caption: String? = null) {
    Card(
        modifier = Modifier.padding(Spacing.ExtraSmall),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = FrauncesStatValue)
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
```

- [ ] **Step 5: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt
git commit -m "Apply SectionHeader, Fraunces stat values, and card elevation to ProgressScreen"
```

---

### Task 7: Full build + test verification

**Files:** none (verification only)

- [ ] **Step 1: Full debug build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Existing unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing domain tests still passing (no test should reference any file touched in this plan, since none of the changes touch `data/` or `domain/`).

- [ ] **Step 3: Update the vault project note**

Add a short session entry to `C:\Users\lsing\.claude\obsidian_demo\Projects\TimeGo - Gym Progress Tracker.md` (top of file, under the existing `[!note]` callout, following the pattern of prior session entries) recording that the visual-identity pass shipped: category icon/accent cues, spacing/elevation consistency, Fraunces on PR values and routine names, across Log/Routines/Progress. Note explicitly that on-device verification is still pending (no phone connected this session) and is the next thing to do when the user reconnects their Galaxy S23 Ultra.

- [ ] **Step 4: Final commit (if the vault note was updated as a separate file outside this repo, this step covers only in-repo state)**

```bash
cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"
git status
git log --oneline -6
```

Expected: six commits from Tasks 1–6 visible, working tree clean.

---

## Self-Review Notes

- **Spec coverage**: Section 1 (category icons/accents) → Task 2 + Task 4. Section 2 (card elevation, spacing scale, SectionHeader) → Tasks 1, 3, 4, 5, 6. Section 3 (Fraunces for PR values + routine names) → Task 1 (styles) + Tasks 5, 6 (application). Section 4 (apply order Log → Routines → Progress, build-only verification) → Tasks 4, 5, 6, 7 in that order.
- **Type consistency checked**: `categoryVisual(category: ExerciseCategory): CategoryVisual` (Task 2) is called identically in both `StrengthLogRow` and `CardioLogRow` (Task 4) via `ExerciseCategory.valueOf(category)`, matching the existing `exercise.category: String` field already used elsewhere in the same file (`exercise.category == ExerciseCategory.CARDIO.name`). `SectionHeader`'s signature (Task 3) is used consistently across Tasks 4–6, including the `trailing` slot only where a real trailing action exists (Routines' "+ New routine" button) and `topPadding = Spacing.ExtraSmall` only on each screen's first header.
- **No new domain/ViewModel changes** — confirmed no task touches `data/`, `domain/`, or any `*ViewModel.kt`, consistent with the spec's "pure UI, no new unit tests" scope.
