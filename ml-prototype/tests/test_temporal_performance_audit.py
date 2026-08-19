from src.observation_contract import WeightedRepObservation
from src.temporal_performance_audit import PerformanceSessionEvidence, run_weighted_envelope_audit


def point(session_id: int, set_id: int, ended_at_ms: int, load: float, reps: int) -> WeightedRepObservation:
    return WeightedRepObservation(
        catalogue_key="timego.seed.v1.pull-up",
        session_id=session_id,
        set_id=set_id,
        ended_at_ms=ended_at_ms,
        reps=reps,
        target_reps=None,
        target_met=None,
        effective_load_kg=load,
        bodyweight_kg=None,
        rpe=None,
    )


def session(session_id: int, ended_at_ms: int, *observations: WeightedRepObservation) -> PerformanceSessionEvidence:
    return PerformanceSessionEvidence(session_id, ended_at_ms, observations)


def test_audit_uses_only_earlier_closed_sessions_and_marks_envelope_extensions():
    result = run_weighted_envelope_audit(
        [
            session(1, 1_000, point(1, 1, 900, 60.0, 8)),
            session(2, 2_000, point(2, 2, 1_900, 55.0, 8), point(2, 3, 1_901, 70.0, 5)),
        ],
    )

    assert [(event.test_set_id, event.training_session_ids, event.extends_prior_envelope) for event in result.events] == [
        (2, (1,), False),
        (3, (1,), True),
    ]
    assert result.comparable_observations == 2
    assert result.extension_rate == 0.5


def test_later_sessions_cannot_change_an_earlier_envelope_audit_event():
    first_two = [
        session(1, 1_000, point(1, 1, 900, 60.0, 8)),
        session(2, 2_000, point(2, 2, 1_900, 55.0, 8)),
    ]
    third = session(3, 3_000, point(3, 3, 2_900, 100.0, 10))

    before = run_weighted_envelope_audit(first_two).events[0]
    after = run_weighted_envelope_audit(first_two + [third]).events[0]

    assert before == after


def test_first_seen_exercise_is_unobserved_not_counted_as_an_extension_or_failure():
    result = run_weighted_envelope_audit(
        [
            session(1, 1_000, point(1, 1, 900, 60.0, 8)),
            session(
                2,
                2_000,
                WeightedRepObservation("timego.seed.v1.push-up", 2, 2, 1_900, 10, None, None, 70.0, None, None),
            ),
        ],
    )

    assert result.events[0].extends_prior_envelope is None
    assert result.comparable_observations == 0
    assert result.extension_rate is None
