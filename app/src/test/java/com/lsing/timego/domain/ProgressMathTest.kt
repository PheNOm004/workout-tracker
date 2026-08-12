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
    fun `ProgressTimeframe sinceDate computes each window`() {
        val today = LocalDate.of(2026, 8, 12)
        val earliest = LocalDate.of(2026, 1, 1)

        assertEquals(LocalDate.of(2026, 8, 5), ProgressTimeframe.WEEK.sinceDate(earliest, today))
        assertEquals(LocalDate.of(2026, 7, 13), ProgressTimeframe.MONTH.sinceDate(earliest, today))
        assertEquals(LocalDate.of(2025, 8, 12), ProgressTimeframe.YEAR.sinceDate(earliest, today))
        assertEquals(earliest, ProgressTimeframe.LIFETIME.sinceDate(earliest, today))
    }

    @Test
    fun `ProgressTimeframe LIFETIME falls back to today with no session history`() {
        val today = LocalDate.of(2026, 8, 12)

        assertEquals(today, ProgressTimeframe.LIFETIME.sinceDate(null, today))
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
            WorkoutSession(id = 1, date = LocalDate.of(2026, 8, 1), routineId = null, startEpochMillis = 0, endEpochMillis = 0),
            WorkoutSession(id = 2, date = LocalDate.of(2026, 8, 2), routineId = null, startEpochMillis = 0, endEpochMillis = 0),
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
    fun `personalRecords picks the single best set per exercise by estimated 1RM`() {
        val squat = com.lsing.timego.data.Exercise(id = 1, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false, category = "STRENGTH")
        val curl = com.lsing.timego.data.Exercise(id = 2, name = "Bicep Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val logs = listOf(
            // 1RM = 100 * (1 + 3/30) = 110.0
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 3, targetReps = 3, loggedAtEpochMillis = 0),
            // 1RM = 80 * (1 + 10/30) = 106.67 -- heavier volume, but a lower estimated 1RM than the triple above.
            SetLog(id = 2, sessionId = 2, exerciseId = 1, weightKg = 80.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0),
            // A much heavier number, but on a *different* exercise -- must not contaminate Squat's PR.
            SetLog(id = 3, sessionId = 1, exerciseId = 2, weightKg = 12.0, reps = 15, targetReps = 15, loggedAtEpochMillis = 0),
        )
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 1), 2L to LocalDate.of(2026, 8, 8))
        val exercisesById = mapOf(1L to squat, 2L to curl)

        val records = personalRecords(logs, sessionDateById, exercisesById)
        val squatRecords = records.filter { it.exerciseId == 1L }

        assertEquals(1, squatRecords.size)
        val squatBest = squatRecords.first()
        assertEquals(PrType.BEST_SET, squatBest.type)
        assertEquals(100.0, squatBest.value, 0.001)
        assertEquals(3.0, squatBest.secondaryValue!!, 0.001)
        assertEquals(LocalDate.of(2026, 8, 1), squatBest.achievedOn)
        assertEquals(1, records.filter { it.exerciseId == 2L }.size)
    }

    @Test
    fun `personalRecords excludes cardio and warmup sets, which have no meaningful weight or reps`() {
        val run = com.lsing.timego.data.Exercise(id = 3, name = "Running", muscleGroups = listOf("FULL_BODY"), isCustom = false, category = "CARDIO", loggingType = "DURATION_DISTANCE")
        val logs = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 3, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 0, durationMinutes = 30.0, distanceKm = 5.0),
        )
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 1))
        val exercisesById = mapOf(3L to run)

        val records = personalRecords(logs, sessionDateById, exercisesById)

        assertEquals(emptyList<PersonalRecord>(), records)
    }

    @Test
    fun `muscleGroupStrengthCurve takes the best 1RM per date among tagged exercises`() {
        val squat = com.lsing.timego.data.Exercise(id = 1, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false, category = "STRENGTH")
        val legPress = com.lsing.timego.data.Exercise(id = 2, name = "Leg Press", muscleGroups = listOf("QUADS"), isCustom = false, category = "STRENGTH")
        val logs = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 1, exerciseId = 2, weightKg = 150.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
        )
        val exercisesById = mapOf(1L to squat, 2L to legPress)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 1))

        val curve = muscleGroupStrengthCurve(logs, exercisesById, sessionDateById, "QUADS")

        assertEquals(1, curve.size)
        assertEquals(estimatedOneRepMax(150.0, 5), curve[0].second, 0.001)
    }

    @Test
    fun `muscleGroupStrengthCurve excludes cardio sets even when tagged with the muscle group`() {
        // Cycling is seeded tagged QUADS/HAMSTRINGS for the muscle-nudge feature's benefit, but
        // its weightKg/reps are 0.0/0 sentinels (see SetLog) -- including it here would corrupt
        // a real strength curve with a fake near-zero 1RM data point.
        val squat = com.lsing.timego.data.Exercise(id = 1, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false, category = "STRENGTH")
        val cycling = com.lsing.timego.data.Exercise(id = 2, name = "Cycling", muscleGroups = listOf("QUADS"), isCustom = false, category = "CARDIO", loggingType = "DURATION_DISTANCE")
        val logs = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 2, exerciseId = 2, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 0, durationMinutes = 30.0, distanceKm = 10.0),
        )
        val exercisesById = mapOf(1L to squat, 2L to cycling)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 1), 2L to LocalDate.of(2026, 8, 2))

        val curve = muscleGroupStrengthCurve(logs, exercisesById, sessionDateById, "QUADS")

        assertEquals(1, curve.size)
        assertEquals(LocalDate.of(2026, 8, 1), curve[0].first)
    }

    @Test
    fun `personalRecords computes longest hold for HOLD exercises`() {
        val plank = com.lsing.timego.data.Exercise(id = 4, name = "Plank", muscleGroups = listOf("ABS"), isCustom = false, category = "CALISTHENICS", loggingType = "HOLD")
        val logs = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 4, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 0, holdSeconds = 30, targetHoldSeconds = 30),
            SetLog(id = 2, sessionId = 2, exerciseId = 4, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 0, holdSeconds = 45, targetHoldSeconds = 35),
        )
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 1), 2L to LocalDate.of(2026, 8, 8))
        val exercisesById = mapOf(4L to plank)

        val records = personalRecords(logs, sessionDateById, exercisesById)

        assertEquals(1, records.size)
        assertEquals(PrType.LONGEST_HOLD, records[0].type)
        assertEquals(45.0, records[0].value, 0.001)
        assertEquals(LocalDate.of(2026, 8, 8), records[0].achievedOn)
    }

    @Test
    fun `muscleGroupStrengthCurve excludes HOLD sets, which have no meaningful weight`() {
        val squat = com.lsing.timego.data.Exercise(id = 1, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false, category = "STRENGTH", loggingType = "WEIGHT_REPS")
        val wallSit = com.lsing.timego.data.Exercise(id = 5, name = "Wall Sit", muscleGroups = listOf("QUADS"), isCustom = false, category = "CALISTHENICS", loggingType = "HOLD")
        val logs = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 2, exerciseId = 5, weightKg = 0.0, reps = 0, targetReps = 0, loggedAtEpochMillis = 0, holdSeconds = 60, targetHoldSeconds = 60),
        )
        val exercisesById = mapOf(1L to squat, 5L to wallSit)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 1), 2L to LocalDate.of(2026, 8, 2))

        val curve = muscleGroupStrengthCurve(logs, exercisesById, sessionDateById, "QUADS")

        assertEquals(1, curve.size)
        assertEquals(LocalDate.of(2026, 8, 1), curve[0].first)
    }
}
