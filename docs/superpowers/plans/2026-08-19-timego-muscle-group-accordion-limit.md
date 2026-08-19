# TimeGo Muscle-Group Accordion Limit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Limit the shared exercise browser to two expanded muscle-group exercise lists at a time.

**Architecture:** Add a pure string-key toggle helper under `ui/common`, then replace each muscle group's local Compose state in `ExerciseSections` with parent-owned, ordered state. Keys combine category and muscle group so a group remains unique across the entire browser.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Gradle Android plugin.

**Spec:** `docs/superpowers/specs/2026-08-19-timego-muscle-group-accordion-limit-design.md`

## Global Constraints

- Maximum simultaneous open muscle-group exercise lists: exactly 2.
- Do not cap category headers.
- Do not modify search behavior, individual exercise-card behavior, timers, logged data, or routine selection.

---

### Task 1: Ordered muscle-group accordion helper

**Files:**
- Create: `app/src/main/java/com/lsing/timego/ui/common/ExpandedExerciseGroups.kt`
- Create: `app/src/test/java/com/lsing/timego/ui/common/ExpandedExerciseGroupsTest.kt`

**Interfaces:**
- Produces: `const val MAX_EXPANDED_EXERCISE_GROUPS: Int`
- Produces: `fun toggleExpandedExerciseGroupKeys(expandedKeys: List<String>, groupKey: String, maxExpanded: Int = MAX_EXPANDED_EXERCISE_GROUPS): List<String>`

- [ ] **Step 1: Write failing tests**

```kotlin
@Test
fun `keeps the newest two groups when a third group opens`() {
    val expanded = toggleExpandedExerciseGroupKeys(listOf("STRENGTH:CHEST", "STRENGTH:BACK"), "STRENGTH:SHOULDERS")

    assertEquals(listOf("STRENGTH:BACK", "STRENGTH:SHOULDERS"), expanded)
}
```

Also test a first group, explicitly collapsing an open group, and reopening a previously evicted group.

- [ ] **Step 2: Run the targeted test and observe its expected missing-helper failure**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:ANDROID_HOME='C:\Users\lsing\AppData\Local\Android\Sdk'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest --tests com.lsing.timego.ui.common.ExpandedExerciseGroupsTest`

Expected: compilation fails because `toggleExpandedExerciseGroupKeys` does not exist.

- [ ] **Step 3: Implement the helper**

```kotlin
const val MAX_EXPANDED_EXERCISE_GROUPS = 2

fun toggleExpandedExerciseGroupKeys(
    expandedKeys: List<String>,
    groupKey: String,
    maxExpanded: Int = MAX_EXPANDED_EXERCISE_GROUPS,
): List<String> =
    if (groupKey in expandedKeys) expandedKeys.filterNot { it == groupKey }
    else (expandedKeys + groupKey).takeLast(maxExpanded)
```

- [ ] **Step 4: Run the targeted test and confirm it passes**

Run the Task 1 command again. Expected: all cases pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/lsing/timego/ui/common/ExpandedExerciseGroups.kt app/src/test/java/com/lsing/timego/ui/common/ExpandedExerciseGroupsTest.kt
git commit -m "Limit expanded muscle groups"
```

### Task 2: Controlled group expansion in the shared browser

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/common/ExerciseListSections.kt`

**Interfaces:**
- Consumes: `toggleExpandedExerciseGroupKeys(List<String>, String): List<String>`.
- Produces: a shared two-open-group accordion for every `ExerciseSections` caller.

- [ ] **Step 1: Add parent state inside `ExerciseSections`**

```kotlin
var expandedGroupKeys by remember { mutableStateOf<List<String>>(emptyList()) }
```

- [ ] **Step 2: Give every nested group a unique key and controlled state**

Inside the existing `(category, group)` loop, define:

```kotlin
val groupKey = "${category.name}:$group"
val groupExpanded = groupKey in expandedGroupKeys
```

Replace the local `var groupExpanded` state with a header callback that assigns:

```kotlin
expandedGroupKeys = toggleExpandedExerciseGroupKeys(expandedGroupKeys, groupKey)
```

- [ ] **Step 3: Preserve the unaffected behavior**

Keep category `expanded` state, search-mode list rendering, and `itemContent(exercise)` untouched. Do not change the Log screen's individual exercise-card state.

- [ ] **Step 4: Run full tests and install**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:ANDROID_HOME='C:\Users\lsing\AppData\Local\Android\Sdk'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest installDebug -q`

Expected: all JVM tests pass and the debug APK installs on the connected device.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/lsing/timego/ui/common/ExerciseListSections.kt
git commit -m "Cap open muscle group exercise lists"
```

### Task 3: Document the behavioral refinement

**Files:**
- Create: `docs/superpowers/specs/2026-08-19-timego-muscle-group-accordion-limit-design.md`
- Create: `docs/superpowers/plans/2026-08-19-timego-muscle-group-accordion-limit.md`
- Modify: `C:/Users/lsing/.claude/obsidian_demo/Projects/TimeGo - Gym Progress Tracker.md`
- Modify: `C:/Users/lsing/.claude/obsidian_demo/Projects/TimeGo/03 Feature Catalog.md`
- Modify: `C:/Users/lsing/.claude/obsidian_demo/Projects/TimeGo/08 Session Log.md`

**Interfaces:**
- Produces: a durable record that distinguishes the two-open muscle-group accordion from the existing individual exercise-card rule.

- [ ] **Step 1: Validate documentation coverage**

Confirm the spec and plan state the two-group maximum, oldest-group eviction, shared Log/Routines scope, unchanged search, and unchanged card behavior.

- [ ] **Step 2: Update the vault**

Replace the prior feature-catalog description that treated the three-card cap as the only exercise-list control. State that muscle-group lists have a separate global two-open limit. Append a dated session-log entry with the helper, UI ownership, commits, test/install result, and manual check.

- [ ] **Step 3: Commit app documentation**

```powershell
git add docs/superpowers/specs/2026-08-19-timego-muscle-group-accordion-limit-design.md docs/superpowers/plans/2026-08-19-timego-muscle-group-accordion-limit.md
git commit -m "Document muscle group accordion limit"
```
