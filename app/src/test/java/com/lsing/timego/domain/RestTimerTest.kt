package com.lsing.timego.domain

import com.lsing.timego.data.RIR_PROGRAM_PRESCRIPTIONS
import org.junit.Assert.assertEquals
import org.junit.Test

class RestTimerTest {
    private val timer = RestTimerState(startedAtMillis = 1_000L, totalSeconds = 120)

    @Test
    fun `counts down in whole seconds and goes negative when over`() {
        assertEquals(120, timer.remainingSeconds(1_000L))
        assertEquals(119, timer.remainingSeconds(2_000L))
        assertEquals(0, timer.remainingSeconds(121_000L))
        assertEquals(-5, timer.remainingSeconds(126_000L))
    }

    @Test
    fun `adjust never drops below the minimum`() {
        assertEquals(135, timer.adjusted(15).totalSeconds)
        assertEquals(MIN_REST_SECONDS, timer.adjusted(-500).totalSeconds)
    }

    @Test
    fun `prescription rest wins over the default`() {
        assertEquals(150, restSecondsFor(RIR_PROGRAM_PRESCRIPTIONS.getValue("Pull-Up"), 90))
        assertEquals(90, restSecondsFor(null, 90))
    }

    @Test
    fun `adaptive rest nudges by RPE and static ignores everything`() {
        val heavy = RIR_PROGRAM_PRESCRIPTIONS.getValue("Pull-Up")
        assertEquals(180, restSecondsFor(heavy, 120, adaptive = true, lastRpe = 9))
        assertEquals(135, restSecondsFor(null, 150, adaptive = true, lastRpe = 5))
        assertEquals(120, restSecondsFor(heavy, 120, adaptive = false, lastRpe = 10))
    }

    @Test
    fun `combined weight entry is halved to per hand`() {
        assertEquals(17.5, combinedEntryToStored(35.0, combined = true), 0.0)
        assertEquals(35.0, storedToCombinedEntry(17.5, combined = true), 0.0)
        assertEquals(20.0, combinedEntryToStored(20.0, combined = false), 0.0)
    }

    @Test
    fun `clock formatting`() {
        assertEquals("2:05", formatRestClock(125))
        assertEquals("+0:12", formatRestClock(-12))
    }
}
