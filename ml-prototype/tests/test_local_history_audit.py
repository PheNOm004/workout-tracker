import sqlite3

from src.local_history_audit import audit_database


def test_local_audit_reports_aggregate_contract_and_chronology_evidence(tmp_path):
    database_path = tmp_path / "timego.db"
    connection = sqlite3.connect(database_path)
    connection.executescript(
        """
        CREATE TABLE exercises (
            id INTEGER PRIMARY KEY,
            catalogueKey TEXT,
            muscleGroups TEXT NOT NULL,
            muscleWeights TEXT NOT NULL,
            isCustom INTEGER NOT NULL,
            category TEXT NOT NULL,
            loggingType TEXT NOT NULL
        );
        CREATE TABLE workout_sessions (
            id INTEGER PRIMARY KEY,
            endEpochMillis INTEGER
        );
        CREATE TABLE set_logs (
            id INTEGER PRIMARY KEY,
            sessionId INTEGER NOT NULL,
            exerciseId INTEGER NOT NULL,
            weightKg REAL NOT NULL,
            reps INTEGER NOT NULL,
            targetReps INTEGER NOT NULL,
            loggedAtEpochMillis INTEGER NOT NULL,
            durationMinutes REAL,
            distanceKm REAL,
            holdSeconds INTEGER,
            targetHoldSeconds INTEGER,
            isWarmup INTEGER NOT NULL,
            addedWeightKg REAL,
            rpe INTEGER,
            targetProvenance TEXT NOT NULL
        );
        """,
    )
    connection.execute(
        "INSERT INTO exercises VALUES (1, 'timego.seed.v1.pull-up', 'LATS\x1fBICEPS', 'BICEPS\x1f35', 0, 'CALISTHENICS', 'WEIGHT_REPS')",
    )
    connection.execute(
        "INSERT INTO exercises VALUES (2, 'timego.seed.v1.steady-run', 'FULL_BODY', '', 0, 'CARDIO', 'DURATION_DISTANCE')",
    )
    connection.execute("INSERT INTO workout_sessions VALUES (1, 1000)")
    connection.execute("INSERT INTO workout_sessions VALUES (2, 2000)")
    connection.execute("INSERT INTO set_logs VALUES (1, 1, 1, 70, 5, 5, 900, NULL, NULL, NULL, NULL, 0, NULL, NULL, 'UNKNOWN')")
    connection.execute("INSERT INTO set_logs VALUES (2, 2, 1, 70, 6, 6, 1900, NULL, NULL, NULL, NULL, 0, NULL, NULL, 'OVERLOAD_SUGGESTION')")
    connection.execute("INSERT INTO set_logs VALUES (3, 2, 2, 0, 0, 0, 1901, 20, NULL, NULL, NULL, 0, NULL, NULL, 'UNKNOWN')")
    connection.commit()
    connection.close()

    result = audit_database(database_path)

    assert result.total_logs == 3
    assert result.weighted_rep_observations == 2
    assert result.hold_observations == 0
    assert result.exclusions == (("duration_without_distance", 1),)
    assert result.closed_weighted_sessions == 2
    assert result.comparable_same_exercise_observations == 1
    assert result.weighted_extension_rate == 1.0
    assert result.trusted_target_outcomes == 1
