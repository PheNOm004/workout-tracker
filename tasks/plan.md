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
- [x] Task 2: Define weighted-rep, hold/bodyweight, and pace-capable stamina observation contracts; add anchors, metadata versioning, and identifiability fixtures (2026-08-20). Schema 12 records future suggested-target provenance and schema 13 persists immutable built-in `catalogueKey` values. Ordinary weighted-rep/hold logs build a conservative time-stamped performance envelope, while binary target outcomes remain a separate signal. The adapter preserves every stored muscle contribution weight for keyed history without Room IDs/names. The reviewed v1 candidate set is deliberately narrow; broader candidate expansion remains deferred, not a roadmap prerequisite.
- [x] Task 3: Closed-session chronological evaluation is implemented for the continuous ordinary-log learner (2026-08-20). It freezes state before each completed session, compares predicted same-exercise/basis work scores with a same-basis last-observation baseline, then applies the whole session in deterministic set/key/basis order. Its aggregate-only report provides MAE/RMSE, boundary and insufficient-evidence counts, and never names a winner with zero boundaries; binary target calibration remains separate. Synthetic leakage, comparator, basis-separation, and no-boundary abstention tests pass. The read-only local audit has 26 baselines, one later prediction boundary/update, and an aggregate candidate/baseline tie (MAE 0.1164); one boundary is far too little for a validation or promotion claim. The later user-authorised Android port is therefore provisional hidden infrastructure only and does not satisfy this gate or authorise a visible card.

### Checkpoint: Research verdict

- [ ] Candidate beats simple history on later sessions where evidence exists.
- [ ] Uncertainty is calibrated and sparse/unseen or unidentifiable movements abstain.
- [ ] Stamina is excluded unless its separate evidence gate passes.
- [ ] User reviews real-history explanations before Kotlin work begins.

### Phase 1.5: Provisional hidden shadow foundation (user-authorised 2026-08-20)

The user authorised Tasks 4–7 to proceed before a real-history promotion verdict, solely as a
fully hidden, local engineering foundation. See
`docs/superpowers/plans/2026-08-20-timego-provisional-shadow-foundation.md`. This changes neither
the existing UI nor the research gate: the port remains provisional and cannot create candidates,
recommendations, policy outcomes, or a card. Tasks 8–10 remain blocked on later evidence.

### Phase 2: Phone-side model and shadow mode

- [x] Task 4: Freeze a **provisional** language-independent synthetic parity contract and vectors (2026-08-20). The versioned, invented-only fixture covers a loaded baseline/update, independent reps-only and hold baselines, and diagonal long-gap variance widening. Kotlin reproduces the frozen values without Python, a model file, personal records, or a runtime dependency. This is implementation parity, not validation.
- [x] Task 5: Implement the **provisional hidden** pure Kotlin domain model (2026-08-20). It keeps measurement bases independent, freezes held-session prediction eligibility, applies actual observations sequentially with evolving residual/variance, and preserves full-rebuild/ordered-incremental equivalence. It is not a promoted learner.
- [x] Task 6: Implement the **provisional hidden** atomic Room snapshot and pure mapper/replay seam (2026-08-20). It selects one maximum completed observation per exercise/basis/closed session, permits zero-load reps-only evidence only for reviewed bodyweight-supported metadata, and does not alter current UI or rule-based suggestions.
- [x] Task 7: Implement the **provisional hidden** schema-14 cache/audit and production rebuild pipeline (2026-08-20). It maps an atomic source snapshot, replays per basis, encodes derived state, atomically checks/writes cache plus aggregate audit, and makes source/model/metadata/order mismatches unusable through the checked read path. Migration coverage includes 11→12→13→14, 12→13→14, and 13→14. This does not pass Checkpoint A or B.

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

The user approved the read-only audit and separately authorised only the provisional hidden Tasks 4–7 foundation. Tasks 8–10 and every visible/candidate/policy change still require the unpassed research and shadow checkpoints above.
