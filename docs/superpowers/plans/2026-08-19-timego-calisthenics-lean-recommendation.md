# Calisthenics-Lean Recommendation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Do not execute this plan until its spec has been reviewed and approved.** The spec
> (`2026-08-19-timego-calisthenics-lean-recommendation-design.md`) is marked "proposed," not
> "approved."

**Goal:** Add a Routines-page Strength/Balanced/Calisthenics preference, and use it (plus usage
frequency, from the sibling exercise-ordering plan) to suggest one specific exercise on the landing
page's recommendation card, instead of just a muscle-group name.

**Architecture:** A new `TrainingLean` enum persisted via `SettingsRepository` (same DataStore
pattern as `holdDelaySeconds`). A new domain function picks one exercise for the recommended muscle
groups, filtered by lean (soft filter, falls back to unfiltered) and ranked by least-used. Wired into
`LandingSummary` and rendered on the Log screen's recommendation card.

**Tech Stack:** Kotlin, Preferences DataStore, Jetpack Compose, JUnit.

**Spec:** `docs/superpowers/specs/2026-08-19-timego-calisthenics-lean-recommendation-design.md`

## Global Constraints

- The lean preference is a **soft filter** — it must never cause the recommendation to disappear.
  Falling back to the unfiltered candidate set when the leaned category has zero matches is not
  optional.
- Reuses `exerciseUsageFrequency` from the sibling exercise-ordering plan/spec rather than
  reimplementing usage counting — if that plan hasn't landed yet, implement
  `exerciseUsageFrequency` as part of Task 2 here instead of duplicating it later.
- Suggested-exercise display is text-only in v1 — no tap/navigation action (see spec's Out of scope).

---

### Task 1: `TrainingLean` preference in `SettingsRepository`

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SettingsRepository.kt`
- Test: `app/src/test/java/com/lsing/timego/data/SettingsRepositoryTest.kt` (create if it doesn't
  exist yet; this project's data layer currently has no tests per its own documented convention —
  if adding real DataStore test infra is out of reach without `androidx.datastore` test
  dependencies already present, skip automated testing here and note it explicitly rather than
  fabricate a test that doesn't actually exercise DataStore.)

**Interfaces:**
- Produces: `enum class TrainingLean { STRENGTH, BALANCED, CALISTHENICS }`,
  `SettingsRepository.trainingLean: Flow<TrainingLean>`, `suspend fun setTrainingLean(lean: TrainingLean)`.

- [ ] **Step 1: Add the enum and DataStore key**

In `SettingsRepository.kt` (or a new small `TrainingLean.kt` in `data/` if the project's file-size
convention prefers splitting — check the existing file's length first):

```kotlin
enum class TrainingLean { STRENGTH, BALANCED, CALISTHENICS }
```

- [ ] **Step 2: Add the Flow and setter**

```kotlin
    val trainingLean: Flow<TrainingLean> = context.settingsDataStore.data.map { prefs ->
        prefs[TRAINING_LEAN_KEY]?.let { runCatching { TrainingLean.valueOf(it) }.getOrNull() } ?: TrainingLean.BALANCED
    }

    suspend fun setTrainingLean(lean: TrainingLean) {
        context.settingsDataStore.edit { prefs -> prefs[TRAINING_LEAN_KEY] = lean.name }
    }
```

Add the key to the companion object, next to `HOLD_DELAY_SECONDS_KEY`:

```kotlin
        private val TRAINING_LEAN_KEY = stringPreferencesKey("training_lean")
```

(Add the `androidx.datastore.preferences.core.stringPreferencesKey` import if not already present.)

- [ ] **Step 3: Build and verify**

Run: `.\gradlew.bat compileDebugKotlin -q`
Expected: succeeds.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SettingsRepository.kt
git commit -m "Add TrainingLean preference (Strength/Balanced/Calisthenics)"
```

---

### Task 2: `suggestedExerciseFor` domain function

**Files:**
- Create: `app/src/main/java/com/lsing/timego/domain/SuggestedExercise.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/SuggestedExerciseTest.kt`

**Interfaces:**
- Consumes: `TrainingLean` (Task 1), `exerciseUsageFrequency` (sibling exercise-ordering plan — if
  not yet landed, define it here instead; see Global Constraints).
- Produces: `fun suggestedExerciseFor(targetGroups: Set<String>, exercises: List<Exercise>, lean: TrainingLean, usageCounts: Map<Long, Int>): Exercise?`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SuggestedExerciseTest {
    private val benchPress = Exercise(id = 1, name = "Bench Press", muscleGroups = listOf("CHEST"), isCustom = false)
    private val pushUp = Exercise(id = 2, name = "Push-Up", muscleGroups = listOf("CHEST"), isCustom = false, category = ExerciseCategory.CALISTHENICS.name)
    private val squat = Exercise(id = 3, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false)
    private val exercises = listOf(benchPress, pushUp, squat)

    @Test
    fun `BALANCED returns least-used matching exercise regardless of category`() {
        val result = suggestedExerciseFor(setOf("CHEST"), exercises, TrainingLean.BALANCED, mapOf(1L to 5, 2L to 1))
        assertEquals(pushUp, result)
    }

    @Test
    fun `STRENGTH excludes calisthenics when a non-calisthenics match exists`() {
        val result = suggestedExerciseFor(setOf("CHEST"), exercises, TrainingLean.STRENGTH, emptyMap())
        assertEquals(benchPress, result)
    }

    @Test
    fun `CALISTHENICS excludes non-calisthenics when a calisthenics match exists`() {
        val result = suggestedExerciseFor(setOf("CHEST"), exercises, TrainingLean.CALISTHENICS, emptyMap())
        assertEquals(pushUp, result)
    }

    @Test
    fun `falls back to unfiltered candidates when the leaned category has no match`() {
        val result = suggestedExerciseFor(setOf("QUADS"), exercises, TrainingLean.CALISTHENICS, emptyMap())
        assertEquals(squat, result)
    }

    @Test
    fun `returns null when no exercise matches targetGroups at all`() {
        assertNull(suggestedExerciseFor(setOf("TRAPS"), exercises, TrainingLean.BALANCED, emptyMap()))
    }

    @Test
    fun `ties broken by name`() {
        val result = suggestedExerciseFor(setOf("CHEST"), exercises, TrainingLean.BALANCED, mapOf(1L to 3, 2L to 3))
        assertEquals(benchPress, result) // "Bench Press" < "Push-Up"
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.SuggestedExerciseTest" -q`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Implement**

```kotlin
package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory

/** Picks one specific exercise to suggest for [targetGroups], given the current [lean] preference
 *  and [usageCounts] (from exerciseUsageFrequency). Preference is a soft filter: falls back to the
 *  full candidate set when the leaned category has zero matches, rather than returning null.
 *  Prefers the LEAST-used candidate (ties broken by name) to surface variety. Returns null only
 *  when [targetGroups] matches nothing in the library at all. */
fun suggestedExerciseFor(
    targetGroups: Set<String>,
    exercises: List<Exercise>,
    lean: TrainingLean,
    usageCounts: Map<Long, Int>,
): Exercise? {
    val matching = exercises.filter { it.muscleGroups.any { g -> g in targetGroups } }
    if (matching.isEmpty()) return null
    val leaned = when (lean) {
        TrainingLean.STRENGTH -> matching.filter { it.category != ExerciseCategory.CALISTHENICS.name }
        TrainingLean.CALISTHENICS -> matching.filter { it.category == ExerciseCategory.CALISTHENICS.name }
        TrainingLean.BALANCED -> matching
    }
    val candidates = leaned.ifEmpty { matching }
    return candidates.minWithOrNull(compareBy<Exercise> { usageCounts[it.id] ?: 0 }.thenBy { it.name })
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.SuggestedExerciseTest" -q`
Expected: PASS, all 6 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/SuggestedExercise.kt app/src/test/java/com/lsing/timego/domain/SuggestedExerciseTest.kt
git commit -m "Add suggestedExerciseFor: lean-aware, least-used exercise pick"
```

---

### Task 3: Wire into `LandingSummary` and the recommendation card

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt`

**Interfaces:**
- Consumes: `suggestedExerciseFor` (Task 2), `SettingsRepository.trainingLean` (Task 1).
- Produces: `LandingSummary.suggestedExercise: Exercise?`.

No automated test — ViewModel/UI wiring.

- [ ] **Step 1: Add `suggestedExercise` to `LandingSummary`**

```kotlin
data class LandingSummary(
    val lastSession: LastSessionSummary?,
    val recommendedMuscleGroups: List<String>,
    val suggestedExercise: Exercise?,
)
```

- [ ] **Step 2: Compute it in `refreshLandingSummary`**

After `recommended` is computed (the existing `expandMuscleGroupRegions(recommendedSeeds).toList()`
line), collect the current `trainingLean` value and `exerciseUsageFrequency`, then call
`suggestedExerciseFor(recommended.toSet(), allExercises, lean, usageCounts)`. Pass the result into
`LandingSummary(...)`'s new field.

- [ ] **Step 3: Render it on the recommendation card**

In `LogScreen.kt`'s recommendation section (the `Surface` block showing
`formatMuscleGroupList(summary.recommendedMuscleGroups)`), add one line beneath it when
`summary.suggestedExercise != null`:

```kotlin
summary.suggestedExercise?.let { exercise ->
    Text(
        "Try: ${exercise.name}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.ExtraSmall),
    )
}
```

- [ ] **Step 4: Build and verify**

Run: `.\gradlew.bat testDebugUnitTest -q`
Expected: full suite green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt
git commit -m "Show a specific suggested exercise on the recommendation card"
```

---

### Task 4: Lean preference UI on the Routines page

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/routines/RoutinesViewModel.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/routines/RoutinesScreen.kt`

**Interfaces:**
- Consumes: `SettingsRepository.trainingLean`/`setTrainingLean` (Task 1).

No automated test — UI wiring.

- [ ] **Step 1: Expose `trainingLean` state and a setter in `RoutinesViewModel`**

Same pattern as `holdDelaySeconds`:

```kotlin
    private val _trainingLean = MutableStateFlow(TrainingLean.BALANCED)
    val trainingLean: StateFlow<TrainingLean> = _trainingLean.asStateFlow()

    fun setTrainingLean(lean: TrainingLean) {
        viewModelScope.launch { settingsRepository.setTrainingLean(lean) }
    }
```

Plus the corresponding `init` collector: `viewModelScope.launch { settingsRepository.trainingLean.collect { _trainingLean.value = it } }`.

- [ ] **Step 2: Add a 3-option row to the "Workout settings" section**

In `RoutinesScreen.kt`, below the existing hold-delay `Row`, add a segmented row of three `FilterChip`
(or `AssistChip` with a selected-state tint) options — "Strength", "Balanced", "Calisthenics" —
calling `viewModel.setTrainingLean(...)` on tap, with the currently-selected option visually
distinguished (matches Material3's `FilterChip(selected = ...)` idiom).

- [ ] **Step 3: Build, test, install**

Run: `.\gradlew.bat testDebugUnitTest installDebug -q`
Expected: suite green, install succeeds.

- [ ] **Step 4: Manually verify on-device**

Toggle each lean option; confirm the Log screen's "Try: <exercise>" line changes accordingly and
never disappears while a recommendation exists.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/routines/RoutinesViewModel.kt app/src/main/java/com/lsing/timego/ui/routines/RoutinesScreen.kt
git commit -m "Add Strength/Balanced/Calisthenics preference to the Routines page"
```

---

## Self-Review

**Spec coverage:** Section 1 (stored preference) → Tasks 1, 4. Section 2 (pick function) → Task 2.
Section 3 (landing integration) → Task 3. Out-of-scope items (tap-to-log, favorites, extending lean
into pieces C/E, distinct "never tried" tier) — correctly untouched.

**Placeholder scan:** No TBD/TODO. Task 1's test step includes an explicit fallback instruction (skip
automated DataStore testing if infra isn't present, rather than fabricate a fake test) — this is a
scoped hedge given the project's documented lack of data-layer test infrastructure, not a
placeholder for undesigned behavior.

**Type consistency:** `TrainingLean` used identically across Tasks 1, 2, 4. `suggestedExerciseFor`
signature matches between Task 2's implementation and Task 3's call site.
