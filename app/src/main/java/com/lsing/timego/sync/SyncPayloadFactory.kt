package com.lsing.timego.sync

import com.lsing.timego.data.BodyMetric
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.Routine
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.TimeGoDatabase
import com.lsing.timego.data.WorkoutSession

data class SyncDocument(val metadata: SyncMetadata, val payload: Map<String, Any?>?)

class SyncPayloadFactory(private val database: TimeGoDatabase) {
    suspend fun build(item: SyncMetadata): SyncDocument {
        if (item.operation == SyncOperation.DELETE.name) return SyncDocument(item, null)
        val payload = when (item.domainType) {
            "exercise" -> database.exerciseDao().getById(item.localId)?.takeIf(Exercise::isCustom)?.toSyncPayload()
            "session" -> database.sessionDao().getById(item.localId)?.toSyncPayload(database)
            "set_log" -> database.setLogDao().getById(item.localId)?.toSyncPayload(database)
            "routine" -> database.routineDao().getRoutineById(item.localId)?.toSyncPayload(database)
            "body_metric" -> database.bodyMetricDao().getById(item.localId)?.toSyncPayload()
            else -> null
        }
        return SyncDocument(item, payload)
    }
}

private fun Exercise.toSyncPayload() = mapOf("schemaVersion" to 1, "name" to name, "muscleGroups" to muscleGroups, "category" to category, "loggingType" to loggingType, "muscleWeights" to muscleWeights)

private suspend fun WorkoutSession.toSyncPayload(database: TimeGoDatabase): Map<String, Any?> = mapOf(
    "schemaVersion" to 1, "date" to date.toString(), "startEpochMillis" to startEpochMillis, "endEpochMillis" to endEpochMillis,
    "routineUuid" to routineId?.let { database.syncDao().find("routine", it)?.stableUuid },
)

private suspend fun SetLog.toSyncPayload(database: TimeGoDatabase): Map<String, Any?> {
    val exercise = database.exerciseDao().getById(exerciseId)
    val exerciseIdentity = exercise?.catalogueKey ?: database.syncDao().find("exercise", exerciseId)?.stableUuid
    return mapOf(
        "schemaVersion" to 1, "sessionUuid" to database.syncDao().find("session", sessionId)?.stableUuid,
        "exerciseIdentity" to exerciseIdentity, "weightKg" to weightKg, "reps" to reps, "targetReps" to targetReps,
        "loggedAtEpochMillis" to loggedAtEpochMillis, "durationMinutes" to durationMinutes, "distanceKm" to distanceKm,
        "holdSeconds" to holdSeconds, "targetHoldSeconds" to targetHoldSeconds, "isWarmup" to isWarmup,
        "addedWeightKg" to addedWeightKg, "rpe" to rpe, "targetProvenance" to targetProvenance,
    )
}

private suspend fun Routine.toSyncPayload(database: TimeGoDatabase): Map<String, Any?> {
    val identities = database.routineDao().exercisesForRoutine(id).mapNotNull { link ->
        val exercise = database.exerciseDao().getById(link.exerciseId)
        (exercise?.catalogueKey ?: database.syncDao().find("exercise", link.exerciseId)?.stableUuid)?.let { identity ->
            mapOf("exerciseIdentity" to identity, "orderIndex" to link.orderIndex)
        }
    }
    return mapOf("schemaVersion" to 1, "name" to name, "daysOfWeek" to daysOfWeek, "exercises" to identities)
}

private fun BodyMetric.toSyncPayload() = mapOf("schemaVersion" to 1, "date" to date.toString(), "weightKg" to weightKg, "waistCm" to waistCm, "heightCm" to heightCm)
