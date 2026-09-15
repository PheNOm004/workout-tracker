package com.lsing.timego.ui.exercise

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.guidance.BUNDLED_EXERCISE_GUIDANCE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseDetailModelTest {
    @Test fun reviewedEntryShowsInstructionsAndPersonalValues() {
        val guide = BUNDLED_EXERCISE_GUIDANCE.first { it.isReviewedComplete }
        val model = buildExerciseDetail(exercise(guide.catalogueKey, guide.name), guide, "60 kg × 8", "70 kg × 5")
        assertTrue(model.isReviewed)
        assertTrue(model.steps.isNotEmpty())
        assertNotNull(model.personalSummary)
    }

    @Test fun metadataOnlyEntryOffersEasierMovementWithoutInventingSteps() {
        val guide = BUNDLED_EXERCISE_GUIDANCE.first { !it.isReviewedComplete && it.easierVariationKey != null }
        val model = buildExerciseDetail(exercise(guide.catalogueKey, guide.name), guide)
        assertFalse(model.isReviewed)
        assertTrue(model.steps.isEmpty())
        assertNotNull(model.easierVariationKey)
    }

    @Test fun customExerciseHasHonestFallback() {
        val model = buildExerciseDetail(exercise(null, "My move", custom = true), null)
        assertEquals("Custom exercise", model.subtitle)
        assertFalse(model.isReviewed)
    }

    private fun exercise(key: String?, name: String, custom: Boolean = false) = Exercise(name = name, catalogueKey = key, muscleGroups = listOf("FULL_BODY"), isCustom = custom)
}
