"""Leakage-safe, closed-session evaluation for the capability research prototype."""

from __future__ import annotations

from dataclasses import dataclass

from src.capability_model import (
    BinaryOutcome,
    CapabilityModelConfig,
    CapabilityPosterior,
    advance_posterior_to,
    assess_candidate,
    update_posterior,
)


@dataclass(frozen=True)
class SessionEvidence:
    session_id: int
    ended_at_ms: int
    outcomes: tuple[BinaryOutcome, ...]


@dataclass(frozen=True)
class ChronologicalSplit:
    training_session_ids: tuple[int, ...]
    test_session_id: int


@dataclass(frozen=True)
class BacktestPrediction:
    training_session_ids: tuple[int, ...]
    test_session_id: int
    test_set_id: int
    probability: float | None
    target_met: bool | None
    abstained: bool
    abstention_reason: str | None


@dataclass(frozen=True)
class BacktestResult:
    predictions: tuple[BacktestPrediction, ...]
    evaluable_predictions: int
    abstentions: int
    brier_score: float | None
    last_observation_brier: float | None
    ewma_brier: float | None


def _ordered_sessions(sessions: list[SessionEvidence]) -> list[SessionEvidence]:
    return sorted(sessions, key=lambda item: (item.ended_at_ms, item.session_id))


def _ordered_outcomes(session: SessionEvidence) -> list[BinaryOutcome]:
    return sorted(session.outcomes, key=lambda item: (item.ended_at_ms, item.session_id, item.set_id))


def chronological_splits(sessions: list[SessionEvidence]) -> list[ChronologicalSplit]:
    """One test session at a time; every prediction sees only strictly earlier sessions."""

    ordered = _ordered_sessions(sessions)
    return [
        ChronologicalSplit(
            training_session_ids=tuple(session.session_id for session in ordered[:index]),
            test_session_id=ordered[index].session_id,
        )
        for index in range(1, len(ordered))
    ]


def _matching_target_outcomes(
    history: list[BinaryOutcome],
    target: BinaryOutcome,
) -> list[BinaryOutcome]:
    return [
        outcome
        for outcome in sorted(history, key=lambda item: (item.ended_at_ms, item.session_id, item.set_id))
        if outcome.demand_vector == target.demand_vector
        and outcome.task_demand == target.task_demand
        and outcome.met_target is not None
    ]


def last_observation_probability(history: list[BinaryOutcome], target: BinaryOutcome) -> float | None:
    """Simple same-task comparator; it cannot borrow signal from future or different tasks."""

    matches = _matching_target_outcomes(history, target)
    if not matches:
        return None
    return 1.0 if matches[-1].met_target else 0.0


def ewma_probability(
    history: list[BinaryOutcome],
    target: BinaryOutcome,
    *,
    alpha: float = 0.35,
) -> float | None:
    """A simple target-outcome history comparator, not a recommendation rule."""

    if not 0.0 < alpha <= 1.0:
        raise ValueError("alpha must be in (0, 1]")
    matches = _matching_target_outcomes(history, target)
    if not matches:
        return None
    value = 1.0 if matches[0].met_target else 0.0
    for outcome in matches[1:]:
        observed = 1.0 if outcome.met_target else 0.0
        value = alpha * observed + (1.0 - alpha) * value
    return value


def _fit_prior_sessions(
    sessions: list[SessionEvidence],
    config: CapabilityModelConfig,
) -> CapabilityPosterior:
    posterior = CapabilityPosterior.prior(config)
    for session in _ordered_sessions(sessions):
        for outcome in _ordered_outcomes(session):
            posterior = update_posterior(posterior, outcome, config).posterior
    return posterior


def run_capability_backtest(
    sessions: list[SessionEvidence],
    config: CapabilityModelConfig,
) -> BacktestResult:
    """Evaluate all later closed-session observations without using any future evidence."""

    ordered = _ordered_sessions(sessions)
    predictions: list[BacktestPrediction] = []
    last_observation_errors: list[float] = []
    ewma_errors: list[float] = []
    for index in range(1, len(ordered)):
        training = ordered[:index]
        test = ordered[index]
        training_ids = tuple(session.session_id for session in training)
        posterior = _fit_prior_sessions(training, config)
        history = [outcome for session in training for outcome in _ordered_outcomes(session)]
        for outcome in _ordered_outcomes(test):
            prediction_state = advance_posterior_to(posterior, outcome.ended_at_ms, config)
            assessment = assess_candidate(
                prediction_state,
                demand_vector=outcome.demand_vector,
                task_demand=outcome.task_demand,
                anchor_supported=outcome.task_demand is not None,
                config=config,
            )
            predictions.append(
                BacktestPrediction(
                    training_session_ids=training_ids,
                    test_session_id=test.session_id,
                    test_set_id=outcome.set_id,
                    probability=assessment.probability,
                    target_met=outcome.met_target,
                    abstained=assessment.abstained,
                    abstention_reason=assessment.reason,
                )
            )
            if outcome.met_target is not None:
                observed = 1.0 if outcome.met_target else 0.0
                last = last_observation_probability(history, outcome)
                ewma = ewma_probability(history, outcome)
                if last is not None:
                    last_observation_errors.append((last - observed) ** 2)
                if ewma is not None:
                    ewma_errors.append((ewma - observed) ** 2)

    evaluable = [
        prediction
        for prediction in predictions
        if not prediction.abstained and prediction.probability is not None and prediction.target_met is not None
    ]
    brier = None
    if evaluable:
        brier = sum(
            (prediction.probability - (1.0 if prediction.target_met else 0.0)) ** 2
            for prediction in evaluable
        ) / len(evaluable)
    return BacktestResult(
        predictions=tuple(predictions),
        evaluable_predictions=len(evaluable),
        abstentions=sum(prediction.abstained for prediction in predictions),
        brier_score=brier,
        last_observation_brier=(sum(last_observation_errors) / len(last_observation_errors))
        if last_observation_errors
        else None,
        ewma_brier=(sum(ewma_errors) / len(ewma_errors)) if ewma_errors else None,
    )
