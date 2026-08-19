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
    fun `delt artwork resolves to its own muscle head`() {
        val intensities = mapOf(
            MuscleGroup.FRONT_DELTS.name to 0.9f,
            MuscleGroup.SIDE_DELTS.name to 0.3f,
            MuscleGroup.REAR_DELTS.name to 0.0f,
        )
        assertEquals(0.9f, diagramZoneIntensity(MuscleGroup.FRONT_DELTS, intensities), 0.001f)
        assertEquals(0.3f, diagramZoneIntensity(MuscleGroup.SIDE_DELTS, intensities), 0.001f)
        assertEquals(0.0f, diagramZoneIntensity(MuscleGroup.REAR_DELTS, intensities), 0.001f)
    }

    @Test
    fun `delt group with no logged intensities resolves to zero`() {
        assertEquals(0f, diagramZoneIntensity(MuscleGroup.REAR_DELTS, emptyMap()), 0.001f)
    }

    @Test
    fun `full body intensity is a fallback for every drawable group`() {
        val intensities = mapOf(MuscleGroup.FULL_BODY.name to 0.7f)
        assertEquals(0.7f, diagramZoneIntensity(MuscleGroup.CHEST, intensities), 0.001f)
        assertEquals(0.7f, diagramZoneIntensity(MuscleGroup.ADDUCTORS, intensities), 0.001f)
    }

    @Test
    fun `full body expands to all anatomical groups for cropped diagrams`() {
        val groups = diagramGroupsForHeatmap(setOf(MuscleGroup.FULL_BODY.name))
        assertEquals(true, MuscleGroup.CHEST.name in groups)
        assertEquals(true, MuscleGroup.ADDUCTORS.name in groups)
        assertEquals(true, MuscleGroup.FULL_BODY.name in groups)
    }

    @Test
    fun `upper body recommendation crop excludes lower body context`() {
        val groups = diagramGroupsForRecommendationCrop(
            setOf(MuscleGroup.UPPER_BACK.name, MuscleGroup.FRONT_DELTS.name),
        )
        assertEquals(true, MuscleGroup.CHEST.name in groups)
        assertEquals(true, MuscleGroup.BICEPS.name in groups)
        assertEquals(false, MuscleGroup.QUADS.name in groups)
        assertEquals(false, MuscleGroup.CALVES.name in groups)
    }

}
