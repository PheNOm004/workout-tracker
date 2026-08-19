"""Leakage-safe audit of direct weighted-rep performance evidence.

This module does not score recommendations. It describes whether later logged performance falls
inside or expands the envelope demonstrated before that closed session.
"""

from __future__ import annotations

from dataclasses import dataclass

from src.observation_contract import WeightedRepObservation


@dataclass(frozen=True)
class PerformanceSessionEvidence:
    session_id: int
    ended_at_ms: int
    observations: tuple[WeightedRepObservation, ...]


@dataclass(frozen=True)
class EnvelopeAuditEvent:
    training_session_ids: tuple[int, ...]
    test_session_id: int
    test_set_id: int
    extends_prior_envelope: bool | None


@dataclass(frozen=True)
class WeightedEnvelopeAuditResult:
    events: tuple[EnvelopeAuditEvent, ...]
    comparable_observations: int
    extension_rate: float | None


def _ordered_sessions(sessions: list[PerformanceSessionEvidence]) -> list[PerformanceSessionEvidence]:
    return sorted(sessions, key=lambda session: (session.ended_at_ms, session.session_id))


def _ordered_observations(session: PerformanceSessionEvidence) -> list[WeightedRepObservation]:
    return sorted(session.observations, key=lambda observation: (observation.ended_at_ms, observation.set_id))


def _is_dominated_by_history(
    observation: WeightedRepObservation,
    history: list[WeightedRepObservation],
) -> bool:
    return any(
        previous.effective_load_kg >= observation.effective_load_kg
        and previous.reps >= observation.reps
        for previous in history
    )


def run_weighted_envelope_audit(
    sessions: list[PerformanceSessionEvidence],
) -> WeightedEnvelopeAuditResult:
    """Compare each test-session set only with the same exercise in earlier closed sessions."""

    ordered = _ordered_sessions(sessions)
    events: list[EnvelopeAuditEvent] = []
    comparable: list[bool] = []
    for index in range(1, len(ordered)):
        training = ordered[:index]
        test = ordered[index]
        training_ids = tuple(session.session_id for session in training)
        history_by_key: dict[str, list[WeightedRepObservation]] = {}
        for prior_session in training:
            for observation in _ordered_observations(prior_session):
                history_by_key.setdefault(observation.catalogue_key, []).append(observation)
        for observation in _ordered_observations(test):
            same_exercise_history = history_by_key.get(observation.catalogue_key, [])
            extends = (
                None
                if not same_exercise_history
                else not _is_dominated_by_history(observation, same_exercise_history)
            )
            events.append(
                EnvelopeAuditEvent(
                    training_session_ids=training_ids,
                    test_session_id=test.session_id,
                    test_set_id=observation.set_id,
                    extends_prior_envelope=extends,
                ),
            )
            if extends is not None:
                comparable.append(extends)
    return WeightedEnvelopeAuditResult(
        events=tuple(events),
        comparable_observations=len(comparable),
        extension_rate=(sum(comparable) / len(comparable)) if comparable else None,
    )
