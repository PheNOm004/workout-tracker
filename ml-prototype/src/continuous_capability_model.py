"""Conservative continuous learning from repeated ordinary performance evidence.

The model learns changes relative to each person's first observed performance for the same task.
It does not interpret a set as an all-out test or a maximum-strength estimate.
"""

from __future__ import annotations

from dataclasses import dataclass
from math import isfinite, log1p


MILLIS_PER_DAY = 86_400_000


@dataclass(frozen=True)
class ContinuousCapabilityConfig:
    coordinate_count: int
    prior_variance: float
    process_variance_per_day: float
    observation_variance: float


@dataclass(frozen=True)
class PerformanceObservation:
    catalogue_key: str
    session_id: int
    set_id: int
    ended_at_ms: int
    demand_vector: tuple[float, ...]
    observed_work_score: float
    measurement_basis: str = "load_reps"


@dataclass(frozen=True)
class ContinuousCapabilityState:
    mean: tuple[float, ...]
    variance: tuple[float, ...]
    observed_at_ms: int | None
    task_baselines: tuple[tuple[str, float], ...]

    @classmethod
    def prior(cls, config: ContinuousCapabilityConfig) -> "ContinuousCapabilityState":
        if config.coordinate_count <= 0:
            raise ValueError("coordinate_count must be positive")
        if config.prior_variance <= 0:
            raise ValueError("prior_variance must be positive")
        if config.observation_variance <= 0:
            raise ValueError("observation_variance must be positive")
        return cls(
            mean=(0.0,) * config.coordinate_count,
            variance=(config.prior_variance,) * config.coordinate_count,
            observed_at_ms=None,
            task_baselines=(),
        )

    def baseline_for(self, catalogue_key: str, measurement_basis: str = "load_reps") -> float | None:
        return dict(self.task_baselines).get(_baseline_key(catalogue_key, measurement_basis))


@dataclass(frozen=True)
class ContinuousPerformanceUpdate:
    state: ContinuousCapabilityState
    updated: bool
    reason: str | None = None


def weighted_rep_observed_work_score(*, effective_load_kg: float, reps: int) -> float:
    """A monotonic record of completed external work, not an E1RM estimate."""

    if effective_load_kg <= 0 or reps <= 0:
        raise ValueError("weighted-rep work requires positive load and reps")
    return log1p(effective_load_kg * reps)


def bodyweight_reps_observed_work_score(*, reps: int) -> float:
    """A reps-only record when total system load was not logged; it is not loaded strength."""

    if reps <= 0:
        raise ValueError("bodyweight-rep work requires positive reps")
    return log1p(reps)


def hold_observed_work_score(*, hold_seconds: float) -> float:
    """A monotonic record of a completed hold, distinct from weighted-rep work."""

    if hold_seconds <= 0:
        raise ValueError("hold work requires positive seconds")
    return log1p(hold_seconds)


def _validate_observation(
    state: ContinuousCapabilityState,
    observation: PerformanceObservation,
) -> None:
    if len(observation.demand_vector) != len(state.mean):
        raise ValueError("demand_vector must match state dimensions")
    if observation.ended_at_ms <= 0:
        raise ValueError("ended_at_ms must be positive")
    if not isfinite(observation.observed_work_score):
        raise ValueError("observed_work_score must be finite")


def _advance_state(
    state: ContinuousCapabilityState,
    ended_at_ms: int,
    config: ContinuousCapabilityConfig,
) -> ContinuousCapabilityState:
    if state.observed_at_ms is None:
        return ContinuousCapabilityState(state.mean, state.variance, ended_at_ms, state.task_baselines)
    elapsed_days = max(0.0, (ended_at_ms - state.observed_at_ms) / MILLIS_PER_DAY)
    return ContinuousCapabilityState(
        mean=state.mean,
        variance=tuple(value + elapsed_days * config.process_variance_per_day for value in state.variance),
        observed_at_ms=max(state.observed_at_ms, ended_at_ms),
        task_baselines=state.task_baselines,
    )


def _baseline_key(catalogue_key: str, measurement_basis: str) -> str:
    return f"{catalogue_key}\x1f{measurement_basis}"


def _with_baseline(
    state: ContinuousCapabilityState,
    catalogue_key: str,
    measurement_basis: str,
    score: float,
) -> ContinuousCapabilityState:
    values = dict(state.task_baselines)
    values[_baseline_key(catalogue_key, measurement_basis)] = score
    return ContinuousCapabilityState(
        state.mean,
        state.variance,
        state.observed_at_ms,
        tuple(sorted(values.items())),
    )


def update_from_performance_observation(
    state: ContinuousCapabilityState,
    observation: PerformanceObservation,
    config: ContinuousCapabilityConfig,
) -> ContinuousPerformanceUpdate:
    """Update only after a same-task personal baseline exists; first encounters stay neutral."""

    _validate_observation(state, observation)
    advanced = _advance_state(state, observation.ended_at_ms, config)
    baseline = advanced.baseline_for(observation.catalogue_key, observation.measurement_basis)
    if baseline is None:
        return ContinuousPerformanceUpdate(
            _with_baseline(
                advanced,
                observation.catalogue_key,
                observation.measurement_basis,
                observation.observed_work_score,
            ),
            updated=False,
            reason="registered_personal_baseline",
        )

    observed_change = observation.observed_work_score - baseline
    predicted_change = sum(mean * demand for mean, demand in zip(advanced.mean, observation.demand_vector))
    total_variance = config.observation_variance + sum(
        variance * demand * demand for variance, demand in zip(advanced.variance, observation.demand_vector)
    )
    gains = tuple(variance * demand / total_variance for variance, demand in zip(advanced.variance, observation.demand_vector))
    updated_mean = tuple(
        mean + gain * (observed_change - predicted_change)
        for mean, gain in zip(advanced.mean, gains)
    )
    updated_variance = tuple(
        max(0.0, variance - gain * demand * variance)
        for variance, gain, demand in zip(advanced.variance, gains, observation.demand_vector)
    )
    return ContinuousPerformanceUpdate(
        ContinuousCapabilityState(updated_mean, updated_variance, advanced.observed_at_ms, advanced.task_baselines),
        updated=True,
    )
