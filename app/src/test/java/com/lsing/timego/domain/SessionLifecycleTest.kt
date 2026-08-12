package com.lsing.timego.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionLifecycleTest {
    @Test
    fun `stays active when last set was well within the threshold`() {
        val lastSet = 0L
        val now = 30 * 60 * 1000L // 30 minutes later
        assertEquals(SessionAutoCloseDecision.STAY_ACTIVE, checkSessionAutoClose(lastSet, now))
    }

    @Test
    fun `auto-closes when last set was well past the threshold`() {
        val lastSet = 0L
        val now = 2 * 60 * 60 * 1000L // 2 hours later
        assertEquals(SessionAutoCloseDecision.AUTO_CLOSE, checkSessionAutoClose(lastSet, now))
    }

    @Test
    fun `stays active at exactly the threshold boundary`() {
        val lastSet = 0L
        val now = ONE_HOUR_MILLIS // exactly 1 hour later
        assertEquals(SessionAutoCloseDecision.STAY_ACTIVE, checkSessionAutoClose(lastSet, now))
    }

    @Test
    fun `auto-closes one millisecond past the threshold`() {
        val lastSet = 0L
        val now = ONE_HOUR_MILLIS + 1
        assertEquals(SessionAutoCloseDecision.AUTO_CLOSE, checkSessionAutoClose(lastSet, now))
    }

    @Test
    fun `respects a custom threshold`() {
        val lastSet = 0L
        val now = 10 * 60 * 1000L // 10 minutes later
        assertEquals(
            SessionAutoCloseDecision.AUTO_CLOSE,
            checkSessionAutoClose(lastSet, now, inactivityThresholdMillis = 5 * 60 * 1000L),
        )
    }
}
