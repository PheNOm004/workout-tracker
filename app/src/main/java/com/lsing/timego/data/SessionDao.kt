package com.lsing.timego.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun observeAll(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE endEpochMillis IS NULL LIMIT 1")
    suspend fun findActiveSession(): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE endEpochMillis IS NOT NULL ORDER BY endEpochMillis DESC LIMIT 1")
    suspend fun findLastClosedSession(): WorkoutSession?

    @Query("UPDATE workout_sessions SET endEpochMillis = :endEpochMillis WHERE id = :sessionId")
    suspend fun closeSession(sessionId: Long, endEpochMillis: Long)
}
