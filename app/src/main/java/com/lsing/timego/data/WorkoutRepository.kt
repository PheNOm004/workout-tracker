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

    /** Inserts any [seed] exercise whose name isn't already present -- NOT gated on the table
     *  being totally empty, since expanding the seed list (Update 1.1: 12 -> 119) must still
     *  reach devices that already have some exercises logged. Matches by name rather than id,
     *  since seed entries have no stable id across app versions. Also syncs [Exercise.loggingType]
     *  for exercises that already exist but whose seed loggingType has since changed (e.g. Plank
     *  reclassified WEIGHT_REPS -> HOLD) -- otherwise an already-migrated device would keep the
     *  stale value forever, since insertAll only touches missing rows. */
    suspend fun seedMissingExercises(seed: List<Exercise>) {
        val existingByName = exercises.first().associateBy { it.name }
        val missing = seed.filter { it.name !in existingByName.keys }
        if (missing.isNotEmpty()) db.exerciseDao().insertAll(missing)
        seed.forEach { seedExercise ->
            val existing = existingByName[seedExercise.name]
            if (existing != null && existing.loggingType != seedExercise.loggingType) {
                db.exerciseDao().updateLoggingType(seedExercise.name, seedExercise.loggingType)
            }
        }
    }

    suspend fun addCustomExercise(name: String, muscleGroups: List<String>, category: String): Long {
        val loggingType = if (category == ExerciseCategory.CARDIO.name || category == ExerciseCategory.WARMUP.name) {
            LoggingType.DURATION_DISTANCE.name
        } else {
            LoggingType.WEIGHT_REPS.name
        }
        return db.exerciseDao().insert(Exercise(name = name, muscleGroups = muscleGroups, isCustom = true, category = category, loggingType = loggingType))
    }

    suspend fun activeSession(): WorkoutSession? = db.sessionDao().findActiveSession()

    suspend fun lastClosedSession(): WorkoutSession? = db.sessionDao().findLastClosedSession()

    suspend fun startSession(routineId: Long?): WorkoutSession {
        val now = System.currentTimeMillis()
        val session = WorkoutSession(date = LocalDate.now(), routineId = routineId, startEpochMillis = now, endEpochMillis = null)
        return session.copy(id = db.sessionDao().insert(session))
    }

    suspend fun endSession(sessionId: Long, endEpochMillis: Long) {
        db.sessionDao().closeSession(sessionId, endEpochMillis)
    }

    suspend fun setLogsForSession(sessionId: Long): List<SetLog> = db.setLogDao().forSession(sessionId)

    suspend fun logSet(
        sessionId: Long,
        exerciseId: Long,
        weightKg: Double,
        reps: Int,
        targetReps: Int,
        isWarmup: Boolean = false,
        addedWeightKg: Double? = null,
    ) {
        db.setLogDao().insert(
            SetLog(
                sessionId = sessionId,
                exerciseId = exerciseId,
                weightKg = weightKg,
                reps = reps,
                targetReps = targetReps,
                loggedAtEpochMillis = System.currentTimeMillis(),
                isWarmup = isWarmup,
                addedWeightKg = addedWeightKg,
            ),
        )
    }

    suspend fun logCardioSet(sessionId: Long, exerciseId: Long, durationMinutes: Double, distanceKm: Double?) {
        db.setLogDao().insert(
            SetLog(
                sessionId = sessionId,
                exerciseId = exerciseId,
                weightKg = 0.0,
                reps = 0,
                targetReps = 0,
                loggedAtEpochMillis = System.currentTimeMillis(),
                durationMinutes = durationMinutes,
                distanceKm = distanceKm,
            ),
        )
    }

    suspend fun logHoldSet(sessionId: Long, exerciseId: Long, durationSeconds: Int, targetDurationSeconds: Int, isWarmup: Boolean = false) {
        db.setLogDao().insert(
            SetLog(
                sessionId = sessionId,
                exerciseId = exerciseId,
                weightKg = 0.0,
                reps = 0,
                targetReps = 0,
                loggedAtEpochMillis = System.currentTimeMillis(),
                holdSeconds = durationSeconds,
                targetHoldSeconds = targetDurationSeconds,
                isWarmup = isWarmup,
            ),
        )
    }

    suspend fun latestBodyWeightKg(): Double? = bodyMetrics.first().lastOrNull { it.weightKg != null }?.weightKg

    suspend fun latestHeightCm(): Double? = bodyMetrics.first().lastOrNull { it.heightCm != null }?.heightCm

    suspend fun historyForExercise(exerciseId: Long): List<SetLog> = db.setLogDao().historyForExercise(exerciseId)

    suspend fun allSetLogs(): List<SetLog> = db.setLogDao().getAll()

    suspend fun allSetLogsOrderedByTime(): List<SetLog> = db.setLogDao().allOrderedByTime()

    suspend fun logBodyMetric(date: LocalDate, weightKg: Double?, waistCm: Double?, heightCm: Double?) {
        db.bodyMetricDao().insert(BodyMetric(date = date, weightKg = weightKg, waistCm = waistCm, heightCm = heightCm))
    }

    suspend fun createRoutine(name: String, exerciseIds: List<Long>, daysOfWeek: List<String>): Long {
        val routineId = db.routineDao().insertRoutine(Routine(name = name, daysOfWeek = daysOfWeek))
        exerciseIds.forEachIndexed { index, exerciseId ->
            db.routineDao().insertRoutineExercise(RoutineExercise(routineId = routineId, exerciseId = exerciseId, orderIndex = index))
        }
        return routineId
    }

    suspend fun deleteRoutine(routineId: Long) {
        db.routineDao().deleteRoutineExercises(routineId)
        db.routineDao().deleteRoutine(routineId)
    }

    suspend fun exercisesForRoutine(routineId: Long): List<RoutineExercise> = db.routineDao().exercisesForRoutine(routineId)
}
