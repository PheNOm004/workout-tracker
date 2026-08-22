# TimeGo Routines — single empty-state action

**Goal:** Remove the two competing create-routine actions from an empty Routines screen.

## Slice

1. Hide the compact `+ New routine` action when no routines exist.
2. Keep `Create your first routine` as the single empty-state call to action.
3. Restore the compact action automatically once at least one routine exists.

## Verification

1. Run the JVM test suite.
2. Assemble and install the debug app.
3. On-device: confirm exactly one creation action when empty, and the header action when a routine exists.

## Out of scope

- No routine creation, deletion, or scheduling changes.
- The stale-muscle list cap is a separate next slice.
