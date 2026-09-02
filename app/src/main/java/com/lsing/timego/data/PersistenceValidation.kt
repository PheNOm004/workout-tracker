package com.lsing.timego.data

/** Last-line validation for values crossing into Room. UI validation remains useful feedback, but
 * repository callers must not be able to persist non-finite or structurally invalid history. */
internal fun requireValidWeightRepsLog(
    sessionId: Long,
    exerciseId: Long,
    weightKg: Double,
    reps: Int,
    targetReps: Int,
    addedWeightKg: Double?,
    rpe: Int?,
    targetProvenance: String,
) {
    requireValidLogReferences(sessionId, exerciseId)
    require(weightKg.isFinite() && weightKg > 0.0) { "Weight must be finite and positive" }
    require(reps > 0) { "Reps must be positive" }
    require(targetReps > 0) { "Target reps must be positive" }
    require(addedWeightKg == null || addedWeightKg.isFinite()) { "Added weight must be finite" }
    require(rpe == null || rpe in 1..10) { "RPE must be between 1 and 10" }
    requireValidTargetProvenance(targetProvenance)
}

internal fun requireValidCardioLog(
    sessionId: Long,
    exerciseId: Long,
    durationMinutes: Double,
    distanceKm: Double?,
) {
    requireValidLogReferences(sessionId, exerciseId)
    require(durationMinutes.isFinite() && durationMinutes > 0.0) { "Duration must be finite and positive" }
    require(distanceKm == null || distanceKm.isFinite() && distanceKm > 0.0) {
        "Distance must be finite and positive when provided"
    }
}

internal fun requireValidHoldLog(
    sessionId: Long,
    exerciseId: Long,
    durationSeconds: Int,
    targetDurationSeconds: Int,
    targetProvenance: String,
) {
    requireValidLogReferences(sessionId, exerciseId)
    require(durationSeconds > 0) { "Hold duration must be positive" }
    require(targetDurationSeconds > 0) { "Target hold duration must be positive" }
    requireValidTargetProvenance(targetProvenance)
}

internal fun requireValidBodyMetric(weightKg: Double?, waistCm: Double?, heightCm: Double?) {
    require(weightKg != null || waistCm != null || heightCm != null) {
        "At least one body measurement is required"
    }
    require(weightKg == null || weightKg.isFinite() && weightKg > 0.0) {
        "Body weight must be finite and positive when provided"
    }
    require(waistCm == null || waistCm.isFinite() && waistCm > 0.0) {
        "Waist measurement must be finite and positive when provided"
    }
    require(heightCm == null || heightCm.isFinite() && heightCm > 0.0) {
        "Height must be finite and positive when provided"
    }
}

private fun requireValidLogReferences(sessionId: Long, exerciseId: Long) {
    require(sessionId > 0) { "Session ID must be positive" }
    require(exerciseId > 0) { "Exercise ID must be positive" }
}

private fun requireValidTargetProvenance(value: String) {
    require(TargetProvenance.entries.any { it.name == value }) { "Unknown target provenance" }
}
