# TimeGo — Calisthenics BW+k weight model (design + plan)

Item 5 (last) of the 8-item post-v1 backlog. Combined design+plan doc (shortened process, per
established pattern from items 3-4).

## Problem

Bodyweight (CALISTHENICS) exercises store/display `weightKg` as a raw absolute number (e.g.
`61.8`), inferred by pre-filling the entry field with the user's latest logged body weight
(editable, e.g. to add a weighted vest). Records/summary/PR display should instead read as
`BW + k` (added weight only) rather than a computed absolute kg number.

## Design

**Storage**: domain math (1RM, PR ranking, strength curve, the overload suggester) genuinely needs
the *total* load lifted, not just k alone -- k=0 alone would flatten 1RM to zero, exactly the bug
the original bodyweight pre-fill fix avoided. So `weightKg` keeps storing bodyweight+k unchanged;
`SetLog` gains a new nullable `addedWeightKg: Double?` (null for non-bodyweight exercises) purely
for display formatting. Room migration 8→9 (`ALTER TABLE set_logs ADD COLUMN addedWeightKg REAL`).

**Formatting** (`domain/CalisthenicsWeight.kt`, pure Kotlin, TDD):
`fun formatCalisthenicsWeight(addedWeightKg: Double): String` — `"BW"` when `addedWeightKg <= 0`,
else `"BW + %.1fkg".format(addedWeightKg)`.

**Entry UI** (`StrengthLogRow` in `LogScreen.kt`): for CALISTHENICS exercises the kg field becomes
an "added weight" field — blank/0 default, placeholder "0", no longer pre-filled with bodyweight.
On log: `weightKg = (latestBodyWeightKg ?: 0.0) + k` (unchanged storage contract), `addedWeightKg =
k`. `onLog` callback signature gains a trailing `addedWeightKg: Double?` param (null for
non-bodyweight rows). `LogViewModel.logSet` threads it through to `WorkoutRepository`/`SetLog`.

**Display sites** (CALISTHENICS exercises only, gated on `exercise.category ==
ExerciseCategory.CALISTHENICS.name`):
- `ProgressScreen`'s PR card "Weight" tile → `formatCalisthenicsWeight`. "Total Weight" tile stays
  a plain numeric volume figure (weight×reps isn't a literal-load statement).
- `buildDayHistoryEntries` (shared by the heatmap day dialog + landing-page last-session dialog) →
  `formatCalisthenicsWeight` per set instead of `"${weightKg}kg"`.
- `LogScreen`'s suggestion hint/note for `StrengthLogRow` → derives k as `suggestion.weightKg -
  (latestBodyWeightKg ?: suggestion.weightKg)` for display only; the suggester's own domain logic
  is untouched (still computes/returns an absolute `weightKg` target as before).

`PersonalRecord` gains an `addedWeightKg: Double?` field, populated by `personalRecords()` from the
winning BEST_SET's `SetLog.addedWeightKg` when the exercise is CALISTHENICS.

## Tasks

1. `SetLog.addedWeightKg` + Room migration 8→9. `PersonalRecord.addedWeightKg` +
   `personalRecords()` threading. `formatCalisthenicsWeight` in domain with unit tests (k=0 → "BW",
   k>0 → "BW + X.Xkg", k<0 clamped display same as 0 -- shouldn't occur but formatting must not
   show a negative).
2. `StrengthLogRow` entry UI: added-weight field for CALISTHENICS, `onLog` signature change,
   `LogViewModel.logSet` threading, suggestion-hint k-derivation display.
3. Display formatting at the 3 read sites: PR card, `buildDayHistoryEntries`, done together since
   they're small, mechanical changes gated on the same `isBodyweight` check already used elsewhere
   in the codebase.

Each task: implement, `./gradlew testDebugUnitTest` + `assembleDebug`, commit individually on a
`calisthenics-bw-plus-k` branch. `installDebug` + on-device verification once all tasks are done —
log a calisthenics set with added weight, confirm PR card/history dialogs/suggestion hint all show
`BW + k`, confirm a plain bodyweight set (k=0) shows just `BW`, confirm strength-training exercises
are unaffected — fast-follow fixes from that feedback, then merge to `master`, delete branch,
update vault note (and mark the 8-item backlog fully complete).
