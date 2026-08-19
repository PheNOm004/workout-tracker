package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FrequencyDistributionTest {
    private val squat = Exercise(id = 1, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false)
    private val exercisesById = mapOf(1L to squat)
    private val today = LocalDate.of(2026, 8, 19)

    private fun session(id: Long, date: LocalDate) =
        WorkoutSession(id = id, date = date, routineId = null, startEpochMillis = 0, endEpochMillis = 0)

    private fun set(sessionId: Long) =
        SetLog(sessionId = sessionId, exerciseId = 1, weightKg = 60.0, reps = 8, targetReps = 8, loggedAtEpochMillis = 0)

    @Test
    fun `group trained at exactly its baseline rate in the selected week reads 1_0`() {
        val baselineSessions = (1..8).map { session(it.toLong(), today.minusWeeks(it.toLong())) }
        val thisWeekSession = session(100, today)
        val sessions = baselineSessions + thisWeekSession

        val result = frequencyDistributionForTimeframe(
            ProgressTimeframe.WEEK,
            sessions,
            sessions.map { set(it.id) },
            exercisesById,
            today,
        )

        assertEquals(1.0f, result["QUADS"]!!, 0.01f)
    }

    @Test
    fun `group trained at half its baseline rate reads about 0_5`() {
        val historicalSessions = listOf(31L, 35L, 39L, 43L, 47L, 51L).map { daysAgo ->
            session(daysAgo, today.minusDays(daysAgo))
        }
        val selectedSessions = listOf(session(101, today.minusDays(3)), session(102, today.minusDays(10)))
        val sessions = historicalSessions + selectedSessions

        val result = frequencyDistributionForTimeframe(
            ProgressTimeframe.MONTH,
            sessions,
            sessions.map { set(it.id) },
            exercisesById,
            today,
        )

        assertEquals(0.46f, result["QUADS"]!!, 0.05f)
    }

    @Test
    fun `group trained above baseline caps at 1_0`() {
        val baselineSessions = (1..2).map { session(it.toLong(), today.minusWeeks(it.toLong())) }
        val heavyWeek = (1..5).map { session(200L + it, today.minusDays(it.toLong())) }
        val sessions = baselineSessions + heavyWeek

        val result = frequencyDistributionForTimeframe(
            ProgressTimeframe.WEEK,
            sessions,
            sessions.map { set(it.id) },
            exercisesById,
            today,
        )

        assertEquals(1.0f, result["QUADS"]!!, 0.01f)
    }

    @Test
    fun `zero baseline sessions but one in the selected period reads 1_0`() {
        val sessions = listOf(session(1, today))

        val result = frequencyDistributionForTimeframe(
            ProgressTimeframe.WEEK,
            sessions,
            sessions.map { set(it.id) },
            exercisesById,
            today,
        )

        assertEquals(1.0f, result["QUADS"]!!, 0.01f)
    }

    @Test
    fun `zero sessions in both baseline and selected period reads 0`() {
        val result = frequencyDistributionForTimeframe(
            ProgressTimeframe.WEEK,
            emptyList(),
            emptyList(),
            exercisesById,
            today,
        )

        assertEquals(0.0f, result["QUADS"]!!, 0.01f)
    }
}
