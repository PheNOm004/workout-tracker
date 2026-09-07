package com.lsing.timego.domain.ai

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.ExerciseCategory
import com.lsing.timego.data.MuscleGroup
import com.lsing.timego.domain.PlateauStatus
import com.lsing.timego.domain.RepRange
import com.lsing.timego.domain.SetPerformance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveAiEngineTest {

    @Test
    fun testAdaptiveOverloadSuggester_singleSessionLock() {
        val suggester = AdaptiveOverloadSuggester()
        val history = listOf(SetPerformance(weightKg = 60.0, reps = 10, targetReps = 10))
        val currentWorking = listOf(SetPerformance(weightKg = 62.5, reps = 10, targetReps = 10))

        val suggestion = suggester.suggestNext(history, currentWorking)
        assertNotNull(suggestion)
        assertEquals(62.5, suggestion!!.weightKg, 0.01)
        assertEquals(PlateauStatus.REPEATING, suggestion.plateauStatus)
    }

    @Test
    fun testAdaptiveOverloadSuggester_escalatesOnCeilingHit() {
        val suggester = AdaptiveOverloadSuggester()
        val history = listOf(
            SetPerformance(weightKg = 50.0, reps = 10, targetReps = 10),
            SetPerformance(weightKg = 50.0, reps = 12, targetReps = 10, rpe = 7)
        )
        val repRange = RepRange(floor = 8, ceiling = 12)

        val suggestion = suggester.suggestNext(history, emptyList(), weightIncrementKg = 2.5, repRange = repRange)
        assertNotNull(suggestion)
        assertEquals(52.5, suggestion!!.weightKg, 0.01)
        assertEquals(PlateauStatus.PROGRESSING, suggestion.plateauStatus)
    }

    @Test
    fun testAdaptiveOverloadSuggester_calisthenicsRepProgression() {
        val suggester = AdaptiveOverloadSuggester()
        val history = listOf(
            SetPerformance(weightKg = 75.0, reps = 15, targetReps = 15, rpe = 7)
        )
        val repRange = RepRange(floor = 10, ceiling = 15)

        // Null weightIncrementKg indicates bodyweight / calisthenics
        val suggestion = suggester.suggestNext(history, emptyList(), weightIncrementKg = null, repRange = repRange)
        assertNotNull(suggestion)
        assertEquals(75.0, suggestion!!.weightKg, 0.01)
        assertEquals(16, suggestion.reps) // Escalates reps rather than adding artificial plate weight
    }

    @Test
    fun testCategoryPreferenceLearner_adaptsToCalisthenics() {
        val learner = CategoryPreferenceLearner()
        // Log 5 calisthenics sessions in a row
        repeat(5) {
            learner.recordInteraction(ExerciseCategory.CALISTHENICS, reward = 1.0)
        }

        val probs = learner.getProbabilities()
        assertTrue("Calisthenics probability should dominate", probs[ExerciseCategory.CALISTHENICS]!! > probs[ExerciseCategory.STRENGTH]!!)
        assertEquals(ExerciseCategory.CALISTHENICS, learner.getDominantLean())
    }

    @Test
    fun testCategoryPreferenceLearner_adaptsToSwaps() {
        val learner = CategoryPreferenceLearner()
        learner.recordSwap(rejected = ExerciseCategory.STRENGTH, accepted = ExerciseCategory.CALISTHENICS)

        val calAffinity = learner.getAffinity(ExerciseCategory.CALISTHENICS)
        val strAffinity = learner.getAffinity(ExerciseCategory.STRENGTH)
        assertTrue("Calisthenics affinity should exceed Strength after swap", calAffinity > strAffinity)
    }

    @Test
    fun testProgressionRecommender_suggestsNextTierWhenMastered() {
        val learner = CategoryPreferenceLearner()
        val recommender = ProgressionRecommender(learner)

        val pushUp = Exercise(
            id = 1,
            name = "Push-Up",
            catalogueKey = seedKey("Push-Up"),
            muscleGroups = listOf(MuscleGroup.CHEST.name),
            isCustom = false,
            category = ExerciseCategory.CALISTHENICS.name
        )

        // User hits mastery ceiling (20 reps) across consecutive sessions
        val history = listOf(
            SetPerformance(weightKg = 0.0, reps = 18, targetReps = 20),
            SetPerformance(weightKg = 0.0, reps = 20, targetReps = 20, rpe = 7)
        )

        val upgrade = recommender.evaluateProgression(pushUp, history)
        assertNotNull(upgrade)
        assertEquals(seedKey("Decline Push-Up"), upgrade!!.recommendedCatalogueKey)
        assertTrue(upgrade.isUpgrade)
        assertEquals(MovementPattern.HORIZONTAL_PUSH, upgrade.pattern)
    }

    @Test
    fun testProgressionRecommender_noUpgradeIfBelowCeiling() {
        val learner = CategoryPreferenceLearner()
        val recommender = ProgressionRecommender(learner)

        val pushUp = Exercise(
            id = 1,
            name = "Push-Up",
            catalogueKey = seedKey("Push-Up"),
            muscleGroups = listOf(MuscleGroup.CHEST.name),
            isCustom = false,
            category = ExerciseCategory.CALISTHENICS.name
        )

        val history = listOf(
            SetPerformance(weightKg = 0.0, reps = 10, targetReps = 15),
            SetPerformance(weightKg = 0.0, reps = 12, targetReps = 15, rpe = 9)
        )

        val upgrade = recommender.evaluateProgression(pushUp, history)
        assertNull("Should not upgrade when below mastery ceiling", upgrade)
    }

    @Test
    fun testCustomExerciseClassifier_infersCorrectMovementPattern() {
        val customInvertedRow = CustomExerciseClassifier.classify(
            name = "My Bed Sheet Row",
            category = ExerciseCategory.CALISTHENICS.name,
            muscleWeights = mapOf(
                MuscleGroup.UPPER_BACK.name to 80,
                MuscleGroup.LATS.name to 50,
                MuscleGroup.BICEPS.name to 40
            )
        )
        assertEquals(MovementPattern.HORIZONTAL_PULL, customInvertedRow.pattern)
        assertEquals(KinematicChain.CKC, customInvertedRow.chain)

        val customSquat = CustomExerciseClassifier.classify(
            name = "Heavy Hack Squat",
            category = ExerciseCategory.STRENGTH.name,
            muscleWeights = mapOf(
                MuscleGroup.QUADS.name to 90,
                MuscleGroup.GLUTES.name to 50
            )
        )
        assertEquals(MovementPattern.KNEE_DOMINANT, customSquat.pattern)
        assertEquals(KinematicChain.OKC, customSquat.chain)
    }
}
