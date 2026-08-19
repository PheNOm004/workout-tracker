"""Aggregate-only chronological evaluation for ordinary continuous performance evidence.

The evaluator scores every session from state built strictly before that session.  It returns
only aggregate metrics and counts: personal observations and per-session predictions never leave
this module in an evaluation report.
"""

from __future__ import annotations

from dataclasses import dataclass
from math import sqrt
from typing import Sequence

from src.continuous_capability_model import (
    ContinuousCapabilityConfig,
    ContinuousCapabilityState,
    PerformanceObservation,
    update_from_performance_observation,
)


@dataclass(frozen=True)
class ContinuousEvaluationSession:
    """One completed session, already reduced by the continuous-observation mapper."""

    session_id: int
    ended_at_ms: int
    observations: tuple[PerformanceObservation, ...]


@dataclass(frozen=True)
class ContinuousEvaluationResult:
    """Aggregate-only result; deliberately contains no session, task, set, or row identifiers."""

    prediction_boundaries: int
    first_observation_registrations: int
    insufficient_evidence_observations: int
    candidate_abstentions: int
    candidate_mae: float | None
    baseline_mae: float | None
    candidate_rmse: float | None
    baseline_rmse: float | None
    winner: str | None
    insufficient_evidence: bool


def _ordered_sessions(
    sessions: Sequence[ContinuousEvaluationSession],
) -> list[ContinuousEvaluationSession]:
    return sorted(sessions, key=lambda session: (session.ended_at_ms, session.session_id))


def _ordered_observations(
    session: ContinuousEvaluationSession,
) -> list[PerformanceObservation]:
    """Stable set/key/basis ordering only affects post-session state, never test predictions."""

    return sorted(
        session.observations,
        key=lambda observation: (
            observation.set_id,
            observation.catalogue_key,
            observation.measurement_basis,
        ),
    )


def _observation_key(observation: PerformanceObservation) -> tuple[str, str]:
    return observation.catalogue_key, observation.measurement_basis


def _validate_session(session: ContinuousEvaluationSession) -> None:
    if session.ended_at_ms <= 0:
        raise ValueError("session ended_at_ms must be positive")
    keys: set[tuple[str, str]] = set()
    for observation in session.observations:
        if observation.session_id != session.session_id:
            raise ValueError("session observations must match the containing session")
        if observation.ended_at_ms != session.ended_at_ms:
            raise ValueError("session observations must use the completed session end time")
        key = _observation_key(observation)
        if key in keys:
            raise ValueError("session must contain at most one observation per exercise and measurement basis")
        keys.add(key)


def _mean_absolute_error(errors: list[float]) -> float | None:
    return sum(abs(error) for error in errors) / len(errors) if errors else None


def _root_mean_squared_error(errors: list[float]) -> float | None:
    return sqrt(sum(error * error for error in errors) / len(errors)) if errors else None


def _winner(candidate_mae: float | None, baseline_mae: float | None) -> str | None:
    if candidate_mae is None or baseline_mae is None:
        return None
    if candidate_mae < baseline_mae:
        return "continuous_capability"
    if baseline_mae < candidate_mae:
        return "last_observation"
    return "tie"


def run_continuous_chronological_evaluation(
    sessions: Sequence[ContinuousEvaluationSession],
    config: ContinuousCapabilityConfig,
) -> ContinuousEvaluationResult:
    """Compare continuous and last-observation predictions at later session boundaries.

    A session is scored against a frozen state from strictly earlier completed sessions, then all of
    its observations are applied in deterministic order.  Consequently a same-session outcome
    cannot improve another prediction in that session, and future sessions cannot affect earlier
    aggregate metrics.
    """

    ordered = _ordered_sessions(sessions)
    for session in ordered:
        _validate_session(session)

    states_by_basis: dict[str, ContinuousCapabilityState] = {}
    last_observed_work: dict[tuple[str, str], float] = {}
    candidate_errors: list[float] = []
    baseline_errors: list[float] = []
    first_observation_registrations = 0
    insufficient_evidence_observations = 0
    candidate_abstentions = 0

    for session in ordered:
        observations = _ordered_observations(session)

        # Evaluate from the pre-session snapshot.  This is intentionally separate from updates.
        for observation in observations:
            key = _observation_key(observation)
            state = states_by_basis.get(
                observation.measurement_basis,
                ContinuousCapabilityState.prior(config),
            )
            baseline = state.baseline_for(*key)
            prior_last_observation = last_observed_work.get(key)
            if baseline is None or prior_last_observation is None:
                first_observation_registrations += 1
                insufficient_evidence_observations += 1
                continue

            predicted_change = sum(
                mean * demand for mean, demand in zip(state.mean, observation.demand_vector)
            )
            candidate_errors.append(baseline + predicted_change - observation.observed_work_score)
            baseline_errors.append(prior_last_observation - observation.observed_work_score)

        # Apply the held-out session only after every one of its scores is final.
        for observation in observations:
            state = states_by_basis.get(
                observation.measurement_basis,
                ContinuousCapabilityState.prior(config),
            )
            update = update_from_performance_observation(state, observation, config)
            states_by_basis[observation.measurement_basis] = update.state
            last_observed_work[_observation_key(observation)] = observation.observed_work_score

    candidate_mae = _mean_absolute_error(candidate_errors)
    baseline_mae = _mean_absolute_error(baseline_errors)
    boundaries = len(candidate_errors)
    return ContinuousEvaluationResult(
        prediction_boundaries=boundaries,
        first_observation_registrations=first_observation_registrations,
        insufficient_evidence_observations=insufficient_evidence_observations,
        candidate_abstentions=candidate_abstentions,
        candidate_mae=candidate_mae,
        baseline_mae=baseline_mae,
        candidate_rmse=_root_mean_squared_error(candidate_errors),
        baseline_rmse=_root_mean_squared_error(baseline_errors),
        winner=_winner(candidate_mae, baseline_mae),
        insufficient_evidence=boundaries == 0,
    )
