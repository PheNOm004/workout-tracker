# TimeGo Logging — custom exercise chip grid

**Goal:** Make custom-exercise category and muscle selection visible and easy to scan in the dialog.

## Slice

1. Replace the horizontally clipped category row with a wrapping chip grid.
2. Replace the single-column muscle checkbox wall with wrapping selectable chips.
3. Preserve category defaults, group selection state, validation, and add callback values.

## Verification

1. Run the JVM test suite.
2. Assemble and install the debug app.
3. On-device: select the fourth category and multiple muscles, add a custom exercise, and confirm its selections persist.

## Out of scope

- No exercise model, schema, or custom-exercise validation changes.
- No changes to the library browser.
