package com.lsing.timego.domain

import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ProgressMathTest {
    @Test
    fun `estimatedOneRepMax uses the Epley formula`() {
        assertEquals(70.0, estimatedOneRepMax(weightKg = 60.0, reps = 5), 0.001)
    }

    @Test
    fun `strengthCurve pairs each set's 1RM with its session date, sorted`() {
        val logs = listOf(
            SetLog(id = 1, sessionId = 2, exerciseId = 1, weightKg = 60.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 1, exerciseId = 1, weightKg = 50.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
        )
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 1), 2L to LocalDate.of(2026, 8, 8))

        val curve = strengthCurve(logs, sessionDateById)

        assertEquals(LocalDate.of(2026, 8, 1), curve[0].first)
        assertEquals(LocalDate.of(2026, 8, 8), curve[1].first)
    }

    @Test
    fun `workoutVolumeRatios normalizes each day's volume against the max`() {
        val sessions = listOf(
            WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 1), routineId = null),
            WorkoutSession(id = 2, date = LocalDate.of(2026, 8, 2), routineId = null),
        )
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 2, exerciseId = 1, weightKg = 50.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
        )

        val ratios = workoutVolumeRatios(sessions, sets)

        assertEquals(1.0f, ratios[LocalDate.of(2026, 8, 1)]!!, 0.001f)
        assertEquals(0.5f, ratios[LocalDate.of(2026, 8, 2)]!!, 0.001f)
    }

    @Test
    fun `personalRecords picks heaviest, most reps, and best volume sets`() {
        val logs = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 3, targetReps = 3, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 2, exerciseId = 1, weightKg = 80.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0),
        )
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 1), 2L to LocalDate.of(2026, 8, 8))

        val records = personalRecords(logs, sessionDateById)

        assertEquals(3, records.size)
        assertEquals(100.0, records.first { it.type == PrType.HEAVIEST_WEIGHT }.value, 0.001)
        assertEquals(10.0, records.first { it.type == PrType.MOST_REPS }.value, 0.001)
        assertEquals(800.0, records.first { it.type == PrType.BEST_VOLUME }.value, 0.001)
    }
}
