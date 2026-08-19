# TimeGo — Frequency-Based Exercise-List Ordering (design)

**Date**: 2026-08-19
**Status**: proposed — open for review, not yet approved
**Context**: Recommendation-engine backlog piece C (`TimeGo/03 Feature Catalog` in the vault). User's
own words: "put most suitable exercise on top (so that I don't have to scroll across 600 and search,
even on freeform ones, if I do back it prioritises back exercises I most commonly do)." Explicitly
flagged as needing more thinking before a design pass; this spec makes the calls the user deferred
and is written for review rather than pre-approved, unlike pieces A/E.

## Problem

`ExerciseSections` (`ExerciseListSections.kt:133`) groups the 600+-exercise library by category then
muscle-group subheading, but exercises *within* a subheading (and in the flat search-results list)
appear in whatever order the input `exercises` list has them — effectively seed/insertion order, not
relevance. Finding "the back exercise I actually do" still means scanning every row in that
subsection every time, even though the same handful of exercises get logged repeatedly.

## Section 1 — Ranking signal: all-time usage frequency (proposed)

Simplest signal that directly matches "exercises I most commonly do": a count of non-warmup working
sets ever logged for that exercise, all-time, no recency decay.

```kotlin
/** Count of non-warmup, non-cardio/warmup-category working sets ever logged per exercise, all-time
 *  -- the ranking signal for exercise-list ordering. No recency weighting: an exercise logged 40
 *  times two years ago still outranks one logged twice last week, on the theory that "what you
 *  reach for" is closer to a long-run habit than a recent blip. Revisit with a recency-weighted
 *  variant only if that theory doesn't hold up in practice. */
fun exerciseUsageFrequency(setLogs: List<SetLog>, exercisesById: Map<Long, Exercise>): Map<Long, Int> =
    setLogs
        .filter { log ->
            !log.isWarmup && exercisesById[log.exerciseId]?.category?.let {
                it != ExerciseCategory.WARMUP.name && it != ExerciseCategory.CARDIO.name
            } == true
        }
        .groupingBy { it.exerciseId }
        .eachCount()
```

**Open call, flagged for review**: no recency decay in v1. An exercise you hammered for months two
years ago and haven't touched since will still outrank something you've been doing every session for
the last month. If that turns out wrong in practice, the fix is a weighted variant (e.g. half-life
decay), not a redesign — the sort-key computation is isolated to this one function.

## Section 2 — Where the sort happens: pre-sort the input list, not `ExerciseSections` itself

`ExerciseSections` never re-sorts its input — `inCategory.groupBy { ... }` and each group's
`forEach { itemContent(exercise) }` both preserve the order of the `exercises` list as given, and the
search-mode `matches` filter does too (`ExerciseListSections.kt:145`, `:153`). That means sorting the
`exercises` list **before** it's passed into `ExerciseSections` automatically produces
frequency-ordered subsections *and* frequency-ordered search results, with zero changes needed inside
`ExerciseSections` itself.

```kotlin
fun exercisesRankedByFrequency(exercises: List<Exercise>, usageCounts: Map<Long, Int>): List<Exercise> =
    exercises.sortedWith(compareByDescending<Exercise> { usageCounts[it.id] ?: 0 }.thenBy { it.name })
```

Never-logged exercises (`usageCounts[it.id] ?: 0` = 0) sort after every used exercise, alphabetically
among themselves via the `thenBy` tiebreak — so browsing a muscle group you've never trained still
gets a stable, readable order rather than becoming effectively random.

## Section 3 — Integration: both `ExerciseSections` call sites

`ExerciseSections` is used in exactly two places (`ExerciseListSections.kt:133`'s only callers):
`LogScreen.kt:326` (the freeform/routine set-logging exercise picker — this is the one the user's
"even on freeform ones" comment is about) and `RoutineFormDialog.kt:90` (routine builder). Both
already have their owning ViewModel's repository access. Each ViewModel computes
`exerciseUsageFrequency` once (from its own `allSetLogs()`/`exercises` collectors, same pattern
`RoutinesViewModel.refreshUntrainedGroups` already uses) and passes `exercisesRankedByFrequency(...)`
into `ExerciseSections` instead of the raw list.

## Verification

Domain-layer TDD, matching this project's existing discipline:
- `exerciseUsageFrequency`: counts non-warmup sets correctly; excludes `isWarmup=true` sets; excludes
  CARDIO/WARMUP-category exercises even if `isWarmup=false` (matches `isTrainingSet`'s convention
  elsewhere in the domain layer); an exercise with zero logged sets is simply absent from the map
  (not a zero entry), matching the `?: 0` fallback the ranking function already expects.
- `exercisesRankedByFrequency`: higher count sorts first; equal counts (including two 0-count
  exercises) tie-break alphabetically by name; empty `usageCounts` map produces a fully alphabetical
  list (degrades gracefully for a brand-new install with no logged history).

No ViewModel/UI automated tests, per this project's existing convention (junit-only test deps).
Verify via build + on-device: open the exercise picker, confirm a frequently-logged exercise (e.g.
whatever the real device data shows most sets for) appears near the top of its muscle-group section.

## Out of scope

- **Recency weighting** — flagged above as the likely first revision if all-time frequency proves
  wrong in practice, not designed here.
- **Cross-muscle-group relevance ranking** (e.g. "since you're browsing Back, also surface Biceps
  higher") — the existing category/muscle-group section structure already scopes browsing; this spec
  only reorders *within* that existing structure, not across it.
- **A learned/statistical ranking model** — per the durable ML-direction decision in `TimeGo/06
  Decisions and Historical Record`, this starts as a plain heuristic; a learned ranking is a possible
  future upgrade once there's a reason to believe the heuristic is insufficient, not designed here.
- **Search-query relevance blending** (e.g. weighting name-match quality against frequency) — out of
  scope; frequency ordering applies uniformly to search results same as browsed sections.
