package com.lsing.timego.sync

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_metadata WHERE domainType = :domainType AND localId = :localId LIMIT 1")
    suspend fun find(domainType: String, localId: Long): SyncMetadata?

    @Upsert
    suspend fun upsert(metadata: SyncMetadata)

    @Query("SELECT * FROM sync_metadata WHERE pending = 1 AND nextAttemptAtEpochMillis <= :nowEpochMillis ORDER BY updatedAtEpochMillis, domainType, localId LIMIT :limit")
    suspend fun pendingBatch(limit: Int, nowEpochMillis: Long): List<SyncMetadata>

    @Query("UPDATE sync_metadata SET pending = 0, attemptCount = 0, lastErrorCategory = NULL WHERE domainType = :domainType AND localId = :localId AND revision = :revision")
    suspend fun acknowledge(domainType: String, localId: Long, revision: Long): Int

    @Query("SELECT COUNT(*) FROM sync_metadata WHERE pending = 1")
    fun observePendingCount(): Flow<Int>
}
