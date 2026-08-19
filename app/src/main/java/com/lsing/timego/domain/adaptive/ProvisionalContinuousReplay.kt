package com.lsing.timego.domain.adaptive

import kotlin.math.max

/**
 * Pure, deterministic hidden-state transition for the provisional continuous-shadow contract.
 * Replay derives closed-session groups and scores each group from the prior closed-session state.
 */
object ProvisionalContinuousReplay {
    fun update(
        state: ShadowState,
        observation: ShadowObservation,
        config: ShadowConfig,
    ): ShadowUpdate = apply(state, assess(state, observation, config), config)

    fun replay(
        initialState: ShadowState,
        observations: List<ShadowObservation>,
        config: ShadowConfig,
    ): ShadowReplay {
        val ordered = observations.sortedWith(OBSERVATION_ORDER)
        val updates = mutableListOf<ShadowUpdate>()
        var currentState = initialState
        var index = 0
        while (index < ordered.size) {
            val first = ordered[index]
            val frozenState = currentState
            val sessionEnd = first.endedAtEpochMillis
            val sessionId = first.sessionId
            while (
                index < ordered.size &&
                ordered[index].endedAtEpochMillis == sessionEnd &&
                ordered[index].sessionId == sessionId
            ) {
                val scored = assess(frozenState, ordered[index], config)
                val update = apply(currentState, scored, config)
                updates += update
                currentState = update.state
                index += 1
            }
        }
        val abstentions = updates.mapNotNull(ShadowUpdate::abstentionReason)
        return ShadowReplay(
            state = currentState,
            updates = updates,
            observationCount = ordered.size,
            updateCount = updates.count(ShadowUpdate::updated),
            baselineRegistrationCount = updates.count {
                it.abstentionReason == "registered_personal_baseline"
            },
            abstentionReasons = abstentions.filterNot { it == "registered_personal_baseline" },
        )
    }

    private fun assess(
        frozenState: ShadowState,
        observation: ShadowObservation,
        config: ShadowConfig,
    ): AssessedObservation {
        if (!config.isValid()) return AssessedObservation(observation, "invalid_config")
        if (!frozenState.isValid(config)) return AssessedObservation(observation, "invalid_state")
        if (!observation.isValid(config)) return AssessedObservation(observation, "invalid_observation")
        if (frozenState.basis != observation.basis) return AssessedObservation(observation, "basis_mismatch")

        val advanced = frozenState.advancedTo(observation.endedAtEpochMillis, config.processVariancePerDay)
        if (!advanced.isValid(config)) return AssessedObservation(observation, "invalid_observation")
        val baseline = advanced.personalBaselines[observation.catalogueKey]
            ?: return AssessedObservation(observation, baselineRegistration = true)
        val predictedChange = advanced.mean.zip(observation.demandVector).sumOf { (mean, demand) ->
            mean * demand
        }
        val residual = observation.observedWorkScore - baseline - predictedChange
        return if (predictedChange.isFinite() && residual.isFinite()) {
            AssessedObservation(observation)
        } else {
            AssessedObservation(observation, "invalid_observation")
        }
    }

    private fun apply(
        state: ShadowState,
        assessed: AssessedObservation,
        config: ShadowConfig,
    ): ShadowUpdate {
        assessed.reason?.let { return abstain(state, it) }
        val advanced = state.advancedTo(assessed.observation.endedAtEpochMillis, config.processVariancePerDay)
        if (!advanced.isValid(config)) return abstain(state, "invalid_observation")
        if (assessed.baselineRegistration) {
            return ShadowUpdate(
                state = if (advanced.personalBaselines.containsKey(assessed.observation.catalogueKey)) {
                    advanced
                } else {
                    advanced.copy(
                        personalBaselines = advanced.personalBaselines +
                            (assessed.observation.catalogueKey to assessed.observation.observedWorkScore),
                    )
                },
                updated = false,
                abstentionReason = "registered_personal_baseline",
            )
        }

        val baseline = advanced.personalBaselines[assessed.observation.catalogueKey]
            ?: return abstain(state, "invalid_observation")
        val predictedChange = advanced.mean.zip(assessed.observation.demandVector).sumOf { (mean, demand) ->
            mean * demand
        }
        val residual = assessed.observation.observedWorkScore - baseline - predictedChange
        if (!predictedChange.isFinite() || !residual.isFinite()) {
            return abstain(state, "invalid_observation")
        }

        val totalVariance = config.observationVariance +
            advanced.variance.zip(assessed.observation.demandVector).sumOf { (variance, demand) ->
                variance * demand * demand
            }
        if (!totalVariance.isFinite() || totalVariance <= 0.0) return abstain(state, "invalid_observation")
        val gains = advanced.variance.zip(assessed.observation.demandVector).map { (variance, demand) ->
            variance * demand / totalVariance
        }
        val nextMean = advanced.mean.zip(gains).map { (mean, gain) -> mean + gain * residual }
        val nextVariance = advanced.variance.zip(gains).zip(assessed.observation.demandVector).map {
                (varianceAndGain, demand) ->
            val (variance, gain) = varianceAndGain
            max(0.0, variance - gain * demand * variance)
        }
        if (nextMean.any { !it.isFinite() } || nextVariance.any { !it.isFinite() }) {
            return abstain(state, "invalid_observation")
        }
        return ShadowUpdate(
            state = advanced.copy(mean = nextMean, variance = nextVariance),
            updated = true,
            abstentionReason = null,
        )
    }

    private fun abstain(state: ShadowState, reason: String) = ShadowUpdate(
        state = state,
        updated = false,
        abstentionReason = reason,
    )

    private fun ShadowConfig.isValid(): Boolean =
        coordinateCount > 0 &&
            priorVariance.isFinite() && priorVariance >= 0.0 &&
            processVariancePerDay.isFinite() && processVariancePerDay >= 0.0 &&
            observationVariance.isFinite() && observationVariance > 0.0

    private fun ShadowState.isValid(config: ShadowConfig): Boolean =
        mean.size == config.coordinateCount &&
            variance.size == config.coordinateCount &&
            mean.all(Double::isFinite) &&
            variance.all { it.isFinite() && it >= 0.0 } &&
            (observedAtEpochMillis == null || observedAtEpochMillis > 0L) &&
            personalBaselines.all { (key, score) -> key.isNotBlank() && score.isFinite() }

    private fun ShadowObservation.isValid(config: ShadowConfig): Boolean =
        catalogueKey.isNotBlank() &&
            sessionId > 0L &&
            setId > 0L &&
            endedAtEpochMillis > 0L &&
            demandVector.size == config.coordinateCount &&
            demandVector.all(Double::isFinite) &&
            observedWorkScore.isFinite()

    private fun ShadowState.advancedTo(
        endedAtEpochMillis: Long,
        processVariancePerDay: Double,
    ): ShadowState {
        val days = observedAtEpochMillis
            ?.let { previous -> max(0.0, (endedAtEpochMillis - previous).toDouble() / MILLIS_PER_DAY) }
            ?: 0.0
        return copy(
            variance = variance.map { it + days * processVariancePerDay },
            observedAtEpochMillis = max(observedAtEpochMillis ?: endedAtEpochMillis, endedAtEpochMillis),
        )
    }

    private data class AssessedObservation(
        val observation: ShadowObservation,
        val reason: String? = null,
        val baselineRegistration: Boolean = false,
    )

    private val OBSERVATION_ORDER = compareBy<ShadowObservation> { it.endedAtEpochMillis }
        .thenBy { it.sessionId }
        .thenBy { it.setId }
        .thenBy { it.catalogueKey }
        .thenBy { it.basis.ordinal }

    private const val MILLIS_PER_DAY = 86_400_000.0
}
