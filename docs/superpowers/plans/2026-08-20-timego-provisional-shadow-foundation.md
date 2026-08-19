# TimeGo Provisional Shadow Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build the deterministic, fully local, non-visible Android shadow foundation without claiming the unvalidated learner is useful or allowing it to recommend an exercise.

**Architecture:** Port the already-specified offline continuous observation/replay contract into a pure Kotlin domain module first. Feed it canonical completed-session Room snapshots only after that module has parity vectors and tests. Persist only rebuildable, versioned shadow state and append-only audit facts; every derived result remains hidden and invalidatable. The later real-history research verdict remains the only authority for candidate scoring or UI.

**Tech Stack:** Kotlin, Jetpack Room, coroutines, JUnit 4, Android instrumentation migration tests, existing Python prototype for synthetic parity fixtures.

**Spec:** `tasks/plan.md`; vault `Projects/TimeGo/11 Adaptive Coach Build Design.md`; `docs/superpowers/research/2026-08-19-timego-adaptive-coach-model-card.md`.

## Global Constraints

- Fully offline and phone-only: no network permission, cloud, telemetry, runtime download, laptop/runtime dependency, chat, LLM, or free text.
- No visible coach card, candidate scorer, exercise rank, progression pathway, recommendation, or change to current muscle-group/rule-based suggestions.
- Do not infer bodyweight; stored calisthenics `weightKg` is already total system load and is read once.
- `load_reps`, `reps_only`, and `hold_seconds` are separate measurement bases and never influence one another.
- Canonical Room logs are the source of truth; derived state is an invalidatable cache. All historic user rows remain unchanged.
- Custom/unkeyed exercises and duration-only cardio abstain/exclude. TrainingLean may not update capability.
- This is a provisional shadow port. It does not satisfy the research verdict or authorize Tasks 8–10.

---

### Task 4 — Provisional model contract and parity vectors

**Files:**

- Create: `docs/superpowers/research/2026-08-20-timego-provisional-shadow-contract.md`
- Create: `app/src/test/resources/adaptive/provisional-shadow-vectors.json`
- Create: `app/src/test/java/com/lsing/timego/domain/adaptive/ProvisionalShadowVectorsTest.kt`
- Modify: `tasks/plan.md`
- Modify: vault `Projects/TimeGo/11 Adaptive Coach Build Design.md`

**Interfaces:**

- Produces a versioned JSON vector schema with `basis`, ordered completed-session observation, expected personal-baseline registration/update, state mean/variance, and abstention reason.
- Later Kotlin code consumes only these synthetic vectors; it must not load Python or a model file at runtime.

- [ ] Write failing vector-loader tests for a loaded-rep baseline, a later same-basis update, independent reps-only state, hold state, and a long time gap.
- [ ] Run `./gradlew testDebugUnitTest --tests '*ProvisionalShadowVectorsTest'` and confirm the vectors/domain API are absent.
- [ ] Document exact transforms: `ln(1 + load*reps)`, `ln(1 + reps)`, `ln(1 + seconds)`, diagonal time-variance widening, neutral first baseline, deterministic ties, and hidden-only output.
- [ ] Add only the static JSON vectors needed by the tests; never add personal values.
- [ ] Commit `Freeze provisional shadow parity vectors`.

### Task 5 — Pure Kotlin replay module

**Files:**

- Create: `app/src/main/java/com/lsing/timego/domain/adaptive/ProvisionalContinuousCapability.kt`
- Create: `app/src/main/java/com/lsing/timego/domain/adaptive/ProvisionalContinuousReplay.kt`
- Create: `app/src/test/java/com/lsing/timego/domain/adaptive/ProvisionalContinuousCapabilityTest.kt`
- Create: `app/src/test/java/com/lsing/timego/domain/adaptive/ProvisionalContinuousReplayTest.kt`

**Interfaces:**

- Consumes immutable `ShadowObservation`, `ShadowBasis`, `ShadowConfig`, and ordered closed-session groups from Task 4.
- Produces `ShadowState`, `ShadowUpdate`, and aggregate hidden audit facts. No Room, Compose, Android context, candidate, or UI type appears in this package.

- [ ] Write failing tests proving first same-task/basis observation is neutral, later stronger/weaker evidence updates only that basis, and a complete replay equals ordered incremental updates.
- [ ] Run the individual JUnit tests and confirm the missing production symbols cause the expected failures.
- [ ] Implement the smallest immutable Kotlin state/update functions that satisfy the vector tests.
- [ ] Add deterministic invalid-observation tests and re-run the full Android unit suite.
- [ ] Commit `Add provisional Kotlin shadow replay`.

### Task 6 — Atomic read-only Room snapshot and hidden replay

**Files:**

- Modify: `app/src/main/java/com/lsing/timego/data/ExerciseDao.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/SessionDao.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/SetLogDao.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/WorkoutRepository.kt`
- Create: `app/src/main/java/com/lsing/timego/data/adaptive/ShadowSnapshotMapper.kt`
- Create: matching unit tests under `app/src/test/java/com/lsing/timego/data/adaptive/`

**Interfaces:**

- `WorkoutRepository` exposes one suspend hidden snapshot function, ordered `(session.endEpochMillis, session.id, set.loggedAtEpochMillis, set.id)` and obtained within one Room transaction.
- Mapper produces only keyed, closed, valid observations for Task 5; it returns explicit exclusions for every omitted row.

- [ ] Write failing mapper tests for calisthenics load read-once, warm-up exclusion, unkeyed custom exclusion, duration-only cardio exclusion, basis separation, and deterministic ties.
- [ ] Run only these tests and confirm they fail for the absent snapshot/mapper API.
- [ ] Implement the transaction-scoped snapshot and pure mapper without collecting separate Flows.
- [ ] Add a repository test that ending/deleting a session does not change current user-facing suggestion inputs.
- [ ] Run `testDebugUnitTest`; install only after migration work in Task 7.
- [ ] Commit `Add hidden Room shadow snapshot`.

### Task 7 — Rebuildable local shadow cache and audit

**Files:**

- Modify: `app/src/main/java/com/lsing/timego/data/TimeGoDatabase.kt`
- Create: `app/src/main/java/com/lsing/timego/data/adaptive/ShadowSnapshotEntity.kt`
- Create: `app/src/main/java/com/lsing/timego/data/adaptive/ShadowAuditEntity.kt`
- Create: `app/src/main/java/com/lsing/timego/data/adaptive/ShadowDao.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/WorkoutRepository.kt`
- Create: migration/instrumented tests and schema export for version 14.

**Interfaces:**

- Cache is keyed by source revision/fingerprint plus model-contract and metadata hashes; its state can be discarded and rebuilt exactly from canonical Room rows.
- Audit records aggregate counts/version/rebuild status only; neither table carries personal workout rows or network identity.

- [ ] Write failing migration test from schema 13 proving existing exercise, session, and set rows survive and new shadow tables exist empty.
- [ ] Write failing domain tests for stale-source rejection, delete invalidation, metadata/model version invalidation, and rebuild-equals-incremental state.
- [ ] Add a strictly additive 13→14 Room migration and entities/DAO; no `ALTER` or update against historic set rows.
- [ ] Write state/audit atomically only when the captured source revision still matches; otherwise discard and rebuild.
- [ ] Run full unit + migration suite, build/install on the S23 Ultra, and verify airplane-mode cold start plus unchanged Log landing screen.
- [ ] Commit `Persist rebuildable hidden shadow state`.

## Deferred research gate

After Tasks 4–7, re-run the chronological evaluator only when ordinary repeated closed-session history exists. Do not begin candidate scoring, card display, policy/action events, or model-promotion claims until the research and shadow verdicts are satisfied.
