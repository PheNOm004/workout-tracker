package com.lsing.timego.account

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountViewModelTest {
    @Test fun guestCanRegisterAsUnverifiedThenSignOut() = runBlocking {
        val repository = FakeAuthRepository()
        val viewModel = AccountViewModel(repository)
        viewModel.register("person@example.com", "long-password", "long-password")
        assertEquals(AuthState.SignedIn("person@example.com", false), viewModel.uiState.value.authState)
        viewModel.signOut()
        assertEquals(AuthState.Guest, viewModel.uiState.value.authState)
    }

    @Test fun validationRejectsMalformedInput() = runBlocking {
        val viewModel = AccountViewModel(FakeAuthRepository())
        viewModel.register("bad", "short", "different")
        assertTrue(viewModel.uiState.value.message!!.startsWith("Check"))
    }

    @Test fun credentialErrorsAreGeneric() = runBlocking {
        val viewModel = AccountViewModel(FakeAuthRepository())
        viewModel.signIn("person@example.com", "wrong-password")
        assertEquals("Sign-in details could not be accepted.", viewModel.uiState.value.message)
    }

    @Test fun resetDoesNotRevealWhetherEmailExists() = runBlocking {
        val viewModel = AccountViewModel(FakeAuthRepository())
        viewModel.resetPassword("unknown@example.com")
        assertTrue(viewModel.uiState.value.message!!.startsWith("If an account exists"))
    }

    @Test fun deletionRequiresRecentReauthentication() = runBlocking {
        val repository = FakeAuthRepository(AuthState.SignedIn("person@example.com", true))
        val viewModel = AccountViewModel(repository)
        viewModel.deleteAccount()
        assertTrue(viewModel.uiState.value.message!!.contains("confirm your password"))
        viewModel.reauthenticate("correct-password")
        viewModel.deleteAccount()
        assertEquals(AuthState.Guest, viewModel.uiState.value.authState)
        assertFalse(viewModel.uiState.value.busy)
    }

    @Test fun offlineFailureHasSafeMessage() = runBlocking {
        val repository = FakeAuthRepository().apply { nextFailure = AuthFailure.OFFLINE }
        val viewModel = AccountViewModel(repository)
        viewModel.signIn("person@example.com", "correct-password")
        assertTrue(viewModel.uiState.value.message!!.contains("offline"))
    }
}
