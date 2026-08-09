# TimeGo — Fine-Grained Muscle Group Tagging (Design)

## Context

Deferred from Update 1.1's muscle-heatmap follow-up: `MuscleGroup` currently has 12
values (CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS, QUADS, HAMSTRINGS, GLUTES,
CALVES, CORE, FULL_BODY), too coarse for the "12-group breakdown" reference the user
wanted (splitting BACK into lats/upper/lower back, SHOULDERS into delt heads, CORE into
abs/obliques). This is the first of three sequenced frontend sessions (muscle groups →
Personal Records redesign → full visual-identity pass), chosen to go first because it's
a data-layer change the other two may want to build on.

**Beyond display**: the user wants exercises tagged with real anatomical detail so a
future on-device ML recommendation layer (mentioned in the original TimeGo brainstorm as
"revisit once real multi-month history exists") has fine-grained muscle-group signal to
work with, not just a display nicety.

## Scope

1. Expand `MuscleGroup` enum to 16 anatomical groups + `FULL_BODY`.
2. Re-tag all 119 seed exercises (`SeedExercises.kt`) to the new groups.
3. Reclassify the anatomical heatmap diagram's ~176 traced SVG paths (`MuscleBodyArt.kt`)
   to match, as far as the source art geometrically allows.
4. Room migration (version bump, re-seed — no in-place data remap; confirmed with the
   user that no real logged workout history exists yet to preserve).

Out of scope for this pass: Personal Records redesign, full visual-identity pass (both
sequenced after this), and any ML recommendation logic itself (this only prepares the
data).

## 1. Data Model

`MuscleGroup.kt` becomes:

```kotlin
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

`FULL_BODY` is retained unchanged as the catch-all tag for cardio/warmup/full-body
exercises (Burpee, Running, Jumping Jacks, etc.) — it isn't part of the anatomical split
and every existing usage of it (seed data, diagram neutral zone) stays as-is.

`Exercise.muscleGroups: List<String>` is unchanged in shape — it already supports
multiple tags per exercise, which the finer split actually makes use of (e.g. Pull-up:
`[LATS, UPPER_BACK, BICEPS]` instead of the old single `BACK`).

No other entity changes. `Converters.fromStringList/toStringList` and all Room DAOs are
untouched — this is purely new enum constant names, not a new column or type.

## 2. Exercise Re-tagging

All 119 exercises in `SeedExercises.kt` get re-tagged to the new groups, done directly
using standard exercise-science muscle mapping (e.g. Bench Press → CHEST; Pull-up →
LATS, UPPER_BACK, BICEPS; Overhead Press → FRONT_DELTS, SIDE_DELTS, TRICEPS; Face Pull →
REAR_DELTS; Sit-up → ABS; Russian Twist → OBLIQUES). After the pass, the full mapping is
shown to the user as a table for spot-checking before commit, rather than requiring a
line-by-line review of all 119 entries.

The 7 forearm exercises added during the muscle-heatmap follow-up session keep their
FOREARMS tag unchanged (already correctly granular).

## 3. Migration

Room version bump 4→5. No in-place data remap: confirmed with the user that no real
logged workout history exists on-device yet (app data was wiped at the end of the prior
session), so the migration is a genuine no-op schema bump (same pattern as the 3→4 bump)
followed by `adb shell pm clear com.lsing.timego` — `seedMissingExercises` re-seeds the
full 119-exercise library fresh under the new tags on next launch.

## 4. Downstream Consumers

**Automatic, no code change beyond the enum:**
- `AddExerciseDialog`'s custom-exercise muscle-group checkbox list (`MuscleGroup.entries.forEach`)
- The untrained-muscle-group nudge banner (`RoutinesViewModel`, `MuscleBalance.kt` domain logic)
- The Progress screen's `RadarChart` "Muscle Distribution" section

All three iterate `MuscleGroup.entries` generically today, so they pick up all 17 values
automatically. Per the user's explicit choice, these stay **fully detailed** — the radar
chart gets 17 spokes instead of 12, and the nudge banner can list any of the 17 names.
This is a UI density trade-off the user accepted knowingly, not an oversight.

**Requires reclassification — `MuscleBodyArt.kt`:**

The ~176 `MusclePathSpec` entries (front + back traced anatomy art) are individually
pre-classified path literals, not a live runtime classifier — originally assigned by a
one-off Python script using each path's bounding box. Reclassification for the new
groups, by feasibility:

| Old group | New treatment | Method |
|---|---|---|
| CHEST, BICEPS, TRICEPS, FOREARMS, QUADS, HAMSTRINGS, GLUTES, CALVES | 1:1 rename, no reclassification | none needed |
| CORE | Split into ABS vs OBLIQUES | lateral (left/right-of-center) bounding-box position |
| BACK | Split into LATS / UPPER_BACK / LOWER_BACK | vertical bounding-box position on the back-view figure |
| SHOULDERS | **Stays one combined `SHOULDERS` diagram zone** — front/side/rear delts all light up together in the diagram | not geometrically separable in the traced art (see below) |

**Why shoulders don't split in the diagram**: the front-view figure's deltoid is one
traced rounded shape covering anterior+lateral delt with no clean boundary between them
in the source art; rear delt only exists on the back-view figure at all. Splitting would
mean re-tracing sub-path boundaries, not relabeling — out of scope for this pass. The
user explicitly chose this trade-off over a rougher best-effort split. Exercises are
still tagged with the specific delt head (FRONT_DELTS/SIDE_DELTS/REAR_DELTS) for
nudge/radar/ML purposes — only the diagram visualization collapses them.

Reclassification is done by re-running the same bounding-box classification approach
used to originally build `MuscleBodyArt.kt`, with finer boundary rules for the ABS/
OBLIQUES and LATS/UPPER_BACK/LOWER_BACK splits, rather than hand-editing 176 entries
individually. A `MuscleGroup → diagram zone` mapping function (SHOULDERS collapse) lives
in `MuscleBodyDiagram.kt` or `MuscleBodyArt.kt`, used only for diagram rendering — every
other consumer (seed data, nudge, radar, PRs) uses the full 17-value detail directly.

## Testing

- Unit tests for the ABS/OBLIQUES and LATS/UPPER_BACK/LOWER_BACK position classification
  logic (if implemented as testable pure functions rather than inline script output),
  following the existing `PathVerticesTest.kt` pattern.
- On-device verification: exercise library re-seeds with new tags, diagram renders
  without the spiky-path regression class of bug hit in the prior session, nudge banner
  and radar chart display all 17 groups correctly, custom-exercise creation dialog shows
  the new checkbox list.
- Screenshot-before-asking discipline continues per the Update 1.1 process lesson.
