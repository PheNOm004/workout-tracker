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

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY loggedAtEpochMillis")
    suspend fun forSession(sessionId: Long): List<SetLog>

    /** Every set, live. The Progress screen's derived values (personal records, volume ratios,
     *  training stats, muscle distribution) are all functions of the full set table, and nothing
     *  else in the schema changes when a set is logged into an already-open session -- without a
     *  set-level Flow those values only refreshed when a *session* row appeared. */
    @Query("SELECT * FROM set_logs ORDER BY loggedAtEpochMillis")
    fun observeAll(): Flow<List<SetLog>>

    @Query("SELECT * FROM set_logs")
    suspend fun getAll(): List<SetLog>

    @Query("SELECT * FROM set_logs ORDER BY loggedAtEpochMillis")
    suspend fun allOrderedByTime(): List<SetLog>

    @Query("DELETE FROM set_logs WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}
