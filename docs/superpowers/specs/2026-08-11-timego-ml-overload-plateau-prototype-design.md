# TimeGo — ML Overload/Plateau Prototype (design)

**Date**: 2026-08-11
**Status**: approved, not yet implemented
**Context**: The ML model deferred out of Sub-project 2 (`suggester-plateau-upgrade`) — that sub-project shipped a rules-only base layer designed specifically so a model could later replace it behind the same `OverloadSuggester`/`HoldSuggester` interface. This design covers only the **offline Python prototype**: proving a shrinkage-regression approach beats the current rule-based suggester on backtested data. It is explicitly not an on-device Kotlin port — that's a separate, later sub-project, scoped only if this prototype validates.

## Problem

`RuleBasedOverloadSuggester` picks from 3 fixed outputs (deload 10% / +2.5kg / +1 rep) and the newer `PlateauStatus` classification uses a first-half-vs-second-half average comparison over a 5-set window. Both are reasonable heuristics but ignore two available signals: (1) your own per-exercise trend shape beyond a simple average split, and (2) population-level strength-progression data (OpenPowerlifting) that could inform predictions before you have much personal history on a given exercise. The goal of this prototype is to check whether a lightweight statistical model exploiting both signals actually outperforms the existing rules — before any app code changes.

## Section 1 — Data pipeline

Two inputs, both offline/local, no network calls at runtime:

- **OpenPowerlifting** (via the previously-scouted `sergeimakarovv/ML-Powerlifting` Kaggle-sourced CSV) — Sex/Age/Bodyweight/lift-performance records across thousands of lifters. Used only to build bodyweight-normalized strength-progression priors per broad lift category (squat/bench/deadlift-style movements); it's cross-sectional, not longitudinal, so it cannot inform trend shape, only typical-strength-for-bodyweight priors.
- **Personal `SetLog` export** — one-time dump of the local Room DB (`adb shell` SQLite export is sufficient for a prototype; no in-app export feature needed at this stage) into a flat CSV/JSON the notebook reads directly. Minimum fields: exercise name, date, weight, reps, estimated 1RM (or recompute via the existing Epley formula in Python).

Both loaded and joined in the notebook by exercise category, not by exercise name (the personal library's naming won't match OpenPowerlifting's three lifts).

## Section 2 — Overload target model (shrinkage regression)

Per exercise, weighted linear regression of estimated-1RM against session index gives a personal trend line. The predicted next-session 1RM blends this with the population prior:

```
predicted_1rm = w * personal_trend_prediction + (1 - w) * population_prior
w = n / (n + k)
```

- `n` = number of logged sets for that exercise so far.
- `k` = a tunable constant controlling how fast personal data overtakes the prior (start with `k = 10` as a rough default, tune empirically during backtesting).
- `population_prior` only exists for exercises mappable to a broad OpenPowerlifting category (squat/bench/deadlift-style compounds); for anything else (isolation work, most calisthenics), `w = 1` always — pure personal-trend regression, no prior available. This is a real coverage gap, noted in Out of scope.

Predicted 1RM is converted back to a weight/rep suggestion the same way `RuleBasedOverloadSuggester`'s existing note-generation does (not redesigned here — only the number feeding it changes).

## Section 3 — Plateau detection model (slope-based)

Same 5-set rolling window `PlateauStatus` already uses, but reclassified via a regression slope + confidence interval instead of the average-comparison heuristic:

- Fit a simple linear regression of estimated-1RM (or hold duration) against set index over the window.
- **PROGRESSING**: slope significantly positive (CI excludes zero).
- **PLATEAUING**: slope not significantly different from zero.
- **REGRESSING**: keeps today's exact rule (2 consecutive misses) unchanged — this path already works and isn't being second-guessed by the model.
- **Fallback**: fewer than 5 sets → same last-2-sets fallback as today. No change to the fallback path.

## Section 4 — Evaluation

Backtest, not live A/B (no live users to A/B against): for each historical logged set beyond the first few per exercise, hold it out, generate a prediction from (a) the current rule-based suggester and (b) the shrinkage-regression model using only data prior to that set, and compare each prediction's distance from what was actually logged next. Aggregate error (e.g. mean absolute error on predicted weight/reps) per exercise and overall. Plateau classification compared similarly: does the slope-based call match what a human glancing at the same window would call it (spot-checked manually, not a formalized metric — no ground-truth plateau labels exist).

No formal train/test split beyond the backtest's natural time-ordering (each prediction only sees prior sessions) — the personal dataset is too small for a further held-out split to be meaningful.

## Section 5 — Deliverable

A single Jupyter notebook, not app code: `TimeGo/ml-prototype/overload_plateau_prototype.ipynb`. Environment: existing Anaconda3/Jupyter setup (already used for MDA522 coursework), pandas + numpy + scikit-learn (LinearRegression is sufficient — no need for a heavier ML library at this stage). Output of this phase is a verdict — does shrinkage regression + slope detection beat the rule-based baseline on real backtested data — not a shipped feature. A Kotlin/on-device port is only scoped as a new sub-project if the answer is yes.

## Out of scope

- Any Kotlin/Android code changes — this is a Python-only prototype. On-device port, `OverloadSuggester` interface implementation, and Room schema changes are all deferred to a future sub-project, contingent on this prototype validating.
- Population priors for non-compound exercises (isolation lifts, most calisthenics) — OpenPowerlifting only covers squat/bench/deadlift-style movements, so most of the 585-exercise library gets no prior (`w = 1` always). A broader prior source is a future problem, not solved here.
- Formal statistical significance testing on the plateau classification (no ground-truth "was this actually a plateau" labels exist to test against) — evaluation is backtested prediction accuracy plus manual spot-checks only.
- An in-app data-export feature — the personal-data export for this prototype is a one-time manual `adb`/SQLite dump, not a shipped feature.
- Retraining cadence / model refresh strategy — irrelevant until there's an on-device deployment to refresh.
