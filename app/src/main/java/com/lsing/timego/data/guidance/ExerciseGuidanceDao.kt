package com.lsing.timego.data.guidance

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseGuidanceDao {
    @Query("SELECT * FROM exercise_guidance WHERE catalogueKey = :key LIMIT 1")
    fun observeByKey(key: String): Flow<ExerciseGuidance?>

    @Query("SELECT * FROM exercise_guidance")
    fun observeAll(): Flow<List<ExerciseGuidance>>

    @Query("SELECT MAX(catalogueVersion) FROM exercise_guidance")
    suspend fun newestVersion(): Int?

    @Upsert
    suspend fun upsertAll(entries: List<ExerciseGuidance>)
}
