# TimeGo Progress Readability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Progress data easier to read through semantic BMI status, a visible inactive heatmap state, and chart fills/tags—without changing any metric or radar semantics.

**Architecture:** Keep all calculations and screen layout intact. Presentation-only changes live at the component boundaries: `ProgressScreen` maps an already computed BMI category to an existing semantic palette, `HeatmapGrid` raises the neutral-cell tone, and `SparklineChart` fills the area below the existing line and labels its extrema/current point.

**Tech Stack:** Kotlin, Jetpack Compose Canvas, Material 3, Gradle Android application plugin.

**Spec:** `C:\Users\lsing\.claude\obsidian_demo\Projects\TimeGo\14 Backlit UI Audit and Implementation Record.md`

## Global Constraints

- Do not change Progress values, window definitions, BMI classification, Room schema, or adaptive-coach code.
- Preserve the existing coral line series and dashed average reference; add only a restrained under-fill and value labels.
- A heatmap absent date remains semantically neutral, not a fabricated zero or missed workout.
- Keep all radar changes out of this slice.
- Do not stage or amend the separately staged secret-remediation files.
- Run the full JVM unit suite and `installDebug`; commit only after user verification.

---

### Task 1: Apply semantic BMI and empty-heatmap presentation

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/progress/ProgressScreen.kt`
- Modify: `app/src/main/java/com/lsing/timego/ui/common/HeatmapGrid.kt`

- [x] **Step 1: Map BMI categories to existing semantic colours**

Use `NightMint` for `NORMAL`, `NightAmber` for `OVERWEIGHT`, and Material error for `UNDERWEIGHT`/`OBESE`. Keep the existing BMI text and category logic unchanged.

- [x] **Step 2: Raise the neutral heatmap cell above the ground plane**

Render missing/future/zero-ratio cells with `surfaceContainerHigh` at a high enough opacity to remain visible on the Backlit ground, while retaining the same shape and click behaviour.

### Task 2: Give sparklines visual body and direct context

**Files:**
- Modify: `app/src/main/java/com/lsing/timego/ui/common/SparklineChart.kt`

- [x] **Step 1: Fill the existing line path down to the plot baseline**

Use a low-alpha vertical gradient derived from the existing primary line colour. Draw it before the dashed average and line so it never obscures data.

- [x] **Step 2: Add compact minimum, maximum, and end-value labels**

Keep the existing date and average labels. Add only short numeric tags positioned within the padded plot bounds; clamp label positions to avoid clipping on extrema.

### Task 3: Verify and install

- [x] **Step 1: Compile and run the JVM suite**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"; .\gradlew.bat testDebugUnitTest`

- [x] **Step 2: Install the debug build**

Run: `cd "C:\Users\lsing\AndroidStudioProjects\TimeGo"; .\gradlew.bat installDebug`

- [x] **Step 3: Request user verification**

Check Progress for: mint normal BMI; visible neutral heatmap dots; a restrained coral chart fill; readable min/max/current tags with no collisions.

- [x] **Step 4: Commit after acceptance**

Commit only these three source files and this plan using:

```text
feat(progress): improve data readability
```

## Self-Review

- **Spec coverage:** Delivers Batch B's BMI semantic map, heatmap empty-cell tone, and chart body/context while leaving stat-tile, radar, body-metrics field, and history-list work for later isolated changes.
- **Scope:** All changes are presentation-only and consume existing values.
