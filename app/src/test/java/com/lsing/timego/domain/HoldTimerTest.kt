package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HoldTimerTest {
    private val start = 1_000_000L

    @Test
    fun `counts down from the full delay at the moment of starting`() {
        assertEquals(HoldTimerPhase.CountingDown(5), timerPhaseAt(start, 5, start))
    }

    @Test
    fun `counting down rounds up so a partly elapsed second still shows`() {
        assertEquals(HoldTimerPhase.CountingDown(4), timerPhaseAt(start, 5, start + 1_500))
    }

    @Test
    fun `switches to Running at zero elapsed once the delay is served`() {
        assertEquals(HoldTimerPhase.Running(0), timerPhaseAt(start, 5, start + 5_000))
    }

    @Test
    fun `zero delay starts running immediately`() {
        assertEquals(HoldTimerPhase.Running(0), timerPhaseAt(start, 0, start))
    }

    @Test
    fun `running elapsed is measured from the end of the delay`() {
        assertEquals(HoldTimerPhase.Running(42), timerPhaseAt(start, 5, start + 47_000))
    }

    /** The drift regression: a tick arriving late must not lose the missed seconds. */
    @Test
    fun `a late tick reports true elapsed time rather than a tick count`() {
        assertEquals(HoldTimerPhase.Running(600), timerPhaseAt(start, 0, start + 600_000))
    }

    @Test
    fun `a clock moving backwards clamps to zero instead of going negative`() {
        assertEquals(HoldTimerPhase.Running(0), timerPhaseAt(start, 0, start - 5_000))
    }
}
