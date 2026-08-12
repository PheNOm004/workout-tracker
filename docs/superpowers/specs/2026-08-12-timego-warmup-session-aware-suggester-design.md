# TimeGo — Warmup-Aware, Session-Aware Overload Suggester (design)

**Date**: 2026-08-12
**Status**: approved, not yet implemented
**Context**: Item 2 of the 8-item post-v1 backlog scoped 2026-08-12 (full backlog in the vault, `TimeGo - Gym Progress Tracker.md`). Depended on item 1 (session model + logging landing page, done and merged 2026-08-12) for explicit session boundaries — that dependency is now satisfied. Builds on the 2026-08-11 suggester-plateau-upgrade work (`PlateauStatus`, `classifyPlateauStatus`) rather than replacing it.

## Problem

Two real gaps in `RuleBasedOverloadSuggester`/`RuleBasedHoldSuggester` today:

1. **No warmup concept.** `SetLog` has no way to mark a set as a warmup. Every logged set — including light warmup ramps before working weight — counts toward the suggester's history and the plateau/trend window, diluting or misleading both.
2. **No session awareness in the suggestion itself.** `LogViewModel.refreshSuggestions`/`refreshSuggestionForExercise` build history as a flat list of every `SetLog` for an exercise, ordered by timestamp, with "last" meaning "the most recently logged set" — which could be a set logged 10 minutes ago in *this same session*. Logging a second working set of an exercise mid-session currently re-triggers the full decision table against that flat history, which can suggest jumping the weight again before the session is even over. Overload should be decided once per session (at the start), not escalate set-to-set within one.

## Section 1 — Data model

`SetLog` gains one field:

```kotlin
@Entity(tableName = "set_logs")
data class SetLog(
    // ...existing fields unchanged...
    val isWarmup: Boolean = false,
)
```

Room migration 7→8: `ALTER TABLE set_logs ADD COLUMN isWarmup INTEGER NOT NULL DEFAULT 0`. Every pre-migration set is implicitly a working set (default `false`) — there's no way to retroactively know which historical sets were warmups, and treating them all as working sets matches current behavior exactly (no regression for existing data).

Scope: warmup marking is available for `WEIGHT_REPS` and `HOLD` logging types (the two types with a suggester). `DURATION_DISTANCE` (cardio/warmup-category exercises) has no suggester and gets no warmup toggle — `isWarmup` simply stays `false` for those rows, consistent with the sentinel-field convention `SetLog`'s doc comment already establishes for other type-specific fields.

## Section 2 — Session-level history

New pure function in `domain/`, following the existing `MuscleBalance.kt` pattern of taking `SetLog` directly rather than a pre-shaped domain type:

```kotlin
/** Reduces raw sets for one exercise into one representative set per session -- the session's
 *  last non-warmup set, which is that session's ending effort. Ordered oldest-session-first.
 *  Warmup sets ([SetLog.isWarmup]) are excluded entirely: they never count toward the working-set
 *  baseline or the plateau/trend window. [sessionStartById] resolves chronological session order
 *  (sessions aren't necessarily ordered by id once multiple sessions can land on the same date). */
fun sessionWorkingSetHistory(setLogs: List<SetLog>, sessionStartById: Map<Long, Long>): List<SetLog> =
    setLogs
        .filterNot { it.isWarmup }
        .groupBy { it.sessionId }
        .mapValues { (_, sets) -> sets.maxBy { it.loggedAtEpochMillis } }
        .values
        .sortedBy { sessionStartById[it.sessionId] ?: 0L }
```

This replaces today's flat `historyByExercise[exercise.id]` grouping as the input to plateau classification: the rolling window (`classifyPlateauStatus`, `WINDOW_SIZE = 5` in `PlateauDetection.kt`, unchanged) now sees at most one point per session instead of every raw set — a 3-working-set session contributes one data point to the trend, not three.

## Section 3 — Suggester signature and mid-session lock

Both suggester interfaces change from one flat history parameter to two:

```kotlin
interface OverloadSuggester {
    fun suggestNext(sessionHistory: List<SetPerformance>, currentSessionWorkingSets: List<SetPerformance>): OverloadSuggestion?
}

interface HoldSuggester {
    fun suggestNext(sessionHistory: List<HoldPerformance>, currentSessionWorkingSets: List<HoldPerformance>, exerciseName: String): HoldSuggestion?
}
```

- `sessionHistory`: chronological, one entry per past session (from `sessionWorkingSetHistory`, converted to `SetPerformance`/`HoldPerformance` the same way `LogViewModel` already converts `SetLog` today) — does **not** include the current session.
- `currentSessionWorkingSets`: the active session's own non-warmup sets for this exercise so far, chronological. Empty if no session is active, or a session is active but nothing (or only warmups) has been logged yet for this exercise this session.

`RuleBasedOverloadSuggester.suggestNext`:

```kotlin
override fun suggestNext(sessionHistory: List<SetPerformance>, currentSessionWorkingSets: List<SetPerformance>): OverloadSuggestion? {
    if (currentSessionWorkingSets.isNotEmpty()) {
        val locked = currentSessionWorkingSets.first()
        return OverloadSuggestion(
            weightKg = locked.weightKg,
            reps = locked.targetReps,
            note = "Repeating today's working weight",
            plateauStatus = PlateauStatus.REPEATING,
        )
    }
    if (sessionHistory.isEmpty()) return null
    val last = sessionHistory.last()
    val oneRepMaxes = sessionHistory.map { estimatedOneRepMax(it.weightKg, it.reps) }
    val hitFlags = sessionHistory.map { it.reps >= it.targetReps }
    val status = classifyPlateauStatus(oneRepMaxes, hitFlags)
    // ...unchanged decision table (REGRESSING/PLATEAUING/PROGRESSING) below this point,
    // operating on `sessionHistory` instead of the old flat `history` parameter...
}
```

Once `currentSessionWorkingSets` has an entry, the suggestion locks to that session's **first** working set's weight/target for every subsequent set of that exercise this session — even if you deviate mid-session (e.g. a drop set), the suggestion keeps pointing at the session's original target rather than chasing whatever you most recently logged. No deload, no plateau-hold-steady, no weight increase, and (for `RuleBasedHoldSuggester`) no calisthenics tier-progression check fire mid-session — all of those are between-session decisions now, made exactly once, at the top of a fresh session.

`RuleBasedHoldSuggester.suggestNext` gets the identical top-of-function lock check (same shape, using `durationSeconds`/`targetDurationSeconds` and `HoldSuggestion`), before its existing tier-progression-ceiling check and decision table, both of which continue to operate on `sessionHistory`.

## Section 4 — `PlateauStatus.REPEATING`

```kotlin
enum class PlateauStatus { PROGRESSING, PLATEAUING, REGRESSING, REPEATING }
```

Additive enum case, used only by the mid-session-lock branch above. Per the 2026-08-11 plateau-upgrade spec, `plateauStatus` isn't rendered anywhere in the UI yet (`note` is the only field shown) — this is a safe additive change with no UI migration needed. `classifyPlateauStatus` itself never returns `REPEATING`; it's only ever set directly by the suggesters' lock branch.

## Section 5 — Wiring

**`WorkoutRepository`**: `logSet`/`logHoldSet` gain an `isWarmup: Boolean = false` parameter, passed straight through to the `SetLog` constructor. `logCardioSet` is unchanged (no warmup concept there).

**`LogViewModel`**: `logSet(exerciseId, weightKg, reps, targetReps, isWarmup)` / `logHoldSet(exerciseId, durationSeconds, targetDurationSeconds, isWarmup)` — new trailing parameter, defaulting `false` so any call site that doesn't yet pass it compiles unchanged during development.

`refreshSuggestions`/`refreshSuggestionForExercise` are rebuilt around the new two-list shape:

```kotlin
private suspend fun buildSuggestionInputs(exerciseId: Long): Pair<List<SetLog>, List<SetLog>> {
    val allSets = repository.historyForExercise(exerciseId)
    val sessionStartById = repository.allSessions().associate { it.id to it.startEpochMillis }
    val sessionHistory = sessionWorkingSetHistory(allSets, sessionStartById)
    val activeSessionId = (sessionState.value as? SessionUiState.Active)?.sessionId
    val currentSessionWorkingSets = if (activeSessionId != null) {
        allSets.filter { it.sessionId == activeSessionId && !it.isWarmup }.sortedBy { it.loggedAtEpochMillis }
    } else {
        emptyList()
    }
    return sessionHistory to currentSessionWorkingSets
}
```

Both `refreshSuggestions` (all exercises, called on library load) and `refreshSuggestionForExercise` (single exercise, called after each logged set — the existing perf fix from the 2026-08-10 logging-field-accuracy session, preserved unchanged) use this helper, converting each `SetLog` list to `SetPerformance`/`HoldPerformance` exactly as today, then calling `suggester.suggestNext(sessionHistory, currentSessionWorkingSets)` / `holdSuggester.suggestNext(sessionHistory, currentSessionWorkingSets, exercise.name)`.

**`LogScreen`**: `StrengthLogRow` and `HoldLogRow` each gain a warmup toggle (a `Checkbox` with a "Warmup" label, placed next to the existing "Log set"/"Log hold" button) and an `isWarmup` local state defaulting `false`, reset (`remember(exerciseName)`, matching the existing per-row state pattern) whenever the row's identity changes. The toggle's value is passed through `onLog`'s existing lambda by adding `isWarmup` as a trailing argument to both `onLog` callback shapes and the `viewModel.logSet`/`logHoldSet` calls they wrap. `CardioLogRow` is unchanged — no toggle, no parameter.

## Testing

Full TDD, matching this domain's existing discipline (plain Kotlin, no Android dependency):

- `sessionWorkingSetHistory`: single session with warmups + working sets → only the last working set represents that session (warmups excluded, earlier working sets excluded too); multiple sessions → one entry each, ordered by `sessionStartById`; a session with only warmup sets → contributes nothing (not a zero-value entry, absent entirely); sets from an unknown/missing session id in `sessionStartById` sort using the `0L` fallback (documented, not a crash).
- `RuleBasedOverloadSuggester`/`RuleBasedHoldSuggester`, new lock-branch cases: `currentSessionWorkingSets` non-empty → returns the *first* entry's weight/target regardless of later entries in the list, `plateauStatus = REPEATING`, regardless of what `sessionHistory` would otherwise suggest (construct a `sessionHistory` that would clearly trigger REGRESSING or PROGRESSING, confirm the lock branch overrides it). `currentSessionWorkingSets` empty, `sessionHistory` non-empty → existing REGRESSING/PLATEAUING/PROGRESSING behavior, now driven by session-representative history (reuse/adapt existing `OverloadSuggesterTest`/`HoldSuggesterTest`/`PlateauDetectionTest` cases, replacing their flat-history fixtures with one-per-session fixtures). Both empty → `null` (unchanged "no history yet" behavior).
- Calisthenics tier-progression check (`RuleBasedHoldSuggester`): confirm it's skipped when `currentSessionWorkingSets` is non-empty even if the ceiling condition would otherwise be met, and still fires normally when `currentSessionWorkingSets` is empty.
- Room migration test (7→8): not applicable — this project has no Room instrumented-test infrastructure (all prior migrations verified manually on-device, per the session-model-landing-page plan's precedent); manual on-device verification covers this instead.

**Manual on-device verification**: log a warmup set for an exercise, confirm the suggestion doesn't change/lock yet (still shows the pre-session suggestion). Log a working set, confirm the suggestion for the next set of that exercise now repeats that exact weight/reps with a "Repeating today's working weight" note. Log a second working set at a different weight (simulating a drop set), confirm the suggestion still shows the *first* working set's weight, not the second. End the session, start a new one, confirm the suggestion now reflects the between-session decision table again (deload/hold/increase) rather than repeating. Confirm a HOLD exercise (e.g. a tiered Planche hold) behaves the same way, including that the tier-progression suggestion doesn't fire mid-session even after clearing the 1.5x ceiling on a later same-session set.

## Out of scope

- Any UI surfacing of `PlateauStatus` (including the new `REPEATING` case) beyond the existing `note` text — unchanged from the 2026-08-11 plateau-upgrade spec's scoping.
- Warmup sets for `DURATION_DISTANCE` (cardio/warmup-category) exercises — no suggester exists for that type, so there's nothing for a warmup flag to affect there.
- Retroactively marking any historical `SetLog` as a warmup — migration defaults every existing row to `isWarmup = false`, and there's no way to infer which historical sets were actually warmups from data that was never captured.
- Changing what counts as "the current session" — unchanged from the session-model-landing-page spec: exactly one session can be active at a time, and `currentSessionWorkingSets` is empty whenever `SessionUiState` isn't `Active`.
