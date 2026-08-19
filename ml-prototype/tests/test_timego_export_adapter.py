from src.observation_contract import LoggingType, TargetProvenance
from src.timego_export_adapter import exercise_metadata_from_room_row, raw_set_log_from_room_row


def test_seeded_room_exercise_becomes_keyed_declarative_metadata_without_using_name_as_identity():
    metadata = exercise_metadata_from_room_row(
        {
            "id": 42,
            "name": "A mutable display label",
            "catalogueKey": "timego.seed.v1.pull-up",
            "muscleGroups": ["LATS", "UPPER_BACK", "BICEPS"],
            "muscleWeights": {"UPPER_BACK": 60, "BICEPS": 35},
            "isCustom": False,
            "category": "CALISTHENICS",
            "loggingType": "WEIGHT_REPS",
        },
    )

    assert metadata is not None
    assert metadata.catalogue_key == "timego.seed.v1.pull-up"
    assert metadata.demand_coordinates == ("BICEPS", "LATS", "UPPER_BACK")
    assert metadata.demand_weights == (("BICEPS", 0.35), ("LATS", 1.0), ("UPPER_BACK", 0.6))
    assert metadata.category == "CALISTHENICS"
    assert metadata.bodyweight_supported


def test_room_serialized_muscle_weights_are_normalised_without_using_the_display_name():
    metadata = exercise_metadata_from_room_row(
        {
            "catalogueKey": "timego.seed.v1.lat-pulldown",
            "muscleGroups": "LATS\x1fBICEPS",
            "muscleWeights": "BICEPS\x1f35",
            "isCustom": False,
            "category": "STRENGTH",
            "loggingType": "WEIGHT_REPS",
        },
    )

    assert metadata is not None
    assert metadata.demand_weights == (("BICEPS", 0.35), ("LATS", 1.0))


def test_custom_or_unkeyed_room_exercise_is_excluded_instead_of_guessed():
    assert exercise_metadata_from_room_row(
        {
            "catalogueKey": None,
            "muscleGroups": ["LATS"],
            "isCustom": True,
            "category": "CALISTHENICS",
            "loggingType": "WEIGHT_REPS",
        },
    ) is None


def test_future_suggested_target_maps_to_pre_set_provenance():
    raw = raw_set_log_from_room_row(
        {
            "sessionId": 1,
            "id": 2,
            "loggedAtEpochMillis": 3,
            "weightKg": 80.0,
            "reps": 8,
            "targetReps": 8,
            "targetProvenance": "OVERLOAD_SUGGESTION",
            "isWarmup": 0,
            "addedWeightKg": 5.0,
            "rpe": None,
        },
        LoggingType.WEIGHT_REPS,
    )

    assert raw.target_provenance == TargetProvenance.DECLARED_BEFORE_SET
    assert raw.weight_kg == 80.0
    assert raw.added_weight_kg == 5.0
