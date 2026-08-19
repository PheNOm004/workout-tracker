# TimeGo — Last-Set Performance While Logging (design)

**Date**: 2026-08-19
**Status**: approved for implementation
**Context**: Recommendation-engine backlog Piece B in `TimeGo/03 Feature Catalog`.

## Problem

The current Log screen can show a derived overload suggestion, but that does not answer the more
immediate question: what did the user actually perform last time for this exercise? The user needs
the prior real weight and reps visible while entering the current set, without treating it as a
recommendation or changing any logging inputs.

## Behaviour

- For each strength or calisthenics exercise, find the latest non-warmup set from the most recent
  **closed** session containing that exercise.
- Do not use sets from the active session: the comparison remains the previous completed workout.
- Display the result only inside the expanded strength logging row as `Last time: <weight> × <reps>`.
- Use existing calisthenics formatting (`BW` / `BW + k`) where added weight was recorded.
- If no eligible prior set exists, show nothing. The display is informational only: no prefilled
  inputs, no changes to overload suggestions, and no tap action.

## Data and integration

`lastWorkingSetByExercise` receives the observed sessions, logs, and exercise map. It excludes
warmup, cardio, and warmup-category logs; closed sessions are identified by non-null
`endEpochMillis`. `LogViewModel` derives a `StateFlow<Map<Long, SetLog>>` from the existing exercise,
set-log, and session streams, and `LogScreen` passes the applicable set to `StrengthLogRow`.

## Verification

- Unit tests prove that the latest log from a closed session wins, warmups are excluded, active
  session logs never replace the reference, and cardio/warmup categories are excluded.
- Build, full unit-test suite, and install.
- On device, expand an exercise with history and confirm its real last completed weight/reps appear;
  begin a session and log a set, then confirm the displayed reference still represents the prior
  completed session.

## Out of scope

- Showing hold or cardio history.
- Prefilling or submitting values from the reference set.
- Replacing or tuning `RuleBasedOverloadSuggester`.
- A history picker or multiple prior sets.
