from math import isclose

from src.continuous_capability_model import ContinuousCapabilityConfig, PerformanceObservation
from src.continuous_chronological_evaluator import (
    ContinuousEvaluationSession,
    run_continuous_chronological_evaluation,
)


def config(*, observation_variance: float = 0.5) -> ContinuousCapabilityConfig:
    return ContinuousCapabilityConfig(
        coordinate_count=1,
        prior_variance=1.0,
        process_variance_per_day=0.0,
        observation_variance=observation_variance,
    )


def observation(
    session_id: int,
    ended_at_ms: int,
    score: float,
    *,
    catalogue_key: str = "timego.seed.v1.pull-up",
    measurement_basis: str = "load_reps",
) -> PerformanceObservation:
    return PerformanceObservation(
        catalogue_key=catalogue_key,
        session_id=session_id,
        set_id=session_id,
        ended_at_ms=ended_at_ms,
        demand_vector=(1.0,),
        observed_work_score=score,
        measurement_basis=measurement_basis,
    )


def session(
    session_id: int,
    ended_at_ms: int,
    *observations: PerformanceObservation,
) -> ContinuousEvaluationSession:
    return ContinuousEvaluationSession(session_id, ended_at_ms, observations)


def test_later_session_does_not_leak_into_an_earlier_prediction_boundary():
    first_two = [
        session(1, 1_000, observation(1, 1_000, 1.0)),
        session(2, 2_000, observation(2, 2_000, 3.0)),
    ]
    later = session(3, 3_000, observation(3, 3_000, 5.0))

    result = run_continuous_chronological_evaluation(first_two + [later], config())

    # First boundary error is 2.0.  The second uses only sessions one and two:
    # candidate prediction 7/3, last-observation prediction 3, actual 5.
    assert result.prediction_boundaries == 2
    assert isclose(result.candidate_mae, (2.0 + (5.0 - (1.0 + 4.0 / 3.0))) / 2.0)
    assert isclose(result.baseline_mae, 2.0)


def test_candidate_and_last_observation_metrics_are_reported_for_repeated_exact_task():
    result = run_continuous_chronological_evaluation(
        [
            session(1, 1_000, observation(1, 1_000, 1.0)),
            session(2, 2_000, observation(2, 2_000, 3.0)),
            session(3, 3_000, observation(3, 3_000, 2.4)),
        ],
        config(observation_variance=0.01),
    )

    assert result.prediction_boundaries == 2
    assert result.candidate_mae is not None
    assert result.baseline_mae is not None
    assert result.candidate_rmse is not None
    assert result.baseline_rmse is not None
    assert result.candidate_mae < result.baseline_mae
    assert result.winner == "continuous_capability"
    assert not result.insufficient_evidence


def test_measurement_bases_never_share_a_baseline_or_last_observation():
    result = run_continuous_chronological_evaluation(
        [
            session(1, 1_000, observation(1, 1_000, 1.0, measurement_basis="load_reps")),
            session(2, 2_000, observation(2, 2_000, 100.0, measurement_basis="reps_only")),
            session(3, 3_000, observation(3, 3_000, 3.0, measurement_basis="load_reps")),
        ],
        config(),
    )

    assert result.first_observation_registrations == 2
    assert result.prediction_boundaries == 1
    assert isclose(result.candidate_mae, 2.0)
    assert isclose(result.baseline_mae, 2.0)


def test_repeated_updates_in_one_basis_cannot_change_another_basis_prediction():
    without_reps_only_update = run_continuous_chronological_evaluation(
        [
            session(1, 1_000, observation(1, 1_000, 1.0, measurement_basis="load_reps")),
            session(2, 2_000, observation(2, 2_000, 1.0, measurement_basis="reps_only")),
            session(3, 3_000, observation(3, 3_000, 3.0, measurement_basis="load_reps")),
        ],
        config(),
    )
    with_reps_only_update = run_continuous_chronological_evaluation(
        [
            session(1, 1_000, observation(1, 1_000, 1.0, measurement_basis="load_reps")),
            session(2, 2_000, observation(2, 2_000, 1.0, measurement_basis="reps_only")),
            session(3, 3_000, observation(3, 3_000, 3.0, measurement_basis="reps_only")),
            session(4, 4_000, observation(4, 4_000, 3.0, measurement_basis="load_reps")),
        ],
        config(),
    )

    # The reps-only update has a non-zero state change, but the later load/reps prediction still
    # uses its untouched load/reps state.  Both evaluation runs therefore retain MAE 2.0.
    assert isclose(without_reps_only_update.candidate_mae, 2.0)
    assert isclose(with_reps_only_update.candidate_mae, 2.0)
    assert isclose(with_reps_only_update.baseline_mae, 2.0)


def test_no_repeated_exercise_basis_boundary_reports_insufficient_evidence_not_a_winner():
    result = run_continuous_chronological_evaluation(
        [
            session(1, 1_000, observation(1, 1_000, 1.0)),
            session(2, 2_000, observation(2, 2_000, 2.0, catalogue_key="timego.seed.v1.push-up")),
        ],
        config(),
    )

    assert result.prediction_boundaries == 0
    assert result.insufficient_evidence
    assert result.winner is None
    assert result.candidate_mae is None
    assert result.baseline_mae is None
    assert result.insufficient_evidence_observations == 2
