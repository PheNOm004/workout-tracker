package com.lsing.timego.sync

import androidx.room.Entity
import androidx.room.Index

enum class SyncOperation { UPSERT, DELETE }

@Entity(
    tableName = "sync_metadata",
    primaryKeys = ["domainType", "localId"],
    indices = [Index(value = ["stableUuid"], unique = true), Index(value = ["pending", "nextAttemptAtEpochMillis"])],
)
data class SyncMetadata(
    val domainType: String,
    val localId: Long,
    val stableUuid: String,
    val operation: String,
    val revision: Long,
    val pending: Boolean,
    val updatedAtEpochMillis: Long,
    val attemptCount: Int = 0,
    val nextAttemptAtEpochMillis: Long = 0,
    val lastErrorCategory: String? = null,
)
