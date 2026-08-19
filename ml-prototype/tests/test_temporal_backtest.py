from src.capability_model import BinaryOutcome, CapabilityModelConfig
from src.temporal_backtest import (
    SessionEvidence,
    chronological_splits,
    ewma_probability,
    last_observation_probability,
    run_capability_backtest,
)


CONFIG = CapabilityModelConfig(
    coordinate_count=1,
    prior_variance=0.1,
    process_variance_per_day=0.01,
    maximum_interval_width=1.0,
)


def session(session_id: int, ended_at_ms: int, met_target: bool) -> SessionEvidence:
    return SessionEvidence(
        session_id=session_id,
        ended_at_ms=ended_at_ms,
        outcomes=(
            BinaryOutcome(
                session_id=session_id,
                set_id=1,
                ended_at_ms=ended_at_ms,
                demand_vector=(1.0,),
                task_demand=0.0,
                met_target=met_target,
            ),
        ),
    )


def test_splits_are_deterministic_by_session_end_then_session_id():
    sessions = [session(3, 3000, True), session(2, 1000, True), session(1, 1000, False)]

    splits = chronological_splits(sessions)

    assert [(split.training_session_ids, split.test_session_id) for split in splits] == [
        ((1,), 2),
        ((1, 2), 3),
    ]


def test_backtest_predicts_a_closed_session_from_earlier_sessions_only():
    sessions = [session(1, 1000, False), session(2, 2000, True), session(3, 3000, True)]

    result = run_capability_backtest(sessions, CONFIG)

    assert len(result.predictions) == 2
    assert result.predictions[0].training_session_ids == (1,)
    assert result.predictions[0].test_session_id == 2
    assert result.predictions[1].training_session_ids == (1, 2)
    assert result.evaluable_predictions == 2
    assert result.last_observation_brier is not None
    assert result.ewma_brier is not None


def test_later_sessions_do_not_change_an_earlier_prediction():
    first_two = [session(1, 1000, False), session(2, 2000, True)]
    all_three = first_two + [session(3, 3000, True)]

    early = run_capability_backtest(first_two, CONFIG).predictions[0]
    extended = run_capability_backtest(all_three, CONFIG).predictions[0]

    assert early.probability == extended.probability
    assert early.training_session_ids == extended.training_session_ids


def test_unknown_demand_is_counted_as_abstention_not_as_an_error_or_success():
    unidentifiable = SessionEvidence(
        session_id=2,
        ended_at_ms=2000,
        outcomes=(
            BinaryOutcome(
                session_id=2,
                set_id=1,
                ended_at_ms=2000,
                demand_vector=(1.0,),
                task_demand=None,
                met_target=True,
            ),
        ),
    )

    result = run_capability_backtest([session(1, 1000, True), unidentifiable], CONFIG)

    assert result.evaluable_predictions == 0
    assert result.abstentions == 1


def test_simple_history_baselines_use_only_matching_past_target_outcomes():
    history = [
        session(1, 1000, False).outcomes[0],
        session(2, 2000, True).outcomes[0],
        BinaryOutcome(
            session_id=3,
            set_id=1,
            ended_at_ms=3000,
            demand_vector=(0.0,),
            task_demand=0.0,
            met_target=True,
        ),
    ]
    target = session(4, 4000, True).outcomes[0]

    assert last_observation_probability(history, target) == 1.0
    assert ewma_probability(history, target, alpha=0.5) == 0.5
