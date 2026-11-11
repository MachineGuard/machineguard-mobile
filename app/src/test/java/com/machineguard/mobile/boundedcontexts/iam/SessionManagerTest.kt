package com.machineguard.mobile.boundedcontexts.iam

import com.machineguard.mobile.boundedcontexts.iam.application.SessionManager
import com.machineguard.mobile.boundedcontexts.iam.application.SessionState
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthFailure
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthRepository
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthResult
import com.machineguard.mobile.boundedcontexts.iam.domain.UserProfile
import com.machineguard.mobile.boundedcontexts.iam.infrastructure.FakeAuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    private fun manager() = SessionManager(FakeAuthRepository(latencyMillis = 0))

    @Test
    fun `starts checking the stored session`() {
        assertEquals(SessionState.Checking, manager().state.value)
    }

    @Test
    fun `restore without a stored session leaves the user signed out`() = runBlocking {
        val manager = manager()
        manager.restore()
        assertEquals(SessionState.SignedOut, manager.state.value)
    }

    @Test
    fun `login with valid credentials signs the user in`() = runBlocking {
        val manager = manager()
        val result = manager.login(FakeAuthRepository.DEMO_EMAIL, FakeAuthRepository.DEMO_PASSWORD)

        assertTrue(result is AuthResult.Success)
        assertEquals(SessionState.SignedIn(FakeAuthRepository.DEMO_PROFILE), manager.state.value)
    }

    @Test
    fun `login trims the email`() = runBlocking {
        val manager = manager()
        val result = manager.login("  ${FakeAuthRepository.DEMO_EMAIL}  ", FakeAuthRepository.DEMO_PASSWORD)

        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun `login with wrong credentials keeps the session closed`() = runBlocking {
        val manager = manager()
        manager.restore()
        val result = manager.login(FakeAuthRepository.DEMO_EMAIL, "wrong")

        assertEquals(AuthResult.Failure(AuthFailure.InvalidCredentials), result)
        assertEquals(SessionState.SignedOut, manager.state.value)
    }

    @Test
    fun `network failure is reported without signing in`() = runBlocking {
        val manager = manager()
        manager.restore()
        val result = manager.login(FakeAuthRepository.NETWORK_ERROR_EMAIL, "anything")

        assertEquals(AuthResult.Failure(AuthFailure.Network), result)
        assertEquals(SessionState.SignedOut, manager.state.value)
    }

    @Test
    fun `logout returns to signed out`() = runBlocking {
        val manager = manager()
        manager.login(FakeAuthRepository.DEMO_EMAIL, FakeAuthRepository.DEMO_PASSWORD)
        manager.logout()

        assertEquals(SessionState.SignedOut, manager.state.value)
    }

    @Test
    fun `logout closes the local session even if the server call fails`() = runBlocking {
        val failingLogout = object : AuthRepository {
            override suspend fun login(email: String, password: String): AuthResult =
                AuthResult.Success(FakeAuthRepository.DEMO_PROFILE)

            override suspend fun logout() {
                throw IllegalStateException("server down")
            }

            override suspend fun restoreSession(): UserProfile? = null
        }
        val manager = SessionManager(failingLogout)
        manager.login("a@b.co", "x")
        manager.logout()

        assertEquals(SessionState.SignedOut, manager.state.value)
    }

    @Test
    fun `restore does not override an active session`() = runBlocking {
        val manager = manager()
        manager.login(FakeAuthRepository.DEMO_EMAIL, FakeAuthRepository.DEMO_PASSWORD)
        manager.restore()

        assertTrue(manager.state.value is SessionState.SignedIn)
    }

    @Test
    fun `expired session sends the user back to login`() = runBlocking {
        val manager = manager()
        manager.login(FakeAuthRepository.DEMO_EMAIL, FakeAuthRepository.DEMO_PASSWORD)
        manager.onSessionExpired()

        assertEquals(SessionState.SignedOut, manager.state.value)
    }
}
