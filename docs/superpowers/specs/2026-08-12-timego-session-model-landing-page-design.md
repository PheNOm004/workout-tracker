# TimeGo — Session Model + Logging Landing Page (design)

**Date**: 2026-08-12
**Status**: approved, not yet implemented
**Context**: First of a 5-item post-v1 backlog scoped this session (full backlog recorded in the vault, `TimeGo - Gym Progress Tracker.md`, "Next-work backlog" section). Builds first because two later items depend on it: the warmup-aware/session-aware overload suggester (backlog item 2) needs explicit session state to distinguish "mid-session" from "fresh session"; the Progress-screen heatmap muscle-group summary (backlog item 3) reuses a domain function this spec introduces.

## Problem

`WorkoutSession` today is `{id, date, routineId}` — session identity is a calendar date, not an explicit event. `SessionDao.findByDate` enforces one session per day, created implicitly the moment the first set of that day is logged (`WorkoutRepository.startOrGetTodaySession`, called from every `logSet`/`logCardioSet`/`logHoldSet`). There's no start/end timestamp, no active/closed state, and no "begin workout" action — the Log tab drops straight into the exercise list.

This causes two concrete problems:
1. A workout spanning midnight (e.g. 11pm–12:20am) splits into two unrelated `WorkoutSession` rows purely because the calendar date changed mid-workout.
2. There's no way to tell "still logging this workout" from "starting fresh," which blocks the next backlog item (suggester should not overload mid-session, only between sessions).

There's also no landing step before logging starts — no summary of what you did last time, no nudge toward what to train today.

## Section 1 — Data model

`WorkoutSession` gains explicit boundaries and drops date-based uniqueness:

```kotlin
@Entity(tableName = "sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,              // unchanged: derived from startEpochMillis, still used for heatmap/date-grouped views
    val routineId: Long?,             // unchanged
    val startEpochMillis: Long,       // new
    val endEpochMillis: Long?,        // new — null means active/open
)
```

`date` is retained (not derived on the fly) because the heatmap and consistency-tracking code already query/group by it — recomputing that everywhere would be unnecessary churn. It's set from `startEpochMillis` at creation and never changes.

`SessionDao.findByDate`'s unique-per-date lookup is removed. New queries:
- `findActiveSession(): WorkoutSession?` — `WHERE endEpochMillis IS NULL LIMIT 1`
- `findLastClosedSession(): WorkoutSession?` — `WHERE endEpochMillis IS NOT NULL ORDER BY endEpochMillis DESC LIMIT 1`

At most one active session should exist at a time; this is an app-level invariant (the landing page is the only place a new session gets created, and it only renders when `findActiveSession()` returns null), not a DB constraint.

**Room migration (schema 6→7)**: add `startEpochMillis`/`endEpochMillis` columns to `sessions`, drop the unique index on `date`. Backfill existing rows from their `SetLog`s: `startEpochMillis` = earliest `loggedAtEpochMillis` among that session's sets (fallback: midnight of `date` if a session somehow has no sets), `endEpochMillis` = latest `loggedAtEpochMillis` among that session's sets (never null — all pre-migration sessions are treated as closed).

`SetLog` is unchanged in this spec — no warmup flag here (that's backlog item 2, which depends on this one landing first).

## Section 2 — Session lifecycle

No implicit session creation anymore. `startOrGetTodaySession` is removed; `logSet`/`logCardioSet`/`logHoldSet` require an already-active session (passed in from the logging screen's state, which only exists after Section 3's flow creates one).

**Auto-close check**, run once when the Log tab is opened:
1. `findActiveSession()`.
2. If null → no active session, proceed to Section 3 (landing page).
3. If found → compare `now - lastSetLog.loggedAtEpochMillis` (the active session's most recent `SetLog`) against a 1-hour threshold.
   - `≤ 1hr`: session is still live — skip the landing page, resume directly into the logging screen for this session.
   - `> 1hr`: auto-close it (`endEpochMillis = lastSetLog.loggedAtEpochMillis`, i.e. the moment logging actually stopped, not the moment this check ran), then proceed to Section 3.

This is a pure function over `(activeSession, lastSetLogTime, now) -> SessionAutoCloseResult` (`ACTIVE`/`AUTO_CLOSE`), unit-testable without touching Room, plus a thin repository wrapper that performs the query and, on `AUTO_CLOSE`, writes `endEpochMillis`.

**Manual end**: the logging screen gains an "End Session" action that sets `endEpochMillis = now` immediately and navigates back to the landing page — an explicit alternative to waiting for the 1hr auto-close, not a replacement for it.

## Section 3 — Landing page

Rendered when the Log tab opens and no active session exists (fresh open, or immediately after an auto-close/manual end). Three elements:

**Last-session summary card** — sourced from `findLastClosedSession()`:
- Sets count, duration (`endEpochMillis - startEpochMillis`), and muscle groups worked (new shared domain function, Section 4).
- Tapping the card expands to the full per-set detail table — reuses the existing Set/Name/Weight-or-Duration dialog the heatmap's tap-a-day interaction already renders (`HeatmapGrid`'s `onDateClick` dialog), just triggered from here instead of a heatmap tap.
- If no closed session exists yet (first-ever use), the card is omitted.

**Recommended muscle group** — extends `MuscleBalance.kt`'s existing `untrainedMuscleGroups` (today: binary ≥7-day-untrained flag, Routines-screen-only) into a ranked `rankUntrainedMuscleGroups(...): List<MuscleGroup>` sorted by days-since-last-trained, descending. The landing page shows the top 1-2 entries as "Recommended: Legs (11 days)" — the framing is "trains toward balance," i.e. surfaces whatever's been neglected longest, consistent with the existing nudge's intent. The Routines-screen nudge banner is unchanged (still binary/threshold-based); this is an additive ranked view, not a replacement.

**Start New Session** — same Freeform vs. Routine chip choice the Log screen shows today (routine auto-preselects if one is scheduled for today, unchanged behavior). Selecting either creates the new `WorkoutSession` (`startEpochMillis = now`, `endEpochMillis = null`, `date` derived from `startEpochMillis`) and navigates into the logging screen.

## Section 4 — Shared domain function

```kotlin
fun muscleGroupsWorkedInSession(
    sessionId: Long,
    setLogs: List<SetLog>,
    exercises: List<Exercise>,
): Set<MuscleGroup>
```

Pure function: filters `setLogs` to `sessionId`, maps each to its `Exercise.muscleGroups`, unions the result. Lives in the domain layer alongside `MuscleBalance.kt`. Used by this spec's landing-page summary card; intentionally written generically (session-scoped, not landing-page-specific) so backlog item 3's heatmap workout-summary feature can call it directly later without modification.

## Testing

Full TDD for the new/changed domain functions, matching existing project discipline:
- `SessionAutoCloseResult` decision function: active (≤1hr), auto-close (>1hr) boundary cases including exactly-1hr.
- `rankUntrainedMuscleGroups`: correct descending sort by staleness, ties, all-groups-recently-trained (empty/near-empty result), never-trained groups (should rank highest).
- `muscleGroupsWorkedInSession`: single-exercise session, multi-exercise/multi-muscle-group session, empty session, unions correctly when two exercises share a muscle group.
- Room migration test (6→7): existing sessions backfill correctly from their sets' timestamps; a session with zero sets doesn't crash the migration.
- `SessionDao`: `findActiveSession`/`findLastClosedSession` return correct rows across multiple sessions per day (new: previously impossible under the date-unique constraint).

**Manual on-device verification** (per established project discipline — user verifies, agent doesn't screenshot proactively): start a session, log a set, background the app, reopen within an hour → resumes into logging. Log a set, wait/simulate >1hr, reopen → lands on landing page with the just-finished session as "last session." Tap End Session mid-workout → immediately lands on landing page. Start a session, log across a real midnight boundary → confirm it stays one session (the actual bug this spec fixes).

## Out of scope

- Warmup-set flag and session-aware/no-mid-session-overload suggester logic — backlog item 2, depends on this spec landing first (needs the active/closed session distinction this spec introduces).
- Editing/reopening a past closed session (e.g. logging a forgotten set after the fact) — not raised as a requirement; today's app has no such capability either, unchanged.
- Any UI surfacing of `muscleGroupsWorkedInSession` beyond this spec's landing-page card — the heatmap integration is backlog item 3's scope, to be wired in when that spec is built.
- A true background mechanism (WorkManager/foreground service) to auto-close sessions while the app isn't open — explicitly declined; auto-close only evaluates on next Log-tab open, which is a negligible-cost check (single indexed query + one comparison), not a scan.
- Multiple simultaneous active sessions, or any UI for choosing between concurrent active sessions — the app-level invariant is exactly one active session at a time.
