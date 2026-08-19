package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.TrainingLean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SuggestedExerciseTest {
    private val benchPress = Exercise(id = 1, name = "Bench Press", muscleGroups = listOf("CHEST"), isCustom = false)
    private val pushUp = Exercise(
        id = 2,
        name = "Push-Up",
        muscleGroups = listOf("CHEST"),
        isCustom = false,
        category = ExerciseCategory.CALISTHENICS.name,
    )
    private val squat = Exercise(id = 3, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false)
    private val exercises = listOf(benchPress, pushUp, squat)

    @Test
    fun `BALANCED returns least-used matching exercise regardless of category`() {
        val result = suggestedExerciseFor(
            setOf("CHEST"),
            exercises,
            TrainingLean.BALANCED,
            mapOf(1L to 5, 2L to 1),
        )

        assertEquals(pushUp, result)
    }

    @Test
    fun `STRENGTH excludes calisthenics when a non-calisthenics match exists`() {
        val result = suggestedExerciseFor(setOf("CHEST"), exercises, TrainingLean.STRENGTH, emptyMap())

        assertEquals(benchPress, result)
    }

    @Test
    fun `CALISTHENICS excludes non-calisthenics when a calisthenics match exists`() {
        val result = suggestedExerciseFor(setOf("CHEST"), exercises, TrainingLean.CALISTHENICS, emptyMap())

        assertEquals(pushUp, result)
    }

    @Test
    fun `falls back to unfiltered candidates when the leaned category has no match`() {
        val result = suggestedExerciseFor(setOf("QUADS"), exercises, TrainingLean.CALISTHENICS, emptyMap())

        assertEquals(squat, result)
    }

    @Test
    fun `returns null when no exercise matches target groups`() {
        assertNull(suggestedExerciseFor(setOf("TRAPS"), exercises, TrainingLean.BALANCED, emptyMap()))
    }

    @Test
    fun `ties are broken by name`() {
        val result = suggestedExerciseFor(
            setOf("CHEST"),
            exercises,
            TrainingLean.BALANCED,
            mapOf(1L to 3, 2L to 3),
        )

        assertEquals(benchPress, result)
    }
}
