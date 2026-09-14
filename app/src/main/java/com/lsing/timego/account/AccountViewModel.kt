package com.lsing.timego.account

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccountUiState(
    val authState: AuthState = AuthState.Guest,
    val message: String? = null,
    val busy: Boolean = false,
)

class AccountViewModel(private val repository: AuthRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AccountUiState(authState = repository.authState.value))
    val uiState: StateFlow<AccountUiState> = mutableUiState.asStateFlow()

    suspend fun register(email: String, password: String, confirmPassword: String) {
        if (!validEmail(email) || password.length < 8 || password != confirmPassword) return fail(AuthFailure.INVALID_INPUT)
        perform { repository.register(email.trim(), password) }
    }

    suspend fun signIn(email: String, password: String) {
        if (!validEmail(email) || password.isBlank()) return fail(AuthFailure.INVALID_INPUT)
        perform { repository.signIn(email.trim(), password) }
    }

    suspend fun resetPassword(email: String) {
        if (!validEmail(email)) return fail(AuthFailure.INVALID_INPUT)
        perform(successMessage = "If an account exists for that email, reset instructions have been requested.") { repository.resetPassword(email.trim()) }
    }

    suspend fun sendVerification() = perform(successMessage = "Verification email requested.") { repository.sendVerification() }
    suspend fun signOut() { repository.signOut(); mutableUiState.value = AccountUiState(repository.authState.value) }
    suspend fun reauthenticate(password: String) = perform { repository.reauthenticate(password) }
    suspend fun deleteAccount() = perform(successMessage = "Account deleted.") { repository.deleteAccount() }

    private suspend fun perform(successMessage: String? = null, action: suspend () -> AuthResult) {
        mutableUiState.value = mutableUiState.value.copy(busy = true, message = null)
        when (val result = action()) {
            AuthResult.Success -> mutableUiState.value = AccountUiState(repository.authState.value, successMessage)
            is AuthResult.Failure -> fail(result.reason)
        }
    }

    private fun fail(failure: AuthFailure) {
        val message = when (failure) {
            AuthFailure.INVALID_INPUT -> "Check the email and password fields and try again."
            AuthFailure.CREDENTIALS_REJECTED -> "Sign-in details could not be accepted."
            AuthFailure.OFFLINE -> "You appear to be offline. Try again when connected."
            AuthFailure.REAUTHENTICATION_REQUIRED -> "Please confirm your password before deleting the account."
            AuthFailure.UNAVAILABLE -> "Accounts are not available in this build."
        }
        mutableUiState.value = mutableUiState.value.copy(busy = false, message = message)
    }

    private fun validEmail(value: String): Boolean = value.trim().matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
}
