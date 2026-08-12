# TimeGo — Progress screen polish (design + plan)

Item 3 of the 8-item post-v1 backlog (see `TimeGo - Gym Progress Tracker.md` vault note). Three
independent sub-fixes on the Progress screen. Combined design+plan doc (shortened process for a
small polish item — no separate spec file).

## 1. Timeframe selector

`ProgressViewModel` currently hardcodes `since = LocalDate.now().minusDays(30)` for both
`trainingStats` and `muscleGroupVolumeDistribution`. Replace with a selector: Week / Month / Year /
Lifetime, shown as `FilterChip`s above the "Muscle Distribution" section (same visual pattern as
the existing Strength Curve mode chips). Affects only Muscle Distribution + the 4 stat tiles —
Strength Curve and the heatmap are unaffected (heatmap is already all-time).

- New `enum class ProgressTimeframe { WEEK, MONTH, YEAR, LIFETIME }` in `domain/ProgressMath.kt`
  with `fun ProgressTimeframe.sinceDate(earliestSessionDate: LocalDate?, today: LocalDate): LocalDate`:
  - WEEK → `today.minusDays(7)`, MONTH → `today.minusDays(30)` (matches current behavior),
    YEAR → `today.minusDays(365)`, LIFETIME → `earliestSessionDate ?: today`.
- `ProgressViewModel`: add `_timeframe = MutableStateFlow(ProgressTimeframe.MONTH)` +
  `selectTimeframe(tf)`. Restructure the sessions-collection block to `combine(repository.sessions,
  _timeframe)` so changing the timeframe recomputes stats/distribution without waiting for new data.
- `ProgressScreen.kt`: FilterChip row above "Muscle Distribution", section header and empty-state
  text reflect the selected timeframe (e.g. "Muscle Distribution (this week)").

## 2. PR card decimal fix

`ProgressScreen.kt`'s PR card interpolates `record.value` / `record.value * secondaryValue` raw
(e.g. `61.79999999999999kg`). Format both to 1 decimal with `"%.1f".format(...)`. Scoped to just
the PR card per the vault note — body-metric list weight display is a separate, unflagged spot, left
alone.

## 3. Heatmap workout-summary muscle groups

`WorkoutHistoryDialog` (shared by the Progress screen's tap-a-heatmap-day dialog and the Log
screen's last-session detail) gains an optional `muscleGroups: Set<String> = emptySet()` param —
rendered as a small `AssistChip` row (matches `RoutinesScreen`'s existing non-interactive tag-chip
pattern) under the title, only if non-empty. Only `ProgressScreen`'s call site passes it this pass;
`LogScreen`'s landing-page dialog is unchanged (its own "Trained: X, Y, Z" format issue stays
deferred per the vault note).

- `ProgressViewModel.selectHistoryDate`: compute `Set<String>` by unioning
  `muscleGroupsWorkedInSession(sessionId, sets, exercises)` over every session on that date (a date
  can have >1 session), expose as `_historyMuscleGroups: StateFlow<Set<String>>`.

## Tasks

1. `ProgressTimeframe` enum + `sinceDate()` in domain, with unit tests (each enum case, LIFETIME
   fallback when `earliestSessionDate` is null).
2. Wire timeframe into `ProgressViewModel` (combine flow, `selectTimeframe`) + FilterChip UI +
   dynamic section header/empty-state text.
3. PR card 1-decimal formatting fix.
4. `historyMuscleGroups` in `ProgressViewModel` (reuse `muscleGroupsWorkedInSession`, union across
   sessions for the date) + `WorkoutHistoryDialog` chip row + `ProgressScreen` call site wiring.

Each task: implement, `./gradlew testDebugUnitTest` + `assembleDebug`, commit individually on a
`progress-screen-polish` branch. `installDebug` + on-device verification once all 4 tasks are done,
fast-follow fixes from that feedback before merge, then merge to `master`, delete branch, update
vault note.
