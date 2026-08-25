package com.lsing.timego.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Insert
    suspend fun insertRoutine(routine: Routine): Long

    @Insert
    suspend fun insertRoutineExercise(routineExercise: RoutineExercise): Long

    @Query("SELECT * FROM routines ORDER BY name")
    fun observeRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routine_exercises ORDER BY routineId, orderIndex")
    fun observeRoutineExercises(): Flow<List<RoutineExercise>>

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY orderIndex")
    suspend fun exercisesForRoutine(routineId: Long): List<RoutineExercise>

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutine(routineId: Long)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: Long)
}
