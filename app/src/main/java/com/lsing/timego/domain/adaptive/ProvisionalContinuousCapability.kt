package com.lsing.timego.domain.adaptive

import java.util.Collections
import java.util.TreeMap
import kotlin.math.ln1p

/** Measurement bases which never share a personal baseline or state. */
enum class ShadowBasis {
    LOAD_REPS,
    REPS_ONLY,
    HOLD_SECONDS,
}

/** Fixed local parameters for one coordinate convention. */
data class ShadowConfig(
    val coordinateCount: Int,
    val priorVariance: Double,
    val processVariancePerDay: Double,
    val observationVariance: Double,
)

/** Immutable state for exactly one [ShadowBasis]. */
class ShadowState(
    val basis: ShadowBasis,
    mean: List<Double>,
    variance: List<Double>,
    val observedAtEpochMillis: Long?,
    personalBaselines: Map<String, Double>,
) {
    val mean: List<Double> = immutableList(mean)
    val variance: List<Double> = immutableList(variance)
    val personalBaselines: Map<String, Double> = immutableSortedMap(personalBaselines)

    fun copy(
        basis: ShadowBasis = this.basis,
        mean: List<Double> = this.mean,
        variance: List<Double> = this.variance,
        observedAtEpochMillis: Long? = this.observedAtEpochMillis,
        personalBaselines: Map<String, Double> = this.personalBaselines,
    ) = ShadowState(basis, mean, variance, observedAtEpochMillis, personalBaselines)

    override fun equals(other: Any?): Boolean =
        other is ShadowState &&
            basis == other.basis &&
            mean == other.mean &&
            variance == other.variance &&
            observedAtEpochMillis == other.observedAtEpochMillis &&
            personalBaselines == other.personalBaselines

    override fun hashCode(): Int =
        arrayOf(basis, mean, variance, observedAtEpochMillis, personalBaselines).contentHashCode()

    override fun toString(): String =
        "ShadowState(basis=$basis, mean=$mean, variance=$variance, " +
            "observedAtEpochMillis=$observedAtEpochMillis, personalBaselines=$personalBaselines)"

    companion object {
        fun prior(basis: ShadowBasis, config: ShadowConfig): ShadowState {
            require(config.isValid()) { "A prior requires a finite valid shadow configuration" }
            return ShadowState(
                basis = basis,
                mean = List(config.coordinateCount) { 0.0 },
                variance = List(config.coordinateCount) { config.priorVariance },
                observedAtEpochMillis = null,
                personalBaselines = emptyMap(),
            )
        }
    }
}

/** A completed, already-transformed synthetic or local shadow observation. */
class ShadowObservation(
    val catalogueKey: String,
    val sessionId: Long,
    val setId: Long,
    val endedAtEpochMillis: Long,
    demandVector: List<Double>,
    val basis: ShadowBasis,
    val observedWorkScore: Double,
) {
    val demandVector: List<Double> = immutableList(demandVector)

    override fun equals(other: Any?): Boolean =
        other is ShadowObservation &&
            catalogueKey == other.catalogueKey &&
            sessionId == other.sessionId &&
            setId == other.setId &&
            endedAtEpochMillis == other.endedAtEpochMillis &&
            demandVector == other.demandVector &&
            basis == other.basis &&
            observedWorkScore == other.observedWorkScore

    override fun hashCode(): Int =
        arrayOf(catalogueKey, sessionId, setId, endedAtEpochMillis, demandVector, basis, observedWorkScore)
            .contentHashCode()

    override fun toString(): String =
        "ShadowObservation(catalogueKey=$catalogueKey, sessionId=$sessionId, setId=$setId, " +
            "endedAtEpochMillis=$endedAtEpochMillis, demandVector=$demandVector, basis=$basis, " +
            "observedWorkScore=$observedWorkScore)"
}

/** The complete result of processing one observation, including an abstention when applicable. */
data class ShadowUpdate(
    val state: ShadowState,
    val updated: Boolean,
    val abstentionReason: String?,
)

/** Hidden audit facts for a replay of caller-ordered observations. */
class ShadowReplay(
    val state: ShadowState,
    updates: List<ShadowUpdate>,
    val observationCount: Int,
    val updateCount: Int,
    val baselineRegistrationCount: Int,
    abstentionReasons: List<String>,
) {
    val updates: List<ShadowUpdate> = immutableList(updates)
    val abstentionReasons: List<String> = immutableList(abstentionReasons)

    override fun equals(other: Any?): Boolean =
        other is ShadowReplay &&
            state == other.state &&
            updates == other.updates &&
            observationCount == other.observationCount &&
            updateCount == other.updateCount &&
            baselineRegistrationCount == other.baselineRegistrationCount &&
            abstentionReasons == other.abstentionReasons

    override fun hashCode(): Int =
        arrayOf(state, updates, observationCount, updateCount, baselineRegistrationCount, abstentionReasons)
            .contentHashCode()

    override fun toString(): String =
        "ShadowReplay(state=$state, updates=$updates, observationCount=$observationCount, " +
            "updateCount=$updateCount, baselineRegistrationCount=$baselineRegistrationCount, " +
            "abstentionReasons=$abstentionReasons)"
}

/** Exact v1 transforms for completed work scores; invalid inputs are excluded. */
object ProvisionalContinuousCapability {
    fun transformWorkScore(
        basis: ShadowBasis,
        primaryValue: Double,
        secondaryValue: Double?,
    ): Double? {
        if (!primaryValue.isFinite() || primaryValue <= 0.0) return null
        val rawWork = when (basis) {
            ShadowBasis.LOAD_REPS -> {
                val reps = secondaryValue ?: return null
                if (!reps.isFinite() || reps <= 0.0) return null
                primaryValue * reps
            }

            ShadowBasis.REPS_ONLY,
            ShadowBasis.HOLD_SECONDS,
            -> {
                if (secondaryValue != null) return null
                primaryValue
            }
        }
        return rawWork.takeIf { it.isFinite() && it > 0.0 }?.let(::ln1p)?.takeIf(Double::isFinite)
    }
}

private fun ShadowConfig.isValid(): Boolean =
    coordinateCount > 0 &&
        priorVariance.isFinite() && priorVariance >= 0.0 &&
        processVariancePerDay.isFinite() && processVariancePerDay >= 0.0 &&
        observationVariance.isFinite() && observationVariance > 0.0

private fun <T> immutableList(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))

private fun immutableSortedMap(values: Map<String, Double>): Map<String, Double> =
    Collections.unmodifiableMap(TreeMap(values))
