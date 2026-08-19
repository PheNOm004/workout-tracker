from src.observation_contract import HoldObservation, WeightedRepObservation
from src.performance_envelope import hold_summary, weighted_rep_summary


def weighted(*, set_id: int, ended_at_ms: int, load: float, reps: int) -> WeightedRepObservation:
    return WeightedRepObservation(
        catalogue_key="timego.seed.v1.pull-up",
        session_id=1,
        set_id=set_id,
        ended_at_ms=ended_at_ms,
        reps=reps,
        target_reps=None,
        target_met=None,
        effective_load_kg=load,
        bodyweight_kg=None,
        rpe=None,
    )


def test_weighted_rep_summary_keeps_only_non_dominated_observed_performance():
    summary = weighted_rep_summary(
        [
            weighted(set_id=1, ended_at_ms=1_000, load=60.0, reps=8),
            weighted(set_id=2, ended_at_ms=2_000, load=55.0, reps=8),
            weighted(set_id=3, ended_at_ms=3_000, load=70.0, reps=5),
            weighted(set_id=4, ended_at_ms=4_000, load=60.0, reps=10),
        ],
    )

    assert [(point.load_kg, point.reps) for point in summary.non_dominated_points] == [
        (60.0, 10),
        (70.0, 5),
    ]
    assert summary.latest_ended_at_ms == 4_000


def test_weighted_rep_summary_does_not_turn_reps_and_load_into_an_unearned_max_estimate():
    summary = weighted_rep_summary([weighted(set_id=1, ended_at_ms=1_000, load=60.0, reps=12)])

    assert summary.non_dominated_points[0].load_kg == 60.0
    assert summary.non_dominated_points[0].reps == 12
    assert not hasattr(summary, "estimated_one_rep_max")


def test_hold_summary_keeps_the_longest_observed_duration_and_its_timestamp():
    observations = [
        HoldObservation("timego.seed.v1.plank", 1, 1, 1_000, 30.0, None, None, None, None),
        HoldObservation("timego.seed.v1.plank", 1, 2, 2_000, 20.0, None, None, None, None),
        HoldObservation("timego.seed.v1.plank", 1, 3, 3_000, 45.0, None, None, None, None),
    ]

    summary = hold_summary(observations)

    assert summary.longest_seconds == 45.0
    assert summary.longest_ended_at_ms == 3_000
    assert summary.latest_ended_at_ms == 3_000
