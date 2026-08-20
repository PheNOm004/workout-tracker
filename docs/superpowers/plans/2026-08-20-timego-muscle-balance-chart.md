# Muscle Balance Chart Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Progress screen's kg-volume-based radar chart with a new "Muscle Balance" chart that scores each muscle group against a fixed, research-grounded weekly training target, without changing the existing body-diagram heatmap.

**Architecture:** Three new pure domain functions in `MuscleDistribution.kt` (an RPE-based effort weight, a weighted effective-set-count aggregator, and a timeframe-normalized balance score), added alongside the existing untouched volume functions. `ProgressViewModel` computes the new result as a second `StateFlow` in its existing `combine` block. `ProgressScreen` points the `RadarChart` at the new state and leaves `MuscleBodyDiagram` wired to the old one.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit4 (`testDebugUnitTest`), Gradle.

**Spec:** `docs/superpowers/specs/2026-08-20-timego-muscle-balance-chart-design.md` (commit `7047da1`)

## Global Constraints

- `effortWeight`: RPE ≥7 → 1.0; RPE 5-6 → linear ramp `0.3 + (rpe - 5) / 2.0 * 0.7`; RPE ≤4 → 0.15; RPE `null` → 1.0.
- Flat target: 10 effective sets per muscle group per week, same for every `MuscleGroup` except `FULL_BODY`.
- Reuse the existing `ProgressTimeframe` enum and its `sinceDate` — no new time-window UI.
- `muscleDistributionForTimeframe` and `muscleGroupVolumeDistribution` must remain byte-for-byte unchanged — `MuscleBodyDiagram` still depends on them.
- Full `testDebugUnitTest` must stay green throughout; `assembleDebug`/`installDebug` and on-device user verification are required before this is considered done.

---

### Task 1: `effortWeight` — RPE-based effort weighting

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/MuscleDistribution.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/MuscleDistributionTest.kt`

**Interfaces:**
- Produces: `fun effortWeight(rpe: Int?): Double`

- [ ] **Step 1: Write the failing tests**

Add to the bottom of `MuscleDistributionTest.kt` (inside the existing `class MuscleDistributionTest { ... }`, before its closing brace):

```kotlin
    @Test
    fun `effortWeight gives full credit to effective-rep-range RPE and to missing RPE`() {
        assertEquals(1.0, effortWeight(7), 0.001)
        assertEquals(1.0, effortWeight(8), 0.001)
        assertEquals(1.0, effortWeight(9), 0.001)
        assertEquals(1.0, effortWeight(10), 0.001)
        assertEquals(1.0, effortWeight(null), 0.001)
    }

    @Test
    fun `effortWeight ramps between RPE 5 and 6`() {
        assertEquals(0.3, effortWeight(5), 0.001)
        assertEquals(0.65, effortWeight(6), 0.001)
    }

    @Test
    fun `effortWeight gives low credit below RPE 5`() {
        assertEquals(0.15, effortWeight(1), 0.001)
        assertEquals(0.15, effortWeight(4), 0.001)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.MuscleDistributionTest"`
Expected: FAIL — `effortWeight` is unresolved.

- [ ] **Step 3: Implement `effortWeight`**

Add to `MuscleDistribution.kt`, above `muscleDistributionForTimeframe`:

```kotlin
/** RPE >=7 (0-3 reps in reserve, "effective rep" territory per hypertrophy research) gets full
 *  credit toward the Muscle Balance chart's weekly target. RPE 5-6 ramps linearly (light-but-not-
 *  trivial effort). RPE <=4 gets low but nonzero credit -- very light work still counts a little,
 *  just not as a real stimulus set. Missing RPE gets full credit, same convention as every other
 *  RPE-gated behavior in this app (see escalationTierForRpe): never penalize a value the user
 *  simply didn't log. */
fun effortWeight(rpe: Int?): Double = when {
    rpe == null -> 1.0
    rpe >= 7 -> 1.0
    rpe >= 5 -> 0.3 + (rpe - 5) / 2.0 * 0.7
    else -> 0.15
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.MuscleDistributionTest"`
Expected: PASS, all tests including the three new ones.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/MuscleDistribution.kt app/src/test/java/com/lsing/timego/domain/MuscleDistributionTest.kt
git commit -m "Add effortWeight for RPE-based training-effort credit"
```

---

### Task 2: `muscleGroupEffectiveSetDistribution` — weighted effective-set aggregation

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/MuscleDistribution.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/MuscleDistributionTest.kt`

**Interfaces:**
- Consumes: `effortWeight(rpe: Int?): Double` (Task 1)
- Produces: `fun muscleGroupEffectiveSetDistribution(history: List<SetLog>, exercisesById: Map<Long, Exercise>, sessionDateById: Map<Long, LocalDate>, since: LocalDate): Map<String, Double>`

- [ ] **Step 1: Write the failing tests**

Add to `MuscleDistributionTest.kt`:

```kotlin
    @Test
    fun `muscleGroupEffectiveSetDistribution weights a set by its RPE effort credit`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sets = listOf(
            // RPE 8 -> full credit (1.0)
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 8),
            // RPE 3 -> low credit (0.15)
            SetLog(id = 2, sessionId = 1, exerciseId = 1, weightKg = 10.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 3),
        )
        val exercisesById = mapOf(1L to curl)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 10))

        val distribution = muscleGroupEffectiveSetDistribution(sets, exercisesById, sessionDateById, since = LocalDate.of(2026, 8, 1))

        assertEquals(1.15, distribution["BICEPS"]!!, 0.001)
    }

    @Test
    fun `muscleGroupEffectiveSetDistribution treats missing RPE as full credit`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = null),
        )
        val distribution = muscleGroupEffectiveSetDistribution(
            sets, mapOf(1L to curl), mapOf(1L to LocalDate.of(2026, 8, 10)), since = LocalDate.of(2026, 8, 1),
        )

        assertEquals(1.0, distribution["BICEPS"]!!, 0.001)
    }

    @Test
    fun `muscleGroupEffectiveSetDistribution excludes cardio and warmup sets, applies muscleWeights partial credit`() {
        val pullover = Exercise(
            id = 1, name = "Pullover", muscleGroups = listOf("LATS", "CHEST"), isCustom = false,
            category = "STRENGTH", muscleWeights = mapOf("CHEST" to 30),
        )
        val run = Exercise(id = 2, name = "Running", muscleGroups = listOf("QUADS"), isCustom = false, category = "CARDIO", loggingType = "DURATION_DISTANCE")
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 10.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 9),
            SetLog(id = 2, sessionId = 1, exerciseId = 2, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 1, durationMinutes = 30.0),
        )
        val exercisesById = mapOf(1L to pullover, 2L to run)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 10))

        val distribution = muscleGroupEffectiveSetDistribution(sets, exercisesById, sessionDateById, since = LocalDate.of(2026, 8, 1))

        assertEquals(1.0, distribution["LATS"]!!, 0.001)
        assertEquals(0.3, distribution["CHEST"]!!, 0.001)
        assertEquals(null, distribution["QUADS"])
    }

    @Test
    fun `muscleGroupEffectiveSetDistribution excludes sets before the cutoff date`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 8),
        )
        val distribution = muscleGroupEffectiveSetDistribution(
            sets, mapOf(1L to curl), mapOf(1L to LocalDate.of(2026, 7, 1)), since = LocalDate.of(2026, 8, 1),
        )

        assertEquals(emptyMap<String, Double>(), distribution)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.MuscleDistributionTest"`
Expected: FAIL — `muscleGroupEffectiveSetDistribution` is unresolved.

- [ ] **Step 3: Implement `muscleGroupEffectiveSetDistribution`**

Add to `MuscleDistribution.kt`, below `effortWeight`:

```kotlin
/** Weighted effective-set count per muscle group, mirroring [muscleGroupVolumeDistribution]'s
 *  filtering and [Exercise.muscleWeights] partial-credit conventions but counting RPE-weighted
 *  sets instead of load -- sets are the unit fitness research treats as comparable across muscle
 *  groups, unlike kg-moved. Deliberately a separate function: [muscleGroupVolumeDistribution] and
 *  [muscleDistributionForTimeframe] remain unchanged and continue to drive the body-diagram
 *  heatmap, which must not change. */
fun muscleGroupEffectiveSetDistribution(
    history: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sessionDateById: Map<Long, LocalDate>,
    since: LocalDate,
): Map<String, Double> {
    val effectiveSetsByGroup = mutableMapOf<String, Double>()
    for (log in history) {
        val exercise = exercisesById[log.exerciseId] ?: continue
        if (log.isWarmup || exercise.category == ExerciseCategory.WARMUP.name || exercise.category == ExerciseCategory.CARDIO.name) continue
        if (exercise.loggingType != LoggingType.WEIGHT_REPS.name && exercise.loggingType != LoggingType.HOLD.name) continue
        val date = sessionDateById[log.sessionId] ?: continue
        if (date.isBefore(since)) continue
        val weight = effortWeight(log.rpe)
        for (group in exercise.muscleGroups) {
            val credit = weight * (exercise.muscleWeights[group] ?: 100) / 100.0
            effectiveSetsByGroup[group] = (effectiveSetsByGroup[group] ?: 0.0) + credit
        }
    }
    return effectiveSetsByGroup
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.MuscleDistributionTest"`
Expected: PASS, all tests including the four new ones.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/MuscleDistribution.kt app/src/test/java/com/lsing/timego/domain/MuscleDistributionTest.kt
git commit -m "Add muscleGroupEffectiveSetDistribution"
```

---

### Task 3: `muscleBalanceForTimeframe` — normalize against the fixed weekly target

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/MuscleDistribution.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/MuscleDistributionTest.kt`

**Interfaces:**
- Consumes: `muscleGroupEffectiveSetDistribution(...)` (Task 2), `ProgressTimeframe.sinceDate(earliestSessionDate: LocalDate?, today: LocalDate): LocalDate` (existing, `ProgressMath.kt`)
- Produces: `fun muscleBalanceForTimeframe(timeframe: ProgressTimeframe, sessions: List<WorkoutSession>, sets: List<SetLog>, exercisesById: Map<Long, Exercise>, today: LocalDate): Map<String, Float>`

- [ ] **Step 1: Write the failing tests**

Add to `MuscleDistributionTest.kt`:

```kotlin
    @Test
    fun `muscleBalanceForTimeframe scores exactly-target effective sets as 1_0 over a one-week window`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sessions = listOf(WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 10), routineId = null, startEpochMillis = 0, endEpochMillis = 0))
        // 10 sets at RPE 8 (full credit each) = 10.0 effective sets, target for WEEK is 10.0 -> 1.0
        val sets = (1..10).map { i ->
            SetLog(id = i.toLong(), sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 8)
        }

        val balance = muscleBalanceForTimeframe(
            timeframe = ProgressTimeframe.WEEK,
            sessions = sessions,
            sets = sets,
            exercisesById = mapOf(1L to curl),
            today = LocalDate.of(2026, 8, 10),
        )

        assertEquals(1.0f, balance["BICEPS"]!!, 0.001f)
    }

    @Test
    fun `muscleBalanceForTimeframe scores half-target effective sets as 0_5`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sessions = listOf(WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 10), routineId = null, startEpochMillis = 0, endEpochMillis = 0))
        val sets = (1..5).map { i ->
            SetLog(id = i.toLong(), sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 8)
        }

        val balance = muscleBalanceForTimeframe(
            timeframe = ProgressTimeframe.WEEK,
            sessions = sessions,
            sets = sets,
            exercisesById = mapOf(1L to curl),
            today = LocalDate.of(2026, 8, 10),
        )

        assertEquals(0.5f, balance["BICEPS"]!!, 0.001f)
    }

    @Test
    fun `muscleBalanceForTimeframe caps at 1_0 and never exceeds it`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sessions = listOf(WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 10), routineId = null, startEpochMillis = 0, endEpochMillis = 0))
        val sets = (1..20).map { i ->
            SetLog(id = i.toLong(), sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 8)
        }

        val balance = muscleBalanceForTimeframe(
            timeframe = ProgressTimeframe.WEEK,
            sessions = sessions,
            sets = sets,
            exercisesById = mapOf(1L to curl),
            today = LocalDate.of(2026, 8, 10),
        )

        assertEquals(1.0f, balance["BICEPS"]!!, 0.001f)
    }

    @Test
    fun `muscleBalanceForTimeframe scales the target across a month window`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sessions = listOf(WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 1), routineId = null, startEpochMillis = 0, endEpochMillis = 0))
        // Month window is 30 days = 30/7 weeks; target = 10 * 30/7 = ~42.857. 21.43 effective sets -> ~0.5.
        val sets = (1..21).map { i ->
            SetLog(id = i.toLong(), sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 8)
        }

        val balance = muscleBalanceForTimeframe(
            timeframe = ProgressTimeframe.MONTH,
            sessions = sessions,
            sets = sets,
            exercisesById = mapOf(1L to curl),
            today = LocalDate.of(2026, 8, 10),
        )

        assertEquals(0.49f, balance["BICEPS"]!!, 0.01f)
    }

    @Test
    fun `muscleBalanceForTimeframe is empty when no qualifying sets exist`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val balance = muscleBalanceForTimeframe(
            timeframe = ProgressTimeframe.WEEK,
            sessions = emptyList(),
            sets = emptyList(),
            exercisesById = mapOf(1L to curl),
            today = LocalDate.of(2026, 8, 10),
        )

        assertEquals(emptyMap<String, Float>(), balance)
    }

    @Test
    fun `muscleBalanceForTimeframe does not divide by zero on a same-day lifetime window`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sessions = listOf(WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 10), routineId = null, startEpochMillis = 0, endEpochMillis = 0))
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 8),
        )

        val balance = muscleBalanceForTimeframe(
            timeframe = ProgressTimeframe.LIFETIME,
            sessions = sessions,
            sets = sets,
            exercisesById = mapOf(1L to curl),
            today = LocalDate.of(2026, 8, 10),
        )

        // since == today (LIFETIME's earliest-session fallback), inclusive-day counting makes the
        // window 1 day (1/7 week), target = 10/7 ~= 1.4286; 1 effective set / 1.4286 ~= 0.7.
        assertEquals(0.7f, balance["BICEPS"]!!, 0.01f)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.MuscleDistributionTest"`
Expected: FAIL — `muscleBalanceForTimeframe` is unresolved.

- [ ] **Step 3: Implement `muscleBalanceForTimeframe`**

Add `import java.time.temporal.ChronoUnit` to the top of `MuscleDistribution.kt` alongside the existing `java.time.LocalDate` import. Then add, below `muscleGroupEffectiveSetDistribution`:

```kotlin
private const val TARGET_EFFECTIVE_SETS_PER_WEEK = 10.0

/** [muscleGroupEffectiveSetDistribution] normalized against a fixed weekly target (evidence-
 *  grounded per the design spec: ~10 effective sets/muscle/week) rather than the period's own max
 *  group -- unlike the reverted frequency-vs-own-baseline attempt, this target is external and
 *  fixed, so genuine under-training still reads as genuinely low instead of flattening toward 1.0.
 *  Reuses the same [ProgressTimeframe]/[timeframe.sinceDate] the rest of the Progress screen
 *  already uses -- Year/Lifetime naturally read as "average weekly rate over that period," the
 *  same semantics every other Progress stat already has for those tabs. Inclusive day-counting
 *  (`+ 1`) guarantees at least a one-day window whenever [since] is on or before [today], so this
 *  never divides by zero even on someone's very first day of use. */
fun muscleBalanceForTimeframe(
    timeframe: ProgressTimeframe,
    sessions: List<WorkoutSession>,
    sets: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    today: LocalDate,
): Map<String, Float> {
    val since = timeframe.sinceDate(sessions.minOfOrNull { it.date }, today)
    val sessionDateById = sessions.associate { it.id to it.date }
    val effectiveSets = muscleGroupEffectiveSetDistribution(sets, exercisesById, sessionDateById, since)
    if (effectiveSets.isEmpty()) return emptyMap()
    val daysInWindow = ChronoUnit.DAYS.between(since, today) + 1
    val weeksInWindow = daysInWindow / 7.0
    val target = TARGET_EFFECTIVE_SETS_PER_WEEK * weeksInWindow
    return effectiveSets.mapValues { (_, value) -> (value / target).toFloat().coerceIn(0f, 1f) }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.MuscleDistributionTest"`
Expected: PASS, all tests including the six new ones.

- [ ] **Step 5: Run the full unit test suite**

Run: `./gradlew.bat testDebugUnitTest`
Expected: PASS — this is also a regression check that `muscleDistributionForTimeframe`/`muscleGroupVolumeDistribution` and every other existing test are untouched.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/MuscleDistribution.kt app/src/test/java/com/lsing/timego/domain/MuscleDistributionTest.kt
git commit -m "Add muscleBalanceForTimeframe, normalized against a fixed weekly target"
```

---

### Task 4: Wire `muscleBalance` into `ProgressViewModel`

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressViewModel.kt`

**Interfaces:**
- Consumes: `muscleBalanceForTimeframe(...)` (Task 3)
- Produces: `val muscleBalance: StateFlow<Map<String, Float>>` on `ProgressViewModel`

No new automated test for this task — this project's ViewModel layer has no unit test coverage today (no `kotlinx-coroutines-test`/`room-testing`, documented as a standing limitation in the project's session log), consistent with every other `ProgressViewModel` field. Verified instead by Task 6's full build/install/on-device pass.

- [ ] **Step 1: Add the import**

In `ProgressViewModel.kt`, add alongside the existing `muscleDistributionForTimeframe` import (line 17):

```kotlin
import com.lsing.timego.domain.muscleBalanceForTimeframe
```

- [ ] **Step 2: Add the backing `StateFlow`**

Add directly below the existing `_muscleDistribution`/`muscleDistribution` pair (after line 89):

```kotlin
    private val _muscleBalance = MutableStateFlow<Map<String, Float>>(emptyMap())
    val muscleBalance: StateFlow<Map<String, Float>> = _muscleBalance.asStateFlow()
```

- [ ] **Step 3: Compute it in the existing `combine` collector**

In the `init` block's `collect { ... }` lambda, directly below the existing `_muscleDistribution.value = ...` assignment (after line 138, before the closing `}` of the lambda):

```kotlin
                _muscleBalance.value = muscleBalanceForTimeframe(
                    timeframe = timeframe,
                    sessions = sessions,
                    sets = allSets,
                    exercisesById = exercisesById,
                    today = today,
                )
```

- [ ] **Step 4: Build to confirm it compiles**

Run: `./gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/progress/ProgressViewModel.kt
git commit -m "Compute muscleBalance alongside muscleDistribution in ProgressViewModel"
```

---

### Task 5: Point the radar chart at `muscleBalance`, update copy

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt`

**Interfaces:**
- Consumes: `viewModel.muscleBalance: StateFlow<Map<String, Float>>` (Task 4), `orderedMuscleDistributionForChart(distribution: Map<String, Float>): Map<String, Float>` (existing, value-agnostic — safe to reuse against the new map unchanged)

No new automated test — this is a Compose UI wiring change with no existing UI test coverage for this screen. Verified by Task 6.

- [ ] **Step 1: Collect the new state**

In `ProgressScreen`, add directly below the existing `val muscleDistribution by viewModel.muscleDistribution.collectAsState()` (line 74):

```kotlin
    val muscleBalance by viewModel.muscleBalance.collectAsState()
```

- [ ] **Step 2: Update the section header and explanatory copy**

Replace:

```kotlin
            SectionHeader("Muscle Distribution (${timeframeLabel(timeframe)})")
```

with:

```kotlin
            SectionHeader("Muscle Balance (${timeframeLabel(timeframe)})")
```

Replace:

```kotlin
            Text(
                "Colors show volume relative to your most-trained muscle group this period",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
```

with:

```kotlin
            Text(
                "Radar shows progress toward an even training balance across muscle groups this period; body diagram below shows volume relative to your most-trained muscle group.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
```

- [ ] **Step 3: Switch the `RadarChart`'s data source**

Replace:

```kotlin
                RadarChart(
                    values = orderedMuscleDistributionForChart(muscleDistribution)
                        .mapKeys { (group, _) -> formatEnumLabel(group) },
                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(vertical = 8.dp),
                )
```

with:

```kotlin
                RadarChart(
                    values = orderedMuscleDistributionForChart(muscleBalance)
                        .mapKeys { (group, _) -> formatEnumLabel(group) },
                    modifier = Modifier.fillMaxWidth().height(220.dp).padding(vertical = 8.dp),
                )
```

Leave the `MuscleBodyDiagram(intensities = muscleDistribution, ...)` call directly below it completely unchanged — it must keep using `muscleDistribution`, not `muscleBalance`.

The surrounding `if (muscleDistribution.isEmpty()) { ... } else { ... }` gate stays checking `muscleDistribution`, not `muscleBalance` — do not change this condition. `muscleGroupEffectiveSetDistribution` (Task 2) filters on the exact same set population as `muscleGroupVolumeDistribution` (`isWarmup`/category exclusion, same `since` cutoff, same loggingType restriction), so the two maps are always empty or non-empty together and share the same key set; gating on either one is equivalent, and leaving it on `muscleDistribution` avoids touching a line unrelated to this task's purpose.

- [ ] **Step 4: Build to confirm it compiles**

Run: `./gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt
git commit -m "Point the Progress radar chart at the new Muscle Balance score"
```

---

### Task 6: Full verification — build, install, on-device check

**Files:** None (verification only).

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew.bat testDebugUnitTest`
Expected: PASS, 0 failures — includes every test from Tasks 1-3 plus the full pre-existing suite untouched.

- [ ] **Step 2: Assemble the debug APK**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Install on the connected device**

Run (PowerShell/Git Bash, adjust the adb path if different):
```bash
"/c/Users/lsing/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r "app/build/outputs/apk/debug/app-debug.apk"
```
Expected: `Success`.

- [ ] **Step 4: On-device verification (user)**

Open the Progress tab. Confirm:
- The chart section now reads "Muscle Balance (<timeframe>)".
- The radar chart's shape looks different from before (balance-vs-target, not volume-vs-max) — a heavily-trained muscle group no longer automatically dominates just because its exercises are heavier.
- The body-diagram heatmap directly below still looks the same as before this change (still volume-based, unaffected).
- Switching Week/Month/Year/Lifetime tabs updates the radar chart sensibly (a longer window with the same training frequency should read similarly, not collapse toward 0 or 1).

- [ ] **Step 5: Report back and await confirmation before any further work**

Do not proceed to the deferred "move to landing page + nudge" follow-up (explicitly out of scope for this plan) until the user confirms this chart looks and behaves correctly on-device.
