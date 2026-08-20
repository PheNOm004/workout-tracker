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
    fun `bodyweight exercise volume uses the stored bodyweight`() {
        val sissySquat = Exercise(
            id = 9,
            name = "Sissy Squat",
            muscleGroups = listOf("QUADS"),
            isCustom = false,
            category = "CALISTHENICS",
            loggingType = "WEIGHT_REPS",
        )
        val sets = listOf(
            SetLog(id = 1, sessionId = 9, exerciseId = 9, weightKg = 80.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0),
        )

        val distribution = muscleGroupVolumeDistribution(
            sets,
            exercisesById = mapOf(9L to sissySquat),
            sessionDateById = mapOf(9L to LocalDate.of(2026, 8, 10)),
            since = LocalDate.of(2026, 8, 1),
        )

        assertEquals(800.0, distribution["QUADS"]!!, 0.001)
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
    fun `muscleGroupIntensityForSession normalizes weighted contribution and excludes warmups`() {
        val pullover = Exercise(
            id = 7,
            name = "Pullover",
            muscleGroups = listOf("LATS", "CHEST"),
            isCustom = false,
            category = "STRENGTH",
            muscleWeights = mapOf("CHEST" to 35),
        )
        val warmup = Exercise(
            id = 8,
            name = "Warmup",
            muscleGroups = listOf("QUADS"),
            isCustom = false,
            category = "WARMUP",
            loggingType = "DURATION_DISTANCE",
        )
        val sets = listOf(
            SetLog(id = 1, sessionId = 7, exerciseId = 7, weightKg = 10.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 7, exerciseId = 8, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 1, durationMinutes = 1.0),
        )

        val result = muscleGroupIntensityForSession(7, sets, mapOf(7L to pullover, 8L to warmup))

        assertEquals(1.0f, result["LATS"]!!, 0.001f)
        assertEquals(0.35f, result["CHEST"]!!, 0.001f)
        assertEquals(null, result["QUADS"])
    }

    @Test
    fun `orderedMuscleDistributionForChart keeps detailed spokes stable`() {
        val input = linkedMapOf(
            "TRICEPS" to 0.9f,
            "TRAPS" to 0.2f,
            "CHEST" to 1.0f,
        )

        assertEquals(listOf("CHEST", "TRAPS", "TRICEPS"), orderedMuscleDistributionForChart(input).keys.toList())
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

    @Test
    fun `effortWeight gives full credit to effective-rep-range RPE and to missing RPE`() {
        assertEquals(1.0, effortWeight(7), 0.001)
        assertEquals(1.0, effortWeight(8), 0.001)
        assertEquals(1.0, effortWeight(9), 0.001)
        assertEquals(1.0, effortWeight(10), 0.001)
        assertEquals(1.0, effortWeight(null), 0.001)
    }

    @Test
    fun `effortWeight ramps between RPE 5 and 6`() {
        assertEquals(0.3, effortWeight(5), 0.001)
        assertEquals(0.65, effortWeight(6), 0.001)
    }

    @Test
    fun `effortWeight gives low credit below RPE 5`() {
        assertEquals(0.15, effortWeight(1), 0.001)
        assertEquals(0.15, effortWeight(4), 0.001)
    }
}
