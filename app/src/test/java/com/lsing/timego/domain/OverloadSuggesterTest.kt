package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverloadSuggesterTest {
    private val suggester = RuleBasedOverloadSuggester()

    @Test
    fun `no history returns null`() {
        assertNull(suggester.suggestNext(emptyList(), emptyList()))
    }

    @Test
    fun `hit target reps suggests weight increase`() {
        val history = listOf(SetPerformance(weightKg = 60.0, reps = 8, targetReps = 8))
        val result = suggester.suggestNext(history, emptyList())
        assertEquals(62.5, result!!.weightKg, 0.001)
        assertEquals(8, result.reps)
        assertEquals(PlateauStatus.PROGRESSING, result.plateauStatus)
    }

    @Test
    fun `missed target reps suggests same weight plus a rep`() {
        val history = listOf(SetPerformance(weightKg = 60.0, reps = 6, targetReps = 8))
        val result = suggester.suggestNext(history, emptyList())
        assertEquals(60.0, result!!.weightKg, 0.001)
        assertEquals(7, result.reps)
        assertEquals(PlateauStatus.PROGRESSING, result.plateauStatus)
    }

    @Test
    fun `missed target twice in a row triggers deload`() {
        val history = listOf(
            SetPerformance(weightKg = 60.0, reps = 5, targetReps = 8),
            SetPerformance(weightKg = 60.0, reps = 6, targetReps = 8),
        )
        val result = suggester.suggestNext(history, emptyList())
        assertEquals(54.0, result!!.weightKg, 0.001)
        assertEquals("Deload: missed target reps twice in a row", result.note)
        assertEquals(PlateauStatus.REGRESSING, result.plateauStatus)
    }

    @Test
    fun `five sets flat oscillating with last set hit target is PLATEAUING and holds weight`() {
        // Estimated 1RM (Epley, weightKg * (1 + reps/30)) alternates 76.0 / 79.1667 / 76.0 /
        // 79.1667 / 76.0 -- every set hits its target (reps == targetReps), so REGRESSING can't
        // trigger, but the window has no net up/down trend (first-half and second-half averages
        // are equal) and the last value (76.0) sits below the preceding average (~77.58).
        val history = listOf(
            SetPerformance(weightKg = 60.0, reps = 8, targetReps = 8),
            SetPerformance(weightKg = 62.5, reps = 8, targetReps = 8),
            SetPerformance(weightKg = 60.0, reps = 8, targetReps = 8),
            SetPerformance(weightKg = 62.5, reps = 8, targetReps = 8),
            SetPerformance(weightKg = 60.0, reps = 8, targetReps = 8),
        )
        val result = suggester.suggestNext(history, emptyList())
        assertEquals(PlateauStatus.PLATEAUING, result!!.plateauStatus)
        assertEquals(60.0, result.weightKg, 0.001)
        assertEquals(8, result.reps)
        assertEquals(true, result.note.contains("plateau", ignoreCase = true))
    }

    @Test
    fun `current session already has a working set locks suggestion to its first entry`() {
        // This sessionHistory would trigger REGRESSING (last two missed target) if it were
        // consulted -- confirms the lock branch short-circuits the decision table entirely rather
        // than merely overriding its numeric output.
        val sessionHistory = listOf(
            SetPerformance(weightKg = 60.0, reps = 5, targetReps = 8),
            SetPerformance(weightKg = 60.0, reps = 6, targetReps = 8),
        )
        val currentSessionWorkingSets = listOf(
            SetPerformance(weightKg = 65.0, reps = 8, targetReps = 8),
            SetPerformance(weightKg = 70.0, reps = 8, targetReps = 8), // later set -- must be ignored
        )
        val result = suggester.suggestNext(sessionHistory, currentSessionWorkingSets)
        assertEquals(65.0, result!!.weightKg, 0.001)
        assertEquals(8, result.reps)
        assertEquals(PlateauStatus.REPEATING, result.plateauStatus)
        assertEquals("Repeating today's working weight", result.note)
    }

    @Test
    fun `both histories empty returns null`() {
        assertNull(suggester.suggestNext(emptyList(), emptyList()))
    }
}
