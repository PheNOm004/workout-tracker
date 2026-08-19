from math import log1p

from src.continuous_capability_model import PerformanceObservation
from src.continuous_observation_mapper import (
    TIMEGO_MUSCLE_COORDINATES,
    session_hold_performance_observations,
    session_weighted_rep_performance_observations,
)
from src.observation_contract import ExerciseMetadata, HoldObservation, LoggingType, WeightedRepObservation


PULL_UP = ExerciseMetadata(
    catalogue_key="timego.seed.v1.pull-up",
    logging_type=LoggingType.WEIGHT_REPS,
    demand_coordinates=("LATS", "BICEPS"),
    demand_weights=(("BICEPS", 0.35), ("LATS", 1.0)),
    equipment=("pull_up_bar",),
    category="CALISTHENICS",
    bodyweight_supported=True,
)


def weighted(set_id: int, load: float, reps: int) -> WeightedRepObservation:
    return WeightedRepObservation(
        catalogue_key=PULL_UP.catalogue_key,
        session_id=3,
        set_id=set_id,
        ended_at_ms=1_000 + set_id,
        reps=reps,
        target_reps=None,
        target_met=None,
        effective_load_kg=load,
        bodyweight_kg=None,
        rpe=None,
    )


def test_weighted_sets_become_one_deterministic_completed_session_observation():
    result = session_weighted_rep_performance_observations(
        [weighted(1, 70.0, 5), weighted(2, 60.0, 8)],
        {PULL_UP.catalogue_key: PULL_UP},
        session_end_ms=2_000,
    )

    assert result == (
        PerformanceObservation(
            catalogue_key=PULL_UP.catalogue_key,
            session_id=3,
            set_id=2,
            ended_at_ms=2_000,
            demand_vector=tuple(
                0.35 if coordinate == "BICEPS" else 1.0 if coordinate == "LATS" else 0.0
                for coordinate in TIMEGO_MUSCLE_COORDINATES
            ),
            observed_work_score=log1p(480.0),
        ),
    )


def test_hold_sets_are_summarised_separately_by_longest_observed_duration():
    metadata = ExerciseMetadata(
        catalogue_key="timego.seed.v1.plank",
        logging_type=LoggingType.HOLD,
        demand_coordinates=("ABS",),
        demand_weights=(("ABS", 1.0),),
        equipment=(),
        category="CALISTHENICS",
        bodyweight_supported=True,
    )
    observations = [
        HoldObservation(metadata.catalogue_key, 3, 1, 1001, 30.0, None, None, None, None),
        HoldObservation(metadata.catalogue_key, 3, 2, 1002, 45.0, None, None, None, None),
    ]

    result = session_hold_performance_observations(
        observations,
        {metadata.catalogue_key: metadata},
        session_end_ms=2_000,
    )

    assert result[0].set_id == 2
    assert result[0].observed_work_score == log1p(45.0)


def test_unloaded_bodyweight_reps_keep_a_separate_measurement_basis():
    result = session_weighted_rep_performance_observations(
        [weighted(1, 0.0, 8)],
        {PULL_UP.catalogue_key: PULL_UP},
        session_end_ms=2_000,
    )

    assert result[0].measurement_basis == "reps_only"
    assert result[0].observed_work_score == log1p(8.0)


def test_unmapped_coordinates_are_rejected_instead_of_silently_dropped():
    invalid = ExerciseMetadata(
        catalogue_key=PULL_UP.catalogue_key,
        logging_type=LoggingType.WEIGHT_REPS,
        demand_coordinates=("UNMAPPED",),
        demand_weights=(("UNMAPPED", 1.0),),
        equipment=(),
    )

    try:
        session_weighted_rep_performance_observations([weighted(1, 70.0, 5)], {invalid.catalogue_key: invalid}, 2_000)
    except ValueError as error:
        assert "unknown coach coordinate" in str(error)
    else:
        raise AssertionError("expected unknown coordinate rejection")
