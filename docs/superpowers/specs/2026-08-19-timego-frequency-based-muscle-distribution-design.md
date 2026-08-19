# TimeGo — Frequency-Based Muscle Distribution Radar (design)

**Date**: 2026-08-19
**Status**: proposed — open for review, not yet approved, least-settled of the three specs written
this session
**Context**: The Muscle Distribution radar chart's normalization bug, caught by the user from their
own shoulder session reading near-zero on the chart. Full history of the discussion (and the
abandoned in-progress clarifying question about baseline window) is in `TimeGo/08 Session Log`'s
2026-08-19 entries. The user's own framing: "I don't need to hit legs twice a week, but I might do
arms twice a week" — each muscle group judged against its *own* normal cadence, not against each
other. This is the least-settled of today's three specs; **the baseline window (8 weeks) and the
decision to ship frequency-only in v1 (deferring the volume-as-secondary-signal idea) are the two
calls most likely to need revision on review.**

## Problem

`muscleDistributionForTimeframe` (`MuscleDistribution.kt:11-24`) normalizes every muscle group's
`weightKg × reps` volume against whichever single group has the highest volume in the selected
period. Since kg-moved isn't comparable across muscle groups (a lat pulldown moves more kg than a
lateral raise for an equally hard set), lighter-loaded groups (shoulders, biceps, forearms) always
read low next to legs/back regardless of whether they were trained appropriately.

## Section 1 — Replace volume-vs-max with frequency-vs-own-baseline

For each muscle group, compute a **baseline cadence** (sessions-per-week touching that group as a
primary mover, over a fixed trailing window) independent of whatever timeframe the user has
selected, then compare the *selected* timeframe's actual session count against what that baseline
would predict for a period of that length.

```kotlin
private const val BASELINE_WINDOW_DAYS = 56 // 8 weeks, a fixed constant independent of the
    // selected ProgressTimeframe -- flagged as the most likely value to tune after seeing it in
    // practice; no principled reason for exactly 8 over, say, 6 or 12.

/** Sessions (not sets) touching [group] as a primary mover, in [sessions] restricted to
 *  [sinceDate]..[today] inclusive. Reuses muscleGroupsWorkedInSession's per-session primary-mover
 *  filter (piece A's fix) so a session only "counts" toward a group's cadence when that group was
 *  actually a primary target, not an incidental synergist tag. */
private fun sessionsTouchingGroup(
    group: String,
    sessions: List<WorkoutSession>,
    sets: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    sinceDate: LocalDate,
    today: LocalDate,
): Int
```

```kotlin
fun frequencyDistributionForTimeframe(
    timeframe: ProgressTimeframe,
    sessions: List<WorkoutSession>,
    sets: List<SetLog>,
    exercisesById: Map<Long, Exercise>,
    today: LocalDate,
): Map<String, Float> {
    val baselineSince = today.minusDays((BASELINE_WINDOW_DAYS - 1).toLong())
    val baselineWeeks = BASELINE_WINDOW_DAYS / 7.0
    val selectedSince = timeframe.sinceDate(sessions.minOfOrNull { it.date }, today)
    val selectedWeeks = (today.toEpochDay() - selectedSince.toEpochDay() + 1) / 7.0

    return ANATOMICAL_MUSCLE_GROUPS.associateWith { group ->
        val baselineSessions = sessionsTouchingGroup(group, sessions, sets, exercisesById, baselineSince, today)
        val cadence = baselineSessions / baselineWeeks // sessions/week, this group's own normal rate
        val expected = cadence * selectedWeeks
        val actual = sessionsTouchingGroup(group, sessions, sets, exercisesById, selectedSince, today)
        when {
            expected <= 0.0 && actual <= 0 -> 0f
            expected <= 0.0 -> 1f // any training on a group with zero baseline cadence is fully "on pace"
            else -> (actual / expected).toFloat().coerceIn(0f, 1f)
        }
    }.filterValues { it > 0f || /* keep untrained groups visible as 0, matching today's chart convention */ true }
}
```

A group trained exactly at its own historical rate reads at the spoke's max (1.0); a group trained
*more* than its own rate also caps at 1.0 rather than overshooting the chart, since the point is "am
I keeping up with myself," not "which group did I train hardest this period" (that framing is what
caused the original bug). A group with zero baseline cadence (never trained in the last 8 weeks) that
gets touched even once in the selected period reads as fully on-pace (1.0) — there's no meaningful
"expected rate" to fall short of yet.

## Section 2 — Volume-as-secondary-signal: deferred, not designed here

The original backlog note floated showing per-group volume as a secondary signal once a group is
"within its normal window." **Deliberately simplified for v1**: this spec ships frequency-only. Two
numbers on one spoke (frequency position + volume-driven color/width) is a real chart-design
question — dual-encoding needs its own visual treatment (a second color scale? a border thickness? a
tooltip?) that's out of scope for a first pass at just fixing the normalization bug. Revisit once
frequency-only has been used for a while and there's a concrete sense of what's still missing.

## Section 3 — Integration

`ProgressViewModel` swaps `muscleDistributionForTimeframe(...)` for
`frequencyDistributionForTimeframe(...)` at its one call site feeding `RadarChart`/the muscle-body
diagram. `muscleGroupVolumeDistribution` and `muscleDistributionForTimeframe` themselves are **not
deleted** — `muscleGroupIntensityForSession` (the per-session, single-session heat shading used
elsewhere, e.g. the landing page's last-session diagram) still legitimately wants volume-based
relative intensity within one session, which isn't the cross-session comparability problem this spec
fixes. Only the *radar chart's* timeframe-level distribution changes.

## Verification

Domain-layer TDD:
- `sessionsTouchingGroup`: counts distinct sessions (not sets) within the date range; excludes
  sessions outside the range; only counts a session where the group is a primary mover somewhere in
  it (reuse `muscleGroupsWorkedInSession` test fixtures/patterns).
- `frequencyDistributionForTimeframe`: a group trained at exactly its baseline rate in the selected
  period reads 1.0; a group trained at half its baseline rate reads ~0.5; a group trained above its
  baseline rate caps at 1.0, doesn't exceed it; a group with zero baseline sessions that gets one
  session in the selected period reads 1.0 (the "no expectation yet" case); a group touched in
  neither the baseline nor the selected period reads 0.
- Regression-style sanity check translating the original bug report: a heavy-volume leg session and a
  light-volume shoulder session logged at each group's own normal cadence should read at comparable
  spoke heights — the specific case that motivated this whole spec.

No ViewModel/UI automated tests, per project convention. Verify via build + on-device: check that a
muscle group trained at its usual rate (whatever that is for the real device data) no longer reads
near-zero next to legs/back.

## Out of scope

- **Volume-as-secondary-signal** — see Section 2; explicitly deferred, not designed.
- **A configurable baseline window** — 8 weeks is a hardcoded constant; no settings UI to change it.
- **Applying this same frequency logic to the landing page's recommended-muscle-group pick** — that
  already uses a different, existing staleness-ranking mechanism (`rankUntrainedMuscleGroups`); this
  spec only touches the Progress screen's radar chart.
- **Backfilling/adjusting the baseline for a brand-new user with under 8 weeks of history** — the math
  degrades gracefully (a shorter real history just means a lower `cadence`, not a crash), but the
  resulting chart is likely to look unhelpfully sparse or spiky for a new install; no special-casing
  designed for that here.
