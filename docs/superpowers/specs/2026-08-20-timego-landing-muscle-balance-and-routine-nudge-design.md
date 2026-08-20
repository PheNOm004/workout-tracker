# TimeGo — Landing Muscle Balance Card + Routine Last-Completed Nudge (design)

**Date**: 2026-08-20
**Status**: proposed, not yet approved
**Context**: Deferred follow-up from the Muscle Balance chart spec
(`docs/superpowers/specs/2026-08-20-timego-muscle-balance-chart-design.md`, merged `ebe4a3a`). User
wants the balance chart (or a version of it) surfaced on the Log landing page, alongside a routine
"last-completed" nudge (e.g. "Push Day — 4 days since last completed") that doesn't exist anywhere in
the app today.

## Correction to prior framing

The vault's Feature Catalog previously claimed Routines already shows a "last-completed state." It
doesn't — checked `RoutinesScreen.kt`/`RoutinesViewModel.kt` directly; neither has any such field.
This spec designs it from scratch, not surfaces something that already existed elsewhere.

## Design

### Landing Muscle Balance card

A new third card on the Log landing page (`LogLandingContent` in `LogScreen.kt`), after the existing
Recommended card and before the session-start buttons: Last Session → Recommended → **Muscle
Balance** → Start a session.

- Reuses `muscleBalanceForTimeframe` (`domain/MuscleDistribution.kt`, already built and merged) —
  no changes needed to that function.
- Reuses the `RadarChart` component at the same full size (220dp) as the Progress screen — not a
  condensed version.
- Landing gets its **own independent timeframe state**, separate from the Progress screen's — a
  user reviewing Progress and a user deciding what to train today are different tasks with different
  natural defaults. Landing's selector defaults to `ProgressTimeframe.MONTH` (enough recent signal to
  decide today's session without Year/Lifetime noise), but exposes the same four
  Week/Month/Year/Lifetime tabs via `FilterChip`s, matching Progress's existing pattern exactly.

### Routine last-completed nudge

Attached directly under the Muscle Balance card (same card, not a separate section) — both are
fundamentally "how long since X was trained" signals, just at different granularity (per-muscle vs.
per-routine). Deliberately **not** merged into the Recommended card, which is muscle-group-based with
no existing link to specific routines, and **not** attached to the "Start a session" button row,
which stays exactly as it is today.

One line per routine, sorted **staleest-first** (most useful nudge surfaces at the top):
`"Push Day — 4d ago"`, or `"Push Day — Never logged"` for a routine with no completed session yet.
"Today" for a routine completed earlier the same day.

## New domain function

```kotlin
// domain/RoutineSchedule.kt, alongside the existing routinesForToday

/** Latest date of a *closed* session per routine id. A routine's still-active session doesn't
 *  count as "completed" yet (endEpochMillis == null is excluded), matching the same closed-session
 *  convention used throughout the app (e.g. WorkoutRepository.deleteSession, RoutinesViewModel's
 *  sessionHistory). A routine id absent from the returned map has never been completed. */
fun routineLastCompletedDates(sessions: List<WorkoutSession>): Map<Long, LocalDate> =
    sessions
        .filter { it.endEpochMillis != null && it.routineId != null }
        .groupBy { it.routineId!! }
        .mapValues { (_, group) -> group.maxOf { it.date } }
```

```kotlin
// domain/RoutineSchedule.kt or ui/common/, a small pure formatter -- exact location decided in
// the implementation plan based on which layer's existing helpers it fits alongside.

/** "Today" for the same day, "<n>d ago" for a past date, "Never logged" for null (no completed
 *  session exists for this routine yet). */
fun formatDaysSince(date: LocalDate?, today: LocalDate): String
```

## `LogViewModel` additions

- `_landingBalanceTimeframe: MutableStateFlow<ProgressTimeframe>` (default `MONTH`) +
  `val landingBalanceTimeframe: StateFlow<ProgressTimeframe>` + `fun
  selectLandingBalanceTimeframe(timeframe: ProgressTimeframe)`.
- `_landingMuscleBalance: MutableStateFlow<Map<String, Float>>` — recomputed via
  `muscleBalanceForTimeframe` inside the **existing** `combine(repository.exercises,
  repository.setLogs, repository.sessions)` collector (already collects everything
  `muscleBalanceForTimeframe` needs), gated additionally on `_landingBalanceTimeframe`.
- `_routineLastCompleted: MutableStateFlow<Map<Long, LocalDate>>` — a **new** small
  `combine(repository.routines, repository.sessions)` feeding `routineLastCompletedDates`, since the
  existing routines collector and the existing sessions-bearing combine are currently separate blocks
  and neither alone has both inputs.

## Shared UI helper relocation

`timeframeLabel(timeframe: ProgressTimeframe): String` is currently `private fun` in
`ProgressScreen.kt`. Both screens need identical timeframe labels now, so it moves to `ui/common/`
(a new small file or an existing one with related formatters, decided in the implementation plan) and
loses its `private` modifier. `ProgressScreen.kt` updates its own call site to the relocated import;
no behavior change.

## `LogScreen.kt` changes

In `LogLandingContent`, after the existing Recommended `Surface` block and before the
`isSessionActive`/session-start-buttons block:

```kotlin
SectionHeader("Muscle Balance (${timeframeLabel(landingBalanceTimeframe)})")
Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    ProgressTimeframe.entries.forEach { option ->
        FilterChip(
            selected = landingBalanceTimeframe == option,
            onClick = { onSelectLandingBalanceTimeframe(option) },
            label = { Text(formatEnumLabel(option.name)) },
            modifier = Modifier.padding(end = 4.dp),
        )
    }
}
if (muscleBalance.isNotEmpty()) {
    RadarChart(
        values = orderedMuscleDistributionForChart(muscleBalance)
            .mapKeys { (group, _) -> formatEnumLabel(group) },
        modifier = Modifier.fillMaxWidth().height(220.dp).padding(vertical = 8.dp),
    )
}
routines
    .sortedWith(compareBy(nullsFirst()) { routine -> routineLastCompleted[routine.id] })
    .forEach { routine ->
        val date = routineLastCompleted[routine.id]
        Text("${routine.name} — ${formatDaysSince(date, today)}", ...)
    }
```

`LogLandingContent` gains new parameters (`landingBalanceTimeframe`, `muscleBalance`,
`routineLastCompleted`, `onSelectLandingBalanceTimeframe`) supplied from `LogScreen`'s
`viewModel.landingBalanceTimeframe`/`viewModel.landingMuscleBalance`/`viewModel.routineLastCompleted`
collectors, following the same pattern the existing `summary`/`routines` parameters already use. The
render loop iterates the existing `routines: List<Routine>` parameter already passed to
`LogLandingContent`, not `routineLastCompleted.entries` — a routine with no logged session at all
(absent from `routineLastCompleted`) must still appear in the nudge list as "Never logged," not be
silently omitted. `nullsFirst()` in the sort comparator puts those never-logged routines at the top
(staleest), matching the edge-case rule below.

## Edge cases

- **No routines exist yet**: nudge list renders nothing (empty `routines`), same convention as the
  existing "Recommended" card's `isEmpty()` handling elsewhere on this screen.
- **A routine with no completed session**: absent from `routineLastCompletedDates`'s returned map,
  renders "Never logged," sorts as staleest (oldest) in the staleest-first ordering — a routine
  you've never done is more "due" than one you did last week.
- **`muscleBalance` empty** (no qualifying sets logged in the selected window): the radar chart is
  hidden, same `isNotEmpty()` gate pattern the Progress screen already uses, rather than rendering an
  empty/broken chart.

## Testing

- `routineLastCompletedDates`: a routine with one closed session returns that date; a routine with
  multiple closed sessions returns the latest; a routine's still-*active* session is excluded; a
  freeform session (`routineId == null`) doesn't appear under any routine; a routine with zero
  sessions is absent from the returned map.
- `formatDaysSince`: same date as today → `"Today"`; a past date → `"<n>d ago"` with the correct day
  count; `null` → `"Never logged"`.
- No new `LogViewModel`/`LogScreen` automated tests — matches this project's existing, documented
  limitation (no ViewModel/Compose UI test coverage), same as every other `LogViewModel` field.
  Verified by full build/install + on-device check, same as the Muscle Balance chart spec.

## Out of scope

- **Changing the Progress screen's own Muscle Balance card or its timeframe state** — landing's
  timeframe selection is independent and does not affect or get affected by Progress's.
- **Tapping a nudge line to jump directly into that routine's session** — the existing routine
  buttons in the "Start a session" row already do this; the nudge line itself is informational only,
  matching how the Recommended card's text is informational without its own tap target.
- **Combining the nudge with the Recommended card** — considered and dropped; no existing link
  between recommended muscle groups and specific routines exists, and building one is a separate,
  larger feature not needed for this nudge.
- **A condensed/smaller radar chart variant** — considered and dropped in favor of reusing the
  existing full-size `RadarChart` unchanged.
