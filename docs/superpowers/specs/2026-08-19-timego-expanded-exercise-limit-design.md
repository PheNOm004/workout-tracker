# TimeGo Expanded Exercise Limit Design

**Date:** 2026-08-19

## Goal

Keep an active session's exercise list manageable by allowing no more than three expanded exercise rows at a time.

## User Experience

- All exercise rows begin collapsed when a session starts.
- A user can expand any three rows, regardless of logging type (strength/calisthenics, cardio, or timed hold).
- Opening a fourth row automatically collapses the row that has been open the longest. The newest three rows stay open.
- Tapping an open row collapses that row only.
- The open-row set resets for a new active session and remains stable across ordinary recompositions of the same session.
- Logging inputs, timers, suggestions, and the existing last-set display keep their current behavior.

## Architecture

`LoggingContent` owns a session-keyed ordered list of expanded exercise IDs. Each exercise row becomes a controlled component: it receives its expanded status plus a toggle callback instead of holding an independent `expanded` state. A small pure domain function performs the ordering and eviction rule, giving it focused JVM unit tests without adding Compose UI-test machinery.

## Constraints

- Maximum simultaneous expanded rows: exactly 3.
- Apply to every supported logging type.
- Do not change exercise ordering, session persistence, logging data, recommendations, or the visual design beyond collapsing rows.
- Do not prefill inputs or stop an already-running timer when another row is opened.

## Verification

- Unit tests cover adding rows, opening a fourth row, collapsing an open row, and reopening an older collapsed row.
- Full debug unit-test suite passes and the debug APK installs on the connected device.
- Manual check: open three exercise rows, then a fourth; confirm the oldest closes and the remaining three stay visible.
