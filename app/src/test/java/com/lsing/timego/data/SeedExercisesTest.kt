package com.lsing.timego.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SeedExercisesTest {
    private val holdExerciseNames = setOf("Plank", "Side Plank", "Wall Sit", "L-Sit", "Dead Hang", "Superman")

    @Test
    fun `exactly the curated hold exercises are tagged HOLD`() {
        val actualHoldNames = SEED_EXERCISES.filter { it.loggingType == LoggingType.HOLD.name }.map { it.name }.toSet()
        assertEquals(holdExerciseNames, actualHoldNames)
    }

    @Test
    fun `every STRENGTH exercise is WEIGHT_REPS`() {
        val strengthExercises = SEED_EXERCISES.filter { it.category == ExerciseCategory.STRENGTH.name }
        assertEquals(true, strengthExercises.all { it.loggingType == LoggingType.WEIGHT_REPS.name })
    }

    @Test
    fun `every CARDIO and WARMUP exercise is DURATION_DISTANCE`() {
        val durationExercises = SEED_EXERCISES.filter {
            it.category == ExerciseCategory.CARDIO.name || it.category == ExerciseCategory.WARMUP.name
        }
        assertEquals(true, durationExercises.all { it.loggingType == LoggingType.DURATION_DISTANCE.name })
    }
}
