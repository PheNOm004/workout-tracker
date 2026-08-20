# Landing Muscle Balance Card + Routine Nudge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Muscle Balance radar chart (with its own independent, Month-defaulted timeframe selector) and a routine "last-completed" nudge list to the Log landing page, as a new third card after the existing Recommended card.

**Architecture:** Two new small pure domain functions (`routineLastCompletedDates`, `formatDaysSince`) in `RoutineSchedule.kt`; a relocated, no-longer-private `timeframeLabel` shared between the Progress and Log screens; `LogViewModel` gains three new pieces of state computed from data it (mostly) already collects; `LogScreen`'s `LogLandingContent` renders the new card using the exact same `RadarChart`/`FilterChip` pattern the Progress screen already uses.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit4 (`testDebugUnitTest`), Gradle.

**Spec:** `docs/superpowers/specs/2026-08-20-timego-landing-muscle-balance-and-routine-nudge-design.md` (commit `5ef218e`)

## Global Constraints

- Landing's timeframe state is independent of the Progress screen's — defaults to `ProgressTimeframe.MONTH`, never reads or writes Progress's own timeframe.
- The new card sits after the existing Recommended card, before the session-start buttons — `LogLandingContent`'s existing sections stay in their current order otherwise.
- The routine nudge list must include a routine with **no** completed session, rendered as "Never logged," sorted staleest-first (never-logged routines sort first).
- `muscleDistributionForTimeframe`/`muscleGroupVolumeDistribution` (volume-based, drives the Progress body-diagram heatmap) are not touched by this plan at all.
- No new `LogViewModel`/`LogScreen` automated tests — this project has no ViewModel/Compose UI test coverage (documented existing limitation); verified by full build/install + on-device check instead.

---

### Task 1: `routineLastCompletedDates`

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/RoutineSchedule.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/RoutineScheduleTest.kt`

**Interfaces:**
- Produces: `fun routineLastCompletedDates(sessions: List<WorkoutSession>): Map<Long, LocalDate>`

- [ ] **Step 1: Write the failing tests**

Add to `RoutineScheduleTest.kt` (inside the existing `class RoutineScheduleTest { ... }`, before its closing brace). Add these imports at the top of the file alongside the existing ones: `import com.lsing.timego.data.WorkoutSession` and `import java.time.LocalDate`.

```kotlin
    @Test
    fun `routineLastCompletedDates returns the latest closed-session date per routine`() {
        val sessions = listOf(
            WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 9), routineId = 1, startEpochMillis = 0, endEpochMillis = 0),
            WorkoutSession(id = 2, date = LocalDate.of(2026, 8, 16), routineId = 1, startEpochMillis = 0, endEpochMillis = 0),
        )

        val result = routineLastCompletedDates(sessions)

        assertEquals(LocalDate.of(2026, 8, 16), result[1L])
    }

    @Test
    fun `routineLastCompletedDates excludes a still-active session`() {
        val sessions = listOf(
            WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 9), routineId = 1, startEpochMillis = 0, endEpochMillis = 0),
            // Active session (endEpochMillis == null) for the same routine, more recent -- must not win.
            WorkoutSession(id = 2, date = LocalDate.of(2026, 8, 20), routineId = 1, startEpochMillis = 0, endEpochMillis = null),
        )

        val result = routineLastCompletedDates(sessions)

        assertEquals(LocalDate.of(2026, 8, 9), result[1L])
    }

    @Test
    fun `routineLastCompletedDates excludes freeform sessions and omits routines with no completed session`() {
        val sessions = listOf(
            // Freeform session, no routine -- must not appear under any routine id.
            WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 9), routineId = null, startEpochMillis = 0, endEpochMillis = 0),
        )

        val result = routineLastCompletedDates(sessions)

        assertEquals(emptyMap<Long, LocalDate>(), result)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.RoutineScheduleTest"`
Expected: FAIL — `routineLastCompletedDates` is unresolved.

- [ ] **Step 3: Implement `routineLastCompletedDates`**

Add to `RoutineSchedule.kt`, below `routinesForToday`. Add `import com.lsing.timego.data.WorkoutSession` and `import java.time.LocalDate` to the file's existing imports (alongside `com.lsing.timego.data.Routine` and `java.time.DayOfWeek`).

```kotlin
/** Latest date of a *closed* session per routine id. A routine's still-active session doesn't
 *  count as "completed" yet (endEpochMillis == null is excluded), matching the same closed-
 *  session convention used elsewhere (e.g. WorkoutRepository.deleteSession,
 *  RoutinesViewModel.sessionHistory). A routine id absent from the returned map has never been
 *  completed -- callers must not assume every routine has an entry. */
fun routineLastCompletedDates(sessions: List<WorkoutSession>): Map<Long, LocalDate> =
    sessions
        .filter { it.endEpochMillis != null && it.routineId != null }
        .groupBy { it.routineId!! }
        .mapValues { (_, group) -> group.maxOf { it.date } }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.RoutineScheduleTest"`
Expected: PASS, all tests including the three new ones.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/RoutineSchedule.kt app/src/test/java/com/lsing/timego/domain/RoutineScheduleTest.kt
git commit -m "Add routineLastCompletedDates"
```

---

### Task 2: `formatDaysSince`

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/domain/RoutineSchedule.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/RoutineScheduleTest.kt`

**Interfaces:**
- Produces: `fun formatDaysSince(date: LocalDate?, today: LocalDate): String`

- [ ] **Step 1: Write the failing tests**

Add to `RoutineScheduleTest.kt`:

```kotlin
    @Test
    fun `formatDaysSince reports Today for the same date`() {
        assertEquals("Today", formatDaysSince(LocalDate.of(2026, 8, 20), today = LocalDate.of(2026, 8, 20)))
    }

    @Test
    fun `formatDaysSince reports elapsed days for a past date`() {
        assertEquals("4d ago", formatDaysSince(LocalDate.of(2026, 8, 16), today = LocalDate.of(2026, 8, 20)))
        assertEquals("1d ago", formatDaysSince(LocalDate.of(2026, 8, 19), today = LocalDate.of(2026, 8, 20)))
    }

    @Test
    fun `formatDaysSince reports Never logged for null`() {
        assertEquals("Never logged", formatDaysSince(null, today = LocalDate.of(2026, 8, 20)))
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.RoutineScheduleTest"`
Expected: FAIL — `formatDaysSince` is unresolved.

- [ ] **Step 3: Implement `formatDaysSince`**

Add to `RoutineSchedule.kt`, below `routineLastCompletedDates`. Add `import java.time.temporal.ChronoUnit` to the file's imports.

```kotlin
/** "Today" for the same day, "<n>d ago" for a past date, "Never logged" for null (no completed
 *  session exists for this routine yet) -- backs the landing page's routine nudge list. */
fun formatDaysSince(date: LocalDate?, today: LocalDate): String {
    if (date == null) return "Never logged"
    val days = ChronoUnit.DAYS.between(date, today)
    return if (days <= 0) "Today" else "${days}d ago"
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.RoutineScheduleTest"`
Expected: PASS, all six tests in the file.

- [ ] **Step 5: Run the full unit test suite**

Run: `./gradlew.bat testDebugUnitTest`
Expected: PASS — regression check that nothing else in `RoutineSchedule.kt`'s existing test coverage moved.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/domain/RoutineSchedule.kt app/src/test/java/com/lsing/timego/domain/RoutineScheduleTest.kt
git commit -m "Add formatDaysSince"
```

---

### Task 3: Relocate `timeframeLabel` to a shared location

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/common/ExerciseListSections.kt` (add the function here — this is the existing shared-formatter file both `ProgressScreen.kt` and `LogScreen.kt` already import from, via `formatEnumLabel`/`formatMuscleGroupList`; no new file needed for one small function)
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt:378-385` (remove the private function, add the import)

**Interfaces:**
- Produces: `fun timeframeLabel(timeframe: ProgressTimeframe): String` (public, in `com.lsing.timego.ui.common`)

No new test — this is a pure relocation of unchanged logic, not new behavior. Verified by the full suite (Step 3 below) plus Task 6's on-device check.

- [ ] **Step 1: Move the function**

In `ExerciseListSections.kt`, add near the top of the file, directly below the existing `formatEnumLabel` function (after its closing brace, before `private enum class SessionBodyRegion`):

```kotlin
/** "last 7 days" / "last 30 days" / "last 12 months" / "lifetime" -- shared between the Progress
 *  screen's own Muscle Balance card and the Log landing page's, which has its own independent
 *  timeframe selection but needs identical labels. */
fun timeframeLabel(timeframe: ProgressTimeframe): String = when (timeframe) {
    ProgressTimeframe.WEEK -> "last 7 days"
    ProgressTimeframe.MONTH -> "last 30 days"
    ProgressTimeframe.YEAR -> "last 12 months"
    ProgressTimeframe.LIFETIME -> "lifetime"
}
```

Add `import com.lsing.timego.domain.ProgressTimeframe` to `ExerciseListSections.kt`'s existing import block.

- [ ] **Step 2: Remove the old copy and update the call site in `ProgressScreen.kt`**

Delete these lines from `ProgressScreen.kt` (currently lines 380-385):

```kotlin
private fun timeframeLabel(timeframe: ProgressTimeframe): String = when (timeframe) {
    ProgressTimeframe.WEEK -> "last 7 days"
    ProgressTimeframe.MONTH -> "last 30 days"
    ProgressTimeframe.YEAR -> "last 12 months"
    ProgressTimeframe.LIFETIME -> "lifetime"
}
```

`ProgressScreen.kt` already imports `com.lsing.timego.ui.common.formatEnumLabel` and `com.lsing.timego.ui.common.orderedMuscleDistributionForChart` — add `import com.lsing.timego.ui.common.timeframeLabel` alongside them. Every existing call to `timeframeLabel(timeframe)` inside `ProgressScreen.kt` keeps working unchanged, now resolving to the relocated shared function.

- [ ] **Step 3: Run the full unit test suite**

Run: `./gradlew.bat testDebugUnitTest`
Expected: PASS — confirms the relocation didn't break compilation or any existing test.

- [ ] **Step 4: Compile to double-check the Compose call sites**

Run: `./gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/common/ExerciseListSections.kt app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt
git commit -m "Relocate timeframeLabel to a shared location for reuse by the Log landing page"
```

---

### Task 4: Wire landing balance timeframe, balance score, and routine nudge state into `LogViewModel`

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt`

**Interfaces:**
- Consumes: `muscleBalanceForTimeframe(...)` (existing, `domain/MuscleDistribution.kt`), `routineLastCompletedDates(sessions: List<WorkoutSession>): Map<Long, LocalDate>` (Task 1)
- Produces: `val landingBalanceTimeframe: StateFlow<ProgressTimeframe>`, `val landingMuscleBalance: StateFlow<Map<String, Float>>`, `val routineLastCompleted: StateFlow<Map<Long, LocalDate>>`, `fun selectLandingBalanceTimeframe(timeframe: ProgressTimeframe)`

No new test for this task — matches the existing project-wide lack of `LogViewModel` unit coverage. Verified by Task 6's build/install/on-device pass.

- [ ] **Step 1: Add imports**

Add to `LogViewModel.kt`'s existing import block:

```kotlin
import com.lsing.timego.domain.ProgressTimeframe
import com.lsing.timego.domain.muscleBalanceForTimeframe
import com.lsing.timego.domain.routineLastCompletedDates
```

- [ ] **Step 2: Add the new backing state**

Add directly below the existing `_landingSummary`/`landingSummary` pair (after line 125's `val landingSummary: StateFlow<LandingSummary> = _landingSummary.asStateFlow()`):

```kotlin
    private val _landingBalanceTimeframe = MutableStateFlow(ProgressTimeframe.MONTH)
    val landingBalanceTimeframe: StateFlow<ProgressTimeframe> = _landingBalanceTimeframe.asStateFlow()

    private val _landingMuscleBalance = MutableStateFlow<Map<String, Float>>(emptyMap())
    val landingMuscleBalance: StateFlow<Map<String, Float>> = _landingMuscleBalance.asStateFlow()

    private val _routineLastCompleted = MutableStateFlow<Map<Long, LocalDate>>(emptyMap())
    val routineLastCompleted: StateFlow<Map<Long, LocalDate>> = _routineLastCompleted.asStateFlow()
```

- [ ] **Step 3: Recompute the balance score alongside the existing exercises/setLogs/sessions combine**

The existing block (inside `init`, currently):

```kotlin
        viewModelScope.launch {
            repository.seedMissingExercises(SEED_EXERCISES)
            combine(repository.exercises, repository.setLogs, repository.sessions) { exercises, setLogs, sessions ->
                Triple(exercises, setLogs, sessions)
            }.collect { (list, setLogs, sessions) ->
                allExercises = list
                exerciseUsageCounts = exerciseUsageFrequency(setLogs, list.associateBy { it.id })
                _lastWorkingSets.value = lastWorkingSetByExercise(setLogs, sessions, list.associateBy { it.id })
                // Session state first: refreshSuggestions reads the active session id to decide
                // whether an exercise's suggestion should lock to this session's first working
                // set. Computing it while _sessionState is still Loading made every suggestion
                // fall back to the between-session decision table on a cold start mid-session.
                refreshSessionState()
                refreshSuggestions(list)
                refreshDisplayedExercises()
            }
        }
```

needs `_landingBalanceTimeframe` as a fourth combined input so changing the landing timeframe recomputes the score. Replace the whole block with:

```kotlin
        viewModelScope.launch {
            repository.seedMissingExercises(SEED_EXERCISES)
            combine(
                repository.exercises,
                repository.setLogs,
                repository.sessions,
                _landingBalanceTimeframe,
            ) { exercises, setLogs, sessions, timeframe ->
                LandingInputs(exercises, setLogs, sessions, timeframe)
            }.collect { (list, setLogs, sessions, timeframe) ->
                allExercises = list
                exerciseUsageCounts = exerciseUsageFrequency(setLogs, list.associateBy { it.id })
                _lastWorkingSets.value = lastWorkingSetByExercise(setLogs, sessions, list.associateBy { it.id })
                // Session state first: refreshSuggestions reads the active session id to decide
                // whether an exercise's suggestion should lock to this session's first working
                // set. Computing it while _sessionState is still Loading made every suggestion
                // fall back to the between-session decision table on a cold start mid-session.
                refreshSessionState()
                refreshSuggestions(list)
                refreshDisplayedExercises()
                _landingMuscleBalance.value = muscleBalanceForTimeframe(
                    timeframe = timeframe,
                    sessions = sessions,
                    sets = setLogs,
                    exercisesById = list.associateBy { it.id },
                    today = LocalDate.now(),
                )
            }
        }
```

Add this private data class near the top of the file, directly below the `LastSessionSummary`/`LandingSummary` data classes (after line 76's closing brace of `LandingSummary`), matching the same named-tuple pattern `ProgressViewModel.kt`'s `Inputs` already uses for its own four-way combine:

```kotlin
/** Named holder for the four-way [combine] feeding suggestions/landing balance -- destructured at
 *  the collector, so the positional tuple never escapes this file. */
private data class LandingInputs(
    val exercises: List<Exercise>,
    val setLogs: List<SetLog>,
    val sessions: List<com.lsing.timego.data.WorkoutSession>,
    val timeframe: ProgressTimeframe,
)
```

- [ ] **Step 4: Add the routine last-completed combine**

Add a new `viewModelScope.launch` block inside `init`, directly after the existing `repository.routines.collect { ... }` block (after its closing `}` around line 167):

```kotlin
        viewModelScope.launch {
            combine(repository.routines, repository.sessions) { _, sessions -> sessions }
                .collect { sessions -> _routineLastCompleted.value = routineLastCompletedDates(sessions) }
        }
```

- [ ] **Step 5: Add the timeframe-selection function**

Add near `selectRoutine` (after its closing brace, around line 182):

```kotlin
    fun selectLandingBalanceTimeframe(timeframe: ProgressTimeframe) {
        _landingBalanceTimeframe.value = timeframe
    }
```

- [ ] **Step 6: Build to confirm it compiles**

Run: `./gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogViewModel.kt
git commit -m "Add landing balance timeframe, muscle balance, and routine last-completed state to LogViewModel"
```

---

### Task 5: Render the Muscle Balance card and routine nudge on the landing page

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt`

**Interfaces:**
- Consumes: `viewModel.landingBalanceTimeframe`, `viewModel.landingMuscleBalance`, `viewModel.routineLastCompleted` (Task 4), `viewModel.selectLandingBalanceTimeframe(...)` (Task 4), `timeframeLabel(...)` (Task 3), `formatDaysSince(...)` (Task 2), `orderedMuscleDistributionForChart(...)` (existing, `domain/MuscleDistribution.kt`), `RadarChart` (existing, `ui/common/`)

No new test — Compose UI change with no existing test coverage for this screen. Verified by Task 6.

- [ ] **Step 1: Add imports**

Add to `LogScreen.kt`'s existing import block:

```kotlin
import androidx.compose.material3.FilterChip
import com.lsing.timego.domain.ProgressTimeframe
import com.lsing.timego.domain.formatDaysSince
import com.lsing.timego.domain.orderedMuscleDistributionForChart
import com.lsing.timego.ui.common.RadarChart
import com.lsing.timego.ui.common.formatEnumLabel
import com.lsing.timego.ui.common.timeframeLabel
import java.time.LocalDate
```

(`FilterChip`, `orderedMuscleDistributionForChart`, `RadarChart`, and `formatEnumLabel` are all new to this file — `LogScreen.kt` currently imports `formatMuscleGroupList` from the same `ui.common` package but not `formatEnumLabel`. `java.time.LocalDate` is also new — `LogScreen.kt` has no `java.time` import today.)

- [ ] **Step 2: Thread the new state through `LogScreen` into `LogLandingContent`**

In `LogScreen` (the top-level composable), add alongside the existing `val routines by viewModel.routines.collectAsState()` (line 82):

```kotlin
    val landingBalanceTimeframe by viewModel.landingBalanceTimeframe.collectAsState()
    val landingMuscleBalance by viewModel.landingMuscleBalance.collectAsState()
    val routineLastCompleted by viewModel.routineLastCompleted.collectAsState()
```

Both `LogLandingContent` call sites (`SessionUiState.NoActiveSession` branch and the `peekingLanding` branch inside `SessionUiState.Active`) need the three new parameters. Update both calls — the `NoActiveSession` branch currently reads:

```kotlin
        is SessionUiState.NoActiveSession -> LogLandingContent(
            summary = landingSummary,
            routines = routines,
            isSessionActive = false,
            onStartOrContinue = viewModel::startSession,
        )
```

becomes:

```kotlin
        is SessionUiState.NoActiveSession -> LogLandingContent(
            summary = landingSummary,
            routines = routines,
            isSessionActive = false,
            onStartOrContinue = viewModel::startSession,
            balanceTimeframe = landingBalanceTimeframe,
            muscleBalance = landingMuscleBalance,
            routineLastCompleted = routineLastCompleted,
            onSelectBalanceTimeframe = viewModel::selectLandingBalanceTimeframe,
        )
```

and the `peekingLanding` branch's `LogLandingContent` call similarly gains the same four new arguments (`balanceTimeframe = landingBalanceTimeframe`, `muscleBalance = landingMuscleBalance`, `routineLastCompleted = routineLastCompleted`, `onSelectBalanceTimeframe = viewModel::selectLandingBalanceTimeframe`).

- [ ] **Step 3: Extend `LogLandingContent`'s signature**

Replace:

```kotlin
@Composable
private fun LogLandingContent(
    summary: LandingSummary,
    routines: List<com.lsing.timego.data.Routine>,
    isSessionActive: Boolean,
    onStartOrContinue: (routineId: Long?) -> Unit,
) {
```

with:

```kotlin
@Composable
private fun LogLandingContent(
    summary: LandingSummary,
    routines: List<com.lsing.timego.data.Routine>,
    isSessionActive: Boolean,
    onStartOrContinue: (routineId: Long?) -> Unit,
    balanceTimeframe: ProgressTimeframe,
    muscleBalance: Map<String, Float>,
    routineLastCompleted: Map<Long, LocalDate>,
    onSelectBalanceTimeframe: (ProgressTimeframe) -> Unit,
) {
```

- [ ] **Step 4: Render the new card**

Inside `LogLandingContent`, insert the new card directly after the existing Recommended `Surface` block's closing `}` (after line 241, before the `if (isSessionActive) { ... } else { ... }` block that starts the session-start buttons at line 243):

```kotlin
        SectionHeader("Muscle Balance (${timeframeLabel(balanceTimeframe)})")
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall)) {
            ProgressTimeframe.entries.forEach { option ->
                FilterChip(
                    selected = balanceTimeframe == option,
                    onClick = { onSelectBalanceTimeframe(option) },
                    label = { Text(formatEnumLabel(option.name)) },
                    modifier = Modifier.padding(end = Spacing.ExtraSmall),
                )
            }
        }
        if (muscleBalance.isNotEmpty()) {
            RadarChart(
                values = orderedMuscleDistributionForChart(muscleBalance)
                    .mapKeys { (group, _) -> formatEnumLabel(group) },
                modifier = Modifier.fillMaxWidth().height(220.dp).padding(vertical = Spacing.Small),
            )
        }
        if (routines.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = Spacing.Small)) {
                routines
                    .sortedWith(compareBy(nullsFirst()) { routine -> routineLastCompleted[routine.id] })
                    .forEach { routine ->
                        Text(
                            "${routine.name} — ${formatDaysSince(routineLastCompleted[routine.id], LocalDate.now())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
            }
        }
```

- [ ] **Step 5: Build to confirm it compiles**

Run: `./gradlew.bat compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt
git commit -m "Render the Muscle Balance card and routine nudge on the Log landing page"
```

---

### Task 6: Full verification — build, install, on-device check

**Files:** None (verification only).

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew.bat testDebugUnitTest`
Expected: PASS, 0 failures.

- [ ] **Step 2: Assemble the debug APK**

Run: `./gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Install on the connected device**

```bash
"/c/Users/lsing/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r "app/build/outputs/apk/debug/app-debug.apk"
```
Expected: `Success`.

- [ ] **Step 4: Cold-start crash check**

```bash
ADB="/c/Users/lsing/AppData/Local/Android/Sdk/platform-tools/adb.exe"
PKG="com.lsing.timego"
"$ADB" logcat -c
"$ADB" shell am force-stop "$PKG"
"$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1
sleep 3
"$ADB" logcat -d *:E | grep -i "$PKG"
```
Expected: no output (no errors logged for the package).

- [ ] **Step 5: On-device verification (user)**

Open the Log tab (no active session). Confirm:
- A new "Muscle Balance" card appears after Recommended, before the session-start buttons.
- Its own Week/Month/Year/Lifetime chips work independently of the Progress screen's own timeframe (switching one doesn't affect the other).
- It defaults to "Month" on a fresh screen load.
- Below the radar, each routine shows "<name> — Xd ago" / "Today" / "Never logged", sorted with the least-recently-done (or never-done) routine first.
- The Progress screen's own Muscle Balance card still works exactly as it did before this plan.

- [ ] **Step 6: Report back and await confirmation**

Report the on-device result. Do not merge/finish the branch until the user confirms this looks and behaves correctly.
