# TimeGo adaptive coach — research and decision brief

**Date:** 2026-08-19
**Status:** R&D only — no product behaviour or app code approved by this note
**Question:** How can TimeGo *learn* a user's level from normal local workout logs and recommend suitable strength, calisthenics, and stamina work without a chatbot, a fixed exercise ladder, cloud services, telemetry, or runtime downloads?

## Executive conclusion

The first researchable system is an **on-device probabilistic capability estimator**, followed later by a separate recommendation policy. It is not a sequence such as “Australian pull-up → negative → pull-up”, and it must not use threshold rules as a substitute for learning.

The estimator represents the athlete as an uncertain, changing capability vector across movement domains. Every normal logged set updates that estimate. An exercise is represented by declarative attributes (movement demands, modality, equipment and logging form), not by a prescribed predecessor/successor graph. The scorer then ranks candidate exercises whose predicted challenge is appropriate **and whose uncertainty is acceptable**. Consequently, a real pull-up history is direct evidence about vertical-pull capacity; it cannot be discarded in favour of a prewritten “foundation” card.

This is deliberately a two-stage plan:

1. **Capability model:** learn and backtest what TimeGo can infer from historical set logs. It produces estimates and uncertainty, but does not yet make visible recommendations.
2. **Recommendation policy:** only after TimeGo records prospective recommendation exposures and later outcomes may it learn which recommendation works best for this user. A contextual bandit is a candidate for that later policy, not for retrospectively treating self-selected history as if it were experimental data.

The hard requirement is **fully offline/local**. Laptop-based Python/R&D work may use a private, read-only local export, but the shipped TimeGo app must be phone-only and self-contained: it performs history analysis, inference, model updates, recommendation selection, and state storage from Room data on the device. It must make no cloud inference/training calls, emit no telemetry, and download neither model nor data at runtime. Public data below is for offline R&D, optional calibration experiments, or a vendored-at-build-time exercise catalogue only.

## What the current data can and cannot teach

TimeGo already records the raw material needed for a serious prototype: exercise identity and category, weighted muscle tags, session/time, weight, reps, target reps, warm-up status, bodyweight, added calisthenics load, optional RPE, hold duration, and cardio duration/distance. `Exercise` currently has muscle weights and category, while `SetLog` provides the actual performance observations. That is a better starting point than a generic gym dataset because it is the actual person the app must serve.

It does **not** currently record all information needed to infer recommendation quality:

- A completed self-chosen set establishes a lower bound on what was performed; by itself it does not say whether that exercise was too easy, ideal, unsafe, selected because equipment was available, or a near-failure attempt.
- RPE is useful direct effort evidence when present, but it is optional. Missing RPE must remain unknown, never silently become an assumed effort level.
- A recommendation cannot learn from an exercise the user did not log: non-selection might mean lack of time/equipment rather than a bad recommendation.
- The current exercise fields do not distinguish movement pattern (for example vertical vs horizontal pull), unilateral/bilateral mechanics, equipment requirements, assistance/resistance direction, or whether the observation is a load/reps, hold, or cardio task.

Therefore, "normal logs only" is feasible, but it has an honest boundary: early updates are performance evidence rather than perfect evidence that a recommendation was optimal. The design must preserve uncertainty and avoid confident claims about unseen exercises.

### Minimal additional *stored* data, not additional user input

The system can stay input-free beyond ordinary logging by automatically storing model and exposure records:

| Record | Produced automatically | Why it is necessary |
|---|---|---|
| `ExerciseCapabilityMetadata` | bundled/maintained exercise catalogue | Lets the learner transfer evidence from a logged movement to a similar unseen one. Fields include modality, movement-demand vector, equipment, loading form, and safety/exclusion flags. This is metadata, **not** an ordered roadmap. |
| `CapabilityPosterior` | updated after completed sessions | Stores means, covariance/uncertainty, model version, and last update. Rebuilding from logs must give the same result. |
| `RecommendationEvent` | stored when the app displays a candidate | Captures candidate set, model version, score/uncertainty, and timestamp locally. Necessary to distinguish exposure from ordinary self-selection. |
| `RecommendationOutcome` | joined from later ordinary logs | Records only observed completion/performance. “Not logged” remains *unobserved*, not failure. |

No one needs to answer a questionnaire, converse with the app, or type a new subjective field. Existing `targetReps` can form an observed `achieved/missed` signal where TimeGo actually provided a target; optional RPE can refine that observation. For a recommendation to be learnable, it must include a concrete task/dose, not merely an exercise name. The system should initially keep the task conservative and report a lower-confidence estimate when it lacks effort evidence.

## Sources and data assessment

| Source | What it actually contains / licence | Useful for | Critical limitation / decision |
|---|---|---|---|
| [OpenPowerlifting data service](https://openpowerlifting.gitlab.io/opl-csv/bulk-csv.html) and [source/licensing](https://github.com/sstangl/openpowerlifting) | Nightly bulk competition-result CSV; the 2026-08-14 full download has 4,001,902 rows. Project documentation says its contributed CSV data is public-domain-dedicated. | Optional offline priors or plausibility checks for squat/bench/deadlift bodyweight-normalised competition performance; reproducing the existing `ml-prototype` experiment. | It is cross-sectional meet data: no ordinary sets, RPE, exercise variants, calisthenics, cardio, recommendation exposure, or personal longitudinal response. Do **not** claim it teaches pull-up/calisthenics proficiency. Do not package its 161 MB file or fetch it from the app. |
| [Free Exercise DB](https://github.com/yuhonas/free-exercise-db) and its [Unlicense](https://github.com/yuhonas/free-exercise-db/blob/main/LICENSE.md) | 800+ local JSON exercises. Schema has level, force, mechanic, equipment, primary/secondary muscles, category and instructions; data is public domain. | Candidate catalogue seed and crosswalk review; it can be vendored as a selected, versioned static asset if its licence/provenance remains acceptable. | The project's own README says some force/mechanic/equipment values are incomplete. Its `level` is editorial metadata, not an observed individual difficulty label; never turn it into a hardcoded progression path. |
| [wger source and API project](https://github.com/wger-project/wger) | Maintained exercise records with muscle/equipment relations; code is AGPL and exercise data has entry-specific Creative Commons licensing. | A reference ontology and a source to inspect during R&D. | Do not copy/bundle data without auditing every entry's licence/attribution. It is metadata, not training performance data, and its public API is incompatible with the no-runtime-download rule. |
| [FitRec / Endomondo dataset](https://cseweb.ucsd.edu/~jmcauley/datasets/fitrec.html) | 253,020 sensor-rich endurance workouts from 1,104 users (heart rate, speed, GPS, sport type, weather); academic-use-only/non-commercial redistribution restriction. | Separate future stamina-model experiments, particularly time-series and heart-rate research if TimeGo ever records corresponding signals. | It has no resistance-set/calisthenics records, and TimeGo currently lacks heart-rate/GPS signals. It must not be shipped or used to make claims about the existing logging schema. |
| [PERSIST on Zenodo](https://zenodo.org/records/7437230) | Resistance-training multimodal/RPE study: 12 healthy male trained participants reporting RPE after sets. | Unit-test/feature-engineering research around the relationship between set observations and perceived exertion. | Very small, demographic-specific, sensor-based data; not an exercise recommender dataset and not a population prior for TimeGo users. |
| [CRAN `strength_training_log`](https://search.r-project.org/CRAN/refmans/STMr/html/strength_training_log.html) | 144-row, one-athlete, 12-week strength log with load, reps and estimated RIR. | Parser and chronological-backtest fixture. | Programmed wave-loading of one athlete; too small and too structured to train or validate a general recommendation system. |

### Dataset conclusion

No inspected open source has the required joint structure: many people, long *set-level* strength/calisthenics histories, load/reps/RPE, exercise attributes, a user's changing state, recommendations shown, and later outcomes. This is not a gap to hide with a large neural network. The scientifically sound source of truth is TimeGo's own local longitudinal record, evaluated with time-respecting backtests and later prospective, local-only recommendation events.

OpenPowerlifting remains valid only in its narrow role. The existing shrinkage-regression prototype has value as a baseline for **next-performance prediction of known barbell lifts**; it does not meet the broader adaptive-coach requirement and cannot supply a calisthenics roadmap.

## Relevant algorithm evidence

### Capability estimation: closest useful analogue

Item-response / adaptive-testing systems estimate an unobserved person ability from observed interactions with items of differing difficulty, then select an informative next item. The clinical IRT review is a useful explanation of the person-ability/item-difficulty separation, although its clinical/assessment setting is not a fitness prescription ([Jones et al., 2022](https://pubmed.ncbi.nlm.nih.gov/36085544/)). This analogy is appropriate for **state estimation**, not because exercises are exam questions:

- athlete capability is latent and changes with training;
- exercises/tasks have attributes and an uncertain relative demand;
- a set outcome supplies noisy evidence;
- the posterior keeps both an estimate and how uncertain it is.

TimeGo needs a *multidimensional, time-varying* version rather than one global score. A low-dimensional movement vector is far more defensible than a deep model with one user's limited logs. Example dimensions for research only: horizontal push/pull, vertical push/pull, knee-dominant lower body, hip-dominant lower body, trunk/bracing, single-leg control, and aerobic duration. These are attributes, not stages of a progression.

At session `t`, a candidate design is:

```text
capability:      theta_t ~ Normal(mean_t, covariance_t)
exercise task:   x(e, dose) = declared movement/dose feature vector
response model:  P(achieved | theta_t, x) = sigmoid(theta_t · x - difficulty_e,dose)
update:          posterior(theta_t+1) from only the newly logged observation
```

For continuous observations (load/reps/hold/duration), first research a modality-specific likelihood rather than force an Epley 1RM into every exercise. In particular, bodyweight work should use total system weight where the app already stores it, while hold/cardio tasks need their own response scale. The prediction UI should expose a confidence band/“still learning” state, rather than translate a wide posterior into a definite exercise order.

This approach learns from logs. It can revise down after an observed miss/high-effort response and revise up after a stronger-than-expected response. It is not a hidden fixed threshold: the score emerges from posterior parameters and observed performance; fixed safety exclusions are separate product safeguards.

### Recommendation policy: later, not retrospectively faked

Contextual-bandit research is relevant when TimeGo has a logged action (which candidate was shown), context, and reward/outcome. The offline-evaluation paper explicitly develops unbiased replay evaluation from **randomly logged action probabilities** ([Li et al., 2010](https://arxiv.org/abs/1003.5956)). Historic self-selected workout logs have neither actions selected by TimeGo nor their exposure probabilities. Training a bandit/RL policy on them would manufacture causal evidence it does not possess.

Once prospective local events exist, linear Thompson sampling is a compact, uncertainty-aware policy option ([Agrawal & Goyal, 2013](https://proceedings.mlr.press/v28/agrawal13.html)). HeartSteps is a closer health example: its Bayesian personalized activity model notes that sparse mHealth data makes rich nonlinear/nonstationary models impractical ([Liao et al., 2020](https://pmc.ncbi.nlm.nih.gov/articles/PMC8439432/)); its trial reports personalized activity suggestions, but is step-focused and not a resistance-training recommender ([Klasnja et al., 2019](https://pmc.ncbi.nlm.nih.gov/articles/PMC5719505/)).

This supports a strict sequencing decision: **do not call a contextual bandit the first model.** First establish that the capability estimator predicts held-out later performance reliably; then collect local prospective data; then consider the bandit inside a safety-filtered candidate pool.

## Candidate approach comparison

| Approach | Learns from user history? | New-exercise transfer | Uncertainty | Honest suitability | Offline fit | Decision |
|---|---:|---:|---:|---:|---:|---|
| Fixed prerequisite ladder / tier thresholds | No | Only preauthored | No | No; repeats the rejected prototype error | Yes | Reject. |
| Current known-exercise E1RM trend + OPL shrinkage | Partly, for next known lift performance | Virtually none beyond S/B/D | No | No calisthenics/stamina selection | Yes | Keep only as a narrow backtest baseline. |
| k-NN/content similarity over muscle tags | Some | Yes | Weak unless added separately | Limited; similarity is not capability | Yes | Useful comparator and candidate-pool helper, not the core model. |
| Online multidimensional Bayesian/IRT-style capability model | Yes, after every logged task | Yes, through declared exercise attributes | Yes, inherent posterior variance | Yes, when it declines to overstate low-confidence candidates | Yes; pure Kotlin feasible | **Recommended first research target.** |
| Contextual bandit (linear Thompson/LinUCB) | Yes, but needs recommendation exposures/outcomes | Yes | Yes | Only after causal/prospective data exists | Yes; pure Kotlin feasible | Phase 2, not initial model. |
| Neural network / LiteRT train-on-phone model | Potentially | Depends on a population-trained model | Usually weak/external | Poor with one user's sparse data | Technically yes, operationally excessive | Reject for now. |

LiteRT is technically capable of Android inference and exposes a Kotlin runtime ([Google's Android guide](https://developers.google.com/edge/litert/android)). Its official on-device-training guide requires a specially constructed model with train/infer/save/restore signatures and identifies resource-intensive training that should run in the background ([Google's on-device-training guide](https://developers.google.com/edge/litert/conversion/tensorflow/build/ondevice_training)). That is an implementation option, not evidence that a neural model is warranted here. For a low-dimensional posterior, a pure Kotlin implementation avoids a model file, native dependency, conversion pipeline, and opaque online training while remaining fully offline and testable.

## Recommended R&D plan — model first, no feature implementation yet

### R0 — data audit and modelling contract

1. Export a **read-only, untracked** local copy of the user's TimeGo records; do not place workout data in Git.
2. Quantify coverage by modality/exercise: number of sessions, working sets, bodyweight observations, target-rep hits/misses, and non-null RPE. Report missingness rather than filling it.
3. Define one concrete observation contract per logging type and confirm that it can be derived with no extra user input. Explicitly record what is a lower-bound observation versus an achieved/missed outcome.
4. Create a versioned candidate metadata draft for a small subset of the existing exercise library. Each vector must be independently reviewable; no `nextExercise` field, rank number, or prerequisite chain is allowed.
5. Write a data dictionary and model card: input fields, transformations, intended use, uncertainty behaviour, excluded situations, and offline boundary.

**Gate:** stop if the existing records cannot produce a meaningful chronologically held-out evaluation. The correct result may be “insufficient evidence yet”, not fabricated recommendations.

### R1 — reproducible offline Python research prototype

1. Implement three comparable predictors with the same chronological/session-level splits: last-observation/EWMA baseline, current E1RM/shrinkage baseline where applicable, and the proposed Bayesian capability estimator.
2. Fit only to sessions before each test session. Never let a set from the held-out session leak into its own prediction.
3. Measure modality-appropriate predictive error, calibration, and interval coverage. Do not claim recommendation quality from an error metric alone.
4. Run ablations: direct-exercise history only; then + muscle metadata; then + movement metadata; then + optional RPE. The difference quantifies whether metadata really transfers learning.
5. Preserve every seed, model version and result summary, while keeping the raw personal export out of source control.

**Gate:** promote a model only if it beats the simple baselines on later unseen sessions, is calibrated rather than merely accurate on average, and has inspectable counterexamples. If it does not, improve data/observation design rather than integrating it into Android.

### R2 — shadow mode in the app

1. Port the validated mathematics—not a Python artefact—into pure Kotlin domain code with deterministic unit/property tests.
2. Persist posterior state locally in Room and rebuild it from a copied log history as a correctness check.
3. Run after a completed session off the UI thread; the landing screen remains unchanged. Store local `RecommendationEvent` candidates for research but show none yet.
4. Compare shadow predictions with subsequent normal logs on-device. Missing subsequent logs are censored/unobserved, not negative rewards.
5. Verify airplane-mode operation and inspect the release manifest/dependencies so no model-related network permission, telemetry SDK or dynamic model/data downloader is introduced.

**Gate:** enable a visual recommendation only after shadow outcomes show stable calibration and the user has reviewed explanation examples that match their real pull-up and strength history.

### R3 — visible learned recommendation, still no bandit

The selector takes the current Strength/Balanced/Calisthenics preference only as a **candidate-family preference**. It applies static availability/safety exclusions, then ranks by posterior-predicted task suitability and uncertainty. It can answer with “not enough evidence to recommend a new variation yet.” It must not show a prescribed ladder, suppress direct evidence of already logged exercises, or replace the existing muscle-group card; a recommendation is an additional component.

Each displayed suggestion is linked to its local event record. Normal logged sets later provide observations. Explanations are generated from factual evidence (“recent vertical-pull performance was above model expectation” / “estimate remains uncertain”) rather than from a language model.

### R4 — prospective policy experiment

Only after sufficient local recommendation events exist, compare a fixed learned scorer with a conservative linear Thompson policy inside the same hard safety filter. Retain a deterministic candidate-pool audit trail and keep exploration bounded by posterior confidence. Evaluation must use only locally stored event/outcome data; no telemetry upload is permitted.

## Non-negotiable product and scientific guardrails

- No chatbot, LLM, free-text coaching, cloud account, cloud inference/training, telemetry, runtime data download, or external API dependency.
- No fixed progression ladder, exercise rank, or “after X reps always recommend Y” logic masquerading as a learned system.
- Human-authored exercise attributes and safety exclusions are permissible *metadata/safety constraints*, but must be clearly separated from learned parameters and never encode the desired answer as a pathway.
- No external dataset is a substitute for the user's own historical validation. OpenPowerlifting is a narrow offline comparison/optional prior, not a universal fitness model.
- Never infer injury status, diagnosis, recovery or medical clearance from gym logs. The app should be able to withhold an uncertain recommendation.
- Do not make a recommendation-policy or RL performance claim before prospective exposure/outcome data exists.

## 2026-08-20 contract amendment — ordinary performance is evidence, not a fictional max

The first prototype originally reserved posterior updates for pre-set binary target outcomes. That
is still required for binary calibration, but it is too narrow as a description of what ordinary
TimeGo logs establish. A non-warm-up weighted-rep set directly demonstrates its recorded
load/repetition pair, and a hold directly demonstrates its recorded duration. The contract now
retains a per-exercise non-dominated load/repetition envelope and longest-hold summary with
timestamps. It makes no E1RM, maximum-effort, or readiness claim when RPE/RIR is absent.

This is consistent with continuous-response latent-trait modelling as a general measurement
approach ([Kern et al., 2024](https://doi.org/10.3102/10769986231184147)), while the direct-1RM
reliability literature does not justify silently treating ordinary submaximal logs as direct 1RM
tests ([Grgic et al., 2020](https://pmc.ncbi.nlm.nih.gov/articles/PMC7367986/)). The envelope is
therefore a conservative history feature; a candidate for a new exercise still needs declared
metadata, a broad task-residual uncertainty model, and an abstention path rather than a fixed
progression route.

### Closed-session continuous-learning implementation

The research harness now turns one closed session into at most one maximum completed work point
per known exercise and measurement basis. Positive recorded total load uses `log1p(load * reps)`;
where no total load was recorded, bodyweight work uses the separate `log1p(reps)` basis; holds use
`log1p(seconds)`. The first personal observation for an exercise/basis is a neutral baseline, not
transfer evidence. Only a later closed-session observation for that same exercise and basis may
update the small time-varying demand-coordinate state. This keeps zero-load bodyweight records
useful without inventing bodyweight or mixing reps-only work with loaded work. The current private
aggregate audit has 26 baselines and one such update. It is still far below the evidence needed to
claim a better predictor, calibrate it, port Kotlin code, or show a card.

## 2026-08-20 candidate-prior decision boundary

Fresh source review confirms that no inspected public source supplies a suitable learned difficulty
model for TimeGo's strength and calisthenics library. Free Exercise DB is public-domain and can be
used offline for equipment, force, mechanic, and muscle metadata, but its `level` field is
editorial; using it as readiness, rank, or a prerequisite would recreate the rejected hard-coded
roadmap ([source](https://github.com/yuhonas/free-exercise-db)). OpenPowerlifting is CC0 but only
covers competition squat, bench, and deadlift data, so it cannot calibrate calisthenics or the
broader library ([source](https://gitlab.com/openpowerlifting/opl-data/-/tree/main)). wger has
useful muscle/equipment relations, but its application and entry data have licensing constraints
that require per-entry review, and it supplies no personal task-performance prior
([source](https://github.com/wger-project/wger)).

Before a new exercise can be shown, the product needs one of two explicit approaches:

1. Strict own-history-only mode: use direct evidence and abstain for an unseen task until safe
   bridge evidence exists.
2. A conservative offline task-residual prior: a broad statistical uncertainty distribution that
   is not an exercise rank, level, or order. It can only pass a candidate when the entire
   uncertainty interval is safe; otherwise it abstains.

**Decision recorded 2026-08-20:** the user approved option 2. The implementation defaults the
prior to disabled, gives it no exercise-specific identity, and adds only its broad variance to a
candidate interval. It still abstains unless that entire combined interval is safe. Do not adopt
exercise `level` metadata, a population tier, or a deterministic fallback path as a substitute.
- Personal logs, database exports, model snapshots containing identifiable workout history, and backup files remain local and untracked.

## Decision requested after R0/R1

Before any Android feature work, review the prototype evidence against these questions:

1. Can the model predict later *actual* user performance better and more honestly than simple history baselines?
2. Does the candidate metadata provide useful transfer to related exercises without hardcoding a sequence?
3. Are confidence intervals wide when the history is sparse or a movement is unseen?
4. Does every model update and visible explanation remain reproducible from local logs alone, in airplane mode?

Only a supported “yes” earns the next implementation phase. Otherwise, the appropriate output is a documented R&D finding and continued normal logging—not a fake adaptive coach.
