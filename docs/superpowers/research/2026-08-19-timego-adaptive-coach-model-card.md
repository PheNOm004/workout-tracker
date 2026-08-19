# TimeGo adaptive coach — model card

**Status:** Offline research contract. It authorises neither a visible card nor a Kotlin runtime port until the chronological evidence gate passes.

## Intended use

Estimate current local training capability from ordinary TimeGo history. The first model is a small time-varying posterior over declarative movement-demand coordinates. It may estimate uncertainty or abstain; it does not prescribe a ladder, diagnose injury or recovery, infer medical readiness, or converse with the user.

The released app is self-contained on the phone: no account, cloud, telemetry, runtime download, LLM/chat interface, or laptop dependency.

## Evidence rules

| Source | Used only when | Never inferred from it |
|---|---|---|
| Closed session | It has deterministic end/start/session identifiers. | Future-session information or within-session held-out evidence. |
| Weighted reps | Non-warm-up with valid reps/load. `weightKg` is read once; calisthenics already stores total bodyweight plus added load. | A second load contribution from `addedWeightKg`. |
| Target result | Target provenance proves it existed before the set. | A hit/miss from an auto-filled or unknown-provenance target. |
| Hold | Non-warm-up with positive duration. | An E1RM-equivalent score. |
| Cardio | Positive duration plus distance produces pace. | Stamina capability from duration-only volume. |
| RPE/body metrics | They are present at the relevant observation. | A default effort value or exact same-day order. |
| Exercise metadata | A versioned immutable catalogue key and reviewed attributes exist. | A key derived from Room IDs, display names, ranks, or prerequisite paths. |

The contract records explicit exclusions for warm-ups, missing/invalid measurements, modality mismatches, unknown target outcomes, unknown metadata, and duration-only cardio.

## Model and metadata

The prototype uses a diagonal Bayesian-style logistic posterior. It widens variance over time gaps without assuming a physiological detraining rate. A binary update requires a genuine pre-set target outcome and identified task demand. Unknown demand or a wide interval produces abstention.

The model output is restricted to local state, confidence interval, factual evidence, or an abstention/exclusion reason. It has no `nextExercise`, rank, prerequisite, or progression field.

The review-only v1 draft is [`metadata/adaptive-coach-catalogue.2026-08-19.v1.json`](../../ml-prototype/metadata/adaptive-coach-catalogue.2026-08-19.v1.json). It contains immutable keys, modality, fixed demand coordinates, equipment, bodyweight form, and static exclusions. Loader tests reject progression-shaped fields and require repeated measurement coverage. The draft is not yet mapped to Room seed rows, so real logs remain excluded from the learner.

## Evaluation, privacy, and versioning

Each backtest holds out a completed session and uses only earlier sessions, ordered by session end, session ID, then set ID. The model is compared with same-task last-observation and EWMA baselines. Promotion requires enough real temporal boundaries, genuine target outcomes, reviewed seed-to-catalogue mapping, calibration/interval coverage, and counterexample review. The current history fails that gate, so abstention is correct.

Raw logs and derived coach state stay on-device. Backup/device transfer are disabled. Private exports, database copies, reports, snapshots, and audit outputs are ignored by Git. A future derived cache must carry model contract, catalogue version/hash, ordering policy, and canonical source fingerprint; a mismatch invalidates and rebuilds it.
