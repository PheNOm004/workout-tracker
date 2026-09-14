package com.lsing.timego.account

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.android.gms.tasks.Task
import android.content.Context
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {
    private val mutableState = MutableStateFlow(auth.currentUser.toState())
    override val authState: StateFlow<AuthState> = mutableState.asStateFlow()
    private val listener = FirebaseAuth.AuthStateListener { updated -> mutableState.value = updated.currentUser.toState() }

    init { auth.addAuthStateListener(listener) }

    override suspend fun register(email: String, password: String): AuthResult = mapTask(auth.createUserWithEmailAndPassword(email, password))
    override suspend fun signIn(email: String, password: String): AuthResult = mapTask(auth.signInWithEmailAndPassword(email, password))
    override suspend fun sendVerification(): AuthResult = auth.currentUser?.let { mapTask(it.sendEmailVerification()) } ?: AuthResult.Failure(AuthFailure.CREDENTIALS_REJECTED)
    override suspend fun resetPassword(email: String): AuthResult = mapTask(auth.sendPasswordResetEmail(email), hideCredentialFailure = true)
    override suspend fun signOut() { auth.signOut() }
    override suspend fun reauthenticate(password: String): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Failure(AuthFailure.CREDENTIALS_REJECTED)
        val email = user.email ?: return AuthResult.Failure(AuthFailure.CREDENTIALS_REJECTED)
        return mapTask(user.reauthenticate(EmailAuthProvider.getCredential(email, password)))
    }
    override suspend fun deleteAccount(): AuthResult = auth.currentUser?.let { mapTask(it.delete()) } ?: AuthResult.Success

    private suspend fun mapTask(task: Task<*>, hideCredentialFailure: Boolean = false): AuthResult {
        val error = suspendCancellableCoroutine<Exception?> { continuation ->
            task.addOnCompleteListener { continuation.resume(it.exception) }
        }
        return when (error) {
            null -> AuthResult.Success
            is FirebaseNetworkException -> AuthResult.Failure(AuthFailure.OFFLINE)
            is FirebaseAuthRecentLoginRequiredException -> AuthResult.Failure(AuthFailure.REAUTHENTICATION_REQUIRED)
            is FirebaseAuthInvalidCredentialsException -> if (hideCredentialFailure) AuthResult.Success else AuthResult.Failure(AuthFailure.CREDENTIALS_REJECTED)
            else -> AuthResult.Failure(AuthFailure.CREDENTIALS_REJECTED)
        }
    }

    companion object {
        fun createIfConfigured(context: Context): AuthRepository =
            if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseAuthRepository(FirebaseAuth.getInstance())
            else UnavailableAuthRepository()
    }
}

private fun com.google.firebase.auth.FirebaseUser?.toState(): AuthState =
    this?.email?.let { AuthState.SignedIn(it, isEmailVerified) } ?: AuthState.Guest

class UnavailableAuthRepository : AuthRepository {
    private val state = MutableStateFlow<AuthState>(AuthState.Guest)
    override val authState: StateFlow<AuthState> = state.asStateFlow()
    private fun unavailable() = AuthResult.Failure(AuthFailure.UNAVAILABLE)
    override suspend fun register(email: String, password: String) = unavailable()
    override suspend fun signIn(email: String, password: String) = unavailable()
    override suspend fun sendVerification() = unavailable()
    override suspend fun resetPassword(email: String) = unavailable()
    override suspend fun signOut() = Unit
    override suspend fun reauthenticate(password: String) = unavailable()
    override suspend fun deleteAccount() = unavailable()
}
