package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverloadSuggesterTest {
    private val suggester = RuleBasedOverloadSuggester()

    @Test
    fun `no history returns null`() {
        assertNull(suggester.suggestNext(emptyList()))
    }

    @Test
    fun `hit target reps suggests weight increase`() {
        val history = listOf(SetPerformance(weightKg = 60.0, reps = 8, targetReps = 8))
        val result = suggester.suggestNext(history)
        assertEquals(62.5, result!!.weightKg, 0.001)
        assertEquals(8, result.reps)
    }

    @Test
    fun `missed target reps suggests same weight plus a rep`() {
        val history = listOf(SetPerformance(weightKg = 60.0, reps = 6, targetReps = 8))
        val result = suggester.suggestNext(history)
        assertEquals(60.0, result!!.weightKg, 0.001)
        assertEquals(7, result.reps)
    }

    @Test
    fun `missed target twice in a row triggers deload`() {
        val history = listOf(
            SetPerformance(weightKg = 60.0, reps = 5, targetReps = 8),
            SetPerformance(weightKg = 60.0, reps = 6, targetReps = 8),
        )
        val result = suggester.suggestNext(history)
        assertEquals(54.0, result!!.weightKg, 0.001)
        assertEquals("Deload: missed target reps twice in a row", result.note)
    }
}
