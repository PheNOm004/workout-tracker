package com.lsing.timego.domain

import com.lsing.timego.data.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagramMuscleZoneTest {
    @Test
    fun `non-delt group resolves to its own intensity`() {
        val intensities = mapOf(MuscleGroup.CHEST.name to 0.8f)
        assertEquals(0.8f, diagramZoneIntensity(MuscleGroup.CHEST, intensities), 0.001f)
    }

    @Test
    fun `group missing from intensities resolves to zero`() {
        assertEquals(0f, diagramZoneIntensity(MuscleGroup.QUADS, emptyMap()), 0.001f)
    }

    @Test
    fun `any delt group resolves to the average of all three delt intensities`() {
        val intensities = mapOf(
            MuscleGroup.FRONT_DELTS.name to 0.9f,
            MuscleGroup.SIDE_DELTS.name to 0.3f,
            MuscleGroup.REAR_DELTS.name to 0.0f,
        )
        val expected = (0.9f + 0.3f + 0.0f) / 3f
        assertEquals(expected, diagramZoneIntensity(MuscleGroup.FRONT_DELTS, intensities), 0.001f)
        assertEquals(expected, diagramZoneIntensity(MuscleGroup.SIDE_DELTS, intensities), 0.001f)
        assertEquals(expected, diagramZoneIntensity(MuscleGroup.REAR_DELTS, intensities), 0.001f)
    }

    @Test
    fun `delt group with no logged intensities resolves to zero`() {
        assertEquals(0f, diagramZoneIntensity(MuscleGroup.REAR_DELTS, emptyMap()), 0.001f)
    }
}
