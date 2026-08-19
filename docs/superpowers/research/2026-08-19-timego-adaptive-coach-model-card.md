# TimeGo adaptive coach — model card

**Status:** Offline research contract. It authorises neither a visible card nor a Kotlin runtime port until the chronological evidence gate passes.

## Intended use

Estimate current local training capability from ordinary TimeGo history. The first model is a small time-varying posterior over declarative movement-demand coordinates. It may estimate uncertainty or abstain; it does not prescribe a ladder, diagnose injury or recovery, infer medical readiness, or converse with the user.

The released app is self-contained on the phone: no account, cloud, telemetry, runtime download, LLM/chat interface, or laptop dependency.

## Evidence rules

| Source | Used only when | Never inferred from it |
|---|---|---|
| Closed session | It has deterministic end/start/session identifiers. | Future-session information or within-session held-out evidence. |
| Weighted reps | Non-warm-up with valid reps/load. `weightKg` is read once; calisthenics already stores total bodyweight plus added load. A time-stamped non-dominated load/repetition envelope records directly demonstrated performance. | A second load contribution from `addedWeightKg`, or a maximum-strength estimate when effort/RPE is absent. |
| Target result | Target provenance proves it existed before the set. | A hit/miss from an auto-filled or unknown-provenance target. |
| Hold | Non-warm-up with positive duration. A summary retains the longest demonstrated hold and the freshness of the latest evidence. | An E1RM-equivalent score or an assumed all-out effort. |
| Cardio | Positive duration plus distance produces pace. | Stamina capability from duration-only volume. |
| RPE/body metrics | They are present at the relevant observation. | A default effort value or exact same-day order. |
| Exercise metadata | A versioned immutable catalogue key, category, declared muscle contribution weights, and reviewed candidate attributes exist. | A key derived from Room IDs, display names, ranks, or prerequisite paths. |

The contract records explicit exclusions for warm-ups, missing/invalid measurements, modality mismatches, unknown target outcomes, unknown metadata, and duration-only cardio.

## Model and metadata

The prototype has three evidence layers. A conservative performance envelope learns only what ordinary weighted-rep and hold logs directly demonstrate; it is rebuilt from history and reports its freshness. A continuous ordinary-log layer reduces each closed session to one maximum work point per exercise and measurement basis: `log1p(load * reps)` for positive recorded total load, `log1p(reps)` where no total load was recorded, and `log1p(hold seconds)` for holds. These are monotonic records, never E1RM or all-out estimates. Its first same-exercise/basis point registers a neutral personal baseline; only a later point on that same basis updates the time-varying demand-coordinate state. Reps-only, loaded, and hold bases are never mixed. A diagonal Bayesian-style logistic posterior is reserved for genuine pre-set target outcomes with identified task demand. It widens variance over time gaps without assuming a physiological detraining rate. Unknown demand or a wide interval produces abstention.

The model output is restricted to local state, confidence interval, factual evidence, or an abstention/exclusion reason. It has no `nextExercise`, rank, prerequisite, or progression field.

### Approved unseen-task uncertainty prior

The user approved an offline broad task-residual prior on 2026-08-20. It is disabled unless a
model configuration explicitly supplies a positive variance. When enabled it has no exercise name,
level, rank, or order: its mean is zero and its configured variance is added to the candidate
interval. A candidate using it must carry an audit flag and is shown only if the *entire combined
capability-plus-task interval* passes the normal uncertainty gate. Ordinary capability and unknown
metadata still abstain. This is a cold-start uncertainty assumption, not a hard-coded pathway.

The review-only v1 draft is [`metadata/adaptive-coach-catalogue.2026-08-19.v1.json`](../../ml-prototype/metadata/adaptive-coach-catalogue.2026-08-19.v1.json). It contains immutable keys, modality, fixed demand coordinates, equipment, bodyweight form, and static exclusions. Loader tests reject progression-shaped fields and require repeated measurement coverage. Schema 13 now persists a `timego.seed.v1.*` key for every built-in seed row, and the offline adapter uses that key—not a mutable display name or Room ID—to recognise current history. It also converts Room's stored per-muscle percentages into normalised demand weights, defaulting an undeclared primary muscle to 1.0. This establishes direct historical identity and demand coverage. The reviewed v1 candidate set remains deliberately narrow; no unreviewed exercise becomes safe to recommend from it.

## Evaluation, privacy, and versioning

Each backtest holds out a completed session and uses only earlier sessions, ordered by session end, session ID, then deterministic set/key/basis ties. The continuous evaluator freezes the pre-session state, predicts each later same-exercise/basis work score, compares it with the same-basis last-observation baseline, and applies the held-out session only after scoring it. Its report is aggregate-only: MAE/RMSE, prediction boundaries, registrations, abstentions/insufficient-evidence counts, and a winner only when at least one boundary exists. Ordinary performance is evaluated against same-task historical envelopes and this continuous learner; binary target calibration is evaluated only where a genuine target outcome exists. Promotion requires enough real temporal boundaries, reviewed candidate metadata, calibrated uncertainty where binary labels exist, and counterexample review. The current read-only export has three closed weighted-evidence sessions, 26 neutral exercise/basis baselines, one later prediction boundary/update, and zero later same-exercise envelope comparisons. At that one boundary the candidate ties the baseline (aggregate MAE 0.1164); this is not a validation score, so no Android shadow port or visible card is authorised.

Raw logs and derived coach state stay on-device. Backup/device transfer are disabled. Private exports, database copies, reports, snapshots, and audit outputs are ignored by Git. A future derived cache must carry model contract, catalogue version/hash, ordering policy, and canonical source fingerprint; a mismatch invalidates and rebuilds it.
