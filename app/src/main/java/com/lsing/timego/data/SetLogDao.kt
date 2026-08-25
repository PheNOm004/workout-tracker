package com.lsing.timego.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SetLogDao {
    @Insert
    suspend fun insert(setLog: SetLog): Long

    /** Every set, live. The Progress screen's derived values (personal records, volume ratios,
     *  training stats, muscle distribution) are all functions of the full set table, and nothing
     *  else in the schema changes when a set is logged into an already-open session -- without a
     *  set-level Flow those values only refreshed when a *session* row appeared. */
    @Query("SELECT * FROM set_logs ORDER BY loggedAtEpochMillis")
    fun observeAll(): Flow<List<SetLog>>

    @Query("SELECT * FROM set_logs")
    suspend fun getAll(): List<SetLog>

    /** Read once inside the hidden shadow snapshot transaction; mapper supplies final tuple order. */
    @Query("SELECT * FROM set_logs ORDER BY loggedAtEpochMillis, id")
    suspend fun allForShadowSnapshot(): List<SetLog>

    @Query("DELETE FROM set_logs WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}
