# Program Templates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a curated Program Templates layer (6 static split templates, including a 3-tier
Calisthenics Progression) that extends the existing landing-page recommender to day-type granularity
and lets a user start a session pre-populated with that day-type's suggested exercises.

**Architecture:** Programs are a static in-memory Kotlin registry (no new Room table). A new
`recommendProgramDayType` function reuses the existing recovery/staleness scoring from
`MuscleBalance.kt` at day-type granularity. Slot-to-exercise resolution reuses the existing
`suggestedExerciseFor` for most slots, and reuses the existing `ProgressionRecommender` +
`BiomechanicalRegistry` ladder lookup for Advanced-calisthenics straight-arm slots. `SettingsRepository`
gains two new DataStore preferences (`activeProgramId`, `calisthenicsTier`), following the exact
pattern `trainingLean` already uses.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room, Preferences DataStore, JUnit4 (domain-layer
unit tests only — no ViewModel/UI/DataStore automated tests, matching existing project convention).

**Spec:** `docs/superpowers/specs/2026-09-15-timego-program-templates-design.md`

## Global Constraints

- No new Room table/migration — Program content is static Kotlin data, not user data.
- No periodization (GZCLP/5-3-1/Westside/PHUL-PHAT rep-intent) — out of scope per spec.
- `calisthenicsTier` is a manual picker in v1 — do NOT wire it to the `development/timego-public-foundation`
  branch's onboarding `experience` field; that field does not exist on `master`.
- When no Program is active, landing-page behavior must be pixel-for-pixel unchanged from today
  (regression-safety requirement from the spec).
- Follow existing project convention: domain-layer logic gets JUnit tests; ViewModel/Compose/DataStore
  changes are verified manually (build + on-device), not unit-tested.
- Commit after each task.

---

## Task 1: Program domain model

**Files:**
- Create: `app/src/main/java/com/lsing/timego/domain/programs/Program.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/programs/ProgramRegistryTest.kt`

**Interfaces:**
- Produces: `ProgramSlot(label: String, targetGroups: Set<String>, movementPattern: MovementPattern? = null)`,
  `ProgramDayType(name: String, regionGroups: Set<String>, slots: List<ProgramSlot>)`,
  `Program(id: String, name: String, dayTypes: List<ProgramDayType>)` — consumed by Task 2 (registry
  content), Task 3 (recommender), Task 4 (slot resolver).

- [ ] **Step 1: Write the data classes**

```kotlin
package com.lsing.timego.domain.programs

import com.lsing.timego.domain.ai.MovementPattern

/** One exercise slot within a [ProgramDayType]. [targetGroups] uses the same MuscleGroup-name
 *  convention as [com.lsing.timego.domain.suggestedExerciseFor]'s targetGroups parameter.
 *  [movementPattern] is set only on Advanced-calisthenics straight-arm slots, where resolution
 *  routes through [com.lsing.timego.domain.ai.BiomechanicalRegistry] instead of the default
 *  least-used-exercise path -- see ProgramSlotResolver. */
data class ProgramSlot(
    val label: String,
    val targetGroups: Set<String>,
    val movementPattern: MovementPattern? = null,
)

/** A named training day within a [Program] (e.g. "Push", "Beginner Full Body"). [regionGroups] is
 *  the union of every slot's target muscle groups, used by [com.lsing.timego.domain.recommendProgramDayType]
 *  to score this day-type the same way a single region is scored today. */
data class ProgramDayType(
    val name: String,
    val regionGroups: Set<String>,
    val slots: List<ProgramSlot>,
)

/** A curated, static split template. [id] is a stable string key (e.g. "ppl") used for
 *  [com.lsing.timego.data.SettingsRepository.activeProgramId] persistence -- never a Room row id. */
data class Program(
    val id: String,
    val name: String,
    val dayTypes: List<ProgramDayType>,
)
```

- [ ] **Step 2: Write a registry-integrity test (fails until Task 2 populates the registry)**

```kotlin
package com.lsing.timego.domain.programs

import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramRegistryTest {
    @Test
    fun `every program has at least one day-type`() {
        ProgramRegistry.ALL.forEach { program ->
            assertTrue("${program.id} has no day-types", program.dayTypes.isNotEmpty())
        }
    }

    @Test
    fun `every day-type has at least one slot and a non-empty region set`() {
        ProgramRegistry.ALL.forEach { program ->
            program.dayTypes.forEach { dayType ->
                assertTrue("${program.id}/${dayType.name} has no slots", dayType.slots.isNotEmpty())
                assertTrue("${program.id}/${dayType.name} has no region groups", dayType.regionGroups.isNotEmpty())
            }
        }
    }

    @Test
    fun `every slot's target groups are a subset of its day-type's region groups`() {
        ProgramRegistry.ALL.forEach { program ->
            program.dayTypes.forEach { dayType ->
                dayType.slots.forEach { slot ->
                    assertTrue(
                        "${program.id}/${dayType.name}/${slot.label} targets groups outside its day-type",
                        dayType.regionGroups.containsAll(slot.targetGroups),
                    )
                }
            }
        }
    }

    @Test
    fun `program ids are unique`() {
        val ids = ProgramRegistry.ALL.map { it.id }
        assertTrue(ids.size == ids.toSet().size)
    }
}
```

- [ ] **Step 3: Run the test to verify it fails (ProgramRegistry doesn't exist yet)**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.programs.ProgramRegistryTest"`
Expected: FAIL — compile error, `ProgramRegistry` unresolved. This confirms Task 2 is needed next.

- [ ] **Step 4: Commit the data model**

```bash
git add app/src/main/java/com/lsing/timego/domain/programs/Program.kt app/src/test/java/com/lsing/timego/domain/programs/ProgramRegistryTest.kt
git commit -m "feat(programs): add Program/ProgramDayType/ProgramSlot data model"
```

---

## Task 2: Program registry content (v1 roster)

**Files:**
- Create: `app/src/main/java/com/lsing/timego/domain/programs/ProgramRegistry.kt`

**Interfaces:**
- Consumes: `Program`, `ProgramDayType`, `ProgramSlot` (Task 1), `MuscleGroup` (`data/MuscleGroup.kt`),
  `MovementPattern` (`domain/ai/MovementPattern.kt`).
- Produces: `ProgramRegistry.ALL: List<Program>` — consumed by RoutinesScreen's picker (Task 6) and
  LogViewModel's day-type recommendation call (Task 7).

- [ ] **Step 1: Write the registry**

```kotlin
package com.lsing.timego.domain.programs

import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.domain.ai.MovementPattern

private fun g(vararg groups: MuscleGroup): Set<String> = groups.map { it.name }.toSet()

private val PUSH_REGIONS = g(MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.TRICEPS)
private val PULL_REGIONS = g(
    MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.LOWER_BACK, MuscleGroup.TRAPS,
    MuscleGroup.BICEPS, MuscleGroup.FOREARMS,
)
private val LEGS_REGIONS = g(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES, MuscleGroup.ADDUCTORS)
private val CORE_REGIONS = g(MuscleGroup.ABS, MuscleGroup.OBLIQUES)
private val SHOULDERS_ARMS_REGIONS = g(
    MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS,
    MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS,
)
private val UPPER_REGIONS = PUSH_REGIONS + PULL_REGIONS + g(MuscleGroup.REAR_DELTS)
private val ALL_REGIONS = MuscleGroup.entries.filterNot { it == MuscleGroup.FULL_BODY }.map { it.name }.toSet()

private val fullBody = Program(
    id = "full_body",
    name = "Full Body",
    dayTypes = listOf(
        ProgramDayType(
            name = "Full Body",
            regionGroups = ALL_REGIONS,
            slots = listOf(
                ProgramSlot("Squat pattern", LEGS_REGIONS),
                ProgramSlot("Push pattern", PUSH_REGIONS),
                ProgramSlot("Pull pattern", PULL_REGIONS),
                ProgramSlot("Core", CORE_REGIONS),
            ),
        ),
    ),
)

private val ppl = Program(
    id = "ppl",
    name = "Push / Pull / Legs",
    dayTypes = listOf(
        ProgramDayType(
            name = "Push",
            regionGroups = PUSH_REGIONS,
            slots = listOf(
                ProgramSlot("Main press", g(MuscleGroup.CHEST)),
                ProgramSlot("Overhead press", g(MuscleGroup.FRONT_DELTS)),
                ProgramSlot("Lateral raise", g(MuscleGroup.SIDE_DELTS)),
                ProgramSlot("Triceps isolation", g(MuscleGroup.TRICEPS)),
            ),
        ),
        ProgramDayType(
            name = "Pull",
            regionGroups = PULL_REGIONS,
            slots = listOf(
                ProgramSlot("Main pull", g(MuscleGroup.LATS)),
                ProgramSlot("Row", g(MuscleGroup.UPPER_BACK)),
                ProgramSlot("Rear delt / face pull", g(MuscleGroup.TRAPS)),
                ProgramSlot("Biceps isolation", g(MuscleGroup.BICEPS)),
            ),
        ),
        ProgramDayType(
            name = "Legs",
            regionGroups = LEGS_REGIONS,
            slots = listOf(
                ProgramSlot("Main squat/hinge", g(MuscleGroup.QUADS)),
                ProgramSlot("Posterior chain", g(MuscleGroup.HAMSTRINGS)),
                ProgramSlot("Glute isolation", g(MuscleGroup.GLUTES)),
                ProgramSlot("Calf raise", g(MuscleGroup.CALVES)),
            ),
        ),
    ),
)

private val upperLower = Program(
    id = "upper_lower",
    name = "Upper / Lower",
    dayTypes = listOf(
        ProgramDayType(
            name = "Upper",
            regionGroups = UPPER_REGIONS,
            slots = listOf(
                ProgramSlot("Press", g(MuscleGroup.CHEST)),
                ProgramSlot("Row", g(MuscleGroup.UPPER_BACK)),
                ProgramSlot("Overhead press", g(MuscleGroup.FRONT_DELTS)),
                ProgramSlot("Vertical pull", g(MuscleGroup.LATS)),
            ),
        ),
        ProgramDayType(
            name = "Lower",
            regionGroups = LEGS_REGIONS + CORE_REGIONS,
            slots = listOf(
                ProgramSlot("Squat", g(MuscleGroup.QUADS)),
                ProgramSlot("Hinge", g(MuscleGroup.HAMSTRINGS)),
                ProgramSlot("Unilateral", g(MuscleGroup.GLUTES)),
                ProgramSlot("Core", CORE_REGIONS),
            ),
        ),
    ),
)

private val broSplit = Program(
    id = "bro_split",
    name = "Bro Split",
    dayTypes = listOf(
        ProgramDayType("Chest", g(MuscleGroup.CHEST), listOf(ProgramSlot("Chest", g(MuscleGroup.CHEST)))),
        ProgramDayType(
            "Back",
            g(MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.LOWER_BACK, MuscleGroup.TRAPS),
            listOf(ProgramSlot("Back", g(MuscleGroup.LATS, MuscleGroup.UPPER_BACK))),
        ),
        ProgramDayType(
            "Shoulders",
            g(MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS),
            listOf(ProgramSlot("Shoulders", g(MuscleGroup.SIDE_DELTS))),
        ),
        ProgramDayType(
            "Arms",
            g(MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS),
            listOf(ProgramSlot("Biceps", g(MuscleGroup.BICEPS)), ProgramSlot("Triceps", g(MuscleGroup.TRICEPS))),
        ),
        ProgramDayType("Legs", LEGS_REGIONS, listOf(ProgramSlot("Legs", g(MuscleGroup.QUADS)))),
    ),
)

private val arnold = Program(
    id = "arnold",
    name = "Arnold Split",
    dayTypes = listOf(
        ProgramDayType(
            "Chest & Back",
            g(MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.UPPER_BACK),
            listOf(ProgramSlot("Chest", g(MuscleGroup.CHEST)), ProgramSlot("Back", g(MuscleGroup.LATS))),
        ),
        ProgramDayType(
            "Shoulders & Arms",
            SHOULDERS_ARMS_REGIONS,
            listOf(ProgramSlot("Shoulders", g(MuscleGroup.SIDE_DELTS)), ProgramSlot("Arms", g(MuscleGroup.BICEPS, MuscleGroup.TRICEPS))),
        ),
        ProgramDayType("Legs", LEGS_REGIONS, listOf(ProgramSlot("Legs", g(MuscleGroup.QUADS)))),
    ),
)

private val calisthenicsProgression = Program(
    id = "calisthenics_progression",
    name = "Calisthenics Progression",
    dayTypes = listOf(
        // Beginner tier
        ProgramDayType(
            name = "Beginner Full Body",
            regionGroups = ALL_REGIONS,
            slots = listOf(
                ProgramSlot("Push", PUSH_REGIONS),
                ProgramSlot("Pull", PULL_REGIONS),
                ProgramSlot("Squat", LEGS_REGIONS),
                ProgramSlot("Core", CORE_REGIONS),
            ),
        ),
        // Intermediate tier
        ProgramDayType("Intermediate Push", PUSH_REGIONS, listOf(ProgramSlot("Push", PUSH_REGIONS))),
        ProgramDayType("Intermediate Pull", PULL_REGIONS, listOf(ProgramSlot("Pull", PULL_REGIONS))),
        ProgramDayType("Intermediate Legs", LEGS_REGIONS, listOf(ProgramSlot("Legs", LEGS_REGIONS))),
        // Advanced tier -- straight-arm slots route through BiomechanicalRegistry (Task 4)
        ProgramDayType(
            name = "Advanced Bent-Arm",
            regionGroups = PULL_REGIONS + PUSH_REGIONS,
            slots = listOf(
                ProgramSlot("Pull-up/row work", PULL_REGIONS, movementPattern = MovementPattern.VERTICAL_PULL),
                ProgramSlot("Dip work", PUSH_REGIONS, movementPattern = MovementPattern.HORIZONTAL_PUSH),
            ),
        ),
        ProgramDayType(
            name = "Advanced Straight-Arm",
            regionGroups = PULL_REGIONS + g(MuscleGroup.FRONT_DELTS),
            slots = listOf(
                ProgramSlot("Lever/skill work", PULL_REGIONS, movementPattern = MovementPattern.HORIZONTAL_PULL),
                ProgramSlot("Handstand/press work", g(MuscleGroup.FRONT_DELTS), movementPattern = MovementPattern.VERTICAL_PUSH),
            ),
        ),
        ProgramDayType("Advanced Legs & Mobility", LEGS_REGIONS, listOf(ProgramSlot("Legs", LEGS_REGIONS, movementPattern = MovementPattern.KNEE_DOMINANT))),
    ),
)

object ProgramRegistry {
    val ALL: List<Program> = listOf(fullBody, ppl, upperLower, broSplit, arnold, calisthenicsProgression)

    fun byId(id: String?): Program? = ALL.firstOrNull { it.id == id }
}
```

- [ ] **Step 2: Run the Task-1 test, now against real content**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.programs.ProgramRegistryTest"`
Expected: PASS (all 4 assertions)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/programs/ProgramRegistry.kt
git commit -m "feat(programs): add v1 program registry (Full Body, PPL, Upper/Lower, Bro Split, Arnold, Calisthenics Progression)"
```

---

## Task 3: Day-type-level recommendation

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/MuscleBalance.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/MuscleBalanceTest.kt`

**Interfaces:**
- Consumes: `ProgramDayType` (Task 1), existing `rankUntrainedMuscleGroups` (same file).
- Produces: `recommendProgramDayType(dayTypes: List<ProgramDayType>, lastTrainedByGroup: Map<String, LocalDate>, today: LocalDate, lastWorkedByGroup: Map<String, LocalDate> = lastTrainedByGroup): ProgramDayType?`
  — consumed by Task 7 (`LogViewModel.refreshLandingSummary`).

- [ ] **Step 1: Write the failing tests**

```kotlin
// append to MuscleBalanceTest.kt
import com.lsing.timego.domain.programs.ProgramDayType
import com.lsing.timego.domain.programs.ProgramSlot

class MuscleBalanceDayTypeTest {
    private val push = ProgramDayType("Push", setOf("CHEST", "TRICEPS"), listOf(ProgramSlot("Chest", setOf("CHEST"))))
    private val pull = ProgramDayType("Pull", setOf("LATS", "BICEPS"), listOf(ProgramSlot("Back", setOf("LATS"))))
    private val legs = ProgramDayType("Legs", setOf("QUADS"), listOf(ProgramSlot("Legs", setOf("QUADS"))))

    @Test
    fun `never-trained day-type ranks first`() {
        val lastTrained = mapOf("CHEST" to LocalDate.of(2026, 9, 1), "TRICEPS" to LocalDate.of(2026, 9, 1))
        val result = recommendProgramDayType(listOf(push, pull, legs), lastTrained, LocalDate.of(2026, 9, 5))
        assertEquals(pull, result)
    }

    @Test
    fun `48-hour recovery guardrail demotes a day-type trained yesterday`() {
        val today = LocalDate.of(2026, 9, 5)
        val lastTrained = mapOf(
            "CHEST" to today.minusDays(1), "TRICEPS" to today.minusDays(1),
            "LATS" to today.minusDays(10), "BICEPS" to today.minusDays(10),
            "QUADS" to today.minusDays(3),
        )
        val result = recommendProgramDayType(listOf(push, pull, legs), lastTrained, today)
        assertEquals(pull, result)
    }

    @Test
    fun `single day-type program always returns that day-type`() {
        val fullBody = ProgramDayType("Full Body", setOf("CHEST"), listOf(ProgramSlot("Chest", setOf("CHEST"))))
        val result = recommendProgramDayType(listOf(fullBody), emptyMap(), LocalDate.of(2026, 9, 5))
        assertEquals(fullBody, result)
    }

    @Test
    fun `empty day-type list returns null`() {
        assertEquals(null, recommendProgramDayType(emptyList(), emptyMap(), LocalDate.of(2026, 9, 5)))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.MuscleBalanceDayTypeTest"`
Expected: FAIL — `recommendProgramDayType` unresolved.

- [ ] **Step 3: Implement `recommendProgramDayType` in `MuscleBalance.kt`**

Add near `recommendSynergisticMuscleGroups`:

```kotlin
import com.lsing.timego.domain.programs.ProgramDayType

/** Day-type analogue of [recommendSynergisticMuscleGroups]: scores each candidate day-type by its
 *  most-stale-yet-recovered region (same [rankUntrainedMuscleGroups] logic, applied per day-type's
 *  region set rather than per single region). Does not duplicate the underlying scoring -- a
 *  day-type's staleness is its most-urgent member region's staleness, so the 48-hour recovery
 *  guardrail applies transitively. */
fun recommendProgramDayType(
    dayTypes: List<ProgramDayType>,
    lastTrainedByGroup: Map<String, LocalDate>,
    today: LocalDate,
    lastWorkedByGroup: Map<String, LocalDate> = lastTrainedByGroup,
): ProgramDayType? {
    if (dayTypes.isEmpty()) return null
    if (dayTypes.size == 1) return dayTypes.first()
    val allGroups = dayTypes.flatMap { it.regionGroups }.distinct()
    val ranked = rankUntrainedMuscleGroups(allGroups, lastWorkedByGroup, today)
    val mostUrgent = ranked.firstOrNull() ?: return dayTypes.first()
    return dayTypes.firstOrNull { mostUrgent in it.regionGroups } ?: dayTypes.first()
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.MuscleBalanceDayTypeTest"`
Expected: PASS

- [ ] **Step 5: Run the full existing `MuscleBalanceTest` suite to confirm no regression**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.MuscleBalanceTest"`
Expected: PASS (all pre-existing tests unaffected)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/MuscleBalance.kt app/src/test/java/com/lsing/timego/domain/MuscleBalanceTest.kt
git commit -m "feat(programs): add day-type-level recommendation scoring"
```

---

## Task 4: Slot-to-exercise resolution

**Files:**
- Create: `app/src/main/java/com/lsing/timego/domain/programs/ProgramSlotResolver.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/programs/ProgramSlotResolverTest.kt`

**Interfaces:**
- Consumes: `ProgramSlot` (Task 1), existing `suggestedExerciseFor` (`domain/SuggestedExercise.kt`),
  existing `ProgressionRecommender.evaluateProgression` (`domain/ai/ProgressionRecommender.kt`),
  existing `BiomechanicalRegistry.getProfile` (`domain/ai/BiomechanicalRegistry.kt`).
- Produces: `resolveSlot(slot: ProgramSlot, exercises: List<Exercise>, lean: TrainingLean, usageCounts: Map<Long, Int>, recentLogsByExerciseId: Map<Long, List<SetPerformance>>): Exercise?`
  — consumed by Task 7 (`LogViewModel`).

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.lsing.timego.domain.programs

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.TrainingLean
import com.lsing.timego.domain.SetPerformance
import com.lsing.timego.domain.ai.MovementPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgramSlotResolverTest {
    private val benchPress = Exercise(id = 1, name = "Bench Press", muscleGroups = listOf("CHEST"), isCustom = false)
    private val pushUp = Exercise(
        id = 2, name = "Push-Up", muscleGroups = listOf("CHEST"), isCustom = false,
        category = ExerciseCategory.CALISTHENICS.name, catalogueKey = "timego.seed.v1.push-up",
    )
    private val exercises = listOf(benchPress, pushUp)

    @Test
    fun `default slot (no movementPattern) resolves via suggestedExerciseFor`() {
        val slot = ProgramSlot("Chest", setOf("CHEST"))
        val result = resolveSlot(slot, exercises, TrainingLean.STRENGTH, emptyMap(), emptyMap())
        assertEquals(benchPress, result)
    }

    @Test
    fun `slot with no matching exercise resolves to null`() {
        val slot = ProgramSlot("Traps", setOf("TRAPS"))
        val result = resolveSlot(slot, exercises, TrainingLean.BALANCED, emptyMap(), emptyMap())
        assertNull(result)
    }

    @Test
    fun `movementPattern slot with no logged history falls back to suggestedExerciseFor`() {
        val slot = ProgramSlot("Push skill", setOf("CHEST"), movementPattern = MovementPattern.HORIZONTAL_PUSH)
        val result = resolveSlot(slot, exercises, TrainingLean.CALISTHENICS, emptyMap(), emptyMap())
        assertEquals(pushUp, result)
    }

    @Test
    fun `movementPattern slot with mastered history resolves to the next ladder progression`() {
        val slot = ProgramSlot("Push skill", setOf("CHEST"), movementPattern = MovementPattern.HORIZONTAL_PUSH)
        // Push-Up's BiomechanicalRegistry profile: masteryRepCeiling = 20, nextProgressionKey = "Decline Push-Up"
        val masteredHistory = mapOf(2L to listOf(SetPerformance(weightKg = 0.0, reps = 20, targetReps = 15, rpe = 7)))
        val result = resolveSlot(slot, exercises, TrainingLean.CALISTHENICS, emptyMap(), masteredHistory)
        // No "Decline Push-Up" in the local exercises list, so this falls back to suggestedExerciseFor
        // rather than returning an exercise that doesn't exist in the catalogue passed in.
        assertEquals(pushUp, result)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.programs.ProgramSlotResolverTest"`
Expected: FAIL — `resolveSlot` unresolved.

- [ ] **Step 3: Implement `resolveSlot`**

```kotlin
package com.lsing.timego.domain.programs

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.TrainingLean
import com.lsing.timego.domain.SetPerformance
import com.lsing.timego.domain.ai.ProgressionRecommender
import com.lsing.timego.domain.suggestedExerciseFor

/** Resolves one [ProgramSlot] to a specific [Exercise], given the current [exercises] catalogue,
 *  [lean] preference, [usageCounts] and [recentLogsByExerciseId] (recent history per exercise, used
 *  only for movementPattern-tagged slots). Default slots (movementPattern == null) always go
 *  through [suggestedExerciseFor], identical to the existing landing-page single-exercise path --
 *  this is a regression-safety requirement, not an implementation shortcut.
 *
 *  movementPattern-tagged slots (Advanced calisthenics only) attempt a ladder-progression lookup via
 *  [ProgressionRecommender] against whichever matching exercise has the most recent logged history;
 *  if that recommender has no history to evaluate, or the recommended catalogueKey isn't present in
 *  [exercises] (this app's catalogue is the source of truth, not the recommender's key space), this
 *  falls back to the same [suggestedExerciseFor] path as a default slot. */
fun resolveSlot(
    slot: ProgramSlot,
    exercises: List<Exercise>,
    lean: TrainingLean,
    usageCounts: Map<Long, Int>,
    recentLogsByExerciseId: Map<Long, List<SetPerformance>>,
    progressionRecommender: ProgressionRecommender = ProgressionRecommender(),
): Exercise? {
    val fallback = { suggestedExerciseFor(slot.targetGroups, exercises, lean, usageCounts) }
    if (slot.movementPattern == null) return fallback()

    val candidatesWithHistory = exercises.filter { exercise ->
        exercise.muscleGroups.any { it in slot.targetGroups } && recentLogsByExerciseId[exercise.id]?.isNotEmpty() == true
    }
    val mostRecentExercise = candidatesWithHistory.maxByOrNull { recentLogsByExerciseId[it.id]?.size ?: 0 }
        ?: return fallback()

    val history = recentLogsByExerciseId[mostRecentExercise.id].orEmpty()
    val recommendation = progressionRecommender.evaluateProgression(mostRecentExercise, history) ?: return fallback()

    return exercises.firstOrNull { it.catalogueKey == recommendation.recommendedCatalogueKey } ?: fallback()
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.lsing.timego.domain.programs.ProgramSlotResolverTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/programs/ProgramSlotResolver.kt app/src/test/java/com/lsing/timego/domain/programs/ProgramSlotResolverTest.kt
git commit -m "feat(programs): add slot-to-exercise resolution with BiomechanicalRegistry ladder hook"
```

---

## Task 5: Settings — active Program + calisthenics tier

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SettingsRepository.kt`

**Interfaces:**
- Produces: `enum class CalisthenicsTier { BEGINNER, INTERMEDIATE, ADVANCED }`,
  `SettingsRepository.activeProgramId: Flow<String?>`, `SettingsRepository.calisthenicsTier: Flow<CalisthenicsTier>`,
  `suspend fun setActiveProgramId(id: String?)`, `suspend fun setCalisthenicsTier(tier: CalisthenicsTier)`
  — consumed by Task 6 (RoutinesViewModel/Screen) and Task 7 (LogViewModel).

- [ ] **Step 1: Add the enum and DataStore fields**

```kotlin
// SettingsRepository.kt -- add alongside the existing TrainingLean enum
enum class CalisthenicsTier { BEGINNER, INTERMEDIATE, ADVANCED }
```

Add to the `SettingsRepository` class body, following the exact `trainingLean` pattern:

```kotlin
    val activeProgramId: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[ACTIVE_PROGRAM_ID_KEY]
    }

    val calisthenicsTier: Flow<CalisthenicsTier> = context.settingsDataStore.data.map { prefs ->
        prefs[CALISTHENICS_TIER_KEY]
            ?.let { saved -> runCatching { CalisthenicsTier.valueOf(saved) }.getOrNull() }
            ?: CalisthenicsTier.BEGINNER
    }

    suspend fun setActiveProgramId(id: String?) {
        context.settingsDataStore.edit { prefs ->
            if (id == null) prefs.remove(ACTIVE_PROGRAM_ID_KEY) else prefs[ACTIVE_PROGRAM_ID_KEY] = id
        }
    }

    suspend fun setCalisthenicsTier(tier: CalisthenicsTier) {
        context.settingsDataStore.edit { prefs -> prefs[CALISTHENICS_TIER_KEY] = tier.name }
    }
```

Add to the `companion object`:

```kotlin
        private val ACTIVE_PROGRAM_ID_KEY = stringPreferencesKey("active_program_id")
        private val CALISTHENICS_TIER_KEY = stringPreferencesKey("calisthenics_tier")
```

- [ ] **Step 2: Build to confirm it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SettingsRepository.kt
git commit -m "feat(programs): add activeProgramId and calisthenicsTier settings"
```

---

## Task 6: Program + tier picker UI (Routines page)

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/routines/RoutinesViewModel.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/routines/RoutinesScreen.kt`

**Interfaces:**
- Consumes: `ProgramRegistry.ALL` (Task 2), `SettingsRepository.activeProgramId`/`calisthenicsTier`
  and their setters (Task 5).
- Produces: `RoutinesViewModel.activeProgramId: StateFlow<String?>`,
  `RoutinesViewModel.calisthenicsTier: StateFlow<CalisthenicsTier>`,
  `RoutinesViewModel.setActiveProgramId(id: String?)`, `RoutinesViewModel.setCalisthenicsTier(tier: CalisthenicsTier)`
  — consumed by Task 7 (LogViewModel reads the same `SettingsRepository` flows directly, not through
  this ViewModel; this task only wires the picker UI).

- [ ] **Step 1: Expose state in `RoutinesViewModel`**

Add near the existing `trainingLean`-adjacent state (same file already imports `SettingsRepository`):

```kotlin
    val activeProgramId: StateFlow<String?> = settingsRepository.activeProgramId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val calisthenicsTier: StateFlow<CalisthenicsTier> = settingsRepository.calisthenicsTier
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalisthenicsTier.BEGINNER)

    fun setActiveProgramId(id: String?) {
        viewModelScope.launch { settingsRepository.setActiveProgramId(id) }
    }

    fun setCalisthenicsTier(tier: CalisthenicsTier) {
        viewModelScope.launch { settingsRepository.setCalisthenicsTier(tier) }
    }
```

Add the two new imports (`CalisthenicsTier`, `com.lsing.timego.domain.programs.ProgramRegistry`) and,
if not already present in this file, `kotlinx.coroutines.flow.SharingStarted` / `stateIn`.

- [ ] **Step 2: Add the picker composable to `RoutinesScreen.kt`**

Add below the existing `TrainingLean` segmented row (same section, same `SurfaceCard`/`SectionHeader`
pattern already used there):

```kotlin
@Composable
private fun ProgramPickerSection(
    activeProgramId: String?,
    calisthenicsTier: com.lsing.timego.data.CalisthenicsTier,
    onSetActiveProgramId: (String?) -> Unit,
    onSetCalisthenicsTier: (com.lsing.timego.data.CalisthenicsTier) -> Unit,
) {
    Column {
        SectionHeader(title = "Program")
        FlowRow {
            FilterChip(
                selected = activeProgramId == null,
                onClick = { onSetActiveProgramId(null) },
                label = { Text("None") },
            )
            com.lsing.timego.domain.programs.ProgramRegistry.ALL.forEach { program ->
                FilterChip(
                    selected = activeProgramId == program.id,
                    onClick = { onSetActiveProgramId(program.id) },
                    label = { Text(program.name) },
                )
            }
        }
        if (activeProgramId == "calisthenics_progression") {
            SectionHeader(title = "Calisthenics tier")
            FlowRow {
                com.lsing.timego.data.CalisthenicsTier.entries.forEach { tier ->
                    FilterChip(
                        selected = calisthenicsTier == tier,
                        onClick = { onSetCalisthenicsTier(tier) },
                        label = { Text(formatEnumLabel(tier.name)) },
                    )
                }
            }
        }
    }
}
```

Wire it into the existing settings section alongside where `trainingLean`/`onSetTrainingLean` are
already passed down (same parent composable that reads `viewModel.trainingLean` at line 69):

```kotlin
    val activeProgramId by viewModel.activeProgramId.collectAsStateWithLifecycle()
    val calisthenicsTier by viewModel.calisthenicsTier.collectAsStateWithLifecycle()
```

and render `ProgramPickerSection(activeProgramId, calisthenicsTier, viewModel::setActiveProgramId, viewModel::setCalisthenicsTier)`
in the same settings column as the existing `TrainingLean` row.

- [ ] **Step 3: Build to confirm it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: On-device verification**

Install debug build, open Routines page, confirm: the Program chip row shows all 6 programs plus
"None"; selecting "Calisthenics Progression" reveals the tier row; selecting any other program hides
it; selections persist across app restart (DataStore-backed).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/routines/RoutinesViewModel.kt app/src/main/java/com/lsing/timego/ui/routines/RoutinesScreen.kt
git commit -m "feat(programs): add Program and calisthenics-tier picker to Routines page"
```

---

## Task 7: Landing page recommendation integration

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt`

**Interfaces:**
- Consumes: `ProgramRegistry.byId` (Task 2), `recommendProgramDayType` (Task 3), `resolveSlot` (Task 4),
  `SettingsRepository.activeProgramId`/`calisthenicsTier` (Task 5).
- Produces: `LandingSummary.programSuggestion: ProgramDayTypeSuggestion?`,
  `LogViewModel.startProgramSession(exercises: List<Exercise>)` — consumed by Task 8 (LogScreen).

- [ ] **Step 1: Add the new data class and `LandingSummary` field**

```kotlin
// near LandingSummary
data class ProgramDayTypeSuggestion(
    val dayTypeName: String,
    val exercises: List<Exercise>,
)
```

Add `val programSuggestion: ProgramDayTypeSuggestion? = null` to `LandingSummary`'s field list (keep
it defaulted so the two existing construction sites that don't set it — the initial `_landingSummary`
value and any other call — still compile unchanged).

- [ ] **Step 2: Track `activeProgramId`/`calisthenicsTier` alongside the existing `_trainingLean` field**

```kotlin
    private var activeProgramId: String? = null
    private var calisthenicsTier: CalisthenicsTier = CalisthenicsTier.BEGINNER
```

Collect them the same way `_trainingLean` is already collected elsewhere in this ViewModel's init
block (find the existing `settingsRepository.trainingLean.collect { ... }` launch and add two sibling
launches for `activeProgramId`/`calisthenicsTier`, each calling `refreshLandingSummary` again on
change — same pattern already used for `trainingLean` changes).

- [ ] **Step 3: Branch `refreshLandingSummary` on the active program**

Insert after the existing `recommendedSeeds`/`recommended` computation (right after line ~514's
`recommendedGroups`), before the `suggestionEpoch` block:

```kotlin
            val program = com.lsing.timego.domain.programs.ProgramRegistry.byId(activeProgramId)
            val candidateDayTypes = when {
                program == null -> null
                program.id == "calisthenics_progression" -> program.dayTypes.filter { dayType ->
                    when (calisthenicsTier) {
                        CalisthenicsTier.BEGINNER -> dayType.name.startsWith("Beginner")
                        CalisthenicsTier.INTERMEDIATE -> dayType.name.startsWith("Intermediate")
                        CalisthenicsTier.ADVANCED -> dayType.name.startsWith("Advanced")
                    }
                }
                else -> program.dayTypes
            }
            val recentLogsByExerciseId: Map<Long, List<com.lsing.timego.domain.SetPerformance>> = allSets
                .groupBy { it.exerciseId }
                .mapValues { (_, logs) -> logs.sortedBy { it.loggedAtEpochMillis }.takeLast(5).map {
                    com.lsing.timego.domain.SetPerformance(it.weightKg, it.reps, it.targetReps, it.rpe)
                } }
            val programSuggestion = candidateDayTypes?.let { dayTypes ->
                com.lsing.timego.domain.recommendProgramDayType(dayTypes, lastTrained, LocalDate.now(), lastWorked)
            }?.let { dayType ->
                val resolved = dayType.slots.mapNotNull { slot ->
                    com.lsing.timego.domain.programs.resolveSlot(slot, exercises, effectiveLean, usageCounts, recentLogsByExerciseId)
                }
                if (resolved.isEmpty()) null else ProgramDayTypeSuggestion(dayType.name, resolved)
            }
```

Note: `effectiveLean` is computed slightly later in the existing function body (around line 525-530);
move the `programSuggestion` computation to after that block, or hoist `effectiveLean`'s computation
above it — either is fine, but it must be computed before this block since `resolveSlot` needs it.

- [ ] **Step 4: Pass `programSuggestion` into the returned `LandingSummary`**

Add `programSuggestion = programSuggestion,` to the existing `LandingSummary(...)` construction at
the end of `refreshLandingSummary`.

- [ ] **Step 5: Add `startProgramSession`**

```kotlin
    /** Starts a freeform session (routineId = null, identical to today's freeform start) and returns
     *  the ids to pre-expand in the session UI. Does not persist anything beyond what [startSession]
     *  already persists -- the suggested exercises are session-local UI state, not written to the
     *  database until the user logs an actual set, matching every other freeform-session behavior. */
    fun startProgramSession(suggestion: ProgramDayTypeSuggestion): List<Long> {
        startSession(routineId = null)
        return suggestion.exercises.map { it.id }
    }
```

- [ ] **Step 6: Build to confirm it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Regression check — confirm behavior is unchanged with no program active**

Run the full domain test suite:

Run: `./gradlew testDebugUnitTest`
Expected: PASS (no existing test touches `LogViewModel` directly per project convention, but this
confirms nothing in the domain layer broke)

On-device: with "None" selected on the Routines page, confirm the landing card looks and behaves
exactly as before this change (no program-related row shown).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt
git commit -m "feat(programs): wire day-type recommendation into landing page summary"
```

---

## Task 8: "Start with this" landing card UI

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt`

**Interfaces:**
- Consumes: `LandingSummary.programSuggestion` (Task 7), `LogViewModel.startProgramSession` (Task 7),
  existing `toggleExpandedExerciseIds` (`domain/ExpandedExerciseRows.kt`), existing
  `expandedExerciseIds` local state (this file, line ~152).

- [ ] **Step 1: Render the program suggestion card**

Find the existing landing-card composable that renders `landingSummary.suggestedExercise` (the
"Try: X" line). Add a sibling block, shown only when `programSuggestion != null`, replacing that
single-line text with a small card:

```kotlin
landingSummary.programSuggestion?.let { suggestion ->
    SurfaceCard {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Text(suggestion.dayTypeName, style = MaterialTheme.typography.titleMedium)
            Text(
                suggestion.exercises.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = {
                val ids = viewModel.startProgramSession(suggestion)
                expandedExerciseIds = ids.take(1)
                pendingProgramSuggestionIds = ids.drop(1)
            }) {
                Text("Start with this")
            }
        }
    }
}
```

`pendingProgramSuggestionIds` is a new `rememberSaveable` local state var (`List<Long>`, default
`emptyList()`) declared next to the existing `expandedExerciseIds` declaration at line 152 — it holds
the remaining suggested exercises not auto-expanded (respecting the existing 3-row expansion cap:
only the first exercise auto-expands, matching the picker's existing single-select-expands behavior
at line 247).

- [ ] **Step 2: Surface the remaining suggestions as tappable chips during the active session**

In the active-session section (where `Quick Add` chips are already rendered), add a sibling row shown
only when `pendingProgramSuggestionIds.isNotEmpty()`:

```kotlin
if (pendingProgramSuggestionIds.isNotEmpty()) {
    FlowRow {
        pendingProgramSuggestionIds.mapNotNull { id -> displayedExercises.firstOrNull { it.id == id } }
            .forEach { exercise ->
                AssistChip(
                    onClick = {
                        expandedExerciseIds = toggleExpandedExerciseIds(expandedExerciseIds, exercise.id)
                        pendingProgramSuggestionIds = pendingProgramSuggestionIds.filterNot { it == exercise.id }
                    },
                    label = { Text(exercise.name) },
                )
            }
    }
}
```

- [ ] **Step 3: Reset `pendingProgramSuggestionIds` when the session ends**

Find the existing reset block at line ~163 (`expandedExerciseIds = emptyList()`, triggered on session
end) and add `pendingProgramSuggestionIds = emptyList()` alongside it.

- [ ] **Step 4: Build to confirm it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: On-device verification**

With a program active (e.g. PPL) and eligible logged history seeded, confirm: landing card shows
"Start with this" + day-type name + exercise list; tapping it starts a session with the first
exercise's row already expanded and the rest available as chips; tapping a chip expands that row
(respecting the existing 3-row cap — a 4th tap collapses the oldest); ending the session clears the
pending chips; the always-available freeform "Start" action is untouched and still reachable.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt
git commit -m "feat(programs): add Start-with-this landing card and suggested-exercise chips"
```

---

## Final verification

- [ ] Run the full unit test suite: `./gradlew testDebugUnitTest` — expect all tests (existing +
  new) passing.
- [ ] Run a full debug build + lint: `./gradlew assembleDebug lintDebug`.
- [ ] On-device pass through all 6 programs (including all 3 calisthenics tiers), confirming the
  day-type recommendation changes sensibly as logged history accumulates, and that "None" remains
  pixel-identical to pre-change behavior.
