package com.lsing.timego.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthRepository(
    initialState: AuthState = AuthState.Guest,
    private val acceptedPassword: String = "correct-password",
) : AuthRepository {
    private val mutableState = MutableStateFlow(initialState)
    override val authState: StateFlow<AuthState> = mutableState.asStateFlow()
    var nextFailure: AuthFailure? = null
    private var reauthenticated = false

    override suspend fun register(email: String, password: String) = resultOr {
        mutableState.value = AuthState.SignedIn(email, verified = false)
        null
    }
    override suspend fun signIn(email: String, password: String) = resultOr {
        if (password != acceptedPassword) return@resultOr AuthFailure.CREDENTIALS_REJECTED
        mutableState.value = AuthState.SignedIn(email, verified = true)
        null
    }
    override suspend fun sendVerification() = resultOr { null }
    override suspend fun resetPassword(email: String) = resultOr { null }
    override suspend fun signOut() { mutableState.value = AuthState.Guest; reauthenticated = false }
    override suspend fun reauthenticate(password: String) = resultOr {
        if (password != acceptedPassword) AuthFailure.CREDENTIALS_REJECTED else null.also { reauthenticated = true }
    }
    override suspend fun deleteAccount() = resultOr {
        if (!reauthenticated) AuthFailure.REAUTHENTICATION_REQUIRED else null.also { mutableState.value = AuthState.Guest }
    }

    private inline fun resultOr(block: () -> AuthFailure?): AuthResult {
        nextFailure?.let { failure -> nextFailure = null; return AuthResult.Failure(failure) }
        return block()?.let(AuthResult::Failure) ?: AuthResult.Success
    }
}
