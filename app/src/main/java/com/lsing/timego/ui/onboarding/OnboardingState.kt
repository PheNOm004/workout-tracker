package com.lsing.timego.ui.onboarding

import com.lsing.timego.profile.TrainingProfile

enum class OnboardingStep {
    INTRO,
    GOAL,
    EXPERIENCE,
    MODALITY,
    EQUIPMENT,
    SCHEDULE,
    PHYSICAL,
    LIMITATIONS,
    REVIEW,
}

typealias OnboardingDraft = TrainingProfile

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.INTRO,
    val draft: OnboardingDraft = TrainingProfile(),
    val validationMessage: String? = null,
    val saving: Boolean = false,
    val completed: Boolean = false,
)

object OnboardingReducer {
    private val steps = OnboardingStep.entries

    fun next(state: OnboardingState): OnboardingState {
        val validationMessage = validationMessage(state)
        if (validationMessage != null) return state.copy(validationMessage = validationMessage)
        val index = steps.indexOf(state.step)
        return if (index == steps.lastIndex) state else state.copy(
            step = steps[index + 1],
            validationMessage = null,
        )
    }

    fun back(state: OnboardingState): OnboardingState {
        val index = steps.indexOf(state.step)
        return if (index == 0) state else state.copy(
            step = steps[index - 1],
            validationMessage = null,
        )
    }

    fun canComplete(state: OnboardingState): Boolean =
        state.step == OnboardingStep.REVIEW && state.draft.isComplete

    private fun validationMessage(state: OnboardingState): String? = when (state.step) {
        OnboardingStep.GOAL -> if (state.draft.goal == null) "Choose your primary goal." else null
        OnboardingStep.EXPERIENCE -> if (state.draft.experience == null) "Choose your experience level." else null
        OnboardingStep.MODALITY -> if (state.draft.modality == null) "Choose a training preference." else null
        OnboardingStep.EQUIPMENT -> if (state.draft.equipment.isEmpty()) "Choose at least one equipment option." else null
        OnboardingStep.SCHEDULE -> when {
            state.draft.trainingDaysPerWeek?.let { it in 1..7 } != true -> "Choose between one and seven training days."
            state.draft.sessionDuration == null -> "Choose a typical session duration."
            else -> null
        }
        OnboardingStep.PHYSICAL -> if (!state.draft.hasValidPhysicalValues) {
            "Check the height and weight values, or leave them blank."
        } else {
            null
        }
        OnboardingStep.REVIEW -> if (!state.draft.isComplete) "Complete the required choices before saving." else null
        OnboardingStep.INTRO,
        OnboardingStep.LIMITATIONS,
        -> null
    }
}
