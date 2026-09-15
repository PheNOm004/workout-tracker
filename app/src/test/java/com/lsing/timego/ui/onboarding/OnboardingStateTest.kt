package com.lsing.timego.ui.onboarding

import com.lsing.timego.data.TrainingLean
import com.lsing.timego.profile.Equipment
import com.lsing.timego.profile.ExperienceLevel
import com.lsing.timego.profile.SessionDurationRange
import com.lsing.timego.profile.TrainingGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStateTest {
    @Test
    fun `flow follows every page and back reverses one page`() {
        var state = OnboardingState()
        assertEquals(OnboardingStep.INTRO, state.step)

        state = OnboardingReducer.next(state)
        assertEquals(OnboardingStep.GOAL, state.step)
        state = state.copy(draft = state.draft.copy(goal = TrainingGoal.STRENGTH))
        state = OnboardingReducer.next(state)
        assertEquals(OnboardingStep.EXPERIENCE, state.step)

        state = OnboardingReducer.back(state)
        assertEquals(OnboardingStep.GOAL, state.step)
    }

    @Test
    fun `required page cannot advance without a choice`() {
        val goalPage = OnboardingState(step = OnboardingStep.GOAL)

        val result = OnboardingReducer.next(goalPage)

        assertEquals(OnboardingStep.GOAL, result.step)
        assertTrue(result.validationMessage!!.isNotBlank())
    }

    @Test
    fun `optional physical page advances when values are absent`() {
        val result = OnboardingReducer.next(OnboardingState(step = OnboardingStep.PHYSICAL))

        assertEquals(OnboardingStep.LIMITATIONS, result.step)
    }

    @Test
    fun `review is completable only with required profile fields`() {
        val incomplete = OnboardingState(step = OnboardingStep.REVIEW)
        assertFalse(OnboardingReducer.canComplete(incomplete))

        val complete = incomplete.copy(
            draft = incomplete.draft.copy(
                experience = ExperienceLevel.NOVICE,
                goal = TrainingGoal.STRENGTH,
                modality = TrainingLean.STRENGTH,
                equipment = setOf(Equipment.BARBELL),
                trainingDaysPerWeek = 3,
                sessionDuration = SessionDurationRange.MINUTES_45_TO_60,
            ),
        )
        assertTrue(OnboardingReducer.canComplete(complete))
    }
}
