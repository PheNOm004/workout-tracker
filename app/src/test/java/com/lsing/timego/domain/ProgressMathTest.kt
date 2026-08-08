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
    fun `personalRecords picks heaviest, most reps, and best volume sets per exercise`() {
        val squat = com.lsing.timego.data.Exercise(id = 1, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false, category = "STRENGTH")
        val curl = com.lsing.timego.data.Exercise(id = 2, name = "Bicep Curl", muscleGroups = listOf("BICEPS"), isCustom = false, category = "STRENGTH")
        val logs = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 3, targetReps = 3, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 2, exerciseId = 1, weightKg = 80.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0),
            // A much heavier number, but on a *different* exercise -- must not contaminate Squat's PR.
            SetLog(id = 3, sessionId = 1, exerciseId = 2, weightKg = 12.0, reps = 15, targetReps = 15, loggedAtEpochMillis = 0),
        )
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 1), 2L to LocalDate.of(2026, 8, 8))
        val exercisesById = mapOf(1L to squat, 2L to curl)

        val records = personalRecords(logs, sessionDateById, exercisesById)
        val squatRecords = records.filter { it.exerciseId == 1L }

        assertEquals(3, squatRecords.size)
        assertEquals(100.0, squatRecords.first { it.type == PrType.HEAVIEST_WEIGHT }.value, 0.001)
        assertEquals(10.0, squatRecords.first { it.type == PrType.MOST_REPS }.value, 0.001)
        assertEquals(800.0, squatRecords.first { it.type == PrType.BEST_VOLUME }.value, 0.001)
        assertEquals(3, records.filter { it.exerciseId == 2L }.size)
    }

    @Test
    fun `personalRecords excludes cardio and warmup sets, which have no meaningful weight or reps`() {
        val run = com.lsing.timego.data.Exercise(id = 3, name = "Running", muscleGroups = listOf("FULL_BODY"), isCustom = false, category = "CARDIO")
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
}
