package com.lsing.timego.domain.programs

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.TrainingLean
import com.lsing.timego.domain.SetPerformance
import com.lsing.timego.domain.ai.MovementPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgramSlotResolverTest {
    private val benchPress = Exercise(id = 1, name = "Bench Press", muscleGroups = listOf("CHEST"), isCustom = false)
    private val pushUp = Exercise(
        id = 2, name = "Push-Up", muscleGroups = listOf("CHEST"), isCustom = false,
        category = ExerciseCategory.CALISTHENICS.name, catalogueKey = "timego.seed.v1.push-up",
    )
    private val exercises = listOf(benchPress, pushUp)

    @Test
    fun `default slot (no movementPattern) resolves via suggestedExerciseFor`() {
        val slot = ProgramSlot("Chest", setOf("CHEST"))
        val result = resolveSlot(slot, exercises, TrainingLean.STRENGTH, emptyMap(), emptyMap())
        assertEquals(benchPress, result)
    }

    @Test
    fun `slot with no matching exercise resolves to null`() {
        val slot = ProgramSlot("Traps", setOf("TRAPS"))
        val result = resolveSlot(slot, exercises, TrainingLean.BALANCED, emptyMap(), emptyMap())
        assertNull(result)
    }

    @Test
    fun `movementPattern slot with no logged history falls back to suggestedExerciseFor`() {
        val slot = ProgramSlot("Push skill", setOf("CHEST"), movementPattern = MovementPattern.HORIZONTAL_PUSH)
        val result = resolveSlot(slot, exercises, TrainingLean.CALISTHENICS, emptyMap(), emptyMap())
        assertEquals(pushUp, result)
    }

    @Test
    fun `movementPattern slot with mastered history falls back when the recommended exercise is not in the catalogue`() {
        val slot = ProgramSlot("Push skill", setOf("CHEST"), movementPattern = MovementPattern.HORIZONTAL_PUSH)
        // Push-Up's BiomechanicalRegistry profile: masteryRepCeiling = 20, nextProgressionKey = "Decline Push-Up"
        val masteredHistory = mapOf(2L to listOf(SetPerformance(weightKg = 0.0, reps = 20, targetReps = 15, rpe = 7)))
        val result = resolveSlot(slot, exercises, TrainingLean.CALISTHENICS, emptyMap(), masteredHistory)
        // "Decline Push-Up" isn't in the local exercises list, so this falls back to suggestedExerciseFor
        // rather than returning an exercise that doesn't exist in the catalogue passed in.
        assertEquals(pushUp, result)
    }
}
