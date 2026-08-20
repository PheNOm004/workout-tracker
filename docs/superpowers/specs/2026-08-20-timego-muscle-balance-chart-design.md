# TimeGo — Muscle Balance Chart (design)

**Date**: 2026-08-20
**Status**: proposed, not yet approved
**Context**: Replaces the reverted "frequency-based Muscle Distribution" attempt (`TimeGo/03 Feature
Catalog`'s backlog entry, commit `e1bffca` reverted it 2026-08-19). That attempt compared each muscle
group's frequency against its own trailing-8-week baseline and was rejected on-device: it turned the
chart into a near-solid blob because everything reads as roughly "on pace" with its own habit,
regardless of whether that habit is itself balanced. This spec starts from a clarified product intent
(user, 2026-08-20): the radar chart's job is **balance across muscle groups**, not raw
strength/volume — the existing body-diagram heatmap already covers "how strong/how much," so the radar
chart needs a distinct, non-redundant signal or it should be removed rather than kept as a second copy
of the same information.

## Problem with the original (pre-revert) formula

`muscleDistributionForTimeframe` (`domain/MuscleDistribution.kt`) sums `weightKg × reps` (or
`holdSeconds` for holds) per muscle group, then normalizes every group against whichever single group
has the highest raw total that period. Compound lifts (squats, deadlifts) always produce far larger
kg×reps totals than isolation work (curls, lateral raises), so leg/back groups structurally dominate
the chart regardless of whether other groups were trained *adequately* — the unit itself isn't
comparable across muscle groups.

## Why the frequency-vs-own-baseline attempt failed

It replaced "compare muscles to each other" with "compare each muscle to its own historical pattern."
That's self-referential: a person's own habit already contains whatever imbalance exists, so measuring
against it can only confirm consistency, never reveal a real gap. This spec's design must compare
against an **external, fixed** reference instead.

## Design

### Metric — weighted effective-set count, not kg×reps volume

Set count (not kg-moved) is the unit fitness literature treats as comparable across muscle groups —
a heavy squat set and a light lateral-raise set both represent one unit of training attention
regardless of load. Within that, not all sets are equal: hypertrophy research places the effective
stimulus specifically in sets taken close to failure.

```kotlin
/** RPE >=7 (0-3 reps in reserve, "effective rep" territory per hypertrophy research) -> full
 *  credit. RPE 5-6 -> partial credit, ramping linearly. RPE <=4 -> low credit (light work still
 *  counts a little, just not as a real stimulus set). Missing RPE -> full credit, same convention
 *  as every other RPE-gated behavior in this app (OverloadSuggester's escalationTierForRpe): never
 *  penalize a value the user simply didn't log. */
fun effortWeight(rpe: Int?): Double = when {
    rpe == null -> 1.0
    rpe >= 7 -> 1.0
    rpe >= 5 -> 0.3 + (rpe - 5) / 2.0 * 0.7
    else -> 0.15
}
```

Each non-warmup, non-cardio `SetLog` in the window contributes `effortWeight(set.rpe) *
(exercise.muscleWeights[group] ?: 100) / 100.0` "effective sets" to every muscle group it's tagged
with — same partial-credit convention `muscleGroupVolumeDistribution` already uses, just applied to a
set-count-like unit instead of a load-based one. `HOLD` sets count as one set each (no separate
duration-based weighting — duration already factors into whether a set was hard via RPE). Cardio/
warm-up excluded, same filter as the existing volume function.

### Target — uniform 10 effective sets/week per muscle group

Evidence-grounded, not assumption-grounded: Schoenfeld et al.'s meta-analyses put ~10-20 weekly sets
per muscle group as the range that maximizes hypertrophy signal, with ~10/week as the well-supported
floor for "adequate." Research does **not** support a lower target for legs specifically — if
anything, studies found legs benefiting from higher volumes, the opposite of the tiered assumption
this spec started from and discarded. One flat target across every `MuscleGroup` (except
`FULL_BODY`, excluded entirely, same as today) avoids inventing per-muscle numbers with no evidential
basis.

### Time window — reuses the existing `ProgressTimeframe` tabs, not a new selector

No new UI control. `weeksInWindow` is derived from the same `since`/`today` bounds
`muscleDistributionForTimeframe` already computes via `ProgressTimeframe.sinceDate(...)` (which
already clamps to the account's actual earliest session, so a new account viewing "Lifetime" or
"Year" isn't penalized by a nominal window it hasn't existed for). `Lifetime`/`Year` naturally read as
"your average weekly rate over that period" — consistent with what every other Progress stat already
means for those tabs, not a new kind of ambiguity.

```kotlin
private const val TARGET_EFFECTIVE_SETS_PER_WEEK = 10.0

/** Weighted effective-set count per muscle group across [sets] logged on/after [since], mirroring
 *  muscleGroupVolumeDistribution's filtering/partial-credit conventions but counting effort-
 *  weighted sets instead of load. Deliberately separate from muscleGroupVolumeDistribution/
 *  muscleDistributionForTimeframe -- those remain unchanged and continue to drive the body-diagram
 *  heatmap, which already covers "how strong/how much" and must not change. */
fun muscleGroupEffectiveSetDistribution(
    history: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sessionDateById: Map<Long, LocalDate>,
    since: LocalDate,
): Map<String, Double>

/** [muscleGroupEffectiveSetDistribution] normalized against a fixed weekly target rather than the
 *  period's own max group -- min(1.0, effectiveSets / (10 * weeksInWindow)). weeksInWindow is
 *  (today - since) in days / 7.0, with a floor to avoid a divide-by-near-zero score on someone's
 *  very first day (see Edge cases). */
fun muscleBalanceForTimeframe(
    timeframe: ProgressTimeframe,
    sessions: List<WorkoutSession>,
    sets: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    today: LocalDate,
): Map<String, Float>
```

### Integration — new state stream, radar chart only, heatmap untouched

`ProgressViewModel` gains a second derived `StateFlow<Map<String, Float>>` (`muscleBalance`),
computed alongside the existing `muscleDistribution` in the same `combine` block (same inputs already
observed, no new Flow sources needed). `ProgressScreen.kt`'s `RadarChart` call switches from
`orderedMuscleDistributionForChart(muscleDistribution)` to
`orderedMuscleDistributionForChart(muscleBalance)` (the ordering/`FULL_BODY`-filtering helper is
value-agnostic and can be reused as-is against the new map). The `MuscleBodyDiagram` call two lines
below stays wired to `muscleDistribution`, completely unchanged. Section header text changes from
"Muscle Distribution" to "Muscle Balance"; the empty-state message and any explanatory copy near the
chart get a short update to describe balance-vs-target rather than distribution-vs-strongest-group.

## Edge cases

- **Very short window** (e.g. viewing "Week" on someone's first day of use): `weeksInWindow` floors
  at `1 / 7.0` (one day) rather than allowing a near-zero denominator to produce a misleadingly huge
  score from a single hard set.
- **No sets in window for a group**: absent from the map, same convention `muscleDistributionForTimeframe`
  already uses (renders as untrained/neutral, not fabricated as an explicit zero).
- **A set with `rpe` outside 1-10** shouldn't occur (existing input UI presumably constrains this),
  but `effortWeight` treats anything `< 5` uniformly as `0.15` and anything `>= 7` as `1.0`, so an
  out-of-range value fails safe rather than crashing or producing a nonsensical weight.

## Testing

Full TDD, matching existing domain-layer discipline:
- `effortWeight`: RPE 1-4 -> 0.15; RPE 5 -> 0.3; RPE 6 -> ~0.65; RPE 7-10 -> 1.0; `null` -> 1.0.
- `muscleGroupEffectiveSetDistribution`: a hard set (RPE 8) contributes full weighted credit; a light
  set (RPE 3) contributes reduced credit; a set with no RPE contributes full credit; warm-up/cardio
  excluded; partial-credit via `muscleWeights` applied correctly (a pull-up's ~30%-weighted biceps tag
  yields 0.3x the set's effort-weighted credit toward `BICEPS`).
- `muscleBalanceForTimeframe`: a group with exactly 10 effective sets in a 1-week window scores 1.0;
  5 effective sets scores 0.5; 20 effective sets caps at 1.0 (does not exceed); a group with zero sets
  is absent from the map; `FULL_BODY` never appears; a `Lifetime` window on a 2-week-old account uses
  2 weeks (not a much larger nominal window) as the denominator; a near-zero window doesn't divide by
  zero.
- Regression check: `muscleDistributionForTimeframe` and `muscleGroupVolumeDistribution` are
  byte-for-byte unchanged (same test inputs/outputs as before this spec) — the body-diagram heatmap's
  behavior must not move.
- `ProgressScreen`/`ProgressViewModel`: `muscleBalance` is a new, independently-collected `StateFlow`;
  `muscleDistribution` continues unchanged and is still what `MuscleBodyDiagram` receives.

## Out of scope

- **Moving this chart (or a version of it) to the Log landing page, alongside a new routine
  "last-completed" nudge.** Explicitly the user's stated next step *after* this redesign is approved
  and verified, not part of this spec. Gets its own brainstorm/spec once this ships.
- **Deleting `muscleDistributionForTimeframe`/`muscleGroupVolumeDistribution`.** They remain in active
  use by the body-diagram heatmap (`muscleGroupIntensityForSession` also depends on the shared
  `muscleGroupVolumeDistribution` helper) — nothing about this spec removes or repurposes them.
- **Per-muscle-group target tiering** (e.g. legs lower, shoulders higher) — considered and dropped;
  the cited research doesn't support it, and a uniform target is simpler and no less defensible.
- **A dedicated time-window selector separate from the existing Progress tabs** — considered and
  dropped in favor of reusing `ProgressTimeframe` directly (see Time window section above).
- **Renaming/removing the underlying `MuscleDistribution.kt` file** — the new function is added
  alongside the existing ones in the same file; no file split is needed at this size.
