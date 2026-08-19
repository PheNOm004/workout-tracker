"""Convert completed-session observations into conservative continuous-learning inputs."""

from __future__ import annotations

from collections import defaultdict
from typing import Mapping, Sequence

from src.continuous_capability_model import (
    PerformanceObservation,
    bodyweight_reps_observed_work_score,
    hold_observed_work_score,
    weighted_rep_observed_work_score,
)
from src.observation_contract import ExerciseMetadata, HoldObservation, LoggingType, WeightedRepObservation


TIMEGO_MUSCLE_COORDINATES = (
    "CHEST",
    "LATS",
    "UPPER_BACK",
    "TRAPS",
    "LOWER_BACK",
    "FRONT_DELTS",
    "SIDE_DELTS",
    "REAR_DELTS",
    "BICEPS",
    "TRICEPS",
    "FOREARMS",
    "ABS",
    "OBLIQUES",
    "QUADS",
    "HAMSTRINGS",
    "GLUTES",
    "ADDUCTORS",
    "CALVES",
    "FULL_BODY",
)


def _demand_vector(metadata: ExerciseMetadata) -> tuple[float, ...]:
    weights = dict(metadata.demand_weights)
    declared = set(metadata.demand_coordinates)
    unknown = declared - set(TIMEGO_MUSCLE_COORDINATES)
    if unknown:
        raise ValueError(f"unknown coach coordinate: {sorted(unknown)[0]}")
    return tuple(weights.get(coordinate, 1.0) if coordinate in declared else 0.0 for coordinate in TIMEGO_MUSCLE_COORDINATES)


def _metadata_for(
    catalogue_key: str,
    metadata_by_key: Mapping[str, ExerciseMetadata],
    expected_type: LoggingType,
) -> ExerciseMetadata:
    metadata = metadata_by_key.get(catalogue_key)
    if metadata is None:
        raise ValueError("missing metadata for completed-session observation")
    if metadata.logging_type != expected_type:
        raise ValueError("completed-session observation has incompatible metadata modality")
    return metadata


def _session_id(observations: Sequence[WeightedRepObservation | HoldObservation]) -> int:
    session_ids = {observation.session_id for observation in observations}
    if len(session_ids) != 1:
        raise ValueError("session summaries require exactly one session")
    return next(iter(session_ids))


def _weighted_score_and_basis(observation: WeightedRepObservation) -> tuple[float, str]:
    if observation.effective_load_kg > 0:
        return (
            weighted_rep_observed_work_score(
                effective_load_kg=observation.effective_load_kg,
                reps=observation.reps,
            ),
            "load_reps",
        )
    return bodyweight_reps_observed_work_score(reps=observation.reps), "reps_only"


def session_weighted_rep_performance_observations(
    observations: Sequence[WeightedRepObservation],
    metadata_by_key: Mapping[str, ExerciseMetadata],
    session_end_ms: int,
) -> tuple[PerformanceObservation, ...]:
    """Use one maximum directly completed work point per exercise after a session closes."""

    if session_end_ms <= 0:
        raise ValueError("session_end_ms must be positive")
    if not observations:
        return ()
    session_id = _session_id(observations)
    grouped: dict[tuple[str, str], list[WeightedRepObservation]] = defaultdict(list)
    for observation in observations:
        _, basis = _weighted_score_and_basis(observation)
        grouped[(observation.catalogue_key, basis)].append(observation)
    result: list[PerformanceObservation] = []
    for (key, basis), items in sorted(grouped.items()):
        metadata = _metadata_for(key, metadata_by_key, LoggingType.WEIGHT_REPS)
        selected = max(
            items,
            key=lambda item: (
                _weighted_score_and_basis(item)[0],
                item.ended_at_ms,
                item.set_id,
            ),
        )
        score, _ = _weighted_score_and_basis(selected)
        result.append(
            PerformanceObservation(
                catalogue_key=key,
                session_id=session_id,
                set_id=selected.set_id,
                ended_at_ms=session_end_ms,
                demand_vector=_demand_vector(metadata),
                observed_work_score=score,
                measurement_basis=basis,
            ),
        )
    return tuple(result)


def session_hold_performance_observations(
    observations: Sequence[HoldObservation],
    metadata_by_key: Mapping[str, ExerciseMetadata],
    session_end_ms: int,
) -> tuple[PerformanceObservation, ...]:
    """Use the longest directly completed hold per exercise after a session closes."""

    if session_end_ms <= 0:
        raise ValueError("session_end_ms must be positive")
    if not observations:
        return ()
    session_id = _session_id(observations)
    grouped: dict[str, list[HoldObservation]] = defaultdict(list)
    for observation in observations:
        grouped[observation.catalogue_key].append(observation)
    result: list[PerformanceObservation] = []
    for key, items in sorted(grouped.items()):
        metadata = _metadata_for(key, metadata_by_key, LoggingType.HOLD)
        selected = max(items, key=lambda item: (item.hold_seconds, item.ended_at_ms, item.set_id))
        result.append(
            PerformanceObservation(
                catalogue_key=key,
                session_id=session_id,
                set_id=selected.set_id,
                ended_at_ms=session_end_ms,
                demand_vector=_demand_vector(metadata),
                observed_work_score=hold_observed_work_score(hold_seconds=selected.hold_seconds),
                measurement_basis="hold_seconds",
            ),
        )
    return tuple(result)
