# TimeGo Update 1.1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix v1's design/interaction gaps found on-device: no visual design pass, exercise library too small and ungrouped, no routine scheduling, cardio/warmup can't be logged meaningfully, strength curve is a text list, and personal records/muscle-nudge dump raw enum names.

**Architecture:** Additive Room migration (v1→v2) adding `Exercise.category`, `SetLog.durationMinutes`/`distanceKm`, `Routine.daysOfWeek`. New domain functions for calorie/pace estimates, today's-routine lookup, and muscle-group-aggregate strength curves — all plain Kotlin, TDD per project convention. UI gets a real redesign pass: collapsible category-grouped exercise lists, a modal add-exercise dialog, a real Canvas line chart, and formatted (not raw-enum) text everywhere.

**Tech Stack:** Same as v1 (Kotlin, Compose, Room 2.8.4, Navigation-Compose, JUnit4). No new dependencies.

## Global Constraints

- The device already has real logged data (confirmed via screenshot: a squat PR, sessions logged) — the migration MUST be additive (`Migration(1, 2)` with `ALTER TABLE ... ADD COLUMN`), never `fallbackToDestructiveMigration()`.
- Domain logic (calorie/pace/schedule/muscle-group-curve functions) gets TDD with JUnit, matching v1's convention. Room/UI changes are verified by building, installing, and the agent taking + reviewing its own screenshots via `adb shell screencap` before asking the user to check anything (the process gap that caused v1's underdelivery).
- MET-based calorie estimates are explicitly rough (single constant per category, no per-exercise granularity) — say so in a code comment, don't oversell precision in the UI copy.
- Execute in the order the user asked for: **backend (Tasks 1-4) → frontend (Tasks 5-9) → verification (Task 10)**, inline in this session.
- `adb` path on this machine: `C:\Users\lsing\AppData\Local\Android\Sdk\platform-tools\adb.exe`. Use `//sdcard/...` (double leading slash) for on-device paths in Git Bash to avoid MSYS path-mangling.

---

## Task 1: Schema changes — `ExerciseCategory`, migration 1→2, entity updates

**Files:**
- Create: `app/src/main/java/com/lsing/timego/data/ExerciseCategory.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/Exercise.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/SetLog.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/Routine.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/TimeGoDatabase.kt`

**Interfaces:**
- Produces: `enum class ExerciseCategory { STRENGTH, CALISTHENICS, CARDIO, WARMUP }`; `Exercise.category: String`; `SetLog.durationMinutes: Double?`, `SetLog.distanceKm: Double?`; `Routine.daysOfWeek: List<String>`; `val MIGRATION_1_2: Migration`.

- [ ] **Step 1: Write `ExerciseCategory.kt`**

```kotlin
package com.lsing.timego.data

enum class ExerciseCategory { STRENGTH, CALISTHENICS, CARDIO, WARMUP }
```

- [ ] **Step 2: Add `category` to `Exercise.kt`**

```kotlin
package com.lsing.timego.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [muscleGroups] stores MuscleGroup enum names as strings (via Converters' fromStringList/
 *  toStringList), not the enum type directly, so Room's converter resolution stays unambiguous.
 *  [category] stores an ExerciseCategory enum name the same way. */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroups: List<String>,
    val isCustom: Boolean,
    val category: String,
)
```

- [ ] **Step 3: Add `durationMinutes`/`distanceKm` to `SetLog.kt`**

```kotlin
package com.lsing.timego.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [targetReps] is the rep count the user was aiming for on this set -- the overload suggester
 *  compares [reps] against it to detect a missed target. [durationMinutes]/[distanceKm] are used
 *  instead of [weightKg]/[reps] for CARDIO/WARMUP exercises (both null for strength/calisthenics
 *  sets, [weightKg]/[reps] are 0.0/0 sentinels for cardio/warmup sets -- callers branch on the
 *  logged exercise's category, never on null-checking these fields, to know which pair applies). */
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
)
```

- [ ] **Step 4: Add `daysOfWeek` to `Routine.kt`**

```kotlin
package com.lsing.timego.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** [daysOfWeek] stores java.time.DayOfWeek enum names (e.g. "WEDNESDAY"), via the same
 *  Converters.fromStringList/toStringList pair Exercise.muscleGroups already uses -- same List<String>
 *  type, same converter, no new Room TypeConverter needed. */
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val daysOfWeek: List<String> = emptyList(),
)
```

- [ ] **Step 5: Write the migration and bump `TimeGoDatabase.kt` to version 2**

```kotlin
package com.lsing.timego.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN category TEXT NOT NULL DEFAULT 'STRENGTH'")
        db.execSQL("ALTER TABLE set_logs ADD COLUMN durationMinutes REAL")
        db.execSQL("ALTER TABLE set_logs ADD COLUMN distanceKm REAL")
        db.execSQL("ALTER TABLE routines ADD COLUMN daysOfWeek TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [Exercise::class, WorkoutSession::class, SetLog::class, Routine::class, RoutineExercise::class, BodyMetric::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TimeGoDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionDao(): SessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun routineDao(): RoutineDao
    abstract fun bodyMetricDao(): BodyMetricDao

    companion object {
        @Volatile private var instance: TimeGoDatabase? = null

        fun getInstance(context: Context): TimeGoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, TimeGoDatabase::class.java, "timego.db")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
```

- [ ] **Step 6: Build and install, confirm the migration runs without crashing on the device's existing data**

Run: `./gradlew installDebug`
Expected: app installs and launches without a crash. A migration exception (e.g. a typo'd column name) crashes immediately on launch with a Room `IllegalStateException` — that's the signal to check for. Take a screenshot (`adb shell screencap`) of the Log tab afterward and confirm your previously-logged exercises/sets are still visible (proves the migration was additive, not destructive).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/ExerciseCategory.kt app/src/main/java/com/lsing/timego/data/Exercise.kt app/src/main/java/com/lsing/timego/data/SetLog.kt app/src/main/java/com/lsing/timego/data/Routine.kt app/src/main/java/com/lsing/timego/data/TimeGoDatabase.kt
git commit -m "Add ExerciseCategory, cardio fields, routine scheduling (Room migration 1->2)"
```

---

## Task 2: Domain logic — calorie/pace estimates, today's-routine lookup, muscle-group curve (TDD)

**Files:**
- Create: `app/src/main/java/com/lsing/timego/domain/CardioMath.kt`
- Create: `app/src/test/java/com/lsing/timego/domain/CardioMathTest.kt`
- Create: `app/src/main/java/com/lsing/timego/domain/RoutineSchedule.kt`
- Create: `app/src/test/java/com/lsing/timego/domain/RoutineScheduleTest.kt`
- Modify: `app/src/main/java/com/lsing/timego/domain/ProgressMath.kt`
- Modify: `app/src/test/java/com/lsing/timego/domain/ProgressMathTest.kt`

**Interfaces:**
- Consumes: `Exercise`, `SetLog`, `Routine` (Task 1's updated entities).
- Produces: `const val MET_CARDIO`, `const val MET_WARMUP`; `fun estimatedCalorieBurn(met: Double, bodyWeightKg: Double, durationMinutes: Double): Double`; `fun averagePaceMinPerKm(durationMinutes: Double, distanceKm: Double): Double?`; `fun routinesForToday(routines: List<Routine>, today: DayOfWeek): List<Routine>`; `fun muscleGroupStrengthCurve(history: List<SetLog>, exercisesById: Map<Long, Exercise>, sessionDateById: Map<Long, LocalDate>, muscleGroup: String): List<Pair<LocalDate, Double>>`.

- [ ] **Step 1: Write the failing `CardioMathTest.kt`**

```kotlin
package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardioMathTest {
    @Test
    fun `estimatedCalorieBurn applies MET times weight times hours`() {
        val result = estimatedCalorieBurn(met = 8.0, bodyWeightKg = 75.0, durationMinutes = 30.0)
        assertEquals(300.0, result, 0.001)
    }

    @Test
    fun `averagePaceMinPerKm divides duration by distance`() {
        val result = averagePaceMinPerKm(durationMinutes = 30.0, distanceKm = 5.0)
        assertEquals(6.0, result!!, 0.001)
    }

    @Test
    fun `averagePaceMinPerKm returns null with zero distance`() {
        assertNull(averagePaceMinPerKm(durationMinutes = 30.0, distanceKm = 0.0))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.CardioMathTest"`
Expected: FAIL — functions don't exist yet.

- [ ] **Step 3: Write `CardioMath.kt`**

```kotlin
package com.lsing.timego.domain

/** Rough MET (metabolic equivalent) constants per exercise category, used only for a ballpark
 *  calorie estimate -- not medical-grade, no per-exercise granularity (e.g. running pace bands
 *  aren't distinguished). */
const val MET_CARDIO = 8.0
const val MET_WARMUP = 3.0

/** Standard calorie-burn approximation: MET * body weight (kg) * duration (hours). */
fun estimatedCalorieBurn(met: Double, bodyWeightKg: Double, durationMinutes: Double): Double =
    met * bodyWeightKg * (durationMinutes / 60.0)

/** Null when there's no distance to divide by -- can't compute a pace. */
fun averagePaceMinPerKm(durationMinutes: Double, distanceKm: Double): Double? =
    if (distanceKm > 0.0) durationMinutes / distanceKm else null
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.CardioMathTest"`
Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 5: Write the failing `RoutineScheduleTest.kt`**

```kotlin
package com.lsing.timego.domain

import com.lsing.timego.data.Routine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class RoutineScheduleTest {
    @Test
    fun `routinesForToday returns only routines scheduled for the given day`() {
        val pushDay = Routine(id = 1, name = "Push Day", daysOfWeek = listOf("WEDNESDAY", "SATURDAY"))
        val legDay = Routine(id = 2, name = "Leg Day", daysOfWeek = listOf("MONDAY"))

        val result = routinesForToday(listOf(pushDay, legDay), DayOfWeek.WEDNESDAY)

        assertEquals(listOf(pushDay), result)
    }

    @Test
    fun `routinesForToday returns empty list when nothing is scheduled`() {
        val legDay = Routine(id = 2, name = "Leg Day", daysOfWeek = listOf("MONDAY"))
        assertEquals(emptyList<Routine>(), routinesForToday(listOf(legDay), DayOfWeek.FRIDAY))
    }
}
```

- [ ] **Step 6: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.RoutineScheduleTest"`
Expected: FAIL — `routinesForToday` doesn't exist yet.

- [ ] **Step 7: Write `RoutineSchedule.kt`**

```kotlin
package com.lsing.timego.domain

import com.lsing.timego.data.Routine
import java.time.DayOfWeek

fun routinesForToday(routines: List<Routine>, today: DayOfWeek): List<Routine> =
    routines.filter { today.name in it.daysOfWeek }
```

- [ ] **Step 8: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.RoutineScheduleTest"`
Expected: `BUILD SUCCESSFUL`, 2 tests passed.

- [ ] **Step 9: Add the failing muscle-group-curve test to `ProgressMathTest.kt`**

Add this test to the existing `ProgressMathTest` class (don't create a new file):

```kotlin
    @Test
    fun `muscleGroupStrengthCurve takes the best 1RM per date among tagged exercises`() {
        val squat = com.lsing.timego.data.Exercise(id = 1, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false, category = "STRENGTH")
        val legPress = com.lsing.timego.data.Exercise(id = 2, name = "Leg Press", muscleGroups = listOf("QUADS"), isCustom = false, category = "STRENGTH")
        val logs = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 1, exerciseId = 2, weightKg = 150.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
        )
        val exercisesById = mapOf(1L to squat, 2L to legPress)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 1))

        val curve = muscleGroupStrengthCurve(logs, exercisesById, sessionDateById, "QUADS")

        assertEquals(1, curve.size)
        assertEquals(estimatedOneRepMax(150.0, 5), curve[0].second, 0.001)
    }
```

- [ ] **Step 10: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.ProgressMathTest"`
Expected: FAIL — `muscleGroupStrengthCurve` doesn't exist yet (the other 4 existing tests still pass).

- [ ] **Step 11: Add `muscleGroupStrengthCurve` to `ProgressMath.kt`**

Add this import and function to the existing file (don't remove anything already there):

```kotlin
import com.lsing.timego.data.Exercise
```

```kotlin
/** Best estimated-1RM per date among sets logged for any exercise tagged with [muscleGroup] --
 *  an aggregate view across e.g. every QUADS exercise, not just one. */
fun muscleGroupStrengthCurve(
    history: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sessionDateById: Map<Long, LocalDate>,
    muscleGroup: String,
): List<Pair<LocalDate, Double>> {
    val bestByDate = mutableMapOf<LocalDate, Double>()
    for (log in history) {
        val exercise = exercisesById[log.exerciseId] ?: continue
        if (muscleGroup !in exercise.muscleGroups) continue
        val date = sessionDateById[log.sessionId] ?: continue
        val oneRepMax = estimatedOneRepMax(log.weightKg, log.reps)
        val current = bestByDate[date]
        if (current == null || oneRepMax > current) bestByDate[date] = oneRepMax
    }
    return bestByDate.entries.sortedBy { it.key }.map { it.key to it.value }
}
```

- [ ] **Step 12: Run to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.ProgressMathTest"`
Expected: `BUILD SUCCESSFUL`, 5 tests passed (4 existing + 1 new).

- [ ] **Step 13: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/CardioMath.kt app/src/test/java/com/lsing/timego/domain/CardioMathTest.kt app/src/main/java/com/lsing/timego/domain/RoutineSchedule.kt app/src/test/java/com/lsing/timego/domain/RoutineScheduleTest.kt app/src/main/java/com/lsing/timego/domain/ProgressMath.kt app/src/test/java/com/lsing/timego/domain/ProgressMathTest.kt
git commit -m "Add calorie/pace estimates, today's-routine lookup, muscle-group strength curve"
```

---

## Task 3: Repository updates for category, cardio logging, and routine scheduling

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/WorkoutRepository.kt`

**Interfaces:**
- Consumes: `Exercise`, `SetLog`, `Routine` (Task 1).
- Produces: updated `addCustomExercise(name: String, muscleGroups: List<String>, category: String): Long`; new `logCardioSet(sessionId: Long, exerciseId: Long, durationMinutes: Double, distanceKm: Double?)`; updated `createRoutine(name: String, exerciseIds: List<Long>, daysOfWeek: List<String>): Long`; new `latestBodyWeightKg(): Double?`.

- [ ] **Step 1: Update `addCustomExercise` and `createRoutine`, add `logCardioSet` and `latestBodyWeightKg`**

In `WorkoutRepository.kt`, replace the existing `addCustomExercise` with:

```kotlin
    suspend fun addCustomExercise(name: String, muscleGroups: List<String>, category: String): Long =
        db.exerciseDao().insert(Exercise(name = name, muscleGroups = muscleGroups, isCustom = true, category = category))
```

Replace the existing `createRoutine` with:

```kotlin
    suspend fun createRoutine(name: String, exerciseIds: List<Long>, daysOfWeek: List<String>): Long {
        val routineId = db.routineDao().insertRoutine(Routine(name = name, daysOfWeek = daysOfWeek))
        exerciseIds.forEachIndexed { index, exerciseId ->
            db.routineDao().insertRoutineExercise(RoutineExercise(routineId = routineId, exerciseId = exerciseId, orderIndex = index))
        }
        return routineId
    }
```

Add these two new methods anywhere inside the class:

```kotlin
    suspend fun logCardioSet(sessionId: Long, exerciseId: Long, durationMinutes: Double, distanceKm: Double?) {
        db.setLogDao().insert(
            SetLog(
                sessionId = sessionId,
                exerciseId = exerciseId,
                weightKg = 0.0,
                reps = 0,
                targetReps = 0,
                loggedAtEpochMillis = System.currentTimeMillis(),
                durationMinutes = durationMinutes,
                distanceKm = distanceKm,
            ),
        )
    }

    suspend fun latestBodyWeightKg(): Double? = bodyMetrics.first().lastOrNull { it.weightKg != null }?.weightKg
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: FAIL at this point — Task 10 (nav wiring aside) doesn't apply here, but the existing call sites in `LogViewModel.kt` and `RoutinesViewModel.kt` still call the OLD 2-arg `addCustomExercise`/`createRoutine` signatures. That's expected and gets fixed in Tasks 7 and 9 (the frontend tasks) when those ViewModels are updated. Confirm the *repository file itself* has no syntax errors by checking the compiler output only mentions the two known call-site mismatches, not anything inside `WorkoutRepository.kt`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/WorkoutRepository.kt
git commit -m "Update WorkoutRepository for exercise categories, cardio logging, routine scheduling"
```

(The two known-broken call sites are fixed in Tasks 7 and 9 — the project will not build cleanly again until Task 9 completes. This is expected and matches "backend first" ordering; do not skip ahead to fix them out of task order.)

---

## Task 4: Expand the exercise seed library (100+, categorized)

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SeedExercises.kt`

**Interfaces:**
- Produces: updated `val SEED_EXERCISES: List<Exercise>` — every entry now includes `category`.

- [ ] **Step 1: Replace `SeedExercises.kt` with the expanded, categorized list**

```kotlin
package com.lsing.timego.data

private fun strength(name: String, vararg groups: MuscleGroup) =
    Exercise(name = name, muscleGroups = groups.map { it.name }, isCustom = false, category = ExerciseCategory.STRENGTH.name)

private fun calisthenics(name: String, vararg groups: MuscleGroup) =
    Exercise(name = name, muscleGroups = groups.map { it.name }, isCustom = false, category = ExerciseCategory.CALISTHENICS.name)

private fun warmup(name: String, vararg groups: MuscleGroup) =
    Exercise(name = name, muscleGroups = groups.map { it.name }, isCustom = false, category = ExerciseCategory.WARMUP.name)

private fun cardio(name: String, vararg groups: MuscleGroup) =
    Exercise(name = name, muscleGroups = groups.map { it.name }, isCustom = false, category = ExerciseCategory.CARDIO.name)

val SEED_EXERCISES = listOf(
    // Strength -- Chest
    strength("Barbell Bench Press", MuscleGroup.CHEST, MuscleGroup.TRICEPS),
    strength("Incline Barbell Bench Press", MuscleGroup.CHEST, MuscleGroup.SHOULDERS),
    strength("Decline Barbell Bench Press", MuscleGroup.CHEST, MuscleGroup.TRICEPS),
    strength("Dumbbell Bench Press", MuscleGroup.CHEST, MuscleGroup.TRICEPS),
    strength("Incline Dumbbell Press", MuscleGroup.CHEST, MuscleGroup.SHOULDERS),
    strength("Dumbbell Fly", MuscleGroup.CHEST),
    strength("Cable Crossover", MuscleGroup.CHEST),
    strength("Machine Chest Press", MuscleGroup.CHEST, MuscleGroup.TRICEPS),
    strength("Pec Deck Machine", MuscleGroup.CHEST),
    // Strength -- Back
    strength("Conventional Deadlift", MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
    strength("Sumo Deadlift", MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
    strength("Barbell Row", MuscleGroup.BACK, MuscleGroup.BICEPS),
    strength("Pendlay Row", MuscleGroup.BACK, MuscleGroup.BICEPS),
    strength("T-Bar Row", MuscleGroup.BACK, MuscleGroup.BICEPS),
    strength("Seated Cable Row", MuscleGroup.BACK, MuscleGroup.BICEPS),
    strength("Lat Pulldown", MuscleGroup.BACK, MuscleGroup.BICEPS),
    strength("Single-Arm Dumbbell Row", MuscleGroup.BACK, MuscleGroup.BICEPS),
    strength("Machine Row", MuscleGroup.BACK, MuscleGroup.BICEPS),
    // Strength -- Shoulders
    strength("Overhead Press", MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
    strength("Seated Dumbbell Shoulder Press", MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
    strength("Arnold Press", MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
    strength("Lateral Raise", MuscleGroup.SHOULDERS),
    strength("Front Raise", MuscleGroup.SHOULDERS),
    strength("Rear Delt Fly", MuscleGroup.SHOULDERS),
    strength("Face Pull", MuscleGroup.SHOULDERS, MuscleGroup.BACK),
    strength("Machine Shoulder Press", MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
    // Strength -- Arms
    strength("Dumbbell Bicep Curl", MuscleGroup.BICEPS),
    strength("Barbell Curl", MuscleGroup.BICEPS),
    strength("Hammer Curl", MuscleGroup.BICEPS),
    strength("Preacher Curl", MuscleGroup.BICEPS),
    strength("Concentration Curl", MuscleGroup.BICEPS),
    strength("Cable Curl", MuscleGroup.BICEPS),
    strength("Tricep Pushdown", MuscleGroup.TRICEPS),
    strength("Overhead Tricep Extension", MuscleGroup.TRICEPS),
    strength("Skull Crusher", MuscleGroup.TRICEPS),
    strength("Close-Grip Bench Press", MuscleGroup.TRICEPS, MuscleGroup.CHEST),
    strength("Cable Kickback", MuscleGroup.TRICEPS),
    // Strength -- Legs
    strength("Barbell Back Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    strength("Barbell Front Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    strength("Leg Press", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    strength("Romanian Deadlift", MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
    strength("Leg Curl", MuscleGroup.HAMSTRINGS),
    strength("Leg Extension", MuscleGroup.QUADS),
    strength("Walking Lunge", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    strength("Bulgarian Split Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    strength("Hip Thrust", MuscleGroup.GLUTES),
    strength("Cable Pull-Through", MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
    strength("Standing Calf Raise", MuscleGroup.CALVES),
    strength("Seated Calf Raise", MuscleGroup.CALVES),
    // Strength -- Core
    strength("Cable Woodchopper", MuscleGroup.CORE),
    strength("Weighted Russian Twist", MuscleGroup.CORE),
    strength("Machine Ab Crunch", MuscleGroup.CORE),

    // Calisthenics
    calisthenics("Push-Up", MuscleGroup.CHEST, MuscleGroup.TRICEPS),
    calisthenics("Diamond Push-Up", MuscleGroup.TRICEPS, MuscleGroup.CHEST),
    calisthenics("Wide Push-Up", MuscleGroup.CHEST),
    calisthenics("Pull-Up", MuscleGroup.BACK, MuscleGroup.BICEPS),
    calisthenics("Chin-Up", MuscleGroup.BACK, MuscleGroup.BICEPS),
    calisthenics("Dip", MuscleGroup.TRICEPS, MuscleGroup.CHEST),
    calisthenics("Bodyweight Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    calisthenics("Jump Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    calisthenics("Lunge", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    calisthenics("Glute Bridge", MuscleGroup.GLUTES),
    calisthenics("Burpee", MuscleGroup.FULL_BODY),
    calisthenics("Mountain Climber", MuscleGroup.CORE, MuscleGroup.FULL_BODY),
    calisthenics("Plank", MuscleGroup.CORE),
    calisthenics("Side Plank", MuscleGroup.CORE),
    calisthenics("Sit-Up", MuscleGroup.CORE),
    calisthenics("Hanging Leg Raise", MuscleGroup.CORE),
    calisthenics("Bicycle Crunch", MuscleGroup.CORE),
    calisthenics("Superman", MuscleGroup.BACK, MuscleGroup.CORE),
    calisthenics("Pike Push-Up", MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
    calisthenics("Inverted Row", MuscleGroup.BACK, MuscleGroup.BICEPS),
    calisthenics("Step-Up", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    calisthenics("Wall Sit", MuscleGroup.QUADS),

    // Warmup
    warmup("Arm Circles", MuscleGroup.SHOULDERS),
    warmup("Leg Swings", MuscleGroup.HAMSTRINGS, MuscleGroup.QUADS),
    warmup("Jumping Jacks", MuscleGroup.FULL_BODY),
    warmup("Band Pull-Apart", MuscleGroup.SHOULDERS, MuscleGroup.BACK),
    warmup("Bodyweight Hip Circles", MuscleGroup.GLUTES),
    warmup("Dynamic Chest Stretch", MuscleGroup.CHEST),
    warmup("Cat-Cow Stretch", MuscleGroup.BACK, MuscleGroup.CORE),
    warmup("World's Greatest Stretch", MuscleGroup.FULL_BODY),
    warmup("High Knees", MuscleGroup.QUADS, MuscleGroup.FULL_BODY),
    warmup("Butt Kicks", MuscleGroup.HAMSTRINGS, MuscleGroup.FULL_BODY),
    warmup("Ankle Bounces", MuscleGroup.CALVES),
    warmup("Torso Twists", MuscleGroup.CORE),

    // Cardio
    cardio("Running", MuscleGroup.FULL_BODY),
    cardio("Treadmill Running", MuscleGroup.FULL_BODY),
    cardio("Incline Walking", MuscleGroup.FULL_BODY),
    cardio("Cycling", MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS),
    cardio("Stationary Bike", MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS),
    cardio("Rowing Machine", MuscleGroup.BACK, MuscleGroup.FULL_BODY),
    cardio("Jump Rope", MuscleGroup.CALVES, MuscleGroup.FULL_BODY),
    cardio("Stair Climbing", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    cardio("Swimming", MuscleGroup.FULL_BODY),
    cardio("Elliptical", MuscleGroup.FULL_BODY),
)
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: still fails on the two known call sites (`LogViewModel.kt`, `RoutinesViewModel.kt`) from Task 3, same as before — confirm the failures are still limited to those two files, not `SeedExercises.kt`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SeedExercises.kt
git commit -m "Expand seed exercise library to 100+, categorized (strength/calisthenics/warmup/cardio)"
```

**Backend is now done.** The two broken call sites are expected and get fixed as part of the frontend tasks below (Tasks 7 and 9), since those tasks own `LogViewModel.kt` and `RoutinesViewModel.kt`.

---

## Task 5: Shared collapsible category-grouped exercise list component

**Files:**
- Create: `app/src/main/java/com/lsing/timego/ui/common/ExerciseListSections.kt`

**Interfaces:**
- Consumes: `Exercise`, `ExerciseCategory` (Task 1).
- Produces: `@Composable fun ExerciseSections(exercises: List<Exercise>, itemContent: @Composable (Exercise) -> Unit)` — groups [exercises] by `ExerciseCategory`, each category collapsible (defaults to expanded), sub-headed by muscle group within. Callers (Log, Routines) supply `itemContent` for what each exercise row looks like, so this component owns only the *grouping/collapse* structure, not the per-exercise UI (which differs between screens: Log needs weight/reps or duration/distance inputs, Routines needs a checkbox).

- [ ] **Step 1: Write `ExerciseListSections.kt`**

```kotlin
package com.lsing.timego.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory

/** Title-Case display label for an ExerciseCategory or MuscleGroup enum name, e.g. "FULL_BODY" ->
 *  "Full Body". Shared here since both category and muscle-group headers need it. */
fun formatEnumLabel(rawName: String): String =
    rawName.lowercase().split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

/** Renders [exercises] grouped by category (collapsible, defaults to expanded) and sub-headed by
 *  muscle group within each category. [itemContent] renders one exercise's row -- this component
 *  owns only the grouping/collapse chrome so Log and Routines can each supply their own row UI
 *  (weight/reps inputs vs a selection checkbox) without duplicating the grouping logic. */
@Composable
fun ExerciseSections(exercises: List<Exercise>, itemContent: @Composable (Exercise) -> Unit) {
    val byCategory = exercises.groupBy { it.category }
    ExerciseCategory.entries.forEach { category ->
        val inCategory = byCategory[category.name].orEmpty()
        if (inCategory.isEmpty()) return@forEach
        var expanded by remember(category) { mutableStateOf(true) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                )
            }
            Text(formatEnumLabel(category.name), style = MaterialTheme.typography.titleMedium)
        }
        if (expanded) {
            val byMuscleGroup = inCategory.groupBy { it.muscleGroups.firstOrNull() ?: "OTHER" }
            byMuscleGroup.forEach { (group, groupExercises) ->
                Text(
                    formatEnumLabel(group),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp, top = 8.dp),
                )
                groupExercises.forEach { exercise -> itemContent(exercise) }
            }
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: still only the two known Task 3 call-site failures.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/common/ExerciseListSections.kt
git commit -m "Add shared collapsible category/muscle-group exercise list component"
```

---

## Task 6: Add Custom Exercise modal dialog

**Files:**
- Create: `app/src/main/java/com/lsing/timego/ui/log/AddExerciseDialog.kt`

**Interfaces:**
- Consumes: `MuscleGroup`, `ExerciseCategory` (Task 1), `formatEnumLabel` (Task 5).
- Produces: `@Composable fun AddExerciseDialog(onDismiss: () -> Unit, onAdd: (name: String, muscleGroups: List<String>, category: String) -> Unit)` — a Material3 `AlertDialog` with a name field, category selector (radio-style `FilterChip` row), and muscle-group checkboxes.

- [ ] **Step 1: Write `AddExerciseDialog.kt`**

```kotlin
package com.lsing.timego.ui.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.ui.common.formatEnumLabel

@Composable
fun AddExerciseDialog(onDismiss: () -> Unit, onAdd: (name: String, muscleGroups: List<String>, category: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExerciseCategory.STRENGTH) }
    val selectedGroups = remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Exercise") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Category", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ExerciseCategory.entries.forEach { entry ->
                        FilterChip(
                            selected = category == entry,
                            onClick = { category = entry },
                            label = { Text(formatEnumLabel(entry.name)) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
                Text("Muscle groups", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                MuscleGroup.entries.forEach { group ->
                    val checked = group.name in selectedGroups.value
                    Row {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                selectedGroups.value = if (isChecked) {
                                    selectedGroups.value + group.name
                                } else {
                                    selectedGroups.value - group.name
                                }
                            },
                        )
                        Text(formatEnumLabel(group.name))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && selectedGroups.value.isNotEmpty()) {
                    onAdd(name, selectedGroups.value.toList(), category.name)
                    onDismiss()
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: still only the two known Task 3 call-site failures (unrelated to this new file).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/AddExerciseDialog.kt
git commit -m "Add modal dialog for creating custom exercises"
```

---

## Task 7: Log screen redesign — cards, category sections, today's routine, cardio logging

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt`

**Interfaces:**
- Consumes: `ExerciseSections` (Task 5), `AddExerciseDialog` (Task 6), `routinesForToday` (Task 2), updated `WorkoutRepository.addCustomExercise`/`logCardioSet` (Task 3), `averagePaceMinPerKm`/`estimatedCalorieBurn`/`MET_CARDIO`/`MET_WARMUP` (Task 2).
- Produces: `LogViewModel` gains `fun logCardioSet(exerciseId: Long, durationMinutes: Double, distanceKm: Double?)`, `val latestBodyWeightKg: StateFlow<Double?>`; `addCustomExercise` now takes a `category` param; `selectRoutine`/init logic auto-selects today's scheduled routine on first load if one exists.

- [ ] **Step 1: Rewrite `LogViewModel.kt`**

```kotlin
package com.lsing.timego.ui.log

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.Routine
import com.lsing.timego.data.SEED_EXERCISES
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.WorkoutRepository
import com.lsing.timego.domain.OverloadSuggestion
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

    private var allExercises: List<Exercise> = emptyList()
    private var hasAutoSelectedTodaysRoutine = false

    private val _displayedExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val displayedExercises: StateFlow<List<Exercise>> = _displayedExercises.asStateFlow()

    private val _suggestions = MutableStateFlow<Map<Long, OverloadSuggestion>>(emptyMap())
    val suggestions: StateFlow<Map<Long, OverloadSuggestion>> = _suggestions.asStateFlow()

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines: StateFlow<List<Routine>> = _routines.asStateFlow()

    private val _selectedRoutineId = MutableStateFlow<Long?>(null)
    val selectedRoutineId: StateFlow<Long?> = _selectedRoutineId.asStateFlow()

    private val _latestBodyWeightKg = MutableStateFlow<Double?>(null)
    val latestBodyWeightKg: StateFlow<Double?> = _latestBodyWeightKg.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedExercisesIfEmpty(SEED_EXERCISES)
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

    private suspend fun refreshSuggestions(exerciseList: List<Exercise>) {
        val map = mutableMapOf<Long, OverloadSuggestion>()
        for (exercise in exerciseList) {
            val history = repository.historyForExercise(exercise.id)
                .map { SetPerformance(it.weightKg, it.reps, it.targetReps) }
            suggester.suggestNext(history)?.let { map[exercise.id] = it }
        }
        _suggestions.value = map
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

    fun addCustomExercise(name: String, muscleGroups: List<String>, category: String) {
        viewModelScope.launch {
            repository.addCustomExercise(name, muscleGroups, category)
        }
    }
}
```

- [ ] **Step 2: Rewrite `LogScreen.kt`**

```kotlin
package com.lsing.timego.ui.log

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.domain.MET_CARDIO
import com.lsing.timego.domain.MET_WARMUP
import com.lsing.timego.domain.averagePaceMinPerKm
import com.lsing.timego.domain.estimatedCalorieBurn
import com.lsing.timego.ui.common.ExerciseSections

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

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Session type", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Button(onClick = { showAddDialog = true }) { Text("+ Add exercise") }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                FilterChip(
                    selected = selectedRoutineId == null,
                    onClick = { viewModel.selectRoutine(null) },
                    label = { Text("Freeform") },
                    modifier = Modifier.padding(end = 8.dp),
                )
                routines.forEach { routine ->
                    FilterChip(
                        selected = selectedRoutineId == routine.id,
                        onClick = { viewModel.selectRoutine(routine.id) },
                        label = { Text(routine.name) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }
        item {
            ExerciseSections(exercises = exercises) { exercise ->
                if (exercise.category == ExerciseCategory.CARDIO.name || exercise.category == ExerciseCategory.WARMUP.name) {
                    CardioLogRow(
                        exerciseName = exercise.name,
                        met = if (exercise.category == ExerciseCategory.CARDIO.name) MET_CARDIO else MET_WARMUP,
                        bodyWeightKg = latestBodyWeightKg,
                        onLog = { duration, distance -> viewModel.logCardioSet(exercise.id, duration, distance) },
                    )
                } else {
                    StrengthLogRow(
                        exerciseName = exercise.name,
                        suggestion = suggestions[exercise.id],
                        onLog = { weight, reps, target -> viewModel.logSet(exercise.id, weight, reps, target) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StrengthLogRow(
    exerciseName: String,
    suggestion: com.lsing.timego.domain.OverloadSuggestion?,
    onLog: (weightKg: Double, reps: Int, targetReps: Int) -> Unit,
) {
    var weightText by remember(exerciseName) { mutableStateOf("") }
    var repsText by remember(exerciseName) { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp)) {
        Text(exerciseName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(12.dp, 12.dp, 12.dp, 0.dp))
        if (suggestion != null) {
            Text(
                "Suggested: ${suggestion.weightKg}kg x ${suggestion.reps} -- ${suggestion.note}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            OutlinedTextField(
                value = repsText,
                onValueChange = { repsText = it },
                label = { Text("reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).padding(end = 8.dp),
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

@Composable
private fun CardioLogRow(
    exerciseName: String,
    met: Double,
    bodyWeightKg: Double?,
    onLog: (durationMinutes: Double, distanceKm: Double?) -> Unit,
) {
    var durationText by remember(exerciseName) { mutableStateOf("") }
    var distanceText by remember(exerciseName) { mutableStateOf("") }
    val duration = durationText.toDoubleOrNull()
    val distance = distanceText.toDoubleOrNull()

    Card(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp)) {
        Text(exerciseName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(12.dp, 12.dp, 12.dp, 0.dp))
        if (duration != null && duration > 0) {
            val pace = distance?.let { averagePaceMinPerKm(duration, it) }
            val calories = bodyWeightKg?.let { estimatedCalorieBurn(met, it, duration) }
            val details = listOfNotNull(
                pace?.let { "Pace: ${"%.1f".format(it)} min/km" },
                calories?.let { "~${it.toInt()} kcal" },
            ).joinToString(" -- ")
            if (details.isNotEmpty()) {
                Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = durationText,
                onValueChange = { durationText = it },
                label = { Text("minutes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            OutlinedTextField(
                value = distanceText,
                onValueChange = { distanceText = it },
                label = { Text("km (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).padding(end = 8.dp),
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
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: `LogViewModel.kt`/`LogScreen.kt` errors are now gone; only `RoutinesViewModel.kt`'s `createRoutine` call-site mismatch remains (fixed in Task 9).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt
git commit -m "Redesign Log screen: cards, category sections, today's-routine auto-select, cardio logging"
```

---

## Task 8: Progress screen redesign — formatted PRs, real strength-curve chart, heatmap label fix

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressViewModel.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/common/HeatmapGrid.kt`

**Interfaces:**
- Consumes: `muscleGroupStrengthCurve` (Task 2), `formatEnumLabel` (Task 5), `MuscleGroup` (Task 1).
- Produces: `ProgressViewModel` gains `val curveMode: StateFlow<CurveMode>`, `val selectedMuscleGroup: StateFlow<String?>`, `fun selectCurveMode(mode: CurveMode)`, `fun selectMuscleGroup(group: String)`, where `enum class CurveMode { EXERCISE, MUSCLE_GROUP }`.

- [ ] **Step 1: Fix the month-label clipping in `HeatmapGrid.kt`**

The full-year month labels ("Au" instead of "Aug") are clipped because the `Box(modifier = Modifier.width(dotSize))` constrains each label to one dot's width, which is too narrow at `18` dots per screen. Find this block in `HeatmapGrid.kt`:

```kotlin
                            Box(modifier = Modifier.width(dotSize)) {
                                if (isMonthStart) {
                                    Text(
                                        weekStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
```

Replace it with (allow the label to overflow its own column into the next, since month starts are spaced weeks apart and won't collide, and shrink the font slightly so 3-letter abbreviations reliably fit):

```kotlin
                            Box(modifier = Modifier.width(dotSize)) {
                                if (isMonthStart) {
                                    Text(
                                        weekStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.85f),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
```

- [ ] **Step 2: Rewrite `ProgressViewModel.kt`**

```kotlin
package com.lsing.timego.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsing.timego.data.BodyMetric
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.WorkoutRepository
import com.lsing.timego.domain.PersonalRecord
import com.lsing.timego.domain.muscleGroupStrengthCurve
import com.lsing.timego.domain.personalRecords
import com.lsing.timego.domain.strengthCurve
import com.lsing.timego.domain.workoutVolumeRatios
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class CurveMode { EXERCISE, MUSCLE_GROUP }

class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WorkoutRepository(TimeGoDatabase.getInstance(application))

    private val _volumeRatios = MutableStateFlow<Map<LocalDate, Float>>(emptyMap())
    val volumeRatios: StateFlow<Map<LocalDate, Float>> = _volumeRatios.asStateFlow()

    private val _records = MutableStateFlow<List<PersonalRecord>>(emptyList())
    val records: StateFlow<List<PersonalRecord>> = _records.asStateFlow()

    private val _bodyMetrics = MutableStateFlow<List<BodyMetric>>(emptyList())
    val bodyMetrics: StateFlow<List<BodyMetric>> = _bodyMetrics.asStateFlow()

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _selectedExerciseId = MutableStateFlow<Long?>(null)
    val selectedExerciseId: StateFlow<Long?> = _selectedExerciseId.asStateFlow()

    private val _curveMode = MutableStateFlow(CurveMode.EXERCISE)
    val curveMode: StateFlow<CurveMode> = _curveMode.asStateFlow()

    private val _selectedMuscleGroup = MutableStateFlow<String?>(null)
    val selectedMuscleGroup: StateFlow<String?> = _selectedMuscleGroup.asStateFlow()

    private val _strengthCurve = MutableStateFlow<List<Pair<LocalDate, Double>>>(emptyList())
    val strengthCurve: StateFlow<List<Pair<LocalDate, Double>>> = _strengthCurve.asStateFlow()

    init {
        viewModelScope.launch {
            repository.sessions.collect { sessions ->
                val allSets = repository.allSetLogs()
                _volumeRatios.value = workoutVolumeRatios(sessions, allSets)
                val sessionDateById = sessions.associate { it.id to it.date }
                _records.value = personalRecords(allSets, sessionDateById)
            }
        }
        viewModelScope.launch {
            repository.bodyMetrics.collect { _bodyMetrics.value = it }
        }
        viewModelScope.launch {
            repository.exercises.collect { exerciseList ->
                _exercises.value = exerciseList
                if (_selectedExerciseId.value == null) {
                    exerciseList.firstOrNull()?.let { selectExercise(it.id) }
                }
            }
        }
    }

    fun selectExercise(exerciseId: Long) {
        _curveMode.value = CurveMode.EXERCISE
        _selectedExerciseId.value = exerciseId
        viewModelScope.launch {
            val history = repository.historyForExercise(exerciseId)
            val sessionDateById = repository.allSessions().associate { it.id to it.date }
            _strengthCurve.value = strengthCurve(history, sessionDateById)
        }
    }

    fun selectMuscleGroup(group: String) {
        _curveMode.value = CurveMode.MUSCLE_GROUP
        _selectedMuscleGroup.value = group
        viewModelScope.launch {
            val allSets = repository.allSetLogs()
            val exercisesById = _exercises.value.associateBy { it.id }
            val sessionDateById = repository.allSessions().associate { it.id to it.date }
            _strengthCurve.value = muscleGroupStrengthCurve(allSets, exercisesById, sessionDateById, group)
        }
    }

    fun logBodyMetric(weightKg: Double?, waistCm: Double?) {
        viewModelScope.launch {
            repository.logBodyMetric(LocalDate.now(), weightKg, waistCm)
        }
    }
}
```

- [ ] **Step 3: Rewrite `ProgressScreen.kt`**

```kotlin
package com.lsing.timego.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.ui.common.HeatmapGrid
import com.lsing.timego.ui.common.formatEnumLabel

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = viewModel()) {
    val volumeRatios by viewModel.volumeRatios.collectAsState()
    val records by viewModel.records.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val selectedExerciseId by viewModel.selectedExerciseId.collectAsState()
    val curveMode by viewModel.curveMode.collectAsState()
    val selectedMuscleGroup by viewModel.selectedMuscleGroup.collectAsState()
    val strengthCurve by viewModel.strengthCurve.collectAsState()
    val bodyMetrics by viewModel.bodyMetrics.collectAsState()

    var weightText by remember { mutableStateOf("") }
    var waistText by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text("Consistency", style = MaterialTheme.typography.titleMedium)
            HeatmapGrid(
                ratios = volumeRatios,
                lightColor = Color(0xFF7FD8A0),
                darkColor = Color(0xFF1B5E3A),
            )
        }
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
        items(records) { record ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    "${formatEnumLabel(record.type.name)}: ${record.value} on ${record.achievedOn}",
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        item {
            Text("Strength Curve", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                FilterChip(
                    selected = curveMode == CurveMode.EXERCISE,
                    onClick = { selectedExerciseId?.let { viewModel.selectExercise(it) } },
                    label = { Text("This exercise") },
                    modifier = Modifier.padding(end = 8.dp),
                )
                FilterChip(
                    selected = curveMode == CurveMode.MUSCLE_GROUP,
                    onClick = { viewModel.selectMuscleGroup(selectedMuscleGroup ?: MuscleGroup.entries.first().name) },
                    label = { Text("Muscle group") },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        if (curveMode == CurveMode.EXERCISE) {
            items(exercises, key = { it.id }) { exercise ->
                Text(
                    exercise.name,
                    style = if (exercise.id == selectedExerciseId) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    color = if (exercise.id == selectedExerciseId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectExercise(exercise.id) }
                        .padding(vertical = 4.dp),
                )
            }
        } else {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    MuscleGroup.entries.forEach { group ->
                        FilterChip(
                            selected = selectedMuscleGroup == group.name,
                            onClick = { viewModel.selectMuscleGroup(group.name) },
                            label = { Text(formatEnumLabel(group.name)) },
                            modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
                        )
                    }
                }
            }
        }
        item {
            if (strengthCurve.isEmpty()) {
                Text("No logged sets yet for this selection.", style = MaterialTheme.typography.bodySmall)
            } else {
                StrengthCurveChart(strengthCurve, modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp))
            }
        }
        item {
            Text("Body Metrics", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                OutlinedTextField(
                    value = waistText,
                    onValueChange = { waistText = it },
                    label = { Text("Waist (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Button(onClick = {
                    viewModel.logBodyMetric(weightText.toDoubleOrNull(), waistText.toDoubleOrNull())
                    weightText = ""
                    waistText = ""
                }) {
                    Text("Log")
                }
            }
        }
        items(bodyMetrics) { metric ->
            Text("${metric.date}: ${metric.weightKg?.let { "${it}kg" } ?: "--"} / ${metric.waistCm?.let { "${it}cm" } ?: "--"}")
        }
    }
}

/** Plain Canvas line chart -- no charting library dependency, same "draw it yourself" approach
 *  HeatP used for its WeeklyBarChart. Normalizes [points]' values to the canvas height. */
@Composable
private fun StrengthCurveChart(points: List<Pair<java.time.LocalDate, Double>>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (points.size < 2) {
            return@Canvas
        }
        val maxValue = points.maxOf { it.second }
        val minValue = points.minOf { it.second }
        val range = (maxValue - minValue).coerceAtLeast(1.0)
        val stepX = size.width / (points.size - 1)
        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { index, (_, value) ->
            val x = stepX * index
            val y = size.height - ((value - minValue) / range * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 4f))
        points.forEachIndexed { index, (_, value) ->
            val x = stepX * index
            val y = size.height - ((value - minValue) / range * size.height).toFloat()
            drawCircle(color = lineColor, radius = 6f, center = Offset(x, y))
        }
    }
}
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: `ProgressViewModel.kt`/`ProgressScreen.kt`/`HeatmapGrid.kt` errors are now gone; only `RoutinesViewModel.kt`'s `createRoutine` call-site mismatch remains (fixed in Task 9).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/progress/ProgressViewModel.kt app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt app/src/main/java/com/lsing/timego/ui/common/HeatmapGrid.kt
git commit -m "Redesign Progress screen: formatted PRs, real strength-curve chart with muscle-group toggle, heatmap label fix"
```

---

## Task 9: Routines screen redesign — day-of-week scheduling, formatted nudge, grouped exercise picker

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/routines/RoutinesViewModel.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/routines/RoutinesScreen.kt`

**Interfaces:**
- Consumes: `ExerciseSections` (Task 5), `formatEnumLabel` (Task 5), updated `WorkoutRepository.createRoutine` (Task 3).
- Produces: `RoutinesViewModel.createRoutine(name: String, exerciseIds: List<Long>, daysOfWeek: List<String>)`.

- [ ] **Step 1: Update `RoutinesViewModel.kt`'s `createRoutine`**

Replace the existing `createRoutine` function with:

```kotlin
    fun createRoutine(name: String, exerciseIds: List<Long>, daysOfWeek: List<String>) {
        viewModelScope.launch { repository.createRoutine(name, exerciseIds, daysOfWeek) }
    }
```

- [ ] **Step 2: Rewrite `RoutinesScreen.kt`**

```kotlin
package com.lsing.timego.ui.routines

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lsing.timego.ui.common.ExerciseSections
import com.lsing.timego.ui.common.formatEnumLabel
import java.time.DayOfWeek

@Composable
fun RoutinesScreen(viewModel: RoutinesViewModel = viewModel()) {
    val routines by viewModel.routines.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val untrainedGroups by viewModel.untrainedGroups.collectAsState()

    var routineName by remember { mutableStateOf("") }
    val selectedExerciseIds = remember { mutableStateOf(setOf<Long>()) }
    val selectedDays = remember { mutableStateOf(setOf<String>()) }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        if (untrainedGroups.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text(
                        "Not trained in a while: ${untrainedGroups.joinToString(", ") { formatEnumLabel(it) }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
        item {
            Text("Your Routines", style = MaterialTheme.typography.titleMedium)
        }
        items(routines, key = { it.id }) { routine ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    "${routine.name} -- ${if (routine.daysOfWeek.isEmpty()) "no days set" else routine.daysOfWeek.joinToString(", ") { formatEnumLabel(it) }}",
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        item {
            Text("New Routine", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            OutlinedTextField(
                value = routineName,
                onValueChange = { routineName = it },
                label = { Text("Routine name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Days", modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                DayOfWeek.entries.forEach { day ->
                    val checked = day.name in selectedDays.value
                    FilterChip(
                        selected = checked,
                        onClick = {
                            selectedDays.value = if (checked) selectedDays.value - day.name else selectedDays.value + day.name
                        },
                        label = { Text(day.name.take(3).lowercase().replaceFirstChar(Char::uppercase)) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }
        }
        item {
            ExerciseSections(exercises = exercises) { exercise ->
                val checked = exercise.id in selectedExerciseIds.value
                Row {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            selectedExerciseIds.value = if (isChecked) {
                                selectedExerciseIds.value + exercise.id
                            } else {
                                selectedExerciseIds.value - exercise.id
                            }
                        },
                    )
                    Text(exercise.name)
                }
            }
        }
        item {
            Button(
                onClick = {
                    if (routineName.isNotBlank() && selectedExerciseIds.value.isNotEmpty()) {
                        viewModel.createRoutine(routineName, selectedExerciseIds.value.toList(), selectedDays.value.toList())
                        routineName = ""
                        selectedExerciseIds.value = emptySet()
                        selectedDays.value = emptySet()
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Create routine")
            }
        }
    }
}
```

- [ ] **Step 3: Verify the full project compiles clean**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL` — this is the first clean build since Task 3 broke the two call sites; both are now fixed (Tasks 7 and 9).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/routines/RoutinesViewModel.kt app/src/main/java/com/lsing/timego/ui/routines/RoutinesScreen.kt
git commit -m "Redesign Routines screen: day-of-week scheduling, formatted nudge, grouped exercise picker"
```

**Frontend is now done.**

---

## Task 10: Verification — install, agent screenshots first, then user confirmation

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: `BUILD SUCCESSFUL` — all domain tests (v1's 11 + this update's `CardioMathTest`, `RoutineScheduleTest`, and the new `ProgressMathTest` case) pass.

- [ ] **Step 2: Install on-device**

Run: `./gradlew installDebug`
Expected: `BUILD SUCCESSFUL`, installs without a migration crash (confirms Task 1's additive migration worked against the device's real existing data).

- [ ] **Step 3: Agent takes and reviews its own screenshots of every screen and the new dialog, BEFORE asking the user anything**

Using `adb shell input tap`/`swipe` to navigate and `adb shell screencap -p //sdcard/<name>.png` + `adb pull` (double-leading-slash to dodge Git Bash's MSYS path mangling), capture and actually look at:
- Log screen: collapsible category sections, a strength exercise card, a cardio exercise card (with pace/calorie text once duration/distance are entered), the routine filter chips, the "+ Add exercise" dialog open.
- Progress screen: heatmap (check month labels aren't clipped), a Personal Record card, the strength curve chart in both "This exercise" and "Muscle group" modes.
- Routines screen: the day-of-week chips, the reformatted (Title Case) muscle-nudge card, a created routine showing its scheduled days.

Fix anything visually broken (overflow, unreadable text, misaligned rows) before proceeding — this is the step v1 skipped, don't skip it again.

- [ ] **Step 4: User confirmation pass**

Ask the user to check, on their real device: logging a cardio set shows pace/calories; creating a Wed/Thu/Sat routine makes it auto-selected on the Log screen on one of those days (or confirm the selection logic by inspecting `routinesForToday` against today's actual weekday if today isn't one of the test days); the exercise sections collapse/expand; the add-exercise dialog opens/closes correctly and the new exercise appears in the right category section.

- [ ] **Step 5: Update the vault note** (`TimeGo - Gym Progress Tracker.md`) with Update 1.1's scope and the process lesson (design pass now happens as part of every UI task, not deferred) once verification passes.
