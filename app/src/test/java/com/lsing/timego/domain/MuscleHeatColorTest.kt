package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleHeatColorTest {
    @Test
    fun `heatColor at the extremes matches the legend's graphite and ember stops`() {
        assertEquals("#30383B", heatColor(0f))
        assertEquals("#FF6B5E", heatColor(1f))
    }

    @Test
    fun `heatColor clamps out-of-range intensities instead of extrapolating`() {
        assertEquals(heatColor(0f), heatColor(-5f))
        assertEquals(heatColor(1f), heatColor(5f))
    }

    @Test
    fun `heat scale exposes the same low-to-high stops used by the legend`() {
        assertEquals(listOf("#30383B", "#4A6A5D", "#9BD8B2", "#F2B866", "#FF6B5E"), heatStopHexes())
        assertEquals(heatStopHexes().first(), heatColor(0f))
        assertEquals(heatStopHexes().last(), heatColor(1f))
    }

    @Test
    fun `heatColor output is always a valid 6-digit hex color`() {
        val hexPattern = Regex("^#[0-9A-Fa-f]{6}$")
        listOf(0f, 0.1f, 0.35f, 0.5f, 0.72f, 0.99f, 1f).forEach {
            assertTrue(hexPattern.matches(heatColor(it)))
        }
    }

    @Test
    fun `heatColor is monotonically warmer -- red channel trends up as intensity rises`() {
        fun red(hex: String) = hex.substring(1, 3).toInt(16)
        assertTrue(red(heatColor(1f)) >= red(heatColor(0.6f)))
        assertTrue(red(heatColor(0.6f)) >= red(heatColor(0f)))
    }

    @Test
    fun `recolorByLightness keeps the heat hue but varies brightness with the target`() {
        val dim = recolorByLightness(heatColor(0.9f), targetLightness = 0.20f)
        val bright = recolorByLightness(heatColor(0.9f), targetLightness = 0.75f)
        fun luma(hex: String): Int {
            val r = hex.substring(1, 3).toInt(16)
            val g = hex.substring(3, 5).toInt(16)
            val b = hex.substring(5, 7).toInt(16)
            return (r + g + b) / 3
        }
        assertTrue(luma(dim) < luma(bright))
    }

    @Test
    fun `recolorByLightness output is always a valid 6-digit hex color`() {
        val hexPattern = Regex("^#[0-9A-Fa-f]{6}$")
        assertTrue(hexPattern.matches(recolorByLightness("#EC4899", 0.4f)))
        assertTrue(hexPattern.matches(recolorByLightness("#22D3EE", 0.6f)))
    }
}
