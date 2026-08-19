package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.SetLog
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseUsageFrequencyTest {
    private val benchPress = Exercise(id = 1, name = "Bench Press", muscleGroups = listOf("CHEST"), isCustom = false)
    private val squat = Exercise(id = 2, name = "Squat", muscleGroups = listOf("QUADS"), isCustom = false)
    private val warmupJog = Exercise(
        id = 3,
        name = "Jog",
        muscleGroups = listOf("FULL_BODY"),
        isCustom = false,
        category = ExerciseCategory.WARMUP.name,
    )
    private val cardioRide = Exercise(
        id = 4,
        name = "Ride",
        muscleGroups = listOf("FULL_BODY"),
        isCustom = false,
        category = ExerciseCategory.CARDIO.name,
    )
    private val exercisesById = mapOf(1L to benchPress, 2L to squat, 3L to warmupJog, 4L to cardioRide)

    private fun set(exerciseId: Long, isWarmup: Boolean = false) =
        SetLog(
            sessionId = 1,
            exerciseId = exerciseId,
            weightKg = 20.0,
            reps = 5,
            targetReps = 5,
            loggedAtEpochMillis = 0,
            isWarmup = isWarmup,
        )

    @Test
    fun `counts non-warmup working sets per exercise`() {
        val counts = exerciseUsageFrequency(listOf(set(1), set(1), set(2)), exercisesById)

        assertEquals(2, counts[1L])
        assertEquals(1, counts[2L])
    }

    @Test
    fun `excludes warmup-flagged sets`() {
        val counts = exerciseUsageFrequency(listOf(set(1, isWarmup = true), set(1)), exercisesById)

        assertEquals(1, counts[1L])
    }

    @Test
    fun `excludes WARMUP-category exercises even when not flagged as warmups`() {
        val counts = exerciseUsageFrequency(listOf(set(3)), exercisesById)

        assertEquals(null, counts[3L])
    }

    @Test
    fun `excludes CARDIO-category exercises even when not flagged as warmups`() {
        val counts = exerciseUsageFrequency(listOf(set(4)), exercisesById)

        assertEquals(null, counts[4L])
    }

    @Test
    fun `sorts higher usage first and breaks ties alphabetically`() {
        val ranked = exercisesRankedByFrequency(listOf(squat, benchPress), mapOf(2L to 5, 1L to 5))

        assertEquals(listOf(benchPress, squat), ranked)
    }

    @Test
    fun `puts never-used exercises last and orders them alphabetically`() {
        val ranked = exercisesRankedByFrequency(listOf(squat, benchPress), mapOf(2L to 3))

        assertEquals(listOf(squat, benchPress), ranked)
    }
}
