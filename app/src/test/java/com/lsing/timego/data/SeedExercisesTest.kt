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
    fun `every muscle tag is a declared muscle group`() {
        val declared = MuscleGroup.entries.map { it.name }.toSet()
        val unknown = SEED_EXERCISES.flatMap { exercise ->
            exercise.muscleGroups.filter { it !in declared }.map { group -> exercise.name to group }
        }
        assertEquals(emptyList<Pair<String, String>>(), unknown)
    }

    @Test
    fun `strength and calisthenics compounds have explicit secondary weights`() {
        val unweightedCompounds = SEED_EXERCISES.filter { exercise ->
            exercise.category == ExerciseCategory.STRENGTH.name || exercise.category == ExerciseCategory.CALISTHENICS.name
        }.filter { it.muscleGroups.size > 1 && it.muscleWeights.isEmpty() }
        assertEquals(emptyList<Exercise>(), unweightedCompounds)
    }

    @Test
    fun `adductor exercises do not inflate glute volume`() {
        val adductorNames = setOf(
            "Hip Adductor Machine", "Adductor Squeeze (Ball)", "Copenhagen Adductor Raise",
            "Cable Hip Adduction", "Standing Adductor Cable Raise", "Copenhagen Plank",
            "Copenhagen Plank Raise",
        )
        val adductorExercises = SEED_EXERCISES.filter { it.name in adductorNames }
        assertEquals(adductorNames, adductorExercises.map { it.name }.toSet())
        assertEquals(true, adductorExercises.all { MuscleGroup.ADDUCTORS.name in it.muscleGroups })
        assertEquals(true, adductorExercises.all { MuscleGroup.GLUTES.name !in it.muscleGroups })
    }

    @Test
    fun `deadlift variants weight lower back as a contributor rather than an implicit primary`() {
        val names = setOf(
            "Conventional Deadlift", "Sumo Deadlift", "Rack Pull", "Trap Bar Deadlift",
            "Deficit Deadlift", "Snatch-Grip Deadlift", "Deficit Sumo Deadlift", "Block Pull",
            "Pin Pull (Deadlift)", "Jefferson Deadlift", "Banded Deadlift", "Chain Deadlift",
            "Suitcase Deadlift",
        )
        val deadlifts = SEED_EXERCISES.filter { it.name in names }
        assertEquals(names, deadlifts.map { it.name }.toSet())
        assertEquals(true, deadlifts.all { (it.muscleWeights[MuscleGroup.LOWER_BACK.name] ?: 0) in 1..69 })
        assertEquals(true, MuscleGroup.QUADS.name in SEED_EXERCISES.first { it.name == "Sumo Deadlift" }.muscleGroups)
    }

    @Test
    fun `trap-relevant library exercises expose traps as weighted contributors`() {
        val names = setOf(
            "Seated Cable Row", "Seated Dumbbell Shoulder Press", "Reverse Cable Crossover",
            "Conventional Deadlift", "Farmer's Carry", "Shrugs", "Prone Trap Raise",
        )
        val exercises = SEED_EXERCISES.filter { it.name in names }
        assertEquals(names, exercises.map { it.name }.toSet())
        assertEquals(emptyList<String>(), exercises.filter { MuscleGroup.TRAPS.name !in it.muscleGroups }.map { it.name })
        assertEquals(emptyList<String>(), exercises.filter { (it.muscleWeights[MuscleGroup.TRAPS.name] ?: 100) !in 1..100 }.map { it.name })
    }

    @Test
    fun `bodyweight-only knee-dominant exercises use bodyweight logging`() {
        val bodyweightNames = setOf("Sissy Squat", "Nordic Hamstring Curl", "Reverse Nordic Curl")
        val exercises = SEED_EXERCISES.filter { it.name in bodyweightNames }
        assertEquals(bodyweightNames, exercises.map { it.name }.toSet())
        assertEquals(true, exercises.all { it.category == ExerciseCategory.CALISTHENICS.name })
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
    fun `library has grown to roughly 600 exercises`() {
        assertEquals(true, SEED_EXERCISES.size in 550..700)
    }

    @Test
    fun `every HOLD exercise is CALISTHENICS`() {
        val holdExercises = SEED_EXERCISES.filter { it.loggingType == LoggingType.HOLD.name }
        assertEquals(true, holdExercises.all { it.category == ExerciseCategory.CALISTHENICS.name })
    }
}
