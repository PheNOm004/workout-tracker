package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import com.lsing.timego.ui.common.rankedMuscleBalanceBars
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

    @Test
    fun `muscleGroupEffectiveSetDistribution weights a set by its RPE effort credit`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sets = listOf(
            // RPE 8 -> full credit (1.0)
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 8),
            // RPE 3 -> low credit (0.15)
            SetLog(id = 2, sessionId = 1, exerciseId = 1, weightKg = 10.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 3),
        )
        val exercisesById = mapOf(1L to curl)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 10))

        val distribution = muscleGroupEffectiveSetDistribution(sets, exercisesById, sessionDateById, since = LocalDate.of(2026, 8, 1))

        assertEquals(1.15, distribution["BICEPS"]!!, 0.001)
    }

    @Test
    fun `muscleGroupEffectiveSetDistribution treats missing RPE as full credit`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = null),
        )
        val distribution = muscleGroupEffectiveSetDistribution(
            sets, mapOf(1L to curl), mapOf(1L to LocalDate.of(2026, 8, 10)), since = LocalDate.of(2026, 8, 1),
        )

        assertEquals(1.0, distribution["BICEPS"]!!, 0.001)
    }

    @Test
    fun `muscleGroupEffectiveSetDistribution excludes cardio and warmup sets, applies muscleWeights partial credit`() {
        val pullover = Exercise(
            id = 1, name = "Pullover", muscleGroups = listOf("LATS", "CHEST"), isCustom = false,
            category = "STRENGTH", muscleWeights = mapOf("CHEST" to 30),
        )
        val run = Exercise(id = 2, name = "Running", muscleGroups = listOf("QUADS"), isCustom = false, category = "CARDIO", loggingType = "DURATION_DISTANCE")
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 10.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 9),
            SetLog(id = 2, sessionId = 1, exerciseId = 2, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 1, durationMinutes = 30.0),
        )
        val exercisesById = mapOf(1L to pullover, 2L to run)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 10))

        val distribution = muscleGroupEffectiveSetDistribution(sets, exercisesById, sessionDateById, since = LocalDate.of(2026, 8, 1))

        assertEquals(1.0, distribution["LATS"]!!, 0.001)
        assertEquals(0.3, distribution["CHEST"]!!, 0.001)
        assertEquals(null, distribution["QUADS"])
    }

    @Test
    fun `muscleGroupEffectiveSetDistribution excludes sets before the cutoff date`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 8),
        )
        val distribution = muscleGroupEffectiveSetDistribution(
            sets, mapOf(1L to curl), mapOf(1L to LocalDate.of(2026, 7, 1)), since = LocalDate.of(2026, 8, 1),
        )

        assertEquals(emptyMap<String, Double>(), distribution)
    }

    @Test
    fun `muscleBalanceForTimeframe scores exactly-target effective sets as 1_0 over a one-week window`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sessions = listOf(WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 10), routineId = null, startEpochMillis = 0, endEpochMillis = 0))
        // 10 sets at RPE 8 (full credit each) = 10.0 effective sets, target for WEEK is 10.0 -> 1.0
        val sets = (1..10).map { i ->
            SetLog(id = i.toLong(), sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 8)
        }

        val balance = muscleBalanceForTimeframe(
            timeframe = ProgressTimeframe.WEEK,
            sessions = sessions,
            sets = sets,
            exercisesById = mapOf(1L to curl),
            today = LocalDate.of(2026, 8, 10),
        )

        assertEquals(1.0f, balance["BICEPS"]!!, 0.001f)
    }

    @Test
    fun `muscleBalanceForTimeframe scores half-target effective sets as 0_5`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sessions = listOf(WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 10), routineId = null, startEpochMillis = 0, endEpochMillis = 0))
        val sets = (1..5).map { i ->
            SetLog(id = i.toLong(), sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 8)
        }

        val balance = muscleBalanceForTimeframe(
            timeframe = ProgressTimeframe.WEEK,
            sessions = sessions,
            sets = sets,
            exercisesById = mapOf(1L to curl),
            today = LocalDate.of(2026, 8, 10),
        )

        assertEquals(0.5f, balance["BICEPS"]!!, 0.001f)
    }

    @Test
    fun `muscleBalanceForTimeframe caps at 1_0 and never exceeds it`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sessions = listOf(WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 10), routineId = null, startEpochMillis = 0, endEpochMillis = 0))
        val sets = (1..20).map { i ->
            SetLog(id = i.toLong(), sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 8)
        }

        val balance = muscleBalanceForTimeframe(
            timeframe = ProgressTimeframe.WEEK,
            sessions = sessions,
            sets = sets,
            exercisesById = mapOf(1L to curl),
            today = LocalDate.of(2026, 8, 10),
        )

        assertEquals(1.0f, balance["BICEPS"]!!, 0.001f)
    }

    @Test
    fun `muscleBalanceForTimeframe scales the target across a month window`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sessions = listOf(WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 1), routineId = null, startEpochMillis = 0, endEpochMillis = 0))
        // Month window is 30 days = 30/7 weeks; target = 10 * 30/7 = ~42.857. 21.43 effective sets -> ~0.5.
        val sets = (1..21).map { i ->
            SetLog(id = i.toLong(), sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 8)
        }

        val balance = muscleBalanceForTimeframe(
            timeframe = ProgressTimeframe.MONTH,
            sessions = sessions,
            sets = sets,
            exercisesById = mapOf(1L to curl),
            today = LocalDate.of(2026, 8, 10),
        )

        assertEquals(0.49f, balance["BICEPS"]!!, 0.01f)
    }

    @Test
    fun `muscleBalanceForTimeframe is empty when no qualifying sets exist`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val balance = muscleBalanceForTimeframe(
            timeframe = ProgressTimeframe.WEEK,
            sessions = emptyList(),
            sets = emptyList(),
            exercisesById = mapOf(1L to curl),
            today = LocalDate.of(2026, 8, 10),
        )

        assertEquals(emptyMap<String, Float>(), balance)
    }

    @Test
    fun `muscleBalanceForTimeframe does not divide by zero on a same-day lifetime window`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val sessions = listOf(WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 10), routineId = null, startEpochMillis = 0, endEpochMillis = 0))
        val sets = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0, rpe = 8),
        )

        val balance = muscleBalanceForTimeframe(
            timeframe = ProgressTimeframe.LIFETIME,
            sessions = sessions,
            sets = sets,
            exercisesById = mapOf(1L to curl),
            today = LocalDate.of(2026, 8, 10),
        )

        // since == today (LIFETIME's earliest-session fallback), inclusive-day counting makes the
        // window 1 day (1/7 week), target = 10/7 ~= 1.4286; 1 effective set / 1.4286 ~= 0.7.
        assertEquals(0.7f, balance["BICEPS"]!!, 0.01f)
    }

    @Test
    fun `muscleBalanceForTimeframe long windows dilute sparse recent work as a weekly-rate consequence`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val today = LocalDate.of(2026, 8, 22)
        // The early empty session defines a full Year/Lifetime observation window. Ten current
        // sets are exactly the weekly target, but cannot also be a year's weekly target.
        val sessions = listOf(
            WorkoutSession(id = 1, date = today.minusDays(364), routineId = null, startEpochMillis = 0, endEpochMillis = 0),
            WorkoutSession(id = 2, date = today, routineId = null, startEpochMillis = 0, endEpochMillis = 0),
        )
        val sets = (1..10).map { index ->
            SetLog(
                id = index.toLong(),
                sessionId = 2,
                exerciseId = 1,
                weightKg = 20.0,
                reps = 10,
                targetReps = 10,
                loggedAtEpochMillis = 0,
                rpe = 8,
            )
        }

        fun score(timeframe: ProgressTimeframe) = muscleBalanceForTimeframe(
            timeframe = timeframe,
            sessions = sessions,
            sets = sets,
            exercisesById = mapOf(1L to curl),
            today = today,
        ).getValue("BICEPS")

        assertEquals(1.0f, score(ProgressTimeframe.WEEK), 0.001f)
        assertEquals(0.23f, score(ProgressTimeframe.MONTH), 0.01f)
        assertEquals(0.019f, score(ProgressTimeframe.YEAR), 0.001f)
        assertEquals(0.019f, score(ProgressTimeframe.LIFETIME), 0.001f)
    }

    @Test
    fun `previousMuscleBalanceForTimeframe uses only the immediately preceding equal window`() {
        val curl = Exercise(id = 1, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val today = LocalDate.of(2026, 8, 22)
        val sessions = listOf(
            WorkoutSession(id = 1, date = today.minusDays(8), routineId = null, startEpochMillis = 0, endEpochMillis = 0),
            WorkoutSession(id = 2, date = today, routineId = null, startEpochMillis = 0, endEpochMillis = 0),
        )
        val sets = (1..15).map { index ->
            SetLog(
                id = index.toLong(),
                sessionId = if (index <= 5) 1 else 2,
                exerciseId = 1,
                weightKg = 20.0,
                reps = 10,
                targetReps = 10,
                loggedAtEpochMillis = 0,
                rpe = 8,
            )
        }

        val previous = previousMuscleBalanceForTimeframe(
            timeframe = ProgressTimeframe.WEEK,
            sessions = sessions,
            sets = sets,
            exercisesById = mapOf(1L to curl),
            today = today,
        )

        // Five prior-week sets = half of the 10-set weekly target; ten current sets must not leak in.
        assertEquals(0.5f, previous["BICEPS"]!!, 0.001f)
    }

    @Test
    fun `previousMuscleBalanceForTimeframe has no fabricated lifetime comparison`() {
        assertEquals(
            emptyMap<String, Float>(),
            previousMuscleBalanceForTimeframe(
                timeframe = ProgressTimeframe.LIFETIME,
                sessions = emptyList(),
                sets = emptyList(),
                exercisesById = emptyMap(),
                today = LocalDate.of(2026, 8, 22),
            ),
        )
    }

    @Test
    fun `rankedMuscleBalanceBars keeps absent groups neutral and puts them after measured groups`() {
        val bars = rankedMuscleBalanceBars(
            current = mapOf("BICEPS" to 0.8f, "CHEST" to 0.4f),
            previous = mapOf("BICEPS" to 0.5f),
        )

        assertEquals(listOf("Biceps", "Chest"), bars.take(2).map { it.label })
        assertEquals(0.5f, bars.first().previous!!, 0.001f)
        assertEquals(null, bars.first { it.label == "Abs" }.current)
    }
}
