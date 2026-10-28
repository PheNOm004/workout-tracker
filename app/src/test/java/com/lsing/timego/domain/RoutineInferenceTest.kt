package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.Routine
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import com.lsing.timego.profile.TrainingGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RoutineInferenceTest {
    private fun ex(id: Long, name: String, vararg groups: String) =
        Exercise(id = id, name = name, muscleGroups = groups.toList(), isCustom = false)

    private val pullUp = ex(1, "Pull-Up", "LATS")
    private val dip = ex(2, "Dip", "CHEST", "TRICEPS")
    private val curl = ex(3, "Preacher Curl", "BICEPS")
    private val incline = ex(4, "Incline Dumbbell Press", "UPPER_CHEST")
    private val hammer = ex(5, "Hammer Curl", "BICEPS")
    private val rearFly = ex(6, "Rear Delt Fly", "REAR_DELTS")
    private val cableCurl = ex(7, "Cable Curl", "BICEPS")
    private val all = listOf(pullUp, dip, curl, incline, hammer, rearFly, cableCurl).associateBy { it.id }

    private val routines = mapOf(
        1L to listOf(pullUp, dip, curl),
        2L to listOf(incline, hammer, rearFly),
    )

    private val day = LocalDate.of(2026, 10, 14)
    private fun freeform(id: Long, date: LocalDate = day) =
        WorkoutSession(id = id, date = date, routineId = null, startEpochMillis = 0, endEpochMillis = 1)
    private fun sets(sessionId: Long, vararg exercises: Exercise) =
        exercises.map { SetLog(sessionId = sessionId, exerciseId = it.id, weightKg = 10.0, reps = 10, targetReps = 10, loggedAtEpochMillis = 0) }

    @Test
    fun `freeform session covering most of a routine counts as that routine`() {
        val result = inferredRoutineLastDone(listOf(freeform(1)), sets(1, pullUp, dip, cableCurl), routines, all)
        assertEquals(mapOf(1L to day), result)
    }

    @Test
    fun `similar exercise with identical primary muscles stands in for the routine exercise`() {
        val result = inferredRoutineLastDone(listOf(freeform(1)), sets(1, pullUp, cableCurl), routines, all)
        assertEquals(day, result[1L])
    }

    @Test
    fun `session matching under half a routine is not credited`() {
        assertNull(inferredRoutineLastDone(listOf(freeform(1)), sets(1, pullUp), routines, all)[1L])
    }

    @Test
    fun `tied routines are not guessed`() {
        val twin = routines + (3L to routines.getValue(1L))
        assertEquals(emptyMap<Long, LocalDate>(), inferredRoutineLastDone(listOf(freeform(1)), sets(1, pullUp, dip, curl), twin, all))
    }

    @Test
    fun `routine-tagged and unfinished sessions are ignored`() {
        val tagged = freeform(1).copy(routineId = 2)
        val open = freeform(2).copy(endEpochMillis = null)
        assertEquals(emptyMap<Long, LocalDate>(), inferredRoutineLastDone(listOf(tagged, open), sets(1, pullUp, dip, curl) + sets(2, pullUp, dip, curl), routines, all))
    }

    @Test
    fun `never-done routine wins the tie when muscle recovery is level`() {
        val a = Routine(id = 1, name = "Upper A")
        val b = Routine(id = 2, name = "Upper B")
        val groups = mapOf(1L to setOf("CHEST"), 2L to setOf("CHEST"))
        val today = LocalDate.of(2026, 10, 28)
        val worked = mapOf("CHEST" to today.minusDays(14))
        val next = nextFlexibleRoutineByRecovery(listOf(a, b), groups, worked, mapOf(1L to day), today)
        assertEquals("Upper B", next?.name)
    }
}

class SessionFailsafeTest {
    private val day = LocalDate.of(2026, 10, 21)
    private val hour = 60 * 60 * 1000L
    private fun session(id: Long, routineId: Long?, start: Long, end: Long?) =
        WorkoutSession(id = id, date = day, routineId = routineId, startEpochMillis = start, endEpochMillis = end)

    @Test
    fun `recently finished same-routine session is offered for continuing`() {
        val done = session(1, 34, 0, hour)
        assertEquals(done, continuableSession(listOf(done), 34, day, nowMillis = 2 * hour))
    }

    @Test
    fun `old, other-routine, or still-active sessions are not offered`() {
        val done = session(1, 34, 0, hour)
        assertNull(continuableSession(listOf(done), 34, day, nowMillis = 6 * hour))
        assertNull(continuableSession(listOf(done), 35, day, nowMillis = 2 * hour))
        assertNull(continuableSession(listOf(session(2, 34, 0, null)), 34, day, nowMillis = 2 * hour))
    }

    @Test
    fun `same-day same-routine sessions close together form one duplicate group`() {
        val first = session(15, 34, 0, hour)
        val second = session(16, 34, 3 * hour, 3 * hour + 1000)
        assertEquals(listOf(listOf(first, second)), duplicateSessionGroups(listOf(second, first)))
    }

    @Test
    fun `freeform and far-apart sessions are never merged`() {
        assertEquals(emptyList<List<WorkoutSession>>(), duplicateSessionGroups(listOf(session(1, null, 0, hour), session(2, null, 2 * hour, 3 * hour))))
        assertEquals(emptyList<List<WorkoutSession>>(), duplicateSessionGroups(listOf(session(1, 34, 0, hour), session(2, 34, 10 * hour, 11 * hour))))
    }
}

class ProfileTuningTest {
    @Test
    fun `goal picks a default rep range only where one is characteristic`() {
        assertEquals(RepRange(3, 6), goalRepRange(TrainingGoal.STRENGTH))
        assertEquals(RepRange(6, 12), goalRepRange(TrainingGoal.HYPERTROPHY))
        assertNull(goalRepRange(TrainingGoal.GENERAL_FITNESS))
        assertNull(goalRepRange(null))
    }
}
