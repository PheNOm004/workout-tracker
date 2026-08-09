# TimeGo Fine-Grained Muscle Group Tagging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand `MuscleGroup` from 12 coarse values to 17 fine-grained anatomical groups, re-tag all seed exercises, and reclassify the anatomical heatmap diagram to match wherever the traced source art geometrically allows it.

**Architecture:** Pure enum expansion — `Exercise.muscleGroups: List<String>` already supports multiple tags per exercise, so no schema/type changes are needed. Every consumer (`AddExerciseDialog`, the untrained-muscle-group nudge, `RadarChart`) already iterates `MuscleGroup.entries` generically and needs zero code changes. Only two files carry hardcoded per-exercise/per-path group assignments that must be rewritten: `SeedExercises.kt` (119 exercises) and `MuscleBodyArt.kt` (176 traced SVG paths). A small new pure domain function handles the one case where the diagram intentionally stays coarser than the data (front/side/rear delts rendering as one combined "shoulders" zone, since the traced art doesn't separate them).

**Tech Stack:** Kotlin, Jetpack Compose, Room, JUnit (plain Kotlin domain tests, no Robolectric needed for this work).

## Global Constraints

- No Room schema/column changes — `Exercise.muscleGroups` stays `List<String>`; this is purely new enum constant names.
- No in-place data migration — confirmed with the user that no real logged workout history exists yet. Verification wipes app data (`adb shell pm clear com.lsing.timego`) rather than migrating it.
- `FULL_BODY` is unchanged and untouched by this work — it's the cardio/warmup catch-all, not part of the anatomical split.
- The diagram (`MuscleBodyArt.kt`) keeps `FRONT_DELTS`/`SIDE_DELTS`/`REAR_DELTS` as one combined "shoulders" visual zone — the traced art has no clean boundary between them (confirmed with the user, this is an accepted limitation, not a bug to fix later in this plan).
- `LOWER_BACK` has no dedicated shapes in the traced diagram art (the 8 existing `BACK`-tagged paths all sit in the upper-to-mid-back y-range) — those paths split only into `LATS`/`UPPER_BACK`; `LOWER_BACK` stays a data-only tag (exercises can still carry it) with no diagram representation, same class of limitation as the delts.
- Screenshot-before-asking discipline continues for any UI verification (per the Update 1.1 process lesson already established in this codebase).

---

### Task 1: Expand `MuscleGroup` enum

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/MuscleGroup.kt`

**Interfaces:**
- Produces: `MuscleGroup` enum with 17 values — `CHEST, LATS, UPPER_BACK, LOWER_BACK, FRONT_DELTS, SIDE_DELTS, REAR_DELTS, BICEPS, TRICEPS, FOREARMS, ABS, OBLIQUES, QUADS, HAMSTRINGS, GLUTES, CALVES, FULL_BODY` — consumed by name (`.name`, stored as `String`) throughout the codebase, so no other type changes.

This is a pure constant rename/expansion. No Room migration is needed: `Exercise.muscleGroups` is a `List<String>` column via `Converters.fromStringList`/`toStringList` — Room's schema validator only cares about the column's SQL type (`TEXT`), not which string values the app happens to write into it, so changing the set of valid enum names doesn't change the Room-computed schema hash at all (unlike the `@ColumnInfo(defaultValue=...)` case documented in `Exercise.kt` that broke migration validation previously). Verification wipes app data anyway, which bypasses every migration path on the next fresh install regardless.

- [ ] **Step 1: Replace the enum**

```kotlin
package com.lsing.timego.data

enum class MuscleGroup {
    CHEST,
    LATS, UPPER_BACK, LOWER_BACK,
    FRONT_DELTS, SIDE_DELTS, REAR_DELTS,
    BICEPS, TRICEPS, FOREARMS,
    ABS, OBLIQUES,
    QUADS, HAMSTRINGS, GLUTES, CALVES,
    FULL_BODY,
}
```

- [ ] **Step 2: Confirm the project does not yet compile (expected — downstream files still reference removed constants)**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo" ; .\gradlew.bat compileDebugKotlin`
Expected: FAIL — unresolved reference errors in `SeedExercises.kt` and `MuscleBodyArt.kt` for `MuscleGroup.BACK`, `MuscleGroup.SHOULDERS`, `MuscleGroup.CORE`. This confirms the old call sites are exactly the ones Tasks 2 and 4 fix — no other files reference the removed constants.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/MuscleGroup.kt
git commit -m "Expand MuscleGroup to 17 fine-grained anatomical groups"
```

---

### Task 2: Re-tag all seed exercises

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SeedExercises.kt`

**Interfaces:**
- Consumes: `MuscleGroup` (Task 1).
- Produces: `SEED_EXERCISES: List<Exercise>` — same shape as before (119 exercises, same names/categories), only `muscleGroups` values change. `WorkoutRepository.seedMissingExercises` (unmodified) consumes this list by exercise name.

Re-tagged using standard exercise-science muscle mapping: `BACK` splits into `LATS` (lat-dominant pulls: rows, pulldowns, pull-ups) plus `UPPER_BACK` (upper-back/rear-delt-adjacent movements: face pulls, reverse flys, shrugs) or `LOWER_BACK` (hip-hinge/spinal-erector movements: deadlifts, good mornings); `SHOULDERS` splits into `FRONT_DELTS`/`SIDE_DELTS`/`REAR_DELTS` by which head the movement actually targets; `CORE` splits into `ABS` (flexion movements: crunches, sit-ups, leg raises) or `OBLIQUES` (rotational/anti-rotational/lateral movements: twists, side planks, woodchoppers).

- [ ] **Step 1: Replace the file**

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
    strength("Incline Barbell Bench Press", MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS),
    strength("Decline Barbell Bench Press", MuscleGroup.CHEST, MuscleGroup.TRICEPS),
    strength("Dumbbell Bench Press", MuscleGroup.CHEST, MuscleGroup.TRICEPS),
    strength("Incline Dumbbell Press", MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS),
    strength("Dumbbell Fly", MuscleGroup.CHEST),
    strength("Cable Crossover", MuscleGroup.CHEST),
    strength("Machine Chest Press", MuscleGroup.CHEST, MuscleGroup.TRICEPS),
    strength("Pec Deck Machine", MuscleGroup.CHEST),
    // Strength -- Back
    strength("Conventional Deadlift", MuscleGroup.LOWER_BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
    strength("Sumo Deadlift", MuscleGroup.LOWER_BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
    strength("Barbell Row", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
    strength("Pendlay Row", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
    strength("T-Bar Row", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
    strength("Seated Cable Row", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
    strength("Lat Pulldown", MuscleGroup.LATS, MuscleGroup.BICEPS),
    strength("Single-Arm Dumbbell Row", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
    strength("Machine Row", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
    // Strength -- Shoulders
    strength("Overhead Press", MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.TRICEPS),
    strength("Seated Dumbbell Shoulder Press", MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.TRICEPS),
    strength("Arnold Press", MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.TRICEPS),
    strength("Lateral Raise", MuscleGroup.SIDE_DELTS),
    strength("Front Raise", MuscleGroup.FRONT_DELTS),
    strength("Rear Delt Fly", MuscleGroup.REAR_DELTS),
    strength("Face Pull", MuscleGroup.REAR_DELTS, MuscleGroup.UPPER_BACK),
    strength("Machine Shoulder Press", MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.TRICEPS),
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
    strength("Cable Woodchopper", MuscleGroup.OBLIQUES),
    strength("Weighted Russian Twist", MuscleGroup.OBLIQUES),
    strength("Machine Ab Crunch", MuscleGroup.ABS),
    strength("Hanging Weighted Leg Raise", MuscleGroup.ABS),
    strength("Landmine Press", MuscleGroup.FRONT_DELTS, MuscleGroup.CHEST),
    strength("Landmine Row", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
    strength("Zercher Squat", MuscleGroup.QUADS, MuscleGroup.ABS),
    strength("Good Morning", MuscleGroup.HAMSTRINGS, MuscleGroup.LOWER_BACK),
    strength("Reverse Fly", MuscleGroup.REAR_DELTS, MuscleGroup.UPPER_BACK),
    strength("Shrugs", MuscleGroup.UPPER_BACK),
    // Strength -- Forearms
    strength("Wrist Curl", MuscleGroup.FOREARMS),
    strength("Reverse Wrist Curl", MuscleGroup.FOREARMS),
    strength("Reverse Barbell Curl", MuscleGroup.FOREARMS, MuscleGroup.BICEPS),
    strength("Zottman Curl", MuscleGroup.FOREARMS, MuscleGroup.BICEPS),
    strength("Farmer's Carry", MuscleGroup.FOREARMS, MuscleGroup.ABS),
    strength("Wrist Roller", MuscleGroup.FOREARMS),
    strength("Plate Pinch Hold", MuscleGroup.FOREARMS),

    // Calisthenics
    calisthenics("Push-Up", MuscleGroup.CHEST, MuscleGroup.TRICEPS),
    calisthenics("Diamond Push-Up", MuscleGroup.TRICEPS, MuscleGroup.CHEST),
    calisthenics("Wide Push-Up", MuscleGroup.CHEST),
    calisthenics("Pull-Up", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
    calisthenics("Chin-Up", MuscleGroup.LATS, MuscleGroup.BICEPS),
    calisthenics("Dip", MuscleGroup.TRICEPS, MuscleGroup.CHEST),
    calisthenics("Bodyweight Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    calisthenics("Jump Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    calisthenics("Lunge", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    calisthenics("Glute Bridge", MuscleGroup.GLUTES),
    calisthenics("Burpee", MuscleGroup.FULL_BODY),
    calisthenics("Mountain Climber", MuscleGroup.ABS, MuscleGroup.FULL_BODY),
    calisthenics("Plank", MuscleGroup.ABS),
    calisthenics("Side Plank", MuscleGroup.OBLIQUES),
    calisthenics("Sit-Up", MuscleGroup.ABS),
    calisthenics("Hanging Leg Raise", MuscleGroup.ABS),
    calisthenics("Bicycle Crunch", MuscleGroup.ABS, MuscleGroup.OBLIQUES),
    calisthenics("Superman", MuscleGroup.LOWER_BACK, MuscleGroup.ABS),
    calisthenics("Pike Push-Up", MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.TRICEPS),
    calisthenics("Inverted Row", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
    calisthenics("Step-Up", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    calisthenics("Wall Sit", MuscleGroup.QUADS),
    calisthenics("Pistol Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    calisthenics("Archer Push-Up", MuscleGroup.CHEST, MuscleGroup.TRICEPS),
    calisthenics("L-Sit", MuscleGroup.ABS),
    calisthenics("Handstand Push-Up", MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.TRICEPS),
    calisthenics("Australian Pull-Up", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS),
    calisthenics("Donkey Kick", MuscleGroup.GLUTES),
    calisthenics("Flutter Kick", MuscleGroup.ABS),
    calisthenics("Dead Hang", MuscleGroup.FOREARMS, MuscleGroup.LATS),

    // Warmup
    warmup("Arm Circles", MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS),
    warmup("Leg Swings", MuscleGroup.HAMSTRINGS, MuscleGroup.QUADS),
    warmup("Jumping Jacks", MuscleGroup.FULL_BODY),
    warmup("Band Pull-Apart", MuscleGroup.REAR_DELTS, MuscleGroup.UPPER_BACK),
    warmup("Bodyweight Hip Circles", MuscleGroup.GLUTES),
    warmup("Dynamic Chest Stretch", MuscleGroup.CHEST),
    warmup("Cat-Cow Stretch", MuscleGroup.LOWER_BACK, MuscleGroup.ABS),
    warmup("World's Greatest Stretch", MuscleGroup.FULL_BODY),
    warmup("High Knees", MuscleGroup.QUADS, MuscleGroup.FULL_BODY),
    warmup("Butt Kicks", MuscleGroup.HAMSTRINGS, MuscleGroup.FULL_BODY),
    warmup("Ankle Bounces", MuscleGroup.CALVES),
    warmup("Torso Twists", MuscleGroup.OBLIQUES),
    warmup("Walking Knee Hugs", MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
    warmup("Inchworm", MuscleGroup.FULL_BODY),
    warmup("Shoulder Dislocates", MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS),
    warmup("Hip Flexor Stretch", MuscleGroup.QUADS),

    // Cardio
    cardio("Running", MuscleGroup.FULL_BODY),
    cardio("Treadmill Running", MuscleGroup.FULL_BODY),
    cardio("Incline Walking", MuscleGroup.FULL_BODY),
    cardio("Cycling", MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS),
    cardio("Stationary Bike", MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS),
    cardio("Rowing Machine", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.FULL_BODY),
    cardio("Jump Rope", MuscleGroup.CALVES, MuscleGroup.FULL_BODY),
    cardio("Stair Climbing", MuscleGroup.QUADS, MuscleGroup.GLUTES),
    cardio("Swimming", MuscleGroup.FULL_BODY),
    cardio("Elliptical", MuscleGroup.FULL_BODY),
    cardio("Sprint Intervals", MuscleGroup.FULL_BODY),
    cardio("Hiking", MuscleGroup.FULL_BODY),
    cardio("Ski Erg", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.FULL_BODY),
    cardio("Assault Bike", MuscleGroup.FULL_BODY),
)
```

- [ ] **Step 2: Compile to confirm this file no longer references removed constants**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo" ; .\gradlew.bat compileDebugKotlin`
Expected: FAIL only on `MuscleBodyArt.kt` now (Task 4 not yet done) — no more errors pointing at `SeedExercises.kt`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SeedExercises.kt
git commit -m "Re-tag all seed exercises with fine-grained muscle groups"
```

---

### Task 3: Domain function for the combined shoulder diagram zone

**Files:**
- Create: `app/src/main/java/com/lsing/timego/domain/DiagramMuscleZone.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/DiagramMuscleZoneTest.kt`

**Interfaces:**
- Consumes: `MuscleGroup` (Task 1), `Map<String, Float>` (same shape as `ProgressViewModel.muscleDistribution` / `RadarChart`'s existing input).
- Produces: `fun diagramZoneIntensity(group: MuscleGroup, intensities: Map<String, Float>): Float` — consumed by `MuscleBodyDiagram.kt` in Task 4.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.lsing.timego.domain

import com.lsing.timego.data.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagramMuscleZoneTest {
    @Test
    fun `non-delt group resolves to its own intensity`() {
        val intensities = mapOf(MuscleGroup.CHEST.name to 0.8f)
        assertEquals(0.8f, diagramZoneIntensity(MuscleGroup.CHEST, intensities), 0.001f)
    }

    @Test
    fun `group missing from intensities resolves to zero`() {
        assertEquals(0f, diagramZoneIntensity(MuscleGroup.QUADS, emptyMap()), 0.001f)
    }

    @Test
    fun `any delt group resolves to the average of all three delt intensities`() {
        val intensities = mapOf(
            MuscleGroup.FRONT_DELTS.name to 0.9f,
            MuscleGroup.SIDE_DELTS.name to 0.3f,
            MuscleGroup.REAR_DELTS.name to 0.0f,
        )
        val expected = (0.9f + 0.3f + 0.0f) / 3f
        assertEquals(expected, diagramZoneIntensity(MuscleGroup.FRONT_DELTS, intensities), 0.001f)
        assertEquals(expected, diagramZoneIntensity(MuscleGroup.SIDE_DELTS, intensities), 0.001f)
        assertEquals(expected, diagramZoneIntensity(MuscleGroup.REAR_DELTS, intensities), 0.001f)
    }

    @Test
    fun `delt group with no logged intensities resolves to zero`() {
        assertEquals(0f, diagramZoneIntensity(MuscleGroup.REAR_DELTS, emptyMap()), 0.001f)
    }
}
```

- [ ] **Step 2: Run the test file to verify it fails to compile (function doesn't exist yet)**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo" ; .\gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.DiagramMuscleZoneTest"`
Expected: FAIL — "unresolved reference: diagramZoneIntensity"

- [ ] **Step 3: Write the implementation**

```kotlin
package com.lsing.timego.domain

import com.lsing.timego.data.MuscleGroup

private val SHOULDER_DELTS = setOf(MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS)

/** Intensity to render a diagram shape tagged [group] with, given per-group [intensities] (0f..1f,
 *  missing key treated as untrained/0f -- same shape as [RadarChart]'s input). The anatomical
 *  heatmap diagram keeps front/side/rear delts as one combined "shoulders" visual zone since the
 *  traced source art has no clean boundary between them (see docs/superpowers/specs), so any of
 *  the three delt groups resolves to the average of all three instead of its own individual value;
 *  every other group resolves to its own value directly. */
fun diagramZoneIntensity(group: MuscleGroup, intensities: Map<String, Float>): Float =
    if (group in SHOULDER_DELTS) {
        SHOULDER_DELTS.map { intensities[it.name] ?: 0f }.average().toFloat()
    } else {
        intensities[group.name] ?: 0f
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo" ; .\gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.DiagramMuscleZoneTest"`
Expected: PASS — 4 tests green

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/DiagramMuscleZone.kt app/src/test/java/com/lsing/timego/domain/DiagramMuscleZoneTest.kt
git commit -m "Add diagramZoneIntensity for the combined shoulder diagram zone"
```

---

### Task 4: Reclassify the anatomical diagram's traced paths

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/common/MuscleBodyArt.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/common/MuscleBodyDiagram.kt`

**Interfaces:**
- Consumes: `MuscleGroup` (Task 1), `diagramZoneIntensity` (Task 3).
- Produces: `FRONT_BODY_PATHS`/`BACK_BODY_PATHS` unchanged in shape (`List<MusclePathSpec>`), only `muscleGroup` values on the previously `BACK`/`SHOULDERS`/`CORE`-tagged entries change.

Reclassification rules, applied to every path in `FRONT_BODY_PATHS` and `BACK_BODY_PATHS` that was `MuscleGroup.CORE`, `MuscleGroup.BACK`, or `MuscleGroup.SHOULDERS` (every other tagged path — `CHEST`, `BICEPS`, `TRICEPS`, `FOREARMS`, `QUADS`, `HAMSTRINGS`, `GLUTES`, `CALVES` — is untouched, a 1:1 rename since those enum names didn't change):

- **`CORE` (24 paths, all in `FRONT_BODY_PATHS`)** → split by the shape's horizontal position relative to the figure's midline (`x` center ≈ 282, front viewbox spans 44–521). Shapes whose leading move-to `x` falls within roughly the central third (255–310) are the rectus-abdominis "six-pack" segments → `ABS`. Shapes further out to either side are the waist/oblique shapes → `OBLIQUES`. This yields 7 `ABS` paths (the central segments) and 17 `OBLIQUES` paths (the lateral waist shapes) — consistent with real abdominal anatomy having a narrow central rectus and much more lateral oblique area.
- **`BACK` (8 paths, all in `BACK_BODY_PATHS`)** → split by vertical position. The two long, large-extent paths (lines starting `M1065,395` and `M879,395`) are the full lat "wings" that run diagonally down the torso side → `LATS`. The four smaller, higher shapes (`M956,267`, `M1032,267`, `M915,299`, `M1073,299`, near the shoulder-blade level) are upper back/trap-adjacent → `UPPER_BACK`. The two tiny detail/highlight shapes (`M1069,372`, `M923,373`) sit within the lat region and are shading detail on the lat shapes → `LATS`. No path in the traced art falls low enough on the figure to represent true lower-back/lumbar anatomy, so `LOWER_BACK` gets no diagram shapes (documented as a Global Constraint, not a bug).
- **`SHOULDERS` (4 paths — 2 front, 2 back)** → per the user's explicit choice, these stay a combined visual zone rather than truly splitting. Tag the 2 front-view shapes (`M421,268`, `M139,269`) `FRONT_DELTS` and the 2 back-view shapes (`M1137,269`, `M852,269`) `REAR_DELTS` (matching which view they're visible from) — the specific enum value stored no longer matters for rendering once Step 2 wires in `diagramZoneIntensity`, which treats any of the three delt groups identically.

- [ ] **Step 1: Replace the classified entries in `MuscleBodyArt.kt`**

Apply these exact substitutions (path data strings are unchanged — only the `MuscleGroup` argument on each line changes):

In `FRONT_BODY_PATHS`:
- Lines with path data `M282,341...`, `M261,453...`, `M297,453...`, `M256,507...`, `M300,507...`, `M267,408...`, `M293,408...` (7 paths): change `MuscleGroup.CORE` → `MuscleGroup.ABS`
- Lines with path data `M191,520...`, `M373,519...`, `M344,448...`, `M220,450...`, `M195,447...`, `M361,423...`, `M198,423...`, `M190,492...`, `M368,447...`, `M345,542...`, `M374,491...`, `M220,542...`, `M193,470...`, `M369,470...`, `M225,415...`, `M333,415...`, `M370,560...` (17 paths): change `MuscleGroup.CORE` → `MuscleGroup.OBLIQUES`
- Line with path data `M421,268...`: change `MuscleGroup.SHOULDERS` → `MuscleGroup.FRONT_DELTS`
- Line with path data `M139,269...`: change `MuscleGroup.SHOULDERS` → `MuscleGroup.FRONT_DELTS`

In `BACK_BODY_PATHS`:
- Lines with path data `M1065,395...`, `M879,395...`, `M1069,372...`, `M923,373...` (4 paths): change `MuscleGroup.BACK` → `MuscleGroup.LATS`
- Lines with path data `M956,267...`, `M1032,267...`, `M915,299...`, `M1073,299...` (4 paths): change `MuscleGroup.BACK` → `MuscleGroup.UPPER_BACK`
- Line with path data `M1137,269...`: change `MuscleGroup.SHOULDERS` → `MuscleGroup.REAR_DELTS`
- Line with path data `M852,269...`: change `MuscleGroup.SHOULDERS` → `MuscleGroup.REAR_DELTS`

Use exact-string find/replace per line (the path data strings are unique per line, so matching on `"<path-data>", MuscleGroup.CORE` / `MuscleGroup.BACK` / `MuscleGroup.SHOULDERS` and replacing just the enum reference is unambiguous) — do not touch `lightness` or `isOutline` values, and do not touch any path not listed above.

- [ ] **Step 2: Wire `diagramZoneIntensity` into `MuscleBodyDiagram.kt`**

Change the import block to add:

```kotlin
import com.lsing.timego.domain.diagramZoneIntensity
```

Change `colorFor`:

```kotlin
    fun colorFor(shape: BuiltMuscleShape): Color = when {
        shape.isOutline -> outlineColor
        shape.muscleGroup != null -> {
            val intensity = diagramZoneIntensity(shape.muscleGroup, intensities)
            hexToColor(recolorByLightness(heatColor(intensity), shape.lightness))
        }
        else -> detailColor
    }
```

(This replaces the previous direct `intensities[shape.muscleGroup.name] ?: 0f` lookup.)

- [ ] **Step 3: Compile to confirm the whole project builds again**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo" ; .\gradlew.bat compileDebugKotlin`
Expected: PASS — no more unresolved-reference errors anywhere.

- [ ] **Step 4: Run the full unit test suite**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo" ; .\gradlew.bat testDebugUnitTest`
Expected: PASS — all existing domain tests (`MuscleBalanceTest`, `MuscleDistributionTest`, `MuscleHeatColorTest`, `PathVerticesTest`, `DiagramMuscleZoneTest`, etc.) green. None of these tests reference specific `MuscleGroup` constant names in a way the rename would break (confirm by reading failures if any appear — if a test does hardcode `MuscleGroup.BACK`/`SHOULDERS`/`CORE`, update it to use one of the new equivalent constants, matching the same semantic the test was checking).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/common/MuscleBodyArt.kt app/src/main/java/com/lsing/timego/ui/common/MuscleBodyDiagram.kt
git commit -m "Reclassify anatomical diagram paths to fine-grained muscle groups"
```

---

### Task 5: On-device verification

**Files:** none (verification only)

**Interfaces:** none — this task validates Tasks 1–4 together on the real device.

- [ ] **Step 1: Full assemble + install**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo" ; .\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Wipe app data for a clean re-seed**

Run: `adb shell pm clear com.lsing.timego`
Expected: `Success`

- [ ] **Step 3: Install and launch**

Run: `adb install -r app\build\outputs\apk\debug\app-debug.apk`
Then launch TimeGo on the device (or `adb shell monkey -p com.lsing.timego -c android.intent.category.LAUNCHER 1`).

- [ ] **Step 4: Screenshot and verify the exercise library**

Open the Log screen's "Add exercise" search/library view. Screenshot via `adb shell screencap -p //sdcard/timego_verify_1.png` then `adb pull //sdcard/timego_verify_1.png`. Confirm:
- Exercises are grouped/labeled with the new muscle-group names (e.g. "Lats", "Front Delts", "Obliques" appear, not "Back"/"Shoulders"/"Core").
- Spot-check ~10 exercises against the Task 2 mapping table (e.g. Pull-Up shows Lats/Upper Back/Biceps; Face Pull shows Rear Delts/Upper Back; Sit-Up shows Abs; Weighted Russian Twist shows Obliques).

- [ ] **Step 5: Screenshot and verify the custom-exercise dialog**

Open "Add custom exercise". Screenshot. Confirm the muscle-group checkbox list shows all 17 groups, correctly labeled, and remains usable (scrolls/renders without clipping).

- [ ] **Step 6: Screenshot and verify the Progress screen's radar chart and nudge banner**

Log a few sets across different exercises (e.g. one chest, one lats, one quads exercise) so there's real volume data. Open Progress screen. Screenshot the "Muscle Distribution" radar chart — confirm it renders 17 spokes without visual breakage (labels may be dense; that's the accepted trade-off from the design). Open Routines screen and confirm the untrained-muscle-group nudge banner lists fine-grained group names.

- [ ] **Step 7: Screenshot and verify the anatomical diagram**

On the Progress screen, scroll to the muscle-heatmap diagram. Screenshot. Confirm:
- No spiky/broken path rendering (the class of bug hit in the prior session's `PathVertices` work — if this reappears here it means Step 1 of Task 4 introduced a typo in a path string, which it shouldn't have since path data was never touched, only the enum argument).
- Chest, lats, upper-back, abs, obliques, quads, hamstrings, glutes, calves, biceps, triceps, forearms zones each light up independently based on logged volume.
- The shoulder region (front + back view) lights up as one combined zone reflecting any of front/side/rear delt volume logged (per Task 3's averaging).

- [ ] **Step 8: Report findings to the user**

Show the user the screenshots from Steps 4–7 before considering this task complete, per the established screenshot-before-asking discipline. If any exercise's re-tagging looks wrong on spot-check, note it for a follow-up correction rather than silently fixing it.
