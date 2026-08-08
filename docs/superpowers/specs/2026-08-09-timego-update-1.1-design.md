# TimeGo Update 1.1 — Design

Follows a critical on-device review of v1 (screenshots + user feedback, 2026-08-09). v1 was functionally wired but never got a real design pass — every screen was a flat column of plain text/inputs, the exercise library was too small and ungrouped, routines had no scheduling concept, and personal records / muscle-group nudges dumped raw enum names to the screen. This update fixes the interaction model and the visual design, and adds real day-of-week routine scheduling.

## Problems Addressed (from user's numbered list)

1/7. Flat, cardless, no type hierarchy on Log/Progress/Routines → real layout pass, all screens.
2. Heatmap dot sizing — verified consistent on-device; the actual bug found was full-year month labels clipping ("Au" instead of "Aug"). Fixed as part of the layout pass.
3. Logging every exercise in the whole library is a hassle → Log screen defaults to *today's scheduled routine* (see #10) instead of the full library; freeform remains available as a fallback, scoped by collapsible category/muscle-group sections (see #5).
4. No muscle-group grouping in exercise lists → collapsible sections by `ExerciseCategory`, sub-headed by muscle group.
5. Exercise library too small → expand to 100+, covering strength, calisthenics, warmups, and cardio.
6. Add-custom-exercise buried inline → modal dialog.
8. Empty personal records — expected pre-data, no design gap; fixed incidentally by proper empty-state copy in the layout pass.
9. Strength curve is a text list, no muscle-group view → real line chart + per-exercise/per-muscle-group toggle.
10. No way to plan e.g. "Wed/Thu/Sat" → `Routine.daysOfWeek`, Log screen surfaces today's scheduled routine automatically.
11. Muscle-group nudge dumps raw enum names → formatted, Title-Case, chip-styled.

## Data Model Changes (Room migration 1→2, additive — real user data exists on-device)

- `Exercise` gains `category: String` (`ExerciseCategory` enum name: `STRENGTH`, `CALISTHENICS`, `CARDIO`, `WARMUP`). Migration default: `'STRENGTH'`.
- `SetLog` gains nullable `durationMinutes: Double?` and `distanceKm: Double?`, used instead of `weightKg`/`reps` for `CARDIO`/`WARMUP` exercises. Existing `weightKg`/`reps` become effectively optional-in-practice for those categories (schema keeps them non-null with `0.0`/`0` sentinel values for duration-based entries, to avoid a second nullable-everything migration — domain logic branches on `Exercise.category` to know which fields are meaningful, never on null-checks of weight/reps).
- `Routine` gains `daysOfWeek: List<String>` (`DayOfWeek` enum names, e.g. `["WEDNESDAY", "THURSDAY", "SATURDAY"]`), reusing the existing `Converters.fromStringList`/`toStringList` pair already used for `Exercise.muscleGroups` — same delimiter, same converter methods, no new Room converter needed.

## Domain Logic Additions

- `ExerciseCategory` enum, `MET_STRENGTH`/`MET_CARDIO`/`MET_WARMUP`/`MET_CALISTHENICS` rough constants for calorie estimation (documented as estimates, not medical-grade).
- `estimatedCalorieBurn(met: Double, bodyWeightKg: Double, durationMinutes: Double): Double` — standard `MET × weight(kg) × hours` formula.
- `averagePaceMinPerKm(durationMinutes: Double, distanceKm: Double): Double?` — null when distance is 0 (can't compute pace).
- `routinesForToday(routines: List<Routine>, today: DayOfWeek): List<Routine>` — filters by `daysOfWeek` containing today's name.
- `muscleGroupStrengthCurve(history: List<SetLog>, exercisesById: Map<Long, Exercise>, sessionDateById: Map<Long, LocalDate>, muscleGroup: String): List<Pair<LocalDate, Double>>` — per date, the best estimated-1RM among that day's sets for exercises tagged with the given muscle group.

## Exercise Library

Expand `SEED_EXERCISES` to 100+ entries, each with `category` and `muscleGroups`. Covers:
- **Strength** (barbell/dumbbell/machine): existing 12 plus additional presses, rows, curls, extensions, machine variants per muscle group.
- **Calisthenics**: push-ups, pull-ups (already have one), dips, lunges, burpees, mountain climbers, bodyweight squats, etc.
- **Warmup**: arm circles, leg swings, dynamic stretches, jumping jacks, band pull-aparts.
- **Cardio**: running, cycling, rowing, jump rope, stair climbing, swimming.

## UI Redesign

**Shared pattern**: exercise lists (Log, Routines) render as collapsible `ExerciseCategory` sections (Strength/Calisthenics/Warmup/Cardio), each internally sub-headed by muscle group (non-collapsible sub-headers, to bound UI state complexity). Cards replace bare rows; Material3 `ElevatedCard` or similar for each exercise entry.

**Log screen**: on load, auto-selects today's scheduled routine (via `routinesForToday`) if one exists; falls back to Freeform. Strength/calisthenics exercises keep the weight+reps+suggestion row (now inside a card); cardio/warmup exercises get a duration+distance row instead, showing computed avg pace and estimated calorie burn once distance is entered. "Add Custom Exercise" moves to a modal `AlertDialog` triggered by a top-bar action, not an inline form.

**Progress screen**: Personal Records rendered as formatted cards (Title Case labels, not enum names) with an explicit empty state ("No personal records yet — log a few sets to see them here"). Strength Curve becomes a real Canvas line chart with a toggle between "This exercise" and "Muscle group" (muscle-group mode adds a muscle-group picker, reusing `MuscleGroup` entries). Heatmap month-label clipping fixed (wider label allowance / shorter abbreviation fallback).

**Routines screen**: routine creation form gains a day-of-week picker (7 `FilterChip`s, Mon-Sun) alongside the existing name + exercise selection. Muscle-group nudge banner reformatted as a labeled card with Title-Case, comma-joined muscle names (e.g. "Chest, Back" not "CHEST, BACK").

## Explicitly Out of Scope (this update)

- Meal tracking, ML prediction, wearable integration — still deferred per the original v1 spec.
- Exact MET-per-exercise granularity (e.g. distinguishing running pace bands) — a single rough MET constant per category is used, documented as an estimate.
- Editing/deleting routines or exercises — only creation, matching v1's scope for routines.

## Execution Order (per user's explicit request)

1. **Backend**: Room migration + entity changes, domain logic, expanded seed library.
2. **Frontend**: redesign all three screens plus the new add-exercise dialog.
3. **Verification**: on-device install, screenshots taken and reviewed by the agent *before* asking the user to check anything, then a user confirmation pass.

All done inline, in this session.
