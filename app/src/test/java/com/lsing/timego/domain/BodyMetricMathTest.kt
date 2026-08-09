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
    fun `bmiCategory labels the standard WHO ranges`() {
        assertEquals(BmiCategory.UNDERWEIGHT, bmiCategory(18.0))
        assertEquals(BmiCategory.NORMAL, bmiCategory(22.0))
        assertEquals(BmiCategory.OVERWEIGHT, bmiCategory(27.0))
        assertEquals(BmiCategory.OBESE, bmiCategory(32.0))
    }
}
