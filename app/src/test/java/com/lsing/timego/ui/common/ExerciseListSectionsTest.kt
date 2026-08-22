package com.lsing.timego.ui.common

import com.lsing.timego.data.Exercise
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
}
