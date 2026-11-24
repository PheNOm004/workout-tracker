package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadinessTest {
    private val raise = OverloadSuggestion(weightKg = 62.5, reps = 8, note = "Target reps achieved", plateauStatus = PlateauStatus.PROGRESSING)

    @Test fun lowReadinessHoldsTheWeightAndSaysSo() {
        val result = applyReadiness(raise, lastWorkingKg = 60.0, readiness = 2)
        assertEquals(60.0, result.weightKg, 1e-9)
        assertTrue(result.note.startsWith("Low readiness today"))
        assertEquals(8, result.reps)
    }

    @Test fun highOrMissingReadinessChangesNothing() {
        assertEquals(raise, applyReadiness(raise, 60.0, 3))
        assertEquals(raise, applyReadiness(raise, 60.0, 5))
        assertEquals(raise, applyReadiness(raise, 60.0, null))
    }

    @Test fun nothingToHoldAgainstOrNoRaiseLeavesItAlone() {
        assertEquals(raise, applyReadiness(raise, null, 1))
        assertEquals(raise, applyReadiness(raise, 62.5, 1))
        assertEquals(raise, applyReadiness(raise, 70.0, 1))
    }

    @Test fun lowReadinessAddsFifteenSecondsOfRest() {
        assertEquals(135, readinessRestSeconds(120, 1))
        assertEquals(135, readinessRestSeconds(120, 2))
        assertEquals(120, readinessRestSeconds(120, 3))
        assertEquals(120, readinessRestSeconds(120, null))
        assertTrue(readinessIsLow(2))
        assertFalse(readinessIsLow(4))
    }
}
