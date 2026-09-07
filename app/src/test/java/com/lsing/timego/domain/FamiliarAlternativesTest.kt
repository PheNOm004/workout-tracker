package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.LoggingType
import com.lsing.timego.data.TrainingLean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FamiliarAlternativesTest {
    private fun ex(
        id: Long,
        name: String,
        groups: List<String> = listOf("CHEST"),
        weights: Map<String, Int> = emptyMap(),
        category: String = ExerciseCategory.STRENGTH.name,
        loggingType: String = LoggingType.WEIGHT_REPS.name,
    ) = Exercise(
        id = id,
        name = name,
        muscleGroups = groups,
        isCustom = false,
        category = category,
        loggingType = loggingType,
        muscleWeights = weights,
    )

    private val benchPress = ex(1, "Bench Press")
    private val inclinePress = ex(2, "Incline Press")
    private val dips = ex(3, "Dips", category = ExerciseCategory.CALISTHENICS.name)
    private val cableFly = ex(4, "Cable Fly")
    private val treadmill = ex(5, "Treadmill", groups = listOf("QUADS"), category = ExerciseCategory.CARDIO.name)
    private val allExercises = listOf(benchPress, inclinePress, dips, cableFly, treadmill)

    private fun call(
        exercises: List<Exercise> = allExercises,
        lean: TrainingLean = TrainingLean.BALANCED,
        usageCounts: Map<Long, Int> = emptyMap(),
        logged: Set<Long> = emptySet(),
        routine: Set<Long> = emptySet(),
        excluded: Set<Long> = emptySet(),
        rejectedId: Long? = null,
        lastShownId: Long? = null,
        targets: Set<String> = setOf("CHEST"),
    ) = familiarAlternativesFor(
        targetGroups = targets,
        exercises = exercises,
        lean = lean,
        usageCounts = usageCounts,
        loggedExerciseIds = logged,
        routineExerciseIds = routine,
        excludedIds = excluded,
        rejectedId = rejectedId,
        lastShownId = lastShownId,
    )

    @Test
    fun `routine members rank ahead of non-members`() {
        val result = call(routine = setOf(cableFly.id))
        assertEquals(cableFly, result.first())
    }

    @Test
    fun `previously logged ranks ahead of never logged, below routine membership`() {
        val result = call(logged = setOf(inclinePress.id), usageCounts = mapOf(1L to 0, 2L to 9))
        // inclinePress is logged; the others are not, so it leads despite higher usage.
        assertEquals(inclinePress, result.first())
    }

    @Test
    fun `lower usage breaks ties among otherwise equal candidates`() {
        val result = call(usageCounts = mapOf(1L to 5, 2L to 1, 3L to 3, 4L to 8))
        assertEquals(inclinePress, result.first())
    }

    @Test
    fun `name is the final stable tie-break`() {
        val result = call().map { it.name }
        assertEquals(listOf("Bench Press", "Cable Fly", "Dips", "Incline Press"), result)
    }

    @Test
    fun `excluded ids and the rejected id are dropped`() {
        val result = call(excluded = setOf(benchPress.id), rejectedId = cableFly.id).map { it.id }
        assertEquals(listOf(dips.id, inclinePress.id), result)
    }

    @Test
    fun `last shown id sorts to the end of its tier`() {
        val result = call(lastShownId = benchPress.id).map { it.name }
        assertEquals(listOf("Cable Fly", "Dips", "Incline Press", "Bench Press"), result)
    }

    @Test
    fun `cardio and warmup categories are never eligible`() {
        val result = call(targets = setOf("CHEST", "QUADS"))
        assertTrue(result.none { it.category == ExerciseCategory.CARDIO.name })
        assertTrue(treadmill !in result)
    }

    @Test
    fun `only a primary mover overlap qualifies, not an incidental synergist`() {
        val synergistOnly = ex(6, "Triceps Pushdown", groups = listOf("TRICEPS", "CHEST"), weights = mapOf("CHEST" to 20))
        val result = call(exercises = listOf(benchPress, synergistOnly))
        assertEquals(listOf(benchPress), result)
    }

    @Test
    fun `strength lean drops calisthenics only when a strength candidate survives`() {
        assertTrue(dips !in call(lean = TrainingLean.STRENGTH))
        // With only calisthenics eligible, the lean filter falls back rather than returning nothing.
        val result = call(exercises = listOf(dips), lean = TrainingLean.STRENGTH)
        assertEquals(listOf(dips), result)
    }

    @Test
    fun `empty target groups yields no candidates`() {
        assertEquals(emptyList<Exercise>(), call(targets = emptySet()))
    }

    @Test
    fun `identical inputs produce an identical ordering`() {
        val a = call(usageCounts = mapOf(1L to 2, 2L to 2), routine = setOf(3L), logged = setOf(4L))
        val b = call(usageCounts = mapOf(1L to 2, 2L to 2), routine = setOf(3L), logged = setOf(4L))
        assertEquals(a, b)
    }

    @Test
    fun `exhausting all candidates returns empty list`() {
        val allIds = allExercises.map { it.id }.toSet()
        val result = call(excluded = allIds)
        assertTrue(result.isEmpty())
    }
}
