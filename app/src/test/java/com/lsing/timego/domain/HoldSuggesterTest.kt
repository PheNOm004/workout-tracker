package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HoldSuggesterTest {
    private val suggester = RuleBasedHoldSuggester()

    @Test
    fun `no history returns null`() {
        assertNull(suggester.suggestNext(emptyList()))
    }

    @Test
    fun `hit target hold suggests a longer duration`() {
        val history = listOf(HoldPerformance(durationSeconds = 30, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history)
        assertEquals(35, result!!.targetDurationSeconds)
    }

    @Test
    fun `missed target hold suggests the same target`() {
        val history = listOf(HoldPerformance(durationSeconds = 20, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history)
        assertEquals(30, result!!.targetDurationSeconds)
    }

    @Test
    fun `missed target twice in a row triggers deload`() {
        val history = listOf(
            HoldPerformance(durationSeconds = 20, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 22, targetDurationSeconds = 30),
        )
        val result = suggester.suggestNext(history)
        assertEquals(27, result!!.targetDurationSeconds)
        assertEquals("Deload: missed target hold twice in a row", result.note)
    }
}
