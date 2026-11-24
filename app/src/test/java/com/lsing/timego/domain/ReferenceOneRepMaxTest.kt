package com.lsing.timego.domain

import com.lsing.timego.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReferenceOneRepMaxTest {
    private val day = 86_400_000L
    private fun set(w: Double, r: Int, at: Long, warmup: Boolean = false) =
        SetLog(sessionId = 1, exerciseId = 1, weightKg = w, reps = r, targetReps = r, loggedAtEpochMillis = at, isWarmup = warmup)

    @Test fun bestPriorSetWins() =
        assertEquals(estimatedOneRepMax(100.0, 5), referenceOneRepMax(listOf(set(100.0, 5, 10 * day), set(80.0, 8, 11 * day)), 20 * day)!!, 1e-9)

    @Test fun excludesTheSetBeingEvaluatedAndLater() =
        assertNull(referenceOneRepMax(listOf(set(100.0, 5, 20 * day), set(120.0, 3, 21 * day)), 20 * day))

    @Test fun ignoresWarmupsSentinelsHighRepsAndOldSets() {
        val history = listOf(set(150.0, 3, 10 * day, warmup = true), set(0.0, 10, 10 * day), set(40.0, 20, 10 * day), set(200.0, 3, 1 * day))
        assertNull(referenceOneRepMax(history, 200 * day))
    }
}
