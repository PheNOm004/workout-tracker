package com.lsing.timego.profile

import com.lsing.timego.data.TrainingLean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingProfileTest {
    @Test
    fun `profile is complete only when recommendation inputs are present`() {
        assertFalse(TrainingProfile().isComplete)
        assertTrue(completeProfile().isComplete)
    }

    @Test
    fun `sync safe copy removes private limitation note`() {
        val profile = completeProfile().copy(privateLimitationNote = "Old shoulder injury")

        val safe = profile.syncSafeCopy()

        assertNull(safe.privateLimitationNote)
        assertEquals(profile.movementLimitations, safe.movementLimitations)
    }

    @Test
    fun `imperial values normalize to metric`() {
        assertEquals(182.88, inchesToCentimetres(72.0), 0.001)
        assertEquals(81.6466, poundsToKilograms(180.0), 0.001)
    }

    @Test
    fun `physical values outside safe storage bounds are rejected`() {
        assertFalse(completeProfile().copy(heightCm = 50.0).hasValidPhysicalValues)
        assertFalse(completeProfile().copy(weightKg = 700.0).hasValidPhysicalValues)
        assertTrue(completeProfile().copy(heightCm = null, weightKg = null).hasValidPhysicalValues)
    }

    private fun completeProfile() = TrainingProfile(
        experience = ExperienceLevel.NOVICE,
        goal = TrainingGoal.GENERAL_FITNESS,
        modality = TrainingLean.BALANCED,
        equipment = setOf(Equipment.BODYWEIGHT, Equipment.DUMBBELL),
        trainingDaysPerWeek = 3,
        sessionDuration = SessionDurationRange.MINUTES_45_TO_60,
    )
}
