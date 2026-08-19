package com.lsing.timego.domain

import com.lsing.timego.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepRangeTest {
    private fun set(sessionId: Long, weightKg: Double, reps: Int, isWarmup: Boolean = false) =
        SetLog(sessionId = sessionId, exerciseId = 1, weightKg = weightKg, reps = reps, targetReps = reps, loggedAtEpochMillis = 0, isWarmup = isWarmup)

    @Test
    fun `fewer than 3 distinct sessions at the weight returns null`() {
        val sets = listOf(set(1, 60.0, 8), set(1, 60.0, 9), set(2, 60.0, 10))
        assertNull(repRangeAtWeight(sets, 60.0))
    }

    @Test
    fun `3 distinct sessions at the weight returns min-max range`() {
        val sets = listOf(set(1, 60.0, 8), set(2, 60.0, 12), set(3, 60.0, 10))
        val range = repRangeAtWeight(sets, 60.0)
        assertEquals(8, range!!.floor)
        assertEquals(12, range.ceiling)
    }

    @Test
    fun `sets at a different weight are excluded`() {
        val sets = listOf(set(1, 60.0, 8), set(2, 60.0, 12), set(3, 60.0, 10), set(4, 65.0, 20))
        val range = repRangeAtWeight(sets, 60.0)
        assertEquals(12, range!!.ceiling)
    }

    @Test
    fun `warmup sets are excluded even if they would otherwise meet the session threshold`() {
        val sets = listOf(set(1, 60.0, 8, isWarmup = true), set(2, 60.0, 12, isWarmup = true), set(3, 60.0, 10, isWarmup = true))
        assertNull(repRangeAtWeight(sets, 60.0))
    }

    @Test
    fun `multiple sets in the same session count as one session toward the threshold`() {
        val sets = listOf(set(1, 60.0, 8), set(1, 60.0, 9), set(2, 60.0, 10), set(3, 60.0, 11))
        val range = repRangeAtWeight(sets, 60.0)
        assertEquals(8, range!!.floor)
        assertEquals(11, range.ceiling)
    }
}
