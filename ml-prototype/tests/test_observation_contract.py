from dataclasses import replace

from src.observation_contract import (
    ExerciseMetadata,
    ExcludedObservation,
    HoldObservation,
    LoggingType,
    RawSetLog,
    StaminaObservation,
    WeightedRepObservation,
    catalogue_fingerprint,
    map_observation,
    missing_measurement_anchors,
)


WEIGHTED = ExerciseMetadata(
    catalogue_key="barbell-horizontal-press-v1",
    logging_type=LoggingType.WEIGHT_REPS,
    demand_coordinates=("horizontal_push", "loaded"),
    equipment=("barbell",),
)
HOLD = ExerciseMetadata(
    catalogue_key="hanging-support-v1",
    logging_type=LoggingType.HOLD,
    demand_coordinates=("vertical_pull", "grip"),
    equipment=("bar",),
    bodyweight_supported=True,
)
STAMINA = ExerciseMetadata(
    catalogue_key="steady-run-v1",
    logging_type=LoggingType.DURATION_DISTANCE,
    demand_coordinates=("locomotion", "aerobic"),
    equipment=(),
)


def test_weighted_rep_log_becomes_a_modality_specific_observation():
    result = map_observation(
        RawSetLog(
            session_id=7,
            set_id=11,
            ended_at_ms=1000,
            logging_type=LoggingType.WEIGHT_REPS,
            reps=8,
            target_reps=8,
            weight_kg=60.0,
            bodyweight_kg=80.0,
        ),
        WEIGHTED,
    )

    assert isinstance(result, WeightedRepObservation)
    assert result.effective_load_kg == 60.0
    assert result.target_met is None


def test_calisthenics_weight_uses_the_already_total_persisted_load_once():
    bodyweight_weighted = ExerciseMetadata(
        catalogue_key="weighted-pull-up-v1",
        logging_type=LoggingType.WEIGHT_REPS,
        demand_coordinates=("vertical_pull",),
        equipment=("bar",),
        bodyweight_supported=True,
    )

    result = map_observation(
        RawSetLog(
            session_id=7,
            set_id=17,
            ended_at_ms=1000,
            logging_type=LoggingType.WEIGHT_REPS,
            reps=5,
            weight_kg=85.0,
            added_weight_kg=5.0,
            bodyweight_kg=80.0,
        ),
        bodyweight_weighted,
    )

    assert isinstance(result, WeightedRepObservation)
    assert result.effective_load_kg == 85.0


def test_warmup_is_explicitly_excluded_from_capability_evidence():
    result = map_observation(
        RawSetLog(
            session_id=7,
            set_id=12,
            ended_at_ms=1000,
            logging_type=LoggingType.WEIGHT_REPS,
            reps=8,
            weight_kg=60.0,
            is_warmup=True,
        ),
        WEIGHTED,
    )

    assert result == ExcludedObservation(reason="warmup")


def test_hold_uses_hold_seconds_and_preserves_unknown_target():
    result = map_observation(
        RawSetLog(
            session_id=7,
            set_id=13,
            ended_at_ms=1000,
            logging_type=LoggingType.HOLD,
            hold_seconds=25.0,
            bodyweight_kg=80.0,
        ),
        HOLD,
    )

    assert isinstance(result, HoldObservation)
    assert result.target_met is None
    assert result.bodyweight_kg == 80.0


def test_duration_without_distance_is_not_stamina_capability_evidence():
    result = map_observation(
        RawSetLog(
            session_id=7,
            set_id=14,
            ended_at_ms=1000,
            logging_type=LoggingType.DURATION_DISTANCE,
            duration_minutes=30.0,
        ),
        STAMINA,
    )

    assert result == ExcludedObservation(reason="duration_without_distance")


def test_duration_with_distance_maps_to_stamina_observation():
    result = map_observation(
        RawSetLog(
            session_id=7,
            set_id=15,
            ended_at_ms=1000,
            logging_type=LoggingType.DURATION_DISTANCE,
            duration_minutes=30.0,
            distance_km=5.0,
        ),
        STAMINA,
    )

    assert isinstance(result, StaminaObservation)
    assert result.pace_minutes_per_km == 6.0


def test_mismatched_catalogue_modality_is_excluded_not_coerced():
    result = map_observation(
        RawSetLog(
            session_id=7,
            set_id=16,
            ended_at_ms=1000,
            logging_type=LoggingType.HOLD,
            hold_seconds=20.0,
        ),
        WEIGHTED,
    )

    assert result == ExcludedObservation(reason="logging_type_mismatch")


def test_catalogue_fingerprint_is_stable_and_does_not_depend_on_display_name_or_order():
    reordered = ExerciseMetadata(
        catalogue_key=WEIGHTED.catalogue_key,
        logging_type=WEIGHTED.logging_type,
        demand_coordinates=tuple(reversed(WEIGHTED.demand_coordinates)),
        equipment=tuple(reversed(WEIGHTED.equipment)),
    )

    assert catalogue_fingerprint([WEIGHTED, HOLD]) == catalogue_fingerprint([HOLD, WEIGHTED])
    assert catalogue_fingerprint([WEIGHTED]) == catalogue_fingerprint([reordered])


def test_anchor_check_requires_repeated_measurement_for_each_demand_vector():
    lone_anchor = ExerciseMetadata(
        catalogue_key="incline-press-v1",
        logging_type=LoggingType.WEIGHT_REPS,
        demand_coordinates=("incline_push", "loaded"),
        equipment=("barbell",),
    )
    repeated_anchor = replace(lone_anchor, catalogue_key="incline-press-dumbbell-v1")

    assert missing_measurement_anchors([WEIGHTED, HOLD, lone_anchor]) == {
        ("horizontal_push", "loaded"),
        ("grip", "vertical_pull"),
        ("incline_push", "loaded"),
    }
    assert missing_measurement_anchors([lone_anchor, repeated_anchor]) == set()
