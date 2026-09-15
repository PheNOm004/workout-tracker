# TimeGo Public Launch, Accounts, Reports, Onboarding, and Exercise Guidance

**Date:** 2026-09-14
**Status:** Approved by user
**Branch:** `development/timego-public-foundation`
**Target:** TimeGo Android app (`com.lsing.timego`)

## 1. Objective

Make a public Google Play launch achievable without weakening TimeGo's in-gym reliability. Add the
foundation for optional email accounts, cloud-backed weekly and monthly email reports, and a
first-run physical/training profile used by the recommendation system. Replace the hand-maintained
exercise metadata path with a versioned catalogue that fully documents 200 common exercises while
keeping all 820 current exercises usable.

The app remains useful without an account. Starting, logging, ending, reviewing, and restoring a
workout must continue to work without a network connection. `master` is not changed during this
work; delivery stays on `development/timego-public-foundation` until the user approves a merge.

## 2. Delivery Decomposition

This programme is delivered as five independently verifiable stages:

1. Public-release and privacy foundation.
2. First-run training profile and AI integration.
3. Versioned exercise catalogue and exercise-detail experience.
4. Optional account, synchronization, deletion, and consent foundation.
5. Weekly/monthly report generation and email delivery.

Each stage must leave the app buildable and locally usable. Cloud-dependent stages use Firebase
emulators and injected fake implementations until the user separately supplies or authorizes real
project configuration, billing, domains, and email-provider credentials.

## 3. Core Architecture

### 3.1 Local-first invariant

Room remains the canonical operational store on the phone. UI and recommendation reads never query
Firebase directly. Local writes complete before synchronization is attempted. A failed or absent
network can delay backup and reports but cannot prevent normal app use.

Existing numeric Room primary keys remain local database identities. Synchronizable records gain a
stable UUID, `updatedAt`, and synchronization state through additive migrations. Deletions that must
propagate use tombstones until acknowledged rather than immediately losing remote identity.

### 3.2 Component boundaries

- `ProfileRepository`: owns the local onboarding/training profile and exposes the subset consumed by
  recommendation code.
- `CatalogueRepository`: exposes locally validated exercise guidance and variation relationships.
- `AuthRepository`: exposes signed-out, guest, unverified, and verified-account states without
  leaking Firebase types into UI/domain code.
- `SyncRepository`: uploads/downloads account-owned records in batches and records per-record state.
- `ReportRepository`: builds deterministic report models from canonical workout data.
- `ReportSubscriptionRepository`: owns explicit weekly/monthly email preferences and consent state.
- Backend scheduled functions: select due subscriptions, build or retrieve report aggregates, send
  idempotently, and record delivery outcome without exposing credentials to the Android client.

Interfaces must have local/fake implementations so onboarding, reports, deletion, and offline
behavior can be tested without a production cloud project.

## 4. First-Run Profile and AI Inputs

### 4.1 Onboarding flow

On first launch, after a short privacy/local-first introduction, collect:

- display name or nickname (optional);
- age range, not date of birth (`UNDER_18`, `18_24`, `25_34`, `35_44`, `45_54`, `55_64`, `65_PLUS`,
  `PREFER_NOT_TO_SAY`);
- height and body weight (optional, metric/imperial UI normalized to metric storage);
- experience (`BEGINNER`, `NOVICE`, `INTERMEDIATE`, `ADVANCED`);
- primary goal (`GENERAL_FITNESS`, `STRENGTH`, `HYPERTROPHY`, `ENDURANCE`, `SKILL`);
- preferred modality (`BALANCED`, `STRENGTH`, `CALISTHENICS`);
- available equipment set;
- intended training days per week and preferred session-duration range;
- optional movement limitations as structured body regions plus free-text private note.

Only experience, goal, modality, equipment, availability, and non-medical movement exclusions may
influence exercise selection initially. Height, weight, and age range are stored for future
calibration and transparent body metrics; they must not silently produce medical, nutrition, or
unsafe load advice. Free-text limitation notes are never passed into a model or emailed.

### 4.2 Completion and editing

Required fields are experience, goal, modality, equipment, days, and duration. Sensitive/physical
fields are skippable. Users can go back, edit the profile from Settings, see which answers affect
recommendations, reset recommendation preferences, or delete the profile. Existing installations
receive a dismissible onboarding invitation and retain their current defaults until completed.

The onboarding completion version is stored so future questions can be introduced deliberately
without replaying the whole flow.

### 4.3 Recommendation contract

Profile inputs filter unsuitable/unavailable candidates and establish priors; actual workout logs
remain the stronger evidence after sufficient use. Every recommendation retains a plain-language
reason. Missing profile values use conservative defaults. The recommender must abstain when it lacks
compatible candidates and must never infer injury status from ordinary workout performance.

## 5. Exercise Catalogue and Guidance

### 5.1 Source and runtime behavior

`catalogue/exercises.json` is the reviewable development source of truth. A deterministic validator
and generator produce the bundled application snapshot. The shipped app does not depend on GitHub
at runtime in this programme. Catalogue changes reach users through signed Play releases.

Room remains the runtime source. Catalogue import is transactional, idempotent, keyed by immutable
`catalogueKey`, and never overwrites custom exercises. Referenced built-ins are deprecated rather
than deleted so historical sessions and routines remain readable.

### 5.2 Guidance model

Each catalogue entry supports:

- canonical name and searchable aliases;
- category, logging type, movement pattern, equipment, difficulty, and complexity;
- primary/secondary muscle contributions consistent with existing `muscleWeights`;
- short purpose statement;
- setup and ordered execution steps;
- concise technique cues and common mistakes;
- easier-variation key and optional harder-variation keys;
- contraindication-style caution phrased as general safety information, never diagnosis;
- content-review status and catalogue/content version.

Variation links must target existing immutable catalogue keys, contain no self-links, and form no
cycles in the easier direction.

### 5.3 The first 200

Exactly 200 common exercises receive complete reviewed guidance in the first release. Selection is
balanced but weighted toward exercises available in a normal commercial gym:

- broad coverage of squat, hinge, horizontal/vertical push and pull, carry, locomotion, rotation,
  anti-rotation, flexion/extension, mobility, and conditioning patterns;
- every tracked muscle group has multiple accessible choices;
- strength receives the largest share, followed by calisthenics, then warm-up/mobility and cardio;
- common machine, cable, dumbbell, barbell, kettlebell, bodyweight, and basic cardio equipment are
  represented;
- duplicate-looking specialty variants do not displace foundational movements.

The other approximately 620 entries remain searchable, selectable, routinable, and loggable. A
niche, advanced, or complex entry shows its existing metadata plus a prominent `Start with` link to
an easier common variation when a valid relationship exists. It never claims full instructions are
available when content has not been reviewed.

### 5.4 Exercise-detail experience

An exercise detail sheet/page is reachable from the catalogue, active workout, routines, and
recommendations without discarding current navigation or input state. It shows guidance, muscles,
equipment, personal last performance/record, and variation links. `Add to workout` and `Add to
routine` reuse existing flows. Guidance content is available offline.

## 6. Accounts, Consent, and Synchronization

### 6.1 Authentication

Firebase Authentication provides optional email/password registration, email verification, sign-in,
password reset, token refresh, sign-out, and account deletion. Email-enumeration protection is
enabled. Error text does not reveal whether an email is registered.

Guest use is first-class, not a temporary anonymous Firebase account. Creating an account does not
automatically upload existing data. The user receives a clear choice to enable cloud backup and
include existing workout history.

### 6.2 Consent separation

These controls are independent and off by default:

- cloud workout backup/synchronization;
- weekly report email;
- monthly report email.

Account creation records acceptance of the current privacy/terms versions but is not bundled consent
for optional email. Report consent records timestamp, policy version, destination, cadence, timezone,
and latest confirmation. Every report contains an unsubscribe/manage link.

### 6.3 Remote data

Firestore stores only data required for the enabled features, partitioned by Firebase UID:

- account profile and consent versions;
- the sync-safe portion of the structured training profile;
- opted-in workout/session/set/routine data with stable UUIDs;
- report subscription settings, timezone, aggregate snapshots, and delivery ledger.

Private limitation notes remain local. No advertising identifier, contacts, location, or unrelated
device information is collected. Security rules deny cross-user access and default to denial.
Administrative SDK access exists only in backend functions.

### 6.4 Sync and conflicts

Synchronization runs through WorkManager when authenticated, verified, opted in, and connected. It
uses bounded batches, exponential backoff, and idempotent upserts. Local edits remain visible
immediately. Initial implementation supports one active writer device per account; a second device
may restore/consume synced data but concurrent-edit conflict handling is not claimed until explicitly
implemented and tested.

The first upload is previewed with record counts. Remote records never silently replace a non-empty
local database. Restore/merge reuses stable catalogue keys and the existing additive backup merge
principles.

### 6.5 Deletion

Settings provides `Delete account and cloud data`, requiring recent authentication and explicit
confirmation. Backend deletion removes authentication identity, Firestore records, report jobs, and
delivery metadata, then returns a deletion receipt/status. Local data is separately offered for
deletion; it is not silently erased merely because the user wants to stop cloud services.

A public HTTPS deletion page lets users initiate or request the same deletion outside the app, as
required for Play-distributed apps that create accounts. Retention exceptions, if ever necessary,
must be specific and published rather than invented in implementation.

## 7. Weekly and Monthly Reports

### 7.1 Report content

Reports are useful summaries rather than raw database dumps.

Weekly reports include:

- sessions, active days, duration, sets, and category-appropriate volume;
- muscles trained and notable coverage gaps;
- personal records and meaningful progression;
- consistency versus the user's intended training days;
- one conservative, explainable next-week suggestion.

Monthly reports include the same measures as trends, comparisons with the preceding month when
enough data exists, body-metric change only when the user has recorded it, and a compact exercise/
muscle progression summary. Weight/repetition, hold, and duration/distance exercises are not merged
into a misleading universal volume number.

### 7.2 Delivery

Firebase scheduled functions run due-report selection in UTC while respecting each subscription's
IANA timezone. Delivery is idempotent per `user + cadence + period`; scheduler retries cannot send a
duplicate. A transactional email provider is called only from the backend. Credentials live in
managed secrets, never the repository or APK.

The Android app can preview the exact report model locally. A user can send a verified test report,
change cadence, pause reports, or unsubscribe. Empty periods do not generate motivational fiction;
they send a short truthful no-workouts summary or skip according to the user's setting.

Delivery logs retain status and provider message identity but not a second unnecessary copy of the
complete report body.

## 8. Google Play Launch Foundation

The programme produces repository-owned, non-secret release artifacts and checklists:

- release signing configuration that consumes local/CI secrets and cannot fall back to debug signing;
- signed AAB build instructions and versioning rules;
- privacy-policy draft covering local data, Firebase, email delivery, retention, and deletion;
- Data Safety mapping tied to actual code/data flows;
- Health Apps declaration rationale for workout/fitness functionality;
- account-deletion URL requirement and implementation status;
- store-listing copy/asset checklist, reviewer instructions, and test-account process;
- internal/closed-test plan, including the applicable personal-account tester requirement;
- pre-launch report, accessibility, offline, upgrade, migration, and release performance gates.

Real keystores, Firebase configuration, domains, Play Console actions, billing, provider accounts,
and credentials require explicit user action/authorization and are never committed.

## 9. Security, Privacy, and Failure Behavior

- TLS-only networking; no cleartext fallback.
- No passwords, tokens, Firebase service credentials, SMTP/API keys, keystores, or secret values in
  Git, logs, analytics, crash text, backups, or UI state.
- Authentication tokens use the provider SDK and platform-protected storage; TimeGo never stores a
  plaintext password.
- Remote rules and backend authorization tests cover ownership and denial paths.
- Email links are HTTPS and resistant to UID/email substitution.
- Catalogue parsing rejects unknown schema versions, duplicate keys, invalid weights, broken links,
  unsupported enums, and oversized content.
- Failed auth, sync, report, or catalogue operations retain valid local data and surface actionable,
  privacy-safe states.
- No telemetry or analytics SDK is introduced by default.

## 10. Verification and Acceptance

Every stage runs the established Android gate:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Add `assembleDebugAndroidTest` when interaction tests change. Release readiness additionally requires:

- migration tests from current schema 15 and representative older exported schemas;
- catalogue schema/generator/golden tests and exactly-200 completeness validation;
- onboarding validation, skip/edit/reset, unit conversion, and existing-user upgrade tests;
- deterministic weekly/monthly report golden tests across empty, partial, mixed-modality, DST, and
  month/year-boundary periods;
- Firebase emulator auth/rules/sync/deletion tests, including unauthorized cross-user access;
- offline-first tests with network unavailable during launch, workout, sync, and sign-out;
- idempotent scheduler and duplicate-email prevention tests;
- account/data deletion end-to-end verification;
- signed AAB inspection, Play internal-test installation, update/migration, and pre-launch report;
- user-owned S23 acceptance after explicit install approval and protected backup, with no automated
  interaction on the primary phone.

## 11. Rollout and Merge Gates

Each stage receives its own commits on the development branch. Do not merge merely because builds
pass. Before merge, the exact candidate must have:

1. completed local and emulator gates;
2. reviewed privacy/Data Safety mappings matching actual behavior;
3. no committed secret or debug-only production dependency;
4. user acceptance of the onboarding and exercise-detail experience;
5. a working deletion path before account creation is enabled in a public track;
6. verified report unsubscribe and duplicate prevention before scheduled email is enabled;
7. a signed Play internal-test AAB and successful upgrade test;
8. explicit user approval to merge into `master`.

## 12. Explicit Non-Goals

- Mandatory login, social profiles, leaderboards, advertising, subscriptions, or public workout data.
- Real-time collaborative editing or an unqualified multi-device conflict claim.
- Runtime dependence on GitHub for exercise availability.
- Medical diagnosis, injury rehabilitation, nutrition prescriptions, or guaranteed outcomes.
- Generating unreviewed exercise instructions merely to claim all 820 entries are complete.
- Sending email directly from the Android client or embedding provider credentials in the app.
- Publishing, spending money, creating cloud accounts, changing credentials, installing on the S23,
  or merging to `master` without the required user-controlled steps.
