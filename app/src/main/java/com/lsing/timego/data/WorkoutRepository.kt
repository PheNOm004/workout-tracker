package com.lsing.timego.data

import androidx.room.withTransaction
import com.lsing.timego.data.adaptive.ShadowAuditEntity
import com.lsing.timego.data.adaptive.ShadowCacheKey
import com.lsing.timego.data.adaptive.ShadowCacheCompatibility
import com.lsing.timego.data.adaptive.ShadowCacheIdentity
import com.lsing.timego.data.adaptive.ShadowCachePipeline
import com.lsing.timego.data.adaptive.ShadowCacheWrite
import com.lsing.timego.data.adaptive.ShadowCacheWriteDecision
import com.lsing.timego.data.adaptive.ShadowCacheWriteDisposition
import com.lsing.timego.data.adaptive.ShadowCacheWritePolicy
import com.lsing.timego.data.adaptive.ShadowRebuildStatus
import com.lsing.timego.data.adaptive.ShadowSnapshot
import com.lsing.timego.data.adaptive.ShadowSnapshotEntity
import com.lsing.timego.data.adaptive.ShadowSourceFingerprint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import com.lsing.timego.sync.SyncRepository

class WorkoutRepository(private val db: TimeGoDatabase) {
    private val syncRepository = SyncRepository(db)
    val exercises: Flow<List<Exercise>> = db.exerciseDao().observeAll()
    val sessions: Flow<List<WorkoutSession>> = db.sessionDao().observeAll()
    val bodyMetrics: Flow<List<BodyMetric>> = db.bodyMetricDao().observeAll()
    val routines: Flow<List<Routine>> = db.routineDao().observeRoutines()
    val routineExercises: Flow<List<RoutineExercise>> = db.routineDao().observeRoutineExercises()
    val setLogs: Flow<List<SetLog>> = db.setLogDao().observeAll()

    /** Permanently removes local workout content after the user explicitly chooses that option
     * during account deletion. Cloud deletion is performed first by the caller; this method only
     * touches the local Room database and deliberately leaves bundled guidance available. */
    suspend fun clearLocalWorkoutData() {
        db.withTransaction {
            db.openHelper.writableDatabase.execSQL("DELETE FROM set_logs")
            db.openHelper.writableDatabase.execSQL("DELETE FROM workout_sessions")
            db.openHelper.writableDatabase.execSQL("DELETE FROM routine_exercises")
            db.openHelper.writableDatabase.execSQL("DELETE FROM routines")
            db.openHelper.writableDatabase.execSQL("DELETE FROM body_metrics")
            db.openHelper.writableDatabase.execSQL("DELETE FROM exercises WHERE isCustom = 1")
            db.openHelper.writableDatabase.execSQL("DELETE FROM sync_metadata")
        }
    }

    /** Inserts any [seed] exercise whose name isn't already present -- NOT gated on the table
     *  being totally empty, since expanding the seed list (Update 1.1: 12 -> 119) must still
     *  reach devices that already have some exercises logged. Matches by name rather than id,
     *  since seed entries have no stable id across app versions. Also syncs curated seed metadata
     *  for existing non-custom rows, so corrected muscle tags and weights reach an already-used
     *  install instead of remaining stale forever. Custom exercises are never overwritten. */
    suspend fun seedMissingExercises(seed: List<Exercise>) {
        val existingByName = exercises.first().associateBy { it.name }
        val missing = seed.filter { it.name !in existingByName.keys }
        // Collected then written once. Issuing one update() per corrected row meant up to one
        // transaction per seed entry at startup, each invalidating the exercises Flow and so
        // re-running every downstream collector (suggestions, landing summary) mid-seed.
        val corrections = seed.mapNotNull { seedExercise ->
            val existing = existingByName[seedExercise.name] ?: return@mapNotNull null
            if (existing.isCustom) return@mapNotNull null
            val corrected = existing.copy(
                muscleGroups = seedExercise.muscleGroups,
                catalogueKey = seedExercise.catalogueKey,
                category = seedExercise.category,
                loggingType = seedExercise.loggingType,
                muscleWeights = seedExercise.muscleWeights,
            )
            corrected.takeIf { it != existing }
        }
        if (missing.isEmpty() && corrections.isEmpty()) return
        db.withTransaction {
            if (missing.isNotEmpty()) db.exerciseDao().insertAll(missing)
            if (corrections.isNotEmpty()) db.exerciseDao().updateAll(corrections)
        }
    }

    suspend fun addCustomExercise(name: String, muscleGroups: List<String>, category: String): Long {
        val loggingType = if (category == ExerciseCategory.CARDIO.name || category == ExerciseCategory.WARMUP.name) {
            LoggingType.DURATION_DISTANCE.name
        } else {
            LoggingType.WEIGHT_REPS.name
        }
        return db.withTransaction {
            val id = db.exerciseDao().insert(Exercise(name = name, muscleGroups = muscleGroups, isCustom = true, category = category, loggingType = loggingType))
            syncRepository.markPending("exercise", id)
            id
        }
    }

    suspend fun startSession(routineId: Long?): WorkoutSession {
        val now = System.currentTimeMillis()
        val session = WorkoutSession(date = LocalDate.now(), routineId = routineId, startEpochMillis = now, endEpochMillis = null)
        return db.withTransaction {
            val id = db.sessionDao().insert(session)
            syncRepository.markPending("session", id)
            session.copy(id = id)
        }
    }

    suspend fun endSession(sessionId: Long, endEpochMillis: Long) {
        db.withTransaction {
            db.sessionDao().closeSession(sessionId, endEpochMillis)
            syncRepository.markPending("session", sessionId)
        }
    }

    /** Deletes a closed session and every set logged into it. No FK cascade exists on
     *  [SetLog.sessionId], so both deletes are explicit, wrapped in one transaction so a crash
     *  mid-delete can't leave orphaned set_logs behind. Callers are responsible for only offering
     *  this on closed sessions (endEpochMillis != null) -- deleting the active session out from
     *  under an in-progress Log screen isn't a case this repository guards against. */
    suspend fun deleteSession(sessionId: Long) {
        db.withTransaction {
            db.setLogDao().idsForSession(sessionId).forEach { syncRepository.markDeleted("set_log", it) }
            db.setLogDao().deleteForSession(sessionId)
            db.sessionDao().delete(sessionId)
            syncRepository.markDeleted("session", sessionId)
        }
    }

    suspend fun logSet(
        sessionId: Long,
        exerciseId: Long,
        weightKg: Double,
        reps: Int,
        targetReps: Int,
        isWarmup: Boolean = false,
        addedWeightKg: Double? = null,
        rpe: Int? = null,
        targetProvenance: String = TargetProvenance.UNKNOWN.name,
    ) {
        requireValidWeightRepsLog(
            sessionId = sessionId,
            exerciseId = exerciseId,
            weightKg = weightKg,
            reps = reps,
            targetReps = targetReps,
            addedWeightKg = addedWeightKg,
            rpe = rpe,
            targetProvenance = targetProvenance,
        )
        db.withTransaction {
        val id = db.setLogDao().insert(
            SetLog(
                sessionId = sessionId,
                exerciseId = exerciseId,
                weightKg = weightKg,
                reps = reps,
                targetReps = targetReps,
                loggedAtEpochMillis = System.currentTimeMillis(),
                isWarmup = isWarmup,
                addedWeightKg = addedWeightKg,
                rpe = rpe,
                targetProvenance = targetProvenance,
            ),
        )
        syncRepository.markPending("set_log", id)
        }
    }

    suspend fun logCardioSet(sessionId: Long, exerciseId: Long, durationMinutes: Double, distanceKm: Double?) {
        requireValidCardioLog(sessionId, exerciseId, durationMinutes, distanceKm)
        db.withTransaction {
        val id = db.setLogDao().insert(
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
        syncRepository.markPending("set_log", id)
        }
    }

    suspend fun logHoldSet(
        sessionId: Long,
        exerciseId: Long,
        durationSeconds: Int,
        targetDurationSeconds: Int,
        isWarmup: Boolean = false,
        targetProvenance: String = TargetProvenance.UNKNOWN.name,
    ) {
        requireValidHoldLog(sessionId, exerciseId, durationSeconds, targetDurationSeconds, targetProvenance)
        db.withTransaction {
        val id = db.setLogDao().insert(
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
                targetProvenance = targetProvenance,
            ),
        )
        syncRepository.markPending("set_log", id)
        }
    }

    /**
     * One atomic, read-only source snapshot for the hidden provisional shadow.  It does not
     * collect any existing UI Flows or alter the rule-based suggestion inputs.
     */
    suspend fun shadowSnapshot(): ShadowSnapshot = db.withTransaction {
        captureShadowSnapshot()
    }

    /** Runs the complete hidden rebuild from one atomic source snapshot through the checked writer. */
    suspend fun rebuildShadowCache(
        pipeline: ShadowCachePipeline,
        completedAtEpochMillis: Long = System.currentTimeMillis(),
    ): ShadowCacheWriteDecision {
        val captured = shadowSnapshot()
        return persistShadowCache(pipeline.build(captured, completedAtEpochMillis))
    }

    /**
     * Returns a cache only when current canonical source plus model, metadata, order, and status
     * all match. A delete or version change therefore becomes unusable before another rebuild.
     */
    suspend fun usableShadowCache(identity: ShadowCacheIdentity): ShadowSnapshotEntity? = db.withTransaction {
        val currentFingerprint = ShadowSourceFingerprint.from(captureShadowSnapshot())
        val requested = identity.forSource(currentFingerprint)
        db.shadowDao().snapshot()?.takeIf { persisted ->
            persisted.completionStatus == ShadowRebuildStatus.COMPLETED.name &&
                ShadowCacheCompatibility.isUsable(persisted.toCacheKey(), requested)
        }
    }

    /**
     * Atomically writes only a cache whose captured canonical source still matches. A changed or
     * deleted source invalidates the old derived payload and leaves an aggregate stale audit fact
     * so the hidden caller can capture again and rebuild without touching historic user rows.
     */
    suspend fun persistShadowCache(write: ShadowCacheWrite): ShadowCacheWriteDecision = db.withTransaction {
        val currentSnapshot = captureShadowSnapshot()
        val currentFingerprint = ShadowSourceFingerprint.from(currentSnapshot)
        val dao = db.shadowDao()
        val existing = dao.snapshot()?.toCacheKey()
        val decision = ShadowCacheWritePolicy.decide(
            captured = write.captured,
            currentSourceFingerprint = currentFingerprint,
            existing = existing,
        )
        if (decision.disposition == ShadowCacheWriteDisposition.STALE_DISCARDED) {
            dao.deleteSnapshot()
            dao.appendAudit(
                auditFor(
                    key = write.captured.copy(sourceFingerprint = currentFingerprint),
                    sourceRowCount = currentSnapshot.rows.size,
                    observationCount = 0,
                    exclusionCount = 0,
                    status = ShadowRebuildStatus.STALE_DISCARDED,
                    recordedAtEpochMillis = write.completedAtEpochMillis,
                ),
            )
            return@withTransaction decision
        }

        if (decision.disposition == ShadowCacheWriteDisposition.INVALIDATED) {
            dao.deleteSnapshot()
        }
        dao.upsertSnapshot(
            ShadowSnapshotEntity(
                sourceFingerprint = write.captured.sourceFingerprint,
                modelContractHash = write.captured.modelContractHash,
                metadataHash = write.captured.metadataHash,
                orderingPolicy = write.captured.orderingPolicy,
                statePayload = write.statePayload,
                sourceRowCount = currentSnapshot.rows.size,
                observationCount = write.observationCount,
                exclusionCount = write.exclusionCount,
                completionStatus = ShadowRebuildStatus.COMPLETED.name,
                completedAtEpochMillis = write.completedAtEpochMillis,
            ),
        )
        dao.appendAudit(
            auditFor(
                key = write.captured,
                sourceRowCount = currentSnapshot.rows.size,
                observationCount = write.observationCount,
                exclusionCount = write.exclusionCount,
                status = if (decision.disposition == ShadowCacheWriteDisposition.INVALIDATED) {
                    ShadowRebuildStatus.INVALIDATED
                } else {
                    ShadowRebuildStatus.COMPLETED
                },
                recordedAtEpochMillis = write.completedAtEpochMillis,
            ),
        )
        decision
    }

    private suspend fun captureShadowSnapshot(): ShadowSnapshot =
        ShadowSnapshot.from(
            sessions = db.sessionDao().allForShadowSnapshot(),
            setLogs = db.setLogDao().allForShadowSnapshot(),
            exercises = db.exerciseDao().allForShadowSnapshot(),
        )

    private fun ShadowSnapshotEntity.toCacheKey() = ShadowCacheKey(
        sourceFingerprint = sourceFingerprint,
        modelContractHash = modelContractHash,
        metadataHash = metadataHash,
        orderingPolicy = orderingPolicy,
    )

    private fun auditFor(
        key: ShadowCacheKey,
        sourceRowCount: Int,
        observationCount: Int,
        exclusionCount: Int,
        status: ShadowRebuildStatus,
        recordedAtEpochMillis: Long,
    ) = ShadowAuditEntity(
        sourceFingerprint = key.sourceFingerprint,
        modelContractHash = key.modelContractHash,
        metadataHash = key.metadataHash,
        orderingPolicy = key.orderingPolicy,
        sourceRowCount = sourceRowCount,
        observationCount = observationCount,
        exclusionCount = exclusionCount,
        rebuildStatus = status.name,
        recordedAtEpochMillis = recordedAtEpochMillis,
    )

    suspend fun logBodyMetric(date: LocalDate, weightKg: Double?, waistCm: Double?, heightCm: Double?) {
        requireValidBodyMetric(weightKg, waistCm, heightCm)
        db.withTransaction {
            val id = db.bodyMetricDao().insert(BodyMetric(date = date, weightKg = weightKg, waistCm = waistCm, heightCm = heightCm))
            syncRepository.markPending("body_metric", id)
        }
    }

    /** Inserts any [seed] routine not already present (matched by `(programId, name)`, mirroring
     *  [seedMissingExercises]'s name-matching convention) -- run once at startup, not gated on the
     *  table being empty, so a future SEED_ROUTINES expansion still reaches an already-used install.
     *  Also repairs any existing seeded routine that has zero exercises (e.g. an earlier install hit
     *  the exercise-lookup race this function used to have) by inserting its exercises now, without
     *  touching the routine row itself. Resolves each [SeedRoutine.exerciseNames] entry against the
     *  already-seeded exercise catalogue by exact name; a name with no catalogue match is skipped
     *  (not a hard failure) so a future catalogue rename can't brick every program's seeding in one
     *  go. Never touches user-created routines (programId == null). */
    suspend fun seedMissingRoutines(seed: List<SeedRoutine>) {
        // Direct one-shot DAO reads, not the exposed Flows -- seeding needs a guaranteed-fresh view
        // immediately after seedMissingExercises's transaction commits, not whatever the Flow has
        // invalidated to by the time it's collected (no synchronous guarantee on Room Flow
        // invalidation timing relative to the write that triggered it).
        val existingRoutines = db.routineDao().allRoutinesOnce()
        val existingByKey = existingRoutines.associateBy { it.programId to it.name }
        val existingExerciseIdsByRoutineId = db.routineDao().allRoutineExercisesOnce()
            .groupingBy { it.routineId }
            .fold(emptySet<Long>()) { acc, re -> acc + re.exerciseId }

        val exerciseIdsByName = db.exerciseDao().allForShadowSnapshot().associate { it.name to it.id }

        val missing = seed.filter { (it.programId to it.name) !in existingByKey }
        // A seeded routine is out of sync with its source (empty from the old seed/read-race bug, or
        // simply edited in SEED_ROUTINES since it was first seeded) whenever its stored exercise set no
        // longer matches what the current seed defines -- resync rather than only patching emptiness, so
        // a SEED_ROUTINES content change actually reaches devices that seeded before the change.
        val outOfSync = seed.mapNotNull { seedRoutine ->
            val existing = existingByKey[seedRoutine.programId to seedRoutine.name] ?: return@mapNotNull null
            val seedExerciseIds = seedRoutine.exerciseNames.mapNotNull { exerciseIdsByName[it] }.toSet()
            if (existingExerciseIdsByRoutineId[existing.id].orEmpty() == seedExerciseIds) return@mapNotNull null
            seedRoutine to existing.id
        }
        if (missing.isEmpty() && outOfSync.isEmpty()) return

        db.withTransaction {
            missing.forEach { seedRoutine ->
                val exerciseIds = seedRoutine.exerciseNames.mapNotNull { exerciseIdsByName[it] }
                if (exerciseIds.isEmpty()) return@forEach
                val routineId = db.routineDao().insertRoutine(
                    Routine(name = seedRoutine.name, programId = seedRoutine.programId, tier = seedRoutine.tier),
                )
                exerciseIds.forEachIndexed { index, exerciseId ->
                    db.routineDao().insertRoutineExercise(RoutineExercise(routineId = routineId, exerciseId = exerciseId, orderIndex = index))
                }
            }
            outOfSync.forEach { (seedRoutine, routineId) ->
                db.routineDao().deleteRoutineExercises(routineId)
                val exerciseIds = seedRoutine.exerciseNames.mapNotNull { exerciseIdsByName[it] }
                exerciseIds.forEachIndexed { index, exerciseId ->
                    db.routineDao().insertRoutineExercise(RoutineExercise(routineId = routineId, exerciseId = exerciseId, orderIndex = index))
                }
            }
        }
    }

    suspend fun createRoutine(name: String, exerciseIds: List<Long>, daysOfWeek: List<String>): Long = db.withTransaction {
        val routineId = db.routineDao().insertRoutine(Routine(name = name, daysOfWeek = daysOfWeek))
        exerciseIds.forEachIndexed { index, exerciseId ->
            db.routineDao().insertRoutineExercise(RoutineExercise(routineId = routineId, exerciseId = exerciseId, orderIndex = index))
        }
        syncRepository.markPending("routine", routineId)
        routineId
    }

    suspend fun deleteRoutine(routineId: Long) = db.withTransaction {
        db.routineDao().deleteRoutineExercises(routineId)
        db.routineDao().deleteRoutine(routineId)
        syncRepository.markDeleted("routine", routineId)
    }

    suspend fun exercisesForRoutine(routineId: Long): List<RoutineExercise> = db.routineDao().exercisesForRoutine(routineId)
}
