# TimeGo adaptive coach — deep R&D findings and required plan revisions

**Date:** 2026-08-19  
**Scope:** Evidence review only. No Android feature, model, UI, personal-data audit, or recommendation behaviour is authorised by this note.

## Bottom line

The proposed first learner remains the right *class* of approach: a small, probabilistic, time-varying capability estimator in pure Kotlin, followed by a separate recommendation policy only after prospective local evidence exists. It should **not** become a neural model, an LLM, a fixed progression graph, or an early bandit.

However, the plan needs several concrete revisions before implementation. The most important are:

1. **Model capability as a state that transitions with elapsed time, not only when a session is logged.** A dynamic IRT model is the closest technical analogue: it uses a time-varying latent ability and accommodates uncertain task difficulty; it can be used retrospectively or online. [Wang, Berger & Burdick (2013)](https://arxiv.org/abs/1304.4441)
2. **Make identifiability explicit.** One person’s self-selected logs cannot independently discover both a universal exercise-difficulty scale and that person’s capability. The initial model needs declared feature vectors, an anchored coordinate system, regularisation/priors, and uncertainty; it must abstain for a new movement where the evidence does not identify an answer.
3. **Do not put stamina into the first visible capability claim unless the local audit verifies a usable intensity/outcome signal.** TimeGo currently stores cardio duration and optional distance, but no planned cardio target, RPE, heart rate, grade, or route/context. `duration + distance` can derive pace; duration without distance is only a volume/lower-bound observation, not a calibrated capacity or success outcome.
4. **A card impression is not an observed recommendation outcome.** To learn a future policy without additional user input, TimeGo must automatically distinguish a card being shown from the user starting that suggested task from the card. A later freeform log must remain ordinary history, not be labelled as a policy reward.
5. **For literal phone-only/no-cloud privacy, disable the current Android Auto Backup configuration or explicitly exclude every coach/log/model store from both cloud backup and transfer.** The present manifest sets `android:allowBackup="true"`, and its empty rules allow the default backup behaviour. Android documents that Auto Backup uploads app data to Google Drive and includes app database files by default. [Android Auto Backup](https://developer.android.com/identity/data/autobackup)

These are design corrections, not reasons to abandon the low-dimensional Bayesian/IRT research track.

## 1. A sound first learner: dynamic, sparse, and identifiable

### What transfers from dynamic IRT—and what does not

Dynamic IRT is valuable because it separates a latent, changing state from noisy task observations and preserves uncertainty. Its authors specifically identify repeated observations through time, non-independent responses, and partially specified/uncertain task difficulties as modelling complications. [Wang, Berger & Burdick (2013)](https://arxiv.org/abs/1304.4441) That maps well to workout logs: sets within a session are correlated, exercises have partially declared demands, and a person’s current capacity is not constant.

It is an analogy for **measurement**, not a claim that exercises are test questions or that a fitness app can infer medical readiness. The production contract should be a deliberately small state-space model:

```text
before session t: theta_t ~ transition(theta_{t-1}, elapsed_time, process_noise)
observation:      y_t ~ modality-specific likelihood(theta_t, task_features, dose)
after session t:  posterior(theta_t | old posterior, observed session)
```

The transition must grow uncertainty across time gaps. It must not assert a particular rate of detraining or a physiology claim without evidence. A mean-reversion/decay rule is therefore a candidate to backtest, not an assumed product truth.

### The identifiability boundary is a real first-model gate

For a single person, a model can explain a missed task either as lower user capability or higher task difficulty. With no anchors, those explanations are interchangeable up to a shift/scale. A candidate unseen exercise has no individual outcome, so its difficulty cannot be learned directly from that person’s history.

The initial contract must therefore include all of the following:

- A small, reviewed feature vector for task demands (movement dimensions, logging modality, equipment/loading form), never a prerequisite order or progression rank.
- One fixed coordinate convention, such as zero-centred features plus a small set of frequently repeated logged movements chosen as **measurement anchors**. An anchor is not a recommended starting exercise or a user-facing ladder.
- Strong regularisation and an explicitly broad prior for exercise-specific residual difficulty. The model may transfer only through declared similarities; it must retain wide intervals when similarity is weak.
- A model-versioned metadata snapshot/hash. If a catalogue attribute changes, a historical prediction must remain explainable under the catalogue that produced it—or the snapshot must be deliberately rebuilt and marked as a new model result.

**Revision:** add identifiability/anchor tests to R1. Synthetic cases must prove that the implementation abstains when the evidence cannot distinguish capability from task difficulty, rather than silently returning an arbitrary ranking.

### Observation contracts must remain modality-specific

The proposed separation of weighted repetitions, holds, and cardio is necessary, but the scope needs tightening:

| Modality | Initial evidence that can be used | Boundary that prevents an overclaim |
|---|---|---|
| Weighted reps | completed load/reps; target-rep hit/miss when a target exists; optional RPE; bodyweight already captured in stored calisthenics total load | A self-selected completed set is a lower bound, not proof that the dose was ideal. Missing RPE is unknown. |
| Holds | achieved/target duration, excluding warm-ups as defined by the audit | Treat holds as their own likelihood; do not convert them to E1RM. |
| Stamina | only `duration + distance` can yield an objective pace-like observation with the current schema | Duration-only history lacks intensity and an explicit target/outcome. It may describe recent volume, but the first learner must abstain from a stamina-capability recommendation unless the audit proves a valid task/outcome subset. |

**Revision:** R0 must audit *target availability and outcome coverage per modality*, not merely number of rows. R1’s first visible-candidate research should be limited to weighted reps and holds unless the audit supports the stamina contract. “Stamina later” is an acceptable, honest result.

## 2. Evaluation: chronological splitting is necessary but not sufficient

Chronological, session-level splits prevent future information leaking into predictions. They do not establish that a recommendation would cause a better outcome. Contextual-bandit feedback is partial-label: an outcome is observed only for the action actually taken. The original replay evaluation work obtains an unbiased result under randomized logged choices; historical self-selected TimeGo workouts have neither TimeGo actions nor logging propensities. [Li, Chu, Langford & Wang (2010)](https://arxiv.org/abs/1003.5956)

The prior research brief currently links this paper using the wrong arXiv identifier (`1003.0146`). The correct primary source is [`1003.5956`](https://arxiv.org/abs/1003.5956). This must be corrected wherever cited.

### Required two-level evaluation

1. **Capability-model backtest:** at each chronological test boundary, rebuild/score using only earlier *closed* sessions, with a deterministic tie-breaker (`session end`, then session ID, then set ID). Report predictive error, calibrated probability only where an achieved/missed target is real, interval coverage, and abstention. Keep all sets from a session on one side of the split; otherwise fatigue/order information leaks.
2. **Policy evaluation:** defer causal claims. When a visible card exists, create a local event at both display and explicit automatic action (for example, “started from this card”). Attach a later logged task only to the action event and its declared dose/window. A card merely displayed without an action is exposure/censoring data, not a failed reward. A freeform session is not a policy trial.

The second record does not add a questionnaire or conversational input. It records existing app interaction and normal logs automatically. It is required because absence of a log can mean that the user was busy, lacked equipment, dismissed the card, or chose another session—not that the suggested exercise failed.

**Revision:** replace the loose instruction to “join future normal logs” with the action-linked outcome contract above. Store full candidate set, deterministic eligibility result, selected candidate/action, task/dose, display/action timestamps, model and metadata versions, and—only if future randomised exploration is approved—the selection probability. Do not run an offline policy comparison without those fields.

## 3. Reproducible local state: raw logs are truth, posterior is a cache

The existing code already has a Room database, exported schema history, session close timestamps, and a session deletion path. It also seeds curated exercise metadata by name and updates existing non-custom rows. Those realities make an incremental posterior alone unsafe: a session can be deleted, a seed attribute can change, an app upgrade can change model maths, and a background job can run twice or finish late.

The architecture should be event-sourced in the limited, practical sense below:

- **Canonical truth:** existing Room logs/sessions/exercises plus versioned coach metadata. Do not overwrite or mutate old logged observations to “fit” the model.
- **Derived cache:** one current `coach_model_state` containing contract/model version, metadata version/hash, source boundary/hash, deterministic ordering policy, quantised posterior payload, uncertainty, and completion status.
- **Audit:** append-only local `coach_learning_audit` records for each rebuild/update attempt and result; event records begin only when a card is shown.
- **Invalidation:** any log/session deletion, relevant metadata change, model version change, or detected source-boundary mismatch marks the cache stale. Rebuild deterministically from canonical data; never try to hand-write inverse posterior updates for deleted/edited history.
- **Atomicity:** compute from a read snapshot; write the matching state/audit record in a Room transaction only if its source revision is still current. Otherwise discard and retry/rebuild. User-facing computation reads a matching cache or abstains/rebuilds; it never presents stale certainty.

Room’s official guidance requires incremental migrations to preserve on-device data, recommends exported schema history for migration tests, and warns that destructive fallback permanently deletes tables. [Room migration guidance](https://developer.android.com/training/data-storage/room/migrating-db-versions) TimeGo already exports schemas; its coach tables must add explicit migrations and test all supported migration paths. Do not introduce a destructive fallback.

Use Room rather than DataStore for complex posterior/audit/event records: Android documents DataStore as appropriate for small datasets and notes it does not support partial updates or referential integrity. [DataStore guidance](https://developer.android.com/topic/libraries/architecture/datastore)

**Revision:** add a stable, immutable catalogue key and metadata-versioning design before snapshot persistence. Do not use Room auto IDs or mutable names as the cross-release identity of a seed exercise. Capture the metadata hash on every state/event record. Add delete-session, seed-metadata-change, model-upgrade, duplicated-update, and rebuild-equals-incremental tests to the Kotlin plan.

## 4. Android execution without turning “offline” into “eventually wrong”

The initial low-dimensional update should run as ordinary app-local coroutine work after the canonical session-close transaction, not depend on a scheduler. WorkManager is intended for persistent work that can run after constraints are met; its execution is deferrable. [WorkManager reference](https://developer.android.com/reference/androidx/work/WorkManager) It is not the learning system’s source of truth.

If a future full rebuild is large enough to survive app/process death, use **one unique, idempotent local one-time worker** as a reconciler. Android’s unique-work API is specifically meant to prevent duplicate active chains. [Manage unique WorkManager work](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work) Pass only a revision/job token, re-read Room inside the worker, and atomically reject stale writes. Use no network constraint; do not schedule the post-session update as periodic or charging-only work. Test a duplicate job, cancellation before commit, and stale-result race. Android provides `work-testing` support for worker tests. [WorkManager integration testing](https://developer.android.com/develop/background-work/background-tasks/testing/persistent/integration-testing)

## 5. Literal phone-only / no-cloud boundary

The current `AndroidManifest.xml` explicitly enables backups and references default-empty backup rule files. Android’s documented default includes app databases, files, and shared preferences, and can upload the backup to the user’s Google Drive. [Android Auto Backup](https://developer.android.com/identity/data/autobackup) That conflicts with the user’s stated “phone only / fully local” boundary, even though no TimeGo feature itself calls a cloud API.

**Required product decision before implementation:** choose and document one of these—not an accidental default:

- **Strict phone-only (recommended for the stated goal):** disable Auto Backup and explicitly prevent cloud backup plus device/cross-platform transfer of the workout DB, coach state, DataStore, and any export/cache. This maximises locality but means app uninstall/loss of phone loses the local history unless the user later asks for a deliberate, manual local export feature.
- **Local-device-transfer exception:** still exclude cloud backup but explicitly allow device-to-device transfer. This is not “phone only,” so it needs user approval and explicit copy/version compatibility tests.

No product code should add `INTERNET`, a telemetry SDK, remote configuration, dynamic model delivery, or a runtime catalogue/model download. Airplane-mode verification must include a cold start, update/rebuild, recommendation calculation, and all current normal logging paths—not only a screen screenshot.

## Required plan changes before execution

| Plan area | Replace/add | Why it is required |
|---|---|---|
| R0 data contract | Audit target/outcome coverage, cardio `duration+distance`, bodyweight correspondence, not just record counts. | Avoid calling lower-bound activity evidence a calibrated capability label. |
| R1 model | Add time transition/process uncertainty, fixed coordinate/anchors, metadata prior, and synthetic identifiability/abstention cases. | Makes sparse transfer a measurable learner rather than an arbitrary ranking. |
| R1 evaluation | Enforce closed-session temporal boundaries and deterministic ordering; correct the Li paper link. | Prevents within-session/future leakage and source confusion. |
| R2 persistence | Treat posterior as versioned cache; canonical Room data rebuilds it. Add revision/hash, immutable catalogue key, transaction, migrations and invalidation. | Handles deletion, upgrades, metadata drift, retries and reproducibility. |
| R2 scheduling | Direct local update for small work; only unique/idempotent WorkManager reconciliation for expensive rebuilds. | Offline state stays correct despite deferred/cancelled background work. |
| R3 outcomes | Record display **and** automatic “started from card” action; match a specified later task/dose/window; censor all other cases. | Self-selected future logs alone cannot validate the policy. |
| Offline product boundary | Decide strict backup/transfer policy and test it; current backup defaults are incompatible with strict no-cloud. | Protects personal training data and model state from unintended upload. |

## Recommendation

Proceed to the planned private, read-only R0 audit only after these revisions are merged into the design. The initial model should be permitted to return “insufficient evidence,” particularly for stamina and unseen exercise metadata. That is evidence of correct uncertainty handling—not a reason to fall back to a hardcoded roadmap.
