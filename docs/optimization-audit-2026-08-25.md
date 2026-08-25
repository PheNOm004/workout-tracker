# TimeGo Optimization Audit — 2026-08-25

## Baseline

- Branch/commit: `master` at `fc7c235`; working tree clean.
- JVM suite: 252 tests, zero failures.
- Android lint: zero errors, 21 maintenance warnings.
- Optimized unsigned release APK: 1,954,488 bytes.
- Baseline combined unit/lint/release command: 149.82 seconds with mixed up-to-date/executed tasks.
- Prior 2026-08-23 audit decisions were treated as constraints; previously rejected micro-
  optimizations were not retried without new evidence.

## Applied changes — what and why

| Commit | What changed | Why it is worth keeping | Evidence |
|---|---|---|---|
| `94a59a9` | Log calculations reuse the observed sessions/sets/exercises snapshot; duplicate per-exercise refresh plumbing removed. | A logged-set emission previously triggered up to eight one-shot table rereads plus two explicit suggestion reads. The observed Room snapshot already contained the same data. | 34 net lines removed; hot flow path now performs zero one-shot rereads; unit suite and S23 Ultra Log landing smoke check passed. |
| `60d762e` | Routines observes all routine links once and derives routine exercises, ranking, stale groups, and history from one combined snapshot. | Removed N per-routine reads and reduced exercises/set-logs/sessions subscriptions from 3/3/2 to 1/1/1 within the ViewModel. | Unit suite passed; S23 Ultra Routines UI-tree smoke check passed with empty crash buffer. |
| `0deb482` | All 43 Compose Flow reads use lifecycle-aware collection; timer polling suspends below `STARTED`. | Backgrounded UI no longer collects screen state or polls a 250 ms timer; elapsed time remains wall-clock based on resume. | Unit/lint/release green; lifecycle artifact was already transitively packaged, so release APK delta was exactly 0 bytes at this checkpoint. |
| `06bd39f` | Restore adds a 64 MiB streaming cap and SQLite-header validation; backup UI describes the real privacy boundary. | Prevents cache exhaustion/wrong-file parsing and removes misleading “fully offline” wording when a cloud-backed document provider may be chosen. | Four focused validation cases pass; lint green. |
| `25cdde1` | Bottom-navigation icons auto-mirror and no longer duplicate their visible label for screen readers. | Correct RTL directionality and less repetitive accessibility output with no visual change in English. | Kotlin compile and lint green. |
| `f72de46` | Backup export/restore moved to `Dispatchers.IO`. | File copying/checkpointing can no longer freeze Compose or cause a main-thread ANR. | Focused backup tests and compile green. |
| `cd45a00` | Shared finite/positive numeric validation, error styling, disabled invalid actions, and missing-bodyweight protection. | Prevents `NaN`/infinity/non-positive analytics and closes the remaining `0.0 + added load` calisthenics corruption path. | JVM suite increased to 257 tests, zero failures; lint green. |
| `4d1849e` | Progress curves/day details derive from live snapshots; dead selection queries and wrappers removed. | Removes selection-time table reads and fixes retained Progress screens showing stale curves after new logs. | 26 net lines removed; full 257-test suite and lint green. |

## Deliberately not changed

| Idea | Verdict | Why |
|---|---|---|
| Upgrade AGP/Kotlin/Compose/AndroidX during this pass | Deferred | Lint update notices are compatibility work, not measured optimization. OSV found zero matches across 142 resolved Maven components; upgrades need a separate regression/device pass. |
| Delete seven legacy color resources | Rejected | R8 already strips them from the optimized release; source deletion produces no release-size or runtime gain. |
| Cache `SurfaceCard` draw brushes | Rejected | Size-aware invalidation adds complexity and the prior audit found no profiler evidence that brush allocation is material. |
| Rewrite capped exercise search or virtualize the one-open category | Rejected | The 40-result cap bounds composition and no device typing/expansion jank is observed. Retain until a trace shows a bottleneck. |
| Encrypt every backup by default | Product decision | Stronger confidentiality, but password loss can destroy the only recovery path and requires a versioned encrypted format. The current pass adds accurate plaintext disclosure. |
| Purge historic Jupyter log immediately | Awaiting approval | Runtime verification found no Jupyter process and no listener at either historical loopback endpoint, so the URLs are dead. Removing them from public history still requires a destructive rewrite and force-push with clone/cache coordination. See `security_best_practices_report.md` S-01. |

## Verification ledger

- Full unit suite: 257 tests, zero failures.
- Lint: zero errors; remaining warnings are dependency/target maintenance, public Compose modifier
  ordering, and resources already removed from release output.
- Current-tree secret-pattern scan: no matches.
- Historical Jupyter runtime check: zero active Jupyter processes; both historical loopback endpoints
  on port 8888 not listening, so the old process-scoped URLs are no longer operational.
- OSV resolved dependency scan: 142 components, zero vulnerability matches.
- Connected Android suite: 10 tests passed on the Galaxy S23 (`SM-S918B`, Android 16), but this was
  not a safe primary-device test. Although the tests use isolated storage, Gradle removed the TimeGo
  package afterward and erased its app-private live data. The user detected the loss and restored the
  data from their backup. Connected instrumentation is now prohibited on the primary phone and must
  use an emulator or dedicated disposable device.
- Final on-device smoke check: after the user's restore, the current debug build showed the restored
  Log data; Log, Progress, and Routines rendered, navigation worked in both directions, and Android's
  crash buffer remained empty. The earlier conclusion that the data had survived was incorrect.
- Final optimized unsigned release APK: 1,954,488 bytes, exactly 0 bytes different from baseline.
- Final combined unit/lint/release gate: successful in 78.61 seconds (25 tasks executed, 56 up to
  date). This confirms the gate only; it is not treated as a speed comparison with the differently
  cached baseline run.
