# TimeGo Public Release Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add repository-owned Play release, privacy, signing, and verification foundations without storing secrets or publishing the app.

**Architecture:** Keep the existing Android application and package identity. Add a release-readiness contract, secret-fed release signing, public policy drafts, and automated checks that prevent debug signing or missing policy artifacts from becoming a candidate release.

**Tech Stack:** Gradle Kotlin DSL, Android App Bundle, PowerShell, Markdown, JUnit

**Spec:** `docs/superpowers/specs/2026-09-14-timego-public-launch-account-reports-onboarding-catalogue-design.md`

## Global Constraints

- Work only on `development/timego-public-foundation`; do not modify `master`.
- Application ID remains `com.lsing.timego`; minimum SDK remains 26; target SDK remains 37.
- Never commit keystores, passwords, tokens, Firebase credentials, email-provider credentials, or `.env` values.
- Do not publish, create accounts, enable billing, install on the S23, or merge without explicit user-controlled action.
- Run `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `assembleRelease` before a release checkpoint.

---

### Task 1: Release signing contract

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `.gitignore`
- Create: `docs/release/PLAY_RELEASE.md`
- Test: `app/src/test/java/com/lsing/timego/ReleaseConfigurationTest.kt`

**Interfaces:**
- Consumes: environment variables `TIMEGO_UPLOAD_STORE_FILE`, `TIMEGO_UPLOAD_STORE_PASSWORD`, `TIMEGO_UPLOAD_KEY_ALIAS`, `TIMEGO_UPLOAD_KEY_PASSWORD`
- Produces: `bundleRelease` signed only when all four values are present; unsigned local `assembleRelease` remains available for normal verification

- [ ] **Step 1: Write a failing source-contract test** that reads `app/build.gradle.kts`, asserts all four names are present, and asserts no literal password assignment exists.
- [ ] **Step 2: Run** `.\gradlew.bat testDebugUnitTest --tests com.lsing.timego.ReleaseConfigurationTest`; expect failure because the contract is absent.
- [ ] **Step 3: Add conditional signing configuration** using `System.getenv`, attach it only when the complete set is present, and add keystore patterns to `.gitignore`.
- [ ] **Step 4: Document exact Android Studio/Gradle AAB commands**, Play App Signing separation, version-code rules, certificate fingerprint retrieval, and local secret setup without example secret values.
- [ ] **Step 5: Run the focused test and** `.\gradlew.bat assembleRelease`; expect both to pass without a keystore and no secret printed.
- [ ] **Step 6: Commit** with `build: add Play release signing contract`.

### Task 2: Privacy and Play declaration matrix

**Files:**
- Create: `docs/release/privacy-policy-draft.md`
- Create: `docs/release/data-safety-matrix.md`
- Create: `docs/release/play-console-checklist.md`
- Create: `docs/release/account-deletion-page.md`
- Test: `app/src/test/java/com/lsing/timego/ReleaseDocumentationTest.kt`

**Interfaces:**
- Consumes: actual manifest permissions and data flows from the approved spec
- Produces: `requiredReleaseDocuments(): List<String>` source-contract test list

- [ ] **Step 1: Write the failing documentation test** asserting all four files exist and contain `com.lsing.timego`, account deletion, report consent, Firebase, retention, and clearly labelled publication-input fields.
- [ ] **Step 2: Run the focused test** and verify it fails on missing files.
- [ ] **Step 3: Write the policy draft and matrix** separating local-only data, optional synchronized data, authentication data, report delivery metadata, third parties, deletion, retention, and user choices.
- [ ] **Step 4: Write the Play checklist** for developer verification, app access, ads, target audience, content rating, Health Apps, Data Safety, privacy URL, deletion URL, store assets, internal/closed testing, AAB inspection, and staged rollout.
- [ ] **Step 5: Write the deletion-page content** with an HTTPS deployment requirement, authenticated deletion route, support-request fallback, identity verification, data categories, and completion notice.
- [ ] **Step 6: Run the focused test and `git diff --check`; expect pass.**
- [ ] **Step 7: Commit** with `docs: add TimeGo Play policy foundation`.

### Task 3: Release verification script

**Files:**
- Create: `scripts/verify-play-release.ps1`
- Modify: `docs/release/PLAY_RELEASE.md`

**Interfaces:**
- Consumes: repository root and optional `-BundlePath`
- Produces: nonzero exit for dirty tree, wrong branch, missing docs, version errors, failed Gradle gate, or invalid supplied AAB

- [ ] **Step 1: Implement parameter parsing and read-only preflight** for branch, status, application ID, target SDK, monotonically positive version code, required documents, and forbidden tracked extensions (`.jks`, `.keystore`, `.p12`).
- [ ] **Step 2: Add Gradle gate execution** in this exact order: `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease`.
- [ ] **Step 3: When `-BundlePath` is supplied**, require an existing `.aab`, call `bundletool`/`jarsigner` only when installed, and report unavailable external tools as an explicit incomplete check rather than success.
- [ ] **Step 4: Document invocation** in `PLAY_RELEASE.md`.
- [ ] **Step 5: Run the script without a bundle** and verify every repository-owned check passes.
- [ ] **Step 6: Commit** with `build: add Play release verification script`.
