package com.lsing.timego.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class WorkoutRepository(private val db: TimeGoDatabase) {
    val exercises: Flow<List<Exercise>> = db.exerciseDao().observeAll()
    val sessions: Flow<List<WorkoutSession>> = db.sessionDao().observeAll()
    val bodyMetrics: Flow<List<BodyMetric>> = db.bodyMetricDao().observeAll()
    val routines: Flow<List<Routine>> = db.routineDao().observeRoutines()

    /** One-shot snapshot for callers that need a plain List, not a subscription (e.g. the
     *  muscle-balance check and the strength-curve lookup, which both need session dates once per
     *  computation, not a live collector). */
    suspend fun allSessions(): List<WorkoutSession> = sessions.first()

    suspend fun seedExercisesIfEmpty(seed: List<Exercise>) {
        if (db.exerciseDao().count() == 0) db.exerciseDao().insertAll(seed)
    }

    suspend fun addCustomExercise(name: String, muscleGroups: List<String>): Long =
        db.exerciseDao().insert(Exercise(name = name, muscleGroups = muscleGroups, isCustom = true))

    suspend fun startOrGetTodaySession(routineId: Long?): WorkoutSession =
        db.sessionDao().findByDate(LocalDate.now())
            ?: WorkoutSession(date = LocalDate.now(), routineId = routineId).let { session ->
                session.copy(id = db.sessionDao().insert(session))
            }

    suspend fun logSet(sessionId: Long, exerciseId: Long, weightKg: Double, reps: Int, targetReps: Int) {
        db.setLogDao().insert(
            SetLog(
                sessionId = sessionId,
                exerciseId = exerciseId,
                weightKg = weightKg,
                reps = reps,
                targetReps = targetReps,
                loggedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun historyForExercise(exerciseId: Long): List<SetLog> = db.setLogDao().historyForExercise(exerciseId)

    suspend fun allSetLogs(): List<SetLog> = db.setLogDao().getAll()

    suspend fun logBodyMetric(date: LocalDate, weightKg: Double?, waistCm: Double?) {
        db.bodyMetricDao().insert(BodyMetric(date = date, weightKg = weightKg, waistCm = waistCm))
    }

    suspend fun createRoutine(name: String, exerciseIds: List<Long>): Long {
        val routineId = db.routineDao().insertRoutine(Routine(name = name))
        exerciseIds.forEachIndexed { index, exerciseId ->
            db.routineDao().insertRoutineExercise(RoutineExercise(routineId = routineId, exerciseId = exerciseId, orderIndex = index))
        }
        return routineId
    }

    suspend fun exercisesForRoutine(routineId: Long): List<RoutineExercise> = db.routineDao().exercisesForRoutine(routineId)
}
