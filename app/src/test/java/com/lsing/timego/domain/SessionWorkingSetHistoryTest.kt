package com.lsing.timego.domain

import com.lsing.timego.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionWorkingSetHistoryTest {
    private fun setLog(id: Long, sessionId: Long, loggedAt: Long, isWarmup: Boolean = false) = SetLog(
        id = id,
        sessionId = sessionId,
        exerciseId = 1,
        weightKg = 60.0,
        reps = 8,
        targetReps = 8,
        loggedAtEpochMillis = loggedAt,
        isWarmup = isWarmup,
    )

    @Test
    fun `excludes warmup sets and keeps the last working set per session`() {
        val sets = listOf(
            setLog(1, sessionId = 10, loggedAt = 100, isWarmup = true),
            setLog(2, sessionId = 10, loggedAt = 200),
            setLog(3, sessionId = 10, loggedAt = 300),
        )
        val result = sessionWorkingSetHistory(sets, mapOf(10L to 0L))
        assertEquals(listOf(sets[2]), result)
    }

    @Test
    fun `orders sessions by session start time, not set id or logged time`() {
        val sets = listOf(
            setLog(1, sessionId = 20, loggedAt = 500),
            setLog(2, sessionId = 10, loggedAt = 100),
        )
        val result = sessionWorkingSetHistory(sets, mapOf(10L to 1000L, 20L to 2000L))
        assertEquals(listOf(sets[1], sets[0]), result)
    }

    @Test
    fun `a session with only warmup sets contributes nothing`() {
        val sets = listOf(setLog(1, sessionId = 10, loggedAt = 100, isWarmup = true))
        val result = sessionWorkingSetHistory(sets, mapOf(10L to 0L))
        assertEquals(emptyList<SetLog>(), result)
    }

    @Test
    fun `unknown session id falls back to zero for ordering`() {
        val sets = listOf(
            setLog(1, sessionId = 10, loggedAt = 100),
            setLog(2, sessionId = 99, loggedAt = 200), // no entry in sessionStartById
        )
        val result = sessionWorkingSetHistory(sets, mapOf(10L to 500L))
        assertEquals(listOf(sets[1], sets[0]), result) // session 99 (fallback 0) sorts before session 10 (500)
    }
}
