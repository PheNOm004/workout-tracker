package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HoldTimerTest {
    @Test
    fun `CountingDown decrements while remaining is above 1`() {
        val next = HoldTimerPhase.CountingDown(5).tick()

        assertEquals(HoldTimerPhase.CountingDown(4), next)
    }

    @Test
    fun `CountingDown jumps straight to Running at 0 elapsed once remaining hits 1`() {
        val next = HoldTimerPhase.CountingDown(1).tick()

        assertEquals(HoldTimerPhase.Running(0), next)
    }

    @Test
    fun `Running increments elapsed seconds indefinitely`() {
        val next = HoldTimerPhase.Running(42).tick()

        assertEquals(HoldTimerPhase.Running(43), next)
    }
}
