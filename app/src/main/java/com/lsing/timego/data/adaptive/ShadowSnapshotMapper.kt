package com.lsing.timego.data.adaptive

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import com.lsing.timego.domain.adaptive.ProvisionalContinuousCapability
import com.lsing.timego.domain.adaptive.ShadowBasis
import com.lsing.timego.domain.adaptive.ShadowObservation
import java.util.Collections

/**
 * A read-only source snapshot for the hidden provisional shadow.  It deliberately retains rows
 * that cannot become observations so [ShadowSnapshotMapper] can account for every exclusion.
 */
class ShadowSnapshot private constructor(rows: List<ShadowSnapshotRow>) {
    val rows: List<ShadowSnapshotRow> = immutableList(rows.sortedWith(SNAPSHOT_ORDER))

    companion object {
        fun from(
            sessions: List<WorkoutSession>,
            setLogs: List<SetLog>,
            exercises: List<Exercise>,
        ): ShadowSnapshot {
            val sessionsById = sessions.associateBy { it.id }
            val exercisesById = exercises.associateBy { it.id }
            return ShadowSnapshot(
                setLogs.map { setLog ->
                    ShadowSnapshotRow(
                        session = sessionsById[setLog.sessionId],
                        setLog = setLog,
                        exercise = exercisesById[setLog.exerciseId],
                    )
                },
            )
        }

        fun fromRows(rows: List<ShadowSnapshotRow>): ShadowSnapshot = ShadowSnapshot(rows)
    }
}

data class ShadowSnapshotRow(
    val session: WorkoutSession?,
    val setLog: SetLog,
    val exercise: Exercise?,
)

class ShadowSnapshotMapping(
    observations: List<ShadowObservation>,
    exclusions: List<ShadowSnapshotExclusion>,
) {
    val observations: List<ShadowObservation> = immutableList(observations)
    val exclusions: List<ShadowSnapshotExclusion> = immutableList(exclusions)

    override fun equals(other: Any?): Boolean =
        other is ShadowSnapshotMapping &&
            observations == other.observations &&
            exclusions == other.exclusions

    override fun hashCode(): Int = arrayOf(observations, exclusions).contentHashCode()

    override fun toString(): String =
        "ShadowSnapshotMapping(observations=$observations, exclusions=$exclusions)"
}

data class ShadowSnapshotExclusion(
    val sessionId: Long,
    val setId: Long,
    val reason: ShadowSnapshotExclusionReason,
)

enum class ShadowSnapshotExclusionReason {
    INVALID_SESSION_ID,
    INVALID_SET_ID,
    MISSING_SESSION,
    OPEN_SESSION,
    INVALID_SESSION_END,
    MISSING_EXERCISE,
    WARMUP,
    UNKEYED_CUSTOM_EXERCISE,
    CUSTOM_EXERCISE,
    UNKEYED_EXERCISE,
    INVALID_CATALOGUE_KEY,
    DURATION_ONLY_CARDIO,
    UNSUPPORTED_LOGGING_TYPE,
    UNKNOWN_DEMAND,
    INVALID_DEMAND,
    INVALID_WORK,
    SESSION_SUMMARY_NOT_SELECTED,
}

/** Reviewed model metadata required to turn a canonical exercise row into shadow evidence. */
class ShadowExerciseMetadata(
    demandVector: List<Double>,
    val bodyweightSupported: Boolean,
) {
    val demandVector: List<Double> = immutableList(demandVector)
}

/**
 * Pure bridge from canonical Room records to Task 5 observations.  Demand vectors are supplied
 * by the hidden caller because Room exercise rows intentionally do not encode model demands.
 */
class ShadowSnapshotMapper(
    private val coordinateCount: Int,
    private val metadataForCatalogueKey: (String) -> ShadowExerciseMetadata?,
) {
    init {
        require(coordinateCount > 0) { "A shadow snapshot mapper needs a positive coordinate count" }
    }

    fun map(snapshot: ShadowSnapshot): ShadowSnapshotMapping {
        val candidates = mutableListOf<ShadowObservationCandidate>()
        val exclusions = mutableListOf<ShadowSnapshotExclusion>()
        snapshot.rows.forEach { row ->
            val exclusion = exclusionFor(row)
            if (exclusion != null) {
                exclusions += exclusion
                return@forEach
            }

            val session = checkNotNull(row.session)
            val exercise = checkNotNull(row.exercise)
            val catalogueKey = checkNotNull(exercise.catalogueKey)
            val metadata = metadataForCatalogueKey(catalogueKey)
            if (metadata == null) {
                exclusions += row.excluded(ShadowSnapshotExclusionReason.UNKNOWN_DEMAND)
                return@forEach
            }
            val demand = metadata.demandVector
            if (demand.size != coordinateCount || demand.any { !it.isFinite() }) {
                exclusions += row.excluded(ShadowSnapshotExclusionReason.INVALID_DEMAND)
                return@forEach
            }

            val basisAndScore = basisAndScore(row, exercise.loggingType, metadata)
            if (basisAndScore == null) {
                exclusions += row.excluded(ShadowSnapshotExclusionReason.INVALID_WORK)
                return@forEach
            }
            val (basis, workScore) = basisAndScore
            candidates += ShadowObservationCandidate(
                row = row,
                observation = ShadowObservation(
                    catalogueKey = catalogueKey,
                    sessionId = session.id,
                    setId = row.setLog.id,
                    endedAtEpochMillis = checkNotNull(session.endEpochMillis),
                    demandVector = demand,
                    basis = basis,
                    observedWorkScore = workScore,
                ),
            )
        }

        val observations = candidates
            .groupBy { candidate ->
                SessionObservationKey(
                    sessionId = candidate.observation.sessionId,
                    catalogueKey = candidate.observation.catalogueKey,
                    basis = candidate.observation.basis,
                )
            }
            .values
            .map { sameSessionCandidates ->
                val selected = sameSessionCandidates.maxWith(SESSION_SUMMARY_SELECTION)
                sameSessionCandidates
                    .filterNot { it === selected }
                    .forEach { candidate ->
                        exclusions += candidate.row.excluded(
                            ShadowSnapshotExclusionReason.SESSION_SUMMARY_NOT_SELECTED,
                        )
                    }
                selected.observation
            }
            .sortedWith(OBSERVATION_ORDER)
        return ShadowSnapshotMapping(
            observations = immutableList(observations),
            exclusions = immutableList(exclusions.sortedWith(EXCLUSION_ORDER)),
        )
    }

    private fun exclusionFor(row: ShadowSnapshotRow): ShadowSnapshotExclusion? {
        if (row.setLog.id <= 0L) return row.excluded(ShadowSnapshotExclusionReason.INVALID_SET_ID)
        val session = row.session ?: return row.excluded(ShadowSnapshotExclusionReason.MISSING_SESSION)
        if (session.id <= 0L) return row.excluded(ShadowSnapshotExclusionReason.INVALID_SESSION_ID)
        val endedAt = session.endEpochMillis
        if (endedAt == null) return row.excluded(ShadowSnapshotExclusionReason.OPEN_SESSION)
        if (endedAt <= 0L) return row.excluded(ShadowSnapshotExclusionReason.INVALID_SESSION_END)
        val exercise = row.exercise ?: return row.excluded(ShadowSnapshotExclusionReason.MISSING_EXERCISE)
        if (row.setLog.isWarmup) return row.excluded(ShadowSnapshotExclusionReason.WARMUP)
        val catalogueKey = exercise.catalogueKey
        if (exercise.isCustom && catalogueKey.isNullOrBlank()) {
            return row.excluded(ShadowSnapshotExclusionReason.UNKEYED_CUSTOM_EXERCISE)
        }
        if (exercise.isCustom) return row.excluded(ShadowSnapshotExclusionReason.CUSTOM_EXERCISE)
        if (catalogueKey.isNullOrBlank()) return row.excluded(ShadowSnapshotExclusionReason.UNKEYED_EXERCISE)
        if (catalogueKey != catalogueKey.trim()) {
            return row.excluded(ShadowSnapshotExclusionReason.INVALID_CATALOGUE_KEY)
        }
        return when (exercise.loggingType) {
            LoggingType.DURATION_DISTANCE.name -> row.excluded(ShadowSnapshotExclusionReason.DURATION_ONLY_CARDIO)
            LoggingType.WEIGHT_REPS.name,
            LoggingType.HOLD.name,
            -> null

            else -> row.excluded(ShadowSnapshotExclusionReason.UNSUPPORTED_LOGGING_TYPE)
        }
    }

    private fun basisAndScore(
        row: ShadowSnapshotRow,
        loggingType: String,
        metadata: ShadowExerciseMetadata,
    ): Pair<ShadowBasis, Double>? =
        when (loggingType) {
            LoggingType.WEIGHT_REPS.name -> {
                val load = row.setLog.weightKg
                if (!load.isFinite() || load < 0.0) {
                    null
                } else if (load > 0.0) {
                    ProvisionalContinuousCapability.transformWorkScore(
                        basis = ShadowBasis.LOAD_REPS,
                        primaryValue = load,
                        secondaryValue = row.setLog.reps.toDouble(),
                    )?.let { ShadowBasis.LOAD_REPS to it }
                } else if (metadata.bodyweightSupported) {
                    ProvisionalContinuousCapability.transformWorkScore(
                        basis = ShadowBasis.REPS_ONLY,
                        primaryValue = row.setLog.reps.toDouble(),
                        secondaryValue = null,
                    )?.let { ShadowBasis.REPS_ONLY to it }
                } else {
                    null
                }
            }

            LoggingType.HOLD.name -> ProvisionalContinuousCapability.transformWorkScore(
                basis = ShadowBasis.HOLD_SECONDS,
                primaryValue = row.setLog.holdSeconds?.toDouble() ?: Double.NaN,
                secondaryValue = null,
            )?.let { ShadowBasis.HOLD_SECONDS to it }

            else -> null
        }

    private fun ShadowSnapshotRow.excluded(reason: ShadowSnapshotExclusionReason) = ShadowSnapshotExclusion(
        sessionId = setLog.sessionId,
        setId = setLog.id,
        reason = reason,
    )
}

private data class SessionObservationKey(
    val sessionId: Long,
    val catalogueKey: String,
    val basis: ShadowBasis,
)

private data class ShadowObservationCandidate(
    val row: ShadowSnapshotRow,
    val observation: ShadowObservation,
)

private fun <T> immutableList(values: List<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))

private val SNAPSHOT_ORDER = compareBy<ShadowSnapshotRow> {
    it.session?.endEpochMillis ?: Long.MAX_VALUE
}.thenBy {
    it.session?.id ?: Long.MAX_VALUE
}.thenBy {
    it.setLog.loggedAtEpochMillis
}.thenBy {
    it.setLog.id
}

private val SESSION_SUMMARY_SELECTION = compareBy<ShadowObservationCandidate> {
    it.observation.observedWorkScore
}.thenBy {
    it.row.setLog.loggedAtEpochMillis
}.thenBy {
    it.row.setLog.id
}

private val OBSERVATION_ORDER = compareBy<ShadowObservation> {
    it.endedAtEpochMillis
}.thenBy {
    it.sessionId
}.thenBy {
    it.setId
}.thenBy {
    it.catalogueKey
}.thenBy {
    it.basis.ordinal
}

private val EXCLUSION_ORDER = compareBy<ShadowSnapshotExclusion> {
    it.sessionId
}.thenBy {
    it.setId
}.thenBy {
    it.reason.ordinal
}
