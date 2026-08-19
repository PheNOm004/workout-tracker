package com.lsing.timego.data.adaptive

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ShadowDao {
    @Query("SELECT * FROM shadow_snapshots WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun snapshot(cacheKey: String = ShadowSnapshotEntity.CACHE_KEY): ShadowSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: ShadowSnapshotEntity)

    @Query("DELETE FROM shadow_snapshots WHERE cacheKey = :cacheKey")
    suspend fun deleteSnapshot(cacheKey: String = ShadowSnapshotEntity.CACHE_KEY): Int

    @Insert
    suspend fun appendAudit(audit: ShadowAuditEntity): Long

    @Query("SELECT * FROM shadow_audit ORDER BY id")
    suspend fun allAudit(): List<ShadowAuditEntity>
}
