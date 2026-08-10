# TimeGo — Suggester Plateau-Detection Upgrade (design)

**Date**: 2026-08-11
**Status**: approved, not yet implemented
**Context**: Sub-project 2 of a two-part follow-up scoped out of a broader "ML for progressive overload" discussion. Sub-project 1 (external exercise library import) shipped and merged to `master`. This sub-project is **not** the ML model itself — real per-user longitudinal training data doesn't exist yet (confirmed: neither Kaggle nor GitHub had a usable dataset for it; the exercise-catalog datasets imported in Sub-project 1 are metadata, not progression history). This is a rules-only upgrade to `RuleBasedOverloadSuggester`/`RuleBasedHoldSuggester`, design-inspired by LiftShift's plateau detection and stalled-lift suggestions (clean-room reimplementation in Kotlin — LiftShift and wger are both AGPL-3.0, so their code isn't reused, only the conceptual approach). It becomes the base layer a future ML model sits on top of or replaces once real training data exists.

**Future ML note** (out of scope here, recorded for later): OpenPowerlifting (surfaced via the sergeimakarovv/ML-Powerlifting GitHub repo, sourced from a Kaggle mirror) has Sex/Age/Bodyweight/lift-performance data across thousands of athletes — cross-sectional, not longitudinal, so it doesn't solve the progression-training-data gap, but it's a strong candidate for a future bodyweight-normalized strength-standard model (e.g. "for your bodyweight, a typical intermediate deadlift is X"), which is closer to what the user actually wants than generic progression prediction. Revisit when the ML phase starts.

## Problem

`RuleBasedOverloadSuggester`/`RuleBasedHoldSuggester` only ever look at the last 1-2 logged sets and pick one of 3 fixed outputs (deload 10% / +2.5kg / +1 rep). This can't distinguish "steadily improving" from "stuck at the same numbers for weeks" — both currently produce the same "same weight, one more rep" suggestion whenever the last set merely hit target. It also has no calisthenics-specific ceiling behavior: when a bodyweight exercise's reps cap out, the suggester has nothing useful to say (the user rarely adds external load to calisthenics, so "add weight" isn't a real answer for that case).

## Section 1 — Plateau status data model

New `PlateauStatus` enum (`PROGRESSING`, `PLATEAUING`, `REGRESSING`) added as a field on both `OverloadSuggestion` and `HoldSuggestion`, alongside their existing fields:

```kotlin
enum class PlateauStatus { PROGRESSING, PLATEAUING, REGRESSING }

data class OverloadSuggestion(val weightKg: Double, val reps: Int, val note: String, val plateauStatus: PlateauStatus)
data class HoldSuggestion(val targetDurationSeconds: Int, val note: String, val plateauStatus: PlateauStatus)
```

Structured, not just folded into `note` text, so it's independently usable later (a future Progress-screen indicator, or a feature the eventual ML layer consumes) without parsing strings.

## Section 2 — Plateau detection algorithm

Computed from a rolling window of the **last 5 logged sets** for that exercise (a named constant, not a magic number), instead of just the last 1-2:

- **Metric**: estimated 1RM per set (reusing `ProgressMath`'s existing Epley-formula helper) for `WEIGHT_REPS`/`OverloadSuggester`; hold duration directly for `HOLD`/`HoldSuggester`.
- **REGRESSING**: the last 2 sets both missed target — same condition as today's deload trigger, now also labeled with the status.
- **PROGRESSING**: the most recent value is at or above the window's rolling average, or the window shows a net-upward trend.
- **PLATEAUING**: neither of the above — values flat or oscillating with no clear direction across the window. This is the new signal: today's suggester has no way to express "stuck," only "hit target" or "missed target."
- **Fallback**: fewer than 5 logged sets → use today's simpler last-2-sets comparison (REGRESSING if both missed, otherwise PROGRESSING). No plateau claim without enough history to support one.

## Section 3 — Suggestion decision table

Replaces the current 3-branch `when` with a table keyed by `plateauStatus`:

| Status | Suggestion |
|---|---|
| REGRESSING | Deload 10% (unchanged from today) — note explains it's from 2 consecutive misses. |
| PROGRESSING | Last set hit target → +2.5kg (or +5s for holds), same target reps/duration. Last set missed target → same weight, aim for one more rep (unchanged branch logic from today, now gated by an explicit "trending up" status). |
| PLATEAUING | **New behavior**: hold weight/target flat for one more session rather than blindly adding weight into a stall (today's rule would suggest +2.5kg here whenever the last set merely hit target, regardless of the surrounding trend). Note flags the plateau and warns that a continued stall next session may trigger a deload. |

Applies identically to `HoldSuggester` using hold-duration trend in place of 1RM.

## Section 4 — Calisthenics progression-tier suggestion

For `CALISTHENICS`-category exercises specifically, when status is PROGRESSING or PLATEAUING **and** the last set hit or exceeded target reps by a wide margin (≥1.5x target — a proxy for "this has gotten easy"), check a small hand-curated progression map before falling through to Section 3's normal behavior:

```kotlin
private val CALISTHENICS_PROGRESSIONS: Map<String, String> = mapOf(
    "Tuck Planche Hold" to "Advanced Tuck Planche Hold",
    "Advanced Tuck Planche Hold" to "Straddle Planche Hold",
    "Straddle Planche Hold" to "Full Planche Hold",
    "Tuck Front Lever" to "Advanced Tuck Front Lever",
    "Advanced Tuck Front Lever" to "Straddle Front Lever",
    "Straddle Front Lever" to "Front Lever Hold",
    "Tuck Back Lever" to "Straddle Back Lever",
    "Straddle Back Lever" to "Back Lever Hold",
    "Human Flag Tuck" to "Human Flag Straddle",
    "Human Flag Straddle" to "Human Flag Hold",
)
```

If the current exercise name is a key in this map, the suggestion's `note` names the next tier (e.g. *"Consistently hitting target — try Advanced Tuck Planche Hold next"*) instead of suggesting a weight/duration bump. If not found (already at the top tier, or no known progression chain for this exercise), falls through to Section 3's normal PROGRESSING/PLATEAUING behavior unchanged. `STRENGTH` exercises are unaffected — external load is the natural progression there, so they keep the normal +2.5kg suggestion.

This is a static, hand-authored map covering only the explicitly-tiered families already in the library (from Sub-project 1's imports) — not derived from muscle tags or any automatic naming heuristic, since most of the library isn't tiered.

## Verification

Full TDD, matching the existing discipline for this domain code (plain Kotlin, no Android dependency, already unit-tested). New test cases:
- Plateau-status classification at each window size: fewer than 5 sets (fallback to today's logic), clear uptrend → PROGRESSING, clear downtrend (2 misses) → REGRESSING, flat/oscillating → PLATEAUING.
- Regression check: REGRESSING still produces the same 10% deload as today's unmodified behavior (Section 3 shouldn't silently change what already works).
- Calisthenics progression-tier lookup: found (returns next tier), not found (falls through to normal suggestion), already-top-tier (falls through).
- STRENGTH vs CALISTHENICS branch split: STRENGTH exercises never consult the progression map even when the rep-ceiling condition is met.

**Integration**: no `LogViewModel` or UI changes needed. `LogViewModel` already calls `suggester.suggestNext(...)`/`holdSuggester.suggestNext(...)` and renders the returned `note` string on the Log screen — the richer reasoning surfaces through that existing text field. `plateauStatus` isn't shown anywhere in the UI yet; it's a structured field for future consumers (a Progress-screen indicator, or the eventual ML layer) per this sub-project's role as the ML base layer.

## Out of scope

- The ML model itself — tracked as a future sub-project, blocked on real per-user longitudinal training data (or a viable alternative like an OpenPowerlifting-based bodyweight-normalized model, noted above).
- Rep-scheme switching (LiftShift's "switch up the rep scheme" suggestion) — requires a rep-scheme concept TimeGo's data model doesn't have (`Exercise`/`SetLog` have no notion of a prescribed rep scheme beyond a single `targetReps`); would need its own design if pursued later.
- Any UI surfacing of `plateauStatus` — deferred until there's a concrete consumer for it.
- Bodyweight/height wired in beyond the calisthenics progression-tier map — a fuller "relative strength" model (e.g. actually computing reps-per-kg-bodyweight ratios) is deferred to the ML phase per the user's explicit scoping decision this session.
