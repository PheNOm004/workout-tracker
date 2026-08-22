# TimeGo Radar Chart — previous-period ghost outline

**Goal:** Add a subdued violet comparison outline for the immediately preceding equal-length period, without inventing missing muscle data.

## Slice

1. Calculate an explicit closed prior date range for Week, Month, and Year; Lifetime has no equal prior window.
2. Score the prior period using the existing fixed weekly effective-set target.
3. Pass the prior balance to Progress only and draw a violet outline only when every currently displayed spoke has a real prior value.

## Verification

1. Unit-test that a prior Week excludes current-week sets and that Lifetime has no comparison.
2. Run the JVM test suite.
3. Assemble and install the debug app.
4. Inspect Progress → Muscle Balance with a timeframe that has a complete comparable prior period.

## Out of scope

- No change to the current balance calculation, its target, or Log's glance chart.
- No fabricated zero values for muscles absent in the earlier period.
- The Bars view remains a separate future slice.
