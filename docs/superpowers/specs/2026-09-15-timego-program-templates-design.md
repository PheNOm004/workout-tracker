# TimeGo — Program Templates (design)

**Date**: 2026-09-15
**Status**: proposed — open for review, not yet approved
**Context**: Backlog item promoted from "later" to "now" after web research into competitor apps
(Boostcamp's program library, Alpha Progression, SwoleX) and reddit-sourced split research. User
confirmed this should be built properly, not as an easy/shortcut version, including a
tiered calisthenics track that hooks into existing infrastructure rather than duplicating it.

## Problem

TimeGo's Routines are entirely user-built — there is no pre-populated "start from a known split"
option, which several competitor apps (Boostcamp's 11k+ programs, Alpha Progression's AI plans) use
as their main differentiator. The landing page's recommendation (`recommendSynergisticMuscleGroups`
in `MuscleBalance.kt`) already picks the single best-fit muscle region + a synergistic partner —
Programs add a layer that turns that region-level signal into a named, multi-exercise session
preset, without replacing or duplicating the underlying recovery/balance logic.

## Confirmed model (from brainstorming dialogue)

- A **Program** is not a Routine and is not weekday-scheduled. It is a small set of **day-types**
  (e.g. Push / Pull / Legs), each defined by a set of muscle regions and an ordered list of exercise
  **slots**.
- The existing recommender is **unchanged in its scoring logic**. When a Program is active, it is
  evaluated at day-type granularity instead of single-region granularity: each day-type's regions are
  scored with the same recovery/staleness function, and the best-scoring day-type becomes the landing
  recommendation. With no Program active, today's single-region behavior is exactly unchanged.
- Selecting the suggested day-type opens a normal logging session **pre-sorted with that day-type's
  resolved exercises as suggestions** — fully editable, not a locked Routine. Freeform/blank-slate
  stays available and is not removed or hidden.
- Static day-type lists only — no periodization (no weekly-changing targets). This rules out
  GZCLP, 5/3/1, Westside Conjugate (exercise rotates every 1-3 weeks) and PHUL/PHAT's split power/
  hypertrophy *intent* per pass, all deferred (see Out of scope).

## v1 Program roster

Chosen for clean static-list fit (research: reddit PPL spreadsheet, upper/lower split guides,
r/bodyweightfitness Recommended Routine, bent-arm/straight-arm split):

| Program | Day-types | Region sets |
|---|---|---|
| Full Body | Full Body (single day-type, A/B exercise rotation) | all regions |
| PPL | Push / Pull / Legs | Push={Chest,FrontDelts,SideDelts,Triceps}, Pull={Lats,UpperBack,LowerBack,Traps,Biceps,Forearms}, Legs={Quads,Hamstrings,Glutes,Calves,Adductors} |
| Upper/Lower | Upper / Lower | Upper={Chest,Back groups,Delts,Biceps,Triceps,Forearms}, Lower={Legs groups,Abs,Obliques} |
| Bro Split | Chest / Back / Shoulders / Arms / Legs | one region-cluster each, matching existing `DISPLAY_REGION_GROUPS` groupings in `MuscleBalance.kt` |
| Arnold Split | Chest+Back / Shoulders+Arms / Legs | compound pairs per research |
| **Calisthenics Progression** | Beginner: Full Body (circuit/superset pairs) · Intermediate: Push/Pull/Legs (calisthenics-only exercise pool) · Advanced: Bent-Arm / Straight-Arm / Legs+Mobility | see Section 3 |

Region sets reuse `MuscleGroup` names already defined in `data/MuscleGroup.kt` and the clustering
precedent in `MuscleBalance.kt`'s `DISPLAY_REGION_GROUPS` / `SYNERGISTIC_MUSCLE_CLUSTERS` — no new
region taxonomy invented.

## Section 1 — Program content as a static Kotlin registry, not a Room table

Programs are curated, versioned-in-code content (6 programs, ~20 day-types total) — the same shape
as `BiomechanicalRegistry`'s in-memory `registry` map, not user-editable data. A Room table would add
migration overhead for content that only changes via app updates.

```kotlin
// domain/programs/ProgramDefinitions.kt
data class ProgramSlot(
    val label: String,               // "Main Lift", "Accessory" -- display only
    val targetGroups: Set<String>,   // MuscleGroup names, same convention as suggestedExerciseFor
    val movementPattern: MovementPattern? = null, // set only for Advanced calisthenics straight-arm slots
)

data class ProgramDayType(
    val name: String,                // "Push", "Beginner Full Body"
    val regionGroups: Set<String>,   // used by the day-type-level recommender scoring
    val slots: List<ProgramSlot>,
)

data class Program(
    val id: String,
    val name: String,
    val dayTypes: List<ProgramDayType>,
)

object ProgramRegistry {
    val ALL: List<Program> = listOf(fullBody, ppl, upperLower, broSplit, arnold, calisthenicsProgression)
}
```

`calisthenicsProgression` is itself one `Program` whose `dayTypes` list is the union of all three
tiers' day-types (e.g. `"Beginner Full Body"`, `"Intermediate Push"`, `"Advanced Bent-Arm"`) — see
Section 4 for how tier selection narrows this down to one tier's day-types at recommendation time,
rather than modeling three separate Programs.

## Section 2 — Day-type-level recommendation (extends `MuscleBalance.kt`)

New function alongside `recommendSynergisticMuscleGroups`, same file:

```kotlin
/** Day-type analogue of [recommendSynergisticMuscleGroups]: scores each candidate day-type by its
 *  most-stale-yet-recovered region (same [rankUntrainedMuscleGroups] logic, applied per day-type's
 *  region set rather than per single region), and returns the best-scoring day-type. Does not
 *  replace or duplicate the underlying scoring -- a day-type's "staleness" is its most-urgent
 *  member region's staleness, so the same 48-hour recovery guardrail and synergist-fatigue
 *  protection apply transitively. */
fun recommendProgramDayType(
    dayTypes: List<ProgramDayType>,
    lastTrainedByGroup: Map<String, LocalDate>,
    today: LocalDate,
    lastWorkedByGroup: Map<String, LocalDate> = lastTrainedByGroup,
): ProgramDayType? {
    if (dayTypes.isEmpty()) return null
    val allGroups = dayTypes.flatMap { it.regionGroups }.distinct()
    val ranked = rankUntrainedMuscleGroups(allGroups, lastWorkedByGroup, today)
    val mostUrgent = ranked.firstOrNull() ?: return dayTypes.first()
    return dayTypes.firstOrNull { mostUrgent in it.regionGroups }
        ?: dayTypes.maxByOrNull { dt -> dt.regionGroups.sumOf { g -> ranked.indexOf(g).let { if (it < 0) 0 else ranked.size - it } } }
}
```

`LogViewModel.refreshLandingSummary` calls this instead of `recommendSynergisticMuscleGroups` only
when `activeProgramId` (Section 4) is non-null, passing that program's (tier-narrowed, for
Calisthenics Progression) day-types.

## Section 3 — Resolving a day-type's slots to specific exercises

Each `ProgramSlot` resolves to one exercise at suggestion time (not stored/cached — computed fresh
like today's `suggestedExerciseFor` call):

- **Default path** (all barbell/general programs, and Beginner/Intermediate calisthenics tiers):
  reuse the existing `suggestedExerciseFor(targetGroups, exercises, lean, usageCounts)` from
  `SuggestedExercise.kt` unchanged, called once per slot.
- **Advanced calisthenics straight-arm slots only** (`movementPattern` set on the slot): instead of
  `suggestedExerciseFor`, look up the user's most-recently-logged exercise matching that
  `movementPattern` via `BiomechanicalRegistry`, and call the existing
  `nextProgressionKey`/`regressionKey` chain to surface the next ungated skill — the same mechanism
  that already gates Front Lever progression today. No new progression logic; this is a second call
  site for infrastructure that already exists.

## Section 4 — Settings: active Program + calisthenics tier

Extends `SettingsRepository`, same pattern as `trainingLean`:

```kotlin
val activeProgramId: Flow<String?> = ... // null = no active program (default; freeform stays primary)
val calisthenicsTier: Flow<CalisthenicsTier> = ... // BEGINNER / INTERMEDIATE / ADVANCED, default BEGINNER

suspend fun setActiveProgramId(id: String?)
suspend fun setCalisthenicsTier(tier: CalisthenicsTier)
```

v1 tier selection is a **manual picker** (Routines page, alongside the Program selector), not
auto-derived from onboarding experience. The training-profile `experience` field referenced in the
Feature Catalog only exists on the unmerged `development/timego-public-foundation` branch — building
against it now would couple this feature to that branch's unfinished Firebase/report work. Auto-
selecting `calisthenicsTier` from that field once merged is a documented follow-up (see Out of
scope), not built here.

**UI**: Routines page gains a Program selector (None + the 6 entries) below the existing
`TrainingLean` row; picking "Calisthenics Progression" reveals the tier picker beneath it.

## Section 5 — Landing page integration

`LandingSummary` gains one field, computed in `refreshLandingSummary` only when a Program is active:

```kotlin
data class ProgramDayTypeSuggestion(
    val dayTypeName: String,
    val exercises: List<Exercise>, // resolved slots, in order, nulls dropped
)

data class LandingSummary(
    val lastSession: LastSessionSummary?,
    val recommendedMuscleGroups: List<String>,
    val suggestedExercise: Exercise?,
    val programSuggestion: ProgramDayTypeSuggestion?, // new
)
```

When `programSuggestion` is non-null, the landing card shows the day-type name (e.g. "Push Day") and
a **"Start with this"** action instead of the existing single "Try: X" line. Tapping it starts a
freeform session with `programSuggestion.exercises` pre-added as expanded, editable rows — same
underlying session-start path as today's freeform start, just with an initial exercise set instead
of empty. The existing blank-slate "Start freeform" action remains present and unchanged, per the
confirmed "less likely choice, but always available" requirement. When no Program is active, the
landing card is pixel-for-pixel what it is today — no regression to the existing single-region flow.

## Verification

Domain-layer TDD, following existing project convention (no ViewModel/UI automated tests):

- `recommendProgramDayType`: never-trained day-type ranks first; 48-hour recovery guardrail applies
  transitively (a day-type whose only region was trained yesterday is demoted below a fully-rested
  one); tie-breaking when two day-types share their most-urgent region; single-day-type program
  (Full Body) always returns that day-type.
- Slot resolution: default path returns identical results to existing `suggestedExerciseFor` calls
  (regression-safety); `movementPattern`-tagged slots resolve via `BiomechanicalRegistry` and never
  fall through to `suggestedExerciseFor`.
- `SettingsRepository.activeProgramId` / `calisthenicsTier`: default null/BEGINNER, round-trip,
  corrupted stored value falls back to default (same `runCatching` convention as `trainingLean`).

On-device verification: set each of the 6 programs active in turn, confirm the landing recommendation
switches to day-type-level suggestions and "Start with this" pre-populates the session correctly;
confirm switching back to "None" restores today's exact single-region behavior; confirm Advanced
calisthenics tier's straight-arm slot surfaces a plausible next-progression exercise given a seeded
logging history.

## Out of scope

- **GZCLP, 5/3/1, Westside Conjugate** — periodized/exercise-rotating, not static lists; would need a
  scripted-progression layer (closer to Liftosaur's Liftoscript) that is a separate, larger design.
- **PHUL/PHAT-style power/hypertrophy A/B intent per day-type pass** — would need `ProgramSlot` to
  carry rep-range intent distinct from the existing overload suggester's own rep-range logic;
  deferred pending a decision on how the two interact (does a Program slot's intent override or feed
  `OverloadSuggester`?).
- **Auto-selecting `calisthenicsTier` from the onboarding training-profile `experience` field** —
  blocked on the `development/timego-public-foundation` branch merging; this spec's manual tier
  picker is the interim mechanism and the wiring hook, not the intended permanent UX.
- **Exercise demo video/media per slot** — explicitly deferred to "far future" per prior discussion
  (storage/pipeline cost).
- **Program-aware `OverloadSuggester`/RPE-gating changes** — Section 3's slot resolution picks *which*
  exercise; it does not change how that exercise's weight/reps progress once logged. Unaffected by
  this spec.
- **User-created/custom Programs** — v1 ships the fixed 6-entry registry only; no UI to define a new
  Program or day-type.
