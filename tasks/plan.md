# Implementation Plan: TimeGo adaptive coach

**Status:** Proposed. This mirror supports implementation work; the durable source of truth is [the vault build design](C:/Users/lsing/.claude/obsidian_demo/Projects/TimeGo/11%20Adaptive%20Coach%20Build%20Design.md), revised by [deep R&D](C:/Users/lsing/.claude/obsidian_demo/Projects/TimeGo/12%20Adaptive%20Coach%20Deep%20R%26D.md).

## Overview

Build a fully offline, phone-only adaptive coach in gated vertical slices. Start with private read-only research and a small uncertainty-aware capability model. Do not change the visible app until both temporal backtests and on-device shadow mode prove that the model is useful and honest.

## Architecture decisions

- Enforce strict no-cloud/no-transfer backup rules first: the current Android Auto Backup defaults may upload Room/DataStore data. Verify the packaged release has no `INTERNET` permission or telemetry/dynamic-download dependency.
- Use existing Room logs and TrainingLean as inputs; add no chat, free-text input, cloud service, telemetry, runtime download, or laptop dependency.
- Model capability before recommendation policy. It is time-varying, anchored, regularised, and allowed to abstain when task demand cannot be identified from individual history. Contextual bandits/RL are deferred until prospective local exposure/outcome data exists.
- Keep exercise metadata declarative: movement demands, modality, equipment/loading, and safety exclusions only. Never encode a ladder, rank, prerequisite, or `nextExercise` field.
- Treat Room logs plus versioned metadata as canonical; derived model state is an invalidatable/rebuildable cache with source revision and metadata/model hashes.
- Start with local coroutine updates after a completed session. A unique idempotent local worker is only a later reconciler if full rebuild work becomes too large.
- A visible learned card is additional to the existing muscle-group card.

## Task list

### Phase 0: Phone-only privacy foundation

- [x] Task 0: Enforce and verify strict phone-only backup/permission policy (2026-08-19).

### Phase 1: Local research

- [x] Task 1: Audit a read-only, ignored local export for modality outcome coverage, bodyweight correspondence, and missingness (2026-08-19; insufficient temporal evidence for a validated learner).
- [ ] Task 2: Define weighted-rep, hold/bodyweight, and pace-capable stamina observation contracts; add anchors, metadata versioning, and identifiability fixtures. Research contract and v1 subset are built; schema 12 records future suggested-target provenance and schema 13 persists immutable built-in `catalogueKey` values. The current history resolves through those keys without using Room IDs or display names; finish review of candidate safety/demand metadata before any learned candidate can be shown.
- [ ] Task 3: Build closed-session chronological backtests with time-gap uncertainty, leakage tests, and abstention evaluation.

### Checkpoint: Research verdict

- [ ] Candidate beats simple history on later sessions where evidence exists.
- [ ] Uncertainty is calibrated and sparse/unseen or unidentifiable movements abstain.
- [ ] Stamina is excluded unless its separate evidence gate passes.
- [ ] User reviews real-history explanations before Kotlin work begins.

### Phase 2: Phone-side model and shadow mode

- [ ] Task 4: Freeze language-independent synthetic parity vectors and a versioned model contract.
- [ ] Task 5: Implement the pure Kotlin domain model with rebuild-equals-incremental tests.
- [ ] Task 6: Integrate read-only analysis from Room into `LogViewModel` off the UI thread, with no visible behaviour change.
- [ ] Task 7: Add additive local cache/audit persistence and validate invalidation, stale-write rejection, and rebuild against a full rebuild.

### Checkpoint: Shadow verdict

- [ ] Later normal logs support calibration and self-correction claims.
- [ ] Full tests, migration tests, install, and airplane-mode checks pass.
- [ ] User verifies explanation samples against real history.

### Phase 3: One visible learned card

- [ ] Task 8: Implement a deterministic candidate scorer with safety exclusions, confidence, and abstention.
- [ ] Task 9: Add local display and explicit start-from-card action events, never labelling freeform logs as policy rewards.
- [ ] Task 10: Add one factual learned card beside the existing muscle-group card and run device review.

### Checkpoint: Visible learner

- [ ] The card does not replace current recommendations or expose a hard-coded roadmap.
- [ ] Every displayed recommendation is reproducible from local data/model version.
- [ ] User approves the actual phone behaviour before candidate-set expansion.

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| Too little or too uneven history | Gate at research verdict; abstain rather than guess. |
| A model appears accurate through future-data leakage | Session-level chronological splits and synthetic leakage tests. |
| Metadata becomes a hidden pathway | Review attributes explicitly; prohibit ordering fields. |
| In-app model state drifts | Compare incremental updates with deterministic full rebuilds. |
| A new card harms current UX | Shadow mode first; add beside, never replace, the muscle card. |

## Approval requirement

Do not start implementation until the user approves this plan and explicitly authorises the read-only local data audit. Each later phase requires its preceding checkpoint.
