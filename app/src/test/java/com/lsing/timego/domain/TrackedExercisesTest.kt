package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TrackedExercisesTest {
    private fun ex(id: Long, name: String) = Exercise(id = id, name = name, muscleGroups = listOf("CHEST"), isCustom = false)
    private fun session(id: Long, start: Long) =
        WorkoutSession(id = id, date = LocalDate.of(2026, 10, 1), routineId = null, startEpochMillis = start, endEpochMillis = start + 1)
    private fun set(sessionId: Long, exerciseId: Long, warmup: Boolean = false) =
        SetLog(sessionId = sessionId, exerciseId = exerciseId, weightKg = 10.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 0, isWarmup = warmup)

    private val exercises = listOf(ex(1, "Bench"), ex(2, "Row"), ex(3, "Curl"), ex(4, "Never done"))

    @Test
    fun `only exercises with working sets appear, most recent first`() {
        val sessions = listOf(session(10, 1000), session(11, 2000))
        val sets = listOf(set(10, 1), set(11, 2), set(10, 3, warmup = true))
        assertEquals(listOf("Row", "Bench"), trackedExercises(sets, sessions, exercises).map { it.name })
    }

    @Test
    fun `no logged work gives an empty list`() {
        assertEquals(emptyList<Exercise>(), trackedExercises(emptyList(), emptyList(), exercises))
    }
}
