"""Immutable, ladder-free observations for adaptive-coach research.

This module deliberately describes what one completed set measured.  It never decides what
exercise should come next.  Those decisions require a separately validated capability model.
"""

from __future__ import annotations

from dataclasses import dataclass
from enum import Enum
from hashlib import sha256
import json
from typing import TypeAlias


class LoggingType(str, Enum):
    WEIGHT_REPS = "WEIGHT_REPS"
    HOLD = "HOLD"
    DURATION_DISTANCE = "DURATION_DISTANCE"


class TargetProvenance(str, Enum):
    """Whether a target existed before the set, rather than being filled in after it."""

    DECLARED_BEFORE_SET = "DECLARED_BEFORE_SET"
    AUTO_FILLED_AFTER_SET = "AUTO_FILLED_AFTER_SET"
    UNKNOWN = "UNKNOWN"


@dataclass(frozen=True)
class ExerciseMetadata:
    """Versioned catalogue identity and declarative measurement attributes only."""

    catalogue_key: str
    logging_type: LoggingType
    demand_coordinates: tuple[str, ...]
    equipment: tuple[str, ...]
    demand_weights: tuple[tuple[str, float], ...] = ()
    category: str | None = None
    static_exclusions: tuple[str, ...] = ()
    bodyweight_supported: bool = False


@dataclass(frozen=True)
class RawSetLog:
    """The subset of a canonical log needed to form one research observation."""

    session_id: int
    set_id: int
    ended_at_ms: int | None
    logging_type: LoggingType
    reps: int | None = None
    target_reps: int | None = None
    weight_kg: float | None = None
    added_weight_kg: float | None = None
    hold_seconds: float | None = None
    target_hold_seconds: float | None = None
    duration_minutes: float | None = None
    distance_km: float | None = None
    bodyweight_kg: float | None = None
    rpe: float | None = None
    is_warmup: bool = False
    target_provenance: TargetProvenance = TargetProvenance.UNKNOWN


@dataclass(frozen=True)
class ExcludedObservation:
    reason: str


@dataclass(frozen=True)
class WeightedRepObservation:
    catalogue_key: str
    session_id: int
    set_id: int
    ended_at_ms: int
    reps: int
    target_reps: int | None
    target_met: bool | None
    effective_load_kg: float
    bodyweight_kg: float | None
    rpe: float | None


@dataclass(frozen=True)
class HoldObservation:
    catalogue_key: str
    session_id: int
    set_id: int
    ended_at_ms: int
    hold_seconds: float
    target_hold_seconds: float | None
    target_met: bool | None
    bodyweight_kg: float | None
    rpe: float | None


@dataclass(frozen=True)
class StaminaObservation:
    catalogue_key: str
    session_id: int
    set_id: int
    ended_at_ms: int
    duration_minutes: float
    distance_km: float
    pace_minutes_per_km: float
    rpe: float | None


Observation: TypeAlias = (
    ExcludedObservation | WeightedRepObservation | HoldObservation | StaminaObservation
)


def _target_met(
    actual: float,
    target: float | int | None,
    provenance: TargetProvenance,
) -> bool | None:
    if provenance != TargetProvenance.DECLARED_BEFORE_SET or target is None or target <= 0:
        return None
    return actual >= target


def _valid_timestamp(raw: RawSetLog) -> ExcludedObservation | None:
    if raw.ended_at_ms is None or raw.ended_at_ms <= 0:
        return ExcludedObservation("missing_timestamp")
    return None


def map_observation(raw: RawSetLog, metadata: ExerciseMetadata) -> Observation:
    """Map one raw log, preserving uncertainty and refusing invalid modality evidence."""

    if raw.is_warmup:
        return ExcludedObservation("warmup")
    if raw.logging_type != metadata.logging_type:
        return ExcludedObservation("logging_type_mismatch")
    invalid_timestamp = _valid_timestamp(raw)
    if invalid_timestamp is not None:
        return invalid_timestamp

    if raw.logging_type == LoggingType.WEIGHT_REPS:
        if raw.reps is None or raw.reps <= 0:
            return ExcludedObservation("missing_reps")
        effective_load = raw.weight_kg or 0.0
        if effective_load < 0:
            return ExcludedObservation("invalid_load")
        if effective_load == 0 and not metadata.bodyweight_supported:
            return ExcludedObservation("missing_load")
        return WeightedRepObservation(
            catalogue_key=metadata.catalogue_key,
            session_id=raw.session_id,
            set_id=raw.set_id,
            ended_at_ms=raw.ended_at_ms,
            reps=raw.reps,
            target_reps=raw.target_reps,
            target_met=_target_met(raw.reps, raw.target_reps, raw.target_provenance),
            # TimeGo persists calisthenics weightKg as the total bodyweight-plus-added load.
            # addedWeightKg is only a display aid, so incorporating it here would double-count.
            effective_load_kg=effective_load,
            bodyweight_kg=raw.bodyweight_kg,
            rpe=raw.rpe,
        )

    if raw.logging_type == LoggingType.HOLD:
        if raw.hold_seconds is None or raw.hold_seconds <= 0:
            return ExcludedObservation("missing_hold_seconds")
        return HoldObservation(
            catalogue_key=metadata.catalogue_key,
            session_id=raw.session_id,
            set_id=raw.set_id,
            ended_at_ms=raw.ended_at_ms,
            hold_seconds=raw.hold_seconds,
            target_hold_seconds=raw.target_hold_seconds,
            target_met=_target_met(raw.hold_seconds, raw.target_hold_seconds, raw.target_provenance),
            bodyweight_kg=raw.bodyweight_kg,
            rpe=raw.rpe,
        )

    if raw.duration_minutes is None or raw.duration_minutes <= 0:
        return ExcludedObservation("missing_duration")
    if raw.distance_km is None or raw.distance_km <= 0:
        # A duration-only record remains useful volume information, but not a calibrated stamina
        # capability observation. It must not be promoted to one by imputation.
        return ExcludedObservation("duration_without_distance")
    return StaminaObservation(
        catalogue_key=metadata.catalogue_key,
        session_id=raw.session_id,
        set_id=raw.set_id,
        ended_at_ms=raw.ended_at_ms,
        duration_minutes=raw.duration_minutes,
        distance_km=raw.distance_km,
        pace_minutes_per_km=raw.duration_minutes / raw.distance_km,
        rpe=raw.rpe,
    )


def catalogue_fingerprint(metadata: list[ExerciseMetadata]) -> str:
    """Stable version input; display labels and Room IDs are intentionally absent."""

    payload = [
        {
            "catalogue_key": item.catalogue_key,
            "logging_type": item.logging_type.value,
            "demand_coordinates": sorted(item.demand_coordinates),
            "demand_weights": sorted(item.demand_weights),
            "equipment": sorted(item.equipment),
            "category": item.category,
            "static_exclusions": sorted(item.static_exclusions),
            "bodyweight_supported": item.bodyweight_supported,
        }
        for item in metadata
    ]
    canonical = json.dumps(sorted(payload, key=lambda item: item["catalogue_key"]), separators=(",", ":"))
    return sha256(canonical.encode("utf-8")).hexdigest()


def missing_measurement_anchors(metadata: list[ExerciseMetadata]) -> set[tuple[str, ...]]:
    """Return demand vectors with fewer than two independent catalogue measurements."""

    keys_by_vector: dict[tuple[str, ...], set[str]] = {}
    for item in metadata:
        vector = tuple(sorted(item.demand_coordinates))
        keys_by_vector.setdefault(vector, set()).add(item.catalogue_key)
    return {vector for vector, keys in keys_by_vector.items() if len(keys) < 2}
