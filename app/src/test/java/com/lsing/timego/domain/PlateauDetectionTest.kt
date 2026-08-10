package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PlateauDetectionTest {
    @Test
    fun `fewer than 5 entries with last two missed is REGRESSING`() {
        val values = listOf(60.0, 60.0)
        val hits = listOf(false, false)
        assertEquals(PlateauStatus.REGRESSING, classifyPlateauStatus(values, hits))
    }

    @Test
    fun `fewer than 5 entries not both missed is PROGRESSING`() {
        val values = listOf(60.0)
        val hits = listOf(true)
        assertEquals(PlateauStatus.PROGRESSING, classifyPlateauStatus(values, hits))
    }

    @Test
    fun `fewer than 5 entries with one miss is PROGRESSING not REGRESSING`() {
        val values = listOf(60.0, 62.0)
        val hits = listOf(false, true)
        assertEquals(PlateauStatus.PROGRESSING, classifyPlateauStatus(values, hits))
    }

    @Test
    fun `five entries with clear uptrend is PROGRESSING`() {
        val values = listOf(60.0, 61.0, 62.0, 63.0, 65.0)
        val hits = listOf(true, true, true, true, true)
        assertEquals(PlateauStatus.PROGRESSING, classifyPlateauStatus(values, hits))
    }

    @Test
    fun `five entries flat with last two missed is REGRESSING`() {
        val values = listOf(60.0, 60.0, 60.0, 58.0, 57.0)
        val hits = listOf(true, true, true, false, false)
        assertEquals(PlateauStatus.REGRESSING, classifyPlateauStatus(values, hits))
    }

    @Test
    fun `five entries flat oscillating with no clear trend is PLATEAUING`() {
        val values = listOf(60.0, 61.0, 59.0, 61.0, 60.0)
        val hits = listOf(true, true, true, true, true)
        assertEquals(PlateauStatus.PLATEAUING, classifyPlateauStatus(values, hits))
    }

    @Test
    fun `six entries only considers the last five`() {
        // Oldest value (50.0) would drag the average down if included -- it must be windowed out.
        val values = listOf(50.0, 60.0, 61.0, 59.0, 61.0, 60.0)
        val hits = listOf(true, true, true, true, true, true)
        assertEquals(PlateauStatus.PLATEAUING, classifyPlateauStatus(values, hits))
    }
}
