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
    fun `primaryMuscleGroups excludes synergist and stabilizer tags below the primary-mover threshold`() {
        val benchPress = Exercise(
            id = 4,
            name = "Bench Press",
            muscleGroups = listOf("CHEST", "TRICEPS", "FRONT_DELTS"),
            muscleWeights = mapOf("CHEST" to 100, "TRICEPS" to 65, "FRONT_DELTS" to 40),
            isCustom = false,
        )

        assertEquals(setOf("CHEST"), primaryMuscleGroups(benchPress))
    }

    @Test
    fun `primaryMuscleGroups treats an unweighted tagged group as fully primary`() {
        assertEquals(setOf("QUADS"), primaryMuscleGroups(legsExercise))
    }

    @Test
    fun `lastTrainedDatesByMuscleGroup ignores synergist-only muscle groups`() {
        val benchPress = Exercise(
            id = 4,
            name = "Bench Press",
            muscleGroups = listOf("CHEST", "TRICEPS"),
            muscleWeights = mapOf("CHEST" to 100, "TRICEPS" to 65),
            isCustom = false,
        )
        val logs = listOf(
            SetLog(id = 1, sessionId = 1, exerciseId = 4, weightKg = 60.0, reps = 8, targetReps = 8, loggedAtEpochMillis = 0),
        )
        val exercisesById = mapOf(4L to benchPress)
        val sessionDateById = mapOf(1L to LocalDate.of(2026, 8, 5))

        val result = lastTrainedDatesByMuscleGroup(logs, exercisesById, sessionDateById)

        assertEquals(LocalDate.of(2026, 8, 5), result["CHEST"])
        assertEquals(null, result["TRICEPS"])
    }

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

    @Test
    fun `muscleGroupsWorkedInSession unions muscle groups across a session's sets`() {
        val setLogs = listOf(
            SetLog(id = 1, sessionId = 10, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 10, exerciseId = 2, weightKg = 60.0, reps = 8, targetReps = 8, loggedAtEpochMillis = 0),
        )

        val result = muscleGroupsWorkedInSession(sessionId = 10, setLogs = setLogs, exercises = listOf(legsExercise, chestExercise))

        assertEquals(setOf("QUADS", "CHEST"), result)
    }

    @Test
    fun `muscleGroupsWorkedInSession ignores sets from other sessions`() {
        val setLogs = listOf(
            SetLog(id = 1, sessionId = 10, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 99, exerciseId = 2, weightKg = 60.0, reps = 8, targetReps = 8, loggedAtEpochMillis = 0),
        )

        val result = muscleGroupsWorkedInSession(sessionId = 10, setLogs = setLogs, exercises = listOf(legsExercise, chestExercise))

        assertEquals(setOf("QUADS"), result)
    }

    @Test
    fun `muscleGroupsWorkedInSession returns empty set for a session with no sets`() {
        val result = muscleGroupsWorkedInSession(sessionId = 10, setLogs = emptyList(), exercises = listOf(legsExercise))

        assertEquals(emptySet<String>(), result)
    }

    @Test
    fun `muscleGroupsWorkedInSession excludes synergist-only muscle groups`() {
        val benchPress = Exercise(
            id = 4,
            name = "Bench Press",
            muscleGroups = listOf("CHEST", "TRICEPS", "FRONT_DELTS"),
            muscleWeights = mapOf("CHEST" to 100, "TRICEPS" to 65, "FRONT_DELTS" to 40),
            isCustom = false,
        )
        val setLogs = listOf(
            SetLog(id = 1, sessionId = 10, exerciseId = 4, weightKg = 60.0, reps = 8, targetReps = 8, loggedAtEpochMillis = 0),
        )

        val result = muscleGroupsWorkedInSession(sessionId = 10, setLogs = setLogs, exercises = listOf(benchPress))

        assertEquals(setOf("CHEST"), result)
    }

    @Test
    fun `muscleGroupsWorkedInSession dedupes when two exercises share a muscle group`() {
        val secondLegsExercise = Exercise(id = 3, name = "Leg Press", muscleGroups = listOf("QUADS"), isCustom = false)
        val setLogs = listOf(
            SetLog(id = 1, sessionId = 10, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0),
            SetLog(id = 2, sessionId = 10, exerciseId = 3, weightKg = 80.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0),
        )

        val result = muscleGroupsWorkedInSession(sessionId = 10, setLogs = setLogs, exercises = listOf(legsExercise, secondLegsExercise))

        assertEquals(setOf("QUADS"), result)
    }
}
