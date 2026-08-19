package com.lsing.timego.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): Exercise?

    @Update
    suspend fun update(exercise: Exercise)

    @Update
    suspend fun updateAll(exercises: List<Exercise>)
}
