package com.lsing.timego.data.adaptive

import com.lsing.timego.domain.adaptive.ProvisionalContinuousReplay
import com.lsing.timego.domain.adaptive.ShadowBasis
import com.lsing.timego.domain.adaptive.ShadowConfig
import com.lsing.timego.domain.adaptive.ShadowState

/** Model/metadata/order identity requested by the hidden shadow runtime. */
data class ShadowCacheIdentity(
    val modelContractHash: String,
    val metadataHash: String,
    val orderingPolicy: String = ORDERING_POLICY_V1,
) {
    init {
        require(modelContractHash.isNotBlank()) { "A model contract hash is required" }
        require(metadataHash.isNotBlank()) { "A metadata hash is required" }
        require(orderingPolicy.isNotBlank()) { "An ordering policy is required" }
    }

    fun forSource(sourceFingerprint: String) = ShadowCacheKey(
        sourceFingerprint = sourceFingerprint,
        modelContractHash = modelContractHash,
        metadataHash = metadataHash,
        orderingPolicy = orderingPolicy,
    )

    companion object {
        const val ORDERING_POLICY_V1 = "end-session-set-key-basis-v1"
    }
}

/**
 * Complete pure rebuild from one immutable canonical snapshot. It maps the source once, replays
 * independent state for every basis, and produces the only payload accepted by the repository's
 * atomic cache/audit writer.
 */
class ShadowCachePipeline(
    private val config: ShadowConfig,
    val identity: ShadowCacheIdentity,
    private val metadataForCatalogueKey: (String) -> ShadowExerciseMetadata?,
) {
    fun build(
        snapshot: ShadowSnapshot,
        completedAtEpochMillis: Long,
    ): ShadowCacheWrite {
        val mapping = ShadowSnapshotMapper(
            coordinateCount = config.coordinateCount,
            metadataForCatalogueKey = metadataForCatalogueKey,
        ).map(snapshot)
        val states = ShadowBasis.entries.map { basis ->
            ProvisionalContinuousReplay.replay(
                initialState = ShadowState.prior(basis, config),
                observations = mapping.observations.filter { it.basis == basis },
                config = config,
            ).state
        }
        return ShadowCacheWrite(
            captured = identity.forSource(ShadowSourceFingerprint.from(snapshot)),
            statePayload = ShadowDerivedState.encode(states),
            observationCount = mapping.observations.size,
            exclusionCount = mapping.exclusions.size,
            completedAtEpochMillis = completedAtEpochMillis,
        )
    }
}
