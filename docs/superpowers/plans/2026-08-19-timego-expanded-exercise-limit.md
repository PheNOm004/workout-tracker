# TimeGo Expanded Exercise Limit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep no more than three exercise logging rows open in a TimeGo active session.

**Architecture:** Store the open exercise IDs, ordered oldest to newest, in session-keyed Compose state within `LoggingContent`. A pure domain helper adds, removes, and evicts IDs; all three logging-row variants receive controlled expansion state from that parent.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Gradle Android plugin.

**Spec:** `docs/superpowers/specs/2026-08-19-timego-expanded-exercise-limit-design.md`

## Global Constraints

- Maximum simultaneous expanded rows: exactly 3.
- Apply to strength/calisthenics, cardio, and timed-hold rows.
- Do not alter stored workout data, current timers, input values, recommendations, or visual styling.
- Reset expanded-row state when the active session ID changes.

---

### Task 1: Ordered expanded-row state helper

**Files:**
- Create: `app/src/main/java/com/lsing/timego/domain/ExpandedExerciseRows.kt`
- Test: `app/src/test/java/com/lsing/timego/domain/ExpandedExerciseRowsTest.kt`

**Interfaces:**
- Produces: `const val MAX_EXPANDED_EXERCISE_ROWS: Int`
- Produces: `fun toggleExpandedExerciseIds(expandedIds: List<Long>, exerciseId: Long, maxExpanded: Int = MAX_EXPANDED_EXERCISE_ROWS): List<Long>`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `keeps the newest three rows when a fourth row opens`() {
    val expanded = toggleExpandedExerciseIds(listOf(10L, 20L, 30L), 40L)

    assertEquals(listOf(20L, 30L, 40L), expanded)
}
```

Also add tests asserting `emptyList()` toggled with `10L` yields `listOf(10L)`, toggling `20L` out of `listOf(10L, 20L, 30L)` yields `listOf(10L, 30L)`, and reopening `10L` from `listOf(20L, 30L, 40L)` yields `listOf(30L, 40L, 10L)`.

- [ ] **Step 2: Run test to verify it fails**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:ANDROID_HOME='C:\Users\lsing\AppData\Local\Android\Sdk'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest --tests com.lsing.timego.domain.ExpandedExerciseRowsTest`

Expected: compilation failure because `toggleExpandedExerciseIds` does not exist.

- [ ] **Step 3: Write minimal implementation**

```kotlin
const val MAX_EXPANDED_EXERCISE_ROWS = 3

fun toggleExpandedExerciseIds(
    expandedIds: List<Long>,
    exerciseId: Long,
    maxExpanded: Int = MAX_EXPANDED_EXERCISE_ROWS,
): List<Long> =
    if (exerciseId in expandedIds) {
        expandedIds.filterNot { it == exerciseId }
    } else {
        (expandedIds + exerciseId).takeLast(maxExpanded)
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:ANDROID_HOME='C:\Users\lsing\AppData\Local\Android\Sdk'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest --tests com.lsing.timego.domain.ExpandedExerciseRowsTest`

Expected: all `ExpandedExerciseRowsTest` cases pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/lsing/timego/domain/ExpandedExerciseRows.kt app/src/test/java/com/lsing/timego/domain/ExpandedExerciseRowsTest.kt
git commit -m "Limit expanded exercise rows"
```

### Task 2: Controlled session exercise rows

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt`

**Interfaces:**
- Consumes: `toggleExpandedExerciseIds(List<Long>, Long): List<Long>` from Task 1.
- Produces: session-keyed parent-owned expansion state shared by `StrengthLogRow`, `CardioLogRow`, and `HoldLogRow`.

- [ ] **Step 1: Add session-keyed parent state**

Change `LoggingContent` to accept `sessionId: Long` and create:

```kotlin
var expandedExerciseIds by remember(sessionId) { mutableStateOf<List<Long>>(emptyList()) }
```

Pass `state.sessionId` from the active-session branch of `LogScreen`.

- [ ] **Step 2: Route each row through the shared toggle**

For every `ExerciseSections` row, pass:

```kotlin
expanded = exercise.id in expandedExerciseIds,
onToggle = {
    expandedExerciseIds = toggleExpandedExerciseIds(expandedExerciseIds, exercise.id)
},
```

to strength/calisthenics, cardio, and hold rows.

- [ ] **Step 3: Make each row controlled**

Add `expanded: Boolean` and `onToggle: () -> Unit` parameters to `StrengthLogRow`, `CardioLogRow`, and `HoldLogRow`; remove each row's local `expanded` state; route its header click to the passed `onToggle`. Keep the existing input and timer state keyed by exercise name.

- [ ] **Step 4: Run full automated verification and install**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:ANDROID_HOME='C:\Users\lsing\AppData\Local\Android\Sdk'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest installDebug -q`

Expected: unit tests pass and the debug APK installs on the connected device.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/lsing/timego/ui/log/LogScreen.kt
git commit -m "Cap open exercise rows per session"
```

### Task 3: Preserve project record

**Files:**
- Create: `docs/superpowers/specs/2026-08-19-timego-expanded-exercise-limit-design.md`
- Create: `docs/superpowers/plans/2026-08-19-timego-expanded-exercise-limit.md`
- Modify: `C:/Users/lsing/.claude/obsidian_demo/Projects/TimeGo - Gym Progress Tracker.md`
- Modify: `C:/Users/lsing/.claude/obsidian_demo/Projects/TimeGo/03 Feature Catalog.md`
- Modify: `C:/Users/lsing/.claude/obsidian_demo/Projects/TimeGo/08 Session Log.md`

**Interfaces:**
- Produces: implementation spec, executable plan, and durable vault record of the user-visible cap.

- [ ] **Step 1: Confirm the design and plan describe the shipped behavior**

Verify that the documents state the three-row maximum, oldest-row eviction, all logging types, session reset, and manual verification steps.

- [ ] **Step 2: Update the TimeGo vault notes**

Add the cap to the feature catalogue and append a dated session-log entry naming the domain helper, controlled screen state, commits, and full test/install verification. Update the TimeGo project next action to ask the user to verify the newest installed build, alongside the still-unmerged recommendation and last-set work.

- [ ] **Step 3: Commit app documentation**

```powershell
git add docs/superpowers/specs/2026-08-19-timego-expanded-exercise-limit-design.md docs/superpowers/plans/2026-08-19-timego-expanded-exercise-limit.md
git commit -m "Document expanded exercise row limit"
```
