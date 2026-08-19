# Frequency-Based Exercise-List Ordering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Do not execute this plan until its spec has been reviewed and approved.** The spec
> (`2026-08-19-timego-frequency-exercise-ordering-design.md`) is marked "proposed," not "approved" —
> unlike the piece A/E plans, this one was written without a clarifying-questions round with the user.

**Goal:** Sort the exercise picker (both the freeform/routine Log screen picker and the Routine
builder picker) by all-time usage frequency within each existing muscle-group section, instead of
seed/insertion order, so frequently-logged exercises surface first.

**Architecture:** One new domain function counts non-warmup working sets per exercise; a second sorts
an exercise list by that count (descending, alphabetical tiebreak). Both call sites pre-sort their
`exercises` list before handing it to the existing `ExerciseSections` composable, which is otherwise
untouched — it already preserves input order internally.

**Tech Stack:** Kotlin, plain domain functions, JUnit.

**Spec:** `docs/superpowers/specs/2026-08-19-timego-frequency-exercise-ordering-design.md`

## Global Constraints

- No recency weighting in v1 — all-time set counts only (explicit spec decision, flagged for review).
- `ExerciseSections` itself is not modified — sorting happens at the two call sites only.
- Warmup sets and CARDIO/WARMUP-category exercises never count toward usage frequency.

---

### Task 1: `exerciseUsageFrequency` + `exercisesRankedByFrequency`

**Files:**
- Create: `app/src/main/java/com/lsing/timego/domain/ExerciseUsageFrequency.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/ExerciseUsageFrequencyTest.kt`

**Interfaces:**
- Produces: `fun exerciseUsageFrequency(setLogs: List<SetLog>, exercisesById: Map<Long, Exercise>): Map<Long, Int>`,
  `fun exercisesRankedByFrequency(exercises: List<Exercise>, usageCounts: Map<Long, Int>): List<Exercise>`.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseUsageFrequencyTest {
    private val benchPress = Exercise(id = 1, name = "Bench Press", muscleGroups = listOf("CHEST"), isCustom = false)
    private val squat = Exercise(id = 2, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false)
    private val warmupJog = Exercise(id = 3, name = "Jog", muscleGroups = listOf("FULL_BODY"), isCustom = false, category = ExerciseCategory.WARMUP.name)
    private val exercisesById = mapOf(1L to benchPress, 2L to squat, 3L to warmupJog)

    private fun set(exerciseId: Long, isWarmup: Boolean = false) =
        SetLog(sessionId = 1, exerciseId = exerciseId, weightKg = 20.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0, isWarmup = isWarmup)

    @Test
    fun `counts non-warmup working sets per exercise`() {
        val sets = listOf(set(1), set(1), set(2))
        val counts = exerciseUsageFrequency(sets, exercisesById)
        assertEquals(2, counts[1L])
        assertEquals(1, counts[2L])
    }

    @Test
    fun `excludes warmup-flagged sets`() {
        val sets = listOf(set(1, isWarmup = true), set(1))
        assertEquals(1, exerciseUsageFrequency(sets, exercisesById)[1L])
    }

    @Test
    fun `excludes WARMUP-category exercises even when isWarmup is false`() {
        val sets = listOf(set(3, isWarmup = false))
        assertEquals(null, exerciseUsageFrequency(sets, exercisesById)[3L])
    }

    @Test
    fun `exercisesRankedByFrequency sorts descending by count then alphabetically`() {
        val exercises = listOf(benchPress, squat)
        val ranked = exercisesRankedByFrequency(exercises, mapOf(2L to 5, 1L to 5))
        assertEquals(listOf(benchPress, squat), ranked) // tie -> alphabetical: "Bench Press" < "Squat"
    }

    @Test
    fun `exercisesRankedByFrequency puts never-used exercises last, alphabetical among themselves`() {
        val ranked = exercisesRankedByFrequency(listOf(squat, benchPress), mapOf(2L to 3))
        assertEquals(listOf(squat, benchPress), ranked)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.ExerciseUsageFrequencyTest" -q`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Implement**

```kotlin
package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.SetLog

/** Count of non-warmup, non-cardio/warmup-category working sets ever logged per exercise, all-time
 *  -- the ranking signal for exercise-list ordering. No recency weighting: an exercise logged 40
 *  times two years ago still outranks one logged twice last week. Revisit with a recency-weighted
 *  variant only if that proves wrong in practice. */
fun exerciseUsageFrequency(setLogs: List<SetLog>, exercisesById: Map<Long, Exercise>): Map<Long, Int> =
    setLogs
        .filter { log ->
            !log.isWarmup && exercisesById[log.exerciseId]?.category?.let {
                it != ExerciseCategory.WARMUP.name && it != ExerciseCategory.CARDIO.name
            } == true
        }
        .groupingBy { it.exerciseId }
        .eachCount()

/** Sorts [exercises] by [usageCounts] descending, ties (including never-used exercises, absent from
 *  the map) broken alphabetically by name. ExerciseSections preserves whatever order it's given, so
 *  sorting here is sufficient to reorder both its grouped sections and its flat search results. */
fun exercisesRankedByFrequency(exercises: List<Exercise>, usageCounts: Map<Long, Int>): List<Exercise> =
    exercises.sortedWith(compareByDescending<Exercise> { usageCounts[it.id] ?: 0 }.thenBy { it.name })
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.ExerciseUsageFrequencyTest" -q`
Expected: PASS, all 5 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/ExerciseUsageFrequency.kt app/src/test/java/com/lsing/timego/domain/ExerciseUsageFrequencyTest.kt
git commit -m "Add exercise usage-frequency ranking for exercise-list ordering"
```

---

### Task 2: Wire ranking into the Log screen's exercise picker

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt:326`

**Interfaces:**
- Consumes: `exerciseUsageFrequency`, `exercisesRankedByFrequency` (Task 1).
- Produces: `LogViewModel.rankedExercises: StateFlow<List<Exercise>>` (or equivalent already-ranked
  list the screen reads instead of the raw `exercises` state).

No automated test — ViewModel/UI wiring, matches this project's existing convention.

- [ ] **Step 1: Add a ranked-exercises StateFlow to `LogViewModel`**

Add alongside the existing `displayedExercises`/`allExercises` state: collect `repository.setLogs`
and `repository.exercises` together (same `combine` pattern already used elsewhere in this file),
compute `exerciseUsageFrequency` once, and expose
`_displayedExercises.value = exercisesRankedByFrequency(existingFilteredList, usageCounts)` at the
point `refreshDisplayedExercises()` already assembles its result — ranking is applied as the final
step after the existing routine-filter logic, not instead of it.

- [ ] **Step 2: Confirm `LogScreen.kt:326`'s call site needs no change**

`ExerciseSections(exercises = exercises) { ... }` already receives whatever `displayedExercises`
resolves to — since Step 1 makes that list pre-ranked, no call-site edit is needed here. (Verify this
during implementation; if `LogScreen.kt` reads a *different* state than `displayedExercises`, adjust
accordingly.)

- [ ] **Step 3: Build and verify**

Run: `.\gradlew.bat compileDebugKotlin -q`
Expected: succeeds.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt
git commit -m "Rank the Log screen's exercise picker by usage frequency"
```

---

### Task 3: Wire ranking into the Routine builder's exercise picker

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/routines/RoutinesViewModel.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/routines/RoutineFormDialog.kt:90`

**Interfaces:**
- Consumes: `exerciseUsageFrequency`, `exercisesRankedByFrequency` (Task 1).

No automated test, same reasoning as Task 2.

- [ ] **Step 1: Add ranked exercises to `RoutinesViewModel`**

`RoutinesViewModel` already collects `repository.exercises` into `_exercises`. Add a
`repository.setLogs` collector (or a one-shot `repository.allSetLogs()` fetch, matching
`refreshUntrainedGroups`'s existing pattern in the same file), compute `exerciseUsageFrequency`, and
re-derive `_exercises.value` via `exercisesRankedByFrequency` before it's exposed — `exercises:
StateFlow<Exercise>` is what `RoutineFormDialog` already reads (`RoutinesScreen.kt`'s
`exercises = exercises` prop already flows into `RoutineFormDialog`'s own `exercises` param).

- [ ] **Step 2: Confirm `RoutineFormDialog.kt:90`'s call site needs no change**

Same reasoning as Task 2 Step 2 — verify during implementation.

- [ ] **Step 3: Build and verify**

Run: `.\gradlew.bat testDebugUnitTest -q`
Expected: full suite still green (this touches ViewModel state derivation, not domain logic, so no
new failures expected, but confirm nothing downstream of `_exercises` broke).

- [ ] **Step 4: Install and manually verify on-device**

Run: `.\gradlew.bat installDebug -q`

Open the Log screen's exercise picker and the Routine builder's exercise picker; confirm a
frequently-logged exercise (check against real device data) appears near the top of its
muscle-group section in both.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/routines/RoutinesViewModel.kt
git commit -m "Rank the Routine builder's exercise picker by usage frequency"
```

---

## Self-Review

**Spec coverage:** Section 1 (ranking signal) → Task 1. Section 2 (pre-sort, no `ExerciseSections`
change) → Tasks 2/3 explicitly confirm no changes needed there. Section 3 (both call sites) → Tasks 2
and 3, one each. Out-of-scope items (recency weighting, cross-group ranking, ML, search-relevance
blending) — correctly untouched.

**Placeholder scan:** Tasks 2/3 include a "confirm during implementation" verification step rather
than a hard claim about exact call-site line contents, since `LogViewModel.kt`/`RoutinesViewModel.kt`
have been edited several times this session and their exact current structure should be re-read at
execution time, not assumed from this plan's authoring moment — this is a deliberate hedge, not a
placeholder for missing design.

**Type consistency:** `exerciseUsageFrequency`/`exercisesRankedByFrequency` signatures match between
Task 1's implementation and Tasks 2/3's usage.
