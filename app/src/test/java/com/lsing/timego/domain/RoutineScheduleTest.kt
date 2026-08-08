package com.lsing.timego.domain

import com.lsing.timego.data.Routine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

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
}
