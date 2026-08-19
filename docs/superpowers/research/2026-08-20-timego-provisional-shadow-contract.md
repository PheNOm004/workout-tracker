# TimeGo provisional continuous-shadow parity contract (v1)

**Status:** Provisional, hidden-only local engineering contract. It is a frozen synthetic parity
target for the later pure-Kotlin replay; it is not a promotion result, recommendation engine,
exercise ranking, candidate scorer, visible card, or medical/recovery claim. Checkpoint A and the
later shadow verdict remain required before any visible learner work.

## Runtime boundary

The Android implementation is deterministic, offline, and self-contained. It must not load
Python, a Python package, an external model file, the internet, cloud state, telemetry, a laptop
resource, or user-history fixtures at runtime. The JSON file named below is a **test resource**
containing invented values only. It is consumed by JVM parity tests, not the app.

The canonical later input remains a completed, ordered Room snapshot. This contract itself owns no
Room, Compose, UI, candidate, recommendation, exercise-name-pathway, or Android-context type.

## Version and fixture

- Contract version: `timego.provisional-continuous-shadow.v1`
- Vector schema version: `1`
- Test fixture: `app/src/test/resources/adaptive/provisional-shadow-vectors.json`
- Numeric tolerance: absolute error no greater than `1e-9`
- Fixture metadata identity: `synthetic.parity.v1` /
  `sha256:synthetic-fixture-no-user-history`. It is deliberately not a production metadata hash.

Each vector has a synthetic task key, basis, deterministic completed-session observation, an
explicit input state, expected baseline result, expected state mean/diagonal variance, and an
abstention/update reason. The five vectors cover a loaded-rep neutral baseline, a later loaded-rep
update, an independent reps-only neutral baseline, an independent hold neutral baseline, and a
long-gap loaded-rep update. They are static invented examples—not exported, copied, or derived
from a person's workout records.

## Immutable observation contract

An observation is valid only when all of the following are true:

- `catalogueKey` is immutable and non-empty.
- It belongs to exactly one `basis`: `load_reps`, `reps_only`, or `hold_seconds`.
- It comes from a completed session and carries deterministic session/set identifiers and the
  completed-session end epoch millis.
- `demandVector` has the state coordinate count and finite values.
- The transformed work score is finite.
- It is the single maximum completed work point for its exercise/basis/closed session (longest for
  holds), selected deterministically by score, logged time, then set ID.

The pure replay derives ascending
`(session.endEpochMillis, session.id, set.id, catalogueKey, basis)` order and groups closed
sessions by `(session.endEpochMillis, session.id)`. Equal values are not resolved by wall-clock
iteration order; these fields are the deterministic ties. Every observation in a completed session
is scored from the frozen state built from strictly earlier closed sessions and is only then
applied in the stated order, so a same-session observation cannot improve another same-session
prediction. Application uses the evolving current state: each actual recomputes its residual and
gain from the mean/variance left by the preceding actual. A residual calculated from the frozen
prediction state is never reused across every update.

The three measurement bases maintain completely independent state and personal-baseline maps.
No loaded score is compared with, calibrated from, or used to update a reps-only or hold state.

## Exact transforms

These are monotonic records of directly completed work, not E1RM, maximum-effort, or a prescribed
dose:

| Basis | Required positive values | Work score |
| --- | --- | --- |
| `load_reps` | recorded total load `load`, reps `reps` | `ln(1 + load * reps)` |
| `reps_only` | reps `reps`, exactly zero load, reviewed bodyweight-supported metadata | `ln(1 + reps)` |
| `hold_seconds` | hold duration `seconds` | `ln(1 + seconds)` |

`load` is already the stored total system load where the canonical log supplies it; it is read
once and no bodyweight is inferred or added. Negative/non-finite load is always excluded. Zero
load becomes reps-only evidence only when reviewed metadata explicitly declares bodyweight
support; an ordinary malformed zero-load strength row is excluded. Other non-positive required
values, incomplete, warm-up, unkeyed/custom, open-session, or duration-only-cardio inputs are also
excluded/abstain before this contract. They do not create a baseline or mutate state.

## State, parameters, and transition

For one basis, the immutable state is:

`mean[k]`, diagonal `variance[k]`, `observedAtEpochMillis`, and an ordered map of
`catalogueKey -> personalBaselineWorkScore`.

Frozen vectors use one coordinate and these provisional parameters:

`priorVariance = 1.0`, `processVariancePerDay = 0.05`, `observationVariance = 0.1`.

The prior has `mean[k] = 0` and `variance[k] = priorVariance`. Before an observation at `t`, let
`days = max(0, (t - observedAtEpochMillis) / 86,400,000)` (zero for a prior state). The transition
does not change `mean`; it widens only diagonal uncertainty:

`variance[k] = variance[k] + days * processVariancePerDay`.

`observedAtEpochMillis` becomes `max(previous, t)`. This deliberately does not encode a detraining
rate or make a physiological claim.

For a first `(catalogueKey, basis)` point, record its work score as that personal baseline,
preserve mean and variance after the time transition, return `updated = false`, and return
`registered_personal_baseline`. This neutral registration is not evidence of progress.

For a later same-key/same-basis point, with demand `d`, baseline `b`, observed score `y`, and
advanced state `(m, v)`, apply the deterministic diagonal update:

```text
observedChange  = y - b
predictedChange = Σ(m[k] * d[k])
totalVariance   = observationVariance + Σ(v[k] * d[k]^2)
gain[k]         = v[k] * d[k] / totalVariance
mean'[k]        = m[k] + gain[k] * (observedChange - predictedChange)
variance'[k]    = max(0, v[k] - gain[k] * d[k] * v[k])
```

The personal baseline remains the first score; later points update state but do not overwrite it.
For the supplied vectors, all demands equal `[1.0]` and the expected values are hand-frozen in the
fixture. The parity test must compare them using the stated tolerance, never recompute expected
results with the production implementation.

## Abstention and output boundary

No baseline yields `registered_personal_baseline`, which is the explicit insufficient-evidence
reason. Invalid/excluded inputs must return a deterministic exclusion/abstention and preserve the
prior state; unknown demand, non-finite scores, wrong dimensions, non-positive timestamps, and a
basis mismatch are invalid. The eventual caller may also abstain on unknown task demand or an
interval wider than its configured gate. This contract produces only hidden state/update facts and
abstention reasons. It cannot select, rank, display, or recommend an exercise.

## Parity acceptance

The Kotlin domain test must load only the static fixture and reproduce every expected update flag,
reason, baseline map, timestamp, mean, and diagonal variance within `1e-9`. The fixture is the
cross-language boundary; no runtime call into the research prototype is permitted.
