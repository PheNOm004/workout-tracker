# TimeGo Batch E1 Picker and Balance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the selector's accidental-looking center tick and prove whether sparse-history Year/Lifetime Muscle Balance behaviour is a math defect or an honest presentation limitation.

**Architecture:** `HorizontalWheelPicker` owns its selected-item marker, so one pill replacement fixes every call site without adding per-item state. Muscle Balance continues to use its fixed 10 effective-sets-per-week target; a synthetic test makes long-window dilution explicit before any separately approved readability treatment.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, JUnit 4, Gradle Android application plugin.

**Spec:** `C:\Users\lsing\.claude\obsidian_demo\Projects\TimeGo\14 Backlit UI Audit and Implementation Record.md`

## Global Constraints

- Preserve the fixed weekly-target semantics of `muscleBalanceForTimeframe`; do not fabricate zeroes, rewrite historic results, or add the unapproved diverging heatmap.
- Change no database schema, adaptive-coach code, permissions, networking, or dependencies.
- Keep the picker item width, snap-to-centre behaviour, alpha emphasis, and existing ellipsis logic unchanged.
- Do not stage or amend the separately staged secret-remediation files.
- Run targeted JVM tests, the full JVM suite, and `installDebug`; commit only after the user validates the picker on-device.

---

### Task 1: Replace the shared picker tick with a visible selection pill

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/common/HorizontalWheelPicker.kt`

**Interfaces:**
- Produces the same `HorizontalWheelPicker` API and centered-selection behaviour.
- Replaces the 2×6dp `drawLine` tick with a static 28×3dp rounded coral pill; the snapped selected item stays centered above it.

- [x] **Step 1: Remove the old line-only marker imports and add `CornerRadius`**

Remove `Offset`; add `androidx.compose.ui.geometry.CornerRadius`.

- [x] **Step 2: Replace the tick Canvas**

Use this marker after the `LazyRow`:

```kotlin
Canvas(
    modifier = Modifier
        .padding(top = 2.dp)
        .width(28.dp)
        .height(3.dp),
) {
    drawRoundRect(
        color = MaterialTheme.colorScheme.primary,
        cornerRadius = CornerRadius(size.height / 2f),
    )
}
```

- [x] **Step 3: Compile the picker**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"; .\gradlew.bat :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`.

### Task 2: Lock down the Muscle Balance long-window diagnosis

**Files:**
- Modify: `app/src/test/java/com/lsing/timego/domain/MuscleDistributionTest.kt`

**Interfaces:**
- Keeps `muscleBalanceForTimeframe` unchanged.
- Adds a fixture proving that ten recent effective sets score 1.0 for Week but correctly dilute over Month, Year, and Lifetime when the observation window grows.

- [x] **Step 1: Add the synthetic sparse-history fixture**

Create one early session with no qualifying sets and one current session with ten BICEPS sets at RPE 8. Evaluate all four `ProgressTimeframe` values using the same `today`.

- [x] **Step 2: Assert the semantic verdict**

Assert Week is `1.0f`, Month is approximately `0.23f`, and Year/Lifetime are approximately `0.019f`. Name the test to state that this is a weekly-rate presentation consequence, not a math failure.

- [x] **Step 3: Run the targeted domain test**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"; .\gradlew.bat testDebugUnitTest --tests "com.lsing.timego.domain.MuscleDistributionTest"`

Expected: `BUILD SUCCESSFUL`.

### Task 3: Verify and install Batch E1

**Files:**
- Verify only: Task 1 and Task 2 files

- [x] **Step 1: Run the complete JVM suite**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"; .\gradlew.bat testDebugUnitTest`

- [x] **Step 2: Install the debug build**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"; .\gradlew.bat installDebug`

- [x] **Step 3: Ask for on-device picker verification**

Check the PR selector and both Strength Curve selectors. Acceptance: each selected label has one clean 28×3dp coral pill beneath it; side labels remain ellipsized rather than colliding.

- [x] **Step 4: Commit after user acceptance**

Stage only the picker, the synthetic test, and this plan. Commit message:

```text
fix(ui): clarify wheel picker selection
```

## Self-Review

- **Spec coverage:** Task 1 resolves C2 at its shared source. Task 2 completes C3's math-versus-rendering verdict without prematurely choosing a display encoding.
- **Scope:** The balance function and UI remain unchanged; a future presentation fix requires a separate approved design.
