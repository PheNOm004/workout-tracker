# TimeGo Muscle-Group Accordion Limit Design

**Date:** 2026-08-19

## Goal

Keep the exercise browser short enough to scan by showing the exercise list for only one muscle group at a time.

## User Experience

- Category sections retain their existing independent expansion behavior.
- A muscle-group header reveals its exercise list when tapped.
- At most one muscle-group exercise list may be expanded across one exercise browser.
- Opening another muscle group closes the current group, including when it belongs to another category.
- Tapping an already open muscle group closes only that group.
- The search result mode remains ungrouped and unchanged.
- Individual exercise cards retain their existing independent three-open-row session rule.
- Because the shared `ExerciseSections` component owns the behavior, it applies to both the Log screen and the Routines exercise selector.

## Architecture

`ExerciseSections` owns an ordered list of expanded category-plus-muscle-group keys. A small pure helper in `ui/common` toggles those keys and keeps only the newest one. Each nested group header reads controlled expanded state from the parent list, replacing its individual local expanded state.

## Constraints

- Maximum simultaneous open muscle-group exercise lists: exactly 1.
- Do not cap category headers.
- Do not modify search behavior, the existing individual-exercise cap, timers, logging data, or routine selection.

## Verification

- JVM tests cover adding, replacing the current group, explicit collapse, and reopening a former group.
- Full debug unit suite passes and the APK installs on the connected device.
- Manual check: expand Chest, then Back; Chest closes. Open Shoulders; Back closes. Individual exercise-card expansion continues to behave as before.
