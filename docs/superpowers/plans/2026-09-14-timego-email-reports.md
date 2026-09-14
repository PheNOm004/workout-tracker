# TimeGo Weekly and Monthly Email Reports Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build truthful weekly/monthly workout summaries, explicit subscriptions, reliable scheduled delivery, preview, unsubscribe, and duplicate prevention.

**Architecture:** A pure Kotlin report engine builds the same typed report model used by local preview and backend serialization. Firestore subscriptions drive scheduled Firebase functions; a server-only provider adapter sends email and an idempotency ledger prevents duplicates.

**Tech Stack:** Kotlin/JUnit, Compose, Firebase Functions v2, Cloud Scheduler, Firestore, TypeScript tests, transactional email provider adapter

**Spec:** `docs/superpowers/specs/2026-09-14-timego-public-launch-account-reports-onboarding-catalogue-design.md`

## Global Constraints

- Weekly and monthly consent are separate and off by default.
- Reports require a verified email and cloud sync.
- Mixed logging modalities never become one misleading universal volume value.
- Every email has a signed HTTPS manage/unsubscribe link.
- Delivery is idempotent by user, cadence, and report period.

---

### Task 1: Pure report domain

**Files:**
- Create: `app/src/main/java/com/lsing/timego/report/WorkoutReport.kt`
- Create: `app/src/main/java/com/lsing/timego/report/ReportPeriod.kt`
- Create: `app/src/main/java/com/lsing/timego/report/ReportBuilder.kt`
- Test: `app/src/test/java/com/lsing/timego/report/ReportBuilderTest.kt`

**Interfaces:**
- Produces: `buildWeeklyReport(input, week, profile): WorkoutReport` and `buildMonthlyReport(input, month, profile): WorkoutReport`

- [ ] **Step 1: Write failing tests** for empty periods, sessions/active days/duration/sets, strength volume, holds, cardio distance/duration, muscle coverage, PRs, intended-day consistency, previous-period comparisons, body metrics, and conservative suggestions.
- [ ] **Step 2: Add DST and year/month-boundary tests** using explicit `LocalDate` periods and no elapsed-millisecond period math.
- [ ] **Step 3: Implement immutable report models and deterministic builder** reusing existing progress math where semantics match.
- [ ] **Step 4: Run focused/all JVM tests and commit** with `feat(reports): build deterministic workout summaries`.

### Task 2: Report preview and preferences

**Files:**
- Create: `app/src/main/java/com/lsing/timego/report/ReportSubscriptionRepository.kt`
- Create: `app/src/main/java/com/lsing/timego/ui/report/ReportSettingsScreen.kt`
- Create: `app/src/main/java/com/lsing/timego/ui/report/ReportPreviewSheet.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/account/AccountScreen.kt`
- Test: `app/src/test/java/com/lsing/timego/report/ReportSubscriptionTest.kt`

**Interfaces:**
- Produces: independent weekly/monthly toggles, timezone, empty-period behavior, consent version/timestamp, preview, and test-send request

- [ ] **Step 1: Write failing preference tests** for defaults-off, verified-account/cloud-sync prerequisites, separate cadence changes, consent version, pause, unsubscribe, and timezone validation.
- [ ] **Step 2: Implement repository and provider-neutral remote contract.**
- [ ] **Step 3: Implement Settings and preview UI** showing the exact local report model and why prerequisites block enabling.
- [ ] **Step 4: Run tests/lint/build and commit** with `feat(reports): add consent and preview experience`.

### Task 3: Backend scheduling and idempotency

**Files:**
- Create: `functions/src/reports/scheduleReports.ts`
- Create: `functions/src/reports/reportPeriods.ts`
- Create: `functions/src/reports/deliveryLedger.ts`
- Create: `functions/test/scheduleReports.test.ts`

**Interfaces:**
- Produces: `scheduleWeeklyReports`, `scheduleMonthlyReports`, `deliveryKey(uid, cadence, period)`, and due-subscription query

- [ ] **Step 1: Write failing tests** for IANA timezone due selection, DST, month/year boundaries, disabled/unverified subscriptions, retry, concurrency, and duplicate ledger acquisition.
- [ ] **Step 2: Implement UTC scheduler handlers** with transactionally acquired idempotency records and bounded fan-out.
- [ ] **Step 3: Implement report input retrieval** only for opted-in user paths and validate serialized report schema/version.
- [ ] **Step 4: Run Functions emulator tests and commit** with `feat(reports): schedule idempotent report delivery`.

### Task 4: Email rendering and provider boundary

**Files:**
- Create: `functions/src/reports/renderReportEmail.ts`
- Create: `functions/src/reports/EmailProvider.ts`
- Create: `functions/src/reports/sendReport.ts`
- Create: `functions/test/renderReportEmail.test.ts`
- Create: `functions/test/sendReport.test.ts`

**Interfaces:**
- Produces: escaped text/HTML email, server-only provider call, signed manage URL, provider-message ledger update

- [ ] **Step 1: Write rendering tests** for escaping, accessibility/plain-text parity, mixed modalities, empty period, privacy footer, and no private limitation note.
- [ ] **Step 2: Write sending tests** for secret absence, provider failure retry, permanent rejection, duplicate suppression, and minimal delivery log.
- [ ] **Step 3: Implement templates and provider interface** with credentials read only from managed backend secrets.
- [ ] **Step 4: Implement send orchestration** and test-send mode with a separate idempotency namespace.
- [ ] **Step 5: Run backend tests and commit** with `feat(reports): render and send private workout reports`.

### Task 5: Manage/unsubscribe path and end-to-end gate

**Files:**
- Create: `functions/src/reports/manageSubscription.ts`
- Create: `functions/test/manageSubscription.test.ts`
- Create: `docs/release/report-delivery-runbook.md`
- Modify: `docs/release/data-safety-matrix.md`

**Interfaces:**
- Produces: signed single-purpose manage token, cadence-specific unsubscribe, all-reports unsubscribe, delivery/runbook evidence

- [ ] **Step 1: Write tests** for valid, expired, tampered, wrong-user, replayed, cadence-only, and unsubscribe-all links.
- [ ] **Step 2: Implement token verification and preference update** without exposing account or workout data.
- [ ] **Step 3: Document provider/domain/DNS/secret deployment inputs**, monitoring, retries, suppression, support, and incident disable switch.
- [ ] **Step 4: Run full backend/emulator/Android gates** and a non-delivering local end-to-end report cycle.
- [ ] **Step 5: Commit** with `feat(reports): complete report subscription lifecycle`.
