package com.lsing.timego.data.adaptive

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lsing.timego.domain.adaptive.ShadowState
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.security.MessageDigest

/**
 * One replaceable, derived shadow-cache record.  [statePayload] is opaque derived model state;
 * it never stores a canonical exercise, session, or set row.  Canonical Room rows remain the
 * only recoverable source and can recreate this record after any invalidation.
 */
@Entity(tableName = "shadow_snapshots")
data class ShadowSnapshotEntity(
    @PrimaryKey val cacheKey: String = CACHE_KEY,
    val sourceFingerprint: String,
    val modelContractHash: String,
    val metadataHash: String,
    val orderingPolicy: String,
    val statePayload: String,
    val sourceRowCount: Int,
    val observationCount: Int,
    val exclusionCount: Int,
    val completionStatus: String,
    val completedAtEpochMillis: Long,
) {
    companion object {
        const val CACHE_KEY = "provisional-continuous-shadow"
    }
}

/**
 * Append-only aggregate audit fact for hidden cache lifecycle events.  It intentionally has no
 * workout rows, exercise/session identifiers, user/network identifier, or payload.
 */
@Entity(tableName = "shadow_audit")
data class ShadowAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceFingerprint: String,
    val modelContractHash: String,
    val metadataHash: String,
    val orderingPolicy: String,
    val sourceRowCount: Int,
    val observationCount: Int,
    val exclusionCount: Int,
    val rebuildStatus: String,
    val recordedAtEpochMillis: Long,
)

enum class ShadowRebuildStatus {
    COMPLETED,
    STALE_DISCARDED,
    INVALIDATED,
}

/** The exact version/source tuple that makes a derived cache state usable. */
data class ShadowCacheKey(
    val sourceFingerprint: String,
    val modelContractHash: String,
    val metadataHash: String,
    val orderingPolicy: String,
) {
    init {
        require(sourceFingerprint.isNotBlank()) { "A cache key requires a source fingerprint" }
        require(modelContractHash.isNotBlank()) { "A cache key requires a model contract hash" }
        require(metadataHash.isNotBlank()) { "A cache key requires a metadata hash" }
        require(orderingPolicy.isNotBlank()) { "A cache key requires an ordering policy" }
    }
}

object ShadowCacheCompatibility {
    fun isUsable(persisted: ShadowCacheKey, requested: ShadowCacheKey): Boolean =
        persisted == requested
}

enum class ShadowCacheWriteDisposition {
    PERSISTED,
    INVALIDATED,
    STALE_DISCARDED,
}

data class ShadowCacheWriteDecision(
    val disposition: ShadowCacheWriteDisposition,
    val rebuildFromSourceFingerprint: String,
)

/**
 * Pure stale-write gate. The repository invokes it only after recapturing canonical source rows
 * inside the Room write transaction, so it cannot bless a cache built from a deleted/changed row.
 */
object ShadowCacheWritePolicy {
    fun decide(
        captured: ShadowCacheKey,
        currentSourceFingerprint: String,
        existing: ShadowCacheKey?,
    ): ShadowCacheWriteDecision {
        require(currentSourceFingerprint.isNotBlank()) { "A current source fingerprint is required" }
        if (captured.sourceFingerprint != currentSourceFingerprint) {
            return ShadowCacheWriteDecision(
                disposition = ShadowCacheWriteDisposition.STALE_DISCARDED,
                rebuildFromSourceFingerprint = currentSourceFingerprint,
            )
        }
        return ShadowCacheWriteDecision(
            disposition = if (existing != null && !ShadowCacheCompatibility.isUsable(existing, captured)) {
                ShadowCacheWriteDisposition.INVALIDATED
            } else {
                ShadowCacheWriteDisposition.PERSISTED
            },
            rebuildFromSourceFingerprint = currentSourceFingerprint,
        )
    }
}

/**
 * Canonical payload encoding for derived basis state. The output is independent of caller list or
 * map iteration order, making a full rebuild byte-identical to ordered incremental replay.
 */
object ShadowDerivedState {
    fun encode(states: List<ShadowState>): String = states
        .sortedBy { it.basis.ordinal }
        .joinToString(separator = "|") { state ->
            buildString {
                append(state.basis.name)
                append(';')
                append(state.mean.joinToString(",") { it.toStableBits() })
                append(';')
                append(state.variance.joinToString(",") { it.toStableBits() })
                append(';')
                append(state.observedAtEpochMillis ?: "none")
                append(';')
                append(
                    state.personalBaselines.entries
                        .sortedBy { it.key }
                        .joinToString(",") { (key, score) -> "${key.base64()}:${score.toStableBits()}" },
                )
            }
        }

    private fun Double.toStableBits(): String = java.lang.Double.doubleToLongBits(this).toString(16)

    private fun String.base64(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(StandardCharsets.UTF_8))
}

/** Request produced from a captured, immutable Task 6 source snapshot. */
data class ShadowCacheWrite(
    val captured: ShadowCacheKey,
    val statePayload: String,
    val observationCount: Int,
    val exclusionCount: Int,
    val completedAtEpochMillis: Long,
) {
    init {
        require(observationCount >= 0) { "Observation count cannot be negative" }
        require(exclusionCount >= 0) { "Exclusion count cannot be negative" }
        require(completedAtEpochMillis > 0L) { "Completion time must be positive" }
    }
}

/** Stable, local-only fingerprint of the canonical Room snapshot captured by Task 6. */
object ShadowSourceFingerprint {
    fun from(snapshot: ShadowSnapshot): String {
        val source = buildString {
            append("timego.shadow-source.v1\n")
            snapshot.rows.forEach { row ->
                append(row.setLog.id).append('|')
                append(row.setLog.sessionId).append('|')
                append(row.setLog.exerciseId).append('|')
                append(row.setLog.weightKg.toStableBits()).append('|')
                append(row.setLog.reps).append('|')
                append(row.setLog.targetReps).append('|')
                append(row.setLog.loggedAtEpochMillis).append('|')
                append(row.setLog.durationMinutes.toStableBits()).append('|')
                append(row.setLog.distanceKm.toStableBits()).append('|')
                append(row.setLog.holdSeconds ?: "none").append('|')
                append(row.setLog.targetHoldSeconds ?: "none").append('|')
                append(row.setLog.isWarmup).append('|')
                append(row.setLog.addedWeightKg.toStableBits()).append('|')
                append(row.setLog.rpe ?: "none").append('|')
                append(row.setLog.targetProvenance.base64()).append('|')
                row.session?.let { session ->
                    append(session.id).append('|')
                    append(session.date.toEpochDay()).append('|')
                    append(session.routineId ?: "none").append('|')
                    append(session.startEpochMillis).append('|')
                    append(session.endEpochMillis ?: "none")
                } ?: append("missing-session")
                append('|')
                row.exercise?.let { exercise ->
                    append(exercise.id).append('|')
                    append(exercise.name.base64()).append('|')
                    append(exercise.catalogueKey?.base64() ?: "none").append('|')
                    append(exercise.muscleGroups.joinToString(",") { it.base64() }).append('|')
                    append(exercise.isCustom).append('|')
                    append(exercise.category.base64()).append('|')
                    append(exercise.loggingType.base64()).append('|')
                    append(
                        exercise.muscleWeights.toSortedMap()
                            .entries.joinToString(",") { (key, weight) -> "${key.base64()}:$weight" },
                    )
                } ?: append("missing-exercise")
                append('\n')
            }
        }
        return "sha256:" + MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { "%02x".format(it) }
    }

    private fun Double?.toStableBits(): String = this?.let { java.lang.Double.doubleToLongBits(it).toString(16) } ?: "none"

    private fun String.base64(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(StandardCharsets.UTF_8))
}
