# TimeGo — Rep-Range + RPE-Gated Double Progression (design)

**Date**: 2026-08-19
**Status**: approved, not yet implemented
**Context**: Recommendation-engine backlog piece E (see `TimeGo/03 Feature Catalog` in the vault). User
identified that `RuleBasedOverloadSuggester`'s current PROGRESSING rule — hit `targetReps` once,
immediately add the full weight increment — doesn't match real progressive-overload practice: hitting
a rep target at RPE 9-10 (near failure) means you're maxed out at that load, not ready for more,
while hitting it at RPE 7 with reps in reserve is a genuine signal to add load. This is a rules-only
upgrade, same category as the 2026-08-11 plateau-detection upgrade it builds on top of — not the
deferred ML model. Two techniques from the user's own `MDA522 - Artificial Intelligence` coursework
back this design: Week 11 fuzzy logic for the RPE gate (this spec), and Week 6 reinforcement learning
(contextual bandit) as the actual future ML layer, explicitly out of scope below and sequenced after
this ships and after real longitudinal data exists.

## Problem

`RuleBasedOverloadSuggester`'s `PROGRESSING` branch (`OverloadSuggester.kt:71-85`) has one escalation
rule: `last.reps >= last.targetReps` → add `weightIncrementKg` immediately. `targetReps` is whatever
single number the user typed in when logging that set — there's no notion of a rep *range*, and no
signal for how hard that rep count was to achieve. A set that barely ground out at RPE 10 and a set
that flew up at RPE 6 currently produce the identical suggestion.

## Section 1 — `SetLog.rpe` (new nullable field)

```kotlin
data class SetLog(
    // ...existing fields unchanged...
    val rpe: Int? = null,
)
```

Room migration **10 → 11**, pure additive column, default `null`. Every existing row reads back as
`null` — no backfill, no reinterpretation of historical data. Logging UI (`AddExerciseDialog`/Log
screen set-entry row) gains an optional 1-10 RPE input alongside weight/reps; leaving it blank is a
first-class, permanently-supported state, not just a migration artifact — plenty of sets (warm-ups,
easy accessory work) don't warrant logging effort.

## Section 2 — Deriving a rep range from history (new domain function)

No stored rep range exists anywhere (`Exercise` has no min/max reps field, and none is being added —
see the rejected explicit-range option in the vault backlog note). Instead, the range is derived per
exercise from the sets actually logged at that exercise's **current working weight** — the weight of
the most recent working set.

```kotlin
data class RepRange(val floor: Int, val ceiling: Int)

private const val MIN_SESSIONS_FOR_RANGE = 3

/** Derives a working rep range for [weightKg] from every past working set logged at exactly that
 *  weight, requiring sets from at least [MIN_SESSIONS_FOR_RANGE] distinct sessions before returning
 *  a range -- 3 sets within one session reflect within-session fatigue, not a real range the lifter
 *  operates in at this weight. Returns null (not enough history) when that bar isn't met, including
 *  immediately after every weight escalation, when by definition no history exists at the new
 *  weight yet -- callers fall back to today's single-targetReps behavior in that case. */
fun repRangeAtWeight(
    allWorkingSets: List<SetLog>,
    sessionStartById: Map<Long, Long>,
    weightKg: Double,
): RepRange?
```

Takes the **raw** working-set list (`isWarmup == false`, same filter as today), not the
`sessionWorkingSetHistory`-reduced one-set-per-session list the suggester currently consumes for
trend detection — a rep range needs every set at that weight, not just the last one per session.
Floor = minimum reps ever logged at `weightKg`; ceiling = maximum. Session count is the number of
distinct `sessionId`s (via `sessionStartById`) among the matching sets.

`LogViewModel` computes this once per exercise per suggestion refresh (same place it already builds
`sessionHistory`/`currentSessionWorkingSets`) and passes it into `suggestNext` as a new parameter.

## Section 3 — Escalation logic (replaces one sub-rule, not the whole suggester)

`PlateauStatus` classification (`classifyPlateauStatus`) and the `REGRESSING`/`PLATEAUING` branches
are **unchanged** — that machinery is tested and correct. Only the `PROGRESSING` branch's escalation
condition changes:

| `repRange` | Condition | Behavior |
|---|---|---|
| `null` (not enough history) | — | **Unchanged from today**: `last.reps >= last.targetReps` → full increment. Applies to every new exercise and for the first 1-2 sessions after every weight escalation. |
| present | `last.reps < repRange.ceiling` | Same weight, aim for one more rep (unchanged branch logic from today). |
| present | `last.reps >= repRange.ceiling` | **RPE-gated escalation** — Section 4. |

## Section 4 — RPE-gated escalation (fuzzy logic)

Three deterministic tiers over the logged set's `rpe`, framed as fuzzy membership so the boundary
isn't a hard cliff, but defuzzified to one of three discrete, inspectable actions — no continuous
model, no training data, unit-testable like the rest of this domain layer:

```kotlin
enum class EscalationTier { FULL, PARTIAL, HOLD }

/** RPE <=7 (low effort) -> FULL; RPE 8 (moderate) -> PARTIAL; RPE >=9 (near/at failure) -> HOLD;
 *  null (not logged) -> FULL, same as today's unconditional-escalate behavior when there's no
 *  effort signal to gate on. */
fun escalationTierForRpe(rpe: Int?): EscalationTier
```

| Tier | Weight change | Note text |
|---|---|---|
| `FULL` | `+ weightIncrementKg` (unchanged increment logic, including calisthenics' `null` increment opt-out) | "Ceiling hit with reps in reserve — increasing weight." |
| `PARTIAL` | `+ weightIncrementKg / 2`, rounded via the existing `roundDownToIncrement` to a loadable step; if that rounds to `0.0`, treat as `HOLD` instead | "Ceiling hit but close to your limit — small increase." |
| `HOLD` | unchanged weight | "Ceiling hit at max effort — hold before adding load." |

After a `FULL`/`PARTIAL` escalation, the next 1-2 sessions naturally fall back to the `repRange ==
null` row in Section 3's table (no history exists yet at the new weight) — no special-casing needed,
this falls out of Section 2's own logic.

## Section 5 — Integration

`OverloadSuggester.suggestNext` gains one new parameter (`repRange: RepRange?`) and reads `rpe` off
the `SetPerformance`/`SetLog` it's already given — `SetPerformance` needs `rpe: Int? = null` added
alongside its existing fields to carry that value through from `LogViewModel`. No other UI change
needed beyond the RPE input field itself (Section 1) — `LogViewModel` already renders the suggester's
`note` string on the Log screen, so the richer reasoning surfaces through that existing text field,
same integration pattern as the 2026-08-11 plateau upgrade.

## Verification

Full TDD, matching the existing discipline for this domain code. New test cases:
- `repRangeAtWeight`: fewer than 3 distinct sessions at the weight → `null`; 3+ sessions → correct
  floor/ceiling; sets at a *different* weight are excluded even if plentiful; warm-up sets excluded.
- `escalationTierForRpe`: RPE 1-7 → `FULL`; RPE 8 → `PARTIAL`; RPE 9-10 → `HOLD`; `null` → `FULL`.
- `PARTIAL` tier rounding to `0.0` (e.g. a very small `weightIncrementKg`) falls back to `HOLD`
  behavior rather than suggesting a no-op "increase."
- Full suggester integration: `repRange == null` reproduces today's exact behavior byte-for-byte
  (regression guard, same pattern as the 2026-08-11 spec's REGRESSING regression check); `repRange`
  present + reps below ceiling → unchanged "add a rep" branch; at/above ceiling → each of the three
  RPE tiers produces the documented weight/note.
- Migration 10 → 11 test: existing rows read back with `rpe == null`; schema JSON diffed
  byte-for-byte against Room's generated output before accepting, per the standing project practice
  from the 9 → 10 migration.

## Out of scope

- **The Week 6 reinforcement-learning layer** (contextual bandit: state = recent performance/RPE
  trend, action = weight/rep adjustment, reward = whether the next session hits target at a good
  RPE) — the actual "future ML layer" `OverloadSuggester.kt`'s own doc comment anticipates. Blocked
  on real per-user longitudinal data existing, same durable ML-deferral decision as everything else
  in `06 Decisions and Historical Record`. This spec's fuzzy-logic version is the deterministic base
  layer that future model sits on top of.
- **Explicit stored rep ranges on `Exercise`** — considered and explicitly rejected in favor of
  history-derived ranges (see the AskUserQuestion decision this spec came from); would touch
  `Exercise`, `AddExerciseDialog`, and routine-editing UI, not just the suggester.
- **Time-decay on the history window** used by `repRangeAtWeight` — v1 uses all-time history at a
  given weight with no aging-out of old sessions. Flagged as a candidate follow-up, not designed here.
- **`HoldSuggester`** — this spec covers `OverloadSuggester`/`WEIGHT_REPS` only. Hold-duration
  exercises have no analogous "reps at a weight" concept; extending RPE-gating to holds (e.g. gating
  on hold-duration ceiling instead) would need its own pass if wanted later.
- **Any UI surfacing of `EscalationTier` or `RepRange` beyond the existing `note` string** — same
  deferral pattern as `plateauStatus` in the prior spec; no dedicated indicator is being added now.
