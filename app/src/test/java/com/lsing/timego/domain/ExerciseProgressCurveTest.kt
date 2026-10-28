package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ExerciseProgressCurveTest {
    private val d1 = LocalDate.of(2026, 10, 14)
    private val d2 = LocalDate.of(2026, 10, 21)
    private val dates = mapOf(1L to d1, 2L to d1, 3L to d2)

    private fun ex(category: ExerciseCategory = ExerciseCategory.STRENGTH, logging: LoggingType = LoggingType.WEIGHT_REPS) =
        Exercise(id = 1, name = "X", muscleGroups = listOf("CHEST"), isCustom = false, category = category.name, loggingType = logging.name)

    private fun set(session: Long, weight: Double, reps: Int, warmup: Boolean = false, hold: Int? = null) =
        SetLog(sessionId = session, exerciseId = 1, weightKg = weight, reps = reps, targetReps = reps, loggedAtEpochMillis = 0, isWarmup = warmup, holdSeconds = hold)

    @Test
    fun `weighted lifts chart the best estimated 1RM per day, ignoring warm-ups`() {
        val history = listOf(set(1, 60.0, 10), set(2, 70.0, 5), set(1, 100.0, 1, warmup = true), set(3, 72.5, 5))
        val curve = exerciseProgressCurve(ex(), history, dates)
        assertEquals("kg", curve.unit)
        assertEquals(listOf(d1, d2), curve.points.map { it.first })
        assertEquals(estimatedOneRepMax(70.0, 5), curve.points[0].second, 1e-9) // 81.67 beats 60x10 = 80
        assertEquals(estimatedOneRepMax(72.5, 5), curve.points[1].second, 1e-9)
    }

    @Test
    fun `calisthenics chart best reps, not a bodyweight-based 1RM`() {
        val history = listOf(set(1, 59.8, 7), set(2, 59.8, 8), set(3, 59.8, 9))
        val curve = exerciseProgressCurve(ex(ExerciseCategory.CALISTHENICS), history, dates)
        assertEquals("reps", curve.unit)
        assertEquals(listOf(8.0, 9.0), curve.points.map { it.second })
    }

    @Test
    fun `holds chart the longest hold in seconds`() {
        val history = listOf(set(1, 0.0, 0, hold = 40), set(2, 0.0, 0, hold = 55), set(3, 0.0, 0, hold = 50))
        val curve = exerciseProgressCurve(ex(ExerciseCategory.CALISTHENICS, LoggingType.HOLD), history, dates)
        assertEquals("s", curve.unit)
        assertEquals(listOf(55.0, 50.0), curve.points.map { it.second })
    }

    @Test
    fun `two sets on one day make one point`() {
        val curve = exerciseProgressCurve(ex(), listOf(set(1, 50.0, 8), set(2, 50.0, 10)), dates)
        assertEquals(1, curve.points.size)
    }
}
