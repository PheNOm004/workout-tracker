package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightStepTest {
    @Test
    fun defaultsFollowEquipmentAndOverrideWins() {
        assertEquals(1.0, weightStepKg("Incline Dumbbell Press", false, null), 0.0)
        assertEquals(1.0, weightStepKg("Hammer Curl", true, null), 0.0)
        assertEquals(2.5, weightStepKg("Barbell Row", false, null), 0.0)
        assertEquals(5.0, weightStepKg("Incline Dumbbell Press", false, 5.0), 0.0)
    }

    @Test
    fun cyclingWraps() {
        assertEquals(1.25, nextWeightStep(1.0), 0.0)
        assertEquals(1.0, nextWeightStep(5.0), 0.0)
    }
}
