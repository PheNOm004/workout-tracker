# TimeGo — External Exercise Library Import (design)

**Date**: 2026-08-10
**Status**: approved, not yet implemented
**Context**: First of two sub-projects scoped out of a broader "ML for progressive overload" discussion. This one is independent of the ML work — a bigger, calisthenics-leaning exercise library is useful regardless of what happens with the suggester — so it ships on its own, same pattern as Project A/Project B earlier this session-chain. The second sub-project (LiftShift-inspired suggester upgrade, designed as the base for a future ML layer) gets its own spec afterward.

## Problem

TimeGo's 278-exercise seed library was curated by hand. The user found several external open-source exercise datasets (via Hugging Face/GitHub) with broader calisthenics coverage than the current library, and wants the useful ones merged in — both to close specific calisthenics gaps and simply to grow the library further, following the same curation discipline (muscle tagging, EMG-grounded weighting) as the last expansion.

## Sources

| Source | Format | Count | License | Notes |
|---|---|---|---|---|
| [RafaelJaime/calisthenics_exercises](https://huggingface.co/datasets/RafaelJaime/calisthenics_exercises) | CSV | 104 | CC0 (public domain) | Calisthenics-only. Has `muscle_groups`, `families`, `categories`, `materials`. Progression tiers encoded in names (Tuck/Adv/Negatives/One-Arm etc). No images. |
| [hasaneyldrm/exercises-dataset](https://github.com/hasaneyldrm/exercises-dataset) | JSON | 1,324 (~325 bodyweight) | MIT (data/text only — images/GIFs are separately © Gym visual, **excluded from this import**) | General library, all categories. Has `target`, `muscle_group`, `secondary_muscles`. |

Both sources are text/metadata-only imports — no media assets are pulled from either, sidestepping the Gym visual media license entirely.

## Scope

Import from **both categories** in both sources — calisthenics and general strength/equipment — not calisthenics-only, per explicit choice. Expected net new count after dedup: roughly 300-400 exercises (exact number depends on overlap found during curation), growing the library from 278 to an estimated ~600-680.

## Pipeline

Reuses the existing `SeedExercises.kt` builder pattern (`strength()`/`calisthenics()`/`cardio()`/`warmup()`) — no new import/loader infrastructure, since `Exercise` has no `equipment` field to carry through and the helpers already produce the right shape.

1. **Extract**: pull `name` + source muscle tags from both datasets (text fields only).
2. **Dedup**: normalize names (case, whitespace, common synonyms — e.g. "Push-Up" vs "Pushup" vs "Push Up") and diff against the current ~278-name list plus names being added within this same import (cross-source dedup too, since both datasets likely both have "Pull-Up"). Exact and close-name matches are skipped; only genuinely new movements are added.
3. **Progression tiers kept distinct**: RafaelJaime's tiered names (e.g. "Tuck Front Lever" / "Advanced Tuck Front Lever" / "Full Front Lever") each become their own `Exercise` entry, consistent with existing entries like "Tuck Planche" already in the library — each tier gets independent PR/progress tracking.
4. **Muscle group mapping**: both sources use coarser or differently-shaped muscle taxonomies than TimeGo's 17-value `MuscleGroup` enum. Map each source tag onto the closest TimeGo group(s) using the same movement-pattern judgment calls already established for the existing library (e.g. a generic "Back" tag splits into LATS/UPPER_BACK/LOWER_BACK by the specific movement's known emphasis, not a blind 1:1 rename).
5. **Category + loggingType classification**: `category` (STRENGTH/CALISTHENICS/CARDIO/WARMUP) determined by equipment tag (bodyweight → CALISTHENICS, equipment-based → STRENGTH, etc). `loggingType` determined by movement type — isometric/hold-named movements (planks, levers, flags, wall-sits, etc) get `HOLD`, everything else defaults `WEIGHT_REPS` unless clearly a cardio/duration movement.
6. **muscleWeights curation**: full EMG-grounded weighting for every new exercise, same three-tier convention as the last expansion (primary mover = 100, major synergist = 60-70, minor stabilizer = 25-40). Given the volume (300-400+ new entries, well beyond the last expansion's ~160), weighting is curated **by movement family** rather than researched individually per exercise — e.g. all front-lever progressions (Tuck/Adv Tuck/Straddle/Full) share the same lats/core/shoulder weighting ratios since the muscle recruitment pattern doesn't change across tiers, only the moment-arm difficulty does. Genuinely distinct movement patterns still get their own researched ratios.
7. **Append**: new entries added to `SEED_EXERCISES`. `seedMissingExercises` (name-matching, already built for exactly this append-only case from the last expansion) gets them onto the device without a Room migration — no schema change needed, this is pure seed-data growth.

## Verification

Same structural-test discipline as the last expansion's `SeedExercisesTest`:
- No duplicate exercise names (including cross-source duplicates caught in step 2).
- Every `muscleWeights` key corresponds to a `MuscleGroup` actually tagged in that exercise's `muscleGroups` (no orphaned weight entries).
- Weight values in the valid 1-100 range.
- HOLD-loggingType exercises are all CALISTHENICS (matches the existing invariant).
- Total exercise count lands in the expected ~600-680 range (sanity check against silent data loss or duplication bugs in the merge step).

Content curation itself (is this the *right* muscle group / weighting for a given exercise) isn't unit-testable — same limitation as the last expansion, mitigated by the movement-family-grounded methodology above rather than ad-hoc per-exercise guessing.

On-device verification: library size increase visible in the exercise picker, spot-check a few imported calisthenics progressions render correctly with expected HOLD/WEIGHT_REPS logging fields, confirm search/section grouping still works with the larger list.

## Out of scope

- Any media (images/GIFs) from either source — text/metadata only, avoids the Gym visual license entirely.
- Changes to `Exercise`'s schema or any Room migration — this is additive seed data only.
- The suggester/ML work — tracked as a separate sub-project spec.
- Re-weighting the *existing* 278 exercises — this pass only curates the new imports; the last expansion's retrofit already covered the prior library.
