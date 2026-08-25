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

    /** Includes active rows so the pure mapper can record an explicit open-session exclusion. */
    @Query("SELECT * FROM workout_sessions ORDER BY CASE WHEN endEpochMillis IS NULL THEN 1 ELSE 0 END, endEpochMillis, id")
    suspend fun allForShadowSnapshot(): List<WorkoutSession>

    @Query("UPDATE workout_sessions SET endEpochMillis = :endEpochMillis WHERE id = :sessionId")
    suspend fun closeSession(sessionId: Long, endEpochMillis: Long)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun delete(sessionId: Long)
}
