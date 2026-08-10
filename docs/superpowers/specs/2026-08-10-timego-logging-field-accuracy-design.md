# TimeGo — Logging Field Accuracy (design)

**Date**: 2026-08-10
**Status**: approved, not yet implemented
**Context**: First of two follow-up sessions after the visual-identity pass ("Project A"). User reported that Dead Hang and similar exercises ask for weight+reps when they're actually timed holds. The library is about to grow substantially (Project B, next), so the fix needs to be a real per-exercise property, not a special case for a handful of names.

## Problem

Input fields (weight+reps vs duration+distance) are currently hard-coded by `ExerciseCategory` (`STRENGTH`/`CALISTHENICS` → weight+reps via `StrengthLogRow`; `CARDIO`/`WARMUP` → duration+distance via `CardioLogRow`, see `ui/log/LogScreen.kt`). But CALISTHENICS mixes rep-based exercises (Push-Up, Pull-Up) with timed holds (Plank, Dead Hang) that need duration, not reps. Category correctly groups exercises for display; it's the wrong thing to drive input fields.

## Section 1 — Data model

- New `data/LoggingType.kt`: `enum class LoggingType { WEIGHT_REPS, HOLD, DURATION_DISTANCE }`.
- `Exercise` gains `val loggingType: String = LoggingType.WEIGHT_REPS.name` (same string-storage convention as `category` — Room converter resolution stays unambiguous). **Not** annotated with `@ColumnInfo(defaultValue=...)` — matches the documented gotcha on `Exercise.category`: Room's schema reader doesn't reflect an `ALTER TABLE ... DEFAULT` column's default back through `PRAGMA table_info` the way the annotation assumes; declaring it breaks every real migrated install.
- `SetLog` gains `val holdSeconds: Int? = null` and `val targetHoldSeconds: Int? = null`, mirroring the existing `durationMinutes`/`distanceKm` nullable-pair convention already documented on `SetLog` for CARDIO/WARMUP. For HOLD sets, `weightKg`/`reps` stay 0.0/0 sentinels — same convention, extended to a third case.
- Room migration `MIGRATION_4_5`: `ALTER TABLE exercises ADD COLUMN loggingType TEXT NOT NULL DEFAULT 'WEIGHT_REPS'`, `ALTER TABLE set_logs ADD COLUMN holdSeconds INTEGER`, `ALTER TABLE set_logs ADD COLUMN targetHoldSeconds INTEGER`. Database version bumps 4 → 5.

## Section 2 — Seed data curation

Every one of the 119 seed exercises gets an explicit `loggingType` (via the `strength`/`calisthenics`/`warmup`/`cardio` builder functions in `SeedExercises.kt`, each updated to accept a `loggingType` parameter defaulting appropriately per category):

- STRENGTH → `WEIGHT_REPS` (all of them; external-load exercises always have weight+reps).
- CARDIO, WARMUP → `DURATION_DISTANCE` (existing behavior, now explicit).
- CALISTHENICS → `WEIGHT_REPS` by default, **except** these six → `HOLD`: **Plank, Side Plank, Wall Sit, L-Sit, Dead Hang, Superman**.

(Mountain Climber and Flutter Kick were considered but stay `WEIGHT_REPS`/reps-based — rapid alternating movements more naturally counted in reps than timed.)

Existing installs: any exercise not covered by the migration default (`WEIGHT_REPS`) is already logged with weight+reps today, so the default is correct with zero data loss — the six HOLD exercises get their `loggingType` corrected via `seedMissingExercises`' existing name-matching update path (already used for the 12→119 library expansion in Update 1.1), extended to also update `loggingType` on name match, not just insert missing rows.

## Section 3 — UI

New `HoldLogRow` composable in `ui/log/LogScreen.kt`, sibling to `StrengthLogRow`/`CardioLogRow`, reusing the same `ExerciseCard`/`ExerciseRowHeader` accent-bar treatment added in the visual-identity pass. Single input: "seconds held". `LogScreen`'s branch in the `ExerciseSections` callback switches on `exercise.loggingType` (three-way: `HOLD` → `HoldLogRow`, `DURATION_DISTANCE` → `CardioLogRow`, `WEIGHT_REPS` → `StrengthLogRow`) instead of the current two-way category check.

## Section 4 — Domain

- `personalRecords` and `muscleGroupStrengthCurve` (`domain/ProgressMath.kt`): filter changes from `exercise.category in STRENGTH_CATEGORIES` to `exercise.loggingType == LoggingType.WEIGHT_REPS.name`. Required, not optional — otherwise HOLD sets' 0.0/0 weight/reps sentinels would pollute these functions exactly like the CARDIO/WARMUP bug already fixed in Update 1.1 (see that spec's "real bugs found" section).
- New `PrType.LONGEST_HOLD`, computed in `personalRecords` from `loggingType == HOLD` sets' `holdSeconds` (parallel branch alongside the existing weight/reps computation, not a replacement).
- New `domain/HoldSuggester.kt`: `data class HoldPerformance(val durationSeconds: Int, val targetDurationSeconds: Int)`, `data class HoldSuggestion(val targetDurationSeconds: Int, val note: String)`, `class RuleBasedHoldSuggester`. Same deload-after-two-consecutive-misses rule as `RuleBasedOverloadSuggester`: hit target → +5s next time; missed but logged something → same target restated; missed twice in a row → target × 0.9 (deload).
- `muscleGroupVolumeDistribution` (`domain/MuscleDistribution.kt`): filter changes from `category !in STRENGTH_CATEGORIES` (exclude) to including both `WEIGHT_REPS` and `HOLD` logging types. HOLD sets contribute `holdSeconds` directly as their volume figure — a rough proxy, not weight-equivalent, explicitly commented in code as an interim measure superseded once the next session (Project B) redesigns the muscle-correlation/heatmap model properly. Without this, a muscle trained only via holds would show as untrained on the heatmap/radar — a regression, not just a missed feature.

## Out of scope (explicitly deferred to Project B)

- Redesigning the muscle-correlation/volume model properly (per-muscle weighted contribution, not just tag-presence).
- Heatmap color-threshold clarity.
- Any further library expansion beyond curating `loggingType` on the existing 119.

## Verification

Real domain logic this time (unlike the visual-identity pass) — TDD throughout: unit tests for `RuleBasedHoldSuggester`, the updated `personalRecords`/`muscleGroupStrengthCurve`/`muscleGroupVolumeDistribution` filters, and the new `LONGEST_HOLD` PR computation. Phone is connected this session — full on-device verification (install, log a Plank/Dead Hang/Wall Sit set, confirm PR and suggestion behavior) happens before merge, not deferred.
