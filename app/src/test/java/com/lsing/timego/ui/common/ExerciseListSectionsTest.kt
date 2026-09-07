package com.lsing.timego.ui.common

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseListSectionsTest {
    @Test
    fun `boundedExerciseSearch limits broad matches while preserving the supplied order`() {
        val exercises = (1..45).map { index ->
            Exercise(id = index.toLong(), name = "Press $index", muscleGroups = listOf("CHEST"), isCustom = false)
        }

        val matches = boundedExerciseSearch(exercises, "press")

        assertEquals(EXERCISE_SEARCH_RESULT_LIMIT, matches.size)
        assertEquals("Press 1", matches.first().name)
        assertEquals("Press 40", matches.last().name)
    }

    @Test
    fun `boundedExerciseSearch treats spaced and hyphenated queries as equivalent`() {
        val exercise = Exercise(id = 1, name = "Pull-Up", muscleGroups = listOf("LATS"), isCustom = false)

        assertEquals(listOf(exercise), boundedExerciseSearch(listOf(exercise), "pull up"))
    }

    @Test
    fun `sessionDayLabel prioritizes volume dominance and suppresses trace finishers`() {
        val legPress = Exercise(id = 1, name = "Leg Press", muscleGroups = listOf("QUADS"), isCustom = false)
        val shoulderPress = Exercise(id = 2, name = "Shoulder Press", muscleGroups = listOf("FRONT_DELTS", "SIDE_DELTS"), isCustom = false)
        val deadHang = Exercise(id = 3, name = "Dead Hang", muscleGroups = listOf("FOREARMS"), isCustom = false)

        val exercisesById = mapOf(1L to legPress, 2L to shoulderPress, 3L to deadHang)
        // 12 sets of Legs, 6 sets of Shoulders, 2 sets of Dead Hang (Forearms)
        val logs = (1..12).map { SetLog(id = it.toLong(), sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 10, targetReps = 10, loggedAtEpochMillis = it.toLong()) } +
            (13..18).map { SetLog(id = it.toLong(), sessionId = 1, exerciseId = 2, weightKg = 20.0, reps = 10, targetReps = 10, loggedAtEpochMillis = it.toLong()) } +
            (19..20).map { SetLog(id = it.toLong(), sessionId = 1, exerciseId = 3, weightKg = 0.0, reps = 0, holdSeconds = 60, targetReps = 0, loggedAtEpochMillis = it.toLong()) }

        val label = sessionDayLabel(logs, exercisesById)
        assertEquals("Legs & Shoulders", label)
    }

    @Test
    fun `sessionDayLabel detects Push split archetype`() {
        val bench = Exercise(id = 1, name = "Bench Press", muscleGroups = listOf("CHEST"), isCustom = false)
        val ohp = Exercise(id = 2, name = "Overhead Press", muscleGroups = listOf("FRONT_DELTS"), isCustom = false)
        val tricepPushdown = Exercise(id = 3, name = "Tricep Pushdown", muscleGroups = listOf("TRICEPS"), isCustom = false)

        val exercisesById = mapOf(1L to bench, 2L to ohp, 3L to tricepPushdown)
        val logs = (1..6).map { SetLog(id = it.toLong(), sessionId = 1, exerciseId = 1, weightKg = 80.0, reps = 8, targetReps = 8, loggedAtEpochMillis = it.toLong()) } +
            (7..12).map { SetLog(id = it.toLong(), sessionId = 1, exerciseId = 2, weightKg = 50.0, reps = 8, targetReps = 8, loggedAtEpochMillis = it.toLong()) } +
            (13..15).map { SetLog(id = it.toLong(), sessionId = 1, exerciseId = 3, weightKg = 25.0, reps = 12, targetReps = 12, loggedAtEpochMillis = it.toLong()) }

        val label = sessionDayLabel(logs, exercisesById)
        assertEquals("Push", label)
    }

    @Test
    fun `sessionDayLabel detects Pull split archetype and groups Rear Delts into Back`() {
        val pullUp = Exercise(id = 1, name = "Pull-Up", muscleGroups = listOf("LATS", "UPPER_BACK"), isCustom = false)
        val facePull = Exercise(id = 2, name = "Face Pull", muscleGroups = listOf("REAR_DELTS"), isCustom = false)
        val bicepCurl = Exercise(id = 3, name = "Bicep Curl", muscleGroups = listOf("BICEPS"), isCustom = false)

        val exercisesById = mapOf(1L to pullUp, 2L to facePull, 3L to bicepCurl)
        val logs = (1..6).map { SetLog(id = it.toLong(), sessionId = 1, exerciseId = 1, weightKg = 0.0, reps = 10, targetReps = 10, loggedAtEpochMillis = it.toLong()) } +
            (7..9).map { SetLog(id = it.toLong(), sessionId = 1, exerciseId = 2, weightKg = 20.0, reps = 15, targetReps = 15, loggedAtEpochMillis = it.toLong()) } +
            (10..13).map { SetLog(id = it.toLong(), sessionId = 1, exerciseId = 3, weightKg = 14.0, reps = 10, targetReps = 10, loggedAtEpochMillis = it.toLong()) }

        val label = sessionDayLabel(logs, exercisesById)
        assertEquals("Back & Arms", label)
    }

    @Test
    fun `sessionDayLabel detects Full Body archetype`() {
        val squat = Exercise(id = 1, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false)
        val bench = Exercise(id = 2, name = "Bench Press", muscleGroups = listOf("CHEST"), isCustom = false)
        val row = Exercise(id = 3, name = "Barbell Row", muscleGroups = listOf("UPPER_BACK"), isCustom = false)

        val exercisesById = mapOf(1L to squat, 2L to bench, 3L to row)
        val logs = (1..5).map { SetLog(id = it.toLong(), sessionId = 1, exerciseId = 1, weightKg = 100.0, reps = 5, targetReps = 5, loggedAtEpochMillis = it.toLong()) } +
            (6..10).map { SetLog(id = it.toLong(), sessionId = 1, exerciseId = 2, weightKg = 80.0, reps = 5, targetReps = 5, loggedAtEpochMillis = it.toLong()) } +
            (11..15).map { SetLog(id = it.toLong(), sessionId = 1, exerciseId = 3, weightKg = 70.0, reps = 5, targetReps = 5, loggedAtEpochMillis = it.toLong()) }

        val label = sessionDayLabel(logs, exercisesById)
        assertEquals("Full Body", label)
    }

    @Test
    fun `exerciseMatchesFilter matches Rear Delts under Back as well as Shoulders`() {
        val facePull = Exercise(id = 1, name = "Face Pull", muscleGroups = listOf("REAR_DELTS"), isCustom = false)
        assertEquals(true, exerciseMatchesFilter(facePull, MuscleFilterOption.BACK))
        assertEquals(true, exerciseMatchesFilter(facePull, MuscleFilterOption.SHOULDERS))
    }
}
