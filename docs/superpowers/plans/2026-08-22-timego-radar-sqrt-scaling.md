# TimeGo Radar Chart — square-root display scaling

**Goal:** Make low but meaningful muscle-volume values visible on the balance radar without changing its measurements.

## Slice

1. Apply a square-root transform only to chart radii.
2. Draw reference rings using the same transform, so their displayed positions remain truthful to their labelled values.
3. Keep axis endpoints and labels unchanged.

## Verification

1. Run the JVM test suite.
2. Assemble and install the debug app.
3. Inspect the Muscle Balance radar on-device; values should retain their ordering while smaller values become easier to see.

## Out of scope

- Quadrant labels, comparison/ghost polygons, and a Bars toggle remain separate future commits.
- No muscle-balance calculation or target changes.
