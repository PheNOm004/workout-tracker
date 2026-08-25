# TimeGo Engineering Rules

These rules apply to the entire TimeGo repository. They supplement the user-level instructions in
`C:\Users\lsing\AGENTS.md`; the more specific safety rule wins.

## Project boundary

- Work only in this existing project. Do not create parallel or renamed copies.
- TimeGo is a Kotlin/Jetpack Compose, Room, and Preferences DataStore Android application. It is not
  a JavaScript project.
- Preserve the local-first product boundary. Do not add `INTERNET` permission, analytics, telemetry,
  accounts, cloud sync, or remote services without explicit user approval and a documented privacy
  design.
- The project vault note is the durable source of truth:
  `C:\Users\lsing\.claude\obsidian_demo\Projects\TimeGo - Gym Progress Tracker.md`.

## Change discipline

- Diagnose from current code and runtime evidence before editing.
- Explain what each material change does and why it is necessary.
- Remove demonstrated waste; do not introduce speculative abstractions or micro-optimizations.
- Prefer one observed Room snapshot over repeated one-shot reads. Avoid N+1 queries and duplicate
  subscriptions for the same table in one ViewModel.
- Collect UI flows with lifecycle-aware APIs. Suspend polling, timers, and other repeated work while
  their screen is below `STARTED`, while preserving wall-clock correctness on resume.
- Run blocking database-copy and file operations on an I/O dispatcher, never the Compose/UI thread.
- Reject invalid data at the persistence boundary. Numeric workout/body-metric values must be finite
  and valid for their domain; bodyweight-derived exercises require a valid bodyweight.
- A Room schema change must include a migration, an updated exported schema, and migration tests.
- Do not mix dependency, Gradle, Kotlin, Compose, or SDK upgrades into an unrelated optimization or
  bug-fix pass.

## Security and backup

- Treat files chosen through Android document providers as untrusted input. Keep bounded streaming
  and format validation before Room opens a restore file.
- Backups are raw plaintext SQLite unless an explicitly designed, versioned authenticated-encryption
  format replaces them. Keep the UI disclosure truthful about local and cloud-backed providers.
- Never print, commit, or reproduce secret/token values. Report secret-history evidence using paths,
  commits, and safe counts only.
- Do not rewrite Git history, force-push, publish, or change repository visibility without explicit
  user approval.
- Preserve `allowBackup=false` and the existing backup/data-extraction exclusions unless a reviewed
  recovery design explicitly replaces them.

## Primary-device safety

- The Galaxy S23 is a live-data device, not the default instrumentation-test target.
- Before any phone operation, read the latest device incidents in
  `C:\Users\lsing\.claude\obsidian_demo\Projects\TimeGo\05 Verification and Device State.md` and
  `C:\Users\lsing\.claude\obsidian_demo\Projects\TimeGo\08 Session Log.md`.
- Never run `pm clear`, uninstall TimeGo, delete its private files, or perform a fresh install on the
  primary device without explicit approval and a verified recovery copy.
- Never run `connectedDebugAndroidTest` or another uninstalling instrumentation task on the primary
  phone. Its tests may use isolated databases while Gradle still removes the production package and
  all app-private live data. Use an emulator or dedicated disposable test device only.
- Before any normal install/update on the S23, require a current user-controlled export outside app-
  private storage and explicit approval. Check the installed package and live-data markers before and
  after the operation; stop immediately if they differ unexpectedly.
- UI smoke tests must be non-destructive unless the user asks for data-entry testing. Do not create,
  finish, restore, or delete workouts merely to verify navigation.
- Do not take screenshots of the user's phone unless explicitly requested or necessary to diagnose a
  visual defect. Prefer UI hierarchy inspection and report only the minimum personal data needed.

## Frontend quality

- Check all three main destinations—Log, Progress, and Routines—when shared navigation, state, theme,
  or data-flow code changes.
- Visible labels should provide accessibility names; avoid duplicate icon announcements. Use
  auto-mirrored directional icons where appropriate.
- Invalid form state must be visible and must disable or reject the corresponding action. Never
  silently clamp or coerce bad workout data into a plausible record.
- Preserve the Night Training Console design system and existing responsive behavior unless the user
  requests a design change.

## Verification gates

Use the Android Studio JBR for Gradle:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest lintDebug assembleRelease --console=plain
```

- Run focused tests after each behavioral change and the full JVM/lint/release gate before wrap-up.
- Treat APK size as a regression signal, not proof of runtime speed. Claim performance improvements
  only from concrete work removal or measured profiling evidence.
- Run connected tests only on an emulator or dedicated disposable test device, never the S23.
- For final device verification, install the exact tested debug build, render affected destinations,
  and check Android's crash buffer without modifying live workout data.

## Git and wrap-up

- Preserve unrelated user changes and keep commits narrow, reversible, and named by outcome.
- Do not use destructive cleanup commands or discard work without explicit approval.
- Never push automatically. Report the branch, ahead/behind state, test evidence, and whether commits
  remain local.
- Update the focused TimeGo vault notes with durable decisions and verification evidence.
- Finish with a concise what/why summary, tests performed, residual risks/product decisions, and links
  to any audit reports.
