# TimeGo foundation layer (design, short)

Date: 2026-11-24. Branch: `feat/foundation`, stacked on `feat/set-entry-redesign`. Approach B (staged, one commit per step) approved by the owner.

## Why
Per-exercise notes, per-set notes, left/right labels and the roadmap ideas (effective reps, asymmetry, supersets, back-off sets, INOL) all need the same few things. Fixing them once avoids a migration and an RPE-capture rework per feature.

## Goals
- One place to get a reference e1RM that excludes the set being judged.
- The after-set effort (RPE) prompt works when the rest timer is off or deferred.
- One additive schema bump carrying set side, set note and routine-exercise note, preserved by backup restore and routine edits.

## Non-goals
Any UI for notes or sides, superset or rest-deferral logic, stimulating reps, asymmetry, INOL, axial load, time-to-peak.

## What was built
1. `referenceOneRepMax` (`ProgressMath.kt`): best Epley e1RM in the 90 days strictly before a timestamp; skips warm-ups, zero-weight rows, reps over 12.
2. `shouldPromptForEffort` plus a standalone `EffortPromptCard` in `TimeGoNavHost`: the prompt no longer requires a rest bar. Previously, rest default 0 meant no RPE prompt at all.
3. Schema v18 (`MIGRATION_17_18`): `set_logs.side`, `set_logs.note`, `routine_exercises.note`, all nullable TEXT. `relinkRoutineExercises` keeps notes through routine edits; `BackupMerge` and `BackupManager` carry notes through restore.

## Decisions (my calls, flagged for the owner)
- Exercise notes belong to the exercise inside a routine (`routine_exercises.note`), not to the exercise globally.
- Set notes and exercise notes are not sent by account sync (they may hold health detail); `side` is synced.
- Muscle credit needed no change: the app already defaults unlisted primaries to 100.

## Known gaps
- `TimeGoDatabaseMigration14Test` lists migrations only up to 13->14, so it cannot reach the current schema (pre-existing since v17; not touched here).
- The standalone prompt card is untested on a device.
