package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BodyMetricMathTest {
    @Test
    fun `bodyMassIndex divides weight by height in meters squared`() {
        // 70kg at 175cm -> 70 / 1.75^2 = 22.857...
        assertEquals(22.857, bodyMassIndex(weightKg = 70.0, heightCm = 175.0), 0.001)
    }

    @Test
    fun `latest height and weight ignore implausible typos`() {
        val d = java.time.LocalDate.of(2026, 10, 1)
        val metrics = listOf(
            com.lsing.timego.data.BodyMetric(date = d, weightKg = 59.8, waistCm = null, heightCm = 168.0),
            com.lsing.timego.data.BodyMetric(date = d.plusDays(1), weightKg = 5000.0, waistCm = null, heightCm = 1111111111.0),
        )

        assertEquals(168.0, latestHeightCm(metrics)!!, 0.0)
        assertEquals(59.8, latestWeightKg(metrics)!!, 0.0)
    }

    @Test
    fun `bmiCategory labels the standard WHO ranges`() {
        assertEquals(BmiCategory.UNDERWEIGHT, bmiCategory(18.0))
        assertEquals(BmiCategory.NORMAL, bmiCategory(22.0))
        assertEquals(BmiCategory.OVERWEIGHT, bmiCategory(27.0))
        assertEquals(BmiCategory.OBESE, bmiCategory(32.0))
    }
}
