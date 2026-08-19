# TimeGo — Calisthenics-Lean Recommendation (design)

**Date**: 2026-08-19
**Status**: proposed — open for review, not yet approved
**Context**: Recommendation-engine backlog piece D. User's own words: "recommend me bodyweight
variations sometimes or recommend me new ones. (can be an option at the routines page; leaning
towards strength or calisthenics)." Confirmed as real scope by the user, not just an illustrative
example, unlike piece C. Written for review per the same "make reasonable calls, flag for review"
instruction as piece C's spec.

## Problem

The landing page's "Recommended" section (`LogViewModel.refreshLandingSummary`) only ever names a
*muscle-group region* (e.g. "Legs"), never a specific exercise — the user still has to pick one
themselves from the full library every time. There's also no way to express a standing preference
toward bodyweight training, and no mechanism to nudge toward exercises the user hasn't tried.

## Section 1 — Stored preference: `TrainingLean` (new DataStore value)

```kotlin
enum class TrainingLean { STRENGTH, BALANCED, CALISTHENICS }
```

Added to `SettingsRepository` (`SettingsRepository.kt`) using the exact same pattern as
`holdDelaySeconds` — a `stringPreferencesKey` (enum name stored as string, same convention
`Converters` already uses elsewhere for enum persistence), default `BALANCED`.

```kotlin
val trainingLean: Flow<TrainingLean> = context.settingsDataStore.data.map { prefs ->
    prefs[TRAINING_LEAN_KEY]?.let { runCatching { TrainingLean.valueOf(it) }.getOrNull() } ?: TrainingLean.BALANCED
}

suspend fun setTrainingLean(lean: TrainingLean) {
    context.settingsDataStore.edit { prefs -> prefs[TRAINING_LEAN_KEY] = lean.name }
}
```

**UI**: a 3-option segmented row on the Routines page's existing "Workout settings" section
(`RoutinesScreen.kt`, alongside the hold-delay stepper) — "Strength / Balanced / Calisthenics",
single-select, matches the user's own "leaning towards strength or calisthenics" phrasing directly.

## Section 2 — Picking one specific exercise for a recommended region (new domain function)

```kotlin
/** Picks one specific exercise to suggest for [targetGroups] (the landing page's already-computed
 *  recommended-region groups), given the current [lean] preference and [usageCounts] (reusing
 *  piece C's exerciseUsageFrequency -- see 2026-08-19-timego-frequency-exercise-ordering-design.md).
 *  Preference is a soft filter, never a hard one: if the leaned category has zero matching
 *  exercises for these groups, falls back to the full candidate set rather than returning null --
 *  a recommendation that silently disappears because of a settings toggle would be a worse
 *  experience than one that ignores the toggle this one time. Among the (possibly filtered)
 *  candidates, prefers the LEAST-used exercise (ties broken by name) -- surfaces variety/novelty
 *  rather than suggesting the same exercise the recommendation logic would already nudge the user
 *  toward via habit. Returns null only when [targetGroups] matches nothing in the library at all. */
fun suggestedExerciseFor(
    targetGroups: Set<String>,
    exercises: List<Exercise>,
    lean: TrainingLean,
    usageCounts: Map<Long, Int>,
): Exercise? {
    val matching = exercises.filter { it.muscleGroups.any { g -> g in targetGroups } }
    if (matching.isEmpty()) return null
    val leaned = when (lean) {
        TrainingLean.STRENGTH -> matching.filter { it.category != ExerciseCategory.CALISTHENICS.name }
        TrainingLean.CALISTHENICS -> matching.filter { it.category == ExerciseCategory.CALISTHENICS.name }
        TrainingLean.BALANCED -> matching
    }
    val candidates = leaned.ifEmpty { matching }
    return candidates.minWithOrNull(compareBy<Exercise> { usageCounts[it.id] ?: 0 }.thenBy { it.name })
}
```

**Open call, flagged for review**: "least-used" is the whole novelty mechanism — there's no separate
"never tried before" tier beyond that (a 1-time-logged exercise and a 0-time one are adjacent in the
sort, not specially distinguished). If the user wants an explicit "brand new to you" callout
distinct from "just infrequent," that's a small follow-up (filter to `usageCounts[it.id] == null`
first, fall back to least-used only if that's empty), not a redesign.

## Section 3 — Landing page integration

`LandingSummary`'s existing `recommendedMuscleGroups: List<String>` (`LogViewModel.kt`) gains a
sibling field, computed alongside it in `refreshLandingSummary`:

```kotlin
data class LandingSummary(
    val lastSession: LastSessionSummary?,
    val recommendedMuscleGroups: List<String>,
    val suggestedExercise: Exercise?,  // new
)
```

`LogScreen.kt`'s recommendation card gains one line beneath the existing muscle-group text: `"Try:
${suggestedExercise.name}"`, styled as secondary text (not a button — see Out of scope). Omitted
entirely when `suggestedExercise` is null (empty recommended-groups case, unchanged from today).

## Verification

Domain-layer TDD:
- `suggestedExerciseFor`: BALANCED returns any matching exercise; STRENGTH excludes CALISTHENICS
  candidates when a non-calisthenics match exists; CALISTHENICS excludes non-calisthenics candidates
  when a calisthenics match exists; either lean falls back to the full matching set when its
  preferred category has zero matches for `targetGroups`; least-used candidate wins ties broken by
  name; empty `targetGroups` match (no exercise tags any given group) returns null.
- `SettingsRepository.trainingLean`: default `BALANCED` when unset; round-trips through
  `setTrainingLean`; a corrupted/unrecognized stored string (defensive case, matches the
  `runCatching` guard) falls back to `BALANCED` rather than crashing.

No ViewModel/UI automated tests, per project convention. Verify via build + on-device: toggle each
lean option on the Routines page, confirm the landing page's "Try: <exercise>" line changes
accordingly and never disappears while `recommendedMuscleGroups` is non-empty.

## Out of scope

- **Tapping the suggested exercise to jump straight into logging it** — v1 is display-only text, no
  deep-link/navigation action. Real scope, but a separate UI-flow decision (does it start a session?
  pre-select the exercise in an already-active one? what if no session is active?) that deserves its
  own pass rather than being bundled in here.
- **Per-exercise "mark as favorite/never suggest this" override** — not requested, not designed.
- **Extending the lean preference into the overload/progression suggester** (piece E) or the smart
  exercise-list ordering (piece C) — this spec only touches the landing-page single-exercise
  recommendation. A future pass could apply `lean` to piece C's ordering too (e.g. nudge calisthenics
  exercises up within a muscle-group section), but that's not designed here.
- **A "new exercise you've truly never tried" distinct UI treatment** — see Section 2's flagged open
  call; not built in v1.
