package com.lsing.timego.domain.adaptive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProvisionalContinuousReplayTest {

    @Test
    fun completeOrderedReplayEqualsItsOrderedIncrementalUpdates() {
        val initial = ShadowState(
            basis = ShadowBasis.REPS_ONLY,
            mean = listOf(0.0),
            variance = listOf(1.0),
            observedAtEpochMillis = null,
            personalBaselines = emptyMap(),
        )
        val observations = listOf(
            observation(1L, 1_000L, 5.0),
            observation(2L, 86_401_000L, 6.0),
            observation(3L, 172_801_000L, 4.0),
        )

        val incremental = observations.fold(initial) { state, observation ->
            ProvisionalContinuousReplay.update(state, observation, config()).state
        }
        val replay = ProvisionalContinuousReplay.replay(initial, observations, config())

        assertEquals(incremental, replay.state)
        assertEquals(3, replay.observationCount)
        assertEquals(2, replay.updateCount)
        assertEquals(1, replay.baselineRegistrationCount)
        assertEquals(emptyList<String>(), replay.abstentionReasons)
    }

    @Test
    fun replayCountsInvalidObservationAsAnAbstentionAndContinuesInInputOrder() {
        val initial = ShadowState(
            basis = ShadowBasis.REPS_ONLY,
            mean = listOf(0.0),
            variance = listOf(1.0),
            observedAtEpochMillis = null,
            personalBaselines = emptyMap(),
        )
        val replay = ProvisionalContinuousReplay.replay(
            initial,
            listOf(
                observation(1L, 1_000L, 3.0),
                observation(2L, 2_000L, Double.POSITIVE_INFINITY),
                observation(3L, 3_000L, 4.0),
            ),
            config(),
        )

        assertEquals(3, replay.observationCount)
        assertEquals(1, replay.updateCount)
        assertEquals(1, replay.baselineRegistrationCount)
        assertEquals(listOf("invalid_observation"), replay.abstentionReasons)
        assertEquals(mapOf("synthetic.task.row" to 3.0), replay.state.personalBaselines)
        assertFalse(replay.updates[1].updated)
    }

    @Test
    fun replaySortsTiesAndKeepsEverySameSessionPredictionFrozenUntilTheNextSession() {
        val initial = ShadowState.prior(ShadowBasis.REPS_ONLY, config())
        val replay = ProvisionalContinuousReplay.replay(
            initial,
            listOf(
                observation(sessionId = 9L, setId = 2L, endedAt = 1_000L, score = 7.0),
                observation(sessionId = 10L, setId = 1L, endedAt = 1_000L, score = 6.0),
                observation(sessionId = 9L, setId = 1L, endedAt = 1_000L, score = 5.0),
            ),
            config(),
        )

        assertEquals(1, replay.updateCount)
        assertEquals(2, replay.baselineRegistrationCount)
        assertEquals(mapOf("synthetic.task.row" to 5.0), replay.state.personalBaselines)
        assertEquals(0.9090909090909091, replay.state.mean.single(), 1e-12)
        assertFalse(replay.updates[0].updated)
        assertFalse(replay.updates[1].updated)
        assertEquals("registered_personal_baseline", replay.updates[0].abstentionReason)
        assertEquals("registered_personal_baseline", replay.updates[1].abstentionReason)
    }

    @Test
    fun sameSessionActualsUpdateSequentiallyUsingTheEvolvingResidualAndVariance() {
        val initial = ShadowState(
            basis = ShadowBasis.REPS_ONLY,
            mean = listOf(0.0),
            variance = listOf(1.0),
            observedAtEpochMillis = 900L,
            personalBaselines = mapOf(
                "synthetic.task.a" to 1.0,
                "synthetic.task.b" to 1.0,
            ),
        )
        val observations = listOf(
            observation(
                sessionId = 4L,
                setId = 1L,
                endedAt = 1_000L,
                score = 2.0,
                catalogueKey = "synthetic.task.a",
            ),
            observation(
                sessionId = 4L,
                setId = 2L,
                endedAt = 1_000L,
                score = 2.0,
                catalogueKey = "synthetic.task.b",
            ),
        )

        val replay = ProvisionalContinuousReplay.replay(initial, observations, config())

        assertEquals(0.9523809550054588, replay.state.mean.single(), 1e-12)
        assertEquals(0.04761904775027296, replay.state.variance.single(), 1e-12)
        assertEquals(2, replay.updateCount)
    }

    private fun config() = ShadowConfig(
        coordinateCount = 1,
        priorVariance = 1.0,
        processVariancePerDay = 0.05,
        observationVariance = 0.1,
    )

    private fun observation(
        setId: Long,
        endedAt: Long,
        score: Double,
        sessionId: Long = 1L,
        catalogueKey: String = "synthetic.task.row",
    ) = ShadowObservation(
        catalogueKey = catalogueKey,
        sessionId = sessionId,
        setId = setId,
        endedAtEpochMillis = endedAt,
        demandVector = listOf(1.0),
        basis = ShadowBasis.REPS_ONLY,
        observedWorkScore = score,
    )
}
