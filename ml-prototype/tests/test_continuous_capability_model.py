from math import log1p

from src.continuous_capability_model import (
    ContinuousCapabilityConfig,
    ContinuousCapabilityState,
    PerformanceObservation,
    bodyweight_reps_observed_work_score,
    hold_observed_work_score,
    update_from_performance_observation,
    weighted_rep_observed_work_score,
)


CONFIG = ContinuousCapabilityConfig(
    coordinate_count=1,
    prior_variance=1.0,
    process_variance_per_day=0.05,
    observation_variance=0.1,
)


def observation(*, key: str = "timego.seed.v1.pull-up", ended_at_ms: int, score: float) -> PerformanceObservation:
    return PerformanceObservation(
        catalogue_key=key,
        session_id=1,
        set_id=1,
        ended_at_ms=ended_at_ms,
        demand_vector=(1.0,),
        observed_work_score=score,
    )


def test_first_known_exercise_observation_sets_a_personal_baseline_without_claiming_progress():
    start = ContinuousCapabilityState.prior(CONFIG)

    result = update_from_performance_observation(start, observation(ended_at_ms=1_000, score=4.0), CONFIG)

    assert not result.updated
    assert result.reason == "registered_personal_baseline"
    assert result.state.mean == start.mean
    assert result.state.baseline_for("timego.seed.v1.pull-up") == 4.0


def test_later_observed_work_changes_update_matching_movement_evidence_in_both_directions():
    state = update_from_performance_observation(
        ContinuousCapabilityState.prior(CONFIG), observation(ended_at_ms=1_000, score=4.0), CONFIG
    ).state
    stronger = update_from_performance_observation(state, observation(ended_at_ms=2_000, score=5.0), CONFIG)
    weaker = update_from_performance_observation(stronger.state, observation(ended_at_ms=3_000, score=3.0), CONFIG)

    assert stronger.updated
    assert stronger.state.mean[0] > 0.0
    assert weaker.updated
    assert weaker.state.mean[0] < stronger.state.mean[0]


def test_a_new_exercise_registers_its_own_baseline_and_cannot_be_mistaken_for_transfer_evidence():
    state = update_from_performance_observation(
        ContinuousCapabilityState.prior(CONFIG), observation(ended_at_ms=1_000, score=4.0), CONFIG
    ).state

    result = update_from_performance_observation(
        state,
        observation(key="timego.seed.v1.push-up", ended_at_ms=2_000, score=9.0),
        CONFIG,
    )

    assert not result.updated
    assert result.state.mean == state.mean
    assert result.state.baseline_for("timego.seed.v1.push-up") == 9.0


def test_reps_only_and_loaded_work_for_one_exercise_cannot_be_mixed_into_one_measurement_scale():
    state = update_from_performance_observation(
        ContinuousCapabilityState.prior(CONFIG), observation(ended_at_ms=1_000, score=4.0), CONFIG
    ).state
    reps_only = PerformanceObservation(
        catalogue_key="timego.seed.v1.pull-up",
        session_id=2,
        set_id=2,
        ended_at_ms=2_000,
        demand_vector=(1.0,),
        observed_work_score=2.0,
        measurement_basis="reps_only",
    )

    result = update_from_performance_observation(state, reps_only, CONFIG)

    assert not result.updated
    assert result.reason == "registered_personal_baseline"
    assert result.state.mean == state.mean
    assert result.state.baseline_for("timego.seed.v1.pull-up", "reps_only") == 2.0


def test_observed_work_scores_are_monotonic_records_not_e1rm_estimates():
    assert weighted_rep_observed_work_score(effective_load_kg=80.0, reps=5) == log1p(400.0)
    assert bodyweight_reps_observed_work_score(reps=5) == log1p(5.0)
    assert hold_observed_work_score(hold_seconds=30.0) == log1p(30.0)
