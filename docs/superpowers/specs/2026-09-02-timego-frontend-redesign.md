# Spec: TimeGo Frontend & Design Overhaul

**Date:** 2026-09-02  
**Status:** Approved by User  
**Target:** TimeGo Android App (`com.lsing.timego`)

---

## 1. Problem Statement & Objectives

TimeGo v1 is feature-complete, robust, and local-first, but its frontend has accumulated layout friction:
1. **Active Workout Clutter:** Browsing 600+ exercises during an in-gym session relies on a 4-level nested accordion (`Category` -> `Muscle Group` -> `Exercise` -> `Form`), requiring excessive tapping and causing visual disorientation.
2. **Invisible In-Session Sets:** Logged sets clear the input fields without a clear in-line table of what sets have been performed so far today.
3. **Log Landing Redundancy:** The idle landing screen duplicates the Muscle Balance Radar and routine lists from other tabs, reducing actionability.
4. **Progress Screen Scroll Fatigue:** 7 distinct analytics widgets (Heatmap, Timeframes, Anatomy Diagram, Stat Tiles, PR Wheel, Strength Curve Wheel, Body Metrics) are stacked in a single vertical scroll.
5. **Routines Maintenance Clutter:** Settings, delay steppers, database backups, and session deletion are embedded inside the routines list.
6. **Interaction & Motion Deficit:** Set logging lacks tactile confirmation, and list switches snap abruptly.

---

## 2. Design System Architecture

### 2.1 Navigation & Screen Architecture
- **Retain 3 Bottom Tabs:** `Log`, `Progress`, `Routines`.
- **Settings Sheet:** A quiet, non-intrusive gear icon inside the `Routines` header opens a dedicated `SettingsBottomSheet` (Hold delay, training style preference, database export/restore, session history deletion).

### 2.2 Log Tab: Idle "Command Center"
- **Hero Card ("Next Workout"):** Scheduled routine for today (or recommended muscle group focus) with a single-tap "Start Workout" / "Start Freeform" button.
- **Secondary Card ("Last Session Recap"):** Compact card with duration, sets, volume, and the cropped anatomical heatmap. Tap opens full session detail.
- **Routines Quick Carousel:** Horizontal row of chips to start any routine directly.
- **Clean-up:** Redundant radar chart removed from landing.

### 2.3 Log Tab: Active In-Gym Session
- **Pinned Active Workout Section (Top):**
  - Displays currently active exercises in the session.
  - Shows completed sets in a clean, legible table (*Set 1: 80kg x 8 ✓, Set 2: 82.5kg x 6 ✓*).
- **Sticky Muscle-Filter Pill Bar (Middle):**
  - Horizontal filter chips: `All`, `Chest`, `Back`, `Shoulders`, `Arms`, `Legs`, `Core`, `Cardio`, `Favorites`.
  - 1-tap filtering of the 600+ exercise library without nested category/muscle accordions.
- **Tactile Set Inputs:**
  - In-line row with +/- 2.5kg and +/- 1 rep stepper helpers or clean direct entry.
  - Previous performance reference (`Last time: 80kg x 8`).
  - Warm-up checkbox and optional RPE.
  - Rest timer HUD / indicator upon logging a set.

### 2.4 Progress Tab: 2-Segment Overhaul
- **Top Segment Toggle:** `[ Training ]` and `[ Body ]`.
- **`Training` Segment:**
  - Consistency Heatmap (all-time).
  - Timeframe selector (`Week`, `Month`, `Year`, `Lifetime`).
  - Muscle Anatomy Diagram (Front/Back, hold-to-peek best set readout) + 4 Stat Tiles.
  - **Unified Exercise Performance Card:** Exercise selector with unified PR readouts (Best Set, Reps, Max Volume / Longest Hold) + Strength Progression Sparkline Curve together in one card.
- **`Body` Segment:**
  - Bodyweight trend sparkline & BMI health card.
  - Log inputs for weight, waist, height + chronological history log.

### 2.5 Routines Tab: Pure Workout Builder
- Clean, focused cards showing scheduled days, target muscle tags, and numbered exercise order.
- Action to create and reorder routines.

### 2.6 Motion & Micro-Interactions
- Smooth list item transitions using Compose `Modifier.animateItem()`.
- Tactile set completion flash and settle animation.
- Rest timer countdown bar.

---

## 3. Non-Goals & Invariants
- No schema changes required (Room schema 14 preserved).
- All data remains strictly local and offline.
- Existing tests (257 JVM tests) must remain green.
