# TimeGo Suggester Plateau-Detection Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade `RuleBasedOverloadSuggester`/`RuleBasedHoldSuggester` from a 3-branch last-2-sets rule into a plateau-aware suggester (PROGRESSING/PLATEAUING/REGRESSING, computed from a 5-set rolling window) with a calisthenics progression-tier suggestion for HOLD exercises that have hit a rep/duration ceiling — a rules-only base layer for a future ML suggester, not the ML model itself.

**Architecture:** A new shared `classifyPlateauStatus` helper (plain Kotlin, domain layer) computes `PlateauStatus` from a metric-value history and hit-target flags — used by both suggesters, which differ only in which metric they extract (estimated 1RM vs hold duration) and their calisthenics-specific behavior. `OverloadSuggestion`/`HoldSuggestion` gain a `plateauStatus` field. `HoldSuggester.suggestNext` gains an `exerciseName: String` parameter (new, not on `OverloadSuggester` — see Global Constraints) so it can consult a static progression-tier map; `LogViewModel` is updated to pass the exercise name through at its one call site.

**Tech Stack:** Kotlin, JUnit.

## Global Constraints

- Plain Kotlin domain logic, no Android dependency — same as the existing suggesters. Full TDD.
- **Existing REGRESSING behavior must not change**: the "missed target twice in a row → deload 10%, same note text" behavior and its exact note strings (`"Deload: missed target reps twice in a row"` / `"Deload: missed target hold twice in a row"`) stay byte-identical — existing tests assert on them and must keep passing unmodified in their assertions (only the `suggestNext` call sites change where a new parameter is added).
- **Window size**: last 5 logged sets (`WINDOW_SIZE = 5`, a named constant). Fewer than 5 → fall back to today's simpler last-2-sets-missed check (REGRESSING if both missed, otherwise PROGRESSING; never PLATEAUING without enough history).
- **Interface asymmetry is intentional**: only `HoldSuggester.suggestNext` gains the `exerciseName: String` parameter. The calisthenics progression-tier map (spec Section 4) only has entries for HOLD-type exercises in the current library (Planche/Lever/Human Flag tiers are all `loggingType = HOLD` in `SeedExercises.kt`) — there are no WEIGHT_REPS calisthenics progression chains to look up yet, so adding the same parameter to `OverloadSuggester` would be dead code. `OverloadSuggester`'s interface is unchanged. If a rep-based progression chain is added to the library later, extend `OverloadSuggester` the same way then.
- `HOLD` exercises are always `CALISTHENICS` (enforced by the `every HOLD exercise is CALISTHENICS` test added in the library-import work) — so `HoldSuggester` doesn't need a separate category check before consulting the progression map, just the exercise name.

---

### Task 1: `PlateauStatus` enum and shared classification helper

**Files:**
- Create: `app/src/main/java/com/lsing/timego/domain/PlateauDetection.kt`
- Create: `app/src/test/java/com/lsing/timego/domain/PlateauDetectionTest.kt`

**Interfaces:**
- Produces: `enum class PlateauStatus { PROGRESSING, PLATEAUING, REGRESSING }`; `fun classifyPlateauStatus(recentValues: List<Double>, hitTargetFlags: List<Boolean>): PlateauStatus` — both ordered oldest-first, same size, consumed by Task 2 (`RuleBasedOverloadSuggester`) and Task 3 (`RuleBasedHoldSuggester`).

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PlateauDetectionTest {
    @Test
    fun `fewer than 5 entries with last two missed is REGRESSING`() {
        val values = listOf(60.0, 60.0)
        val hits = listOf(false, false)
        assertEquals(PlateauStatus.REGRESSING, classifyPlateauStatus(values, hits))
    }

    @Test
    fun `fewer than 5 entries not both missed is PROGRESSING`() {
        val values = listOf(60.0)
        val hits = listOf(true)
        assertEquals(PlateauStatus.PROGRESSING, classifyPlateauStatus(values, hits))
    }

    @Test
    fun `fewer than 5 entries with one miss is PROGRESSING not REGRESSING`() {
        val values = listOf(60.0, 62.0)
        val hits = listOf(false, true)
        assertEquals(PlateauStatus.PROGRESSING, classifyPlateauStatus(values, hits))
    }

    @Test
    fun `five entries with clear uptrend is PROGRESSING`() {
        val values = listOf(60.0, 61.0, 62.0, 63.0, 65.0)
        val hits = listOf(true, true, true, true, true)
        assertEquals(PlateauStatus.PROGRESSING, classifyPlateauStatus(values, hits))
    }

    @Test
    fun `five entries flat with last two missed is REGRESSING`() {
        val values = listOf(60.0, 60.0, 60.0, 58.0, 57.0)
        val hits = listOf(true, true, true, false, false)
        assertEquals(PlateauStatus.REGRESSING, classifyPlateauStatus(values, hits))
    }

    @Test
    fun `five entries flat oscillating with no clear trend is PLATEAUING`() {
        val values = listOf(60.0, 61.0, 59.0, 61.0, 60.0)
        val hits = listOf(true, true, true, true, true)
        assertEquals(PlateauStatus.PLATEAUING, classifyPlateauStatus(values, hits))
    }

    @Test
    fun `six entries only considers the last five`() {
        // Oldest value (50.0) would drag the average down if included -- it must be windowed out.
        val values = listOf(50.0, 60.0, 61.0, 59.0, 61.0, 60.0)
        val hits = listOf(true, true, true, true, true, true)
        assertEquals(PlateauStatus.PLATEAUING, classifyPlateauStatus(values, hits))
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.domain.PlateauDetectionTest"`
Expected: FAIL (compile error — `PlateauStatus`/`classifyPlateauStatus` don't exist yet).

- [ ] **Step 3: Implement `PlateauDetection.kt`**

```kotlin
package com.lsing.timego.domain

enum class PlateauStatus { PROGRESSING, PLATEAUING, REGRESSING }

private const val WINDOW_SIZE = 5

/** Classifies training trend from a metric history (oldest first) and whether each entry hit its
 *  target -- shared by [RuleBasedOverloadSuggester] (estimated 1RM) and [RuleBasedHoldSuggester]
 *  (hold duration), which differ only in which metric they extract. REGRESSING (last two entries
 *  both missed target) is checked first regardless of history length -- this is the same trigger
 *  as the pre-upgrade suggesters' deload rule, now labeled. With fewer than [WINDOW_SIZE] entries
 *  there isn't enough history for a trend, so anything short of REGRESSING falls back to
 *  PROGRESSING (today's original behavior) rather than ever claiming PLATEAUING. With
 *  [WINDOW_SIZE]+ entries, only the most recent [WINDOW_SIZE] are considered (older entries are
 *  windowed out, not averaged in) -- PROGRESSING if the latest value is at or above the average of
 *  the preceding entries in the window, or if the window shows a net-upward trend (latest >= the
 *  window's oldest value); PLATEAUING otherwise. */
fun classifyPlateauStatus(recentValues: List<Double>, hitTargetFlags: List<Boolean>): PlateauStatus {
    require(recentValues.size == hitTargetFlags.size) { "recentValues and hitTargetFlags must be the same size" }

    val lastTwoMissed = hitTargetFlags.size >= 2 &&
        !hitTargetFlags[hitTargetFlags.size - 1] &&
        !hitTargetFlags[hitTargetFlags.size - 2]
    if (lastTwoMissed) return PlateauStatus.REGRESSING

    if (recentValues.size < WINDOW_SIZE) return PlateauStatus.PROGRESSING

    val window = recentValues.takeLast(WINDOW_SIZE)
    val precedingAverage = window.dropLast(1).average()
    // Split the window in half (the middle entry, if any, belongs to neither half) and compare
    // averages -- a strict inequality, not a first-vs-last endpoint comparison, so a window that
    // oscillates back to its starting value doesn't get misread as an upward trend.
    val halfSize = window.size / 2
    val firstHalfAverage = window.take(halfSize).average()
    val secondHalfAverage = window.takeLast(halfSize).average()
    val trendingUp = secondHalfAverage > firstHalfAverage
    return if (window.last() >= precedingAverage || trendingUp) PlateauStatus.PROGRESSING else PlateauStatus.PLATEAUING
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.domain.PlateauDetectionTest"`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/PlateauDetection.kt app/src/test/java/com/lsing/timego/domain/PlateauDetectionTest.kt
git commit -m "Add PlateauStatus enum and shared classification helper"
```

---

### Task 2: `RuleBasedOverloadSuggester` plateau-aware decision table

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/OverloadSuggester.kt`
- Modify: `app/src/test/java/com/lsing/timego/domain/OverloadSuggesterTest.kt`

**Interfaces:**
- Consumes: `classifyPlateauStatus(List<Double>, List<Boolean>): PlateauStatus` (Task 1).
- Produces: `OverloadSuggestion` now has a `plateauStatus: PlateauStatus` field — consumed by Task 5 if `LogViewModel` needs it later (not required by this plan's UI scope, but the field exists on the type from this task forward).
- `OverloadSuggester.suggestNext` signature is **unchanged**: `fun suggestNext(history: List<SetPerformance>): OverloadSuggestion?` (see Global Constraints on interface asymmetry).

- [ ] **Step 1: Write the failing tests**

Replace `OverloadSuggesterTest.kt` entirely:

```kotlin
package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverloadSuggesterTest {
    private val suggester = RuleBasedOverloadSuggester()

    @Test
    fun `no history returns null`() {
        assertNull(suggester.suggestNext(emptyList()))
    }

    @Test
    fun `hit target reps suggests weight increase`() {
        val history = listOf(SetPerformance(weightKg = 60.0, reps = 8, targetReps = 8))
        val result = suggester.suggestNext(history)
        assertEquals(62.5, result!!.weightKg, 0.001)
        assertEquals(8, result.reps)
        assertEquals(PlateauStatus.PROGRESSING, result.plateauStatus)
    }

    @Test
    fun `missed target reps suggests same weight plus a rep`() {
        val history = listOf(SetPerformance(weightKg = 60.0, reps = 6, targetReps = 8))
        val result = suggester.suggestNext(history)
        assertEquals(60.0, result!!.weightKg, 0.001)
        assertEquals(7, result.reps)
        assertEquals(PlateauStatus.PROGRESSING, result.plateauStatus)
    }

    @Test
    fun `missed target twice in a row triggers deload`() {
        val history = listOf(
            SetPerformance(weightKg = 60.0, reps = 5, targetReps = 8),
            SetPerformance(weightKg = 60.0, reps = 6, targetReps = 8),
        )
        val result = suggester.suggestNext(history)
        assertEquals(54.0, result!!.weightKg, 0.001)
        assertEquals("Deload: missed target reps twice in a row", result.note)
        assertEquals(PlateauStatus.REGRESSING, result.plateauStatus)
    }

    @Test
    fun `five sets flat oscillating with last set hit target is PLATEAUING and holds weight`() {
        // Estimated 1RM (Epley, weightKg * (1 + reps/30)) alternates 76.0 / 79.1667 / 76.0 /
        // 79.1667 / 76.0 -- every set hits its target (reps == targetReps), so REGRESSING can't
        // trigger, but the window has no net up/down trend (first-half and second-half averages
        // are equal) and the last value (76.0) sits below the preceding average (~77.58).
        val history = listOf(
            SetPerformance(weightKg = 60.0, reps = 8, targetReps = 8),
            SetPerformance(weightKg = 62.5, reps = 8, targetReps = 8),
            SetPerformance(weightKg = 60.0, reps = 8, targetReps = 8),
            SetPerformance(weightKg = 62.5, reps = 8, targetReps = 8),
            SetPerformance(weightKg = 60.0, reps = 8, targetReps = 8),
        )
        val result = suggester.suggestNext(history)
        assertEquals(PlateauStatus.PLATEAUING, result!!.plateauStatus)
        assertEquals(60.0, result.weightKg, 0.001)
        assertEquals(8, result.reps)
        assertEquals(true, result.note.contains("plateau", ignoreCase = true))
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.domain.OverloadSuggesterTest"`
Expected: FAIL (`plateauStatus` doesn't exist on `OverloadSuggestion` yet; the PLATEAUING test fails on old 3-branch logic).

- [ ] **Step 3: Update `OverloadSuggester.kt`**

```kotlin
package com.lsing.timego.domain

data class SetPerformance(val weightKg: Double, val reps: Int, val targetReps: Int)

data class OverloadSuggestion(val weightKg: Double, val reps: Int, val note: String, val plateauStatus: PlateauStatus)

interface OverloadSuggester {
    fun suggestNext(history: List<SetPerformance>): OverloadSuggestion?
}

/** Deterministic, on-device, no ML -- see the v1 spec's "Recommendation Engine" section for why,
 *  and the 2026-08-11 suggester-plateau-upgrade-design spec for why this is the base layer a
 *  future ML model sits on top of rather than the model itself. Plateau status is computed from
 *  a 5-set rolling window of estimated 1RM via [classifyPlateauStatus] -- REGRESSING (last two
 *  sets missed target) still triggers the same 10% deload as before; PROGRESSING keeps the
 *  original hit-target/missed-target branches; PLATEAUING is new -- holds weight and reps flat
 *  for one more session instead of blindly adding weight into a stall. */
class RuleBasedOverloadSuggester : OverloadSuggester {
    override fun suggestNext(history: List<SetPerformance>): OverloadSuggestion? {
        if (history.isEmpty()) return null
        val last = history.last()
        val oneRepMaxes = history.map { estimatedOneRepMax(it.weightKg, it.reps) }
        val hitFlags = history.map { it.reps >= it.targetReps }
        val status = classifyPlateauStatus(oneRepMaxes, hitFlags)

        return when (status) {
            PlateauStatus.REGRESSING -> OverloadSuggestion(
                weightKg = last.weightKg * 0.9,
                reps = last.targetReps,
                note = "Deload: missed target reps twice in a row",
                plateauStatus = status,
            )
            PlateauStatus.PLATEAUING -> OverloadSuggestion(
                weightKg = last.weightKg,
                reps = last.targetReps,
                note = "Plateau: performance has been flat for several sessions -- hold steady one more session before deciding",
                plateauStatus = status,
            )
            PlateauStatus.PROGRESSING -> if (last.reps >= last.targetReps) {
                OverloadSuggestion(
                    weightKg = last.weightKg + 2.5,
                    reps = last.targetReps,
                    note = "Increase weight: hit target reps last time",
                    plateauStatus = status,
                )
            } else {
                OverloadSuggestion(
                    weightKg = last.weightKg,
                    reps = last.reps + 1,
                    note = "Same weight, aim for one more rep",
                    plateauStatus = status,
                )
            }
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.domain.OverloadSuggesterTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/OverloadSuggester.kt app/src/test/java/com/lsing/timego/domain/OverloadSuggesterTest.kt
git commit -m "Make RuleBasedOverloadSuggester plateau-aware"
```

---

### Task 3: `RuleBasedHoldSuggester` plateau-aware decision table with calisthenics progression tiers

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/HoldSuggester.kt`
- Modify: `app/src/test/java/com/lsing/timego/domain/HoldSuggesterTest.kt`

**Interfaces:**
- Consumes: `classifyPlateauStatus(List<Double>, List<Boolean>): PlateauStatus` (Task 1).
- Produces: `HoldSuggestion` now has a `plateauStatus: PlateauStatus` field. `HoldSuggester.suggestNext` signature changes to `fun suggestNext(history: List<HoldPerformance>, exerciseName: String): HoldSuggestion?` — Task 4 (`LogViewModel`) must update its one call site to pass the exercise name.

- [ ] **Step 1: Write the failing tests**

Replace `HoldSuggesterTest.kt` entirely:

```kotlin
package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HoldSuggesterTest {
    private val suggester = RuleBasedHoldSuggester()

    @Test
    fun `no history returns null`() {
        assertNull(suggester.suggestNext(emptyList(), "Plank"))
    }

    @Test
    fun `hit target hold suggests a longer duration`() {
        val history = listOf(HoldPerformance(durationSeconds = 30, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, "Plank")
        assertEquals(35, result!!.targetDurationSeconds)
        assertEquals(PlateauStatus.PROGRESSING, result.plateauStatus)
    }

    @Test
    fun `missed target hold suggests the same target`() {
        val history = listOf(HoldPerformance(durationSeconds = 20, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, "Plank")
        assertEquals(30, result!!.targetDurationSeconds)
        assertEquals(PlateauStatus.PROGRESSING, result.plateauStatus)
    }

    @Test
    fun `missed target twice in a row triggers deload`() {
        val history = listOf(
            HoldPerformance(durationSeconds = 20, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 22, targetDurationSeconds = 30),
        )
        val result = suggester.suggestNext(history, "Plank")
        assertEquals(27, result!!.targetDurationSeconds)
        assertEquals("Deload: missed target hold twice in a row", result.note)
        assertEquals(PlateauStatus.REGRESSING, result.plateauStatus)
    }

    @Test
    fun `five holds flat oscillating with last hit target is PLATEAUING and holds duration`() {
        // Alternates 30 / 32 / 30 / 32 / 30 -- every hold clears its 30s target, so REGRESSING
        // can't trigger, but the window has no net trend (first-half/second-half averages equal)
        // and the last value (30) sits below the preceding average (31.0).
        val history = listOf(
            HoldPerformance(durationSeconds = 30, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 32, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 30, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 32, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 30, targetDurationSeconds = 30),
        )
        val result = suggester.suggestNext(history, "Plank")
        assertEquals(PlateauStatus.PLATEAUING, result!!.plateauStatus)
        assertEquals(30, result.targetDurationSeconds)
        assertEquals(true, result.note.contains("plateau", ignoreCase = true))
    }

    @Test
    fun `hitting target well past the ceiling on a mapped exercise suggests the next tier`() {
        // 45s held against a 30s target is 1.5x -- the ceiling threshold.
        val history = listOf(HoldPerformance(durationSeconds = 45, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, "Tuck Planche Hold")
        assertEquals(true, result!!.note.contains("Advanced Tuck Planche Hold"))
    }

    @Test
    fun `hitting target without reaching the ceiling on a mapped exercise does not suggest next tier`() {
        val history = listOf(HoldPerformance(durationSeconds = 32, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, "Tuck Planche Hold")
        assertEquals(false, result!!.note.contains("Advanced Tuck Planche Hold"))
    }

    @Test
    fun `ceiling hit on an exercise with no known progression falls through to normal suggestion`() {
        val history = listOf(HoldPerformance(durationSeconds = 45, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, "Plank")
        assertEquals(35, result!!.targetDurationSeconds)
    }

    @Test
    fun `ceiling hit on the top tier of a progression chain falls through to normal suggestion`() {
        val history = listOf(HoldPerformance(durationSeconds = 45, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, "Full Planche Hold")
        assertEquals(35, result!!.targetDurationSeconds)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.domain.HoldSuggesterTest"`
Expected: FAIL (signature mismatch — old `suggestNext(history)` calls don't compile against the new tests, and the new tests reference behavior that doesn't exist yet).

- [ ] **Step 3: Update `HoldSuggester.kt`**

```kotlin
package com.lsing.timego.domain

data class HoldPerformance(val durationSeconds: Int, val targetDurationSeconds: Int)

data class HoldSuggestion(val targetDurationSeconds: Int, val note: String, val plateauStatus: PlateauStatus)

interface HoldSuggester {
    fun suggestNext(history: List<HoldPerformance>, exerciseName: String): HoldSuggestion?
}

/** Static, hand-curated progression chain for the tiered HOLD-type calisthenics families added in
 *  the 2026-08-11 library import (Planche/Front Lever/Back Lever/Human Flag) -- not derived from
 *  muscle tags or any naming heuristic, since most of the library isn't tiered. Every HOLD
 *  exercise is CALISTHENICS (enforced by SeedExercisesTest's "every HOLD exercise is CALISTHENICS"
 *  invariant), so no separate category check is needed before consulting this map. */
private val CALISTHENICS_PROGRESSIONS: Map<String, String> = mapOf(
    "Tuck Planche Hold" to "Advanced Tuck Planche Hold",
    "Advanced Tuck Planche Hold" to "Straddle Planche Hold",
    "Straddle Planche Hold" to "Full Planche Hold",
    "Tuck Front Lever" to "Advanced Tuck Front Lever",
    "Advanced Tuck Front Lever" to "Straddle Front Lever",
    "Straddle Front Lever" to "Front Lever Hold",
    "Tuck Back Lever" to "Straddle Back Lever",
    "Straddle Back Lever" to "Back Lever Hold",
    "Human Flag Tuck" to "Human Flag Straddle",
    "Human Flag Straddle" to "Human Flag Hold",
)

private const val PROGRESSION_CEILING_RATIO = 1.5

/** Same deterministic, no-ML philosophy as [RuleBasedOverloadSuggester], applied to timed holds.
 *  Plateau status is computed from a 5-hold rolling window via [classifyPlateauStatus]. When the
 *  last hold clears [PROGRESSION_CEILING_RATIO]x its target and [exerciseName] has a known next
 *  tier in [CALISTHENICS_PROGRESSIONS], suggests switching to that tier instead of a duration bump
 *  -- the user rarely adds external load to calisthenics, so a duration/weight increase isn't the
 *  natural ceiling-buster here the way it is for STRENGTH exercises. */
class RuleBasedHoldSuggester : HoldSuggester {
    override fun suggestNext(history: List<HoldPerformance>, exerciseName: String): HoldSuggestion? {
        if (history.isEmpty()) return null
        val last = history.last()
        val durations = history.map { it.durationSeconds.toDouble() }
        val hitFlags = history.map { it.durationSeconds >= it.targetDurationSeconds }
        val status = classifyPlateauStatus(durations, hitFlags)

        val nextTier = CALISTHENICS_PROGRESSIONS[exerciseName]
        val clearedCeiling = last.durationSeconds >= last.targetDurationSeconds * PROGRESSION_CEILING_RATIO
        if (status != PlateauStatus.REGRESSING && clearedCeiling && nextTier != null) {
            return HoldSuggestion(
                targetDurationSeconds = last.targetDurationSeconds,
                note = "Consistently hitting target -- try $nextTier next",
                plateauStatus = status,
            )
        }

        return when (status) {
            PlateauStatus.REGRESSING -> HoldSuggestion(
                targetDurationSeconds = (last.targetDurationSeconds * 0.9).toInt(),
                note = "Deload: missed target hold twice in a row",
                plateauStatus = status,
            )
            PlateauStatus.PLATEAUING -> HoldSuggestion(
                targetDurationSeconds = last.targetDurationSeconds,
                note = "Plateau: hold duration has been flat for several sessions -- hold steady one more session before deciding",
                plateauStatus = status,
            )
            PlateauStatus.PROGRESSING -> if (last.durationSeconds >= last.targetDurationSeconds) {
                HoldSuggestion(
                    targetDurationSeconds = last.targetDurationSeconds + 5,
                    note = "Increase hold: hit target last time",
                    plateauStatus = status,
                )
            } else {
                HoldSuggestion(
                    targetDurationSeconds = last.targetDurationSeconds,
                    note = "Same target, aim to hold longer",
                    plateauStatus = status,
                )
            }
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.domain.HoldSuggesterTest"`
Expected: PASS (9 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/HoldSuggester.kt app/src/test/java/com/lsing/timego/domain/HoldSuggesterTest.kt
git commit -m "Make RuleBasedHoldSuggester plateau-aware with calisthenics progression tiers"
```

---

### Task 4: `LogViewModel` integration

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt:107`

**Interfaces:**
- Consumes: `HoldSuggester.suggestNext(history: List<HoldPerformance>, exerciseName: String): HoldSuggestion?` (Task 3's new signature).

- [ ] **Step 1: Update the `holdSuggester.suggestNext` call site**

The current code (around line 104-111 of `LogViewModel.kt`):
```kotlin
            val history = historyByExercise[exercise.id].orEmpty()
            if (exercise.loggingType == LoggingType.HOLD.name) {
                val holdHistory = history.map { HoldPerformance(it.holdSeconds ?: 0, it.targetHoldSeconds ?: 0) }
                holdSuggester.suggestNext(holdHistory)?.let { holdMap[exercise.id] = it }
            } else {
                val performanceHistory = history.map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
                suggester.suggestNext(performanceHistory)?.let { map[exercise.id] = it }
            }
```
becomes:
```kotlin
            val history = historyByExercise[exercise.id].orEmpty()
            if (exercise.loggingType == LoggingType.HOLD.name) {
                val holdHistory = history.map { HoldPerformance(it.holdSeconds ?: 0, it.targetHoldSeconds ?: 0) }
                holdSuggester.suggestNext(holdHistory, exercise.name)?.let { holdMap[exercise.id] = it }
            } else {
                val performanceHistory = history.map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
                suggester.suggestNext(performanceHistory)?.let { map[exercise.id] = it }
            }
```
(`OverloadSuggester`'s call is unchanged — only the `HoldSuggester` call gains the `exercise.name` argument, per Task 3's signature change.)

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt
git commit -m "Wire exercise name through to HoldSuggester for progression-tier lookup"
```

---

### Task 5: Full verification, on-device check, and vault update

**Files:** none (verification and documentation only)

- [ ] **Step 1: Full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests passing (including every pre-existing test elsewhere in the project — this plan doesn't touch any other file).

- [ ] **Step 2: Full debug build and install**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL, "Installed on 1 device."

- [ ] **Step 3: On-device verification (hand off to the user)**

Ask the user to: open Log, start a session, log a few sets for an exercise with existing history and confirm a suggestion still appears with a sensible note; specifically try logging a HOLD exercise from the progression chain (e.g. "Tuck Planche Hold") with a hold well past 1.5x its target and confirm the suggestion note mentions "Advanced Tuck Planche Hold"; confirm nothing crashes and suggestions still render for exercises with little or no history (the `< 5 entries` fallback path).

- [ ] **Step 4: Update the vault project note**

Add a session entry to `C:\Users\lsing\.claude\obsidian_demo\Projects\TimeGo - Gym Progress Tracker.md` recording: suggester plateau-detection upgrade shipped (Sub-project 2), `PlateauStatus` enum (PROGRESSING/PLATEAUING/REGRESSING) computed from a 5-set/hold rolling window, calisthenics progression-tier suggestions for HOLD exercises with a known next tier, `HoldSuggester.suggestNext` gained an `exerciseName` parameter. Note this closes out the two-part "ML for progressive overload" follow-up's rules-only phase — the actual ML model remains a future sub-project blocked on real per-user longitudinal training data (or a viable alternative like an OpenPowerlifting-based bodyweight-normalized model).

- [ ] **Step 5: Verify git state**

```bash
cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"
git status
git log --oneline -4
```

Expected: four commits from Tasks 1-4 visible, working tree clean.

---

## Self-Review Notes

- **Spec coverage**: Section 1 (plateau status data model) → Task 1 (enum) + Task 2/3 (fields added to `OverloadSuggestion`/`HoldSuggestion`). Section 2 (detection algorithm) → Task 1 (`classifyPlateauStatus`, window/fallback logic). Section 3 (decision table) → Task 2 (`RuleBasedOverloadSuggester`) and Task 3 (`RuleBasedHoldSuggester`). Section 4 (calisthenics progression tiers) → Task 3 (`CALISTHENICS_PROGRESSIONS` map + ceiling check). Verification → Task 5.
- **Spec deviation, documented**: the spec's Section 4 describes the progression-tier check generically as applying to "CALISTHENICS-category exercises," but every actual chain in `CALISTHENICS_PROGRESSIONS` is a HOLD-type exercise (confirmed by reading `SeedExercises.kt` — the Planche/Lever/Human Flag tiers were all seeded with `loggingType = LoggingType.HOLD`). This plan implements the progression-tier logic only in `RuleBasedHoldSuggester`, not `RuleBasedOverloadSuggester`, since there's no WEIGHT_REPS calisthenics chain in the library to look up — adding the parameter to `OverloadSuggester` too would be dead, untestable code. Captured in Global Constraints as an intentional interface asymmetry, not an oversight.
- **Type consistency checked**: `classifyPlateauStatus(recentValues: List<Double>, hitTargetFlags: List<Boolean>): PlateauStatus` (Task 1) is called identically in Task 2 (`oneRepMaxes`/`hitFlags` from `SetPerformance`) and Task 3 (`durations`/`hitFlags` from `HoldPerformance`) — same parameter order and types both places. `HoldSuggester.suggestNext`'s new `exerciseName: String` parameter (Task 3) matches exactly what Task 4 passes at the one call site (`exercise.name`, a `String` per `Exercise.kt`'s existing field).
- **Backward-compatibility checked**: `OverloadSuggesterTest`'s and `HoldSuggesterTest`'s pre-existing three tests (no-history, hit-target, missed-target, two-miss-deload) keep their exact original assertions on `weightKg`/`reps`/`targetDurationSeconds`/`note` text — only new `plateauStatus` assertions and (for `HoldSuggesterTest`) the new `exerciseName` argument were added, confirming this plan doesn't silently change already-working behavior.
- **Placeholder scan**: no TBD/TODO markers. All code steps are complete, compilable Kotlin.
- **Test data hand-verified against the algorithm**: an earlier draft's trend check used `window.last() >= window.first()`, which misclassifies a window that oscillates back to its starting value as "trending up" (equal endpoints). Fixed to a strict first-half-average-vs-second-half-average comparison, and the PLATEAUING test data in Tasks 2/3 was hand-recalculated against the corrected algorithm (documented inline in each test's comment) rather than left as illustrative numbers — this is exactly the kind of arithmetic mismatch TDD's "run and verify it fails/passes" steps catch, so Task 1 Step 2/4 and the equivalent steps in Tasks 2/3 aren't optional formalities here.
