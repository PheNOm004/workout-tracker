# TimeGo Security Best-Practices Report

Date: 2026-08-25  
Scope: Android/Kotlin application, Compose UI, Room/DataStore persistence, backup/restore, release manifest, dependencies, and Git exposure

## Executive summary

TimeGo has a strong local-first baseline: the release manifest requests no `INTERNET` permission,
platform backup is disabled and explicitly excluded, app data stays in Android internal storage,
release optimization is enabled, and the current tree contains no detected secret-like values. An
OSV query of all 142 resolved Maven components returned zero known vulnerability matches.

This pass fixed unsafe backup ingestion, UI-thread backup work, misleading backup privacy wording,
invalid numeric persistence, missing-bodyweight calisthenics corruption, and navigation accessibility.
One repository-history hygiene issue remains: a deleted Jupyter log and four dead, token-shaped URL
occurrences are still present in commit `0d3e7ff`. No token value is reproduced in this report.

## Critical findings

None found.

## Repository-history hygiene

### S-01 — Dead Jupyter URLs remain in public Git history (optional cleanup)

- Evidence: `ml-prototype/jupyter.log` exists in commit `0d3e7ff`; the current-tree removal is commit
  `0cf902e`. A content-safe scan counted four token-shaped URL occurrences in the historic blob.
- Impact: anyone who can read the public repository history can recover those URLs. Runtime
  verification shows the process-scoped URLs are dead, so they do not currently grant notebook
  access; retaining them can still trigger secret scanners and is poor repository hygiene.
- Current mitigation: the file is no longer tracked, `.gitignore` blocks the path, and no current-tree
  secret-like match was found. A local runtime check on 2026-08-25 found zero active Jupyter processes
  and confirmed that both historical loopback endpoints on port 8888 were not listening. Because a
  Jupyter server token is scoped to its server process, the recorded URLs are no longer operational.
- Remaining action: if repository hygiene requires the dead URLs to be purged, perform a coordinated
  `git-filter-repo` rewrite and force-push, then address cached GitHub references/clones. This was not
  applied because history rewriting and force-pushing require explicit user approval.
- Guidance: https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository

## Medium priority — fixed

### S-02 — Restore accepted unbounded, unverified files (fixed in `06bd39f`)

- Previous risk: any document-provider input was copied into cache without a limit and opened as a
  Room database. A very large or wrong file could exhaust storage, waste resources, or reach SQLite
  before basic format rejection.
- Change: restore now streams through a 64 MiB cap and verifies the SQLite header before Room opens
  the file (`BackupManager.kt:16-47`, `BackupManager.kt:87-88`).
- Why: document-provider data is user-selected but still untrusted input; cheap boundary validation
  prevents avoidable availability and parser exposure.
- Verification: boundary, over-limit, valid-header, and invalid-header unit tests pass.

### S-03 — Backup work could block the UI thread (fixed in `f72de46`)

- Previous risk: checkpointing and file copies ran in a `viewModelScope` coroutine on the main
  dispatcher, so a large export/restore could freeze the UI or cause an ANR.
- Change: the complete export and restore operations now run on `Dispatchers.IO`
  (`BackupManager.kt:68`, `BackupManager.kt:81`).
- Why: blocking file and SQLite-copy work belongs off the UI thread; Room transaction behavior and
  result handling are unchanged.

### S-04 — Numeric forms could persist invalid analytics data (fixed in `cd45a00`)

- Previous risk: pasted `NaN`/infinity, non-positive metrics, invalid reps, and out-of-range RPE could
  pass parsing or be silently coerced. Calisthenics still used a `0.0` fallback when bodyweight was
  absent, corrupting total load, PRs, curves, and suggestions.
- Change: shared finite/positive parsers now gate workout and body-metric actions; invalid fields show
  error state, and calisthenics is blocked until a valid bodyweight exists (`NumericInput.kt:3-10`,
  `LogScreen.kt:641-708`, `ProgressScreen.kt:94-102`).
- Why: Room is the canonical source of truth, so invalid floating-point values must be rejected before
  persistence rather than repaired downstream.

## Low priority / residual risk

### S-05 — Exported backups are plaintext (accepted with disclosure)

- Evidence: export is a raw SQLite copy. The UI now states that it is unencrypted and that a selected
  cloud-backed document provider may upload it (`RoutinesScreen.kt:234`). TimeGo itself still has no
  network permission.
- Residual risk: anyone with access to the exported file can read workout/body-metric data.
- Better option: add optional password-encrypted exports with authenticated encryption and a recovery
  warning. This is a product decision, not a zero-cost hardening change: forgotten passwords make the
  only recovery copy unusable and format/version migration must be designed and tested.

## Passed controls

- Release merged manifest: no `INTERNET` permission; no TimeGo service/provider/receiver exported.
  `MainActivity` is exported only as the launcher. The AndroidX profile receiver is permission-gated.
- `android:allowBackup="false"` plus Android 11 and Android 12+ exclusion rules cover database,
  preferences, files, external storage, device transfer, and device-protected domains.
- Current tracked tree: no secret-like pattern match in source files.
- Git history path scan: no workout `.db`, SQLite, keystore, `.env`, or credential file path found;
  only the known `ml-prototype/jupyter.log` finding appeared.
- Dependencies: 142 resolved Maven components checked against OSV; zero vulnerability matches at the
  time of this audit.
- Release: R8/resource optimization enabled; unsigned release output remains small and non-debuggable.
- Device verification: all 10 connected tests passed on the Galaxy S23; Log, Progress, and Routines
  rendered without a crash after the final debug build was installed.

## References

- Android security checklist: https://developer.android.com/privacy-and-security/security-tips
- Android Compose lifecycle guidance: https://developer.android.com/develop/ui/compose/state
- GitHub sensitive-data removal: https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository
