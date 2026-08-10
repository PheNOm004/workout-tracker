# TimeGo Logging Field Accuracy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exercises log the fields that actually match how they're performed — weight+reps for lifts, duration+distance for cardio/warmup, and a new timed-hold field for isometric exercises (Plank, Dead Hang, etc.) — by giving every exercise an explicit `loggingType` independent of its display category, and fixing the domain logic (PRs, strength curves, muscle-volume distribution) that currently assumes category implies loggable shape.

**Architecture:** A new `LoggingType` enum stored on `Exercise` (parallel to the existing `category` string-storage pattern), a matching `holdSeconds`/`targetHoldSeconds` pair on `SetLog` (parallel to the existing `durationMinutes`/`distanceKm` pair), a new `HoldSuggester` domain component mirroring `RuleBasedOverloadSuggester`, and a new `HoldLogRow` UI composable mirroring `StrengthLogRow`/`CardioLogRow`. Existing domain functions (`personalRecords`, `muscleGroupStrengthCurve`, `muscleGroupVolumeDistribution`) switch their category-based filters to loggingType-based filters.

**Tech Stack:** Kotlin, Jetpack Compose, Room, JUnit (existing `app/src/test` unit-test setup, no Robolectric).

## Global Constraints

- Room schema version bumps 4 → 5 via a genuine `ALTER TABLE` migration. **Never** add `@ColumnInfo(defaultValue=...)` to the new Kotlin fields — this repo has a documented, previously-shipped-and-reverted bug where that annotation breaks Room's migration validation on real devices (see `Exercise.kt`'s comment on `category` and `TimeGoDatabase.kt`'s `MIGRATION_3_4` comment). Rely on the Kotlin-level default (`= LoggingType.WEIGHT_REPS.name`) plus the SQL `DEFAULT` clause in the `ALTER TABLE` statement instead.
- HOLD exercises never carry a weight in this pass — no weighted-hold support (explicitly deferred per the design spec).
- Real domain logic in this plan (unlike the visual-identity pass) — TDD throughout: write the failing test first for every new/changed domain function.
- Phone is connected — full on-device verification (install, log a Plank/Dead Hang/Wall Sit set, confirm PR + suggestion behavior) happens in the final task, before merge.
- Six exercises get `loggingType = HOLD`: **Plank, Side Plank, Wall Sit, L-Sit, Dead Hang, Superman**. Mountain Climber and Flutter Kick stay `WEIGHT_REPS` (reps-based).

---

### Task 1: Data model — LoggingType, schema migration, DAO, repository

**Files:**
- Create: `app/src/main/java/com/lsing/timego/data/LoggingType.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/Exercise.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/SetLog.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/TimeGoDatabase.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/ExerciseDao.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/WorkoutRepository.kt`

**Interfaces:**
- Produces: `enum class LoggingType { WEIGHT_REPS, HOLD, DURATION_DISTANCE }`; `Exercise.loggingType: String`; `SetLog.holdSeconds: Int?`; `SetLog.targetHoldSeconds: Int?`; `ExerciseDao.updateLoggingType(name: String, loggingType: String)`; `WorkoutRepository.logHoldSet(sessionId: Long, exerciseId: Long, durationSeconds: Int, targetDurationSeconds: Int)` — all consumed by later tasks.

- [ ] **Step 1: Create `LoggingType.kt`**

```kotlin
package com.lsing.timego.data

enum class LoggingType { WEIGHT_REPS, HOLD, DURATION_DISTANCE }
```

- [ ] **Step 2: Add `loggingType` to `Exercise.kt`**

Change the `Exercise` data class:
```kotlin
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroups: List<String>,
    val isCustom: Boolean,
    val category: String = ExerciseCategory.STRENGTH.name,
    val loggingType: String = LoggingType.WEIGHT_REPS.name,
)
```
Extend the class doc comment (currently explaining why `category` has no `@ColumnInfo(defaultValue=...)`) to also cover `loggingType`, since it's the same gotcha:
```kotlin
/** [muscleGroups] stores MuscleGroup enum names as strings (via Converters' fromStringList/
 *  toStringList), not the enum type directly, so Room's converter resolution stays unambiguous.
 *  [category] and [loggingType] store their enum names the same way.
 *
 *  Deliberately NOT annotated with @ColumnInfo(defaultValue=...) even though MIGRATION_1_2/
 *  MIGRATION_4_5 add these columns via `ALTER TABLE ... DEFAULT '...'` -- confirmed on a real
 *  device that Room's schema reader doesn't reflect an ALTER-added column's DEFAULT back through
 *  PRAGMA table_info in a way its validator accepts, so declaring the annotation makes Room
 *  reject every real migrated install with "Migration didn't properly handle: exercises" on
 *  open. Room still enforces NOT NULL at the Kotlin/insert level via these fields' non-null
 *  types either way. */
```

- [ ] **Step 3: Add `holdSeconds`/`targetHoldSeconds` to `SetLog.kt`**

```kotlin
package com.lsing.timego.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [targetReps] is the rep count the user was aiming for on this set -- the overload suggester
 *  compares [reps] against it to detect a missed target. [durationMinutes]/[distanceKm] are used
 *  instead of [weightKg]/[reps] for CARDIO/WARMUP exercises; [holdSeconds]/[targetHoldSeconds]
 *  are used instead for HOLD exercises (see LoggingType). [weightKg]/[reps] are 0.0/0 sentinels
 *  whenever a different pair applies -- callers branch on the logged exercise's loggingType,
 *  never on null-checking these fields, to know which pair is real. */
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
)
```

- [ ] **Step 4: Add `MIGRATION_4_5` and bump the database version in `TimeGoDatabase.kt`**

```kotlin
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN loggingType TEXT NOT NULL DEFAULT 'WEIGHT_REPS'")
        db.execSQL("ALTER TABLE set_logs ADD COLUMN holdSeconds INTEGER")
        db.execSQL("ALTER TABLE set_logs ADD COLUMN targetHoldSeconds INTEGER")
    }
}
```
Add it after `MIGRATION_3_4`. Change the `@Database` annotation's `version = 4` to `version = 5`. Change `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)` to `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)`.

- [ ] **Step 5: Add `updateLoggingType` to `ExerciseDao.kt`**

```kotlin
    @Query("UPDATE exercises SET loggingType = :loggingType WHERE name = :name")
    suspend fun updateLoggingType(name: String, loggingType: String)
```
Add it after the existing `getById` query.

- [ ] **Step 6: Update `WorkoutRepository.kt`**

Change `seedMissingExercises` to also sync `loggingType` on exercises that already exist by name but whose stored `loggingType` doesn't match the seed's (covers the six HOLD exercises on already-migrated devices, the same name-matching approach `seedMissingExercises` already uses for missing-row detection):
```kotlin
    /** Inserts any [seed] exercise whose name isn't already present -- NOT gated on the table
     *  being totally empty, since expanding the seed list (Update 1.1: 12 -> 119) must still
     *  reach devices that already have some exercises logged. Matches by name rather than id,
     *  since seed entries have no stable id across app versions. Also syncs [Exercise.loggingType]
     *  for exercises that already exist but whose seed loggingType has since changed (e.g. Plank
     *  reclassified WEIGHT_REPS -> HOLD) -- otherwise an already-migrated device would keep the
     *  stale value forever, since insertAll only touches missing rows. */
    suspend fun seedMissingExercises(seed: List<Exercise>) {
        val existingByName = exercises.first().associateBy { it.name }
        val missing = seed.filter { it.name !in existingByName.keys }
        if (missing.isNotEmpty()) db.exerciseDao().insertAll(missing)
        seed.forEach { seedExercise ->
            val existing = existingByName[seedExercise.name]
            if (existing != null && existing.loggingType != seedExercise.loggingType) {
                db.exerciseDao().updateLoggingType(seedExercise.name, seedExercise.loggingType)
            }
        }
    }
```

Change `addCustomExercise` to derive `loggingType` from the chosen category, so a custom CARDIO/WARMUP exercise doesn't silently get the wrong default (`WEIGHT_REPS`):
```kotlin
    suspend fun addCustomExercise(name: String, muscleGroups: List<String>, category: String): Long {
        val loggingType = if (category == ExerciseCategory.CARDIO.name || category == ExerciseCategory.WARMUP.name) {
            LoggingType.DURATION_DISTANCE.name
        } else {
            LoggingType.WEIGHT_REPS.name
        }
        return db.exerciseDao().insert(Exercise(name = name, muscleGroups = muscleGroups, isCustom = true, category = category, loggingType = loggingType))
    }
```
Add `import com.lsing.timego.data.LoggingType` — already in the same package, so no import needed (both `Exercise.kt` and `WorkoutRepository.kt` are in `com.lsing.timego.data`). Confirm no new import is actually required; `ExerciseCategory` is also same-package.

Add `logHoldSet`, mirroring `logCardioSet`:
```kotlin
    suspend fun logHoldSet(sessionId: Long, exerciseId: Long, durationSeconds: Int, targetDurationSeconds: Int) {
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
            ),
        )
    }
```
Add this method after `logCardioSet`.

- [ ] **Step 7: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/LoggingType.kt app/src/main/java/com/lsing/timego/data/Exercise.kt app/src/main/java/com/lsing/timego/data/SetLog.kt app/src/main/java/com/lsing/timego/data/TimeGoDatabase.kt app/src/main/java/com/lsing/timego/data/ExerciseDao.kt app/src/main/java/com/lsing/timego/data/WorkoutRepository.kt
git commit -m "Add LoggingType data model, schema migration, and repository support"
```

---

### Task 2: Seed data curation

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SeedExercises.kt`
- Create: `app/src/test/java/com/lsing/timego/data/SeedExercisesTest.kt`

**Interfaces:**
- Consumes: `LoggingType` (Task 1).
- Produces: every `SEED_EXERCISES` entry now has a curated `loggingType` — consumed by Task 1's `seedMissingExercises` sync logic (already written) once the app runs, and directly asserted by this task's own test.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.lsing.timego.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SeedExercisesTest {
    private val holdExerciseNames = setOf("Plank", "Side Plank", "Wall Sit", "L-Sit", "Dead Hang", "Superman")

    @Test
    fun `exactly the curated hold exercises are tagged HOLD`() {
        val actualHoldNames = SEED_EXERCISES.filter { it.loggingType == LoggingType.HOLD.name }.map { it.name }.toSet()
        assertEquals(holdExerciseNames, actualHoldNames)
    }

    @Test
    fun `every STRENGTH exercise is WEIGHT_REPS`() {
        val strengthExercises = SEED_EXERCISES.filter { it.category == ExerciseCategory.STRENGTH.name }
        assertEquals(true, strengthExercises.all { it.loggingType == LoggingType.WEIGHT_REPS.name })
    }

    @Test
    fun `every CARDIO and WARMUP exercise is DURATION_DISTANCE`() {
        val durationExercises = SEED_EXERCISES.filter {
            it.category == ExerciseCategory.CARDIO.name || it.category == ExerciseCategory.WARMUP.name
        }
        assertEquals(true, durationExercises.all { it.loggingType == LoggingType.DURATION_DISTANCE.name })
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.data.SeedExercisesTest"`
Expected: FAIL (`loggingType` doesn't exist on the builder calls' resulting exercises the way the test expects yet -- specifically the HOLD set will be empty since no exercise is tagged HOLD).

- [ ] **Step 3: Update the builder functions and the six HOLD call sites**

Change the four private builder functions at the top of `SeedExercises.kt`:
```kotlin
private fun strength(name: String, vararg groups: MuscleGroup) =
    Exercise(name = name, muscleGroups = groups.map { it.name }, isCustom = false, category = ExerciseCategory.STRENGTH.name, loggingType = LoggingType.WEIGHT_REPS.name)

private fun calisthenics(name: String, vararg groups: MuscleGroup, loggingType: LoggingType = LoggingType.WEIGHT_REPS) =
    Exercise(name = name, muscleGroups = groups.map { it.name }, isCustom = false, category = ExerciseCategory.CALISTHENICS.name, loggingType = loggingType.name)

private fun warmup(name: String, vararg groups: MuscleGroup) =
    Exercise(name = name, muscleGroups = groups.map { it.name }, isCustom = false, category = ExerciseCategory.WARMUP.name, loggingType = LoggingType.DURATION_DISTANCE.name)

private fun cardio(name: String, vararg groups: MuscleGroup) =
    Exercise(name = name, muscleGroups = groups.map { it.name }, isCustom = false, category = ExerciseCategory.CARDIO.name, loggingType = LoggingType.DURATION_DISTANCE.name)
```

Change these six lines in the `calisthenics` section of `SEED_EXERCISES` (add `loggingType = LoggingType.HOLD` as a named argument after the muscle groups):
```kotlin
    calisthenics("Plank", MuscleGroup.ABS, loggingType = LoggingType.HOLD),
    calisthenics("Side Plank", MuscleGroup.OBLIQUES, loggingType = LoggingType.HOLD),
```
(replacing the existing `calisthenics("Plank", MuscleGroup.ABS)` and `calisthenics("Side Plank", MuscleGroup.OBLIQUES)` lines)
```kotlin
    calisthenics("Superman", MuscleGroup.LOWER_BACK, MuscleGroup.ABS, loggingType = LoggingType.HOLD),
```
(replacing `calisthenics("Superman", MuscleGroup.LOWER_BACK, MuscleGroup.ABS)`)
```kotlin
    calisthenics("Wall Sit", MuscleGroup.QUADS, loggingType = LoggingType.HOLD),
```
(replacing `calisthenics("Wall Sit", MuscleGroup.QUADS)`)
```kotlin
    calisthenics("L-Sit", MuscleGroup.ABS, loggingType = LoggingType.HOLD),
```
(replacing `calisthenics("L-Sit", MuscleGroup.ABS)`)
```kotlin
    calisthenics("Dead Hang", MuscleGroup.FOREARMS, MuscleGroup.LATS, loggingType = LoggingType.HOLD),
```
(replacing `calisthenics("Dead Hang", MuscleGroup.FOREARMS, MuscleGroup.LATS)`)

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.data.SeedExercisesTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SeedExercises.kt app/src/test/java/com/lsing/timego/data/SeedExercisesTest.kt
git commit -m "Curate loggingType across the seed exercise library"
```

---

### Task 3: HoldSuggester domain component

**Files:**
- Create: `app/src/main/java/com/lsing/timego/domain/HoldSuggester.kt`
- Create: `app/src/test/java/com/lsing/timego/domain/HoldSuggesterTest.kt`

**Interfaces:**
- Produces: `data class HoldPerformance(val durationSeconds: Int, val targetDurationSeconds: Int)`; `data class HoldSuggestion(val targetDurationSeconds: Int, val note: String)`; `class RuleBasedHoldSuggester { fun suggestNext(history: List<HoldPerformance>): HoldSuggestion? }` — consumed by Task 6 (LogViewModel).

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HoldSuggesterTest {
    private val suggester = RuleBasedHoldSuggester()

    @Test
    fun `no history returns null`() {
        assertNull(suggester.suggestNext(emptyList()))
    }

    @Test
    fun `hit target hold suggests a longer duration`() {
        val history = listOf(HoldPerformance(durationSeconds = 30, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history)
        assertEquals(35, result!!.targetDurationSeconds)
    }

    @Test
    fun `missed target hold suggests the same target`() {
        val history = listOf(HoldPerformance(durationSeconds = 20, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history)
        assertEquals(30, result!!.targetDurationSeconds)
    }

    @Test
    fun `missed target twice in a row triggers deload`() {
        val history = listOf(
            HoldPerformance(durationSeconds = 20, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 22, targetDurationSeconds = 30),
        )
        val result = suggester.suggestNext(history)
        assertEquals(27, result!!.targetDurationSeconds)
        assertEquals("Deload: missed target hold twice in a row", result.note)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.domain.HoldSuggesterTest"`
Expected: FAIL (compile error -- `RuleBasedHoldSuggester`/`HoldPerformance`/`HoldSuggestion` don't exist yet).

- [ ] **Step 3: Create `HoldSuggester.kt`**

```kotlin
package com.lsing.timego.domain

data class HoldPerformance(val durationSeconds: Int, val targetDurationSeconds: Int)

data class HoldSuggestion(val targetDurationSeconds: Int, val note: String)

interface HoldSuggester {
    fun suggestNext(history: List<HoldPerformance>): HoldSuggestion?
}

/** Same deterministic, on-device, no-ML philosophy as [RuleBasedOverloadSuggester], applied to
 *  timed holds instead of weight+reps. Deload triggers only on the last TWO logged holds both
 *  missing target duration, so a single off day doesn't force a target drop. */
class RuleBasedHoldSuggester : HoldSuggester {
    override fun suggestNext(history: List<HoldPerformance>): HoldSuggestion? {
        if (history.isEmpty()) return null
        val last = history.last()
        val lastTwo = history.takeLast(2)
        val missedLastTwo = lastTwo.size == 2 && lastTwo.all { it.durationSeconds < it.targetDurationSeconds }
        return when {
            missedLastTwo -> HoldSuggestion(
                targetDurationSeconds = (last.targetDurationSeconds * 0.9).toInt(),
                note = "Deload: missed target hold twice in a row",
            )
            last.durationSeconds >= last.targetDurationSeconds -> HoldSuggestion(
                targetDurationSeconds = last.targetDurationSeconds + 5,
                note = "Increase hold: hit target last time",
            )
            else -> HoldSuggestion(
                targetDurationSeconds = last.targetDurationSeconds,
                note = "Same target, aim to hold longer",
            )
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.domain.HoldSuggesterTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/HoldSuggester.kt app/src/test/java/com/lsing/timego/domain/HoldSuggesterTest.kt
git commit -m "Add RuleBasedHoldSuggester for timed-hold progressive overload"
```

---

### Task 4: PR and strength-curve filtering by loggingType

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/ProgressMath.kt`
- Modify: `app/src/test/java/com/lsing/timego/domain/ProgressMathTest.kt`

**Interfaces:**
- Consumes: `LoggingType` (Task 1).
- Produces: `PrType` gains `LONGEST_HOLD`; `personalRecords` now also returns `LONGEST_HOLD` records for HOLD exercises — consumed by Task 8 (ProgressScreen).

- [ ] **Step 1: Update existing test fixtures that depend on category-based exclusion**

In `ProgressMathTest.kt`, the two tests that construct a CARDIO `Exercise` (`personalRecords excludes cardio and warmup sets...` and `muscleGroupStrengthCurve excludes cardio sets...`) currently rely on `Exercise`'s default `loggingType` accidentally NOT matching WEIGHT_REPS -- once the filter switches to loggingType-based, that default (`WEIGHT_REPS`) would be wrong for a CARDIO fixture and silently make these tests pass for the wrong reason (or fail, once real filtering changes). Make the fixtures explicit: change
```kotlin
        val run = com.lsing.timego.data.Exercise(id = 3, name = "Running", muscleGroups = listOf("FULL_BODY"), isCustom = false, category = "CARDIO")
```
to
```kotlin
        val run = com.lsing.timego.data.Exercise(id = 3, name = "Running", muscleGroups = listOf("FULL_BODY"), isCustom = false, category = "CARDIO", loggingType = "DURATION_DISTANCE")
```
and change
```kotlin
        val cycling = com.lsing.timego.data.Exercise(id = 2, name = "Cycling", muscleGroups = listOf("QUADS"), isCustom = false, category = "CARDIO")
```
to
```kotlin
        val cycling = com.lsing.timego.data.Exercise(id = 2, name = "Cycling", muscleGroups = listOf("QUADS"), isCustom = false, category = "CARDIO", loggingType = "DURATION_DISTANCE")
```

- [ ] **Step 2: Write the new failing tests**

Append to `ProgressMathTest.kt`:
```kotlin
    @Test
    fun `personalRecords computes longest hold for HOLD exercises`() {
        val plank = com.lsing.timego.data.Exercise(id = 4, name = "Plank", muscleGroups = listOf("ABS"), isCustom = false, category = "CALISTHENICS", loggingType = "HOLD")
        val logs = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 4, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 0, holdSeconds = 30, targetHoldSeconds = 30),
            SetLog(id = 2, sessionId = 2, exerciseId = 4, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 0, holdSeconds = 45, targetHoldSeconds = 35),
        )
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 1), 2L to LocalDate.of(2026, 8, 8))
        val exercisesById = mapOf(4L to plank)

        val records = personalRecords(logs, sessionDateById, exercisesById)

        assertEquals(1, records.size)
        assertEquals(PrType.LONGEST_HOLD, records[0].type)
        assertEquals(45.0, records[0].value, 0.001)
        assertEquals(LocalDate.of(2026, 8, 8), records[0].achievedOn)
    }

    @Test
    fun `muscleGroupStrengthCurve excludes HOLD sets, which have no meaningful weight`() {
        val squat = com.lsing.timego.data.Exercise(id = 1, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false, category = "STRENGTH", loggingType = "WEIGHT_REPS")
        val wallSit = com.lsing.timego.data.Exercise(id = 5, name = "Wall Sit", muscleGroups = listOf("QUADS"), isCustom = false, category = "CALISTHENICS", loggingType = "HOLD")
        val logs = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 2, exerciseId = 5, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 0, holdSeconds = 60, targetHoldSeconds = 60),
        )
        val exercisesById = mapOf(1L to squat, 5L to wallSit)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 1), 2L to LocalDate.of(2026, 8, 2))

        val curve = muscleGroupStrengthCurve(logs, exercisesById, sessionDateById, "QUADS")

        assertEquals(1, curve.size)
        assertEquals(LocalDate.of(2026, 8, 1), curve[0].first)
    }
```

- [ ] **Step 3: Run the tests to verify the new ones fail and the updated fixtures still compile**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.domain.ProgressMathTest"`
Expected: FAIL on the two new tests (`PrType.LONGEST_HOLD` doesn't exist, `personalRecords` doesn't compute hold records yet).

- [ ] **Step 4: Update `ProgressMath.kt`**

Remove the `ExerciseCategory` import and the `STRENGTH_CATEGORIES` val (both now unused/replaced); add the `LoggingType` import:
```kotlin
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import java.time.LocalDate
```
(delete the `import com.lsing.timego.data.ExerciseCategory` line and the `internal val STRENGTH_CATEGORIES = ...` line)

Change `PrType`:
```kotlin
enum class PrType { HEAVIEST_WEIGHT, MOST_REPS, BEST_VOLUME, LONGEST_HOLD }
```

Replace `personalRecords`:
```kotlin
/** Computed fresh from full history each time (not incrementally tracked) -- simpler and correct
 *  by construction; history sizes here are small enough (one person's own lifts) that this is
 *  cheap even called on every Progress screen load. Grouped per exercise -- a global "heaviest
 *  weight across everything" would mix e.g. a squat and a bicep curl together, which is
 *  meaningless. CARDIO/WARMUP sets are excluded entirely via the loggingType check: their
 *  weightKg/reps are 0.0/0 sentinels (see SetLog), not real values worth ranking. HOLD exercises
 *  get their own LONGEST_HOLD record computed separately, since weightKg/reps are sentinels for
 *  them too but holdSeconds is real. */
fun personalRecords(
    history: List<SetLog>,
    sessionDateById: Map<Long, LocalDate>,
    exercisesById: Map<Long, Exercise>,
): List<PersonalRecord> {
    val weightRepsRecords = history
        .filter { log -> exercisesById[log.exerciseId]?.loggingType == LoggingType.WEIGHT_REPS.name }
        .groupBy { it.exerciseId }
        .flatMap { (exerciseId, sets) ->
            val heaviest = sets.maxBy { it.weightKg }
            val mostReps = sets.maxBy { it.reps }
            val bestVolume = sets.maxBy { it.weightKg * it.reps }
            listOfNotNull(
                sessionDateById[heaviest.sessionId]?.let { PersonalRecord(exerciseId, PrType.HEAVIEST_WEIGHT, heaviest.weightKg, it) },
                sessionDateById[mostReps.sessionId]?.let { PersonalRecord(exerciseId, PrType.MOST_REPS, mostReps.reps.toDouble(), it) },
                sessionDateById[bestVolume.sessionId]?.let { PersonalRecord(exerciseId, PrType.BEST_VOLUME, bestVolume.weightKg * bestVolume.reps, it) },
            )
        }
    val holdRecords = history
        .filter { log -> exercisesById[log.exerciseId]?.loggingType == LoggingType.HOLD.name }
        .groupBy { it.exerciseId }
        .flatMap { (exerciseId, sets) ->
            val longest = sets.maxBy { it.holdSeconds ?: 0 }
            listOfNotNull(
                sessionDateById[longest.sessionId]?.let { PersonalRecord(exerciseId, PrType.LONGEST_HOLD, (longest.holdSeconds ?: 0).toDouble(), it) },
            )
        }
    return weightRepsRecords + holdRecords
}
```

Change the filter line inside `muscleGroupStrengthCurve` from
```kotlin
        if (exercise.category !in STRENGTH_CATEGORIES) continue
```
to
```kotlin
        if (exercise.loggingType != LoggingType.WEIGHT_REPS.name) continue
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.domain.ProgressMathTest"`
Expected: PASS (all tests, including the two new ones).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/ProgressMath.kt app/src/test/java/com/lsing/timego/domain/ProgressMathTest.kt
git commit -m "Filter PRs and strength curve by loggingType, add LONGEST_HOLD"
```

---

### Task 5: Muscle-volume distribution includes hold-based training

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/MuscleDistribution.kt`
- Modify: `app/src/test/java/com/lsing/timego/domain/MuscleDistributionTest.kt`

**Interfaces:**
- Consumes: `LoggingType` (Task 1).
- No signature change to `muscleGroupVolumeDistribution` -- same parameters, same return type, only the internal filter and per-set volume calculation change.

- [ ] **Step 1: Update the existing test fixture**

In `MuscleDistributionTest.kt`, the `muscleGroupVolumeDistribution excludes cardio and warmup sets` test's `run` fixture needs an explicit `loggingType` for the same reason as Task 4's fixtures (its default would otherwise be `WEIGHT_REPS`, which is wrong for a CARDIO exercise and would defeat the point of the exclusion test once filtering switches to loggingType). Change:
```kotlin
        val run = Exercise(id = 3, name = "Running", muscleGroups = listOf("QUADS"), isCustom = false, category = "CARDIO")
```
to
```kotlin
        val run = Exercise(id = 3, name = "Running", muscleGroups = listOf("QUADS"), isCustom = false, category = "CARDIO", loggingType = "DURATION_DISTANCE")
```

- [ ] **Step 2: Write the new failing test**

Append to `MuscleDistributionTest.kt`:
```kotlin
    @Test
    fun `muscleGroupVolumeDistribution counts hold seconds as volume for HOLD exercises`() {
        val plank = Exercise(id = 4, name = "Plank", muscleGroups = listOf("ABS"), isCustom = false, category = "CALISTHENICS", loggingType = "HOLD")
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 4, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 0, holdSeconds = 40, targetHoldSeconds = 40),
        )
        val exercisesById = mapOf(4L to plank)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 10))

        val distribution = muscleGroupVolumeDistribution(sets, exercisesById, sessionDateById, since = LocalDate.of(2026, 8, 1))

        assertEquals(40.0, distribution["ABS"]!!, 0.001)
    }
```

- [ ] **Step 3: Run the tests to verify the new one fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.domain.MuscleDistributionTest"`
Expected: FAIL on the new test (Plank's HOLD set is currently excluded entirely, so `distribution["ABS"]` is null).

- [ ] **Step 4: Update `MuscleDistribution.kt`**

```kotlin
package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import java.time.LocalDate

/** Total volume per muscle group across WEIGHT_REPS/HOLD sets logged on or after [since] -- an
 *  exercise contributes its full volume to every muscle group it's tagged with (e.g. a squat set
 *  counts toward both QUADS and GLUTES). Backs the "muscle distribution" radar chart and the
 *  muscle-body heatmap. CARDIO/WARMUP excluded, same reasoning as personalRecords/
 *  muscleGroupStrengthCurve -- their weightKg/reps are 0.0/0 sentinels, not real values. HOLD
 *  exercises use holdSeconds directly as their "volume" figure: a rough proxy, not weight-
 *  equivalent, kept only so a muscle trained purely via holds doesn't show as untrained --
 *  superseded once the muscle-correlation model is redesigned with real per-muscle weighting. */
fun muscleGroupVolumeDistribution(
    history: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sessionDateById: Map<Long, LocalDate>,
    since: LocalDate,
): Map<String, Double> {
    val volumeByGroup = mutableMapOf<String, Double>()
    for (log in history) {
        val exercise = exercisesById[log.exerciseId] ?: continue
        val volume = when (exercise.loggingType) {
            LoggingType.WEIGHT_REPS.name -> log.weightKg * log.reps
            LoggingType.HOLD.name -> (log.holdSeconds ?: 0).toDouble()
            else -> continue
        }
        val date = sessionDateById[log.sessionId] ?: continue
        if (date.isBefore(since)) continue
        for (group in exercise.muscleGroups) {
            volumeByGroup[group] = (volumeByGroup[group] ?: 0.0) + volume
        }
    }
    return volumeByGroup
}

data class TrainingStats(
    val workouts: Int,
    val totalDurationMinutes: Double,
    val totalVolumeKg: Double,
    val totalSets: Int,
)

/** [totalDurationMinutes] is an estimate -- sessions have no explicit start/end time, so it's the
 *  span between each session's first and last logged set timestamp, summed across sessions. A
 *  session with only one set contributes 0 (no span to measure). */
fun trainingStats(sessions: List<WorkoutSession>, sets: List<SetLog>, since: LocalDate): TrainingStats {
    val sessionIdsSince = sessions.filter { !it.date.isBefore(since) }.map { it.id }.toSet()
    val setsSince = sets.filter { it.sessionId in sessionIdsSince }
    val durationMinutes = setsSince.groupBy { it.sessionId }
        .values
        .sumOf { sessionSets ->
            val timestamps = sessionSets.map { it.loggedAtEpochMillis }
            ((timestamps.max() - timestamps.min()) / 60_000.0)
        }
    return TrainingStats(
        workouts = sessionIdsSince.size,
        totalDurationMinutes = durationMinutes,
        totalVolumeKg = setsSince.sumOf { it.weightKg * it.reps },
        totalSets = setsSince.size,
    )
}
```
(`trainingStats` is unchanged -- included above only for full-file context since the whole file is small; do not alter its body.)

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.domain.MuscleDistributionTest"`
Expected: PASS (all tests, including the two new/updated ones).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/MuscleDistribution.kt app/src/test/java/com/lsing/timego/domain/MuscleDistributionTest.kt
git commit -m "Include HOLD-exercise duration as muscle-volume proxy"
```

---

### Task 6: Wire hold logging into LogViewModel

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt`

**Interfaces:**
- Consumes: `RuleBasedHoldSuggester`, `HoldPerformance`, `HoldSuggestion` (Task 3); `WorkoutRepository.logHoldSet` (Task 1).
- Produces: `LogViewModel.holdSuggestions: StateFlow<Map<Long, HoldSuggestion>>`; `LogViewModel.logHoldSet(exerciseId: Long, durationSeconds: Int, targetDurationSeconds: Int)` -- consumed by Task 7 (LogScreen).

- [ ] **Step 1: Replace the full contents of `LogViewModel.kt`**

```kotlin
package com.lsing.timego.ui.log

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.Routine
import com.lsing.timego.data.SEED_EXERCISES
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.WorkoutRepository
import com.lsing.timego.domain.HoldPerformance
import com.lsing.timego.domain.HoldSuggestion
import com.lsing.timego.domain.OverloadSuggestion
import com.lsing.timego.domain.RuleBasedHoldSuggester
import com.lsing.timego.domain.RuleBasedOverloadSuggester
import com.lsing.timego.domain.SetPerformance
import com.lsing.timego.domain.routinesForToday
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** [selectedRoutineId] null means freeform (all exercises shown, sessions logged with no routine
 *  link); non-null filters [displayedExercises] to that routine's exercises and tags logged
 *  sessions with it. On first load, if today has a scheduled routine, it's auto-selected instead
 *  of defaulting to freeform -- that's the whole point of routine scheduling (Update 1.1). */
class LogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(TimeGoDatabase.getInstance(application))
    private val suggester = RuleBasedOverloadSuggester()
    private val holdSuggester = RuleBasedHoldSuggester()

    private var allExercises: List<Exercise> = emptyList()
    private var hasAutoSelectedTodaysRoutine = false

    private val _displayedExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val displayedExercises: StateFlow<List<Exercise>> = _displayedExercises.asStateFlow()

    private val _suggestions = MutableStateFlow<Map<Long, OverloadSuggestion>>(emptyMap())
    val suggestions: StateFlow<Map<Long, OverloadSuggestion>> = _suggestions.asStateFlow()

    private val _holdSuggestions = MutableStateFlow<Map<Long, HoldSuggestion>>(emptyMap())
    val holdSuggestions: StateFlow<Map<Long, HoldSuggestion>> = _holdSuggestions.asStateFlow()

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    private val _selectedRoutineId = MutableStateFlow<Long?>(null)
    val selectedRoutineId: StateFlow<Long?> = _selectedRoutineId.asStateFlow()

    private val _latestBodyWeightKg = MutableStateFlow<Double?>(null)
    val latestBodyWeightKg: StateFlow<Double?> = _latestBodyWeightKg.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedMissingExercises(SEED_EXERCISES)
            repository.exercises.collect { list ->
                allExercises = list
                refreshSuggestions(list)
                refreshDisplayedExercises()
            }
        }
        viewModelScope.launch {
            repository.routines.collect { routineList ->
                _routines.value = routineList
                if (!hasAutoSelectedTodaysRoutine) {
                    hasAutoSelectedTodaysRoutine = true
                    routinesForToday(routineList, LocalDate.now().dayOfWeek).firstOrNull()?.let {
                        selectRoutine(it.id)
                    }
                }
            }
        }
        viewModelScope.launch {
            _latestBodyWeightKg.value = repository.latestBodyWeightKg()
        }
    }

    fun selectRoutine(routineId: Long?) {
        _selectedRoutineId.value = routineId
        viewModelScope.launch { refreshDisplayedExercises() }
    }

    private suspend fun refreshDisplayedExercises() {
        val routineId = _selectedRoutineId.value
        _displayedExercises.value = if (routineId == null) {
            allExercises
        } else {
            val exerciseIds = repository.exercisesForRoutine(routineId).map { it.exerciseId }.toSet()
            allExercises.filter { it.id in exerciseIds }
        }
    }

    /** Splits suggestion computation by loggingType: WEIGHT_REPS exercises get a weight/reps
     *  suggestion from [suggester], HOLD exercises get a duration suggestion from [holdSuggester]
     *  -- an exercise can only produce one kind, so each history is built from the fields that
     *  are real for that exercise (see SetLog's doc comment on its sentinel-field convention). */
    private suspend fun refreshSuggestions(exerciseList: List<Exercise>) {
        val historyByExercise = repository.allSetLogsOrderedByTime().groupBy { it.exerciseId }
        val map = mutableMapOf<Long, OverloadSuggestion>()
        val holdMap = mutableMapOf<Long, HoldSuggestion>()
        for (exercise in exerciseList) {
            val history = historyByExercise[exercise.id].orEmpty()
            if (exercise.loggingType == LoggingType.HOLD.name) {
                val holdHistory = history.map { HoldPerformance(it.holdSeconds ?: 0, it.targetHoldSeconds ?: 0) }
                holdSuggester.suggestNext(holdHistory)?.let { holdMap[exercise.id] = it }
            } else {
                val performanceHistory = history.map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
                suggester.suggestNext(performanceHistory)?.let { map[exercise.id] = it }
            }
        }
        _suggestions.value = map
        _holdSuggestions.value = holdMap
    }

    fun logSet(exerciseId: Long, weightKg: Double, reps: Int, targetReps: Int) {
        viewModelScope.launch {
            val session = repository.startOrGetTodaySession(routineId = _selectedRoutineId.value)
            repository.logSet(session.id, exerciseId, weightKg, reps, targetReps)
            refreshSuggestions(allExercises)
        }
    }

    fun logCardioSet(exerciseId: Long, durationMinutes: Double, distanceKm: Double?) {
        viewModelScope.launch {
            val session = repository.startOrGetTodaySession(routineId = _selectedRoutineId.value)
            repository.logCardioSet(session.id, exerciseId, durationMinutes, distanceKm)
        }
    }

    fun logHoldSet(exerciseId: Long, durationSeconds: Int, targetDurationSeconds: Int) {
        viewModelScope.launch {
            val session = repository.startOrGetTodaySession(routineId = _selectedRoutineId.value)
            repository.logHoldSet(session.id, exerciseId, durationSeconds, targetDurationSeconds)
            refreshSuggestions(allExercises)
        }
    }

    fun addCustomExercise(name: String, muscleGroups: List<String>, category: String) {
        viewModelScope.launch {
            repository.addCustomExercise(name, muscleGroups, category)
        }
    }
}
```

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt
git commit -m "Wire hold-set logging and suggestions into LogViewModel"
```

---

### Task 7: HoldLogRow and three-way branch in LogScreen

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt`

**Interfaces:**
- Consumes: `LogViewModel.holdSuggestions`, `LogViewModel.logHoldSet` (Task 6); `HoldSuggestion` (Task 3); `LoggingType` (Task 1).

- [ ] **Step 1: Add the `holdSuggestions` collection and `LoggingType` import**

Add to the import list (alphabetically among the existing `com.lsing.timego.*` imports):
```kotlin
import com.lsing.timego.data.LoggingType
import com.lsing.timego.domain.HoldSuggestion
```

Add this line alongside the other `collectAsState()` calls at the top of `LogScreen`:
```kotlin
    val holdSuggestions by viewModel.holdSuggestions.collectAsState()
```

- [ ] **Step 2: Replace the two-way branch with a three-way branch**

Change:
```kotlin
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
```
to:
```kotlin
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
```

- [ ] **Step 3: Add the `HoldLogRow` composable**

Add this after `CardioLogRow` (the last private composable in the file):
```kotlin

@Composable
private fun HoldLogRow(
    exerciseName: String,
    category: String,
    suggestion: HoldSuggestion?,
    onLog: (durationSeconds: Int, targetDurationSeconds: Int) -> Unit,
) {
    var expanded by remember(exerciseName) { mutableStateOf(false) }
    var secondsText by remember(exerciseName) { mutableStateOf("") }
    val visual = categoryVisual(ExerciseCategory.valueOf(category))

    ExerciseCard(visual.accent) {
        ExerciseRowHeader(exerciseName, visual.icon, visual.accent, expanded) { expanded = !expanded }
        if (expanded) {
            if (suggestion != null) {
                Text(
                    "Suggested: hold ${suggestion.targetDurationSeconds}s -- ${suggestion.note}",
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
                    value = secondsText,
                    onValueChange = { secondsText = it },
                    label = { Text("seconds held") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).padding(end = Spacing.Small),
                )
                Button(onClick = {
                    val seconds = secondsText.toIntOrNull()
                    if (seconds != null) {
                        onLog(seconds, suggestion?.targetDurationSeconds ?: seconds)
                        secondsText = ""
                    }
                }) {
                    Text("Log hold")
                }
            }
        }
    }
}
```

- [ ] **Step 4: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt
git commit -m "Add HoldLogRow and three-way loggingType branch to LogScreen"
```

---

### Task 8: HOLD support in ProgressViewModel and ProgressScreen

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressViewModel.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt`

**Interfaces:**
- Consumes: `LoggingType`, `PrType.LONGEST_HOLD` (Tasks 1, 4).

- [ ] **Step 1: Update `ProgressViewModel.kt`'s history-dialog description**

Change the import:
```kotlin
import com.lsing.timego.data.ExerciseCategory
```
to
```kotlin
import com.lsing.timego.data.LoggingType
```

Change the description branch inside `selectHistoryDate`:
```kotlin
                    val description = if (exercise.category == ExerciseCategory.CARDIO.name || exercise.category == ExerciseCategory.WARMUP.name) {
                        val distance = log.distanceKm?.let { " -- ${it}km" } ?: ""
                        "${log.durationMinutes ?: 0.0} min$distance"
                    } else {
                        "${log.weightKg}kg x ${log.reps}"
                    }
```
to
```kotlin
                    val description = when (exercise.loggingType) {
                        LoggingType.DURATION_DISTANCE.name -> {
                            val distance = log.distanceKm?.let { " -- ${it}km" } ?: ""
                            "${log.durationMinutes ?: 0.0} min$distance"
                        }
                        LoggingType.HOLD.name -> "${log.holdSeconds ?: 0}s hold"
                        else -> "${log.weightKg}kg x ${log.reps}"
                    }
```

- [ ] **Step 2: Update `ProgressScreen.kt`'s PR display and `formatRecordValue`**

Add an import:
```kotlin
import com.lsing.timego.data.LoggingType
```
(add alongside the existing `import com.lsing.timego.data.MuscleGroup` line)

Change the PR card's stat-tile loop (currently iterates every `PrType`, which after Task 4 would show an always-"--" Longest Hold tile for every lift and always-"--" weight/reps/volume tiles for every hold exercise):
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
(replacing the existing `PrType.entries.forEach { type -> ... }` block)

Change `formatRecordValue`:
```kotlin
private fun formatRecordValue(record: PersonalRecord): String = when (record.type) {
    PrType.HEAVIEST_WEIGHT -> "${record.value}kg"
    PrType.MOST_REPS -> "${record.value.toInt()} reps"
    PrType.BEST_VOLUME -> "${record.value}kg total"
    PrType.LONGEST_HOLD -> "${record.value.toInt()}s"
}
```

- [ ] **Step 3: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/progress/ProgressViewModel.kt app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt
git commit -m "Show HOLD sets and LONGEST_HOLD records correctly in Progress"
```

---

### Task 9: Full verification and on-device check

**Files:** none (verification only)

- [ ] **Step 1: Full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests passing (existing suite plus every test added in Tasks 2-5).

- [ ] **Step 2: Full debug build and install**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL, "Installed on 1 device."

- [ ] **Step 3: On-device verification (hand off to the user)**

Ask the user to: open Log, expand Plank/Dead Hang/Wall Sit and confirm they show a single "seconds held" field (not weight/reps); log a hold set and confirm a suggestion appears on the next view; open Progress, select one of the hold exercises in the Personal Records wheel picker and confirm it shows a single "Longest Hold" tile (not three empty weight/reps/volume tiles); confirm a normal lift (e.g. Squat, if any test data exists) still shows the original three PR tiles, not a fourth empty "Longest Hold" tile; tap a heatmap day that included a hold-exercise set and confirm the day-history dialog shows "Ns hold" for it.

- [ ] **Step 4: Update the vault project note**

Add a session entry to `C:\Users\lsing\.claude\obsidian_demo\Projects\TimeGo - Gym Progress Tracker.md` recording that logging-field accuracy shipped: `LoggingType` (WEIGHT_REPS/HOLD/DURATION_DISTANCE) added per exercise, six exercises (Plank, Side Plank, Wall Sit, L-Sit, Dead Hang, Superman) reclassified to hold-based logging, `RuleBasedHoldSuggester` and `LONGEST_HOLD` PRs added, muscle-volume distribution updated to count hold seconds as a proxy. Note that Project B (library expansion + heatmap threshold) is next.

- [ ] **Step 5: Verify git state**

```bash
cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"
git status
git log --oneline -9
```

Expected: eight commits from Tasks 1-8 visible, working tree clean (the vault note lives outside this repo, so it doesn't appear in `git status` here).

---

## Self-Review Notes

- **Spec coverage**: Section 1 (data model) -> Task 1. Section 2 (seed curation) -> Task 2. Section 3 (UI) -> Task 7. Section 4 (domain: PR/curve filtering, LONGEST_HOLD, HoldSuggester, muscle-volume proxy) -> Tasks 3, 4, 5. The custom-exercise `addCustomExercise` loggingType-derivation fix (identified during planning, not explicitly in the spec but required to avoid the same bug for user-added exercises) -> Task 1. Verification -> Task 9.
- **Type consistency checked**: `HoldPerformance`/`HoldSuggestion` (Task 3) field names (`durationSeconds`, `targetDurationSeconds`) match exactly between `HoldSuggester.kt`, `LogViewModel.kt`'s `refreshSuggestions`, and `LogScreen.kt`'s `HoldLogRow` `onLog` callback signature (`durationSeconds: Int, targetDurationSeconds: Int`) and `LogViewModel.logHoldSet`'s parameters. `LoggingType.HOLD.name`/`LoggingType.WEIGHT_REPS.name`/`LoggingType.DURATION_DISTANCE.name` string comparisons are used consistently across `ProgressMath.kt`, `MuscleDistribution.kt`, `LogViewModel.kt`, `LogScreen.kt`, and `ProgressViewModel.kt` rather than mixing enum and string comparisons.
- **Existing-test-fixture risk caught during planning**: switching `personalRecords`/`muscleGroupStrengthCurve`/`muscleGroupVolumeDistribution` from category-based to loggingType-based filtering silently breaks the existing CARDIO-exclusion tests, since their `Exercise` fixtures never set `loggingType` explicitly and would fall back to the `WEIGHT_REPS` default -- Tasks 4 and 5 each start with a step fixing this before adding new tests, so the change is caught by the test suite rather than shipping a silent regression.
