"""Read-only adapter from canonical TimeGo Room rows to research observations.

Room IDs and display names stay outside coach identity. Built-in rows contribute only when their
persisted catalogue key is present; custom/unkeyed rows are deliberately excluded.
"""

from __future__ import annotations

from typing import Any, Mapping

from src.observation_contract import ExerciseMetadata, LoggingType, RawSetLog, TargetProvenance


def _string_list(value: Any) -> list[str]:
    if isinstance(value, list):
        return [item for item in value if isinstance(item, str) and item]
    if isinstance(value, str):
        return [item for item in value.split("\x1f") if item]
    return []


def _logging_type(value: Any) -> LoggingType | None:
    try:
        return LoggingType(value)
    except ValueError:
        return None


def _muscle_weights(value: Any) -> dict[str, float]:
    if isinstance(value, Mapping):
        entries = value.items()
    elif isinstance(value, str):
        entries = (
            tuple(item.split("\x1f", 1))
            for item in value.split("\x1e")
            if "\x1f" in item
        )
    else:
        return {}
    parsed: dict[str, float] = {}
    for group, raw_weight in entries:
        try:
            weight = float(raw_weight)
        except (TypeError, ValueError):
            continue
        if isinstance(group, str) and group and 0.0 < weight <= 100.0:
            parsed[group] = weight / 100.0
    return parsed


def exercise_metadata_from_room_row(row: Mapping[str, Any]) -> ExerciseMetadata | None:
    """Return only declared seed metadata; neither the display name nor Room ID is identity."""

    key = row.get("catalogueKey")
    logging_type = _logging_type(row.get("loggingType"))
    demands = _string_list(row.get("muscleGroups"))
    if row.get("isCustom") or not isinstance(key, str) or not key.startswith("timego.seed."):
        return None
    if logging_type is None or not demands:
        return None
    category = row.get("category")
    weights = _muscle_weights(row.get("muscleWeights"))
    return ExerciseMetadata(
        catalogue_key=key,
        logging_type=logging_type,
        demand_coordinates=tuple(sorted(demands)),
        equipment=(),
        demand_weights=tuple(sorted((demand, weights.get(demand, 1.0)) for demand in demands)),
        category=category if isinstance(category, str) else None,
        bodyweight_supported=category == "CALISTHENICS",
    )


def _target_provenance(value: Any) -> TargetProvenance:
    return (
        TargetProvenance.DECLARED_BEFORE_SET
        if value == "OVERLOAD_SUGGESTION"
        else TargetProvenance.UNKNOWN
    )


def raw_set_log_from_room_row(row: Mapping[str, Any], logging_type: LoggingType) -> RawSetLog:
    """Translate canonical fields without inferring missing effort, targets, or load."""

    return RawSetLog(
        session_id=int(row["sessionId"]),
        set_id=int(row["id"]),
        ended_at_ms=int(row["loggedAtEpochMillis"]),
        logging_type=logging_type,
        reps=row.get("reps"),
        target_reps=row.get("targetReps"),
        weight_kg=row.get("weightKg"),
        added_weight_kg=row.get("addedWeightKg"),
        hold_seconds=row.get("holdSeconds"),
        target_hold_seconds=row.get("targetHoldSeconds"),
        duration_minutes=row.get("durationMinutes"),
        distance_km=row.get("distanceKm"),
        rpe=row.get("rpe"),
        is_warmup=bool(row.get("isWarmup")),
        target_provenance=_target_provenance(row.get("targetProvenance")),
    )
