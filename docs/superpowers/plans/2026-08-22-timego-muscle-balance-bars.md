# TimeGo Muscle Balance — Bars view

**Goal:** Make Progress's Muscle Balance assessment scannable as a ranked list while preserving the radar as a glanceable alternate view.

## Slice

1. Default Progress to a `Bars | Radar` segmented control; Log keeps its radar unchanged.
2. Rank all concrete muscle groups by current target attainment.
3. Show a coral attainment bar, JetBrains Mono percentage, and prior equal-period delta for measured groups.
4. Mark untrained groups `NO DATA` with a neutral empty track, rather than a fabricated zero bar.

## Verification

1. Unit-test ranking, prior value retention, and neutral absent groups.
2. Run the JVM test suite.
3. Assemble and install the debug app.
4. Inspect Bars and Radar on the Progress screen and confirm Log remains radar-only.

## Out of scope

- No persistence of the temporary display selection.
- No change to balance calculations, targets, or previous-period range rules.
