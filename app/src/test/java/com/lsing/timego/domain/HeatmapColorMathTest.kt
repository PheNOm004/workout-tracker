package com.lsing.timego.domain

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapColorMathTest {
    @Test
    fun `light and dark shades differ from each other and from the input`() {
        val (light, dark) = habitHeatmapColorHexes("#3F51B5")
        assertNotEquals(light, dark)
        assertNotEquals("#3F51B5", light)
        assertNotEquals("#3F51B5", dark)
    }

    @Test
    fun `output is always a valid 6-digit hex color`() {
        val hexPattern = Regex("^#[0-9A-Fa-f]{6}$")
        val (light, dark) = habitHeatmapColorHexes("#FF7A3D")
        assertTrue(hexPattern.matches(light))
        assertTrue(hexPattern.matches(dark))
    }

    @Test
    fun `dark shade is darker than light shade for a saturated color`() {
        val (light, dark) = habitHeatmapColorHexes("#1B7F3F")
        fun luma(hex: String): Int {
            val r = hex.substring(1, 3).toInt(16)
            val g = hex.substring(3, 5).toInt(16)
            val b = hex.substring(5, 7).toInt(16)
            return (r + g + b) / 3
        }
        assertTrue(luma(dark) < luma(light))
    }
}
