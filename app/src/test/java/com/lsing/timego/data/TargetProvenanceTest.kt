package com.lsing.timego.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TargetProvenanceTest {
    @Test
    fun `historic and freeform set logs default to unknown provenance`() {
        val set = SetLog(
            sessionId = 1,
            exerciseId = 2,
            weightKg = 60.0,
            reps = 8,
            targetReps = 8,
            loggedAtEpochMillis = 10,
        )

        assertEquals(TargetProvenance.UNKNOWN.name, set.targetProvenance)
    }

    @Test
    fun `a target shown by an existing suggestion is recorded as pre-set evidence`() {
        assertEquals(
            TargetProvenance.OVERLOAD_SUGGESTION,
            targetProvenanceFor(suggestionWasShown = true),
        )
    }

    @Test
    fun `a freeform completed value remains unknown rather than a fabricated target outcome`() {
        assertEquals(
            TargetProvenance.UNKNOWN,
            targetProvenanceFor(suggestionWasShown = false),
        )
    }
}
