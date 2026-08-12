# TimeGo — Exercise timer with configurable delay (design + plan)

Item 4 of the 8-item post-v1 backlog. Combined design+plan doc (shortened process, per established
pattern from item 3).

## Scope

HOLD-type exercises only (planks, dead hangs, levers, etc.) — the ones actively timed live during
a set. `LoggingType.DURATION_DISTANCE` (cardio) stays manual post-hoc entry, unchanged. There is
currently **no live timer at all** — `HoldLogRow` only has a manual "seconds held" text field.

## Design

**Domain** (`domain/HoldTimer.kt`, pure Kotlin, TDD): `HoldTimerPhase` sealed class —
`CountingDown(secondsRemaining: Int)` / `Running(elapsedSeconds: Int)` — with a `tick()` extension:
`CountingDown` decrements until `secondsRemaining <= 1` then jumps to `Running(0)`; `Running`
increments forever. `null`/absent phase represents idle (not part of the sealed class, handled by
the caller as a nullable `HoldTimerPhase?`).

**Settings storage** (`data/SettingsRepository.kt`): wraps Jetpack DataStore (Preferences) — new
dependency `androidx.datastore:datastore-preferences`. `holdDelaySeconds: Flow<Int>` (default 5,
via `intPreferencesKey`), `suspend fun setHoldDelaySeconds(seconds: Int)`. A `Context` extension
property `Context.settingsDataStore` backs it, following the standard DataStore singleton pattern.

**Settings UI**: small stepper row ("Hold delay: [-] 5s [+]", clamped 0-30) at the top of
`RoutinesScreen`, above the existing routine list — wired through `RoutinesViewModel` (new
`holdDelaySeconds: StateFlow<Int>` + `setHoldDelaySeconds(Int)`, backed by `SettingsRepository`).

**HoldLogRow rewrite** (`ui/log/LogScreen.kt`): manual seconds field replaced entirely.
- Idle (`phase == null`): "Start" button + Warmup checkbox (unchanged).
- `CountingDown`: large "Starting in Ns…" text + Cancel button (resets to idle, nothing logged).
- `Running`: large live "Xs" count-up text + "Stop & Log" button.
- Stop calls the existing `onLog(durationSeconds, targetDurationSeconds, isWarmup)` callback
  unchanged — only the row's internal UI/state changes, not the ViewModel contract.
- Ticking: `LaunchedEffect(phase) { delay(1000); phase = phase?.tick() }`.
- If `delaySeconds <= 0`, Start jumps straight to `Running(0)` (skips a zero-length countdown).
- `LogViewModel` gains `holdDelaySeconds: StateFlow<Int>` (reads `SettingsRepository`, default 5),
  `LogScreen` passes it into `HoldLogRow`.

No Room migration — this is a DataStore-backed setting, not a database column.

## Tasks

1. `HoldTimerPhase` + `tick()` in domain, unit tests (countdown decrement, countdown→running
   transition at 1→0, running increments indefinitely).
2. `SettingsRepository` (DataStore) + Gradle dependency; `RoutinesViewModel`/`RoutinesScreen`
   stepper UI.
3. `LogViewModel.holdDelaySeconds` + `HoldLogRow` rewrite (timer states replace the manual field).

Each task: implement, `./gradlew testDebugUnitTest` + `assembleDebug`, commit individually on an
`exercise-timer` branch. `installDebug` + on-device verification once all tasks are done — test a
real hold (countdown visible, count-up visible, Stop logs correct duration, Cancel during countdown
logs nothing, delay setting change on Routines screen actually changes the next hold's countdown)
— fast-follow fixes from that feedback, then merge to `master`, delete branch, update vault note.
