# TimeGo Logging — zero-routine start state

**Goal:** Make the Logging landing page useful and self-explanatory when the user has no saved routines.

## Slice

1. Show a compact empty-state card only when the routine list is empty.
2. Keep one primary action: start a freeform session through the existing callback.
3. Explain that reusable plans can be created in the Routines tab.
4. Leave the existing horizontal Freeform + routine picker unchanged when routines exist.

## Verification

1. Run the JVM test suite.
2. Assemble and install the debug app.
3. On-device: verify the empty state with no routines, then verify Freeform and routine buttons after routines exist.

## Out of scope

- No new navigation, routing, or routine persistence.
- No changes to how a session is created.
