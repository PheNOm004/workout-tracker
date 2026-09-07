package com.lsing.timego.domain

import com.lsing.timego.data.Routine
import com.lsing.timego.data.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RoutineScheduleTest {
    @Test
    fun `routinesForToday returns only routines scheduled for the given day`() {
        val pushDay = Routine(id = 1, name = "Push Day", daysOfWeek = listOf("WEDNESDAY", "SATURDAY"))
        val legDay = Routine(id = 2, name = "Leg Day", daysOfWeek = listOf("MONDAY"))

        val result = routinesForToday(listOf(pushDay, legDay), DayOfWeek.WEDNESDAY)

        assertEquals(listOf(pushDay), result)
    }

    @Test
    fun `routinesForToday returns empty list when nothing is scheduled`() {
        val legDay = Routine(id = 2, name = "Leg Day", daysOfWeek = listOf("MONDAY"))
        assertEquals(emptyList<Routine>(), routinesForToday(listOf(legDay), DayOfWeek.FRIDAY))
    }

    @Test
    fun `routineLastCompletedDates returns the latest closed-session date per routine`() {
        val sessions = listOf(
            WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 9), routineId = 1, startEpochMillis = 0, endEpochMillis = 0),
            WorkoutSession(id = 2, date = LocalDate.of(2026, 8, 16), routineId = 1, startEpochMillis = 0, endEpochMillis = 0),
        )

        val result = routineLastCompletedDates(sessions)

        assertEquals(LocalDate.of(2026, 8, 16), result[1L])
    }

    @Test
    fun `routineLastCompletedDates excludes a still-active session`() {
        val sessions = listOf(
            WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 9), routineId = 1, startEpochMillis = 0, endEpochMillis = 0),
            // Active session (endEpochMillis == null) for the same routine, more recent -- must not win.
            WorkoutSession(id = 2, date = LocalDate.of(2026, 8, 20), routineId = 1, startEpochMillis = 0, endEpochMillis = null),
        )

        val result = routineLastCompletedDates(sessions)

        assertEquals(LocalDate.of(2026, 8, 9), result[1L])
    }

    @Test
    fun `routineLastCompletedDates excludes freeform sessions and omits routines with no completed session`() {
        val sessions = listOf(
            // Freeform session, no routine -- must not appear under any routine id.
            WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 9), routineId = null, startEpochMillis = 0, endEpochMillis = 0),
        )

        val result = routineLastCompletedDates(sessions)

        assertEquals(emptyMap<Long, LocalDate>(), result)
    }

    @Test
    fun `formatDaysSince reports Today for the same date`() {
        assertEquals("Today", formatDaysSince(LocalDate.of(2026, 8, 20), today = LocalDate.of(2026, 8, 20)))
    }

    @Test
    fun `formatDaysSince reports elapsed days for a past date`() {
        assertEquals("4d ago", formatDaysSince(LocalDate.of(2026, 8, 16), today = LocalDate.of(2026, 8, 20)))
        assertEquals("1d ago", formatDaysSince(LocalDate.of(2026, 8, 19), today = LocalDate.of(2026, 8, 20)))
    }

    @Test
    fun `formatDaysSince reports Never logged for null`() {
        assertEquals("Never logged", formatDaysSince(null, today = LocalDate.of(2026, 8, 20)))
    }

    @Test
    fun `flexibleRoutines returns only routines with empty daysOfWeek`() {
        val flexPush = Routine(id = 1, name = "Push", daysOfWeek = emptyList())
        val scheduledLegs = Routine(id = 2, name = "Legs", daysOfWeek = listOf("MONDAY"))
        val flexPull = Routine(id = 3, name = "Pull", daysOfWeek = emptyList())

        val result = flexibleRoutines(listOf(flexPush, scheduledLegs, flexPull))

        assertEquals(listOf(flexPush, flexPull), result)
    }

    @Test
    fun `nextFlexibleRoutineInRotation prioritizes unlogged routines first`() {
        val push = Routine(id = 1, name = "Push", daysOfWeek = emptyList())
        val pull = Routine(id = 2, name = "Pull", daysOfWeek = emptyList())
        val legs = Routine(id = 3, name = "Legs", daysOfWeek = emptyList())

        // Pull was completed yesterday, Legs was completed 3 days ago, Push never completed
        val history = mapOf(
            2L to LocalDate.of(2026, 8, 19),
            3L to LocalDate.of(2026, 8, 17),
        )

        val next = nextFlexibleRoutineInRotation(listOf(push, pull, legs), history)

        assertEquals(push, next)
    }

    @Test
    fun `nextFlexibleRoutineInRotation selects least-recently completed routine`() {
        val push = Routine(id = 1, name = "Push", daysOfWeek = emptyList())
        val pull = Routine(id = 2, name = "Pull", daysOfWeek = emptyList())
        val legs = Routine(id = 3, name = "Legs", daysOfWeek = emptyList())

        // Push completed yesterday, Pull completed 5 days ago, Legs completed 2 days ago
        val history = mapOf(
            1L to LocalDate.of(2026, 8, 19),
            2L to LocalDate.of(2026, 8, 15),
            3L to LocalDate.of(2026, 8, 18),
        )

        val next = nextFlexibleRoutineInRotation(listOf(push, pull, legs), history)

        assertEquals(pull, next)
    }

    @Test
    fun `nextFlexibleRoutineInRotation returns null when list is empty`() {
        val next = nextFlexibleRoutineInRotation(emptyList(), emptyMap())
        assertEquals(null, next)
    }
}
