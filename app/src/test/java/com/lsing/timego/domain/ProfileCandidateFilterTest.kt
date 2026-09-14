package com.lsing.timego.domain

import com.lsing.timego.data.Exercise
import com.lsing.timego.data.TrainingLean
import com.lsing.timego.profile.Equipment
import com.lsing.timego.profile.ExperienceLevel
import com.lsing.timego.profile.MovementLimitation
import com.lsing.timego.profile.SessionDurationRange
import com.lsing.timego.profile.TrainingGoal
import com.lsing.timego.profile.TrainingProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileCandidateFilterTest {
    private val bench = Exercise(1, "Bench", "bench", listOf("CHEST"), false)
    private val pushUp = Exercise(2, "Push-Up", "push_up", listOf("CHEST"), false)

    @Test
    fun `missing profile preserves every candidate`() {
        val result = filterCandidates(listOf(bench, pushUp), TrainingProfile(), emptyMap())

        assertEquals(listOf(bench, pushUp), result.candidates)
    }

    @Test
    fun `unavailable equipment removes documented candidate`() {
        val profile = completeProfile(equipment = setOf(Equipment.BODYWEIGHT))
        val metadata = mapOf(
            "bench" to CandidateMetadata(equipment = setOf(Equipment.BARBELL, Equipment.BENCH)),
            "push_up" to CandidateMetadata(equipment = setOf(Equipment.BODYWEIGHT)),
        )

        val result = filterCandidates(listOf(bench, pushUp), profile, metadata)

        assertEquals(listOf(pushUp), result.candidates)
        assertTrue(result.exclusionReasons.getValue(bench.id).contains("equipment"))
    }

    @Test
    fun `structured limitation removes only documented stress region`() {
        val profile = completeProfile().copy(movementLimitations = setOf(MovementLimitation.SHOULDERS))
        val metadata = mapOf(
            "bench" to CandidateMetadata(stressRegions = setOf(MovementLimitation.SHOULDERS)),
            "push_up" to CandidateMetadata(),
        )

        assertEquals(listOf(pushUp), filterCandidates(listOf(bench, pushUp), profile, metadata).candidates)
    }

    @Test
    fun `all excluded returns an explicit abstention`() {
        val profile = completeProfile(equipment = setOf(Equipment.BODYWEIGHT))
        val metadata = mapOf("bench" to CandidateMetadata(equipment = setOf(Equipment.BARBELL)))

        val result = filterCandidates(listOf(bench), profile, metadata)

        assertTrue(result.candidates.isEmpty())
        assertEquals("No exercises match your equipment and movement preferences.", result.abstentionReason)
    }

    private fun completeProfile(equipment: Set<Equipment> = setOf(Equipment.BODYWEIGHT, Equipment.BARBELL, Equipment.BENCH)) =
        TrainingProfile(
            experience = ExperienceLevel.NOVICE,
            goal = TrainingGoal.GENERAL_FITNESS,
            modality = TrainingLean.BALANCED,
            equipment = equipment,
            trainingDaysPerWeek = 3,
            sessionDuration = SessionDurationRange.MINUTES_45_TO_60,
        )
}
