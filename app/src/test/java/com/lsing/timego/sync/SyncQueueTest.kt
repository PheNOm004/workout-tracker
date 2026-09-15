package com.lsing.timego.sync

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncQueueTest {
    @Test fun repeatedUpdatesKeepStableUuidAndCoalesceRevision() = runBlocking {
        val repository = SyncRepository(FakeSyncDao())
        val first = repository.markPending("session", 7, 100)
        val second = repository.markPending("session", 7, 200)
        assertEquals(first.stableUuid, second.stableUuid)
        assertEquals(2, second.revision)
        assertEquals(1, repository.pendingBatch(20, 200).size)
    }

    @Test fun deletionBecomesTombstoneAndStaleAckCannotDropIt() = runBlocking {
        val repository = SyncRepository(FakeSyncDao())
        val update = repository.markPending("session", 7, 100)
        val deletion = repository.markDeleted("session", 7, 200)
        assertEquals(SyncOperation.DELETE.name, deletion.operation)
        assertFalse(repository.acknowledge(update))
        assertTrue(repository.acknowledge(deletion))
    }

    @Test fun batchesAreBoundedAndRetryIsDeferred() = runBlocking {
        val dao = FakeSyncDao()
        val repository = SyncRepository(dao)
        repeat(205) { repository.markPending("set_log", (it + 1).toLong(), 100) }
        val batch = repository.pendingBatch(500, 100)
        assertEquals(200, batch.size)
        val retried = batch.first()
        repository.markRetry(retried, "network", 100)
        assertFalse(repository.pendingBatch(500, 100).any { it.stableUuid == retried.stableUuid })
        assertTrue(repository.pendingBatch(500, 30_100).any { it.stableUuid == retried.stableUuid })
    }
}

private class FakeSyncDao : SyncDao {
    private val rows = linkedMapOf<Pair<String, Long>, SyncMetadata>()
    override suspend fun find(domainType: String, localId: Long) = rows[domainType to localId]
    override suspend fun upsert(metadata: SyncMetadata) { rows[metadata.domainType to metadata.localId] = metadata }
    override suspend fun pendingBatch(limit: Int, nowEpochMillis: Long) = rows.values.filter { it.pending && it.nextAttemptAtEpochMillis <= nowEpochMillis }.sortedBy { it.updatedAtEpochMillis }.take(limit)
    override suspend fun acknowledge(domainType: String, localId: Long, revision: Long): Int {
        val key = domainType to localId
        val row = rows[key] ?: return 0
        if (row.revision != revision) return 0
        rows[key] = row.copy(pending = false)
        return 1
    }
    override fun observePendingCount() = flowOf(rows.values.count { it.pending })
}
