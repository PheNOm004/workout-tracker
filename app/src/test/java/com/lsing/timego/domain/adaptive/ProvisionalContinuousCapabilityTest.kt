package com.lsing.timego.domain.adaptive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ProvisionalContinuousCapabilityTest {

    @Test
    fun firstSameTaskAndBasisObservationIsNeutralAndRegistersItsFirstScore() {
        val state = priorState(ShadowBasis.LOAD_REPS)

        val result = ProvisionalContinuousReplay.update(
            state = state,
            observation = observation(
                key = "synthetic.task.press",
                basis = ShadowBasis.LOAD_REPS,
                endedAt = 1_000L,
                score = 5.0,
            ),
            config = config(),
        )

        assertFalse(result.updated)
        assertEquals("registered_personal_baseline", result.abstentionReason)
        assertEquals(1_000L, result.state.observedAtEpochMillis)
        assertEquals(mapOf("synthetic.task.press" to 5.0), result.state.personalBaselines)
        assertEquals(listOf(0.0), result.state.mean)
        assertEquals(listOf(1.0), result.state.variance)
    }

    @Test
    fun laterStrongerAndWeakerEvidenceUpdateOnlyTheMatchingBasis() {
        val initial = priorState(ShadowBasis.LOAD_REPS)
        val baseline = ProvisionalContinuousReplay.update(
            initial,
            observation("synthetic.task.press", ShadowBasis.LOAD_REPS, 1_000L, 5.0),
            config(),
        ).state

        val stronger = ProvisionalContinuousReplay.update(
            baseline,
            observation("synthetic.task.press", ShadowBasis.LOAD_REPS, 1_000L, 6.0),
            config(),
        )
        val weaker = ProvisionalContinuousReplay.update(
            stronger.state,
            observation("synthetic.task.press", ShadowBasis.LOAD_REPS, 1_000L, 4.0),
            config(),
        )
        val mismatchedBasis = ProvisionalContinuousReplay.update(
            weaker.state,
            observation("synthetic.task.press", ShadowBasis.REPS_ONLY, 2_000L, 7.0),
            config(),
        )

        assertTrue(stronger.updated)
        assertEquals(0.9090909090909091, stronger.state.mean.single(), 1e-12)
        assertTrue(weaker.updated)
        assertEquals(0.0, weaker.state.mean.single(), 1e-12)
        assertFalse(mismatchedBasis.updated)
        assertEquals("basis_mismatch", mismatchedBasis.abstentionReason)
        assertEquals(weaker.state, mismatchedBasis.state)
        assertEquals(mapOf("synthetic.task.press" to 5.0), mismatchedBasis.state.personalBaselines)
    }

    @Test
    fun invalidObservationsAreDeterministicallyRejectedWithoutChangingState() {
        val state = priorState(ShadowBasis.HOLD_SECONDS)
        val invalid = listOf(
            observation("", ShadowBasis.HOLD_SECONDS, 1_000L, 2.0),
            observation("synthetic.task.hold", ShadowBasis.HOLD_SECONDS, 0L, 2.0),
            observation("synthetic.task.hold", ShadowBasis.HOLD_SECONDS, 1_000L, Double.NaN),
            ShadowObservation(
                catalogueKey = "synthetic.task.hold",
                sessionId = 1L,
                setId = 2L,
                endedAtEpochMillis = 1_000L,
                demandVector = emptyList(),
                basis = ShadowBasis.HOLD_SECONDS,
                observedWorkScore = 2.0,
            ),
        )

        invalid.forEach { observation ->
            val result = ProvisionalContinuousReplay.update(state, observation, config())

            assertFalse(result.updated)
            assertEquals("invalid_observation", result.abstentionReason)
            assertEquals(state, result.state)
        }
    }

    @Test
    fun domainValuesDefensivelyCopyCallerCollectionsAndExposeUnmodifiableSnapshots() {
        val sourceMean = arrayListOf(0.0)
        val sourceVariance = arrayListOf(1.0)
        val sourceBaselines = linkedMapOf("synthetic.task.press" to 5.0)
        val sourceDemand = arrayListOf(1.0)
        val state = ShadowState(
            basis = ShadowBasis.LOAD_REPS,
            mean = sourceMean,
            variance = sourceVariance,
            observedAtEpochMillis = 1_000L,
            personalBaselines = sourceBaselines,
        )
        val observation = ShadowObservation(
            catalogueKey = "synthetic.task.press",
            sessionId = 1L,
            setId = 2L,
            endedAtEpochMillis = 2_000L,
            demandVector = sourceDemand,
            basis = ShadowBasis.LOAD_REPS,
            observedWorkScore = 6.0,
        )
        val sourceUpdates = arrayListOf(ShadowUpdate(state, updated = true, abstentionReason = null))
        val sourceReasons = arrayListOf("invalid_observation")
        val replay = ShadowReplay(state, sourceUpdates, 1, 1, 0, sourceReasons)

        sourceMean[0] = 99.0
        sourceVariance[0] = 99.0
        sourceBaselines["synthetic.task.press"] = 99.0
        sourceDemand[0] = 99.0
        sourceUpdates.clear()
        sourceReasons.clear()

        assertEquals(listOf(0.0), state.mean)
        assertEquals(listOf(1.0), state.variance)
        assertEquals(mapOf("synthetic.task.press" to 5.0), state.personalBaselines)
        assertEquals(listOf(1.0), observation.demandVector)
        assertEquals(1, replay.updates.size)
        assertEquals(listOf("invalid_observation"), replay.abstentionReasons)
        assertUnmodifiable { (state.mean as MutableList<Double>)[0] = 3.0 }
        assertUnmodifiable { (state.personalBaselines as MutableMap<String, Double>)["new"] = 1.0 }
        assertUnmodifiable { (observation.demandVector as MutableList<Double>)[0] = 3.0 }
        assertUnmodifiable { (replay.updates as MutableList<ShadowUpdate>).clear() }
    }

    @Test
    fun priorFactoryUsesTheConfiguredFinitePriorVariance() {
        val state = ShadowState.prior(
            basis = ShadowBasis.HOLD_SECONDS,
            config = ShadowConfig(2, 3.5, 0.05, 0.1),
        )

        assertEquals(listOf(0.0, 0.0), state.mean)
        assertEquals(listOf(3.5, 3.5), state.variance)
        assertEquals(null, state.observedAtEpochMillis)
        assertEquals(emptyMap<String, Double>(), state.personalBaselines)
    }

    @Test
    fun overflowingElapsedVarianceAbstainsWithoutEmittingAnInfiniteState() {
        val config = ShadowConfig(
            coordinateCount = 1,
            priorVariance = 1.0,
            processVariancePerDay = Double.MAX_VALUE,
            observationVariance = 0.1,
        )
        val registered = ProvisionalContinuousReplay.update(
            ShadowState.prior(ShadowBasis.HOLD_SECONDS, config),
            observation("synthetic.task.hold", ShadowBasis.HOLD_SECONDS, 1L, 3.0),
            config,
        ).state

        val overflow = ProvisionalContinuousReplay.update(
            registered,
            observation("synthetic.task.other-hold", ShadowBasis.HOLD_SECONDS, 172_800_001L, 3.0),
            config,
        )

        assertFalse(overflow.updated)
        assertEquals("invalid_observation", overflow.abstentionReason)
        assertEquals(registered, overflow.state)
        assertTrue(overflow.state.variance.all(Double::isFinite))
    }

    private fun config() = ShadowConfig(
        coordinateCount = 1,
        priorVariance = 1.0,
        processVariancePerDay = 0.05,
        observationVariance = 0.1,
    )

    private fun priorState(basis: ShadowBasis) = ShadowState(
        basis = basis,
        mean = listOf(0.0),
        variance = listOf(1.0),
        observedAtEpochMillis = null,
        personalBaselines = emptyMap(),
    )

    private fun observation(
        key: String,
        basis: ShadowBasis,
        endedAt: Long,
        score: Double,
    ) = ShadowObservation(
        catalogueKey = key,
        sessionId = 1L,
        setId = 2L,
        endedAtEpochMillis = endedAt,
        demandVector = listOf(1.0),
        basis = basis,
        observedWorkScore = score,
    )

    private fun assertUnmodifiable(mutation: () -> Unit) {
        try {
            mutation()
            fail("Expected immutable collection mutation to fail")
        } catch (_: UnsupportedOperationException) {
            // Expected: domain values expose snapshots, never mutable caller backing collections.
        }
    }
}
