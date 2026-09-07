package com.lsing.timego.ui.common

import com.lsing.timego.data.MuscleGroup
import org.junit.Assert.*
import org.junit.Test

class MuscleBodyArtTest {
    @Test
    fun `every anatomical group has drawable coverage on both sides of the body`() {
        val all = FRONT_BODY_PATHS + BACK_BODY_PATHS
        val expected = MuscleGroup.entries.toSet() - MuscleGroup.FULL_BODY
        assertEquals(expected, all.mapNotNull { it.muscleGroup }.toSet())
        for (group in expected) {
            val front = FRONT_BODY_PATHS.filter { it.muscleGroup == group }
            val back = BACK_BODY_PATHS.filter { it.muscleGroup == group }
            assertTrue("$group needs bilateral geometry",
                (front.any { it.bounds[2] < 471f } && front.any { it.bounds[0] > 471f }) ||
                    (back.any { it.bounds[2] < 1060f } && back.any { it.bounds[0] > 1060f }))
        }
    }

    @Test
    fun `all crop bounds are finite nonempty and contained by aligned full body viewports`() {
        for ((specs, viewBox) in listOf(FRONT_BODY_PATHS to FRONT_BODY_VIEWBOX, BACK_BODY_PATHS to BACK_BODY_VIEWBOX)) {
            specs.forEach { spec ->
                val b = spec.bounds
                assertEquals(4, b.size)
                assertTrue(b.all { it.isFinite() })
                assertTrue(b[0] < b[2] && b[1] < b[3])
                assertTrue(b[0] >= viewBox[0] && b[1] >= viewBox[1])
                assertTrue(b[2] <= viewBox[2] && b[3] <= viewBox[3])
                assertFalse(spec.isOutline && spec.muscleGroup != null)
            }
        }
        assertEquals(FRONT_BODY_VIEWBOX[3] - FRONT_BODY_VIEWBOX[1], BACK_BODY_VIEWBOX[3] - BACK_BODY_VIEWBOX[1], 0f)
        assertEquals(FRONT_BODY_VIEWBOX[2] - FRONT_BODY_VIEWBOX[0], BACK_BODY_VIEWBOX[2] - BACK_BODY_VIEWBOX[0], 0f)
    }

    @Test
    fun `cropping includes every component plus consistent padding`() {
        assertNull(muscleCropBounds(emptyList()))
        for (group in MuscleGroup.entries - MuscleGroup.FULL_BODY) {
            for (half in listOf(FRONT_BODY_PATHS, BACK_BODY_PATHS)) {
                val specs = half.filter { it.muscleGroup == group }
                if (specs.isEmpty()) continue
                val crop = muscleCropBounds(specs)!!
                specs.forEach {
                    assertTrue(it.bounds[0] >= crop[0] + 19.99f)
                    assertTrue(it.bounds[1] >= crop[1] + 19.99f)
                    assertTrue(it.bounds[2] <= crop[2] - 19.99f)
                    assertTrue(it.bounds[3] <= crop[3] - 19.99f)
                }
            }
        }
    }

    @Test
    fun `head hands and feet have no training assignment and muscles have uniform source tone`() {
        for (half in listOf(FRONT_BODY_PATHS, BACK_BODY_PATHS)) {
            val muscles = half.filter { it.muscleGroup != null }
            assertTrue(muscles.all { it.bounds[1] >= 120f && it.bounds[3] < 800f })
            assertEquals(setOf(0.6f), muscles.map { it.lightness }.toSet())
        }
    }
}
