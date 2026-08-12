package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MuscleBalanceTest {
    private val legsExercise = Exercise(id = 1, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false)
    private val chestExercise = Exercise(id = 2, name = "Bench", muscleGroups = listOf("CHEST"), isCustom = false)

    @Test
    fun `lastTrainedDatesByMuscleGroup takes the most recent date per group`() {
        val logs = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 2, exerciseId = 1, weightKg = 105.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
        )
        val exercisesById = mapOf(1L to legsExercise)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 1), 2L to LocalDate.of(2026, 8, 5))

        val result = lastTrainedDatesByMuscleGroup(logs, exercisesById, sessionDateById)

        assertEquals(LocalDate.of(2026, 8, 5), result["QUADS"])
    }

    @Test
    fun `untrainedMuscleGroups flags groups never trained or stale`() {
        val lastTrained = mapOf("QUADS" to LocalDate.of(2026, 8, 5))
        val today = LocalDate.of(2026, 8, 14)

        val result = untrainedMuscleGroups(
            allGroups = listOf("QUADS", "CHEST"),
            lastTrainedByGroup = lastTrained,
            today = today,
            thresholdDays = 7,
        )

        assertEquals(listOf("QUADS", "CHEST"), result)
    }

    @Test
    fun `untrainedMuscleGroups excludes recently trained groups`() {
        val lastTrained = mapOf("QUADS" to LocalDate.of(2026, 8, 10))
        val today = LocalDate.of(2026, 8, 14)

        val result = untrainedMuscleGroups(
            allGroups = listOf("QUADS"),
            lastTrainedByGroup = lastTrained,
            today = today,
            thresholdDays = 7,
        )

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `rankUntrainedMuscleGroups sorts most-neglected first`() {
        val lastTrained = mapOf(
            "QUADS" to LocalDate.of(2026, 8, 10), // 4 days ago
            "CHEST" to LocalDate.of(2026, 8, 1),  // 13 days ago
        )
        val today = LocalDate.of(2026, 8, 14)

        val result = rankUntrainedMuscleGroups(
            allGroups = listOf("QUADS", "CHEST"),
            lastTrainedByGroup = lastTrained,
            today = today,
        )

        assertEquals(listOf("CHEST", "QUADS"), result)
    }

    @Test
    fun `rankUntrainedMuscleGroups ranks never-trained groups above stale-but-trained ones`() {
        val lastTrained = mapOf("CHEST" to LocalDate.of(2026, 1, 1)) // very stale, but trained at least once
        val today = LocalDate.of(2026, 8, 14)

        val result = rankUntrainedMuscleGroups(
            allGroups = listOf("QUADS", "CHEST"), // QUADS never trained
            lastTrainedByGroup = lastTrained,
            today = today,
        )

        assertEquals(listOf("QUADS", "CHEST"), result)
    }

    @Test
    fun `rankUntrainedMuscleGroups returns groups unordered by name when equally stale`() {
        val lastTrained = mapOf(
            "QUADS" to LocalDate.of(2026, 8, 5),
            "CHEST" to LocalDate.of(2026, 8, 5),
        )
        val today = LocalDate.of(2026, 8, 14)

        val result = rankUntrainedMuscleGroups(
            allGroups = listOf("QUADS", "CHEST"),
            lastTrainedByGroup = lastTrained,
            today = today,
        )

        assertEquals(setOf("QUADS", "CHEST"), result.toSet())
        assertEquals(2, result.size)
    }
}
