"""Read-only aggregate audit for a local TimeGo Room database.

The runner deliberately emits counts and validation evidence only. It never exports, modifies, or
prints the user's individual workout rows.
"""

from __future__ import annotations

import argparse
from dataclasses import asdict, dataclass
import json
from pathlib import Path
import sqlite3
from collections import defaultdict

from src.observation_contract import HoldObservation, StaminaObservation, WeightedRepObservation, map_observation
from src.temporal_performance_audit import PerformanceSessionEvidence, run_weighted_envelope_audit
from src.timego_export_adapter import exercise_metadata_from_room_row, raw_set_log_from_room_row


@dataclass(frozen=True)
class LocalHistoryAudit:
    total_logs: int
    weighted_rep_observations: int
    hold_observations: int
    stamina_observations: int
    exclusions: tuple[tuple[str, int], ...]
    closed_weighted_sessions: int
    comparable_same_exercise_observations: int
    weighted_extension_rate: float | None
    trusted_target_outcomes: int


_ROWS_SQL = """
SELECT s.id, s.sessionId, s.loggedAtEpochMillis, s.reps, s.targetReps, s.weightKg,
       s.addedWeightKg, s.holdSeconds, s.targetHoldSeconds, s.durationMinutes,
       s.distanceKm, s.rpe, s.isWarmup, s.targetProvenance,
       e.catalogueKey, e.muscleGroups, e.muscleWeights, e.isCustom, e.category, e.loggingType,
       ws.endEpochMillis
FROM set_logs s
JOIN exercises e ON e.id = s.exerciseId
LEFT JOIN workout_sessions ws ON ws.id = s.sessionId
"""


def _read_only_connection(database_path: Path) -> sqlite3.Connection:
    uri = f"file:{database_path.resolve().as_posix()}?mode=ro"
    connection = sqlite3.connect(uri, uri=True)
    connection.row_factory = sqlite3.Row
    return connection


def audit_database(database_path: Path) -> LocalHistoryAudit:
    """Return deterministic, aggregate-only evidence from a schema-13 TimeGo database."""

    with _read_only_connection(database_path) as connection:
        rows = connection.execute(_ROWS_SQL).fetchall()

    exclusions: dict[str, int] = defaultdict(int)
    weighted: list[WeightedRepObservation] = []
    holds: list[HoldObservation] = []
    stamina: list[StaminaObservation] = []
    trusted_target_outcomes = 0
    closed_weighted_by_session: dict[int, list[WeightedRepObservation]] = defaultdict(list)
    session_end: dict[int, int] = {}

    for row in rows:
        payload = dict(row)
        metadata = exercise_metadata_from_room_row(payload)
        if metadata is None:
            exclusions["unkeyed_or_custom"] += 1
            continue
        observation = map_observation(raw_set_log_from_room_row(payload, metadata.logging_type), metadata)
        if isinstance(observation, WeightedRepObservation):
            weighted.append(observation)
            trusted_target_outcomes += int(observation.target_met is not None)
            session_end_value = payload.get("endEpochMillis")
            if isinstance(session_end_value, int) and session_end_value > 0:
                closed_weighted_by_session[observation.session_id].append(observation)
                session_end[observation.session_id] = session_end_value
        elif isinstance(observation, HoldObservation):
            holds.append(observation)
            trusted_target_outcomes += int(observation.target_met is not None)
        elif isinstance(observation, StaminaObservation):
            stamina.append(observation)
        else:
            exclusions[observation.reason] += 1

    closed_sessions = [
        PerformanceSessionEvidence(session_id, session_end[session_id], tuple(observations))
        for session_id, observations in closed_weighted_by_session.items()
    ]
    chronology = run_weighted_envelope_audit(closed_sessions)
    return LocalHistoryAudit(
        total_logs=len(rows),
        weighted_rep_observations=len(weighted),
        hold_observations=len(holds),
        stamina_observations=len(stamina),
        exclusions=tuple(sorted(exclusions.items())),
        closed_weighted_sessions=len(closed_sessions),
        comparable_same_exercise_observations=chronology.comparable_observations,
        weighted_extension_rate=chronology.extension_rate,
        trusted_target_outcomes=trusted_target_outcomes,
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Read-only aggregate TimeGo adaptive-coach audit")
    parser.add_argument("database", type=Path, help="Path to a local copied TimeGo Room database")
    args = parser.parse_args()
    print(json.dumps(asdict(audit_database(args.database)), sort_keys=True))


if __name__ == "__main__":
    main()
