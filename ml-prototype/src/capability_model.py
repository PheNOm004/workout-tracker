"""Small, local capability posterior used for research before any app integration.

It learns only from real achieved/missed targets with declared task demand. Unknown demand,
unknown outcome, sparse evidence, and wide uncertainty all preserve abstention. This is an
estimator, not a progression ladder or exercise-ranking policy.
"""

from __future__ import annotations

from dataclasses import dataclass
from math import exp, sqrt


MILLIS_PER_DAY = 86_400_000


@dataclass(frozen=True)
class CapabilityModelConfig:
    coordinate_count: int
    prior_variance: float
    process_variance_per_day: float
    maximum_interval_width: float
    unseen_task_prior_variance: float | None = None


@dataclass(frozen=True)
class CapabilityPosterior:
    mean: tuple[float, ...]
    variance: tuple[float, ...]
    observed_at_ms: int | None

    @classmethod
    def prior(cls, config: CapabilityModelConfig) -> "CapabilityPosterior":
        if config.coordinate_count <= 0:
            raise ValueError("coordinate_count must be positive")
        if config.prior_variance <= 0:
            raise ValueError("prior_variance must be positive")
        return cls(
            mean=(0.0,) * config.coordinate_count,
            variance=(config.prior_variance,) * config.coordinate_count,
            observed_at_ms=None,
        )


@dataclass(frozen=True)
class BinaryOutcome:
    session_id: int
    set_id: int
    ended_at_ms: int
    demand_vector: tuple[float, ...]
    task_demand: float | None
    met_target: bool | None


@dataclass(frozen=True)
class TaskDemandPrior:
    """A task-demand distribution, never an exercise level or progression position."""

    mean: float
    variance: float
    is_broad_unseen_prior: bool = False


@dataclass(frozen=True)
class PosteriorUpdate:
    posterior: CapabilityPosterior
    updated: bool
    reason: str | None = None


@dataclass(frozen=True)
class CapabilityAssessment:
    abstained: bool
    reason: str | None
    probability: float | None
    lower_probability: float | None
    upper_probability: float | None
    used_unseen_task_prior: bool = False


def unseen_task_demand_prior(config: CapabilityModelConfig) -> TaskDemandPrior | None:
    """Return the explicitly enabled broad residual prior for a declared but unseen task."""

    if config.unseen_task_prior_variance is None:
        return None
    if config.unseen_task_prior_variance <= 0:
        raise ValueError("unseen_task_prior_variance must be positive when configured")
    return TaskDemandPrior(
        mean=0.0,
        variance=config.unseen_task_prior_variance,
        is_broad_unseen_prior=True,
    )


def _sigmoid(value: float) -> float:
    if value >= 0:
        return 1.0 / (1.0 + exp(-value))
    exponent = exp(value)
    return exponent / (1.0 + exponent)


def _validate_dimensions(values: tuple[float, ...], posterior: CapabilityPosterior) -> None:
    if len(values) != len(posterior.mean):
        raise ValueError("demand_vector must match posterior dimensions")


def _advance_uncertainty(
    posterior: CapabilityPosterior,
    ended_at_ms: int,
    config: CapabilityModelConfig,
) -> CapabilityPosterior:
    if ended_at_ms <= 0:
        raise ValueError("ended_at_ms must be positive")
    if posterior.observed_at_ms is None:
        return CapabilityPosterior(posterior.mean, posterior.variance, ended_at_ms)
    elapsed_days = max(0.0, (ended_at_ms - posterior.observed_at_ms) / MILLIS_PER_DAY)
    return CapabilityPosterior(
        mean=posterior.mean,
        variance=tuple(value + elapsed_days * config.process_variance_per_day for value in posterior.variance),
        observed_at_ms=max(posterior.observed_at_ms, ended_at_ms),
    )


def advance_posterior_to(
    posterior: CapabilityPosterior,
    ended_at_ms: int,
    config: CapabilityModelConfig,
) -> CapabilityPosterior:
    """Advance uncertainty to a prediction time without inventing an outcome update."""

    return _advance_uncertainty(posterior, ended_at_ms, config)


def update_posterior(
    posterior: CapabilityPosterior,
    outcome: BinaryOutcome,
    config: CapabilityModelConfig,
) -> PosteriorUpdate:
    """Apply one local Bayesian-style logistic update, or explicitly decline the evidence."""

    _validate_dimensions(outcome.demand_vector, posterior)
    advanced = _advance_uncertainty(posterior, outcome.ended_at_ms, config)
    if outcome.met_target is None:
        return PosteriorUpdate(advanced, updated=False, reason="missing_target_outcome")
    if outcome.task_demand is None:
        return PosteriorUpdate(advanced, updated=False, reason="unidentified_task_demand")

    linear = sum(mean * coordinate for mean, coordinate in zip(advanced.mean, outcome.demand_vector))
    probability = _sigmoid(linear - outcome.task_demand)
    observed = 1.0 if outcome.met_target else 0.0

    means: list[float] = []
    variances: list[float] = []
    for mean, variance, coordinate in zip(advanced.mean, advanced.variance, outcome.demand_vector):
        information = probability * (1.0 - probability) * coordinate * coordinate
        updated_variance = 1.0 / ((1.0 / variance) + information)
        means.append(mean + updated_variance * coordinate * (observed - probability))
        variances.append(updated_variance)
    return PosteriorUpdate(
        CapabilityPosterior(tuple(means), tuple(variances), advanced.observed_at_ms),
        updated=True,
    )


def assess_candidate(
    posterior: CapabilityPosterior,
    *,
    demand_vector: tuple[float, ...],
    task_demand: float | None,
    anchor_supported: bool,
    config: CapabilityModelConfig,
    task_demand_variance: float = 0.0,
    task_demand_prior: TaskDemandPrior | None = None,
) -> CapabilityAssessment:
    """Return a calibrated estimate only for an identified, anchored candidate task."""

    _validate_dimensions(demand_vector, posterior)
    if task_demand_prior is not None:
        task_demand = task_demand_prior.mean
        task_demand_variance = task_demand_prior.variance
    if task_demand is None or not anchor_supported:
        return CapabilityAssessment(True, "unidentified_task_demand", None, None, None)
    if task_demand_variance < 0:
        raise ValueError("task_demand_variance must be non-negative")

    linear = sum(mean * coordinate for mean, coordinate in zip(posterior.mean, demand_vector)) - task_demand
    standard_deviation = sqrt(
        sum(variance * coordinate * coordinate for variance, coordinate in zip(posterior.variance, demand_vector))
        + task_demand_variance
    )
    probability = _sigmoid(linear)
    lower = _sigmoid(linear - 1.96 * standard_deviation)
    upper = _sigmoid(linear + 1.96 * standard_deviation)
    if upper - lower > config.maximum_interval_width:
        return CapabilityAssessment(
            True,
            "uncertainty_too_high",
            None,
            None,
            None,
            task_demand_prior is not None and task_demand_prior.is_broad_unseen_prior,
        )
    return CapabilityAssessment(
        False,
        None,
        probability,
        lower,
        upper,
        task_demand_prior is not None and task_demand_prior.is_broad_unseen_prior,
    )
