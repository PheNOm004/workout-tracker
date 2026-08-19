package com.lsing.timego.data.adaptive

import com.lsing.timego.domain.adaptive.ProvisionalContinuousReplay
import com.lsing.timego.domain.adaptive.ShadowBasis
import com.lsing.timego.domain.adaptive.ShadowConfig
import com.lsing.timego.domain.adaptive.ShadowObservation
import com.lsing.timego.domain.adaptive.ShadowState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ShadowCacheLifecycleTest {
    @Test
    fun staleCapturedSourceIsDiscardedEvenWhenVersionsMatch() {
        val captured = key(source = "source-before-delete")

        val decision = ShadowCacheWritePolicy.decide(
            captured = captured,
            currentSourceFingerprint = "source-after-delete",
            existing = null,
        )

        assertEquals(ShadowCacheWriteDisposition.STALE_DISCARDED, decision.disposition)
        assertEquals("source-after-delete", decision.rebuildFromSourceFingerprint)
    }

    @Test
    fun modelOrMetadataChangeMakesPersistedCacheUnusable() {
        val persisted = key(source = "source-a")

        assertFalse(ShadowCacheCompatibility.isUsable(persisted, key(source = "source-a", model = "contract-v2")))
        assertFalse(ShadowCacheCompatibility.isUsable(persisted, key(source = "source-a", metadata = "metadata-v2")))
    }

    @Test
    fun rebuildPayloadEqualsOrderedIncrementalPayload() {
        val config = ShadowConfig(1, 1.0, 0.05, 0.1)
        val first = observation(sessionId = 10, setId = 11, endedAt = 1_000, score = 2.0)
        val later = observation(sessionId = 20, setId = 21, endedAt = 2_000, score = 2.5)
        val prior = ShadowState.prior(ShadowBasis.REPS_ONLY, config)

        val rebuilt = ProvisionalContinuousReplay.replay(prior, listOf(first, later), config).state
        val incrementallyUpdated = ProvisionalContinuousReplay.update(
            ProvisionalContinuousReplay.update(prior, first, config).state,
            later,
            config,
        ).state

        assertEquals(
            ShadowDerivedState.encode(listOf(rebuilt)),
            ShadowDerivedState.encode(listOf(incrementallyUpdated)),
        )
    }

    private fun key(
        source: String,
        model: String = "contract-v1",
        metadata: String = "metadata-v1",
    ) = ShadowCacheKey(
        sourceFingerprint = source,
        modelContractHash = model,
        metadataHash = metadata,
        orderingPolicy = "end-session-set-key-basis-v1",
    )

    private fun observation(sessionId: Long, setId: Long, endedAt: Long, score: Double) = ShadowObservation(
        catalogueKey = "timego.seed.v1.synthetic",
        sessionId = sessionId,
        setId = setId,
        endedAtEpochMillis = endedAt,
        demandVector = listOf(1.0),
        basis = ShadowBasis.REPS_ONLY,
        observedWorkScore = score,
    )
}
