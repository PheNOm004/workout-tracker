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
    fun `upper body region crop excludes lower body context`() {
        val groups = diagramGroupsForBodyRegionCrop(
            setOf(MuscleGroup.UPPER_BACK.name, MuscleGroup.FRONT_DELTS.name),
        )
        assertEquals(true, MuscleGroup.CHEST.name in groups)
        assertEquals(true, MuscleGroup.BICEPS.name in groups)
        assertEquals(false, MuscleGroup.QUADS.name in groups)
        assertEquals(false, MuscleGroup.CALVES.name in groups)
    }

    @Test
    fun `mixed body region crop includes coherent full body context`() {
        val groups = diagramGroupsForBodyRegionCrop(
            setOf(MuscleGroup.CHEST.name, MuscleGroup.QUADS.name),
        )

        assertEquals(true, MuscleGroup.BICEPS.name in groups)
        assertEquals(true, MuscleGroup.HAMSTRINGS.name in groups)
    }

    @Test
    fun `recommendation hierarchy distinguishes primary and secondary regions`() {
        val strengths = recommendationHighlightStrengths(
            listOf(MuscleGroup.CHEST.name, MuscleGroup.TRICEPS.name),
        )

        assertEquals(1f, strengths[MuscleGroup.CHEST.name] ?: 0f, 0.001f)
        assertEquals(0.55f, strengths[MuscleGroup.TRICEPS.name] ?: 0f, 0.001f)
        assertEquals(0.55f, strengths[MuscleGroup.BICEPS.name] ?: 0f, 0.001f)
        assertEquals(false, MuscleGroup.QUADS.name in strengths)
    }

    @Test
    fun `primary emphasis wins when recommendation regions overlap`() {
        val strengths = recommendationHighlightStrengths(
            listOf(MuscleGroup.LATS.name, MuscleGroup.UPPER_BACK.name),
        )

        assertEquals(1f, strengths[MuscleGroup.LATS.name] ?: 0f, 0.001f)
        assertEquals(1f, strengths[MuscleGroup.UPPER_BACK.name] ?: 0f, 0.001f)
    }

}
