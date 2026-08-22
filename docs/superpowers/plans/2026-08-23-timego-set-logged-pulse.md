# TimeGo Logging — saved-set pulse

**Goal:** Give a brief visual acknowledgement only after a set has been successfully saved.

## Slice

1. Emit a monotonic UI-only event after strength, hold, or cardio persistence completes.
2. Route the event to the matching exercise row.
3. Expand its existing coral edge briefly, then settle at the ordinary expanded-row width.

## Verification

1. Run the JVM test suite.
2. Assemble and install the debug app.
3. On-device: log one strength, one hold, and one cardio set; each matching row should pulse after saving.

## Out of scope

- No persisted event, schema change, or recommendation behaviour change.
- No animation for a failed or invalid form submission.
