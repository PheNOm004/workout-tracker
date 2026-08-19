from src.capability_model import (
    BinaryOutcome,
    CapabilityModelConfig,
    CapabilityPosterior,
    assess_candidate,
    unseen_task_demand_prior,
    update_posterior,
)


CONFIG = CapabilityModelConfig(
    coordinate_count=2,
    prior_variance=1.0,
    process_variance_per_day=0.05,
    maximum_interval_width=0.45,
)


def outcome(*, ended_at_ms: int, met_target: bool | None, demand: float | None = 0.0):
    return BinaryOutcome(
        session_id=1,
        set_id=1,
        ended_at_ms=ended_at_ms,
        demand_vector=(1.0, 0.0),
        task_demand=demand,
        met_target=met_target,
    )


def test_success_and_miss_move_capability_in_opposite_directions():
    start = CapabilityPosterior.prior(CONFIG)
    after_success = update_posterior(start, outcome(ended_at_ms=1_000, met_target=True), CONFIG)
    after_miss = update_posterior(start, outcome(ended_at_ms=1_000, met_target=False), CONFIG)

    assert after_success.posterior.mean[0] > start.mean[0]
    assert after_miss.posterior.mean[0] < start.mean[0]
    assert after_success.updated and after_miss.updated


def test_long_time_gap_widens_uncertainty_without_assuming_strength_loss():
    start = CapabilityPosterior(mean=(0.2, 0.0), variance=(0.2, 0.2), observed_at_ms=0)
    result = update_posterior(
        start,
        outcome(ended_at_ms=10 * 86_400_000, met_target=None),
        CONFIG,
    )

    assert result.posterior.mean == start.mean
    assert result.posterior.variance[0] > start.variance[0]
    assert result.reason == "missing_target_outcome"


def test_missing_target_outcome_does_not_create_fake_learning_signal():
    start = CapabilityPosterior.prior(CONFIG)
    result = update_posterior(start, outcome(ended_at_ms=1_000, met_target=None), CONFIG)

    assert not result.updated
    assert result.posterior.mean == start.mean
    assert result.reason == "missing_target_outcome"


def test_unknown_candidate_demand_abstains_instead_of_ranking_it():
    assessment = assess_candidate(
        CapabilityPosterior.prior(CONFIG),
        demand_vector=(1.0, 0.0),
        task_demand=None,
        anchor_supported=False,
        config=CONFIG,
    )

    assert assessment.abstained
    assert assessment.reason == "unidentified_task_demand"


def test_confident_calibrated_candidate_has_a_probability_interval():
    posterior = CapabilityPosterior(mean=(2.0, 0.0), variance=(0.02, 0.02), observed_at_ms=1_000)
    assessment = assess_candidate(
        posterior,
        demand_vector=(1.0, 0.0),
        task_demand=0.0,
        anchor_supported=True,
        config=CONFIG,
    )

    assert not assessment.abstained
    assert assessment.lower_probability < assessment.probability < assessment.upper_probability
    assert assessment.lower_probability > 0.5


def test_unseen_task_prior_is_not_available_until_explicitly_configured():
    assert unseen_task_demand_prior(CONFIG) is None


def test_broad_unseen_task_prior_abstains_at_ordinary_capability():
    config = CapabilityModelConfig(
        coordinate_count=1,
        prior_variance=0.1,
        process_variance_per_day=0.01,
        maximum_interval_width=0.5,
        unseen_task_prior_variance=4.0,
    )
    posterior = CapabilityPosterior(mean=(0.0,), variance=(0.01,), observed_at_ms=1_000)
    prior = unseen_task_demand_prior(config)

    assessment = assess_candidate(
        posterior,
        demand_vector=(1.0,),
        task_demand=None,
        task_demand_prior=prior,
        anchor_supported=True,
        config=config,
    )

    assert assessment.abstained
    assert assessment.reason == "uncertainty_too_high"
    assert assessment.used_unseen_task_prior


def test_broad_unseen_task_prior_can_only_pass_when_its_entire_interval_is_safe():
    config = CapabilityModelConfig(
        coordinate_count=1,
        prior_variance=0.1,
        process_variance_per_day=0.01,
        maximum_interval_width=0.5,
        unseen_task_prior_variance=4.0,
    )
    posterior = CapabilityPosterior(mean=(10.0,), variance=(0.01,), observed_at_ms=1_000)
    prior = unseen_task_demand_prior(config)

    assessment = assess_candidate(
        posterior,
        demand_vector=(1.0,),
        task_demand=None,
        task_demand_prior=prior,
        anchor_supported=True,
        config=config,
    )

    assert not assessment.abstained
    assert assessment.used_unseen_task_prior
