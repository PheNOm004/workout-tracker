package com.lsing.timego.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SeedExercisesTest {
    private val holdExerciseNames = setOf(
        "Plank", "Side Plank", "Wall Sit", "L-Sit", "Dead Hang", "Superman",
        "Planche Lean", "Hollow Body Hold", "Copenhagen Plank", "Wall Handstand Hold",
        "Tuck Planche Hold", "Front Lever Hold", "Back Lever Hold", "Human Flag Hold",
        // Imported (external exercise library import)
        "Frog Stand", "Advanced Tuck Planche Hold", "Straddle Planche Hold", "Full Planche Hold",
        "Freestanding Handstand Hold", "Tuck Front Lever", "Advanced Tuck Front Lever",
        "Straddle Front Lever", "Tuck Back Lever", "Straddle Back Lever", "Ring Support Hold",
        "Bent-Arm Iron Cross Hold", "Star Plank", "Single-Leg Wall Sit", "Human Flag Tuck",
        "Human Flag Straddle", "Bear Crawl Hold", "Animal Flow Beast Hold",
    )

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

    @Test
    fun `every muscleWeights key is one of the exercise's own tagged muscle groups`() {
        val orphaned = SEED_EXERCISES.filter { exercise ->
            exercise.muscleWeights.keys.any { it !in exercise.muscleGroups }
        }
        assertEquals(emptyList<Exercise>(), orphaned)
    }

    @Test
    fun `every muscleWeights value is between 1 and 100`() {
        val outOfRange = SEED_EXERCISES.filter { exercise -> exercise.muscleWeights.values.any { it !in 1..100 } }
        assertEquals(emptyList<Exercise>(), outOfRange)
    }

    @Test
    fun `no duplicate exercise names`() {
        val names = SEED_EXERCISES.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `library has grown to roughly 300 exercises`() {
        assertEquals(true, SEED_EXERCISES.size in 250..350)
    }
}
