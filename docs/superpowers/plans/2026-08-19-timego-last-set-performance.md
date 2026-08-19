# Last-Set Performance While Logging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show the actual previous completed working set for each strength/calisthenics exercise while the user enters a new set.

**Architecture:** A pure domain function selects one eligible `SetLog` per exercise from closed
sessions. `LogViewModel` maintains the live map from the repository streams, and the existing
expanded strength row renders a text-only reference without changing suggestions or form values.

**Tech Stack:** Kotlin, Coroutines Flow, Jetpack Compose, JUnit.

**Spec:** `docs/superpowers/specs/2026-08-19-timego-last-set-performance-design.md`

## Global Constraints

- Only non-warmup strength/calisthenics logs from closed sessions qualify.
- Never use an active-session set as the reference.
- Display only; do not prefill inputs or alter overload suggestions.

---

### Task 1: Closed-session last-set selection

**Files:**
- Create: `app/src/main/java/com/lsing/timego/domain/LastSetPerformance.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/LastSetPerformanceTest.kt`

**Interfaces:**
- Produces: `fun lastWorkingSetByExercise(setLogs: List<SetLog>, sessions: List<WorkoutSession>, exercisesById: Map<Long, Exercise>): Map<Long, SetLog>`.

- [ ] **Step 1: Write failing tests** for latest closed-session selection, warmup exclusion,
  active-session exclusion, and cardio exclusion.
- [ ] **Step 2: Run** `./gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.LastSetPerformanceTest" -q` and confirm the missing-function failure.
- [ ] **Step 3: Implement** a filtered, per-exercise maximum by `loggedAtEpochMillis`; only session
  IDs whose `endEpochMillis` is non-null may participate.
- [ ] **Step 4: Re-run** the targeted test and confirm it passes.
- [ ] **Step 5: Commit** the domain implementation and test.

---

### Task 2: Live Log state and reference display

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt`

**Interfaces:**
- Consumes: `lastWorkingSetByExercise` from Task 1.
- Produces: `LogViewModel.lastWorkingSets: StateFlow<Map<Long, SetLog>>` and a `lastWorkingSet`
  parameter on `StrengthLogRow`.

- [ ] **Step 1: Add** `repository.sessions` to the existing Log-screen combined collector and derive
  `lastWorkingSets` when logs, sessions, or exercises change.
- [ ] **Step 2: Pass** the map entry for each exercise into `StrengthLogRow`.
- [ ] **Step 3: Render** `Last time: …` in the expanded row, using existing calisthenics formatting;
  do not alter suggestions, default text-field values, or the log callback.
- [ ] **Step 4: Run** `./gradlew.bat testDebugUnitTest installDebug -q` and confirm the suite and install succeed.
- [ ] **Step 5: Commit** the ViewModel and UI wiring.

## Self-review

Task 1 covers eligibility and selection; Task 2 covers live state and display. The plan intentionally
does not add a ViewModel/UI test because the project uses domain-level JUnit tests for this class of
behaviour. It introduces no changes to the suggestion algorithm or log persistence.
