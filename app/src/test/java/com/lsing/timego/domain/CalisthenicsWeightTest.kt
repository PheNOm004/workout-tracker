package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CalisthenicsWeightTest {
    @Test
    fun `zero added weight formats as plain BW`() {
        assertEquals("BW", formatCalisthenicsWeight(0.0))
    }

    @Test
    fun `positive added weight formats as BW plus one decimal`() {
        assertEquals("BW + 2.5kg", formatCalisthenicsWeight(2.5))
    }

    @Test
    fun `negative added weight displays the same as zero`() {
        assertEquals("BW", formatCalisthenicsWeight(-1.0))
    }
}
