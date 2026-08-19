package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class LastSetPerformanceTest {
    private val bench = Exercise(id = 1, name = "Bench Press", muscleGroups = listOf("CHEST"), isCustom = false)
    private val run = Exercise(
        id = 2,
        name = "Running",
        muscleGroups = listOf("QUADS"),
        isCustom = false,
        category = ExerciseCategory.CARDIO.name,
    )
    private val exercisesById = mapOf(1L to bench, 2L to run)
    private val closedOlder = WorkoutSession(1, LocalDate.of(2026, 8, 1), null, 0, 1)
    private val closedLatest = WorkoutSession(2, LocalDate.of(2026, 8, 8), null, 0, 1)
    private val active = WorkoutSession(3, LocalDate.of(2026, 8, 15), null, 0, null)

    private fun set(
        sessionId: Long,
        exerciseId: Long = 1,
        weightKg: Double,
        reps: Int,
        loggedAt: Long,
        isWarmup: Boolean = false,
    ) = SetLog(
        sessionId = sessionId,
        exerciseId = exerciseId,
        weightKg = weightKg,
        reps = reps,
        targetReps = reps,
        loggedAtEpochMillis = loggedAt,
        isWarmup = isWarmup,
    )

    @Test
    fun `uses the last working set from the most recent closed session`() {
        val prior = set(1, weightKg = 70.0, reps = 8, loggedAt = 100)
        val latest = set(2, weightKg = 75.0, reps = 6, loggedAt = 300)

        val result = lastWorkingSetByExercise(listOf(prior, latest), listOf(closedOlder, closedLatest), exercisesById)

        assertEquals(latest, result[1L])
    }

    @Test
    fun `ignores a newer set from the active session`() {
        val prior = set(2, weightKg = 75.0, reps = 6, loggedAt = 300)
        val current = set(3, weightKg = 80.0, reps = 5, loggedAt = 400)

        val result = lastWorkingSetByExercise(listOf(prior, current), listOf(closedLatest, active), exercisesById)

        assertEquals(prior, result[1L])
    }

    @Test
    fun `ignores warmup and cardio logs`() {
        val warmup = set(2, weightKg = 40.0, reps = 10, loggedAt = 200, isWarmup = true)
        val cardio = set(2, exerciseId = 2, weightKg = 0.0, reps = 0, loggedAt = 300)

        val result = lastWorkingSetByExercise(listOf(warmup, cardio), listOf(closedLatest), exercisesById)

        assertFalse(result.containsKey(1L))
        assertFalse(result.containsKey(2L))
    }
}
