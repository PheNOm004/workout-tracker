# TimeGo Logging — bounded library search

**Goal:** Keep exercise search responsive after the catalog expansion without changing match behaviour.

## Slice

1. Preserve normalized matching (spaces and hyphens remain equivalent).
2. Preserve the supplied frequency order.
3. Limit a broad search to 40 composed results and explain when the user should refine it.

## Verification

1. Unit-test the result bound, supplied order, and punctuation-insensitive match.
2. Run the JVM test suite.
3. Assemble and install the debug app.
4. On-device: search a broad term and refine it to reach a specific exercise.

## Out of scope

- No full-text index, schema, or library data changes.
- No change to category browsing when search is empty.
