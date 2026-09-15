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

    /** Read once inside [WorkoutRepository.seedMissingRoutines] -- never a live Flow, since seeding
     *  needs a guaranteed-fresh read immediately after a transaction commits, not whatever
     *  [observeRoutines]'s Flow has invalidated to by the time it's collected. */
    @Query("SELECT * FROM routines")
    suspend fun allRoutinesOnce(): List<Routine>

    @Query("SELECT * FROM routine_exercises ORDER BY routineId, orderIndex")
    fun observeRoutineExercises(): Flow<List<RoutineExercise>>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getRoutineById(id: Long): Routine?

    /** Read once inside [WorkoutRepository.seedMissingRoutines] -- see [allRoutinesOnce]. */
    @Query("SELECT * FROM routine_exercises")
    suspend fun allRoutineExercisesOnce(): List<RoutineExercise>

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY orderIndex")
    suspend fun exercisesForRoutine(routineId: Long): List<RoutineExercise>

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutine(routineId: Long)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: Long)
}
