# TimeGo Logging — local exercise favorites

**Goal:** Let the user pin personally useful exercises without altering workout data or the Room schema.

## Slice

1. Store favorite exercise IDs as a local Preferences DataStore string set.
2. Add a star toggle to every log exercise row.
3. Surface visible favorites as shortcuts above Quick Add and the full library.

## Verification

1. Run the JVM test suite.
2. Assemble and install the debug app.
3. On-device: favorite an exercise, leave/reopen Log, and confirm it remains in Favorites and opens its normal log row.

## Out of scope

- No Room migration, cloud sync, or workout-data change.
- No automatic logging or routine membership changes.
