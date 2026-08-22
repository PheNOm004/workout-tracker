# TimeGo Logging — End Session confirmation

**Goal:** Prevent an accidental end to an active workout without changing saved-session behaviour.

## Slice

1. Replace the primary-looking header End Session button with a low-emphasis text action.
2. Require a confirmation dialog before calling the existing `endActiveSession` operation.
3. Make cancellation explicit as `Keep logging`.

## Verification

1. Run the JVM test suite.
2. Assemble and install the debug app.
3. On-device: tap End session, dismiss with Keep logging, then confirm End session in a safe test workout.

## Out of scope

- No repository, session, or set persistence changes.
- No change to the back-to-landing action.
