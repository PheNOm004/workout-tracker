# Warmup-Aware, Session-Aware Overload Suggester Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a per-set warmup flag and rework `RuleBasedOverloadSuggester`/`RuleBasedHoldSuggester` so overload decisions happen once per session (against session-representative history), not once per raw set — a second working set logged mid-session repeats the session's first working-set target instead of escalating further.

**Architecture:** `SetLog.isWarmup` (new, default `false`) marks a set as excluded from suggester input. A new domain function `sessionWorkingSetHistory` reduces raw sets to one representative (last non-warmup) set per session, ordered chronologically by session start time — this becomes the suggesters' trend/decision input, replacing today's flat raw-set list. Both suggester interfaces gain a second parameter, `currentSessionWorkingSets`: when non-empty, the suggestion locks to its first entry's weight/target (new `PlateauStatus.REPEATING`), skipping the whole decision table.

**Tech Stack:** Kotlin, Jetpack Compose, Room (migration 7→8), JUnit (plain-Kotlin domain unit tests, full TDD — same discipline as the rest of this project's domain layer).

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-12-timego-warmup-session-aware-suggester-design.md` — every task below implements one of its sections.
- No new Gradle dependencies.
- Follow existing project conventions: DAOs use `@Insert`/`@Query` only, Room migrations are raw SQL `object : Migration(n, n+1)` blocks in `TimeGoDatabase.kt`, domain logic is plain Kotlin with no Android dependency and full TDD, no Room instrumented-test infra exists in this project (migrations are verified manually on-device, not via `MigrationTestHelper`).
- Branch: `warmup-session-aware-suggester`, off `master`. Commit after every task, one commit per task.
- Do not touch: Progress-screen timeframe/PR-decimal/heatmap work (backlog item 3), exercise timer (item 4), calisthenics BW+k display (item 5) — all out of scope here.

---

### Task 1: `SetLog.isWarmup` + Room migration 7→8

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SetLog.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/TimeGoDatabase.kt`

**Interfaces:**
- Produces: `SetLog.isWarmup: Boolean = false` (defaulted, so every existing `SetLog(...)` call site across the codebase and test suite compiles unchanged). `MIGRATION_7_8`, `TimeGoDatabase` at `version = 8`.

- [ ] **Step 1: Add the field**

In `SetLog.kt`, add `isWarmup` as the last constructor parameter, and extend the doc comment:

```kotlin
/** [targetReps] is the rep count the user was aiming for on this set -- the overload suggester
 *  compares [reps] against it to detect a missed target. [durationMinutes]/[distanceKm] are used
 *  instead of [weightKg]/[reps] for CARDIO/WARMUP exercises; [holdSeconds]/[targetHoldSeconds]
 *  are used instead for HOLD exercises (see LoggingType). [weightKg]/[reps] are 0.0/0 sentinels
 *  whenever a different pair applies -- callers branch on the logged exercise's loggingType,
 *  never on null-checking these fields, to know which pair is real. [isWarmup] marks a set as
 *  excluded from the overload suggester's working-set baseline (see [sessionWorkingSetHistory])
 *  -- only meaningful for WEIGHT_REPS/HOLD sets, which are the only logging types with a
 *  suggester; CARDIO/WARMUP-category sets never set it. */
@Entity(tableName = "set_logs")
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val weightKg: Double,
    val reps: Int,
    val targetReps: Int,
    val loggedAtEpochMillis: Long,
    val durationMinutes: Double? = null,
    val distanceKm: Double? = null,
    val holdSeconds: Int? = null,
    val targetHoldSeconds: Int? = null,
    val isWarmup: Boolean = false,
)
```

- [ ] **Step 2: Add the migration and bump the version**

In `TimeGoDatabase.kt`, add after `MIGRATION_6_7`:

```kotlin
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE set_logs ADD COLUMN isWarmup INTEGER NOT NULL DEFAULT 0")
    }
}
```

Update the `@Database` annotation's `version` to `8`, and `addMigrations` to include `MIGRATION_7_8`:

```kotlin
@Database(
    entities = [Exercise::class, WorkoutSession::class, SetLog::class, Routine::class, RoutineExercise::class, BodyMetric::class],
    version = 8,
    exportSchema = true,
)
```

```kotlin
.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
```

- [ ] **Step 3: Build to confirm the schema compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. Every existing `SetLog(...)` call site (production and test) compiles unchanged since `isWarmup` is defaulted — unlike the session-model-landing-page plan's `WorkoutSession` change, no existing fixtures need updating here.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SetLog.kt app/src/main/java/com/lsing/timego/data/TimeGoDatabase.kt app/schemas
git commit -m "feat(data): add SetLog.isWarmup and Room migration 7->8"
```

---

### Task 2: `WorkoutRepository.logSet`/`logHoldSet` gain `isWarmup`

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/WorkoutRepository.kt`

**Interfaces:**
- Produces: `logSet(sessionId, exerciseId, weightKg, reps, targetReps, isWarmup: Boolean = false)`, `logHoldSet(sessionId, exerciseId, durationSeconds, targetDurationSeconds, isWarmup: Boolean = false)`. `logCardioSet` is unchanged.

- [ ] **Step 1: Update both functions**

```kotlin
suspend fun logSet(sessionId: Long, exerciseId: Long, weightKg: Double, reps: Int, targetReps: Int, isWarmup: Boolean = false) {
    db.setLogDao().insert(
        SetLog(
            sessionId = sessionId,
            exerciseId = exerciseId,
            weightKg = weightKg,
            reps = reps,
            targetReps = targetReps,
            loggedAtEpochMillis = System.currentTimeMillis(),
            isWarmup = isWarmup,
        ),
    )
}
```

```kotlin
suspend fun logHoldSet(sessionId: Long, exerciseId: Long, durationSeconds: Int, targetDurationSeconds: Int, isWarmup: Boolean = false) {
    db.setLogDao().insert(
        SetLog(
            sessionId = sessionId,
            exerciseId = exerciseId,
            weightKg = 0.0,
            reps = 0,
            targetReps = 0,
            loggedAtEpochMillis = System.currentTimeMillis(),
            holdSeconds = durationSeconds,
            targetHoldSeconds = targetDurationSeconds,
            isWarmup = isWarmup,
        ),
    )
}
```

- [ ] **Step 2: Build**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL (both parameters are defaulted, so `LogViewModel`'s existing call sites — not yet updated, that's Task 6 — still compile).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/WorkoutRepository.kt
git commit -m "feat(data): add isWarmup parameter to logSet/logHoldSet"
```

---

### Task 3: Domain — `sessionWorkingSetHistory` (TDD)

**Files:**
- Create: `app/src/main/java/com/lsing/timego/domain/SessionWorkingSetHistory.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/SessionWorkingSetHistoryTest.kt`

**Interfaces:**
- Consumes: `SetLog` (Task 1's `isWarmup` field).
- Produces: `fun sessionWorkingSetHistory(setLogs: List<SetLog>, sessionStartById: Map<Long, Long>): List<SetLog>` — one entry per session (that session's last non-warmup set), ordered by session start time ascending. Sessions absent from `sessionStartById` sort using a `0L` fallback.

- [ ] **Step 1: Write the failing tests**

Create `SessionWorkingSetHistoryTest.kt`:

```kotlin
package com.lsing.timego.domain

import com.lsing.timego.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionWorkingSetHistoryTest {
    private fun setLog(id: Long, sessionId: Long, loggedAt: Long, isWarmup: Boolean = false) = SetLog(
        id = id,
        sessionId = sessionId,
        exerciseId = 1,
        weightKg = 60.0,
        reps = 8,
        targetReps = 8,
        loggedAtEpochMillis = loggedAt,
        isWarmup = isWarmup,
    )

    @Test
    fun `excludes warmup sets and keeps the last working set per session`() {
        val sets = listOf(
            setLog(1, sessionId = 10, loggedAt = 100, isWarmup = true),
            setLog(2, sessionId = 10, loggedAt = 200),
            setLog(3, sessionId = 10, loggedAt = 300),
        )
        val result = sessionWorkingSetHistory(sets, mapOf(10L to 0L))
        assertEquals(listOf(sets[2]), result)
    }

    @Test
    fun `orders sessions by session start time, not set id or logged time`() {
        val sets = listOf(
            setLog(1, sessionId = 20, loggedAt = 500),
            setLog(2, sessionId = 10, loggedAt = 100),
        )
        val result = sessionWorkingSetHistory(sets, mapOf(10L to 1000L, 20L to 2000L))
        assertEquals(listOf(sets[1], sets[0]), result)
    }

    @Test
    fun `a session with only warmup sets contributes nothing`() {
        val sets = listOf(setLog(1, sessionId = 10, loggedAt = 100, isWarmup = true))
        val result = sessionWorkingSetHistory(sets, mapOf(10L to 0L))
        assertEquals(emptyList<SetLog>(), result)
    }

    @Test
    fun `unknown session id falls back to zero for ordering`() {
        val sets = listOf(
            setLog(1, sessionId = 10, loggedAt = 100),
            setLog(2, sessionId = 99, loggedAt = 200), // no entry in sessionStartById
        )
        val result = sessionWorkingSetHistory(sets, mapOf(10L to 500L))
        assertEquals(listOf(sets[1], sets[0]), result) // session 99 (fallback 0) sorts before session 10 (500)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.SessionWorkingSetHistoryTest"`
Expected: FAIL (compile error — `sessionWorkingSetHistory` doesn't exist)

- [ ] **Step 3: Implement**

Create `SessionWorkingSetHistory.kt`:

```kotlin
package com.lsing.timego.domain

import com.lsing.timego.data.SetLog

/** Reduces raw sets for one exercise into one representative set per session -- the session's
 *  last non-warmup set, which is that session's ending effort. Ordered oldest-session-first.
 *  Warmup sets ([SetLog.isWarmup]) are excluded entirely: they never count toward the working-set
 *  baseline or the plateau/trend window. [sessionStartById] resolves chronological session order
 *  (sessions aren't necessarily ordered by id once multiple sessions can land on the same date). */
fun sessionWorkingSetHistory(setLogs: List<SetLog>, sessionStartById: Map<Long, Long>): List<SetLog> =
    setLogs
        .filterNot { it.isWarmup }
        .groupBy { it.sessionId }
        .mapValues { (_, sets) -> sets.maxBy { it.loggedAtEpochMillis } }
        .values
        .sortedBy { sessionStartById[it.sessionId] ?: 0L }
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.SessionWorkingSetHistoryTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/SessionWorkingSetHistory.kt app/src/test/java/com/lsing/timego/domain/SessionWorkingSetHistoryTest.kt
git commit -m "feat(domain): add sessionWorkingSetHistory"
```

---

### Task 4: Domain — `OverloadSuggester` two-list signature + mid-session lock (TDD)

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/OverloadSuggester.kt`
- Modify: `app/src/main/java/com/lsing/timego/domain/PlateauDetection.kt`
- Modify: `app/src/test/java/com/lsing/timego/domain/OverloadSuggesterTest.kt`

**Interfaces:**
- Produces: `PlateauStatus.REPEATING` (new enum case). `OverloadSuggester.suggestNext(sessionHistory: List<SetPerformance>, currentSessionWorkingSets: List<SetPerformance>): OverloadSuggestion?` (signature change from the old single-list `suggestNext(history)`).

- [ ] **Step 1: Add the enum case**

In `PlateauDetection.kt`, change:

```kotlin
enum class PlateauStatus { PROGRESSING, PLATEAUING, REGRESSING }
```

to:

```kotlin
enum class PlateauStatus { PROGRESSING, PLATEAUING, REGRESSING, REPEATING }
```

`classifyPlateauStatus` itself is unchanged — it never returns `REPEATING`; only the suggesters set it directly, in the lock branch below.

- [ ] **Step 2: Update the existing tests to the new signature (still red — implementation isn't updated yet)**

In `OverloadSuggesterTest.kt`, every existing `suggester.suggestNext(history)` call becomes `suggester.suggestNext(history, emptyList())` — these tests exercise the between-session decision table, so they always pass an empty `currentSessionWorkingSets` (no set logged yet this session):

```kotlin
    @Test
    fun `no history returns null`() {
        assertNull(suggester.suggestNext(emptyList(), emptyList()))
    }

    @Test
    fun `hit target reps suggests weight increase`() {
        val history = listOf(SetPerformance(weightKg = 60.0, reps = 8, targetReps = 8))
        val result = suggester.suggestNext(history, emptyList())
        assertEquals(62.5, result!!.weightKg, 0.001)
        assertEquals(8, result.reps)
        assertEquals(PlateauStatus.PROGRESSING, result.plateauStatus)
    }

    @Test
    fun `missed target reps suggests same weight plus a rep`() {
        val history = listOf(SetPerformance(weightKg = 60.0, reps = 6, targetReps = 8))
        val result = suggester.suggestNext(history, emptyList())
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
        val result = suggester.suggestNext(history, emptyList())
        assertEquals(54.0, result!!.weightKg, 0.001)
        assertEquals("Deload: missed target reps twice in a row", result.note)
        assertEquals(PlateauStatus.REGRESSING, result.plateauStatus)
    }

    @Test
    fun `five sets flat oscillating with last set hit target is PLATEAUING and holds weight`() {
        val history = listOf(
            SetPerformance(weightKg = 60.0, reps = 8, targetReps = 8),
            SetPerformance(weightKg = 62.5, reps = 8, targetReps = 8),
            SetPerformance(weightKg = 60.0, reps = 8, targetReps = 8),
            SetPerformance(weightKg = 62.5, reps = 8, targetReps = 8),
            SetPerformance(weightKg = 60.0, reps = 8, targetReps = 8),
        )
        val result = suggester.suggestNext(history, emptyList())
        assertEquals(PlateauStatus.PLATEAUING, result!!.plateauStatus)
        assertEquals(60.0, result.weightKg, 0.001)
        assertEquals(8, result.reps)
        assertEquals(true, result.note.contains("plateau", ignoreCase = true))
    }
```

Then add two new tests to the same file:

```kotlin
    @Test
    fun `current session already has a working set locks suggestion to its first entry`() {
        // This sessionHistory would trigger REGRESSING (last two missed target) if it were
        // consulted -- confirms the lock branch short-circuits the decision table entirely rather
        // than merely overriding its numeric output.
        val sessionHistory = listOf(
            SetPerformance(weightKg = 60.0, reps = 5, targetReps = 8),
            SetPerformance(weightKg = 60.0, reps = 6, targetReps = 8),
        )
        val currentSessionWorkingSets = listOf(
            SetPerformance(weightKg = 65.0, reps = 8, targetReps = 8),
            SetPerformance(weightKg = 70.0, reps = 8, targetReps = 8), // later set -- must be ignored
        )
        val result = suggester.suggestNext(sessionHistory, currentSessionWorkingSets)
        assertEquals(65.0, result!!.weightKg, 0.001)
        assertEquals(8, result.reps)
        assertEquals(PlateauStatus.REPEATING, result.plateauStatus)
        assertEquals("Repeating today's working weight", result.note)
    }

    @Test
    fun `both histories empty returns null`() {
        assertNull(suggester.suggestNext(emptyList(), emptyList()))
    }
```

- [ ] **Step 3: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.OverloadSuggesterTest"`
Expected: FAIL (compile error — `suggestNext` still takes one parameter)

- [ ] **Step 4: Implement**

Replace `OverloadSuggester.kt` in full:

```kotlin
package com.lsing.timego.domain

data class SetPerformance(val weightKg: Double, val reps: Int, val targetReps: Int)

data class OverloadSuggestion(val weightKg: Double, val reps: Int, val note: String, val plateauStatus: PlateauStatus)

interface OverloadSuggester {
    fun suggestNext(sessionHistory: List<SetPerformance>, currentSessionWorkingSets: List<SetPerformance>): OverloadSuggestion?
}

/** Deterministic, on-device, no ML -- see the v1 spec's "Recommendation Engine" section for why,
 *  and the 2026-08-11 suggester-plateau-upgrade-design spec for why this is the base layer a
 *  future ML model sits on top of rather than the model itself. [sessionHistory] is one
 *  representative (last working) set per past session (see [sessionWorkingSetHistory]), not every
 *  raw set -- overload is a between-session decision. [currentSessionWorkingSets] non-empty means
 *  a working set has already been logged for this exercise this session: the suggestion locks to
 *  that session's *first* working set's weight/target (2026-08-12 warmup-session-aware-suggester
 *  design) rather than re-running the decision table, so a second/third set of the same exercise
 *  doesn't escalate further mid-session even if you deviate (e.g. a drop set) on a later one. */
class RuleBasedOverloadSuggester : OverloadSuggester {
    override fun suggestNext(sessionHistory: List<SetPerformance>, currentSessionWorkingSets: List<SetPerformance>): OverloadSuggestion? {
        if (currentSessionWorkingSets.isNotEmpty()) {
            val locked = currentSessionWorkingSets.first()
            return OverloadSuggestion(
                weightKg = locked.weightKg,
                reps = locked.targetReps,
                note = "Repeating today's working weight",
                plateauStatus = PlateauStatus.REPEATING,
            )
        }
        if (sessionHistory.isEmpty()) return null
        val last = sessionHistory.last()
        val oneRepMaxes = sessionHistory.map { estimatedOneRepMax(it.weightKg, it.reps) }
        val hitFlags = sessionHistory.map { it.reps >= it.targetReps }
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
            PlateauStatus.REPEATING -> error("classifyPlateauStatus never returns REPEATING")
        }
    }
}
```

Note the `PlateauStatus.REPEATING -> error(...)` branch: `classifyPlateauStatus` (unchanged) can never actually return `REPEATING`, but the `when` must be exhaustive now that the enum has four cases. This documents that invariant rather than silently ignoring it.

- [ ] **Step 5: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.OverloadSuggesterTest"`
Expected: PASS (6 tests: 4 updated + 2 new)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/OverloadSuggester.kt app/src/main/java/com/lsing/timego/domain/PlateauDetection.kt app/src/test/java/com/lsing/timego/domain/OverloadSuggesterTest.kt
git commit -m "feat(domain): add mid-session lock to RuleBasedOverloadSuggester"
```

---

### Task 5: Domain — `HoldSuggester` two-list signature + mid-session lock (TDD)

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/HoldSuggester.kt`
- Modify: `app/src/test/java/com/lsing/timego/domain/HoldSuggesterTest.kt`

**Interfaces:**
- Produces: `HoldSuggester.suggestNext(sessionHistory: List<HoldPerformance>, currentSessionWorkingSets: List<HoldPerformance>, exerciseName: String): HoldSuggestion?` (signature change; `exerciseName` moves to third position).

- [ ] **Step 1: Update the existing tests to the new signature (still red)**

In `HoldSuggesterTest.kt`, every `suggester.suggestNext(history, "X")` becomes `suggester.suggestNext(history, emptyList(), "X")`:

```kotlin
    @Test
    fun `no history returns null`() {
        assertNull(suggester.suggestNext(emptyList(), emptyList(), "Plank"))
    }

    @Test
    fun `hit target hold suggests a longer duration`() {
        val history = listOf(HoldPerformance(durationSeconds = 30, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, emptyList(), "Plank")
        assertEquals(35, result!!.targetDurationSeconds)
        assertEquals(PlateauStatus.PROGRESSING, result.plateauStatus)
    }

    @Test
    fun `missed target hold suggests the same target`() {
        val history = listOf(HoldPerformance(durationSeconds = 20, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, emptyList(), "Plank")
        assertEquals(30, result!!.targetDurationSeconds)
        assertEquals(PlateauStatus.PROGRESSING, result.plateauStatus)
    }

    @Test
    fun `missed target twice in a row triggers deload`() {
        val history = listOf(
            HoldPerformance(durationSeconds = 20, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 22, targetDurationSeconds = 30),
        )
        val result = suggester.suggestNext(history, emptyList(), "Plank")
        assertEquals(27, result!!.targetDurationSeconds)
        assertEquals("Deload: missed target hold twice in a row", result.note)
        assertEquals(PlateauStatus.REGRESSING, result.plateauStatus)
    }

    @Test
    fun `five holds flat oscillating with last hit target is PLATEAUING and holds duration`() {
        val history = listOf(
            HoldPerformance(durationSeconds = 30, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 32, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 30, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 32, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 30, targetDurationSeconds = 30),
        )
        val result = suggester.suggestNext(history, emptyList(), "Plank")
        assertEquals(PlateauStatus.PLATEAUING, result!!.plateauStatus)
        assertEquals(30, result.targetDurationSeconds)
        assertEquals(true, result.note.contains("plateau", ignoreCase = true))
    }

    @Test
    fun `hitting target well past the ceiling on a mapped exercise suggests the next tier`() {
        val history = listOf(HoldPerformance(durationSeconds = 45, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, emptyList(), "Tuck Planche Hold")
        assertEquals(true, result!!.note.contains("Advanced Tuck Planche Hold"))
    }

    @Test
    fun `hitting target without reaching the ceiling on a mapped exercise does not suggest next tier`() {
        val history = listOf(HoldPerformance(durationSeconds = 32, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, emptyList(), "Tuck Planche Hold")
        assertEquals(false, result!!.note.contains("Advanced Tuck Planche Hold"))
    }

    @Test
    fun `ceiling hit on an exercise with no known progression falls through to normal suggestion`() {
        val history = listOf(HoldPerformance(durationSeconds = 45, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, emptyList(), "Plank")
        assertEquals(35, result!!.targetDurationSeconds)
    }

    @Test
    fun `ceiling hit on the top tier of a progression chain falls through to normal suggestion`() {
        val history = listOf(HoldPerformance(durationSeconds = 45, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, emptyList(), "Full Planche Hold")
        assertEquals(35, result!!.targetDurationSeconds)
    }
```

Then add two new tests:

```kotlin
    @Test
    fun `current session already has a working hold locks suggestion to its first entry`() {
        val currentSessionWorkingSets = listOf(
            HoldPerformance(durationSeconds = 35, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 40, targetDurationSeconds = 30), // later hold -- must be ignored
        )
        val result = suggester.suggestNext(emptyList(), currentSessionWorkingSets, "Plank")
        assertEquals(30, result!!.targetDurationSeconds)
        assertEquals(PlateauStatus.REPEATING, result.plateauStatus)
        assertEquals("Repeating today's working hold", result.note)
    }

    @Test
    fun `tier progression is skipped mid-session even if the ceiling is cleared`() {
        val currentSessionWorkingSets = listOf(HoldPerformance(durationSeconds = 45, targetDurationSeconds = 30))
        val result = suggester.suggestNext(emptyList(), currentSessionWorkingSets, "Tuck Planche Hold")
        assertEquals(30, result!!.targetDurationSeconds)
        assertEquals(PlateauStatus.REPEATING, result.plateauStatus)
        assertEquals(false, result.note.contains("Advanced Tuck Planche Hold"))
    }
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.HoldSuggesterTest"`
Expected: FAIL (compile error — `suggestNext` still takes the old two-parameter shape)

- [ ] **Step 3: Implement**

Replace `HoldSuggester.kt` in full:

```kotlin
package com.lsing.timego.domain

data class HoldPerformance(val durationSeconds: Int, val targetDurationSeconds: Int)

data class HoldSuggestion(val targetDurationSeconds: Int, val note: String, val plateauStatus: PlateauStatus)

interface HoldSuggester {
    fun suggestNext(sessionHistory: List<HoldPerformance>, currentSessionWorkingSets: List<HoldPerformance>, exerciseName: String): HoldSuggestion?
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
 *  [sessionHistory] is one representative (last working) hold per past session (see
 *  [sessionWorkingSetHistory]), not every raw hold. [currentSessionWorkingSets] non-empty --
 *  including the calisthenics tier-progression check below -- locks the suggestion to this
 *  session's *first* working hold's target instead of consulting [sessionHistory] at all: tier
 *  progression is a between-session decision exactly like a weight/duration increase is, per the
 *  2026-08-12 warmup-session-aware-suggester design. */
class RuleBasedHoldSuggester : HoldSuggester {
    override fun suggestNext(
        sessionHistory: List<HoldPerformance>,
        currentSessionWorkingSets: List<HoldPerformance>,
        exerciseName: String,
    ): HoldSuggestion? {
        if (currentSessionWorkingSets.isNotEmpty()) {
            val locked = currentSessionWorkingSets.first()
            return HoldSuggestion(
                targetDurationSeconds = locked.targetDurationSeconds,
                note = "Repeating today's working hold",
                plateauStatus = PlateauStatus.REPEATING,
            )
        }
        if (sessionHistory.isEmpty()) return null
        val last = sessionHistory.last()
        val durations = sessionHistory.map { it.durationSeconds.toDouble() }
        val hitFlags = sessionHistory.map { it.durationSeconds >= it.targetDurationSeconds }
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
            PlateauStatus.REPEATING -> error("classifyPlateauStatus never returns REPEATING")
        }
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.HoldSuggesterTest"`
Expected: PASS (10 tests: 8 updated + 2 new)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/HoldSuggester.kt app/src/test/java/com/lsing/timego/domain/HoldSuggesterTest.kt
git commit -m "feat(domain): add mid-session lock to RuleBasedHoldSuggester"
```

---

### Task 6: `LogViewModel` — rewire suggestion computation and logging calls

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt`

**Interfaces:**
- Consumes: `sessionWorkingSetHistory` (Task 3), new `OverloadSuggester`/`HoldSuggester` signatures (Tasks 4-5), `WorkoutRepository.logSet`/`logHoldSet` with `isWarmup` (Task 2).
- Produces: `LogViewModel.logSet(exerciseId, weightKg, reps, targetReps, isWarmup: Boolean = false)`, `logHoldSet(exerciseId, durationSeconds, targetDurationSeconds, isWarmup: Boolean = false)`.

- [ ] **Step 1: Add the import**

Add to `LogViewModel.kt`:

```kotlin
import com.lsing.timego.domain.sessionWorkingSetHistory
```

- [ ] **Step 2: Replace `refreshSuggestions`**

Replace the existing `refreshSuggestions` function:

```kotlin
/** Splits suggestion computation by loggingType: WEIGHT_REPS exercises get a weight/reps
 *  suggestion from [suggester], HOLD exercises get a duration suggestion from [holdSuggester] --
 *  an exercise can only produce one kind, so each history is built from the fields that are real
 *  for that exercise (see SetLog's doc comment on its sentinel-field convention). Each exercise's
 *  raw sets are reduced to [sessionWorkingSetHistory] (one representative set per past session)
 *  plus, separately, the active session's own working sets for that exercise so far -- see the
 *  2026-08-12 warmup-session-aware-suggester design for why suggestions no longer look at a flat
 *  raw-set history. */
private suspend fun refreshSuggestions(exerciseList: List<Exercise>) {
    val allSets = repository.allSetLogsOrderedByTime()
    val sessionStartById = repository.allSessions().associate { it.id to it.startEpochMillis }
    val activeSessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId
    val setsByExercise = allSets.groupBy { it.exerciseId }
    val map = mutableMapOf<Long, OverloadSuggestion>()
    val holdMap = mutableMapOf<Long, HoldSuggestion>()
    for (exercise in exerciseList) {
        val exerciseSets = setsByExercise[exercise.id].orEmpty()
        val sessionHistory = sessionWorkingSetHistory(exerciseSets, sessionStartById)
        val currentSessionWorkingSets = if (activeSessionId != null) {
            exerciseSets.filter { it.sessionId == activeSessionId && !it.isWarmup }.sortedBy { it.loggedAtEpochMillis }
        } else {
            emptyList()
        }
        if (exercise.loggingType == LoggingType.HOLD.name) {
            val historyPerf = sessionHistory.map { HoldPerformance(it.holdSeconds ?: 0, it.targetHoldSeconds ?: 0) }
            val currentPerf = currentSessionWorkingSets.map { HoldPerformance(it.holdSeconds ?: 0, it.targetHoldSeconds ?: 0) }
            holdSuggester.suggestNext(historyPerf, currentPerf, exercise.name)?.let { holdMap[exercise.id] = it }
        } else {
            val historyPerf = sessionHistory.map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
            val currentPerf = currentSessionWorkingSets.map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
            suggester.suggestNext(historyPerf, currentPerf)?.let { map[exercise.id] = it }
        }
    }
    _suggestions.value = map
    _holdSuggestions.value = holdMap
}
```

This replaces the per-exercise-loop use of `repository.allSetLogsOrderedByTime().groupBy { it.exerciseId }` with the same batched fetch (one query, not one per exercise) — `sessionStartById` is now fetched once per call too, not once per exercise.

- [ ] **Step 3: Update `logSet`/`logHoldSet`**

```kotlin
fun logSet(exerciseId: Long, weightKg: Double, reps: Int, targetReps: Int, isWarmup: Boolean = false) {
    val sessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId ?: return
    viewModelScope.launch {
        repository.logSet(sessionId, exerciseId, weightKg, reps, targetReps, isWarmup)
        refreshSuggestionForExercise(exerciseId)
    }
}
```

```kotlin
fun logHoldSet(exerciseId: Long, durationSeconds: Int, targetDurationSeconds: Int, isWarmup: Boolean = false) {
    val sessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId ?: return
    viewModelScope.launch {
        repository.logHoldSet(sessionId, exerciseId, durationSeconds, targetDurationSeconds, isWarmup)
        refreshSuggestionForExercise(exerciseId)
    }
}
```

(`logCardioSet` is unchanged — leave it exactly as-is.)

- [ ] **Step 4: Replace `refreshSuggestionForExercise` and add `buildSuggestionInputs`**

```kotlin
/** Recomputes the suggestion for just the exercise that was logged, instead of every exercise in
 *  the library (see [refreshSuggestions]'s doc comment for why -- unchanged perf rationale from
 *  the 2026-08-10 logging-field-accuracy session). */
private suspend fun refreshSuggestionForExercise(exerciseId: Long) {
    val exercise = allExercises.firstOrNull { it.id == exerciseId } ?: return
    val (sessionHistory, currentSessionWorkingSets) = buildSuggestionInputs(exerciseId)
    if (exercise.loggingType == LoggingType.HOLD.name) {
        val historyPerf = sessionHistory.map { HoldPerformance(it.holdSeconds ?: 0, it.targetHoldSeconds ?: 0) }
        val currentPerf = currentSessionWorkingSets.map { HoldPerformance(it.holdSeconds ?: 0, it.targetHoldSeconds ?: 0) }
        val suggestion = holdSuggester.suggestNext(historyPerf, currentPerf, exercise.name)
        _holdSuggestions.value = if (suggestion != null) {
            _holdSuggestions.value + (exerciseId to suggestion)
        } else {
            _holdSuggestions.value - exerciseId
        }
    } else {
        val historyPerf = sessionHistory.map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
        val currentPerf = currentSessionWorkingSets.map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
        val suggestion = suggester.suggestNext(historyPerf, currentPerf)
        _suggestions.value = if (suggestion != null) {
            _suggestions.value + (exerciseId to suggestion)
        } else {
            _suggestions.value - exerciseId
        }
    }
}

/** Shared by [refreshSuggestionForExercise] -- fetches this exercise's full history once, splits
 *  it into past-session representative performances and the active session's own working sets so
 *  far (empty if no session is active, per [SessionUiState]). */
private suspend fun buildSuggestionInputs(exerciseId: Long): Pair<List<com.lsing.timego.data.SetLog>, List<com.lsing.timego.data.SetLog>> {
    val allSets = repository.historyForExercise(exerciseId)
    val sessionStartById = repository.allSessions().associate { it.id to it.startEpochMillis }
    val sessionHistory = sessionWorkingSetHistory(allSets, sessionStartById)
    val activeSessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId
    val currentSessionWorkingSets = if (activeSessionId != null) {
        allSets.filter { it.sessionId == activeSessionId && !it.isWarmup }.sortedBy { it.loggedAtEpochMillis }
    } else {
        emptyList()
    }
    return sessionHistory to currentSessionWorkingSets
}
```

- [ ] **Step 5: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt
git commit -m "feat(log): wire session-aware suggestion inputs and isWarmup logging into LogViewModel"
```

---

### Task 7: `LogScreen` — warmup toggle UI

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt`

**Interfaces:**
- Consumes: `LogViewModel.logSet`/`logHoldSet` with `isWarmup` (Task 6).

- [ ] **Step 1: Add the import**

Add to `LogScreen.kt`:

```kotlin
import androidx.compose.material3.Checkbox
```

- [ ] **Step 2: Update `StrengthLogRow`'s `onLog` shape and add the toggle**

Change the parameter type:

```kotlin
@Composable
private fun StrengthLogRow(
    exerciseName: String,
    category: String,
    suggestion: com.lsing.timego.domain.OverloadSuggestion?,
    isBodyweight: Boolean,
    latestBodyWeightKg: Double?,
    onLog: (weightKg: Double, reps: Int, targetReps: Int, isWarmup: Boolean) -> Unit,
) {
    var expanded by remember(exerciseName) { mutableStateOf(false) }
    var weightText by remember(exerciseName) {
        mutableStateOf(if (isBodyweight) latestBodyWeightKg?.toString().orEmpty() else "")
    }
    var repsText by remember(exerciseName) { mutableStateOf("") }
    var isWarmup by remember(exerciseName) { mutableStateOf(false) }
    val visual = categoryVisual(ExerciseCategory.valueOf(category))
```

Add the toggle row right after the suggestion note (still inside `AnimatedExpand`, before the input `Row`):

```kotlin
        AnimatedExpand(expanded) {
            if (suggestion != null) {
                Text(
                    suggestion.note,
                    style = LedgerFigureValue.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Spacing.Medium),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Spacing.Medium),
            ) {
                Checkbox(checked = isWarmup, onCheckedChange = { isWarmup = it })
                Text("Warmup set", style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
```

Update the log button's `onClick` to pass and then reset `isWarmup`:

```kotlin
                Button(onClick = {
                    val weight = weightText.toDoubleOrNull()
                    val reps = repsText.toIntOrNull()
                    if (weight != null && reps != null) {
                        onLog(weight, reps, suggestion?.reps ?: reps, isWarmup)
                        weightText = ""
                        repsText = ""
                        isWarmup = false
                    }
                }) {
                    Text("Log set")
                }
```

- [ ] **Step 3: Update `HoldLogRow`'s `onLog` shape and add the toggle**

```kotlin
@Composable
private fun HoldLogRow(
    exerciseName: String,
    category: String,
    suggestion: HoldSuggestion?,
    onLog: (durationSeconds: Int, targetDurationSeconds: Int, isWarmup: Boolean) -> Unit,
) {
    var expanded by remember(exerciseName) { mutableStateOf(false) }
    var secondsText by remember(exerciseName) { mutableStateOf("") }
    var isWarmup by remember(exerciseName) { mutableStateOf(false) }
    val visual = categoryVisual(ExerciseCategory.valueOf(category))
```

```kotlin
        AnimatedExpand(expanded) {
            if (suggestion != null) {
                Text(
                    suggestion.note,
                    style = LedgerFigureValue.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Spacing.Medium),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Spacing.Medium),
            ) {
                Checkbox(checked = isWarmup, onCheckedChange = { isWarmup = it })
                Text("Warmup set", style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = secondsText,
                    onValueChange = { secondsText = it },
                    label = { Text("seconds held") },
                    textStyle = LedgerFigureValue.copy(fontSize = 16.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).padding(end = Spacing.Small),
                )
                Button(onClick = {
                    val seconds = secondsText.toIntOrNull()
                    if (seconds != null) {
                        onLog(seconds, suggestion?.targetDurationSeconds ?: seconds, isWarmup)
                        secondsText = ""
                        isWarmup = false
                    }
                }) {
                    Text("Log hold")
                }
            }
        }
    }
}
```

- [ ] **Step 4: Update the two call sites in `LoggingContent`**

```kotlin
                ExerciseSections(exercises = exercises) { exercise ->
                    when (exercise.loggingType) {
                        LoggingType.HOLD.name -> HoldLogRow(
                            exerciseName = exercise.name,
                            category = exercise.category,
                            suggestion = holdSuggestions[exercise.id],
                            onLog = { duration, target, isWarmup -> viewModel.logHoldSet(exercise.id, duration, target, isWarmup) },
                        )
                        LoggingType.DURATION_DISTANCE.name -> CardioLogRow(
                            exerciseName = exercise.name,
                            category = exercise.category,
                            met = if (exercise.category == ExerciseCategory.CARDIO.name) MET_CARDIO else MET_WARMUP,
                            bodyWeightKg = latestBodyWeightKg,
                            onLog = { duration, distance -> viewModel.logCardioSet(exercise.id, duration, distance) },
                        )
                        else -> StrengthLogRow(
                            exerciseName = exercise.name,
                            category = exercise.category,
                            suggestion = suggestions[exercise.id],
                            isBodyweight = exercise.category == ExerciseCategory.CALISTHENICS.name,
                            latestBodyWeightKg = latestBodyWeightKg,
                            onLog = { weight, reps, target, isWarmup -> viewModel.logSet(exercise.id, weight, reps, target, isWarmup) },
                        )
                    }
                }
```

(`CardioLogRow`'s call site is unchanged.)

- [ ] **Step 5: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt
git commit -m "feat(log): add warmup toggle to StrengthLogRow and HoldLogRow"
```

---

### Task 8: Full verification pass

**Files:** none (verification only)

- [ ] **Step 1: Full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests green (existing suite + `SessionWorkingSetHistoryTest`, updated `OverloadSuggesterTest`/`HoldSuggesterTest`).

- [ ] **Step 2: Full debug build + install**

Run: `./gradlew assembleDebug installDebug`
Expected: BUILD SUCCESSFUL, installs on the connected device.

- [ ] **Step 3: Hand off for on-device manual verification**

Per this project's established discipline, report the following checklist to the user rather than performing it:

- Start a session, log a warmup set for an exercise (toggle checked) — confirm the suggestion for that exercise doesn't change yet (still shows the pre-session/between-session suggestion, not a "repeating" note).
- Log a working set (toggle unchecked) — confirm the suggestion for the *next* set of that exercise now shows "Repeating today's working weight"/"Repeating today's working hold" at exactly that weight/target.
- Log a second working set at a different weight (e.g. simulate a drop set) — confirm the suggestion still repeats the *first* working set's weight, not the second.
- End the session, start a new one, log a set for the same exercise — confirm the suggestion now reflects the normal between-session decision table (deload/plateau-hold/increase) again, not a repeat.
- Test a tiered HOLD exercise (e.g. a Planche tier) the same way, including confirming the tier-progression suggestion doesn't fire mid-session even after clearing the 1.5x ceiling on a later same-session hold.
- Confirm cardio/warmup-category logging (`CardioLogRow`) is unaffected — no warmup toggle appears there, logging still works as before.

- [ ] **Step 4: Merge**

Once the user confirms all checklist items on-device:

```bash
git checkout master
git merge --ff-only warmup-session-aware-suggester
git branch -d warmup-session-aware-suggester
```

- [ ] **Step 5: Update the vault**

Update `TimeGo - Gym Progress Tracker.md`'s backlog section: mark item 2 done, note the branch/spec/plan paths and merge date, matching the pattern of item 1.
