# TimeGo Motion Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace instant state cuts (row expand/collapse, screen transitions, Progress content swaps) with smooth, purposeful animation, per the `ui-ux-pro-max` skill's motion guidance -- targeted at the specific gaps its checks found, not a blanket animate-everything pass.

**Architecture:** One shared `AnimatedExpand` composable in `ui/common` encapsulates the expand/collapse transition convention (ease-out enter, faster ease-in exit) and replaces five duplicated `if (expanded) { ... }` blocks across `LogScreen.kt` and `ExerciseListSections.kt`. `TimeGoNavHost.kt` gets per-destination crossfade transitions. `ProgressScreen.kt` gets two `AnimatedContent` wraps for its PR-tile row and strength-curve chart.

**Tech Stack:** Kotlin, Jetpack Compose (`androidx.compose.animation`, transitively available via the existing Material3 dependency -- no new Gradle dependency expected, verified in Task 1).

## Global Constraints

- Shared easing convention (from the design spec): enter over 250ms with `LinearOutSlowInEasing` (ease-out), exit over 150ms with `FastOutLinearInEasing` (ease-in) -- exit faster than enter.
- No haptics, no color/typography/layout changes, no Canvas-content animation (muscle diagram / radar chart) -- all explicitly out of scope per the design spec.
- Pure UI change, no domain logic -- no new unit tests needed. Build-verify + existing suite staying green, then on-device verification (phone connected) before merge.

---

### Task 1: Shared `AnimatedExpand` composable

**Files:**
- Create: `app/src/main/java/com/lsing/timego/ui/common/AnimatedExpand.kt`

**Interfaces:**
- Produces: `@Composable fun AnimatedExpand(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit)` -- consumed by Tasks 2 and 3.

- [ ] **Step 1: Create `AnimatedExpand.kt`**

```kotlin
package com.lsing.timego.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Shared expand/collapse transition for the app's several collapsible sections (exercise log
 *  rows, library category/muscle-group headers) -- ease-out entering over 250ms, faster ease-in
 *  exiting over 150ms, per standard "exit-faster-than-enter" motion convention. Wraps [content]
 *  in a Column since AnimatedVisibility needs a single child layout slot, matching what each
 *  call site's previous bare `if (expanded) { ... }` block implicitly required anyway. */
@Composable
fun AnimatedExpand(visible: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(tween(250, easing = LinearOutSlowInEasing)) + fadeIn(tween(250)),
        exit = shrinkVertically(tween(150, easing = FastOutLinearInEasing)) + fadeOut(tween(150)),
    ) {
        Column { content() }
    }
}
```

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If `androidx.compose.animation.AnimatedVisibility` is unresolved, add `implementation(libs.androidx.compose.animation)` (or the BOM-versioned `androidx.compose.animation:animation` artifact) to `app/build.gradle.kts`'s dependencies block and to `gradle/libs.versions.toml`'s `[libraries]` section following the existing `androidx-compose-*` alias pattern, then retry -- Material3 pulls in `animation-core` transitively but not necessarily the full `animation` artifact that hosts `AnimatedVisibility`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/common/AnimatedExpand.kt
git commit -m "Add shared AnimatedExpand composable for expand/collapse transitions"
```

(If Step 2 required a dependency change, also stage `app/build.gradle.kts` and `gradle/libs.versions.toml` in this commit.)

---

### Task 2: Apply to LogScreen's three row types

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt`

**Interfaces:**
- Consumes: `AnimatedExpand` (Task 1).

- [ ] **Step 1: Add the import**

```kotlin
import com.lsing.timego.ui.common.AnimatedExpand
```

- [ ] **Step 2: Replace `StrengthLogRow`'s expand block**

Change:
```kotlin
    ExerciseCard(visual.accent) {
        ExerciseRowHeader(exerciseName, visual.icon, visual.accent, expanded) { expanded = !expanded }
        if (expanded) {
            if (suggestion != null) {
```
to:
```kotlin
    ExerciseCard(visual.accent) {
        ExerciseRowHeader(exerciseName, visual.icon, visual.accent, expanded) { expanded = !expanded }
        AnimatedExpand(expanded) {
            if (suggestion != null) {
```
And change the matching closing brace: the block currently ends with two closing braces before `}` that closes `ExerciseCard` -- i.e. `if (expanded) { ... }` becomes `AnimatedExpand(expanded) { ... }` with identical inner content and identical brace nesting depth, so only the `if (expanded)` line and its condition change; the closing `}` for that block stays exactly where it was.

- [ ] **Step 3: Replace `CardioLogRow`'s expand block**

Change:
```kotlin
    ExerciseCard(visual.accent) {
        ExerciseRowHeader(exerciseName, visual.icon, visual.accent, expanded) { expanded = !expanded }
        if (expanded) {
            if (duration != null && duration > 0) {
```
to:
```kotlin
    ExerciseCard(visual.accent) {
        ExerciseRowHeader(exerciseName, visual.icon, visual.accent, expanded) { expanded = !expanded }
        AnimatedExpand(expanded) {
            if (duration != null && duration > 0) {
```

- [ ] **Step 4: Replace `HoldLogRow`'s expand block**

Change:
```kotlin
    ExerciseCard(visual.accent) {
        ExerciseRowHeader(exerciseName, visual.icon, visual.accent, expanded) { expanded = !expanded }
        if (expanded) {
            if (suggestion != null) {
```
to:
```kotlin
    ExerciseCard(visual.accent) {
        ExerciseRowHeader(exerciseName, visual.icon, visual.accent, expanded) { expanded = !expanded }
        AnimatedExpand(expanded) {
            if (suggestion != null) {
```

- [ ] **Step 5: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt
git commit -m "Animate exercise row expand/collapse in LogScreen"
```

---

### Task 3: Apply to ExerciseListSections' category and muscle-group headers

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/common/ExerciseListSections.kt`

**Interfaces:**
- Consumes: `AnimatedExpand` (Task 1, same file's own package -- `com.lsing.timego.ui.common` -- so no import needed, both are top-level functions in the same package).

- [ ] **Step 1: Replace the category-level expand block**

Change:
```kotlin
            if (expanded) {
                // Sorted so iteration order is deterministic across recompositions -- a plain
                // HashMap's order isn't guaranteed, and combined with key() below, an unstable
                // order would still churn which composable slot each group lands in.
                val byMuscleGroup = remember(inCategory) {
                    inCategory.groupBy { it.muscleGroups.firstOrNull() ?: "OTHER" }.toSortedMap()
                }
                byMuscleGroup.forEach { (group, groupExercises) ->
```
to:
```kotlin
            AnimatedExpand(expanded) {
                // Sorted so iteration order is deterministic across recompositions -- a plain
                // HashMap's order isn't guaranteed, and combined with key() below, an unstable
                // order would still churn which composable slot each group lands in.
                val byMuscleGroup = remember(inCategory) {
                    inCategory.groupBy { it.muscleGroups.firstOrNull() ?: "OTHER" }.toSortedMap()
                }
                byMuscleGroup.forEach { (group, groupExercises) ->
```

- [ ] **Step 2: Replace the muscle-group-level expand block**

Change:
```kotlin
                        if (groupExpanded) {
                            groupExercises.forEach { exercise -> itemContent(exercise) }
                        }
```
to:
```kotlin
                        AnimatedExpand(groupExpanded) {
                            groupExercises.forEach { exercise -> itemContent(exercise) }
                        }
```

- [ ] **Step 3: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/common/ExerciseListSections.kt
git commit -m "Animate category/muscle-group header expand/collapse in the exercise library"
```

---

### Task 4: Bottom-nav screen crossfade

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/TimeGoNavHost.kt`

**Interfaces:** none (pure UI, no new state).

- [ ] **Step 1: Add imports**

```kotlin
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
```

- [ ] **Step 2: Add transitions to each `composable(...)` call**

Change:
```kotlin
            composable("log") { com.lsing.timego.ui.log.LogScreen() }
            composable("progress") { com.lsing.timego.ui.progress.ProgressScreen() }
            composable("routines") { com.lsing.timego.ui.routines.RoutinesScreen() }
```
to:
```kotlin
            composable(
                "log",
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(150)) },
            ) { com.lsing.timego.ui.log.LogScreen() }
            composable(
                "progress",
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(150)) },
            ) { com.lsing.timego.ui.progress.ProgressScreen() }
            composable(
                "routines",
                enterTransition = { fadeIn(tween(200)) },
                exitTransition = { fadeOut(tween(150)) },
            ) { com.lsing.timego.ui.routines.RoutinesScreen() }
```

- [ ] **Step 3: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If the `composable(route, enterTransition = ..., exitTransition = ...) { }` overload isn't resolved, confirm `navigationCompose` in `gradle/libs.versions.toml` is at least `2.7.0` (it's pinned to `2.9.8`, well above the minimum) and that the import is `androidx.navigation.compose.composable`, not a same-named symbol from elsewhere.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/TimeGoNavHost.kt
git commit -m "Add crossfade transitions between bottom-nav screens"
```

---

### Task 5: Progress screen PR/curve content swap

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt`

**Interfaces:** none (pure UI, no new state).

- [ ] **Step 1: Add imports**

```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
```

- [ ] **Step 2: Wrap the PR stat-tile row in `AnimatedContent`**

Change:
```kotlin
                val selectedExercise = exercisesWithRecords[selectedIndex]
                val exerciseRecords = recordsByExercise[selectedExercise.id].orEmpty()
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        Text(selectedExercise.name, style = MaterialTheme.typography.titleSmall)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            val applicableTypes = if (selectedExercise.loggingType == LoggingType.HOLD.name) {
                                listOf(PrType.LONGEST_HOLD)
                            } else {
                                listOf(PrType.HEAVIEST_WEIGHT, PrType.MOST_REPS, PrType.BEST_VOLUME)
                            }
                            applicableTypes.forEach { type ->
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
```
to:
```kotlin
                val selectedExercise = exercisesWithRecords[selectedIndex]
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        Text(selectedExercise.name, style = MaterialTheme.typography.titleSmall)
                        AnimatedContent(
                            targetState = selectedExercise.id,
                            transitionSpec = {
                                (fadeIn(tween(200)) + scaleIn(initialScale = 0.95f, animationSpec = tween(200))) togetherWith
                                    (fadeOut(tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150)))
                            },
                            modifier = Modifier.padding(top = 8.dp),
                        ) { exerciseId ->
                            val exerciseRecords = recordsByExercise[exerciseId].orEmpty()
                            val applicableTypes = if (selectedExercise.loggingType == LoggingType.HOLD.name) {
                                listOf(PrType.LONGEST_HOLD)
                            } else {
                                listOf(PrType.HEAVIEST_WEIGHT, PrType.MOST_REPS, PrType.BEST_VOLUME)
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                applicableTypes.forEach { type ->
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
```
(The `val exerciseRecords = recordsByExercise[selectedExercise.id].orEmpty()` line moves inside the `AnimatedContent` content lambda, keyed on `exerciseId`, so a mid-transition frame reads records for the exercise actually being animated rather than always the latest `selectedExercise`.)

- [ ] **Step 3: Wrap the strength-curve chart in `AnimatedContent`**

Change:
```kotlin
        item {
            if (strengthCurve.isEmpty()) {
                Text("No logged sets yet for this selection.", style = MaterialTheme.typography.bodySmall)
            } else {
                SparklineChart(strengthCurve, modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp))
            }
        }
```
to:
```kotlin
        item {
            val curveKey = if (curveMode == CurveMode.EXERCISE) selectedExerciseId else selectedMuscleGroup
            AnimatedContent(
                targetState = curveKey,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(initialScale = 0.95f, animationSpec = tween(200))) togetherWith
                        (fadeOut(tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150)))
                },
            ) {
                if (strengthCurve.isEmpty()) {
                    Text("No logged sets yet for this selection.", style = MaterialTheme.typography.bodySmall)
                } else {
                    SparklineChart(strengthCurve, modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp))
                }
            }
        }
```
(`curveKey`'s type is `Any?` -- `selectedExerciseId: Long?` and `selectedMuscleGroup: String?` are different types, but `AnimatedContent`'s `targetState` only needs `equals`/`hashCode`, which both `Long?` and `String?` provide. No cast needed.)

- [ ] **Step 4: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt
git commit -m "Animate Progress screen PR and strength-curve content on selection change"
```

---

### Task 6: Full verification and on-device check

**Files:** none (verification only)

- [ ] **Step 1: Full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests still passing (no test in this codebase touches Compose UI directly, so none should be affected by this plan's changes).

- [ ] **Step 2: Full debug build and install**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL, "Installed on 1 device."

- [ ] **Step 3: On-device verification (hand off to the user)**

Ask the user to: expand and collapse a Log screen exercise row and confirm it animates smoothly instead of cutting instantly; expand/collapse a library category header and a muscle-group sub-header the same way; switch between the Log/Progress/Routines bottom-nav tabs and confirm a crossfade instead of a hard cut; on Progress, change the Personal Records wheel picker's selected exercise and confirm the PR tiles fade/scale-transition; switch the Strength Curve between "This exercise" and "Muscle group" modes (and change the wheel-picker selection within each) and confirm the chart transitions smoothly.

- [ ] **Step 4: Update the vault project note**

Add a session entry to `C:\Users\lsing\.claude\obsidian_demo\Projects\TimeGo - Gym Progress Tracker.md` recording that motion polish shipped: installed the `ui-ux-pro-max` Claude Code plugin, used its UX-guideline queries (not its generic style recommendation, which was declined) to find and fix two real motion gaps -- animated expand/collapse (Log rows, library headers) and animated screen/content transitions (bottom-nav, Progress PR/curve swaps).

- [ ] **Step 5: Verify git state**

```bash
cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"
git status
git log --oneline -6
```

Expected: five commits from Tasks 1-5 visible, working tree clean.

---

## Self-Review Notes

- **Spec coverage**: Spec item 1 (row/section expand-collapse) -> Tasks 1, 2, 3. Spec item 2 (bottom-nav crossfade) -> Task 4. Spec item 3 (Progress PR/curve swap) -> Task 5. Verification -> Task 6.
- **Type consistency checked**: `AnimatedExpand(visible: Boolean, modifier: Modifier, content: @Composable () -> Unit)` (Task 1) is called identically (`AnimatedExpand(expanded) { ... }`, no modifier override) at all five sites across Tasks 2 and 3. `AnimatedContent`'s `targetState` in Task 5 uses `selectedExercise.id: Long` for the PR-tile swap and a nullable `Any?`-inferred `curveKey` for the strength-curve swap -- both valid since `AnimatedContent` only requires the type support equality, not a common supertype across the two separate call sites.
- **No placeholder scan issues**: every step shows the actual before/after code, not a description of the change.
