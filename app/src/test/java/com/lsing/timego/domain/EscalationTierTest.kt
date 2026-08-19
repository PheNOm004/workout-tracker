package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class EscalationTierTest {
    @Test
    fun `rpe 1 through 7 is FULL`() {
        (1..7).forEach { assertEquals("rpe=$it", EscalationTier.FULL, escalationTierForRpe(it)) }
    }

    @Test
    fun `rpe 8 is PARTIAL`() {
        assertEquals(EscalationTier.PARTIAL, escalationTierForRpe(8))
    }

    @Test
    fun `rpe 9 and 10 is HOLD`() {
        assertEquals(EscalationTier.HOLD, escalationTierForRpe(9))
        assertEquals(EscalationTier.HOLD, escalationTierForRpe(10))
    }

    @Test
    fun `null rpe is FULL, same as no effort signal`() {
        assertEquals(EscalationTier.FULL, escalationTierForRpe(null))
    }
}
