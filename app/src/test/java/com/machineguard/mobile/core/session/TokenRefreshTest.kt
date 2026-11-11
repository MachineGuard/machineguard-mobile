package com.machineguard.mobile.core.session

import com.machineguard.mobile.boundedcontexts.iam.domain.SessionState
import com.machineguard.mobile.core.DataLayerEnv
import com.machineguard.mobile.core.data.AppError
import com.machineguard.mobile.core.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.atomic.AtomicInteger

class TokenRefreshTest {
    private val server = MockWebServer()
    private val refreshCalls = AtomicInteger()
    private lateinit var env: DataLayerEnv

    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun setUp() {
        server.start()
        env = DataLayerEnv(server, folder.newFolder("cache"))
        env.signIn(access = "A1", refresh = "R1")
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private val refreshed = """{"accessToken":"A2","refreshToken":"R2","expiresIn":900,
        "user":{"id":"u-1","organizationId":"o-1","email":"ana@example.com","fullName":"Ana Torres","role":"ADMIN"}}"""

    /** El API simulado: solo acepta el access token vigente; /auth/refresh responde con [onRefresh]. */
    private fun serve(validToken: String, onRefresh: () -> MockResponse) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == "/api/v1/auth/refresh" -> {
                    refreshCalls.incrementAndGet()
                    Thread.sleep(80)
                    onRefresh()
                }
                request.getHeader("Authorization") == "Bearer $validToken" -> MockResponse().setBody("[]")
                else -> MockResponse().setResponseCode(401)
            }
        }
    }

    @Test
    fun `an expired access token is renewed and the request is retried once`() = runBlocking {
        serve("A2") { MockResponse().setBody(refreshed) }

        val result = env.api.get("/api/v1/alerts")

        assertTrue(result is ApiResult.Success)
        assertEquals(1, refreshCalls.get())
        assertEquals("A2", env.storage.tokens?.accessToken)
        assertEquals("R2", env.storage.tokens?.refreshToken)
        assertTrue(env.sessions.state.value is SessionState.SignedIn)
    }

    @Test
    fun `the new token is sent in later requests without renewing again`() = runBlocking {
        serve("A2") { MockResponse().setBody(refreshed) }

        env.api.get("/api/v1/alerts")
        env.api.get("/api/v1/alerts")

        assertEquals(1, refreshCalls.get())
    }

    @Test
    fun `concurrent expired requests renew the session only once`() = runBlocking {
        serve("A2") { MockResponse().setBody(refreshed) }

        val results = (1..5).map { async(Dispatchers.IO) { env.api.get("/api/v1/alerts") } }.awaitAll()

        assertTrue(results.all { it is ApiResult.Success })
        // El servidor rota el refresh token: renovar dos veces con R1 revocaría la sesión completa.
        assertEquals(1, refreshCalls.get())
    }

    @Test
    fun `when the refresh token is rejected the session ends and the data is cleared`() = runBlocking {
        serve("never") { MockResponse().setResponseCode(401) }

        val result = env.api.get("/api/v1/alerts")

        assertEquals(AppError.SessionExpired, (result as ApiResult.Failure).error)
        assertEquals(SessionState.SignedOut(expired = true), env.sessions.state.value)
        assertNull(env.storage.tokens)
        assertNull(env.storage.profileJson)
        assertEquals(1, env.cleared.get())
    }

    @Test
    fun `a server failure while renewing is not treated as an expired session`() = runBlocking {
        serve("A2") { MockResponse().setResponseCode(500) }

        val result = env.api.get("/api/v1/alerts")

        assertEquals(AppError.Offline, (result as ApiResult.Failure).error)
        assertTrue(env.sessions.state.value is SessionState.SignedIn)
        assertEquals("R1", env.storage.tokens?.refreshToken)
        assertEquals(0, env.cleared.get())
    }

    @Test
    fun `a throttled refresh keeps the session`() = runBlocking {
        serve("A2") { MockResponse().setResponseCode(429) }

        env.api.get("/api/v1/alerts")

        assertTrue(env.sessions.state.value is SessionState.SignedIn)
    }

    @Test
    fun `requests marked as public never carry the token`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = env.api.post("/api/v1/auth/login", "{}", public = true, unauthorizedAs = AppError.InvalidCredentials)

        assertEquals(AppError.InvalidCredentials, (result as ApiResult.Failure).error)
        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals(1, server.requestCount)
        assertTrue(env.sessions.state.value is SessionState.SignedIn)
    }

    @Test
    fun `without a session the request goes out without a token`() = runBlocking {
        env.sessions.signOut()
        server.enqueue(MockResponse().setBody("[]"))

        env.api.get("/api/v1/alerts")

        assertNull(server.takeRequest().getHeader("Authorization"))
    }
}
