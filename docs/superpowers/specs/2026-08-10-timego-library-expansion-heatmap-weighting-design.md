# TimeGo — Library Expansion & Weighted Muscle Correlation (design)

**Date**: 2026-08-10
**Status**: approved, not yet implemented
**Context**: "Project B" of the post-visual-identity-pass follow-up ("a much bigger library with which muscles it targets and how much? ... the threshold for the heatmap coloring is unclear"). Project A (logging field accuracy) is merged. This session addresses the two remaining, interlinked asks: real per-muscle weighted contribution (not just tag presence) and heatmap-color clarity.

## Problem

`Exercise.muscleGroups` is a flat tag list — every tagged muscle gets 100% credit for a set's full volume in `muscleGroupVolumeDistribution` (e.g. a Squat's full volume counts equally toward QUADS and GLUTES, even though GLUTES is clearly a secondary mover). This flattens the muscle-distribution radar chart and body-heatmap into "which muscles were touched" rather than "how much was each muscle actually worked" — the user's literal ask. Separately, the heatmap's color scale is relative to the user's own single most-trained muscle group in the period, with no explanation on screen, so a color reads as unexplained.

## Section 1 — Weighted muscle contribution (data model)

New `Exercise.muscleWeights: Map<String, Int>` (MuscleGroup name → 0-100 percentage). **Additive, not a replacement**: `muscleGroups` (the flat tag list) is untouched and keeps driving library section grouping (`ExerciseSections`), the untrained-muscle nudge banner (`untrainedMuscleGroups`/`lastTrainedDatesByMuscleGroup`), and custom-exercise creation — none of those need weighting, only the volume/heatmap calculation does. `muscleGroupVolumeDistribution` looks up `exercise.muscleWeights[group] ?: 100`, so any exercise without explicit weights (all custom exercises, any seed exercise not yet retrofitted) behaves exactly as it does today.

- New Room `TypeConverter` for `Map<String, Int>`, same ASCII-delimiter style as the existing `fromStringList`/`toStringList` (record separator `` between entries, unit separator `` between key/value within an entry — one level below the existing list delimiter, same non-printable-character convention lifted from HeatP).
- Migration 5→6: `ALTER TABLE exercises ADD COLUMN muscleWeights TEXT NOT NULL DEFAULT ''` (empty string decodes to an empty map via the converter, meaning "no overrides, everything defaults to 100" — safe backward-compatible default). No `@ColumnInfo(defaultValue=...)` annotation, per the established gotcha.
- Builder-function change to avoid rewriting every existing call site: `strength`/`calisthenics` gain an optional `weights: Map<MuscleGroup, Int> = emptyMap()` parameter. A single-muscle-group exercise needs zero changes. A multi-group exercise only needs an override for muscles that aren't 100% — e.g. `strength("Barbell Back Squat", MuscleGroup.QUADS, MuscleGroup.GLUTES, weights = mapOf(MuscleGroup.GLUTES to 60))`.

## Section 2 — Weighting methodology

Grounded in published EMG %MVC research where available, general biomechanics/exercise-science knowledge otherwise. Web research this session confirmed representative ratios (e.g. Bench Press: pectoralis major ~95% MVC, anterior deltoid ~79%, triceps ~67% — [Jefit EMG data summary](https://www.jefit.com/wp/exercise-tips/best-exercises-for-each-major-muscle-group-backed-emg-data/); deadlift/squat glute-hamstring activation patterns — [PLOS ONE deadlift EMG study](https://journals.plos.org/plosone/article/file?type=printable&id=10.1371/journal.pone.0229507), [MTSU muscle activation/volume comparison](https://jewlscholar.mtsu.edu/bitstreams/039afd05-3876-4403-a4d7-446686c2be32/download)). These inform a simplified three-tier convention (not literal %MVC, which measures instantaneous activation, not volume contribution):

- **Primary mover**: 100 (the muscle group the exercise is named/categorized for).
- **Major synergist**: 60-70 (a muscle doing real, substantial work — e.g. glutes in a squat, triceps in a bench press).
- **Minor stabilizer**: 25-40 (assists but isn't a target — e.g. abs in a farmer's carry, forearms in a row).

Applied consistently across **both** the ~180 new exercises and a retrofit pass over existing multi-group exercises in the current 119 (so the heatmap doesn't end up half-weighted depending on when an exercise was added).

## Section 3 — Library expansion

Grow from 119 to roughly 300 exercises, curated in per-category batches (Chest/Back/Shoulders/Arms/Legs/Forearms strength, Calisthenics, Cardio, Warmup) — equipment variants, unilateral work, machine-specific movements, and calisthenics progressions not currently covered. Each new exercise gets `category`, `loggingType` (per Project A's model — most new calisthenics entries are `WEIGHT_REPS`, isometric variants like Hollow Hold get `HOLD`), `muscleGroups`, and `muscleWeights` (Section 2's methodology) — curated together, not as a separate pass, so nothing ships half-classified.

## Section 4 — Heatmap clarity (labeling only)

Per the user's explicit choice, the underlying relative-to-self color model is unchanged. Add a caption under the "Muscle Distribution" section header in `ProgressScreen.kt`: *"Colors show volume relative to your most-trained muscle group this period"* — small `bodySmall`/`onSurfaceVariant` text, matching the style already used for other explanatory captions on that screen (e.g. the "No strength sets logged..." empty state).

## Verification

TDD for the `muscleWeights` `TypeConverter` and the updated `muscleGroupVolumeDistribution` weighting logic (real domain logic, same discipline as Project A). Library curation content itself isn't unit-testable exercise-by-exercise, but structural checks apply and get their own test: no duplicate exercise names, every `muscleWeights` key is a real `MuscleGroup` tagged in that exercise's `muscleGroups` (no orphaned weight entries pointing at an untagged group), and category/loggingType counts sanity-check against expectations (e.g. total exercise count lands near 300, HOLD exercises are all CALISTHENICS). On-device verification: confirm the Muscle Distribution section's caption renders, and spot-check that an isolation exercise (e.g. Leg Extension, QUADS-only at 100) visibly outweighs a compound exercise's partial-credit contribution (e.g. Squat's GLUTES at 60) in the radar chart / heatmap, where before they'd have looked identical at 100/100.

## Out of scope

- Changing the heatmap's underlying relative-to-self semantics (explicitly declined in favor of labeling).
- Any further library expansion beyond ~300 in this pass.
- A UI for editing weights on custom user-added exercises (custom exercises stay unweighted/defaulted to 100, same as any seed exercise not yet retrofitted — revisit only if raised).
