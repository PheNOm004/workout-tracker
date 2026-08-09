package com.lsing.timego.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SetLogDao {
    @Insert
    suspend fun insert(setLog: SetLog): Long

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId ORDER BY loggedAtEpochMillis")
    suspend fun historyForExercise(exerciseId: Long): List<SetLog>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId")
    fun observeForSession(sessionId: Long): Flow<List<SetLog>>

    @Query("SELECT * FROM set_logs")
    suspend fun getAll(): List<SetLog>

    @Query("SELECT * FROM set_logs ORDER BY loggedAtEpochMillis")
    suspend fun allOrderedByTime(): List<SetLog>
}
