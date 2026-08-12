# TimeGo — Session Model + Logging Landing Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `WorkoutSession`'s date-keyed implicit creation with an explicit start/end state machine, and add a logging landing page (last-session summary, recommended muscle group, start-new-session) that appears whenever no session is active.

**Architecture:** `WorkoutSession` gains `startEpochMillis`/`endEpochMillis` (null = active). Sessions are created only by an explicit "Start New Session" action; auto-close (session inactive >1hr) is checked once, lazily, when the Log tab opens — a single indexed query plus one timestamp comparison, not a background job. `LogViewModel` exposes a session-state (`NoActiveSession` vs `Active`) that `LogScreen` branches on to show either the landing page or the existing logging UI.

**Tech Stack:** Kotlin, Jetpack Compose, Room (migration 6→7), JUnit (plain-Kotlin domain unit tests — this project has no Room/DAO instrumented test infra; all prior migrations were verified manually on-device, and this plan follows that same precedent).

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-12-timego-session-model-landing-page-design.md` — every task below implements one of its sections.
- No new Gradle dependencies.
- Follow existing project conventions: DAOs use `@Insert`/`@Query` only (no `@Update` anywhere in the codebase — raw `UPDATE` queries instead), Room migrations are raw SQL `object : Migration(n, n+1)` blocks in `TimeGoDatabase.kt`, domain logic is plain Kotlin with no Android dependency and full TDD, `Exercise.muscleGroups`/`MuscleGroup` values are stored/compared as `String`, not the enum type directly.
- Branch: `session-model-landing-page`, off `master`. Commit after every task per the project's established one-commit-per-task discipline.
- Do not touch: warmup-set flag, session-aware suggester logic (backlog item 2 — out of scope per spec, depends on this landing first), any Progress-screen heatmap muscle-summary wiring (backlog item 3 — the shared `muscleGroupsWorkedInSession` function this plan adds is used only by the landing page here; item 3 wires it into the heatmap later).

---

### Task 1: `WorkoutSession` entity + `SessionDao` queries

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/WorkoutSession.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/SessionDao.kt`

**Interfaces:**
- Produces: `WorkoutSession(id, date, routineId, startEpochMillis, endEpochMillis)`; `SessionDao.findActiveSession(): WorkoutSession?`, `SessionDao.findLastClosedSession(): WorkoutSession?`, `SessionDao.closeSession(sessionId: Long, endEpochMillis: Long)`. Removes `SessionDao.findByDate`.

This task has no independent test cycle of its own (Room entities/DAOs in this project aren't unit-tested directly — see Task 2's migration, which is the first thing that actually exercises these fields against a real schema). Written and verified for compile-correctness only here; behavior is verified end-to-end in Task 10.

- [ ] **Step 1: Update the entity**

Replace the full contents of `WorkoutSession.kt`:

```kotlin
package com.lsing.timego.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/** [date] is derived from [startEpochMillis] at creation and never changes afterward -- kept as
 *  its own column (rather than computed on read) because the heatmap and other date-grouped
 *  queries already group by it. [endEpochMillis] null means the session is still active; at most
 *  one session should have a null [endEpochMillis] at a time (an app-level invariant, not a DB
 *  constraint -- enforced by the landing page being the only place a new session is created, and
 *  it only renders when no active session exists). */
@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val routineId: Long?,
    val startEpochMillis: Long,
    val endEpochMillis: Long?,
)
```

- [ ] **Step 2: Update the DAO**

Replace the full contents of `SessionDao.kt`:

```kotlin
package com.lsing.timego.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun observeAll(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE endEpochMillis IS NULL LIMIT 1")
    suspend fun findActiveSession(): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE endEpochMillis IS NOT NULL ORDER BY endEpochMillis DESC LIMIT 1")
    suspend fun findLastClosedSession(): WorkoutSession?

    @Query("UPDATE workout_sessions SET endEpochMillis = :endEpochMillis WHERE id = :sessionId")
    suspend fun closeSession(sessionId: Long, endEpochMillis: Long)
}
```

Note `LocalDate` import is dropped from `SessionDao.kt` (no longer referenced there) but stays in `WorkoutSession.kt`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/WorkoutSession.kt app/src/main/java/com/lsing/timego/data/SessionDao.kt
git commit -m "feat(data): add explicit start/end fields to WorkoutSession"
```

(This won't compile standalone yet — `WorkoutRepository.startOrGetTodaySession` still calls the now-removed `findByDate`, and `TimeGoDatabase` still expects schema version 6. Task 2 and Task 3 fix both in the same session before the next full build.)

---

### Task 2: Room migration 6→7

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/TimeGoDatabase.kt`

**Interfaces:**
- Consumes: `WorkoutSession` from Task 1.
- Produces: `MIGRATION_6_7`, `TimeGoDatabase` at `version = 7`.

- [ ] **Step 1: Add the migration and bump the version**

In `TimeGoDatabase.kt`, add after `MIGRATION_5_6`:

```kotlin
/** Backfills startEpochMillis/endEpochMillis from each session's own set_logs (min/max
 *  loggedAtEpochMillis) rather than leaving them at a fixed default -- pre-migration sessions have
 *  no explicit boundaries, so their nearest real signal is the timestamps of the sets actually
 *  logged in them. A session with zero sets (shouldn't normally happen, but not impossible if a
 *  session was created and nothing was ever logged) falls back to midnight of its date column
 *  (`date` is stored as an epoch-day Long via Converters.toEpochDay, so `date * 86400000` is that
 *  day's midnight in epoch millis). Every backfilled row gets a non-null endEpochMillis --
 *  pre-migration data has no concept of "still active," so all of it is treated as closed. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN startEpochMillis INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN endEpochMillis INTEGER")
        db.execSQL(
            """
            UPDATE workout_sessions SET
                startEpochMillis = COALESCE(
                    (SELECT MIN(loggedAtEpochMillis) FROM set_logs WHERE set_logs.sessionId = workout_sessions.id),
                    date * 86400000
                ),
                endEpochMillis = COALESCE(
                    (SELECT MAX(loggedAtEpochMillis) FROM set_logs WHERE set_logs.sessionId = workout_sessions.id),
                    date * 86400000
                )
            """,
        )
    }
}
```

Update the `@Database` annotation's `version`:

```kotlin
@Database(
    entities = [Exercise::class, WorkoutSession::class, SetLog::class, Routine::class, RoutineExercise::class, BodyMetric::class],
    version = 7,
    exportSchema = true,
)
```

Update `addMigrations` in `getInstance`:

```kotlin
.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
```

- [ ] **Step 2: Build to confirm the schema compiles**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. (Room's schema export will fail the build if `exportSchema = true` can't find `app/schemas` — this project already has that directory from prior migrations, so no new setup needed.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/TimeGoDatabase.kt app/schemas
git commit -m "feat(data): add Room migration 6->7 for session start/end fields"
```

---

### Task 3: `SetLogDao.forSession` + `WorkoutRepository` session methods

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SetLogDao.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/WorkoutRepository.kt`

**Interfaces:**
- Consumes: `SessionDao.findActiveSession/findLastClosedSession/closeSession` (Task 1), `WorkoutSession` fields (Task 1).
- Produces: `WorkoutRepository.activeSession(): WorkoutSession?`, `lastClosedSession(): WorkoutSession?`, `startSession(routineId: Long?): WorkoutSession`, `endSession(sessionId: Long, endEpochMillis: Long)`, `setLogsForSession(sessionId: Long): List<SetLog>`. Removes `WorkoutRepository.startOrGetTodaySession`.

- [ ] **Step 1: Add `SetLogDao.forSession`**

Add to `SetLogDao.kt` (after `historyForExercise`):

```kotlin
@Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY loggedAtEpochMillis")
suspend fun forSession(sessionId: Long): List<SetLog>
```

- [ ] **Step 2: Replace `startOrGetTodaySession` in `WorkoutRepository`**

In `WorkoutRepository.kt`, remove the `startOrGetTodaySession` function (lines 46-50) and replace it with:

```kotlin
suspend fun activeSession(): WorkoutSession? = db.sessionDao().findActiveSession()

suspend fun lastClosedSession(): WorkoutSession? = db.sessionDao().findLastClosedSession()

suspend fun startSession(routineId: Long?): WorkoutSession {
    val now = System.currentTimeMillis()
    val session = WorkoutSession(date = LocalDate.now(), routineId = routineId, startEpochMillis = now, endEpochMillis = null)
    return session.copy(id = db.sessionDao().insert(session))
}

suspend fun endSession(sessionId: Long, endEpochMillis: Long) {
    db.sessionDao().closeSession(sessionId, endEpochMillis)
}

suspend fun setLogsForSession(sessionId: Long): List<SetLog> = db.setLogDao().forSession(sessionId)
```

- [ ] **Step 3: Build to confirm `WorkoutRepository` compiles standalone**

Run: `./gradlew compileDebugKotlin`
Expected: FAILS at this point — `LogViewModel.kt` still calls the now-removed `startOrGetTodaySession` (three call sites: `logSet`, `logCardioSet`, `logHoldSet`). This is expected; Task 8 fixes `LogViewModel`. Confirm the *only* compile errors reported are in `LogViewModel.kt`, not in `WorkoutRepository.kt`/`SetLogDao.kt` themselves — that isolates the change to the right file.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SetLogDao.kt app/src/main/java/com/lsing/timego/data/WorkoutRepository.kt
git commit -m "feat(data): replace startOrGetTodaySession with explicit session lifecycle methods"
```

---

### Task 4: Domain — `SessionLifecycle.kt` (auto-close decision)

**Files:**
- Create: `app/src/main/java/com/lsing/timego/domain/SessionLifecycle.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/SessionLifecycleTest.kt`

**Interfaces:**
- Produces: `enum class SessionAutoCloseDecision { STAY_ACTIVE, AUTO_CLOSE }`, `fun checkSessionAutoClose(lastSetLoggedAtEpochMillis: Long, nowEpochMillis: Long, inactivityThresholdMillis: Long = ONE_HOUR_MILLIS): SessionAutoCloseDecision`, `const val ONE_HOUR_MILLIS: Long`.

- [ ] **Step 1: Write the failing tests**

Create `SessionLifecycleTest.kt`:

```kotlin
package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionLifecycleTest {
    @Test
    fun `stays active when last set was well within the threshold`() {
        val lastSet = 0L
        val now = 30 * 60 * 1000L // 30 minutes later
        assertEquals(SessionAutoCloseDecision.STAY_ACTIVE, checkSessionAutoClose(lastSet, now))
    }

    @Test
    fun `auto-closes when last set was well past the threshold`() {
        val lastSet = 0L
        val now = 2 * 60 * 60 * 1000L // 2 hours later
        assertEquals(SessionAutoCloseDecision.AUTO_CLOSE, checkSessionAutoClose(lastSet, now))
    }

    @Test
    fun `stays active at exactly the threshold boundary`() {
        val lastSet = 0L
        val now = ONE_HOUR_MILLIS // exactly 1 hour later
        assertEquals(SessionAutoCloseDecision.STAY_ACTIVE, checkSessionAutoClose(lastSet, now))
    }

    @Test
    fun `auto-closes one millisecond past the threshold`() {
        val lastSet = 0L
        val now = ONE_HOUR_MILLIS + 1
        assertEquals(SessionAutoCloseDecision.AUTO_CLOSE, checkSessionAutoClose(lastSet, now))
    }

    @Test
    fun `respects a custom threshold`() {
        val lastSet = 0L
        val now = 10 * 60 * 1000L // 10 minutes later
        assertEquals(
            SessionAutoCloseDecision.AUTO_CLOSE,
            checkSessionAutoClose(lastSet, now, inactivityThresholdMillis = 5 * 60 * 1000L),
        )
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.SessionLifecycleTest"`
Expected: FAIL (compile error — `checkSessionAutoClose`/`SessionAutoCloseDecision`/`ONE_HOUR_MILLIS` don't exist yet)

- [ ] **Step 3: Implement**

Create `SessionLifecycle.kt`:

```kotlin
package com.lsing.timego.domain

const val ONE_HOUR_MILLIS: Long = 60 * 60 * 1000L

enum class SessionAutoCloseDecision { STAY_ACTIVE, AUTO_CLOSE }

/** Decides whether an active session should be auto-closed because its last logged set is more
 *  than [inactivityThresholdMillis] in the past. A pure function -- the caller is responsible for
 *  fetching [lastSetLoggedAtEpochMillis] (the active session's most recent SetLog) and actually
 *  writing the resulting end time via WorkoutRepository.endSession when this returns AUTO_CLOSE. */
fun checkSessionAutoClose(
    lastSetLoggedAtEpochMillis: Long,
    nowEpochMillis: Long,
    inactivityThresholdMillis: Long = ONE_HOUR_MILLIS,
): SessionAutoCloseDecision =
    if (nowEpochMillis - lastSetLoggedAtEpochMillis > inactivityThresholdMillis) {
        SessionAutoCloseDecision.AUTO_CLOSE
    } else {
        SessionAutoCloseDecision.STAY_ACTIVE
    }
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.SessionLifecycleTest"`
Expected: PASS (5 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/SessionLifecycle.kt app/src/test/java/com/lsing/timego/domain/SessionLifecycleTest.kt
git commit -m "feat(domain): add session auto-close decision logic"
```

---

### Task 5: Domain — `rankUntrainedMuscleGroups`

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/MuscleBalance.kt`
- Modify: `app/src/test/java/com/lsing/timego/domain/MuscleBalanceTest.kt`

**Interfaces:**
- Consumes: same shape as existing `untrainedMuscleGroups` (`allGroups: List<String>`, `lastTrainedByGroup: Map<String, LocalDate>`, `today: LocalDate`).
- Produces: `fun rankUntrainedMuscleGroups(allGroups: List<String>, lastTrainedByGroup: Map<String, LocalDate>, today: LocalDate): List<String>` — sorted most-neglected first. Never-trained groups (no entry in `lastTrainedByGroup`) rank above every trained group, regardless of how stale the trained ones are.

- [ ] **Step 1: Write the failing tests**

Add to `MuscleBalanceTest.kt`:

```kotlin
    @Test
    fun `rankUntrainedMuscleGroups sorts most-neglected first`() {
        val lastTrained = mapOf(
            "QUADS" to LocalDate.of(2026, 8, 10), // 4 days ago
            "CHEST" to LocalDate.of(2026, 8, 1),  // 13 days ago
        )
        val today = LocalDate.of(2026, 8, 14)

        val result = rankUntrainedMuscleGroups(
            allGroups = listOf("QUADS", "CHEST"),
            lastTrainedByGroup = lastTrained,
            today = today,
        )

        assertEquals(listOf("CHEST", "QUADS"), result)
    }

    @Test
    fun `rankUntrainedMuscleGroups ranks never-trained groups above stale-but-trained ones`() {
        val lastTrained = mapOf("CHEST" to LocalDate.of(2026, 1, 1)) // very stale, but trained at least once
        val today = LocalDate.of(2026, 8, 14)

        val result = rankUntrainedMuscleGroups(
            allGroups = listOf("QUADS", "CHEST"), // QUADS never trained
            lastTrainedByGroup = lastTrained,
            today = today,
        )

        assertEquals(listOf("QUADS", "CHEST"), result)
    }

    @Test
    fun `rankUntrainedMuscleGroups returns groups unordered by name when equally stale`() {
        val lastTrained = mapOf(
            "QUADS" to LocalDate.of(2026, 8, 5),
            "CHEST" to LocalDate.of(2026, 8, 5),
        )
        val today = LocalDate.of(2026, 8, 14)

        val result = rankUntrainedMuscleGroups(
            allGroups = listOf("QUADS", "CHEST"),
            lastTrainedByGroup = lastTrained,
            today = today,
        )

        assertEquals(setOf("QUADS", "CHEST"), result.toSet())
        assertEquals(2, result.size)
    }
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.MuscleBalanceTest"`
Expected: FAIL (compile error — `rankUntrainedMuscleGroups` doesn't exist)

- [ ] **Step 3: Implement**

Add to `MuscleBalance.kt`:

```kotlin
/** Same neglect signal as [untrainedMuscleGroups] but returns every group ranked by staleness
 *  (most-neglected first) instead of a threshold-filtered flag list -- backs the logging landing
 *  page's "recommended muscle group" pick (top of this list = best candidate for balanced
 *  growth). Never-trained groups (absent from [lastTrainedByGroup]) sort first, ahead of any
 *  trained-but-stale group, since "never" is more neglected than any finite number of days. */
fun rankUntrainedMuscleGroups(
    allGroups: List<String>,
    lastTrainedByGroup: Map<String, LocalDate>,
    today: LocalDate,
): List<String> = allGroups.sortedByDescending { group ->
    val last = lastTrainedByGroup[group] ?: return@sortedByDescending Long.MAX_VALUE
    ChronoUnit.DAYS.between(last, today)
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.MuscleBalanceTest"`
Expected: PASS (6 tests total: 3 existing + 3 new)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/MuscleBalance.kt app/src/test/java/com/lsing/timego/domain/MuscleBalanceTest.kt
git commit -m "feat(domain): add ranked untrained-muscle-group recommendation"
```

---

### Task 6: Domain — `muscleGroupsWorkedInSession`

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/MuscleBalance.kt`
- Modify: `app/src/test/java/com/lsing/timego/domain/MuscleBalanceTest.kt`

**Interfaces:**
- Produces: `fun muscleGroupsWorkedInSession(sessionId: Long, setLogs: List<SetLog>, exercises: List<Exercise>): Set<String>`. Written generically (session-scoped, not landing-page-specific) so backlog item 3's Progress-screen heatmap feature can call it directly later.

- [ ] **Step 1: Write the failing tests**

Add to `MuscleBalanceTest.kt`:

```kotlin
    @Test
    fun `muscleGroupsWorkedInSession unions muscle groups across a session's sets`() {
        val setLogs = listOf(
            SetLog(id = 1, sessionId = 10, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 10, exerciseId = 2, weightKg = 60.0, reps = 8, targetReps = 8, loggedAtEpochMillis = 0),
        )

        val result = muscleGroupsWorkedInSession(sessionId = 10, setLogs = setLogs, exercises = listOf(legsExercise, chestExercise))

        assertEquals(setOf("QUADS", "CHEST"), result)
    }

    @Test
    fun `muscleGroupsWorkedInSession ignores sets from other sessions`() {
        val setLogs = listOf(
            SetLog(id = 1, sessionId = 10, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 99, exerciseId = 2, weightKg = 60.0, reps = 8, targetReps = 8, loggedAtEpochMillis = 0),
        )

        val result = muscleGroupsWorkedInSession(sessionId = 10, setLogs = setLogs, exercises = listOf(legsExercise, chestExercise))

        assertEquals(setOf("QUADS"), result)
    }

    @Test
    fun `muscleGroupsWorkedInSession returns empty set for a session with no sets`() {
        val result = muscleGroupsWorkedInSession(sessionId = 10, setLogs = emptyList(), exercises = listOf(legsExercise))

        assertEquals(emptySet<String>(), result)
    }

    @Test
    fun `muscleGroupsWorkedInSession dedupes when two exercises share a muscle group`() {
        val secondLegsExercise = Exercise(id = 3, name = "Leg Press", muscleGroups = listOf("QUADS"), isCustom = false)
        val setLogs = listOf(
            SetLog(id = 1, sessionId = 10, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 10, exerciseId = 3, weightKg = 80.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0),
        )

        val result = muscleGroupsWorkedInSession(sessionId = 10, setLogs = setLogs, exercises = listOf(legsExercise, secondLegsExercise))

        assertEquals(setOf("QUADS"), result)
    }
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.MuscleBalanceTest"`
Expected: FAIL (compile error — `muscleGroupsWorkedInSession` doesn't exist)

- [ ] **Step 3: Implement**

Add to `MuscleBalance.kt` (needs `import com.lsing.timego.data.SetLog` and `Exercise`, both already imported at the top of this file):

```kotlin
/** Which muscle groups a session actually trained, derived from its logged sets -- shared by the
 *  logging landing page's last-session summary card (this spec) and, later, the Progress screen's
 *  heatmap workout-summary feature. Deliberately session-scoped rather than date-scoped: two
 *  sessions can share a calendar date now that WorkoutSession isn't date-unique, and this should
 *  answer "what did THIS session train," not "what was trained that whole day." */
fun muscleGroupsWorkedInSession(
    sessionId: Long,
    setLogs: List<SetLog>,
    exercises: List<Exercise>,
): Set<String> {
    val exercisesById = exercises.associateBy { it.id }
    return setLogs
        .filter { it.sessionId == sessionId }
        .flatMap { log -> exercisesById[log.exerciseId]?.muscleGroups.orEmpty() }
        .toSet()
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.MuscleBalanceTest"`
Expected: PASS (10 tests total)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/MuscleBalance.kt app/src/test/java/com/lsing/timego/domain/MuscleBalanceTest.kt
git commit -m "feat(domain): add shared muscle-groups-worked-in-session function"
```

---

### Task 7: Move `DayHistoryEntry`/`DayHistoryDialog`/`StatTile` into `ui/common`

**Files:**
- Create: `app/src/main/java/com/lsing/timego/ui/common/WorkoutHistoryDialog.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressViewModel.kt`

**Interfaces:**
- Produces: `data class DayHistoryEntry(val exerciseName: String, val description: String)`, `@Composable fun WorkoutHistoryDialog(title: String, entries: List<DayHistoryEntry>, onDismiss: () -> Unit)`, `@Composable fun StatTile(label: String, value: String, caption: String? = null, modifier: Modifier = Modifier)`. Both are currently private to `ProgressScreen.kt` — this task makes them public and shared so Task 9's landing page can reuse them without duplicating the table/tile UI.
- Consumes (unchanged behavior, just relocated): nothing new: this is a pure move/rename, no logic changes.

This task is a refactor with no new behavior — verified by the existing test suite and a build staying green, not new tests.

- [ ] **Step 1: Create the shared file**

Create `WorkoutHistoryDialog.kt`:

```kotlin
package com.lsing.timego.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lsing.timego.ui.theme.LedgerFigureValue
import com.lsing.timego.ui.theme.Spacing

data class DayHistoryEntry(val exerciseName: String, val description: String)

/** Set/Name/Reps-or-Duration table, one row per logged set -- shared between the Progress
 *  screen's tap-a-heatmap-day dialog (title = "Workout on <date>") and the logging landing page's
 *  last-session detail (title = "Last session"). [title] is caller-supplied rather than assuming
 *  a date, since the landing page's "last session" isn't itself date-keyed the way the heatmap's
 *  tap target is. */
@Composable
fun WorkoutHistoryDialog(title: String, entries: List<DayHistoryEntry>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (entries.isEmpty()) {
                Text("No sets logged.")
            } else {
                Column {
                    entries.forEachIndexed { index, entry ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text("${index + 1}", style = LedgerFigureValue.copy(fontSize = 14.sp), modifier = Modifier.padding(end = 12.dp))
                            Text(entry.exerciseName, modifier = Modifier.weight(1f))
                            Text(entry.description, style = LedgerFigureValue.copy(fontSize = 14.sp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
fun StatTile(label: String, value: String, caption: String? = null, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(Spacing.ExtraSmall),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = LedgerFigureValue)
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
```

- [ ] **Step 2: Remove the old private declarations from `ProgressScreen.kt`**

Delete the private `DayHistoryDialog` composable (previously lines 348-372) and the private `StatTile` composable (previously lines 376-390) from `ProgressScreen.kt` in full.

- [ ] **Step 3: Remove `DayHistoryEntry` from `ProgressViewModel.kt`, import the shared one**

In `ProgressViewModel.kt`, delete the `data class DayHistoryEntry(...)` declaration (line 29) and add:

```kotlin
import com.lsing.timego.ui.common.DayHistoryEntry
```

- [ ] **Step 4: Update `ProgressScreen.kt`'s call site and imports**

Add imports:

```kotlin
import com.lsing.timego.ui.common.DayHistoryEntry
import com.lsing.timego.ui.common.StatTile
import com.lsing.timego.ui.common.WorkoutHistoryDialog
```

Change the call site (previously calling the private `DayHistoryDialog`):

```kotlin
if (selectedHistoryDate != null) {
    WorkoutHistoryDialog(
        title = "Workout on ${selectedHistoryDate!!}",
        entries = historyForSelectedDate,
        onDismiss = { viewModel.selectHistoryDate(null) },
    )
}
```

Every other `StatTile(...)` call site in `ProgressScreen.kt` (the PR tiles, training-stats row) is unchanged — they now resolve to the imported shared composable instead of the deleted private one, same signature.

- [ ] **Step 5: Build to confirm no regressions**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all existing tests still pass (this task changes no logic, only where the code lives).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/common/WorkoutHistoryDialog.kt app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt app/src/main/java/com/lsing/timego/ui/progress/ProgressViewModel.kt
git commit -m "refactor(ui): move DayHistoryDialog/StatTile to ui/common for reuse"
```

---

### Task 8: `LogViewModel` — session state, auto-close check, landing data, start/end actions

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt`

**Interfaces:**
- Consumes: `WorkoutRepository.activeSession/lastClosedSession/startSession/endSession/setLogsForSession` (Task 3), `checkSessionAutoClose`/`SessionAutoCloseDecision` (Task 4), `rankUntrainedMuscleGroups`/`muscleGroupsWorkedInSession` (Tasks 5-6), `lastTrainedDatesByMuscleGroup` (existing), `DayHistoryEntry` (Task 7).
- Produces: `sealed interface SessionUiState`, `data class LastSessionSummary(val sets: Int, val muscleGroups: Set<String>, val durationMinutes: Long, val detail: List<DayHistoryEntry>)`, `LogViewModel.sessionState: StateFlow<SessionUiState>`, `LogViewModel.startSession(routineId: Long?)`, `LogViewModel.endActiveSession()`.

- [ ] **Step 1: Add the new state types and StateFlow**

At the top of `LogViewModel.kt`, after the existing imports, add:

```kotlin
sealed interface SessionUiState {
    data object Loading : SessionUiState
    data class NoActiveSession(val lastSession: LastSessionSummary?, val recommendedMuscleGroups: List<String>) : SessionUiState
    data class Active(val sessionId: Long) : SessionUiState
}

data class LastSessionSummary(
    val sets: Int,
    val muscleGroups: Set<String>,
    val durationMinutes: Long,
    val detail: List<DayHistoryEntry>,
)
```

Add these imports to `LogViewModel.kt`:

```kotlin
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.domain.SessionAutoCloseDecision
import com.lsing.timego.domain.checkSessionAutoClose
import com.lsing.timego.domain.lastTrainedDatesByMuscleGroup
import com.lsing.timego.domain.muscleGroupsWorkedInSession
import com.lsing.timego.domain.rankUntrainedMuscleGroups
import com.lsing.timego.ui.common.DayHistoryEntry
```

Add the StateFlow (with the other `MutableStateFlow` declarations):

```kotlin
private val _sessionState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
val sessionState: StateFlow<SessionUiState> = _sessionState.asStateFlow()
```

- [ ] **Step 2: Wire the auto-close check into `init`**

`refreshSessionState` (Step 3) reads `allExercises`, which is only populated inside the existing `repository.exercises.collect { ... }` block in `init` — calling it from an independent `launch` would race that collection and could run first against an empty `allExercises`. Call it from inside that same block instead, after `allExercises` is assigned, so it always sees a populated exercise list:

```kotlin
viewModelScope.launch {
    repository.seedMissingExercises(SEED_EXERCISES)
    repository.exercises.collect { list ->
        allExercises = list
        refreshSuggestions(list)
        refreshDisplayedExercises()
        refreshSessionState()
    }
}
```

This replaces the existing `repository.exercises.collect { ... }` block in `init` (the one already there — add the `refreshSessionState()` call as its fourth line, don't add a new block).

- [ ] **Step 3: Implement `refreshSessionState`, `startSession`, `endActiveSession`**

Add these functions to `LogViewModel`:

```kotlin
private suspend fun refreshSessionState() {
    val active = repository.activeSession()
    if (active != null) {
        val sets = repository.setLogsForSession(active.id)
        val lastSetTime = sets.maxOfOrNull { it.loggedAtEpochMillis }
        val decision = if (lastSetTime != null) {
            checkSessionAutoClose(lastSetTime, System.currentTimeMillis())
        } else {
            SessionAutoCloseDecision.STAY_ACTIVE // just started, nothing logged yet -- never auto-close an empty session
        }
        if (decision == SessionAutoCloseDecision.AUTO_CLOSE) {
            repository.endSession(active.id, lastSetTime!!)
            _sessionState.value = buildNoActiveSessionState()
            return
        }
        _sessionState.value = SessionUiState.Active(active.id)
    } else {
        _sessionState.value = buildNoActiveSessionState()
    }
}

private suspend fun buildNoActiveSessionState(): SessionUiState.NoActiveSession {
    val lastSession = repository.lastClosedSession()
    val summary = lastSession?.let { session ->
        val sets = repository.setLogsForSession(session.id)
        val exercisesById = allExercises.associateBy { it.id }
        val muscleGroups = muscleGroupsWorkedInSession(session.id, sets, allExercises)
        val detail = sets.mapNotNull { log ->
            val exercise = exercisesById[log.exerciseId] ?: return@mapNotNull null
            val description = when (exercise.loggingType) {
                LoggingType.DURATION_DISTANCE.name -> {
                    val distance = log.distanceKm?.let { " -- ${it}km" } ?: ""
                    "${log.durationMinutes ?: 0.0} min$distance"
                }
                LoggingType.HOLD.name -> "${log.holdSeconds ?: 0}s hold"
                else -> "${log.weightKg}kg x ${log.reps}"
            }
            DayHistoryEntry(exercise.name, description)
        }
        LastSessionSummary(
            sets = sets.size,
            muscleGroups = muscleGroups,
            durationMinutes = (session.endEpochMillis ?: session.startEpochMillis).minus(session.startEpochMillis) / 60_000,
            detail = detail,
        )
    }

    val allSets = repository.allSetLogs()
    val sessionDateById = repository.allSessions().associate { it.id to it.date }
    val exercisesById = allExercises.associateBy { it.id }
    val lastTrained = lastTrainedDatesByMuscleGroup(allSets, exercisesById, sessionDateById)
    val allGroups = MuscleGroup.entries.map { it.name }
    val recommended = rankUntrainedMuscleGroups(allGroups, lastTrained, LocalDate.now()).take(2)

    return SessionUiState.NoActiveSession(lastSession = summary, recommendedMuscleGroups = recommended)
}

fun startSession(routineId: Long?) {
    viewModelScope.launch {
        val session = repository.startSession(routineId)
        selectRoutine(routineId)
        _sessionState.value = SessionUiState.Active(session.id)
    }
}

fun endActiveSession() {
    val current = _sessionState.value
    if (current !is SessionUiState.Active) return
    viewModelScope.launch {
        repository.endSession(current.sessionId, System.currentTimeMillis())
        _sessionState.value = buildNoActiveSessionState()
    }
}
```

- [ ] **Step 4: Replace `startOrGetTodaySession` call sites**

`logSet`, `logCardioSet`, `logHoldSet` no longer look up "today's session" — they use the already-active session from `sessionState`. Replace all three functions:

```kotlin
fun logSet(exerciseId: Long, weightKg: Double, reps: Int, targetReps: Int) {
    val sessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId ?: return
    viewModelScope.launch {
        repository.logSet(sessionId, exerciseId, weightKg, reps, targetReps)
        refreshSuggestionForExercise(exerciseId)
    }
}

fun logCardioSet(exerciseId: Long, durationMinutes: Double, distanceKm: Double?) {
    val sessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId ?: return
    viewModelScope.launch {
        repository.logCardioSet(sessionId, exerciseId, durationMinutes, distanceKm)
    }
}

fun logHoldSet(exerciseId: Long, durationSeconds: Int, targetDurationSeconds: Int) {
    val sessionId = (_sessionState.value as? SessionUiState.Active)?.sessionId ?: return
    viewModelScope.launch {
        repository.logHoldSet(sessionId, exerciseId, durationSeconds, targetDurationSeconds)
        refreshSuggestionForExercise(exerciseId)
    }
}
```

(The `sessionId ?: return` guard is defensive — `LogScreen`, wired in Task 9, only renders these logging rows when `sessionState` is already `Active`, so this path shouldn't be reachable with no active session in practice.)

- [ ] **Step 5: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. `LogViewModel.kt` no longer references `startOrGetTodaySession` (removed in Task 3), so the compile errors noted in Task 3 Step 3 are now resolved.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt
git commit -m "feat(log): add session state machine, auto-close check, and landing data to LogViewModel"
```

---

### Task 9: `LogScreen` — landing page UI + End Session button

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt`

**Interfaces:**
- Consumes: `LogViewModel.sessionState` (Task 8), `SessionUiState`/`LastSessionSummary` (Task 8), `WorkoutHistoryDialog`/`StatTile`/`DayHistoryEntry` (Task 7), existing `viewModel.routines`/`selectedRoutineId`/`selectRoutine` (unchanged).

- [ ] **Step 1: Add imports**

Add to `LogScreen.kt`:

```kotlin
import androidx.compose.runtime.mutableStateOf
import com.lsing.timego.ui.common.StatTile
import com.lsing.timego.ui.common.WorkoutHistoryDialog
```

(`mutableStateOf` is already imported — skip if the linter flags a duplicate; the other two are new.)

- [ ] **Step 2: Branch `LogScreen` on `sessionState`**

Replace the body of `LogScreen` (the `@Composable fun LogScreen` function) with:

```kotlin
@Composable
fun LogScreen(viewModel: LogViewModel = viewModel()) {
    val sessionState by viewModel.sessionState.collectAsState()

    when (val state = sessionState) {
        is SessionUiState.Loading -> { /* nothing to render yet -- first frame only, resolves on the next recomposition */ }
        is SessionUiState.NoActiveSession -> LogLandingContent(
            state = state,
            routines = viewModel.routines.collectAsState().value,
            onStartSession = viewModel::startSession,
        )
        is SessionUiState.Active -> LoggingContent(viewModel = viewModel, onEndSession = viewModel::endActiveSession)
    }
}

@Composable
private fun LogLandingContent(
    state: SessionUiState.NoActiveSession,
    routines: List<com.lsing.timego.data.Routine>,
    onStartSession: (routineId: Long?) -> Unit,
) {
    var showLastSessionDetail by remember { mutableStateOf(false) }

    if (showLastSessionDetail && state.lastSession != null) {
        WorkoutHistoryDialog(
            title = "Last session",
            entries = state.lastSession.detail,
            onDismiss = { showLastSessionDetail = false },
        )
    }

    Column(modifier = Modifier.padding(Spacing.Large)) {
        SectionHeader("Last session", topPadding = Spacing.ExtraSmall)
        if (state.lastSession == null) {
            Text("No sessions logged yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Row(modifier = Modifier.fillMaxWidth().clickable { showLastSessionDetail = true }) {
                StatTile("Sets", "${state.lastSession.sets}", modifier = Modifier.weight(1f))
                StatTile("Duration", "${state.lastSession.durationMinutes} min", modifier = Modifier.weight(1f))
            }
            Text(
                "Trained: ${state.lastSession.muscleGroups.joinToString(", ") { com.lsing.timego.ui.common.formatEnumLabel(it) }.ifEmpty { "--" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.Small),
            )
        }

        SectionHeader("Recommended")
        if (state.recommendedMuscleGroups.isEmpty()) {
            Text("Everything's been trained recently -- nice balance.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                state.recommendedMuscleGroups.joinToString(", ") { com.lsing.timego.ui.common.formatEnumLabel(it) },
                style = LedgerFigureValue,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        SectionHeader("Start a session")
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Button(onClick = { onStartSession(null) }, modifier = Modifier.padding(end = Spacing.Small)) {
                Text("Freeform")
            }
            routines.forEach { routine ->
                Button(onClick = { onStartSession(routine.id) }, modifier = Modifier.padding(end = Spacing.Small)) {
                    Text(routine.name)
                }
            }
        }
    }
}
```

- [ ] **Step 3: Extract the existing logging UI into `LoggingContent`, add End Session**

Move everything that was previously inline in `LogScreen`'s `Scaffold` (the FAB, the "Session type" chip row, `ExerciseSections`) into a new composable:

```kotlin
@Composable
private fun LoggingContent(viewModel: LogViewModel, onEndSession: () -> Unit) {
    val exercises by viewModel.displayedExercises.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val holdSuggestions by viewModel.holdSuggestions.collectAsState()
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                    SectionHeader("Session type", topPadding = Spacing.ExtraSmall)
                    Button(onClick = onEndSession) { Text("End Session") }
                }
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
                    when (exercise.loggingType) {
                        LoggingType.HOLD.name -> HoldLogRow(
                            exerciseName = exercise.name,
                            category = exercise.category,
                            suggestion = holdSuggestions[exercise.id],
                            onLog = { duration, target -> viewModel.logHoldSet(exercise.id, duration, target) },
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
                            onLog = { weight, reps, target -> viewModel.logSet(exercise.id, weight, reps, target) },
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}
```

`selectRoutine(null)`/`selectRoutine(routine.id)` chip taps inside an active session let you switch which routine's exercises are displayed without ending the session — unchanged behavior from today, just now nested one level deeper under `LoggingContent`.

Note: since starting a session already asks Freeform-vs-Routine on the landing page (Task 8's `startSession(routineId)` already calls `selectRoutine(routineId)`), the chip row inside `LoggingContent` reflects that initial choice and still allows switching mid-session, same as the current app.

- [ ] **Step 4: Add `SessionUiState` import**

Add to `LogScreen.kt`:

```kotlin
import com.lsing.timego.ui.log.SessionUiState
```

(If `SessionUiState` was declared directly in `LogViewModel.kt` within the same package `com.lsing.timego.ui.log`, this import is actually redundant — same-package references don't need an explicit import. Omit it if the compiler flags it as unused.)

- [ ] **Step 5: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt
git commit -m "feat(log): add logging landing page and End Session button to LogScreen"
```

---

### Task 10: Full verification pass

**Files:** none (verification only)

- [ ] **Step 1: Full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests green (existing suite + the new `SessionLifecycleTest` and extended `MuscleBalanceTest` from Tasks 4-6).

- [ ] **Step 2: Full debug build + install**

Run: `./gradlew assembleDebug installDebug`
Expected: BUILD SUCCESSFUL, installs on the connected device (per this project's established toolchain notes in the vault, same as HeatP's).

- [ ] **Step 3: Hand off for on-device manual verification**

Per this project's established discipline (user verifies on-device, agent doesn't screenshot proactively), report the following checklist to the user rather than performing it:

- Open the Log tab fresh (no active session) → landing page appears with recommended muscle group and (if any prior session exists) a last-session summary card.
- Tap the last-session card → detail dialog opens showing the per-set table.
- Tap "Start New Session" (Freeform or a routine) → lands directly in the logging screen, exercises filtered correctly if a routine was picked.
- Log a set, background the app, reopen within an hour → resumes straight into the logging screen (landing page skipped).
- Log a set, then either wait over an hour or manually adjust device time forward, reopen → lands on the landing page with the just-finished session shown as "last session," sets/duration/muscle-groups all correct.
- Tap "End Session" mid-workout → immediately returns to the landing page.
- Start a session, log a set before midnight and another after midnight (or simulate via device clock) → confirm both land in the same session (the actual bug this plan fixes) rather than splitting in two.
- Existing screens (Progress, Routines) still work unchanged — the Progress screen's heatmap tap-a-day dialog still opens correctly (verifies Task 7's refactor didn't break it).

- [ ] **Step 4: Merge**

Once the user confirms all checklist items on-device:

```bash
git checkout master
git merge --ff-only session-model-landing-page
git branch -d session-model-landing-page
```

- [ ] **Step 5: Update the vault**

Update `TimeGo - Gym Progress Tracker.md`'s "Next-work backlog" section: mark item 1 (session model + landing page) done, note the branch/spec/plan paths and merge date, matching the pattern of every other completed backlog item in that file.
