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

    @Test
    fun `buildGroupedDayHistory groups exercises by body region sorted by total set volume`() {
        val squat = Exercise(id = 1, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false)
        val bench = Exercise(id = 2, name = "Bench Press", muscleGroups = listOf("CHEST"), isCustom = false)
        val deadHang = Exercise(id = 3, name = "Dead Hang", muscleGroups = listOf("FOREARMS"), isCustom = false)

        val exercisesById = mapOf(1L to squat, 2L to bench, 3L to deadHang)
        val logs = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 1),
            SetLog(id = 2, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 2),
            SetLog(id = 3, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 3),
            SetLog(id = 4, sessionId = 1, exerciseId = 2, weightKg = 80.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 4),
            SetLog(id = 5, sessionId = 1, exerciseId = 2, weightKg = 80.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 5),
            SetLog(id = 6, sessionId = 1, exerciseId = 3, weightKg = 0.0, reps = 0, holdSeconds = 60, targetReps = 0, loggedAtEpochMillis = 6),
        )

        val groups = buildGroupedDayHistory(logs, exercisesById)
        assertEquals(3, groups.size)
        assertEquals("Legs", groups[0].regionLabel)
        assertEquals(3, groups[0].totalSets)
        assertEquals("Chest", groups[1].regionLabel)
        assertEquals(2, groups[1].totalSets)
        assertEquals("Arms", groups[2].regionLabel)
        assertEquals(1, groups[2].totalSets)
    }
}
