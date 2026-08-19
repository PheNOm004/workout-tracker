# TimeGo Muscle-Group Accordion Limit Design

**Date:** 2026-08-19

## Goal

Keep the exercise browser short enough to scan by showing the exercise lists for at most two muscle groups at once.

## User Experience

- Category sections retain their existing independent expansion behavior.
- A muscle-group header reveals its exercise list when tapped.
- At most two muscle-group exercise lists may be expanded across one exercise browser.
- Opening a third muscle group closes the group that has been open longest, including when it belongs to another category.
- Tapping an already open muscle group closes only that group.
- The search result mode remains ungrouped and unchanged.
- Individual exercise cards retain their existing independent three-open-row session rule.
- Because the shared `ExerciseSections` component owns the behavior, it applies to both the Log screen and the Routines exercise selector.

## Architecture

`ExerciseSections` owns an ordered list of expanded category-plus-muscle-group keys. A small pure helper in `ui/common` toggles those keys and keeps only the two newest. Each nested group header reads controlled expanded state from the parent list, replacing its individual local expanded state.

## Constraints

- Maximum simultaneous open muscle-group exercise lists: exactly 2.
- Do not cap category headers.
- Do not modify search behavior, the existing individual-exercise cap, timers, logging data, or routine selection.

## Verification

- JVM tests cover adding, evicting the oldest third group, explicit collapse, and reopening a former group.
- Full debug unit suite passes and the APK installs on the connected device.
- Manual check: expand Chest and Back, then Shoulders; Chest closes. Open Legs; Back closes. Individual exercise-card expansion continues to behave as before.
