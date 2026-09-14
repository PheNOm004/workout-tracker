package com.lsing.timego.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lsing.timego.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfileRepository(application)
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = repository.profile.first()
            _state.update { current -> current.copy(draft = saved) }
        }
    }

    fun updateDraft(transform: (OnboardingDraft) -> OnboardingDraft) {
        _state.update { current -> current.copy(draft = transform(current.draft), validationMessage = null) }
    }

    fun next() = _state.update(OnboardingReducer::next)

    fun back() = _state.update(OnboardingReducer::back)

    fun skipOptional() {
        _state.update { current ->
            require(current.step == OnboardingStep.PHYSICAL || current.step == OnboardingStep.LIMITATIONS)
            OnboardingReducer.next(current)
        }
    }

    fun complete() {
        val current = _state.value
        if (!OnboardingReducer.canComplete(current)) {
            _state.update(OnboardingReducer::next)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            repository.save(
                _state.value.draft.copy(
                    onboardingVersion = ProfileRepository.CURRENT_ONBOARDING_VERSION,
                    invitationDismissed = false,
                ),
            )
            _state.update { it.copy(saving = false, completed = true) }
        }
    }

    fun dismiss() {
        viewModelScope.launch {
            repository.save(_state.value.draft.copy(invitationDismissed = true))
            _state.update { it.copy(completed = true) }
        }
    }

    fun reset() {
        viewModelScope.launch {
            repository.reset()
            _state.value = OnboardingState()
        }
    }
}
