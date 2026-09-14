package com.lsing.timego.data.guidance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogueRepositoryTest {
    @Test
    fun bundledSnapshotIsCompleteAndVersioned() {
        assertEquals(820, BUNDLED_EXERCISE_GUIDANCE.size)
        assertEquals(820, BUNDLED_EXERCISE_GUIDANCE.map(ExerciseGuidance::catalogueKey).toSet().size)
        assertTrue(BUNDLED_EXERCISE_GUIDANCE.all { it.catalogueVersion == BUNDLED_CATALOGUE_VERSION })
        assertEquals(200, BUNDLED_EXERCISE_GUIDANCE.count(ExerciseGuidance::isReviewedComplete))
    }

    @Test
    fun reviewedAndMetadataOnlyEntriesRemainDistinct() {
        val reviewed = BUNDLED_EXERCISE_GUIDANCE.first(ExerciseGuidance::isReviewedComplete)
        val metadataOnly = BUNDLED_EXERCISE_GUIDANCE.first { !it.isReviewedComplete }

        assertTrue(reviewed.steps.isNotEmpty())
        assertTrue(reviewed.purpose.isNotBlank())
        assertTrue(metadataOnly.steps.isEmpty())
        assertTrue(metadataOnly.purpose.isEmpty())
    }

    @Test
    fun easierLinksResolveAndMissingKeysDoNot() {
        val byKey = BUNDLED_EXERCISE_GUIDANCE.associateBy(ExerciseGuidance::catalogueKey)
        val advanced = BUNDLED_EXERCISE_GUIDANCE.first { it.easierVariationKey != null }

        assertNotNull(byKey[advanced.easierVariationKey])
        assertNull(byKey["timego.seed.v1.not-real"])
    }
}
