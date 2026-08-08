package com.lsing.timego.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun observeAll(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE date = :date LIMIT 1")
    suspend fun findByDate(date: LocalDate): WorkoutSession?
}
