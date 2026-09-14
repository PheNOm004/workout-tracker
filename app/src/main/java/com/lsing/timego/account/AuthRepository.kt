package com.lsing.timego.account

import kotlinx.coroutines.flow.StateFlow

sealed interface AuthState {
    data object Guest : AuthState
    data class SignedIn(val email: String, val verified: Boolean) : AuthState
}

enum class AuthFailure { INVALID_INPUT, CREDENTIALS_REJECTED, OFFLINE, REAUTHENTICATION_REQUIRED, UNAVAILABLE }

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val reason: AuthFailure) : AuthResult
}

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun register(email: String, password: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun sendVerification(): AuthResult
    suspend fun resetPassword(email: String): AuthResult
    suspend fun signOut()
    suspend fun reauthenticate(password: String): AuthResult
    suspend fun deleteAccount(): AuthResult
}
