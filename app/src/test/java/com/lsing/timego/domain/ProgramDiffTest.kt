package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramDiffTest {
    private val bench = Exercise(id = 1, name = "Bench", muscleGroups = listOf("CHEST", "TRICEPS"), isCustom = false, muscleWeights = mapOf("TRICEPS" to 50))
    private val curl = Exercise(id = 2, name = "Curl", muscleGroups = listOf("BICEPS"), isCustom = false)

    @Test fun projectionScalesBySetsFrequencyAndMuscleWeights() {
        val projected = projectedWeeklySets(
            listOf(ProjectedRoutine(listOf("Bench", "Curl"), timesPerWeek = 2), ProjectedRoutine(listOf("Bench"))),
            mapOf("Bench" to bench, "Curl" to curl),
        )
        assertEquals(9.0, projected["CHEST"]!!, 1e-9)
        assertEquals(4.5, projected["TRICEPS"]!!, 1e-9)
        assertEquals(6.0, projected["BICEPS"]!!, 1e-9)
    }

    @Test fun unknownExercisesAreSkippedAndFrequencyIsAtLeastOnce() {
        val projected = projectedWeeklySets(listOf(ProjectedRoutine(listOf("Nope", "Curl"), timesPerWeek = 0)), mapOf("Curl" to curl))
        assertEquals(mapOf("BICEPS" to 3.0), projected)
    }

    @Test fun trailingAverageCountsWorkingSetsInTheWindowPerWeek() {
        val today = LocalDate.of(2026, 11, 24)
        val sessions = listOf(
            WorkoutSession(id = 1, date = today.minusDays(3), routineId = null, startEpochMillis = 0, endEpochMillis = null),
            WorkoutSession(id = 2, date = today.minusDays(60), routineId = null, startEpochMillis = 0, endEpochMillis = null),
        )
        fun set(session: Long, warmup: Boolean = false) =
            SetLog(sessionId = session, exerciseId = 2, weightKg = 10.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 1, isWarmup = warmup)
        val sets = List(6) { set(1) } + set(1, warmup = true) + List(10) { set(2) }
        val weekly = trailingWeeklySets(sets, sessions, mapOf(2L to curl), today, days = 28)
        assertEquals(6.0 / 4.0, weekly["BICEPS"]!!, 1e-9)
    }

    @Test fun diffPicksTopThreeGainersAndDropsAndIgnoresSmallShifts() {
        val projected = mapOf("A" to 10.0, "B" to 8.0, "C" to 6.0, "D" to 5.0, "E" to 0.0, "F" to 4.2)
        val current = mapOf("A" to 4.0, "B" to 4.0, "C" to 4.0, "D" to 4.0, "E" to 9.0, "F" to 4.0, "G" to 3.0)
        val diff = diffWeeklySets(projected, current)
        assertEquals(listOf("A", "B", "C"), diff.gainers.map { it.muscle })
        assertEquals(listOf("E", "G"), diff.drops.map { it.muscle })
        assertTrue(diff.gainers.none { it.muscle == "F" })
        assertEquals(6.0, diff.gainers.first().delta, 1e-9)
    }
}
