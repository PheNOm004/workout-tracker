# TimeGo Routines — focused stale-muscle nudge

**Goal:** Turn the stale-muscle list into a useful next-action cue rather than a wall of chips.

## Slice

1. Restrict candidate groups to concrete anatomical muscles (not `FULL_BODY`).
2. Rank only the stale candidates by neglect, with never-trained groups first.
3. Show at most six chips and explain the cap when additional groups exist.

## Verification

1. Run the JVM test suite; existing ranking tests cover ordering and never-trained precedence.
2. Assemble and install the debug app.
3. On-device: confirm no more than six stale chips appear and the most-neglected groups lead.

## Out of scope

- No change to the seven-day stale threshold or muscle-training calculation.
- No new navigation or interaction for the chips.
