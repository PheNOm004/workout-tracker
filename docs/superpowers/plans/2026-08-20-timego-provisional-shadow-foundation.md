# TimeGo Provisional Shadow Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build the deterministic, fully local, non-visible Android shadow foundation without claiming the unvalidated learner is useful or allowing it to recommend an exercise.

**Architecture:** Port the already-specified offline continuous observation/replay contract into a pure Kotlin domain module first. Feed it canonical completed-session Room snapshots only after that module has parity vectors and tests. Persist only rebuildable, versioned shadow state and append-only audit facts; every derived result remains hidden and invalidatable. The later real-history research verdict remains the only authority for candidate scoring or UI.

**Tech Stack:** Kotlin, Jetpack Room, coroutines, JUnit 4, Android instrumentation migration tests, existing Python prototype for synthetic parity fixtures.

**Spec:** `tasks/plan.md`; vault `Projects/TimeGo/11 Adaptive Coach Build Design.md`; `docs/superpowers/research/2026-08-19-timego-adaptive-coach-model-card.md`.

**Implementation status (2026-08-20):** Tasks 4–7 are implemented as provisional, hidden-only
engineering infrastructure. This records code completion, not a research or shadow promotion
verdict. Checkpoints A and B remain unpassed; Tasks 8–10 remain blocked.

**Fresh batch verification (2026-08-20):** 232 Android JVM tests, 78 Python reference tests, and
10 connected S23 instrumentation tests passed with zero failures. The instrumentation set covers
exported-schema 11/12/13 migration paths and checked real cache-pipeline behavior. `assembleDebug`
and `installDebug` passed. TimeGo then cold-started on the S23 in actual airplane mode with the
normal Log landing content; airplane mode was restored to its prior disabled state. These checks
verify the hidden implementation only and do not pass either data-promotion checkpoint.

## Global Constraints

- Fully offline and phone-only: no network permission, cloud, telemetry, runtime download, laptop/runtime dependency, chat, LLM, or free text.
- No visible coach card, candidate scorer, exercise rank, progression pathway, recommendation, or change to current muscle-group/rule-based suggestions.
- Do not infer bodyweight; stored calisthenics `weightKg` is already total system load and is read once.
- `load_reps`, `reps_only`, and `hold_seconds` are separate measurement bases and never influence one another.
- Canonical Room logs are the source of truth; derived state is an invalidatable cache. All historic user rows remain unchanged.
- Custom/unkeyed exercises and duration-only cardio abstain/exclude. TrainingLean may not update capability.
- This is a provisional shadow port. It does not satisfy the research verdict or authorize Tasks 8–10.
- Per user direction (2026-08-20), do not make per-task commits or pause for questions: complete
  all currently safe Tasks 4–7, verify the full batch, then make one commit. Document every
  validation-gated remainder in the TimeGo vault.

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

- [x] Write failing vector-loader tests for a loaded-rep baseline, a later same-basis update, independent reps-only state, hold state, and a long time gap.
- [x] Run `./gradlew testDebugUnitTest --tests '*ProvisionalShadowVectorsTest'` and confirm the vectors/domain API are absent.
- [x] Document exact transforms: `ln(1 + load*reps)`, `ln(1 + reps)`, `ln(1 + seconds)`, diagonal time-variance widening, neutral first baseline, deterministic ties, and hidden-only output.
- [x] Add only the static JSON vectors needed by the tests; never add personal values.
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

- [x] Write failing tests proving first same-task/basis observation is neutral, later stronger/weaker evidence updates only that basis, and a complete replay equals ordered incremental updates.
- [x] Run the individual JUnit tests and confirm the missing production symbols cause the expected failures.
- [x] Implement the smallest immutable Kotlin state/update functions that satisfy the vector tests.
- [x] Add deterministic invalid-observation tests and re-run the full Android unit suite.
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

- [x] Write failing mapper tests for calisthenics load read-once, warm-up exclusion, unkeyed custom exclusion, duration-only cardio exclusion, basis separation, deterministic ties, one maximum session summary, and reviewed bodyweight eligibility.
- [x] Run only these tests and confirm they fail for the absent or incorrect snapshot/mapper API.
- [x] Implement the transaction-scoped snapshot and pure mapper without collecting separate Flows.
- [x] Add a repository test that ending/deleting a session does not change current user-facing suggestion inputs.
- [x] Run `testDebugUnitTest`; install only after migration work in Task 7.
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

- [x] Add migration coverage for 11→12→13→14, 12→13→14, and 13→14 from committed exported schema DDL/identity, proving canonical rows survive and shadow tables start empty.
- [x] Write failing domain/integration tests for checked stale-source rejection, immediate delete and metadata/model/order invalidation, real snapshot→mapper→per-basis replay output, and rebuild-equals-incremental state.
- [x] Add a strictly additive 13→14 Room migration and entities/DAO; no `ALTER` or update against historic set rows.
- [x] Write state/audit atomically only when the captured source revision still matches; otherwise discard and rebuild.
- [x] Run full unit + migration suite, build/install on the S23 Ultra, and verify airplane-mode cold start plus unchanged Log landing screen.
- [ ] Commit `Persist rebuildable hidden shadow state` (deferred to the user-directed final safe-batch commit).

## Deferred research gate

After Tasks 4–7, collect enough ordinary later closed sessions to create a meaningful number of
same-exercise/same-basis prediction boundaries across more than the current three evidence sessions,
plus genuine pre-set target outcomes carrying trusted provenance. Then re-run the chronological
evaluator, review MAE/RMSE against simple history, interval coverage/calibration, abstention and
stronger/weaker self-correction examples, and have the user review aggregate explanations against
real history. The current 26 baselines and one tied boundary (MAE 0.1164), zero trusted historic
targets, and zero later envelope comparisons do not pass either checkpoint. Do not begin candidate
scoring, card display, policy/action events, or model-promotion claims until both verdicts pass.
