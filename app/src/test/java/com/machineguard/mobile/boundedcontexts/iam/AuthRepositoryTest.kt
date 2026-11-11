package com.machineguard.mobile.boundedcontexts.iam

import com.machineguard.mobile.boundedcontexts.iam.domain.SessionState
import com.machineguard.mobile.boundedcontexts.iam.domain.UserRole
import com.machineguard.mobile.boundedcontexts.iam.infrastructure.AuthRepositoryImpl
import com.machineguard.mobile.core.DataLayerEnv
import com.machineguard.mobile.core.FIXED_CLOCK
import com.machineguard.mobile.core.FIXED_NOW
import com.machineguard.mobile.core.data.AppError
import com.machineguard.mobile.core.data.DataResult
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AuthRepositoryTest {
    private val server = MockWebServer()
    private lateinit var env: DataLayerEnv
    private lateinit var auth: AuthRepositoryImpl

    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun setUp() {
        server.start()
        env = DataLayerEnv(server, folder.newFolder("cache"))
        auth = AuthRepositoryImpl(env.api, env.sessions, FIXED_CLOCK)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private val loginJson = """
        {"tokenType":"Bearer","accessToken":"A1","expiresIn":900,"refreshToken":"R1",
         "user":{"id":"u-1","organizationId":"o-1","email":"ana@example.com","fullName":"Ana Torres","role":"VIEWER",
                 "status":"ACTIVE","lastLoginAt":"2026-11-09T20:00:00Z"},
         "organization":{"id":"o-1","name":"Cadena de Frío SAC","subscriptionPlan":"PILOT"}}
    """.trimIndent()

    @Test
    fun `the session starts restoring until it is read from storage`() {
        assertEquals(SessionState.Restoring, auth.session.value)
    }

    @Test
    fun `restoring without saved data leaves the user signed out`() {
        env.sessions.restore()

        assertEquals(SessionState.SignedOut(expired = false), auth.session.value)
    }

    @Test
    fun `a saved session is restored without a network call`() {
        env.signIn()
        val restored = com.machineguard.mobile.core.session.SessionManager(env.storage)

        restored.restore()

        assertTrue(restored.state.value is SessionState.SignedIn)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `login stores the tokens securely and signs in`() = runBlocking {
        server.enqueue(MockResponse().setBody(loginJson))

        val result = auth.login("  ana@example.com ", "secreta-123") as DataResult.Success

        assertEquals("Ana Torres", result.data.fullName)
        assertEquals(UserRole.VIEWER, result.data.role)
        assertFalse(result.data.canManageIncidents)
        assertEquals("Cadena de Frío SAC", result.data.organizationName)
        assertEquals("A1", env.storage.tokens?.accessToken)
        assertEquals("R1", env.storage.tokens?.refreshToken)
        assertEquals(FIXED_NOW.plusSeconds(900), env.storage.tokens?.accessExpiresAt)
        assertEquals(SessionState.SignedIn(result.data), auth.session.value)

        val request = server.takeRequest()
        assertEquals("/api/v1/auth/login", request.path)
        assertEquals("""{"email":"ana@example.com","password":"secreta-123"}""", request.body.readUtf8())
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun `wrong credentials are reported without opening a session`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"status":401,"message":"Invalid credentials"}"""))

        val result = auth.login("ana@example.com", "mala")

        assertEquals(AppError.InvalidCredentials, (result as DataResult.Failure).error)
        assertNull(env.storage.tokens)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `too many attempts expose the wait time`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "30"))

        assertEquals(AppError.TooManyRequests(30), (auth.login("a@b.c", "x") as DataResult.Failure).error)
    }

    @Test
    fun `login without connection is offline`() = runBlocking {
        server.shutdown()

        assertEquals(AppError.Offline, (auth.login("a@b.c", "x") as DataResult.Failure).error)
    }

    @Test
    fun `logout revokes the refresh token and clears the local session and cache`() = runBlocking {
        env.signIn()
        env.cache.write("zones", "[]", com.machineguard.mobile.core.FIXED_OFFSET_NOW)
        server.enqueue(MockResponse().setResponseCode(204))

        val result = auth.logout()

        assertTrue(result is DataResult.Success)
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/logout", request.path)
        assertEquals("""{"refreshToken":"R1"}""", request.body.readUtf8())
        assertEquals("Bearer A1", request.getHeader("Authorization"))
        assertEquals(SessionState.SignedOut(expired = false), auth.session.value)
        assertNull(env.storage.tokens)
        assertNull(env.cache.read("zones"))
    }

    @Test
    fun `logout always clears the local session even when the server cannot be reached`() = runBlocking {
        env.signIn()
        server.shutdown()

        val result = auth.logout()

        assertEquals(AppError.Offline, (result as DataResult.Failure).error)
        assertEquals(SessionState.SignedOut(expired = false), auth.session.value)
        assertNull(env.storage.tokens)
        assertNull(env.storage.profileJson)
    }

    @Test
    fun `refreshing the profile updates the signed in user`() = runBlocking {
        env.signIn()
        server.enqueue(
            MockResponse().setBody(
                """{"id":"u-1","organizationId":"o-1","email":"ana@example.com","fullName":"Ana T. Rojas","role":"ADMIN"}""",
            ),
        )

        val result = auth.refreshProfile() as DataResult.Success

        assertEquals("/api/v1/users/me", server.takeRequest().path)
        assertEquals("Ana T. Rojas", result.data.fullName)
        // El nombre de la organización no viene en /users/me: se conserva el que ya se tenía.
        assertEquals("Cadena de Frío SAC", result.data.organizationName)
        assertEquals("Ana T. Rojas", (auth.session.value as SessionState.SignedIn).user.fullName)
        assertNotNull(env.storage.profileJson)
    }

    @Test
    fun `an unknown role is treated as read only`() = runBlocking {
        server.enqueue(MockResponse().setBody(loginJson.replace("VIEWER", "AUDITOR")))

        val profile = (auth.login("ana@example.com", "x") as DataResult.Success).data

        assertEquals(UserRole.UNKNOWN, profile.role)
        assertFalse(profile.canManageIncidents)
    }
}
