package com.lsing.timego.domain

import com.lsing.timego.data.BodyMetric
import com.lsing.timego.data.Exercise
import com.lsing.timego.data.Routine
import com.lsing.timego.data.RoutineExercise
import com.lsing.timego.data.SetLog
import com.lsing.timego.data.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

private fun exercise(id: Long, name: String, catalogueKey: String? = null) =
    Exercise(id = id, name = name, catalogueKey = catalogueKey, muscleGroups = emptyList(), isCustom = catalogueKey == null)

private fun session(id: Long, startEpochMillis: Long) =
    WorkoutSession(id = id, date = LocalDate.of(2026, 8, 9), routineId = null, startEpochMillis = startEpochMillis, endEpochMillis = startEpochMillis + 1_000)

private fun setLog(id: Long, sessionId: Long, exerciseId: Long) =
    SetLog(id = id, sessionId = sessionId, exerciseId = exerciseId, weightKg = 60.0, reps = 5, targetReps = 5, loggedAtEpochMillis = 1_000)

class BackupMergeTest {

    @Test
    fun `session absent from live is imported with its sets, exercise ids resolved by catalogue key`() {
        val backupExercise = exercise(id = 1, name = "Bench Press", catalogueKey = "timego.seed.v1.bench-press")
        val liveExercise = exercise(id = 99, name = "Bench Press", catalogueKey = "timego.seed.v1.bench-press")
        val backupSession = session(id = 1, startEpochMillis = 111L)
        val backupSet = setLog(id = 1, sessionId = 1, exerciseId = 1)

        val plan = planBackupMerge(
            backupSessions = listOf(backupSession),
            backupSetLogs = listOf(backupSet),
            backupExercises = listOf(backupExercise),
            backupRoutines = emptyList(),
            backupRoutineExercises = emptyList(),
            backupBodyMetrics = emptyList(),
            liveSessions = emptyList(),
            liveExercises = listOf(liveExercise),
            liveRoutines = emptyList(),
            liveBodyMetrics = emptyList(),
        )

        assertEquals(1, plan.sessionsToImport.size)
        assertEquals(0, plan.skippedSessionCount)
        val imported = plan.sessionsToImport.single()
        assertEquals(0L, imported.session.id)
        assertEquals(1, imported.sets.size)
        assertEquals(99L, imported.sets.single().exerciseId)
        assertEquals(0L, imported.sets.single().id)
        assertEquals(0, plan.skippedSetCountUnknownExercise)
    }

    @Test
    fun `session already present live by start time is skipped entirely, not merged`() {
        val liveSession = session(id = 5, startEpochMillis = 222L)
        val backupSession = session(id = 1, startEpochMillis = 222L)
        val backupSet = setLog(id = 1, sessionId = 1, exerciseId = 1)

        val plan = planBackupMerge(
            backupSessions = listOf(backupSession),
            backupSetLogs = listOf(backupSet),
            backupExercises = listOf(exercise(1, "Bench Press", "timego.seed.v1.bench-press")),
            backupRoutines = emptyList(),
            backupRoutineExercises = emptyList(),
            backupBodyMetrics = emptyList(),
            liveSessions = listOf(liveSession),
            liveExercises = listOf(exercise(99, "Bench Press", "timego.seed.v1.bench-press")),
            liveRoutines = emptyList(),
            liveBodyMetrics = emptyList(),
        )

        assertEquals(0, plan.sessionsToImport.size)
        assertEquals(1, plan.skippedSessionCount)
    }

    @Test
    fun `set referencing an exercise with no live match is skipped and counted, session still imports`() {
        val backupSession = session(id = 1, startEpochMillis = 333L)
        val resolvableSet = setLog(id = 1, sessionId = 1, exerciseId = 1)
        val unresolvableSet = setLog(id = 2, sessionId = 1, exerciseId = 2)

        val plan = planBackupMerge(
            backupSessions = listOf(backupSession),
            backupSetLogs = listOf(resolvableSet, unresolvableSet),
            backupExercises = listOf(
                exercise(1, "Bench Press", "timego.seed.v1.bench-press"),
                exercise(2, "Some Removed Exercise", catalogueKey = null),
            ),
            backupRoutines = emptyList(),
            backupRoutineExercises = emptyList(),
            backupBodyMetrics = emptyList(),
            liveSessions = emptyList(),
            liveExercises = listOf(exercise(99, "Bench Press", "timego.seed.v1.bench-press")),
            liveRoutines = emptyList(),
            liveBodyMetrics = emptyList(),
        )

        val imported = plan.sessionsToImport.single()
        assertEquals(1, imported.sets.size)
        assertEquals(1, plan.skippedSetCountUnknownExercise)
    }

    @Test
    fun `custom exercise with no catalogue key resolves by exact name match`() {
        val backupSession = session(id = 1, startEpochMillis = 444L)
        val backupSet = setLog(id = 1, sessionId = 1, exerciseId = 1)

        val plan = planBackupMerge(
            backupSessions = listOf(backupSession),
            backupSetLogs = listOf(backupSet),
            backupExercises = listOf(exercise(1, "My Custom Move", catalogueKey = null)),
            backupRoutines = emptyList(),
            backupRoutineExercises = emptyList(),
            backupBodyMetrics = emptyList(),
            liveSessions = emptyList(),
            liveExercises = listOf(exercise(50, "My Custom Move", catalogueKey = null)),
            liveRoutines = emptyList(),
            liveBodyMetrics = emptyList(),
        )

        assertEquals(50L, plan.sessionsToImport.single().sets.single().exerciseId)
        assertEquals(0, plan.skippedSetCountUnknownExercise)
    }

    @Test
    fun `routine with a name not present live is imported with resolved ordered exercise ids`() {
        val backupExercises = listOf(
            exercise(1, "Squat", "timego.seed.v1.squat"),
            exercise(2, "Bench Press", "timego.seed.v1.bench-press"),
        )
        val liveExercises = listOf(
            exercise(10, "Squat", "timego.seed.v1.squat"),
            exercise(20, "Bench Press", "timego.seed.v1.bench-press"),
        )
        val routine = Routine(id = 1, name = "Push Day", daysOfWeek = listOf("MONDAY"))
        val routineExercises = listOf(
            RoutineExercise(id = 1, routineId = 1, exerciseId = 2, orderIndex = 0),
            RoutineExercise(id = 2, routineId = 1, exerciseId = 1, orderIndex = 1),
        )

        val plan = planBackupMerge(
            backupSessions = emptyList(),
            backupSetLogs = emptyList(),
            backupExercises = backupExercises,
            backupRoutines = listOf(routine),
            backupRoutineExercises = routineExercises,
            backupBodyMetrics = emptyList(),
            liveSessions = emptyList(),
            liveExercises = liveExercises,
            liveRoutines = emptyList(),
            liveBodyMetrics = emptyList(),
        )

        assertEquals(1, plan.routinesToImport.size)
        val imported = plan.routinesToImport.single()
        assertEquals(0L, imported.routine.id)
        assertEquals(listOf(20L, 10L), imported.exerciseIds)
        assertEquals(0, plan.skippedRoutineCount)
    }

    @Test
    fun `routine with a name already present live is skipped`() {
        val plan = planBackupMerge(
            backupSessions = emptyList(),
            backupSetLogs = emptyList(),
            backupExercises = emptyList(),
            backupRoutines = listOf(Routine(id = 1, name = "Push Day")),
            backupRoutineExercises = emptyList(),
            backupBodyMetrics = emptyList(),
            liveSessions = emptyList(),
            liveExercises = emptyList(),
            liveRoutines = listOf(Routine(id = 9, name = "Push Day")),
            liveBodyMetrics = emptyList(),
        )

        assertEquals(0, plan.routinesToImport.size)
        assertEquals(1, plan.skippedRoutineCount)
    }

    @Test
    fun `body metric on a date not present live is imported, an existing date is skipped`() {
        val plan = planBackupMerge(
            backupSessions = emptyList(),
            backupSetLogs = emptyList(),
            backupExercises = emptyList(),
            backupRoutines = emptyList(),
            backupRoutineExercises = emptyList(),
            backupBodyMetrics = listOf(
                BodyMetric(id = 1, date = LocalDate.of(2026, 8, 9), weightKg = 70.0, waistCm = null),
                BodyMetric(id = 2, date = LocalDate.of(2026, 8, 11), weightKg = 71.0, waistCm = null),
            ),
            liveSessions = emptyList(),
            liveExercises = emptyList(),
            liveRoutines = emptyList(),
            liveBodyMetrics = listOf(BodyMetric(id = 5, date = LocalDate.of(2026, 8, 9), weightKg = 69.5, waistCm = null)),
        )

        assertEquals(1, plan.bodyMetricsToImport.size)
        assertEquals(LocalDate.of(2026, 8, 11), plan.bodyMetricsToImport.single().date)
        assertEquals(1, plan.skippedBodyMetricCount)
    }
}
