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

class WorkoutRepository(private val db: TimeGoDatabase) {
    val exercises: Flow<List<Exercise>> = db.exerciseDao().observeAll()
    val sessions: Flow<List<WorkoutSession>> = db.sessionDao().observeAll()
    val bodyMetrics: Flow<List<BodyMetric>> = db.bodyMetricDao().observeAll()
    val routines: Flow<List<Routine>> = db.routineDao().observeRoutines()
    val routineExercises: Flow<List<RoutineExercise>> = db.routineDao().observeRoutineExercises()
    val setLogs: Flow<List<SetLog>> = db.setLogDao().observeAll()

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
        return db.exerciseDao().insert(Exercise(name = name, muscleGroups = muscleGroups, isCustom = true, category = category, loggingType = loggingType))
    }

    suspend fun startSession(routineId: Long?): WorkoutSession {
        val now = System.currentTimeMillis()
        val session = WorkoutSession(date = LocalDate.now(), routineId = routineId, startEpochMillis = now, endEpochMillis = null)
        return session.copy(id = db.sessionDao().insert(session))
    }

    suspend fun endSession(sessionId: Long, endEpochMillis: Long) {
        db.sessionDao().closeSession(sessionId, endEpochMillis)
    }

    /** Deletes a closed session and every set logged into it. No FK cascade exists on
     *  [SetLog.sessionId], so both deletes are explicit, wrapped in one transaction so a crash
     *  mid-delete can't leave orphaned set_logs behind. Callers are responsible for only offering
     *  this on closed sessions (endEpochMillis != null) -- deleting the active session out from
     *  under an in-progress Log screen isn't a case this repository guards against. */
    suspend fun deleteSession(sessionId: Long) {
        db.withTransaction {
            db.setLogDao().deleteForSession(sessionId)
            db.sessionDao().delete(sessionId)
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
                rpe = rpe,
                targetProvenance = targetProvenance,
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

    suspend fun logHoldSet(
        sessionId: Long,
        exerciseId: Long,
        durationSeconds: Int,
        targetDurationSeconds: Int,
        isWarmup: Boolean = false,
        targetProvenance: String = TargetProvenance.UNKNOWN.name,
    ) {
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
                targetProvenance = targetProvenance,
            ),
        )
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
        db.bodyMetricDao().insert(BodyMetric(date = date, weightKg = weightKg, waistCm = waistCm, heightCm = heightCm))
    }

    suspend fun createRoutine(name: String, exerciseIds: List<Long>, daysOfWeek: List<String>): Long = db.withTransaction {
        val routineId = db.routineDao().insertRoutine(Routine(name = name, daysOfWeek = daysOfWeek))
        exerciseIds.forEachIndexed { index, exerciseId ->
            db.routineDao().insertRoutineExercise(RoutineExercise(routineId = routineId, exerciseId = exerciseId, orderIndex = index))
        }
        routineId
    }

    suspend fun deleteRoutine(routineId: Long) = db.withTransaction {
        db.routineDao().deleteRoutineExercises(routineId)
        db.routineDao().deleteRoutine(routineId)
    }

    suspend fun exercisesForRoutine(routineId: Long): List<RoutineExercise> = db.routineDao().exercisesForRoutine(routineId)
}
