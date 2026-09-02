package com.lsing.timego.data

import org.junit.Assert.assertThrows
import org.junit.Test

class PersistenceValidationTest {
    @Test
    fun validEntriesAreAcceptedIncludingAssistedBodyweight() {
        requireValidWeightRepsLog(
            sessionId = 1,
            exerciseId = 2,
            weightKg = 55.0,
            reps = 8,
            targetReps = 10,
            addedWeightKg = -15.0,
            rpe = 8,
            targetProvenance = TargetProvenance.OVERLOAD_SUGGESTION.name,
        )
        requireValidCardioLog(1, 2, 30.0, null)
        requireValidHoldLog(1, 2, 45, 60, TargetProvenance.UNKNOWN.name)
        requireValidBodyMetric(weightKg = null, waistCm = 82.5, heightCm = null)
    }

    @Test
    fun invalidWeightRepEntriesAreRejected() {
        assertInvalid { validWeightReps(sessionId = 0) }
        assertInvalid { validWeightReps(exerciseId = 0) }
        assertInvalid { validWeightReps(weightKg = Double.NaN) }
        assertInvalid { validWeightReps(weightKg = Double.POSITIVE_INFINITY) }
        assertInvalid { validWeightReps(weightKg = 0.0) }
        assertInvalid { validWeightReps(reps = 0) }
        assertInvalid { validWeightReps(targetReps = 0) }
        assertInvalid { validWeightReps(addedWeightKg = Double.NEGATIVE_INFINITY) }
        assertInvalid { validWeightReps(rpe = 11) }
        assertInvalid { validWeightReps(targetProvenance = "UNRECOGNISED") }
    }

    @Test
    fun invalidCardioAndHoldEntriesAreRejected() {
        assertInvalid { requireValidCardioLog(1, 2, 0.0, null) }
        assertInvalid { requireValidCardioLog(1, 2, Double.NaN, null) }
        assertInvalid { requireValidCardioLog(1, 2, 30.0, -1.0) }
        assertInvalid { requireValidCardioLog(1, 2, 30.0, Double.POSITIVE_INFINITY) }
        assertInvalid { requireValidHoldLog(1, 2, 0, 60, TargetProvenance.UNKNOWN.name) }
        assertInvalid { requireValidHoldLog(1, 2, 45, 0, TargetProvenance.UNKNOWN.name) }
        assertInvalid { requireValidHoldLog(1, 2, 45, 60, "UNRECOGNISED") }
    }

    @Test
    fun emptyOrNonFiniteBodyMetricsAreRejected() {
        assertInvalid { requireValidBodyMetric(null, null, null) }
        assertInvalid { requireValidBodyMetric(0.0, null, null) }
        assertInvalid { requireValidBodyMetric(Double.NaN, null, null) }
        assertInvalid { requireValidBodyMetric(null, Double.POSITIVE_INFINITY, null) }
        assertInvalid { requireValidBodyMetric(null, null, -180.0) }
    }

    private fun validWeightReps(
        sessionId: Long = 1,
        exerciseId: Long = 2,
        weightKg: Double = 50.0,
        reps: Int = 8,
        targetReps: Int = 8,
        addedWeightKg: Double? = null,
        rpe: Int? = null,
        targetProvenance: String = TargetProvenance.UNKNOWN.name,
    ) = requireValidWeightRepsLog(
        sessionId,
        exerciseId,
        weightKg,
        reps,
        targetReps,
        addedWeightKg,
        rpe,
        targetProvenance,
    )

    private fun assertInvalid(block: () -> Unit) {
        assertThrows(IllegalArgumentException::class.java, block)
    }
}
