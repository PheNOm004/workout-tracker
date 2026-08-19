# TimeGo adaptive coach — R&D execution plan

**Status:** Research implementation is underway. The offline observation contract, synthetic
fixtures, time-aware capability prototype, chronological split harness, and simple-history
comparators are implemented and unit-tested. No Android recommendation UI, roadmap, or runtime
model is authorised by this document until the real-history gate passes.

**Goal:** Determine, using only private local data and time-respecting evaluation, whether TimeGo can learn an uncertainty-aware capability estimate from normal logs and use it to rank suitable strength, calisthenics, and stamina tasks.

**Product boundary:** The shipped app is self-contained on the phone. It reads Room data, updates model state, selects candidates, and stores results locally. Laptop work is permitted only for private R&D on a read-only local export; it is not a product dependency. No cloud inference/training, telemetry, chatbot, LLM, account, runtime download, or external API is in scope.

**Research basis:** [Decision brief](../research/2026-08-19-timego-adaptive-coach-r-and-d.md). It documents the source assessment and why a capability model must precede any contextual-bandit policy.

## Decision architecture

```text
Ordinary local logs + TrainingLean preference + declarative exercise metadata
                              ↓
             Modality-specific observation builder
                              ↓
  Capability posterior: learned estimate + uncertainty, updated per session
                              ↓
 Safety/availability filter → candidate scorer → optional additional card
                              ↓
    Later local logs update capability; displayed candidates create exposure events
```

The model learns the athlete's state. A later policy may learn how to choose among safe candidates, but only after TimeGo has locally recorded which candidates it displayed and what was later observed. An exercise catalogue may describe movement demands, equipment, and logging modality; it may not define an ordered ladder, rank, prerequisite, or `nextExercise` relationship.

## R0 — Establish the evidence contract

**Execution update (2026-08-19):** A read-only, ignored phone export was audited successfully. It
has too few completed-session boundaries to validate a temporal learner; missing RPE and
duration-only cardio are retained as unknown/unsupported rather than imputed. The research module
now has explicit weighted-rep, hold, and pace-capable stamina observations plus exclusion reasons.
The implementation audit also found that historical `targetReps`/hold targets can be auto-filled
from the completed value and therefore do **not** establish achieved/missed outcomes without new
target provenance. Calisthenics `weightKg` already stores total bodyweight plus added load, so the
mapper reads it once and never double-counts `addedWeightKg`. The remaining R0 metadata
review/model-card work is deliberately separate from any recommendation UI.

- [ ] Make one read-only, ignored local research export of the TimeGo database. Do not copy raw personal data into Git, test fixtures, screenshots, or reports.
- [ ] Audit the available records by exercise and modality: sessions, working sets, timestamps, load/reps, bodyweight, optional RPE, targets achieved/missed, holds, duration, and distance. Report missingness explicitly; never turn an absent RPE or later log into a negative outcome.
- [ ] Define three observation contracts: weighted-repetition work, bodyweight/hold work, and stamina duration/distance work. Specify what each signal can support, what it cannot support, and how it is normalised without pretending that all modalities share an E1RM scale.
- [ ] Write a versioned, reviewable metadata draft for a small existing-exercise subset: modality, movement-demand vector, equipment/availability, loading form, and static exclusion flags. Keep it declarative and independent from the learner.
- [ ] Publish a model card/data dictionary describing inputs, transformations, intended use, uncertainty behaviour, safety limits, offline boundary, and prohibited inferences (injury, recovery, or medical clearance).

**Gate:** Stop rather than invent a result if the existing history cannot support chronological held-out evaluation. Continue normal logging and revisit when enough data exists.

## R1 — Build an offline, reproducible model comparison

- [ ] Use session-level chronological splits. Every feature for a test session must be derived only from older sessions; no within-session leakage.
- [ ] Establish honest comparators: last-observation/EWMA performance, current known-lift shrinkage prototype where it actually applies, and content-similarity only. None is a recommendation solution.
- [ ] Implement the initial candidate: a low-dimensional Bayesian/IRT-style capability posterior with modality-specific likelihoods and an explicit uncertainty interval. It updates from one newly completed session at a time.
- [ ] Run ablations in the same temporal splits: direct exercise history only; then muscle metadata; movement metadata; optional RPE. This tests whether metadata genuinely transfers evidence to related unseen movements.
- [ ] Evaluate prediction error appropriate to each observation, probability calibration where achieved/missed is observable, uncertainty-interval coverage, abstention rate, and inspectable counterexamples. Preserve run configuration, seeds, and summaries without preserving raw logs in version control.

**Gate:** Promote only a model that beats simple historical baselines on later unseen sessions, stays calibrated, and visibly becomes uncertain for sparse/unseen movement areas. If it fails, improve the observation/metadata design—not a UI or hard-coded fallback.

## R2 — Verify the phone-only implementation in shadow mode

- [ ] Port only the validated mathematical contract to deterministic pure Kotlin domain code; do not embed a Python or neural-model artefact.
- [ ] Add local persistence for model version, posterior state, and rebuild metadata. Rebuilding from a copied local log history must produce the same state as incremental updates.
- [ ] Update the posterior off the UI thread after completed sessions. Keep existing muscle-group cards and all visible recommendation behaviour unchanged.
- [ ] In shadow mode, generate but do not show candidate scores; compare them with later normal local logs. A missing later log is censored/unobserved, not a failure.
- [ ] Test with airplane mode, inspect release dependencies/manifest, and confirm that no model-related network permission, telemetry SDK, or dynamic downloader exists.

**Gate:** No visible learner is enabled until local shadow outcomes support the same calibration/uncertainty claims as the research prototype and explanations match the user's actual logged history.

## R3 — Add one visible, learned recommendation card

- [ ] Use the existing Strength/Balanced/Calisthenics preference only to choose the candidate family; it is not a preset progression.
- [ ] Apply static safety/availability exclusions first, then rank candidates by learned suitability and uncertainty. It may abstain: “still learning from your history.”
- [ ] Add the card alongside—never in place of—the existing muscle-group card. It must use direct history as evidence, so a pull-up history contributes to vertical-pull capability rather than triggering a foundation sequence.
- [ ] Store the displayed candidate set, scores, uncertainty, model version, and timestamp locally as a `RecommendationEvent`. Join future normal logs only to observed outcomes; non-selection is not failure.
- [ ] Explain suggestions with factual evidence and confidence, never generative coaching language or a prewritten pathway.

**Gate:** User review of real on-device examples is required before broadening the candidate set or enabling any exploration.

## R4 — Consider a local recommendation policy only with prospective evidence

- [ ] After sufficient local exposure/outcome records exist, compare the fixed capability scorer with a conservative linear Thompson policy inside the same hard safety filter.
- [ ] Evaluate only from the locally recorded events; do not reinterpret historic self-chosen workouts as policy trials.
- [ ] Keep deterministic candidate-pool audit records and bounded exploration. The policy must be able to abstain and must remain entirely on-device.

**Gate:** Retain the fixed scorer unless prospective evidence supports a meaningful improvement without worse uncertainty or safety behaviour.

## Non-negotiable checks for every R&D step

- The result is learned from this user's history; it is not a fixed exercise sequence with different wording.
- A large uncertainty result abstains rather than inventing a beginner or advanced recommendation.
- External datasets are optional offline research resources, not runtime dependencies and not substitutes for the user's history.
- Personal logs, exports, posterior snapshots, and backups remain local and untracked.
- No medical/injury/recovery inference is made from workout records.
