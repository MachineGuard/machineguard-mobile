package com.machineguard.mobile.core.network

import com.machineguard.mobile.core.data.AppError
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class ApiClientTest {
    private val server = MockWebServer()
    private lateinit var api: ApiClient

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().readTimeout(300, TimeUnit.MILLISECONDS)
            .connectTimeout(300, TimeUnit.MILLISECONDS).build()
        api = ApiClient(client, server.url("/").toString())
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun errorOf(result: ApiResult): AppError = (result as ApiResult.Failure).error

    @Test
    fun `a 2xx response returns its body`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"ok":true}"""))

        val result = api.get("/api/v1/alerts") as ApiResult.Success

        assertEquals("""{"ok":true}""", result.body)
        assertEquals(200, result.statusCode)
    }

    @Test
    fun `status codes become app errors`() = runBlocking {
        val expected = mapOf(
            400 to AppError.Invalid::class,
            401 to AppError.SessionExpired::class,
            403 to AppError.Forbidden::class,
            404 to AppError.NotFound::class,
            409 to AppError.Conflict::class,
            429 to AppError.TooManyRequests::class,
            500 to AppError.Server::class,
            503 to AppError.Server::class,
            418 to AppError.Unknown::class,
        )
        expected.forEach { (code, type) ->
            server.enqueue(MockResponse().setResponseCode(code))
            assertEquals("HTTP $code", type, errorOf(api.get("/x"))::class)
        }
    }

    @Test
    fun `a 408 is a timeout even after the automatic retry`() = runBlocking {
        // OkHttp reintenta un 408 una vez por su cuenta; si vuelve a fallar, llega aquí.
        server.enqueue(MockResponse().setResponseCode(408))
        server.enqueue(MockResponse().setResponseCode(408))

        assertEquals(AppError.Timeout, errorOf(api.get("/x")))
    }

    @Test
    fun `a 401 on login is reported as invalid credentials`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = api.post("/api/v1/auth/login", "{}", public = true, unauthorizedAs = AppError.InvalidCredentials)

        assertEquals(AppError.InvalidCredentials, errorOf(result))
    }

    @Test
    fun `server error text is kept as detail for problem detail and iam formats`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"type":"about:blank","detail":"La alerta ya fue reconocida"}"""))
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"status":403,"message":"Access Denied"}"""))
        server.enqueue(MockResponse().setResponseCode(409).setBody("<html>no es json</html>"))

        assertEquals("La alerta ya fue reconocida", errorOf(api.get("/a")).detail)
        assertEquals("Access Denied", errorOf(api.get("/b")).detail)
        assertNull(errorOf(api.get("/c")).detail)
    }

    @Test
    fun `retry after is exposed for throttled logins`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "45"))

        assertEquals(AppError.TooManyRequests(45), errorOf(api.get("/x")))
    }

    @Test
    fun `no connection is reported as offline`() = runBlocking {
        server.shutdown()

        assertEquals(AppError.Offline, errorOf(api.get("/x")))
    }

    @Test
    fun `a server that does not answer is reported as a timeout`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        assertEquals(AppError.Timeout, errorOf(api.get("/x")))
    }

    @Test
    fun `null query parameters are omitted and values are encoded`() = runBlocking {
        server.enqueue(MockResponse().setBody("[]"))

        api.get("/api/v1/alerts", mapOf("monitoringZoneId" to null, "status" to "PENDING"))

        assertEquals("/api/v1/alerts?status=PENDING", server.takeRequest().path)
    }

    @Test
    fun `post sends the json body and accepts json`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("{}"))

        api.post("/api/v1/incidents/1/resolve", """{"notes":"ok"}""")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("""{"notes":"ok"}""", request.body.readUtf8())
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        assertEquals("application/json", request.getHeader("Accept"))
    }
}
