# TimeGo Accounts and Sync Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add optional verified email accounts, explicit cloud-backup consent, secure synchronization boundaries, and complete account deletion.

**Architecture:** UI/domain code depends on provider-neutral repositories. Firebase implementations are selected only when valid build-time configuration exists; local fakes keep development and tests deterministic. Room stays operational truth and WorkManager performs idempotent background sync.

**Tech Stack:** Kotlin, Firebase Authentication, Cloud Firestore, WorkManager, Firebase Emulator Suite, Compose, JUnit

**Spec:** `docs/superpowers/specs/2026-09-14-timego-public-launch-account-reports-onboarding-catalogue-design.md`

## Global Constraints

- Guest mode remains fully usable.
- Account creation never implies cloud-backup or email consent.
- Existing local data is previewed before first upload and never silently replaced.
- Private limitation notes never sync.
- Public account creation cannot ship before in-app and web deletion work.

---

### Task 1: Provider-neutral auth domain

**Files:**
- Create: `app/src/main/java/com/lsing/timego/account/AuthRepository.kt`
- Create: `app/src/main/java/com/lsing/timego/account/FakeAuthRepository.kt`
- Create: `app/src/main/java/com/lsing/timego/account/AccountViewModel.kt`
- Test: `app/src/test/java/com/lsing/timego/account/AccountViewModelTest.kt`

**Interfaces:**
- Produces: `AuthState`, `AuthFailure`, `register`, `signIn`, `sendVerification`, `resetPassword`, `signOut`, `reauthenticate`, `deleteAccount`

- [ ] **Step 1: Write failing state tests** for guest, unverified, verified, reset, generic credential failure, reauthentication-required deletion, and offline errors.
- [ ] **Step 2: Implement sealed states/failures and fake repository.**
- [ ] **Step 3: Implement ViewModel validation** without provider exception text or email-enumeration leakage.
- [ ] **Step 4: Run tests and commit** with `feat(account): define optional auth boundary`.

### Task 2: Firebase auth implementation and UI

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/lsing/timego/account/FirebaseAuthRepository.kt`
- Create: `app/src/main/java/com/lsing/timego/ui/account/AccountScreen.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/routines/SettingsBottomSheet.kt`
- Test: `app/src/androidTest/java/com/lsing/timego/ui/account/AccountScreenTest.kt`

**Interfaces:**
- Consumes: Firebase configuration supplied outside Git and `AccountViewModel`
- Produces: register/verify/sign-in/reset/sign-out/account-state Settings flow

- [ ] **Step 1: Add Firebase dependencies behind build configuration** that fails clearly for cloud-enabled builds missing configuration while normal local builds use the fake/off state.
- [ ] **Step 2: Implement Firebase mapping** with email verification, password reset, token-state observation, generic failures, and no password persistence.
- [ ] **Step 3: Implement accessible account screens** and Settings entry; do not block guest navigation.
- [ ] **Step 4: Add emulator-backed auth tests** for create, verify, sign-in, reset request, sign-out, and invalid credentials.
- [ ] **Step 5: Run gates and commit** with `feat(account): add verified email authentication`.

### Task 3: Sync identities and local queue

**Files:**
- Create: `app/src/main/java/com/lsing/timego/sync/SyncMetadata.kt`
- Create: `app/src/main/java/com/lsing/timego/sync/SyncDao.kt`
- Create: `app/src/main/java/com/lsing/timego/sync/SyncRepository.kt`
- Modify: `app/src/main/java/com/lsing/timego/data/TimeGoDatabase.kt`
- Modify: workout mutation DAOs/repository files under `app/src/main/java/com/lsing/timego/data/`
- Test: `app/src/test/java/com/lsing/timego/sync/SyncQueueTest.kt`

**Interfaces:**
- Produces: stable UUID metadata, `pendingBatch(limit: Int)`, `acknowledge`, `markRetry`, and tombstone lifecycle

- [ ] **Step 1: Write failing queue tests** for stable UUID creation, update coalescing, deletion tombstones, bounded batches, retry state, and acknowledged cleanup.
- [ ] **Step 2: Add additive Room migration and exported schema** for sync metadata without changing existing numeric relationships.
- [ ] **Step 3: Hook metadata writes into existing repository transactions** so domain data and pending sync cannot diverge.
- [ ] **Step 4: Run migration/queue/all JVM tests and commit** with `feat(sync): add durable local sync queue`.

### Task 4: Firestore sync and explicit enablement

**Files:**
- Create: `app/src/main/java/com/lsing/timego/sync/FirestoreSyncRemote.kt`
- Create: `app/src/main/java/com/lsing/timego/sync/SyncWorker.kt`
- Create: `app/src/main/java/com/lsing/timego/ui/account/CloudBackupScreen.kt`
- Create: `firebase/firestore.rules`
- Create: `firebase/firestore.indexes.json`
- Test: `firebase/test/firestore.rules.test.mjs`
- Test: `app/src/test/java/com/lsing/timego/sync/SyncWorkerTest.kt`

**Interfaces:**
- Produces: explicit enable/disable, first-upload preview, idempotent bounded upload, restore preview, and per-user Firestore paths

- [ ] **Step 1: Write worker and rules tests** for signed-out/no-consent no-op, verified owner access, denied cross-user access, retry, idempotent replay, and private-note exclusion.
- [ ] **Step 2: Implement deny-by-default rules and emulator tests.**
- [ ] **Step 3: Implement first-upload preview and consent UI**, including record counts and existing-history choice.
- [ ] **Step 4: Implement WorkManager sync** with connected constraint, exponential backoff, bounded batches, and safe status text.
- [ ] **Step 5: Run emulator/unit/Android gates and commit** with `feat(sync): add consented cloud backup foundation`.

### Task 5: Account and cloud-data deletion

**Files:**
- Create: `functions/src/deleteAccount.ts`
- Create: `functions/test/deleteAccount.test.ts`
- Modify: `app/src/main/java/com/lsing/timego/ui/account/AccountScreen.kt`
- Modify: `docs/release/account-deletion-page.md`

**Interfaces:**
- Produces: authenticated callable deletion, deletion status/receipt, reauthentication UI, and external HTTPS request contract

- [ ] **Step 1: Write backend tests** proving caller-only deletion, report cancellation, recursive owned-data deletion, idempotent retry, and authentication deletion last.
- [ ] **Step 2: Implement the callable function and emulator configuration** with Admin SDK authorization.
- [ ] **Step 3: Implement in-app reauthentication/confirmation and separate local-data choice.**
- [ ] **Step 4: Finalize the deployable deletion-page contract and verify every Play-required path is discoverable.**
- [ ] **Step 5: Run backend/emulator/Android gates and commit** with `feat(account): add complete deletion flow`.
