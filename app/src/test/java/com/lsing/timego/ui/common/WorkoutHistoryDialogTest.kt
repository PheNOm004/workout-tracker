package com.lsing.timego.ui.common

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutHistoryDialogTest {
    private val running = Exercise(
        id = 1,
        name = "Running",
        muscleGroups = listOf("QUADS"),
        isCustom = false,
        loggingType = LoggingType.DURATION_DISTANCE.name,
    )

    private fun cardioLog(durationMinutes: Double, distanceKm: Double? = null) = SetLog(
        sessionId = 1,
        exerciseId = running.id,
        weightKg = 0.0,
        reps = 0,
        targetReps = 0,
        loggedAtEpochMillis = 0,
        durationMinutes = durationMinutes,
        distanceKm = distanceKm,
    )

    @Test
    fun `formats a sub-minute cardio history duration in seconds`() {
        val entries = buildDayHistoryEntries(
            listOf(cardioLog(0.5166666666666667, distanceKm = 2.5)),
            mapOf(running.id to running),
        )

        assertEquals(listOf(DayHistoryEntry("Running", listOf("31s -- 2.5km"))), entries)
    }

    @Test
    fun `formats a mixed minute and second cardio history duration`() {
        val entries = buildDayHistoryEntries(
            listOf(cardioLog(1.0333333333333334)),
            mapOf(running.id to running),
        )

        assertEquals(listOf(DayHistoryEntry("Running", listOf("1m 2s"))), entries)
    }

    @Test
    fun `formats exact cardio minutes without decimal noise`() {
        val entries = buildDayHistoryEntries(
            listOf(cardioLog(30.0)),
            mapOf(running.id to running),
        )

        assertEquals(listOf(DayHistoryEntry("Running", listOf("30 min"))), entries)
    }
}
