package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardioMathTest {
    @Test
    fun `estimatedCalorieBurn applies MET times weight times hours`() {
        val result = estimatedCalorieBurn(met = 8.0, bodyWeightKg = 75.0, durationMinutes = 30.0)
        assertEquals(300.0, result, 0.001)
    }

    @Test
    fun `averagePaceMinPerKm divides duration by distance`() {
        val result = averagePaceMinPerKm(durationMinutes = 30.0, distanceKm = 5.0)
        assertEquals(6.0, result!!, 0.001)
    }

    @Test
    fun `averagePaceMinPerKm returns null with zero distance`() {
        assertNull(averagePaceMinPerKm(durationMinutes = 30.0, distanceKm = 0.0))
    }
}
