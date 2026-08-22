# TimeGo Logging — frequency-based Quick Add

**Goal:** Surface a compact set of personally familiar exercises before the full library.

## Slice

1. Derive up to six used exercises from the existing all-time usage-frequency counts.
2. Recompute the subset when the visible exercise scope changes (freeform or selected routine).
3. Tap a Quick Add chip to place that exact exercise in the existing search and expanded logging state.

## Verification

1. Unit-test frequency ordering, the used-only rule, and the limit.
2. Run the JVM test suite.
3. Assemble and install the debug app.
4. On-device: tap a Quick Add chip and confirm its expanded input row appears.

## Out of scope

- No stored favorites or schema changes.
- No automatic set logging; the user still reviews and submits the row.
