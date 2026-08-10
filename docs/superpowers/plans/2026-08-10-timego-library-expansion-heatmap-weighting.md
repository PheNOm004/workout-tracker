# TimeGo Library Expansion & Weighted Muscle Correlation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give exercises real per-muscle weighted contribution (not just tag presence) so the muscle-distribution radar chart and body heatmap reflect how much a muscle was actually worked, grow the exercise library from 119 to roughly 300, and clarify the heatmap's color meaning with an on-screen caption.

**Architecture:** An additive `Exercise.muscleWeights: Map<String, Int>` field (0-100 percentage per tagged muscle group), consumed only by `muscleGroupVolumeDistribution` -- every other consumer of `muscleGroups` (section grouping, untrained-muscle nudge, custom-exercise creation) is untouched. Library growth and weight retrofitting happen in per-category curation batches, each independently verified by structural tests.

**Tech Stack:** Kotlin, Jetpack Compose, Room, JUnit.

## Global Constraints

- Room schema version bumps 5 -> 6 via a genuine `ALTER TABLE` migration, same no-`@ColumnInfo(defaultValue=...)` rule as every prior migration in this codebase (see `Exercise.kt`'s doc comment).
- `muscleWeights` is additive and optional: `exercise.muscleWeights[group] ?: 100` is the effective weight everywhere it's read. An exercise with no weights specified behaves exactly as before (full credit per tag).
- Weighting methodology (from the design spec, grounded in published EMG %MVC research): **primary mover = 100** (default, no override needed), **major synergist = 60-70**, **minor stabilizer = 25-40**. Applied via the `weights` param on `strength`/`calisthenics` builder calls, only for muscles that aren't the primary mover.
- `muscleGroups` (the flat tag list), `category`, and `loggingType` (from Project A) are unaffected by this plan's data model changes -- only new exercises and weight retrofits touch `SeedExercises.kt`.
- Real domain logic (the weighting calculation, the converter) gets TDD. Library curation content is data, not logic -- verified by structural tests (counts, no duplicates, no orphaned weight keys), not per-exercise unit tests.
- Phone is connected -- on-device verification happens in the final task, before merge.

---

### Task 1: `muscleWeights` data model, converter, and migration

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/Exercise.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/Converters.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/TimeGoDatabase.kt`
- Create: `app/src/test/java/com/lsing/timego/data/ConvertersTest.kt`

**Interfaces:**
- Produces: `Exercise.muscleWeights: Map<String, Int>`; `Converters.fromMuscleWeights(Map<String, Int>): String`; `Converters.toMuscleWeights(String): Map<String, Int>` -- consumed by Task 2 (`MuscleDistribution.kt`) and every later curation task.

- [ ] **Step 1: Write the failing converter tests**

```kotlin
package com.lsing.timego.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `muscleWeights round-trips through encode-decode`() {
        val original = mapOf("QUADS" to 100, "GLUTES" to 60)
        val encoded = converters.fromMuscleWeights(original)
        val decoded = converters.toMuscleWeights(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `blank muscleWeights string decodes to empty map`() {
        assertEquals(emptyMap<String, Int>(), converters.toMuscleWeights(""))
    }

    @Test
    fun `single-entry muscleWeights round-trips`() {
        val original = mapOf("ABS" to 40)
        assertEquals(original, converters.toMuscleWeights(converters.fromMuscleWeights(original)))
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.data.ConvertersTest"`
Expected: FAIL (compile error -- `fromMuscleWeights`/`toMuscleWeights` don't exist yet).

- [ ] **Step 3: Add the converter to `Converters.kt`**

```kotlin
package com.lsing.timego.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromEpochDay(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    /** For a String list column (e.g. an Exercise's muscle-group tags). ASCII unit separator
     *  (0x1F), not comma, since tag text could legitimately contain a comma. Lifted from HeatP's
     *  sub-option-list converter, which needed the same non-comma delimiter for the same reason. */
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(LIST_DELIMITER)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(LIST_DELIMITER)

    /** For Exercise.muscleWeights: MuscleGroup name -> 0-100 percentage contribution. One
     *  delimiter level below the list delimiter (ASCII record separator 0x1E between entries,
     *  unit separator 0x1F between an entry's group/weight pair), same non-printable-character
     *  convention as [fromStringList] -- group names and weights can't contain either. */
    @TypeConverter
    fun fromMuscleWeights(value: Map<String, Int>): String =
        value.entries.joinToString(ENTRY_DELIMITER) { (group, weight) -> "$group$PAIR_DELIMITER$weight" }

    @TypeConverter
    fun toMuscleWeights(value: String): Map<String, Int> =
        if (value.isBlank()) {
            emptyMap()
        } else {
            value.split(ENTRY_DELIMITER).associate { entry ->
                val (group, weight) = entry.split(PAIR_DELIMITER)
                group to weight.toInt()
            }
        }

    private companion object {
        const val LIST_DELIMITER = "\u001F"
        const val ENTRY_DELIMITER = "\u001E"
        const val PAIR_DELIMITER = "\u001F"
    }
}
```
Note: `LIST_DELIMITER` is written here as the explicit `\u001F` escape for clarity in this plan; the existing file already uses this exact character (it displays as invisible in a plain read). Do not change its value, only add the two new constants and the two new converter functions alongside it.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.data.ConvertersTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Add `muscleWeights` to `Exercise.kt`**

```kotlin
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroups: List<String>,
    val isCustom: Boolean,
    val category: String = ExerciseCategory.STRENGTH.name,
    val loggingType: String = LoggingType.WEIGHT_REPS.name,
    val muscleWeights: Map<String, Int> = emptyMap(),
)
```
Extend the class doc comment's last paragraph to also mention `muscleWeights`:
```kotlin
/** [muscleGroups] stores MuscleGroup enum names as strings (via Converters' fromStringList/
 *  toStringList), not the enum type directly, so Room's converter resolution stays unambiguous.
 *  [category] and [loggingType] store their enum names the same way. [muscleWeights] maps a
 *  tagged group's name to a 0-100 contribution percentage (see Converters.fromMuscleWeights) --
 *  additive and optional: a group missing from this map defaults to 100 (full credit) wherever
 *  it's read, so exercises without explicit weights behave exactly as before.
 *
 *  Deliberately NOT annotated with @ColumnInfo(defaultValue=...) even though MIGRATION_1_2/
 *  MIGRATION_4_5/MIGRATION_5_6 add these columns via `ALTER TABLE ... DEFAULT '...'` -- confirmed
 *  on a real device that Room's schema reader doesn't reflect an ALTER-added column's DEFAULT
 *  back through PRAGMA table_info in a way its validator accepts, so declaring the annotation
 *  makes Room reject every real migrated install with "Migration didn't properly handle:
 *  exercises" on open. Room still enforces NOT NULL at the Kotlin/insert level via these fields'
 *  non-null types either way. */
```

- [ ] **Step 6: Add `MIGRATION_5_6` and bump the database version in `TimeGoDatabase.kt`**

```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN muscleWeights TEXT NOT NULL DEFAULT ''")
    }
}
```
Add after `MIGRATION_4_5`. Change `version = 5` to `version = 6`. Change `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)` to `.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)`.

- [ ] **Step 7: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/Exercise.kt app/src/main/java/com/lsing/timego/data/Converters.kt app/src/main/java/com/lsing/timego/data/TimeGoDatabase.kt app/src/test/java/com/lsing/timego/data/ConvertersTest.kt
git commit -m "Add muscleWeights data model, converter, and schema migration"
```

---

### Task 2: Weighted muscle-volume distribution

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/MuscleDistribution.kt`
- Modify: `app/src/test/java/com/lsing/timego/domain/MuscleDistributionTest.kt`

**Interfaces:**
- Consumes: `Exercise.muscleWeights` (Task 1).
- No signature change to `muscleGroupVolumeDistribution` -- same parameters/return type, only the per-group volume contribution changes.

- [ ] **Step 1: Write the failing test**

Append to `MuscleDistributionTest.kt`:
```kotlin
    @Test
    fun `muscleGroupVolumeDistribution applies muscleWeights as a percentage of volume`() {
        val squat = Exercise(
            id = 1, name = "Squat", muscleGroups = listOf("QUADS", "GLUTES"), isCustom = false,
            category = "STRENGTH", loggingType = "WEIGHT_REPS", muscleWeights = mapOf("GLUTES" to 60),
        )
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
        )
        val exercisesById = mapOf(1L to squat)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 10))

        val distribution = muscleGroupVolumeDistribution(sets, exercisesById, sessionDateById, since = LocalDate.of(2026, 8, 1))

        // 100kg x 5 = 500 volume. QUADS has no override -> defaults to 100% -> full 500.
        // GLUTES is explicitly weighted 60% -> 300.
        assertEquals(500.0, distribution["QUADS"]!!, 0.001)
        assertEquals(300.0, distribution["GLUTES"]!!, 0.001)
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.domain.MuscleDistributionTest"`
Expected: FAIL on the new test (GLUTES currently gets full 500, not 300 -- no weighting applied yet).

- [ ] **Step 3: Update `muscleGroupVolumeDistribution`**

Change the loop body in `MuscleDistribution.kt`:
```kotlin
        for (group in exercise.muscleGroups) {
            volumeByGroup[group] = (volumeByGroup[group] ?: 0.0) + volume
        }
```
to:
```kotlin
        for (group in exercise.muscleGroups) {
            val weightedVolume = volume * (exercise.muscleWeights[group] ?: 100) / 100.0
            volumeByGroup[group] = (volumeByGroup[group] ?: 0.0) + weightedVolume
        }
```
Update the function's doc comment to mention weighting:
```kotlin
/** Total volume per muscle group across WEIGHT_REPS/HOLD sets logged on or after [since] -- an
 *  exercise contributes its volume to every muscle group it's tagged with, scaled by that
 *  group's entry in [Exercise.muscleWeights] (defaulting to 100% when unspecified) -- e.g. a
 *  squat set counts its full volume toward QUADS but a partial-credit fraction toward GLUTES if
 *  weighted lower. Backs the "muscle distribution" radar chart and the muscle-body heatmap.
 *  CARDIO/WARMUP excluded, same reasoning as personalRecords/muscleGroupStrengthCurve -- their
 *  weightKg/reps are 0.0/0 sentinels, not real values. HOLD exercises use holdSeconds directly as
 *  their "volume" figure before weighting: a rough proxy, not weight-equivalent. */
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.domain.MuscleDistributionTest"`
Expected: PASS (all tests, including the new one).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/MuscleDistribution.kt app/src/test/java/com/lsing/timego/domain/MuscleDistributionTest.kt
git commit -m "Apply muscleWeights to muscle-volume distribution"
```

---

### Task 3: Heatmap clarity caption

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt`

**Interfaces:** none (pure UI addition, no new state).

- [ ] **Step 1: Add the caption under the Muscle Distribution section header**

Change:
```kotlin
        item {
            SectionHeader("Muscle Distribution (last 30 days)")
            if (muscleDistribution.isEmpty()) {
```
to:
```kotlin
        item {
            SectionHeader("Muscle Distribution (last 30 days)")
            Text(
                "Colors show volume relative to your most-trained muscle group this period",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            if (muscleDistribution.isEmpty()) {
```

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt
git commit -m "Add explanatory caption to the muscle distribution heatmap section"
```

---

### Task 4: Retrofit weights onto existing multi-muscle-group exercises

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SeedExercises.kt`

**Interfaces:**
- Consumes: the `weights` param on `strength`/`calisthenics` (already exists as a builder param; this task is the first to use it).

**Methodology** (from the design spec, Section 2): primary mover defaults to 100 (no entry needed), major synergist gets 60-70, minor stabilizer gets 25-40. Applied to every existing multi-muscle-group STRENGTH/CALISTHENICS exercise in the current 119-entry list (single-group exercises need no change; CARDIO/WARMUP exercises are tagged for the nudge feature only and don't carry meaningful weighted volume, so they're left unweighted).

- [ ] **Step 1: Add a `weights` parameter to the `strength` builder**

`calisthenics` already has a `weights: Map<MuscleGroup, Int> = emptyMap()` parameter from Project A. `strength` doesn't yet. Change:
```kotlin
private fun strength(name: String, vararg groups: MuscleGroup) =
    Exercise(name = name, muscleGroups = groups.map { it.name }, isCustom = false, category = ExerciseCategory.STRENGTH.name, loggingType = LoggingType.WEIGHT_REPS.name)
```
to:
```kotlin
private fun strength(name: String, vararg groups: MuscleGroup, weights: Map<MuscleGroup, Int> = emptyMap()) =
    Exercise(name = name, muscleGroups = groups.map { it.name }, isCustom = false, category = ExerciseCategory.STRENGTH.name, loggingType = LoggingType.WEIGHT_REPS.name, muscleWeights = weights.mapKeys { it.key.name })
```
Also change `calisthenics` to actually populate `muscleWeights` (it currently only accepts `loggingType`, not `weights` -- add the parameter):
```kotlin
private fun calisthenics(name: String, vararg groups: MuscleGroup, loggingType: LoggingType = LoggingType.WEIGHT_REPS, weights: Map<MuscleGroup, Int> = emptyMap()) =
    Exercise(name = name, muscleGroups = groups.map { it.name }, isCustom = false, category = ExerciseCategory.CALISTHENICS.name, loggingType = loggingType.name, muscleWeights = weights.mapKeys { it.key.name })
```

- [ ] **Step 2: Update every existing multi-group `strength`/`calisthenics` call with a `weights` override**

Go through `SEED_EXERCISES` in order and add `weights = mapOf(...)` to every call that tags 2+ muscle groups, following the methodology above. Worked examples to match the expected quality bar:
```kotlin
    strength("Barbell Bench Press", MuscleGroup.CHEST, MuscleGroup.TRICEPS, weights = mapOf(MuscleGroup.TRICEPS to 65)),
    strength("Conventional Deadlift", MuscleGroup.LOWER_BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, weights = mapOf(MuscleGroup.HAMSTRINGS to 70, MuscleGroup.GLUTES to 65)),
    strength("Barbell Row", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS, weights = mapOf(MuscleGroup.UPPER_BACK to 70, MuscleGroup.BICEPS to 35)),
    strength("Barbell Back Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES, weights = mapOf(MuscleGroup.GLUTES to 60)),
    calisthenics("Pull-Up", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS, weights = mapOf(MuscleGroup.UPPER_BACK to 65, MuscleGroup.BICEPS to 40)),
```
Apply the same pattern (primary group = no entry, every other tagged group gets an explicit 25-70 value per the tier guidance) to the remaining ~35 multi-group entries in the current file: Incline/Decline/Dumbbell Bench Press, Close-Grip Bench Press, Pendlay Row, T-Bar Row, Seated Cable Row, Lat Pulldown, Single-Arm Dumbbell Row, Machine Row, Overhead Press, Seated Dumbbell Shoulder Press, Arnold Press, Face Pull, Machine Shoulder Press, Sumo Deadlift, Romanian Deadlift, Walking Lunge, Bulgarian Split Squat, Cable Pull-Through, Landmine Press, Landmine Row, Zercher Squat, Good Morning, Reverse Fly, Reverse Barbell Curl, Zottman Curl, Farmer's Carry, Push-Up, Diamond Push-Up, Dip, Bodyweight Squat, Jump Squat, Lunge, Mountain Climber, Superman, Pike Push-Up, Inverted Row, Step-Up, Pistol Squat, Archer Push-Up, Handstand Push-Up, Australian Pull-Up.

- [ ] **Step 3: Write structural verification test**

Create/extend the existing `SeedExercisesTest.kt`, appending:
```kotlin
    @Test
    fun `every muscleWeights key is one of the exercise's own tagged muscle groups`() {
        val orphaned = SEED_EXERCISES.filter { exercise ->
            exercise.muscleWeights.keys.any { it !in exercise.muscleGroups }
        }
        assertEquals(emptyList<Exercise>(), orphaned)
    }

    @Test
    fun `every muscleWeights value is between 1 and 100`() {
        val outOfRange = SEED_EXERCISES.filter { exercise -> exercise.muscleWeights.values.any { it !in 1..100 } }
        assertEquals(emptyList<Exercise>(), outOfRange)
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.data.SeedExercisesTest"`
Expected: PASS (5 tests total: the 3 from Project A plus these 2).

- [ ] **Step 5: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SeedExercises.kt app/src/test/java/com/lsing/timego/data/SeedExercisesTest.kt
git commit -m "Retrofit weighted muscle contribution onto existing multi-group exercises"
```

---

### Task 5: New exercises -- Chest, Back, Shoulders (strength)

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SeedExercises.kt`

**Target:** ~15 new Chest, ~20 new Back, ~15 new Shoulders `strength(...)` entries (~50 total), appended after the existing Strength section, following the same muscle-group and weighting conventions as the existing library and Task 4's retrofits. Cover equipment variants not yet present (e.g. Smith Machine, cable variants, single-arm/unilateral versions, additional row/pulldown grips, additional press angles).

- [ ] **Step 1: Add the ~50 new strength exercises to `SEED_EXERCISES`**

Curate and add real entries using the `strength(name, vararg groups, weights = ...)` builder, matching the file's existing section-comment style (`// Strength -- Chest`, etc.) and the weighting methodology from Task 4. Every entry must have a plausible real exercise name, correct `MuscleGroup` tags, and (for multi-group entries) a `weights` override on every non-primary group.

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run structural tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.data.SeedExercisesTest"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SeedExercises.kt
git commit -m "Add Chest/Back/Shoulders strength exercises to the library"
```

---

### Task 6: New exercises -- Arms, Forearms, Legs (strength)

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SeedExercises.kt`

**Target:** ~15 new Arms (biceps/triceps), ~8 new Forearms, ~25 new Legs (quads/hamstrings/glutes/calves) `strength(...)` entries (~48 total). Same conventions as Task 5.

- [ ] **Step 1: Add the ~48 new strength exercises to `SEED_EXERCISES`**

Same process as Task 5, Step 1, for this task's muscle groups.

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run structural tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.data.SeedExercisesTest"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SeedExercises.kt
git commit -m "Add Arms/Forearms/Legs strength exercises to the library"
```

---

### Task 7: New exercises -- Calisthenics (including new HOLD exercises)

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SeedExercises.kt`

**Target:** ~35 new `calisthenics(...)` entries covering more bodyweight progressions (e.g. Archer Pull-Up, Pseudo Planche Push-Up, Nordic Curl, Copenhagen Plank, Hollow Body Hold, Skin the Cat, Muscle-Up) across all muscle groups. Any genuinely isometric addition (e.g. Hollow Body Hold, Copenhagen Plank, additional planche/lever variants) gets `loggingType = LoggingType.HOLD` per Project A's model, same as the six existing HOLD exercises -- don't default new isometric holds to `WEIGHT_REPS` and repeat the bug Project A fixed.

- [ ] **Step 1: Add the ~35 new calisthenics exercises to `SEED_EXERCISES`**

Curate real entries, correctly splitting `loggingType` between `WEIGHT_REPS` (rep-based movements) and `HOLD` (timed holds), with `weights` overrides on multi-group entries per the established methodology.

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run structural tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.data.SeedExercisesTest"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SeedExercises.kt
git commit -m "Add new calisthenics exercises, including additional HOLD-type entries"
```

---

### Task 8: New exercises -- Cardio and Warmup

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SeedExercises.kt`

**Target:** ~10 new `cardio(...)` and ~10 new `warmup(...)` entries (machine variants, additional dynamic-stretch/mobility movements). These stay unweighted (per Task 4's scope note -- CARDIO/WARMUP muscle tags exist for the untrained-muscle nudge only, not weighted volume).

- [ ] **Step 1: Add the ~20 new cardio/warmup exercises to `SEED_EXERCISES`**

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run structural tests, then the total-count sanity check**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.data.SeedExercisesTest"`
Expected: PASS.

Then append this final structural test to `SeedExercisesTest.kt` and run it:
```kotlin
    @Test
    fun `no duplicate exercise names`() {
        val names = SEED_EXERCISES.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `library has grown to roughly 300 exercises`() {
        assertEquals(true, SEED_EXERCISES.size in 250..350)
    }
```
Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.data.SeedExercisesTest"`
Expected: PASS (all tests).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SeedExercises.kt app/src/test/java/com/lsing/timego/data/SeedExercisesTest.kt
git commit -m "Add new cardio/warmup exercises and final library structural checks"
```

---

### Task 9: Full verification and on-device check

**Files:** none (verification only)

- [ ] **Step 1: Full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests passing.

- [ ] **Step 2: Full debug build and install**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL, "Installed on 1 device." (this also runs `MIGRATION_5_6` against the real device database and `seedMissingExercises` inserts every new exercise by name).

- [ ] **Step 3: On-device verification (hand off to the user)**

Ask the user to: open Log and confirm the library sections now show substantially more exercises per muscle group/category; open Progress, confirm the "Colors show volume relative to..." caption renders under Muscle Distribution; log a set for a compound exercise (e.g. Squat) and a corresponding isolation exercise for its secondary muscle (e.g. Leg Extension for QUADS, or a glute-focused exercise), then compare the radar chart / heatmap -- the isolation exercise's target muscle should visibly outweigh the compound exercise's partial-credit secondary contribution, where before Project B they'd have looked identical.

- [ ] **Step 4: Update the vault project note**

Add a session entry to `C:\Users\lsing\.claude\obsidian_demo\Projects\TimeGo - Gym Progress Tracker.md` recording that library expansion + weighted muscle correlation shipped: `Exercise.muscleWeights` added (additive, EMG-informed primary/synergist/stabilizer tiers), library grown from 119 to ~300 exercises across all categories, heatmap caption added for clarity (semantics kept relative-to-self per explicit user choice). Note this completes the two-part post-visual-identity-pass follow-up (Project A + Project B).

- [ ] **Step 5: Verify git state**

```bash
cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"
git status
git log --oneline -9
```

Expected: eight commits from Tasks 1-8 visible, working tree clean.

---

## Self-Review Notes

- **Spec coverage**: Section 1 (weighted data model) -> Task 1. Section 2 (weighting methodology) -> Tasks 4-8 (applied consistently, methodology restated in each task's Target). Section 3 (library expansion to ~300) -> Tasks 5-8. Section 4 (heatmap caption) -> Task 3. Verification -> Task 9.
- **Type consistency checked**: `Converters.fromMuscleWeights`/`toMuscleWeights` (Task 1) match the `Exercise.muscleWeights: Map<String, Int>` field type exactly. `muscleGroupVolumeDistribution`'s weighting lookup (Task 2) uses `exercise.muscleWeights[group] ?: 100` consistently with Task 1's doc comment describing the same default. The `weights` builder parameter (used starting Task 4) was already defined in Project A's `SeedExercises.kt` work as `weights: Map<MuscleGroup, Int> = emptyMap()` on `calisthenics`; this plan adds the equivalent parameter to `strength` in Task 4 Step 1 (the design spec's Section 1 covers both builder functions, not just `calisthenics`).
- **Curation task sizing**: Tasks 5-8 specify methodology, targets, and worked examples rather than pre-authoring all ~180 new exercises inline in this document -- the content itself is generated during each task's execution (same session), verified immediately after by the structural tests already defined in Tasks 4 and 8, rather than duplicated here and in the codebase. This is a deliberate deviation from full inline-code-per-step for this plan's curation tasks specifically, given the content volume; every other task (1-3, 9) follows the standard fully-specified format.
- **Placeholder scan**: no TBD/TODO markers. Task 5-8's "curate real entries" instructions are backed by concrete acceptance criteria (structural tests, target ranges) and worked examples, not vague "add appropriate exercises" language.
