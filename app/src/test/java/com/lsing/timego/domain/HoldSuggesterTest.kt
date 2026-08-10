package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HoldSuggesterTest {
    private val suggester = RuleBasedHoldSuggester()

    @Test
    fun `no history returns null`() {
        assertNull(suggester.suggestNext(emptyList(), "Plank"))
    }

    @Test
    fun `hit target hold suggests a longer duration`() {
        val history = listOf(HoldPerformance(durationSeconds = 30, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, "Plank")
        assertEquals(35, result!!.targetDurationSeconds)
        assertEquals(PlateauStatus.PROGRESSING, result.plateauStatus)
    }

    @Test
    fun `missed target hold suggests the same target`() {
        val history = listOf(HoldPerformance(durationSeconds = 20, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, "Plank")
        assertEquals(30, result!!.targetDurationSeconds)
        assertEquals(PlateauStatus.PROGRESSING, result.plateauStatus)
    }

    @Test
    fun `missed target twice in a row triggers deload`() {
        val history = listOf(
            HoldPerformance(durationSeconds = 20, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 22, targetDurationSeconds = 30),
        )
        val result = suggester.suggestNext(history, "Plank")
        assertEquals(27, result!!.targetDurationSeconds)
        assertEquals("Deload: missed target hold twice in a row", result.note)
        assertEquals(PlateauStatus.REGRESSING, result.plateauStatus)
    }

    @Test
    fun `five holds flat oscillating with last hit target is PLATEAUING and holds duration`() {
        // Alternates 30 / 32 / 30 / 32 / 30 -- every hold clears its 30s target, so REGRESSING
        // can't trigger, but the window has no net trend (first-half/second-half averages equal)
        // and the last value (30) sits below the preceding average (31.0).
        val history = listOf(
            HoldPerformance(durationSeconds = 30, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 32, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 30, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 32, targetDurationSeconds = 30),
            HoldPerformance(durationSeconds = 30, targetDurationSeconds = 30),
        )
        val result = suggester.suggestNext(history, "Plank")
        assertEquals(PlateauStatus.PLATEAUING, result!!.plateauStatus)
        assertEquals(30, result.targetDurationSeconds)
        assertEquals(true, result.note.contains("plateau", ignoreCase = true))
    }

    @Test
    fun `hitting target well past the ceiling on a mapped exercise suggests the next tier`() {
        // 45s held against a 30s target is 1.5x -- the ceiling threshold.
        val history = listOf(HoldPerformance(durationSeconds = 45, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, "Tuck Planche Hold")
        assertEquals(true, result!!.note.contains("Advanced Tuck Planche Hold"))
    }

    @Test
    fun `hitting target without reaching the ceiling on a mapped exercise does not suggest next tier`() {
        val history = listOf(HoldPerformance(durationSeconds = 32, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, "Tuck Planche Hold")
        assertEquals(false, result!!.note.contains("Advanced Tuck Planche Hold"))
    }

    @Test
    fun `ceiling hit on an exercise with no known progression falls through to normal suggestion`() {
        val history = listOf(HoldPerformance(durationSeconds = 45, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, "Plank")
        assertEquals(35, result!!.targetDurationSeconds)
    }

    @Test
    fun `ceiling hit on the top tier of a progression chain falls through to normal suggestion`() {
        val history = listOf(HoldPerformance(durationSeconds = 45, targetDurationSeconds = 30))
        val result = suggester.suggestNext(history, "Full Planche Hold")
        assertEquals(35, result!!.targetDurationSeconds)
    }
}
