package com.lsing.timego.sync

import com.lsing.timego.data.TimeGoDatabase
import java.util.UUID

class SyncRepository(private val dao: SyncDao) {
    constructor(database: TimeGoDatabase) : this(database.syncDao())

    suspend fun markPending(domainType: String, localId: Long, nowEpochMillis: Long = System.currentTimeMillis()): SyncMetadata =
        mark(domainType, localId, SyncOperation.UPSERT, nowEpochMillis)

    suspend fun markDeleted(domainType: String, localId: Long, nowEpochMillis: Long = System.currentTimeMillis()): SyncMetadata =
        mark(domainType, localId, SyncOperation.DELETE, nowEpochMillis)

    suspend fun pendingBatch(limit: Int, nowEpochMillis: Long = System.currentTimeMillis()): List<SyncMetadata> =
        dao.pendingBatch(limit.coerceIn(1, MAX_BATCH_SIZE), nowEpochMillis)

    suspend fun acknowledge(item: SyncMetadata): Boolean = dao.acknowledge(item.domainType, item.localId, item.revision) == 1

    suspend fun markRetry(item: SyncMetadata, errorCategory: String, nowEpochMillis: Long = System.currentTimeMillis()) {
        val current = dao.find(item.domainType, item.localId) ?: return
        if (current.revision != item.revision) return
        val attempts = current.attemptCount + 1
        val delay = (BASE_RETRY_MILLIS * (1L shl (attempts - 1).coerceAtMost(8))).coerceAtMost(MAX_RETRY_MILLIS)
        dao.upsert(current.copy(attemptCount = attempts, nextAttemptAtEpochMillis = nowEpochMillis + delay, lastErrorCategory = errorCategory, pending = true))
    }

    private suspend fun mark(domainType: String, localId: Long, operation: SyncOperation, nowEpochMillis: Long): SyncMetadata {
        require(domainType in SYNCABLE_TYPES)
        require(localId > 0)
        val existing = dao.find(domainType, localId)
        return SyncMetadata(
            domainType = domainType,
            localId = localId,
            stableUuid = existing?.stableUuid ?: UUID.randomUUID().toString(),
            operation = operation.name,
            revision = (existing?.revision ?: 0) + 1,
            pending = true,
            updatedAtEpochMillis = nowEpochMillis,
        ).also { dao.upsert(it) }
    }

    companion object {
        val SYNCABLE_TYPES = setOf("exercise", "session", "set_log", "routine", "body_metric")
        const val MAX_BATCH_SIZE = 200
        const val BASE_RETRY_MILLIS = 30_000L
        const val MAX_RETRY_MILLIS = 6 * 60 * 60 * 1000L
    }
}
