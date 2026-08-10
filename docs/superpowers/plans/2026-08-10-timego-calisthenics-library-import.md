# TimeGo External Exercise Library Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Grow TimeGo's exercise library from 278 to roughly 580-680 exercises by importing and curating text/metadata (no media) from two external open-source datasets — RafaelJaime/calisthenics_exercises (CC0, 104 calisthenics exercises) and hasaneyldrm/exercises-dataset (MIT text/data, 1,324 exercises, ~325 bodyweight) — deduped against the existing library and each other, tagged with `MuscleGroup`/`category`/`loggingType`/`muscleWeights` using TimeGo's established conventions.

**Architecture:** Pure additive seed-data growth via the existing `strength()`/`calisthenics()`/`cardio()`/`warmup()` builder pattern in `SeedExercises.kt` — no schema change, no migration, no new converter or loader infrastructure. `seedMissingExercises` (name-matching, already built) picks up new entries on the user's device automatically. Curation happens in per-category batches, each independently verified by structural tests, same discipline as the prior library expansion (Project B).

**Tech Stack:** Kotlin, Room, JUnit.

## Global Constraints

- **No schema/migration changes.** `Exercise`, `Converters`, and `TimeGoDatabase` are untouched — this plan only adds rows to `SEED_EXERCISES`.
- **Text/metadata only.** No images or GIFs are pulled from either source (hasaneyldrm's media is separately © Gym visual, RafaelJaime has none) — only `name` and muscle tags inform the new entries.
- **Dedup is a curation judgment call, not runtime code.** Before adding any new exercise, check its normalized name (case/whitespace/synonym-insensitive) against the *current full contents of `SEED_EXERCISES` at that point in the file*, including entries added by earlier tasks in this same plan (tasks run sequentially against the same file). Skip anything that already exists under an equivalent name.
- **Progression tiers stay distinct.** Named progression variants (e.g. "Tuck Front Lever" / "Advanced Tuck Front Lever" / "Full Front Lever") are each their own `Exercise` entry — do not collapse them.
- **Muscle-weighting methodology** (same three-tier convention as the prior expansion): primary mover = 100 (default, no `weights` entry needed), major synergist = 60-70, minor stabilizer = 25-40. For this import's volume, weight **by movement family** — e.g. every front-lever progression tier shares the same lats/core/shoulder ratios, since recruitment pattern doesn't change across tiers, only leverage difficulty does. Don't re-derive ratios per tier from scratch.
- **loggingType classification:** default `WEIGHT_REPS`. Use `HOLD` only for genuinely isometric movements (planks, levers, flags, wall-sits, holds) — matches the six original + eight Project-B HOLD exercises already in the library. Every new `HOLD` entry must also be added to `SeedExercisesTest.holdExerciseNames` (Task 6) or the existing exact-match test will fail.
- **CARDIO/WARMUP entries stay unweighted** (`muscleWeights` empty) — same rule as the existing library; those tags exist for the untrained-muscle nudge only, not weighted volume.
- Library curation content is data, not logic — no per-exercise unit tests; verified by structural tests only (counts, no duplicates, no orphaned weights, category/loggingType invariants).
- Phone is connected — on-device verification happens in the final task, before merge.

---

### Task 1: Calisthenics import — upper-body push/pull

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SeedExercises.kt`

**Interfaces:** none new — uses the existing `calisthenics(name, vararg groups, loggingType = ..., weights = ...)` builder.

**Sources for this batch:** RafaelJaime's push/pull/dips families (e.g. planche, handstand push-up, muscle-up, row progressions) + hasaneyldrm's bodyweight-tagged push/pull exercises not already covered.

**Target:** ~60 new `calisthenics(...)` entries covering upper-body pushing (push-up/dip/planche/handstand families) and pulling (pull-up/row/front-and-back-lever families) progressions not already in the library. Check each candidate name against the current full `SEED_EXERCISES` list (278 existing entries) before adding — skip anything already present under an equivalent name.

- [ ] **Step 1: Add the ~60 new calisthenics entries to `SEED_EXERCISES`**

Add under the existing `// Calisthenics` section (or a clearly labeled sub-comment, e.g. `// Calisthenics -- Upper Body Push/Pull (imported)`), following the file's existing style. Worked examples matching the expected quality bar and weighting convention:

```kotlin
    calisthenics("Pseudo Planche Push-Up", MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS, MuscleGroup.TRICEPS, weights = mapOf(MuscleGroup.FRONT_DELTS to 65, MuscleGroup.TRICEPS to 55)),
    calisthenics("Tuck Planche", MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS, MuscleGroup.ABS, weights = mapOf(MuscleGroup.FRONT_DELTS to 60, MuscleGroup.ABS to 50), loggingType = LoggingType.HOLD),
    calisthenics("Advanced Tuck Planche", MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS, MuscleGroup.ABS, weights = mapOf(MuscleGroup.FRONT_DELTS to 60, MuscleGroup.ABS to 50), loggingType = LoggingType.HOLD),
    calisthenics("Straddle Planche", MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS, MuscleGroup.ABS, weights = mapOf(MuscleGroup.FRONT_DELTS to 60, MuscleGroup.ABS to 50), loggingType = LoggingType.HOLD),
    calisthenics("Full Planche", MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS, MuscleGroup.ABS, weights = mapOf(MuscleGroup.FRONT_DELTS to 60, MuscleGroup.ABS to 50), loggingType = LoggingType.HOLD),
    calisthenics("Archer Pull-Up", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS, weights = mapOf(MuscleGroup.UPPER_BACK to 60, MuscleGroup.BICEPS to 40)),
    calisthenics("Typewriter Pull-Up", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS, weights = mapOf(MuscleGroup.UPPER_BACK to 60, MuscleGroup.BICEPS to 40)),
    calisthenics("Skin the Cat", MuscleGroup.LATS, MuscleGroup.ABS, MuscleGroup.SIDE_DELTS, weights = mapOf(MuscleGroup.ABS to 45, MuscleGroup.SIDE_DELTS to 35)),
```

Note the front-lever/planche tier example above: all four planche tiers share the same `weights` map (movement-family reuse per the Global Constraints rule) — only the name and difficulty change. Continue this pattern for the remaining ~53 entries across push (dip/planche/handstand progressions), pull (pull-up/row/lever progressions), and combined-skill movements (muscle-up, skin-the-cat variants) not already in the library.

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SeedExercises.kt
git commit -m "Import calisthenics upper-body push/pull exercises from external datasets"
```

---

### Task 2: Calisthenics import — core, legs, full-body

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SeedExercises.kt`

**Target:** ~60 new `calisthenics(...)` entries covering core (hollow-body/dragon-flag/L-sit families), legs (pistol squat/shrimp squat/Nordic curl progressions), and full-body/skill movements (human flag tiers, back lever tiers) not already in the library. Same dedup and movement-family-weighting discipline as Task 1.

- [ ] **Step 1: Add the ~60 new calisthenics entries to `SEED_EXERCISES`**

Worked examples:

```kotlin
    calisthenics("Dragon Flag", MuscleGroup.ABS, MuscleGroup.LATS, weights = mapOf(MuscleGroup.LATS to 40), loggingType = LoggingType.HOLD),
    calisthenics("Tuck Dragon Flag", MuscleGroup.ABS, MuscleGroup.LATS, weights = mapOf(MuscleGroup.LATS to 40), loggingType = LoggingType.HOLD),
    calisthenics("Nordic Curl", MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, weights = mapOf(MuscleGroup.GLUTES to 30)),
    calisthenics("Shrimp Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, weights = mapOf(MuscleGroup.GLUTES to 60, MuscleGroup.HAMSTRINGS to 40)),
    calisthenics("Human Flag Tuck", MuscleGroup.OBLIQUES, MuscleGroup.LATS, MuscleGroup.SIDE_DELTS, weights = mapOf(MuscleGroup.LATS to 65, MuscleGroup.SIDE_DELTS to 50), loggingType = LoggingType.HOLD),
    calisthenics("Human Flag Straddle", MuscleGroup.OBLIQUES, MuscleGroup.LATS, MuscleGroup.SIDE_DELTS, weights = mapOf(MuscleGroup.LATS to 65, MuscleGroup.SIDE_DELTS to 50), loggingType = LoggingType.HOLD),
```

Continue for the remaining ~54 entries. Every genuinely isometric addition (dragon flag tiers, human flag tiers, additional lever/hold variants) gets `loggingType = LoggingType.HOLD` — do not default new isometric holds to `WEIGHT_REPS`, matching the rule already established in the logging-field-accuracy work.

- [ ] **Step 2: Update `SeedExercisesTest.holdExerciseNames`**

Add every new `HOLD`-type exercise name from Task 1 and Task 2 to the `holdExerciseNames` set in `app/src/test/java/com/lsing/timego/data/SeedExercisesTest.kt` (the existing exact-match test will otherwise fail once new HOLD exercises exist):

```kotlin
    private val holdExerciseNames = setOf(
        "Plank", "Side Plank", "Wall Sit", "L-Sit", "Dead Hang", "Superman",
        "Planche Lean", "Hollow Body Hold", "Copenhagen Plank", "Wall Handstand Hold",
        "Tuck Planche Hold", "Front Lever Hold", "Back Lever Hold", "Human Flag Hold",
        // Imported (Task 1/2) -- replace with the exact names actually added above
        "Tuck Planche", "Advanced Tuck Planche", "Straddle Planche", "Full Planche",
        "Dragon Flag", "Tuck Dragon Flag", "Human Flag Tuck", "Human Flag Straddle",
    )
```

Reconcile this list against whatever HOLD-type names were actually added in Steps 1 of Task 1 and Task 2 — the set must exactly match every `Exercise` in `SEED_EXERCISES` with `loggingType == LoggingType.HOLD.name`.

- [ ] **Step 3: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run structural tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.data.SeedExercisesTest"`
Expected: PASS (the hold-names exact-match test in particular — if it fails, the `holdExerciseNames` set from Step 2 is out of sync with the actual data).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SeedExercises.kt app/src/test/java/com/lsing/timego/data/SeedExercisesTest.kt
git commit -m "Import calisthenics core/legs/full-body exercises, update HOLD exercise test set"
```

---

### Task 3: Strength import — Chest, Back, Shoulders equipment variants

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SeedExercises.kt`

**Interfaces:** none new — uses the existing `strength(name, vararg groups, weights = ...)` builder.

**Source for this batch:** hasaneyldrm's non-bodyweight Chest/Back/Shoulders exercises not already covered (machine variants, cable angle variants, additional unilateral work).

**Target:** ~80 new `strength(...)` entries. Same dedup discipline as Tasks 1-2 — check against the full current list (now including Tasks 1-2's additions) before adding.

- [ ] **Step 1: Add the ~80 new strength entries to `SEED_EXERCISES`**

Worked examples:

```kotlin
    strength("Cable Iron Cross", MuscleGroup.CHEST, MuscleGroup.FRONT_DELTS, weights = mapOf(MuscleGroup.FRONT_DELTS to 40)),
    strength("Plate-Loaded Chest Press", MuscleGroup.CHEST, MuscleGroup.TRICEPS, weights = mapOf(MuscleGroup.TRICEPS to 60)),
    strength("Behind-the-Neck Lat Pulldown", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, weights = mapOf(MuscleGroup.UPPER_BACK to 45)),
    strength("Plate-Loaded Row Machine", MuscleGroup.LATS, MuscleGroup.UPPER_BACK, MuscleGroup.BICEPS, weights = mapOf(MuscleGroup.UPPER_BACK to 65, MuscleGroup.BICEPS to 30)),
    strength("Cable Y-Raise", MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS, weights = mapOf(MuscleGroup.REAR_DELTS to 55)),
    strength("Bradford Press", MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.TRICEPS, weights = mapOf(MuscleGroup.SIDE_DELTS to 60, MuscleGroup.TRICEPS to 45)),
```

Continue for the remaining ~74 entries across Chest, Back, and Shoulders.

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run structural tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.data.SeedExercisesTest"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SeedExercises.kt
git commit -m "Import Chest/Back/Shoulders strength exercises from external dataset"
```

---

### Task 4: Strength import — Arms, Forearms, Legs equipment variants

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SeedExercises.kt`

**Target:** ~80 new `strength(...)` entries across Arms (biceps/triceps), Forearms, and Legs (quads/hamstrings/glutes/calves) — same source and dedup discipline as Task 3.

- [ ] **Step 1: Add the ~80 new strength entries to `SEED_EXERCISES`**

Worked examples:

```kotlin
    strength("Spider Curl", MuscleGroup.BICEPS),
    strength("JM Press", MuscleGroup.TRICEPS, MuscleGroup.CHEST, weights = mapOf(MuscleGroup.CHEST to 35)),
    strength("Behind-the-Back Wrist Curl", MuscleGroup.FOREARMS),
    strength("Sissy Squat", MuscleGroup.QUADS, MuscleGroup.ABS, weights = mapOf(MuscleGroup.ABS to 30)),
    strength("Belt Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES, weights = mapOf(MuscleGroup.GLUTES to 55)),
    strength("Seated Calf Raise", MuscleGroup.CALVES),
    strength("Cossack Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, weights = mapOf(MuscleGroup.GLUTES to 55, MuscleGroup.HAMSTRINGS to 40)),
```

Continue for the remaining ~74 entries.

- [ ] **Step 2: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run structural tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.data.SeedExercisesTest"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SeedExercises.kt
git commit -m "Import Arms/Forearms/Legs strength exercises from external dataset"
```

---

### Task 5: Cardio/Warmup import and final structural test updates

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/data/SeedExercises.kt`
- Modify: `app/src/test/java/com/lsing/timego/data/SeedExercisesTest.kt`

**Target:** ~30 new `cardio(...)`/`warmup(...)` entries (machine variants, additional mobility/dynamic-stretch movements) not already in the library. These stay unweighted per the Global Constraints.

- [ ] **Step 1: Add the ~30 new cardio/warmup entries to `SEED_EXERCISES`**

Worked examples:

```kotlin
    cardio("Air Bike", MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.FULL_BODY),
    cardio("Ski Erg", MuscleGroup.LATS, MuscleGroup.ABS, MuscleGroup.FULL_BODY),
    warmup("World's Greatest Stretch", MuscleGroup.HAMSTRINGS, MuscleGroup.QUADS),
    warmup("Cat-Cow", MuscleGroup.LOWER_BACK, MuscleGroup.ABS),
```

Note: if a candidate movement targets a muscle concept not present in TimeGo's 17-value `MuscleGroup` enum (e.g. "hip flexors" — the World's Greatest Stretch example above maps it to QUADS, the closest existing group), tag it with the closest existing group instead — do not add new enum values as part of this import, that's out of scope. Continue for the remaining ~26 entries.

- [ ] **Step 2: Update the total-count structural test**

In `SeedExercisesTest.kt`, change:

```kotlin
    @Test
    fun `library has grown to roughly 300 exercises`() {
        assertEquals(true, SEED_EXERCISES.size in 250..350)
    }
```

to:

```kotlin
    @Test
    fun `library has grown to roughly 600 exercises`() {
        assertEquals(true, SEED_EXERCISES.size in 550..700)
    }
```

- [ ] **Step 3: Add the HOLD-implies-CALISTHENICS invariant test**

Append to `SeedExercisesTest.kt`:

```kotlin
    @Test
    fun `every HOLD exercise is CALISTHENICS`() {
        val holdExercises = SEED_EXERCISES.filter { it.loggingType == LoggingType.HOLD.name }
        assertEquals(true, holdExercises.all { it.category == ExerciseCategory.CALISTHENICS.name })
    }
```

- [ ] **Step 4: Build-verify**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run the full structural test suite**

Run: `./gradlew :app:testDebugUnitTest --tests "com.lsing.timego.data.SeedExercisesTest"`
Expected: PASS (all tests, including the updated count range and the new HOLD/CALISTHENICS invariant).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/data/SeedExercises.kt app/src/test/java/com/lsing/timego/data/SeedExercisesTest.kt
git commit -m "Import cardio/warmup exercises, update library size and HOLD invariant tests"
```

---

### Task 6: Full verification, on-device check, and vault update

**Files:** none (verification and documentation only)

- [ ] **Step 1: Full unit test suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all tests passing.

- [ ] **Step 2: Full debug build and install**

Run: `./gradlew installDebug`
Expected: BUILD SUCCESSFUL, "Installed on 1 device." (`seedMissingExercises` inserts every new exercise by name on next app launch — no migration involved, this is pure additive seed data).

- [ ] **Step 3: On-device verification (hand off to the user)**

Ask the user to: open Log, confirm the exercise library sections (especially Calisthenics) now show substantially more entries; search for a couple of newly-imported names (e.g. "Dragon Flag", "Shrimp Squat") to confirm they appear and log correctly (HOLD entries show a single duration field, WEIGHT_REPS entries show weight+reps); open Progress and confirm the Muscle Distribution radar chart/heatmap still renders sensibly with the larger library.

- [ ] **Step 4: Update the vault project note**

Add a session entry to `C:\Users\lsing\.claude\obsidian_demo\Projects\TimeGo - Gym Progress Tracker.md` recording: external exercise library import shipped, sources (RafaelJaime/calisthenics_exercises CC0, hasaneyldrm/exercises-dataset MIT text-only), library grown from 278 to ~[actual final count], text/metadata-only import (no media, avoiding the Gym visual license), movement-family-grounded EMG weighting methodology for the new batch. Note this is Sub-project 1 of the two-part "ML for progressive overload" follow-up — Sub-project 2 (LiftShift-inspired suggester upgrade, base layer for future ML) is next.

- [ ] **Step 5: Verify git state**

```bash
cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"
git status
git log --oneline -6
```

Expected: six commits from Tasks 1-6 visible, working tree clean.

---

## Self-Review Notes

- **Spec coverage**: Sources/Scope section → Global Constraints + Task 1-5 targets. Pipeline steps 1-2 (extract/dedup) → the "Dedup is a curation judgment call" Global Constraint, applied inline in every curation task rather than as separate code. Step 3 (progression tiers) → Global Constraints + Task 1's planche worked example. Step 4 (muscle mapping) → Global Constraints + per-task worked examples. Step 5 (category/loggingType) → Global Constraints + Task 2 Step 2 (HOLD test-set sync). Step 6 (muscleWeights, movement-family reuse) → Global Constraints + every curation task's worked examples. Step 7 (append via existing builders, no migration) → stated directly in Architecture, no Task 1-style data-model task needed (unlike the prior expansion, this one adds no new field). Verification section → Task 5 (structural tests) + Task 6 (full suite + on-device).
- **Type consistency checked**: every curation task uses the existing `strength(name, vararg groups: MuscleGroup, weights: Map<MuscleGroup, Int> = emptyMap())` and `calisthenics(name, vararg groups: MuscleGroup, loggingType: LoggingType = LoggingType.WEIGHT_REPS, weights: Map<MuscleGroup, Int> = emptyMap())` signatures already present in `SeedExercises.kt` (confirmed by reading the file before writing this plan) — no signature changes needed anywhere in this plan.
- **Curation task sizing**: Tasks 1-5 specify methodology, per-batch targets, and worked examples rather than pre-authoring all ~300-400 new exercises inline in this document — same deliberate deviation used in the prior library-expansion plan, for the same reason (content volume; actual curation happens live during execution, verified immediately after by structural tests already defined in Tasks 2 and 5). Every other task (6) follows the standard fully-specified format.
- **Placeholder scan**: no TBD/TODO markers. All worked-example code compiles against real `MuscleGroup`/`LoggingType`/`ExerciseCategory` enum values only.
- **Test-sync risk called out explicitly**: the exact-match `holdExerciseNames` test is the one place new data can silently break an existing test in a way that's easy to miss — flagged as its own step (Task 2 Step 2) with an explicit reconciliation instruction, rather than left implicit in a "run tests" step.
