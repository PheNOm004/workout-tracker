# TimeGo Exercise Guidance Catalogue Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce a validated bundled catalogue with complete guidance for exactly 200 common exercises and easier links for niche/advanced entries.

**Architecture:** JSON is the reviewable source, a deterministic generator creates the bundled Kotlin/Room snapshot, and Room remains the runtime store. Guidance is separate from user-created exercise records and keyed by immutable `catalogueKey`.

**Tech Stack:** JSON, Node.js 22 validation/generation, Kotlin, Room, Compose, JUnit

**Spec:** `docs/superpowers/specs/2026-09-14-timego-public-launch-account-reports-onboarding-catalogue-design.md`

## Global Constraints

- All 820 exercises remain searchable and loggable.
- Exactly 200 entries receive `REVIEWED_COMPLETE` guidance in the first release.
- Easier links target existing keys, are acyclic, and never point to self.
- No runtime GitHub or network dependency.
- Existing history and custom exercises are never overwritten or deleted.

---

### Task 1: Catalogue schema and validator

**Files:**
- Create: `catalogue/schema.json`
- Create: `catalogue/exercises.json`
- Create: `scripts/validate-catalogue.mjs`
- Create: `app/src/test/java/com/lsing/timego/data/CatalogueContractTest.kt`

**Interfaces:**
- Produces JSON fields: `schemaVersion`, `catalogueVersion`, `key`, `aliases`, `equipment`, `difficulty`, `complexity`, `purpose`, `setup`, `steps`, `cues`, `mistakes`, `easierVariationKey`, `harderVariationKeys`, `reviewStatus`

- [ ] **Step 1: Write the failing Kotlin source-contract test** that requires 820 unique keys, exactly 200 complete entries, valid enums/weights, and valid variation targets.
- [ ] **Step 2: Write the JSON Schema and Node validator** with duplicate, broken-link, self-link, easier-cycle, size, empty-step, unsupported-enum, and exact-count failures.
- [ ] **Step 3: Generate an initial JSON conversion from `SeedExercises.kt`** preserving every key and existing metadata; mark entries `METADATA_ONLY` until reviewed.
- [ ] **Step 4: Run both validators; expect the exact-200 assertion to fail while structure passes.**
- [ ] **Step 5: Commit** with `feat(catalogue): establish validated exercise source`.

### Task 2: Select and author the common 200

**Files:**
- Modify: `catalogue/exercises.json`
- Create: `catalogue/REVIEW_GUIDE.md`
- Create: `catalogue/common-200.md`

**Interfaces:**
- Consumes: existing categories, muscle weights, logging types, biomechanical profiles
- Produces: exactly 200 reviewed entries and documented selection distribution

- [ ] **Step 1: Produce `common-200.md`** covering foundational movement patterns, every tracked muscle, and common barbell/dumbbell/cable/machine/bodyweight/cardio equipment without specialty-variant crowding.
- [ ] **Step 2: Add reviewed content in batches of 25**, running `node scripts/validate-catalogue.mjs --allow-incomplete` after every batch.
- [ ] **Step 3: Add easier links for advanced/niche entries** only where the target shares the movement intent and is genuinely less complex.
- [ ] **Step 4: Run the strict validator and focused Kotlin contract; require exactly 200 complete and zero link/cycle failures.**
- [ ] **Step 5: Commit** with `content: add 200 reviewed exercise guides`.

### Task 3: Runtime guidance model and schema migration

**Files:**
- Create: `app/src/main/java/com/lsing/timego/data/guidance/ExerciseGuidance.kt`
- Create: `app/src/main/java/com/lsing/timego/data/guidance/ExerciseGuidanceDao.kt`
- Create: `app/src/main/java/com/lsing/timego/data/guidance/CatalogueRepository.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/TimeGoDatabase.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/Converters.kt`
- Test: `app/src/test/java/com/lsing/timego/data/guidance/CatalogueRepositoryTest.kt`

**Interfaces:**
- Produces: `observeGuidance(key): Flow<ExerciseGuidance?>`, `guidanceByKey: Flow<Map<String, ExerciseGuidance>>`, `resolveEasier(key)`

- [ ] **Step 1: Write failing mapper/repository tests** for complete, metadata-only, deprecated, missing, and easier-link records.
- [ ] **Step 2: Add an additive Room 15-to-16 migration** and exported schema for a guidance table keyed by `catalogueKey`; do not modify exercise IDs.
- [ ] **Step 3: Implement transactional idempotent import** of the generated bundled snapshot, rejecting invalid versions before writes.
- [ ] **Step 4: Add migration and startup import tests** proving existing sessions/routines/custom exercises remain unchanged.
- [ ] **Step 5: Run tests and commit** with `feat(catalogue): persist bundled exercise guidance`.

### Task 4: Exercise details UI

**Files:**
- Create: `app/src/main/java/com/lsing/timego/ui/exercise/ExerciseDetailSheet.kt`
- Create: `app/src/main/java/com/lsing/timego/ui/exercise/ExerciseDetailModel.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/common/ExerciseListSections.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/log/ActiveWorkoutSection.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/routines/RoutineFormDialog.kt`
- Test: `app/src/test/java/com/lsing/timego/ui/exercise/ExerciseDetailModelTest.kt`

**Interfaces:**
- Produces: `buildExerciseDetail(exercise, guidance, lastPerformance, record)` and reusable `onShowExerciseDetails(Long)` callback

- [ ] **Step 1: Write failing model tests** for reviewed content, metadata-only `Start with`, personal values, missing guidance, and custom exercises.
- [ ] **Step 2: Implement the pure presentation model.**
- [ ] **Step 3: Implement the TimeGo-styled modal sheet** with offline content, accessibility headings, variation navigation, and no loss of active input state.
- [ ] **Step 4: Wire detail actions from catalogue, active workout, routine editor, and recommendation surfaces.**
- [ ] **Step 5: Run JVM tests, lint, debug/release builds, and commit** with `feat(exercises): add offline guidance details`.
