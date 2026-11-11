package com.machineguard.mobile.boundedcontexts.alertincidentmanagement

import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertQuery
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertSeverity
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertStatus
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentQuery
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentStatus
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.infrastructure.AlertsRepositoryImpl
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.infrastructure.IncidentsRepositoryImpl
import com.machineguard.mobile.core.DataLayerEnv
import com.machineguard.mobile.core.data.AppError
import com.machineguard.mobile.core.data.DataResult
import com.machineguard.mobile.core.data.DataSource
import com.machineguard.mobile.shared.domain.EnvironmentalVariable
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.infrastructure.parseAlert
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.OffsetDateTime

class AlertIncidentRepositoriesTest {
    private val server = MockWebServer()
    private lateinit var alerts: AlertsRepositoryImpl
    private lateinit var incidents: IncidentsRepositoryImpl

    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun setUp() {
        server.start()
        val env = DataLayerEnv(server, folder.newFolder("cache"))
        env.signIn()
        alerts = AlertsRepositoryImpl(env.remote)
        incidents = IncidentsRepositoryImpl(env.remote)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private val alertsJson = """
        [{"id":"a-1","incidentId":"i-1","monitoringZoneId":"z-1","severity":"HIGH","status":"PENDING",
          "message":"TEMPERATURE fuera de rango","raisedAt":"2026-11-10T11:00:00Z","acknowledgedAt":null,
          "escalatedAt":null,"historical":false},
         {"id":"a-2","incidentId":null,"monitoringZoneId":"z-2","severity":"EXTREME","status":"SNOOZED",
          "message":"x","raisedAt":"no es una fecha"}]
    """.trimIndent()

    private val detailJson = """
        {"incident":{"id":"i-1","originMeasurementId":"m-1","monitoringZoneId":"z-1","environmentalVariable":"TEMPERATURE",
           "status":"ACKNOWLEDGED","openedAt":"2026-11-10T11:00:00Z","resolvedAt":null,"resolvedBy":null,
           "resolutionNotes":null,"historical":false},
         "alert":{"id":"a-1","incidentId":"i-1","monitoringZoneId":"z-1","severity":"HIGH","status":"ACKNOWLEDGED",
           "message":"m","raisedAt":"2026-11-10T11:00:00Z","acknowledgedAt":"2026-11-10T11:05:00Z"},
         "acknowledgement":{"acknowledgedBy":"u-1","acknowledgedAt":"2026-11-10T11:05:00Z","note":"En camino"},
         "correctiveActions":[{"id":"c-1","incidentId":"i-1","description":"Se reemplazó el compresor",
           "performedBy":"u-1","performedAt":"2026-11-10T11:30:00Z","notes":""}],
         "withinSafeRange":false,"resolutionAllowed":false,
         "resolutionBlockReasons":["OUT_OF_RANGE"]}
    """.trimIndent()

    @Test
    fun `alerts are mapped and unknown values do not break the list`() = runBlocking {
        server.enqueue(MockResponse().setBody(alertsJson))

        val list = (alerts.getAlerts() as DataResult.Success).data

        assertEquals(2, list.size)
        assertEquals(AlertSeverity.HIGH, list[0].severity)
        assertEquals(AlertStatus.PENDING, list[0].status)
        assertEquals(OffsetDateTime.parse("2026-11-10T11:00:00Z"), list[0].raisedAt)
        assertNull(list[0].acknowledgedAt)
        assertEquals(AlertSeverity.UNKNOWN, list[1].severity)
        assertEquals(AlertStatus.UNKNOWN, list[1].status)
        assertNull(list[1].raisedAt)
        assertNull(list[1].incidentId)
    }

    @Test
    fun `alert reading is parsed when the Core sends it and is null otherwise`() {
        val withReading = parseAlert(
            """{"id":"a1","monitoringZoneId":"z1","severity":"HIGH","status":"PENDING","message":"m",
               "environmentalVariable":"HUMIDITY","measuredValue":67.50,"minimumValue":35.00,"maximumValue":60.00}""",
        )
        val reading = withReading.reading!!
        assertEquals(com.machineguard.mobile.shared.domain.EnvironmentalVariable.HUMIDITY, reading.variable)
        assertEquals(0, java.math.BigDecimal("67.5").compareTo(reading.value))
        assertEquals(0, java.math.BigDecimal("60").compareTo(reading.maximum))
        assertEquals(null, parseAlert("""{"id":"a2","message":"m"}""").reading)
    }

    @Test
    fun `alert filters are sent as query parameters`() = runBlocking {
        server.enqueue(MockResponse().setBody("[]"))

        alerts.getAlerts(AlertQuery(monitoringZoneId = "z-1", status = AlertStatus.PENDING))

        assertEquals("/api/v1/alerts?monitoringZoneId=z-1&status=PENDING", server.takeRequest().path)
    }

    @Test
    fun `alerts fall back to the saved list without connection`() = runBlocking {
        server.enqueue(MockResponse().setBody(alertsJson))
        alerts.getAlerts()
        server.shutdown()

        val result = alerts.getAlerts() as DataResult.Success

        assertEquals(DataSource.CACHE, result.source)
        assertEquals(2, result.data.size)
    }

    @Test
    fun `an alert from another organization is reported as not found`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))

        assertTrue((alerts.getAlert("a-9") as DataResult.Failure).error is AppError.NotFound)
    }

    @Test
    fun `incident detail includes the alert, acknowledgement, actions and resolution rules`() = runBlocking {
        server.enqueue(MockResponse().setBody(detailJson))

        val detail = (incidents.getIncident("i-1") as DataResult.Success).data

        assertEquals(IncidentStatus.ACKNOWLEDGED, detail.incident.status)
        assertEquals(EnvironmentalVariable.TEMPERATURE, detail.incident.environmentalVariable)
        assertEquals("En camino", detail.acknowledgement?.note)
        assertEquals("u-1", detail.acknowledgement?.acknowledgedBy)
        assertEquals("Se reemplazó el compresor", detail.correctiveActions.single().description)
        assertNull(detail.correctiveActions.single().notes)
        assertFalse(detail.resolutionAllowed)
        assertEquals(listOf("OUT_OF_RANGE"), detail.resolutionBlockReasons)
        assertTrue(detail.incident.status.isActive)
    }

    @Test
    fun `incident filters use the api parameter names`() = runBlocking {
        server.enqueue(MockResponse().setBody("[]"))

        incidents.getIncidents(
            IncidentQuery(
                monitoringZoneId = "z-1",
                status = IncidentStatus.OPEN,
                from = OffsetDateTime.parse("2026-11-01T00:00:00Z"),
                limit = 20,
            ),
        )

        assertEquals(
            "/api/v1/incidents?monitoringZoneId=z-1&status=OPEN&from=2026-11-01T00%3A00Z&limit=20",
            server.takeRequest().path,
        )
    }

    @Test
    fun `acknowledging posts the note and never sends who does it`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"alertId":"a-1","incidentId":"i-1","acknowledgedBy":"u-1","acknowledgedAt":"2026-11-10T11:05:00Z",
                   "note":"En camino","alertStatus":"ACKNOWLEDGED","incidentStatus":"ACKNOWLEDGED"}""",
            ),
        )

        val result = (incidents.acknowledgeAlert("a-1", "  En camino ") as DataResult.Success).data

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/alerts/a-1/acknowledgements", request.path)
        assertEquals("""{"note":"En camino"}""", request.body.readUtf8())
        assertEquals(AlertStatus.ACKNOWLEDGED, result.alertStatus)
        assertEquals(IncidentStatus.ACKNOWLEDGED, result.incidentStatus)
    }

    @Test
    fun `acknowledging without a note sends an empty body`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"alertId":"a-1","incidentId":"i-1","acknowledgedBy":"u-1"}"""))

        incidents.acknowledgeAlert("a-1")

        assertEquals("{}", server.takeRequest().body.readUtf8())
    }

    @Test
    fun `a corrective action sends its description and optional notes`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"id":"c-1","incidentId":"i-1","description":"Se limpió el filtro","performedBy":"u-1",
                   "performedAt":"2026-11-10T11:30:00Z","notes":"Revisar mañana"}""",
            ),
        )

        val action = (incidents.registerCorrectiveAction("i-1", " Se limpió el filtro ", "Revisar mañana") as DataResult.Success).data

        val request = server.takeRequest()
        assertEquals("/api/v1/incidents/i-1/corrective-actions", request.path)
        assertEquals("""{"description":"Se limpió el filtro","notes":"Revisar mañana"}""", request.body.readUtf8())
        assertEquals("c-1", action.id)
    }

    @Test
    fun `a business rule rejection keeps the server explanation`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(409)
                .setBody("""{"title":"Conflict","status":409,"detail":"El incidente no puede resolverse sin una accion correctiva"}"""),
        )

        val error = (incidents.resolveIncident("i-1") as DataResult.Failure).error

        assertTrue(error is AppError.Conflict)
        assertEquals("El incidente no puede resolverse sin una accion correctiva", error.detail)
    }

    @Test
    fun `a viewer cannot write`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403))

        assertTrue((incidents.acknowledgeAlert("a-1") as DataResult.Failure).error is AppError.Forbidden)
    }

    @Test
    fun `writes are never served from cache and are not retried when offline`() = runBlocking {
        server.shutdown()

        assertEquals(AppError.Offline, (incidents.resolveIncident("i-1", "ok") as DataResult.Failure).error)
    }

    @Test
    fun `a successful write invalidates the cached alerts and incidents`() = runBlocking {
        server.enqueue(MockResponse().setBody(alertsJson))
        server.enqueue(MockResponse().setBody(detailJson))
        alerts.getAlerts()
        incidents.getIncident("i-1")
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"alertId":"a-1","incidentId":"i-1","acknowledgedBy":"u-1"}"""))
        incidents.acknowledgeAlert("a-1")
        // Sin red, ya no debe quedar una copia anterior a la acción.
        server.shutdown()

        assertTrue(alerts.getAlerts() is DataResult.Failure)
        assertTrue(incidents.getIncident("i-1") is DataResult.Failure)
    }
}
