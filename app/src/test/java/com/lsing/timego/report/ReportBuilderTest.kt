package com.lsing.timego.report

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import com.lsing.timego.profile.TrainingProfile
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportBuilderTest {
    @Test fun emptyWeekIsTruthful() {
        val report = buildWeeklyReport(ReportInput(emptyList(), emptyList(), emptyList()), LocalDate.of(2026, 9, 14))
        assertEquals(0, report.sessionCount)
        assertEquals(0.0, report.strengthVolumeKg.current, 0.0)
        assertTrue(report.suggestion.startsWith("No workouts"))
    }

    @Test fun mixedModalitiesRemainSeparate() {
        val date = LocalDate.of(2026, 9, 16)
        val exercises = listOf(exercise(1, LoggingType.WEIGHT_REPS), exercise(2, LoggingType.HOLD), exercise(3, LoggingType.DURATION_DISTANCE))
        val session = WorkoutSession(10, date, null, 0, 3_600_000)
        val sets = listOf(
            SetLog(sessionId = 10, exerciseId = 1, weightKg = 50.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 1),
            SetLog(sessionId = 10, exerciseId = 2, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 2, holdSeconds = 30),
            SetLog(sessionId = 10, exerciseId = 3, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 3, durationMinutes = 20.0, distanceKm = 4.5),
        )
        val report = buildWeeklyReport(ReportInput(listOf(session), sets, exercises), date)
        assertEquals(500.0, report.strengthVolumeKg.current, 0.0)
        assertEquals(30, report.holdSeconds)
        assertEquals(20.0, report.cardioMinutes, 0.0)
        assertEquals(4.5, report.cardioDistanceKm, 0.0)
        assertEquals(60, report.durationMinutes)
    }

    @Test fun weekAndMonthBoundariesUseDatesNotElapsedMilliseconds() {
        val week = ReportPeriod.weekContaining(LocalDate.of(2026, 1, 1))
        assertEquals(LocalDate.of(2025, 12, 29), week.start)
        assertEquals(LocalDate.of(2026, 1, 4), week.endInclusive)
        val month = ReportPeriod.month(YearMonth.of(2024, 2))
        assertEquals(LocalDate.of(2024, 2, 29), month.endInclusive)
    }

    @Test fun plannedDaysProduceConservativeConsistencySuggestion() {
        val date = LocalDate.of(2026, 9, 14)
        val session = WorkoutSession(1, date, null, 0, 60_000)
        val report = buildWeeklyReport(ReportInput(listOf(session), emptyList(), emptyList()), date, TrainingProfile(trainingDaysPerWeek = 3))
        assertTrue(report.suggestion.contains("fewer active days"))
    }

    private fun exercise(id: Long, type: LoggingType) = Exercise(id, "Exercise $id", "key$id", listOf("FULL_BODY"), false, loggingType = type.name)
}
