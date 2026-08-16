package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MuscleDistributionTest {
    @Test
    fun `muscleGroupVolumeDistribution sums volume per muscle group, excluding sets before the cutoff`() {
        val squat = Exercise(id = 1, name = "Squat", muscleGroups = listOf("QUADS", "GLUTES"), isCustom = false, category = "STRENGTH")
        val curl = Exercise(id = 2, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sets = listOf(
            // 100kg x 5 = 500 volume, on/after cutoff -- counts
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            // Before cutoff -- excluded
            SetLog(id = 2, sessionId = 2, exerciseId = 2, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0),
        )
        val exercisesById = mapOf(1L to squat, 2L to curl)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 10), 2L to LocalDate.of(2026, 7, 1))

        val distribution = muscleGroupVolumeDistribution(sets, exercisesById, sessionDateById, since = LocalDate.of(2026, 8, 1))

        assertEquals(500.0, distribution["QUADS"]!!, 0.001)
        assertEquals(500.0, distribution["GLUTES"]!!, 0.001)
        assertEquals(null, distribution["BICEPS"])
    }

    @Test
    fun `muscleGroupVolumeDistribution excludes cardio and warmup sets`() {
        val run = Exercise(id = 3, name = "Running", muscleGroups = listOf("QUADS"), isCustom = false, category = "CARDIO", loggingType = "DURATION_DISTANCE")
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 3, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 0, durationMinutes = 30.0),
        )
        val exercisesById = mapOf(3L to run)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 10))

        val distribution = muscleGroupVolumeDistribution(sets, exercisesById, sessionDateById, since = LocalDate.of(2026, 8, 1))

        assertEquals(emptyMap<String, Double>(), distribution)
    }

    @Test
    fun `muscleGroupVolumeDistribution counts hold seconds as volume for HOLD exercises`() {
        val plank = Exercise(id = 4, name = "Plank", muscleGroups = listOf("ABS"), isCustom = false, category = "CALISTHENICS", loggingType = "HOLD")
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 4, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 0, holdSeconds = 40, targetHoldSeconds = 40),
        )
        val exercisesById = mapOf(4L to plank)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 10))

        val distribution = muscleGroupVolumeDistribution(sets, exercisesById, sessionDateById, since = LocalDate.of(2026, 8, 1))

        assertEquals(40.0, distribution["ABS"]!!, 0.001)
    }

    @Test
    fun `muscleGroupVolumeDistribution applies muscleWeights as a percentage of volume`() {
        val squat = Exercise(
            id = 1, name = "Squat", muscleGroups = listOf("QUADS", "GLUTES"), isCustom = false,
            category = "STRENGTH", loggingType = "WEIGHT_REPS", muscleWeights = mapOf("GLUTES" to 60),
        )
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
        )
        val exercisesById = mapOf(1L to squat)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 10))

        val distribution = muscleGroupVolumeDistribution(sets, exercisesById, sessionDateById, since = LocalDate.of(2026, 8, 1))

        // 100kg x 5 = 500 volume. QUADS has no override -> defaults to 100% -> full 500.
        // GLUTES is explicitly weighted 60% -> 300.
        assertEquals(500.0, distribution["QUADS"]!!, 0.001)
        assertEquals(300.0, distribution["GLUTES"]!!, 0.001)
    }

    @Test
    fun `timeframe distribution excludes sessions outside the selected calendar window`() {
        val bench = Exercise(id = 5, name = "Bench", muscleGroups = listOf("CHEST"), isCustom = false, category = "STRENGTH")
        val squat = Exercise(id = 6, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false, category = "STRENGTH")
        val sessions = listOf(
            WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 1), routineId = null, startEpochMillis = 0, endEpochMillis = 0),
            WorkoutSession(id = 2, date = LocalDate.of(2026, 8, 11), routineId = null, startEpochMillis = 0, endEpochMillis = 0),
        )
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 6, weightKg = 200.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 2, exerciseId = 5, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
        )

        val distribution = muscleDistributionForTimeframe(
            timeframe = ProgressTimeframe.WEEK,
            sessions = sessions,
            sets = sets,
            exercisesById = mapOf(5L to bench, 6L to squat),
            today = LocalDate.of(2026, 8, 12),
        )

        assertEquals(mapOf("CHEST" to 1f), distribution)

        val lifetimeDistribution = muscleDistributionForTimeframe(
            timeframe = ProgressTimeframe.LIFETIME,
            sessions = sessions,
            sets = sets,
            exercisesById = mapOf(5L to bench, 6L to squat),
            today = LocalDate.of(2026, 8, 12),
        )

        assertEquals(mapOf("CHEST" to 0.5f, "QUADS" to 1f), lifetimeDistribution)
    }

    @Test
    fun `trainingStats counts workouts, sets, volume, and estimates duration from set timestamps`() {
        val sessions = listOf(WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 10), routineId = null, startEpochMillis = 0, endEpochMillis = 0))
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 600_000),
        )

        val stats = trainingStats(sessions, sets, since = LocalDate.of(2026, 8, 1))

        assertEquals(1, stats.workouts)
        assertEquals(2, stats.totalSets)
        assertEquals(1000.0, stats.totalVolumeKg, 0.001)
        assertEquals(10.0, stats.totalDurationMinutes, 0.001)
    }
}
