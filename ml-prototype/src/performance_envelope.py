"""Conservative, time-stamped summaries of ordinary performance observations.

These summaries are evidence of completed work, not maximum-strength estimates or exercise
progressions. A later capability model may use them with uncertainty; it must not imply more than
the underlying logs establish.
"""

from __future__ import annotations

from dataclasses import dataclass

from src.observation_contract import HoldObservation, WeightedRepObservation


@dataclass(frozen=True)
class WeightedRepPoint:
    load_kg: float
    reps: int
    ended_at_ms: int
    set_id: int


@dataclass(frozen=True)
class WeightedRepSummary:
    catalogue_key: str
    non_dominated_points: tuple[WeightedRepPoint, ...]
    latest_ended_at_ms: int


@dataclass(frozen=True)
class HoldSummary:
    catalogue_key: str
    longest_seconds: float
    longest_ended_at_ms: int
    latest_ended_at_ms: int


def _single_catalogue_key(observations: list[WeightedRepObservation | HoldObservation]) -> str:
    keys = {observation.catalogue_key for observation in observations}
    if len(keys) != 1:
        raise ValueError("summaries require observations for exactly one catalogue key")
    return next(iter(keys))


def _non_dominated(point: WeightedRepPoint, all_points: list[WeightedRepPoint]) -> bool:
    return not any(
        other.load_kg >= point.load_kg
        and other.reps >= point.reps
        and (other.load_kg > point.load_kg or other.reps > point.reps)
        for other in all_points
    )


def weighted_rep_summary(observations: list[WeightedRepObservation]) -> WeightedRepSummary:
    """Keep the observed load/repetition frontier without estimating an unlogged maximum."""

    if not observations:
        raise ValueError("weighted_rep_summary requires at least one observation")
    key = _single_catalogue_key(observations)
    points = [
        WeightedRepPoint(
            load_kg=observation.effective_load_kg,
            reps=observation.reps,
            ended_at_ms=observation.ended_at_ms,
            set_id=observation.set_id,
        )
        for observation in observations
    ]
    frontier = tuple(
        sorted(
            (point for point in points if _non_dominated(point, points)),
            key=lambda point: (point.load_kg, point.reps, point.ended_at_ms, point.set_id),
        ),
    )
    return WeightedRepSummary(
        catalogue_key=key,
        non_dominated_points=frontier,
        latest_ended_at_ms=max(point.ended_at_ms for point in points),
    )


def hold_summary(observations: list[HoldObservation]) -> HoldSummary:
    """Return the longest directly observed hold, separately retaining freshness of evidence."""

    if not observations:
        raise ValueError("hold_summary requires at least one observation")
    key = _single_catalogue_key(observations)
    longest = max(observations, key=lambda observation: (observation.hold_seconds, observation.ended_at_ms, observation.set_id))
    return HoldSummary(
        catalogue_key=key,
        longest_seconds=longest.hold_seconds,
        longest_ended_at_ms=longest.ended_at_ms,
        latest_ended_at_ms=max(observation.ended_at_ms for observation in observations),
    )
