# TimeGo AI Onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add first-run physical/training onboarding and safely feed explicit preferences into recommendations.

**Architecture:** Store the versioned profile locally in Preferences DataStore through a focused repository. Route first launch through a resumable Compose wizard; keep physical fields optional and expose a pure preference-filter function to the existing recommendation layer.

**Tech Stack:** Kotlin, Compose Material 3, Preferences DataStore, JUnit

**Spec:** `docs/superpowers/specs/2026-09-14-timego-public-launch-account-reports-onboarding-catalogue-design.md`

## Global Constraints

- Existing users retain current `BALANCED` behavior until they complete onboarding.
- Required: experience, goal, modality, equipment, training days, and duration.
- Optional: display name, age range, height, weight, structured limitations, and private note.
- Free-text limitations never enter recommendation models, sync, or reports.
- All unit conversions normalize to metric storage.

---

### Task 1: Profile domain and persistence

**Files:**
- Create: `app/src/main/java/com/lsing/timego/profile/TrainingProfile.kt`
- Create: `app/src/main/java/com/lsing/timego/profile/ProfileRepository.kt`
- Test: `app/src/test/java/com/lsing/timego/profile/TrainingProfileTest.kt`

**Interfaces:**
- Produces: `TrainingProfile`, `AgeRange`, `ExperienceLevel`, `TrainingGoal`, `Equipment`, `SessionDurationRange`, `ProfileRepository.profile: Flow<TrainingProfile>`, `save`, `reset`

- [ ] **Step 1: Write failing tests** for required-field validation, metric/imperial conversion, optional values, and `syncSafeCopy()` removing the private note.
- [ ] **Step 2: Run the focused test; expect unresolved profile types.**
- [ ] **Step 3: Implement immutable enums/data class and pure validation/conversion functions.**
- [ ] **Step 4: Implement DataStore serialization** using stable enum names, string sets, onboarding version `1`, corruption-safe defaults, `save(profile)`, and `reset()`.
- [ ] **Step 5: Run tests and commit** with `feat(profile): add versioned training profile`.

### Task 2: Resumable onboarding state machine

**Files:**
- Create: `app/src/main/java/com/lsing/timego/ui/onboarding/OnboardingState.kt`
- Create: `app/src/main/java/com/lsing/timego/ui/onboarding/OnboardingViewModel.kt`
- Test: `app/src/test/java/com/lsing/timego/ui/onboarding/OnboardingStateTest.kt`

**Interfaces:**
- Consumes: `TrainingProfile` and `ProfileRepository.save`
- Produces: `OnboardingStep`, `OnboardingDraft`, `next()`, `back()`, `skipOptional()`, `complete()`

- [ ] **Step 1: Write failing transition tests** covering intro, goals, experience, equipment, schedule, optional physical details, limitations, review, back navigation, and required-field blocking.
- [ ] **Step 2: Run the focused test; expect unresolved state types.**
- [ ] **Step 3: Implement the pure state reducer** and a ViewModel wrapper that persists only on completion.
- [ ] **Step 4: Test process-restorable draft encoding and completion version behavior.**
- [ ] **Step 5: Run tests and commit** with `feat(onboarding): add resumable profile flow`.

### Task 3: Onboarding UI and navigation gate

**Files:**
- Create: `app/src/main/java/com/lsing/timego/ui/onboarding/OnboardingScreen.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/TimeGoNavHost.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/routines/SettingsBottomSheet.kt`
- Test: `app/src/androidTest/java/com/lsing/timego/ui/onboarding/OnboardingScreenTest.kt`

**Interfaces:**
- Consumes: `OnboardingViewModel`, `ProfileRepository.profile`
- Produces: first-launch gate and Settings `Training profile` editor entry

- [ ] **Step 1: Write Compose tests** asserting required choices, optional skip, progress semantics, review copy, and completion callback.
- [ ] **Step 2: Run the instrumentation compile; expect missing UI.**
- [ ] **Step 3: Implement one-question-per-page Compose UI** using existing TimeGo surfaces, 48dp targets, IME-safe scrolling, metric/imperial input labels, and explicit `Used for recommendations` explanations.
- [ ] **Step 4: Gate `TimeGoNavHost` on profile state** while allowing existing installs to dismiss the invitation and preserve current behavior.
- [ ] **Step 5: Add Settings edit/reset entry** with confirmation before reset.
- [ ] **Step 6: Run unit tests, `assembleDebugAndroidTest`, lint, and debug build; commit** with `feat(onboarding): add training profile experience`.

### Task 4: Recommendation preference integration

**Files:**
- Create: `app/src/main/java/com/lsing/timego/domain/ProfileCandidateFilter.kt`
- Modify: `app/src/main/java/com/lsing/timego/domain/SuggestedExercise.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/ProfileCandidateFilterTest.kt`

**Interfaces:**
- Produces: `filterCandidates(exercises, profile, guidanceByKey): CandidateFilterResult`

- [ ] **Step 1: Write failing tests** proving unavailable equipment and structured exclusions remove candidates, goals/modality only rank compatible candidates, logs retain priority, missing profiles preserve current results, and empty results abstain.
- [ ] **Step 2: Run focused tests; expect failure.**
- [ ] **Step 3: Implement the pure filter/result with reason strings** and no use of private notes, height, weight, or age for load prescriptions.
- [ ] **Step 4: Integrate it before existing suggestion selection** without changing logged-history familiarity precedence.
- [ ] **Step 5: Run all JVM tests and commit** with `feat(ai): personalize candidates from explicit profile`.
