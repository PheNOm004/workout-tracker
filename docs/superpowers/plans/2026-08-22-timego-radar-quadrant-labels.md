# TimeGo Radar Chart — quadrant-aligned labels

**Goal:** Prevent muscle-group labels from colliding at the chart's lower axes while preserving the current radar geometry and values.

## Slice

1. Align right-side labels to their left edge.
2. Align left-side labels to their right edge.
3. Keep labels on the vertical axis centered.

## Verification

1. Run the JVM test suite.
2. Assemble and install the debug app.
3. Inspect the Muscle Balance chart on-device, especially the lower labels.

## Out of scope

- No data, scaling, chart-size, label-content, or label-order changes.
- The previous-period ghost polygon and the Bars view remain independent future slices.
