package com.machineguard.mobile.boundedcontexts.environmentalmonitoring

import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.Freshness
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.ZoneCondition
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.ZoneStatus
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.infrastructure.MeasurementsRepositoryImpl
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.infrastructure.ZonesRepositoryImpl
import com.machineguard.mobile.core.DataLayerEnv
import com.machineguard.mobile.core.FIXED_OFFSET_NOW
import com.machineguard.mobile.core.data.AppError
import com.machineguard.mobile.core.data.DataResult
import com.machineguard.mobile.core.data.DataSource
import com.machineguard.mobile.shared.domain.EnvironmentalVariable
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.math.BigDecimal
import java.time.OffsetDateTime

class ZonesRepositoryTest {
    private val server = MockWebServer()
    private lateinit var env: DataLayerEnv
    private lateinit var zones: ZonesRepositoryImpl

    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun setUp() {
        server.start()
        env = DataLayerEnv(server, folder.newFolder("cache"))
        env.signIn()
        zones = ZonesRepositoryImpl(env.remote)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private val zonesJson = """
        [{
          "id": "z-1", "organizationId": "o-1", "name": "Cámara fría 1", "description": "Lácteos",
          "status": "ACTIVE", "environmentalCondition": "OUT_OF_RANGE",
          "sensorsOnline": 2, "sensorsOffline": 1, "sensorsInactive": 0,
          "lastUpdatedAt": "2026-11-10T11:58:00Z",
          "points": [{"id": "p-1", "name": "Puerta", "location": "Entrada", "status": "ACTIVE"}],
          "latestMeasurements": [
            {"id": "m-1", "monitoringZoneId": "z-1", "monitoringPointId": "p-1", "environmentalVariable": "TEMPERATURE",
             "measuredValue": 9.50, "recordedAt": "2026-11-10T11:58:00Z", "source": "SENSOR", "freshness": "FRESH"},
            {"id": "m-2", "monitoringZoneId": "z-1", "monitoringPointId": "p-1", "environmentalVariable": "PRESSURE",
             "measuredValue": 1.0, "recordedAt": "2026-11-10T11:58:00Z"},
            {"id": "m-3", "monitoringZoneId": "z-1", "monitoringPointId": "p-1", "environmentalVariable": "HUMIDITY",
             "measuredValue": null, "recordedAt": "2026-11-10T11:58:00Z"}
          ],
          "campoNuevoDelServidor": true
        },
        {"id": "z-2", "name": "Almacén", "status": "ARCHIVED", "environmentalCondition": "ALGO_NUEVO"}]
    """.trimIndent()

    @Test
    fun `zones are mapped to the domain model`() = runBlocking {
        server.enqueue(MockResponse().setBody(zonesJson))

        val result = zones.getZones() as DataResult.Success
        val zone = result.data.first()

        assertEquals(DataSource.NETWORK, result.source)
        assertEquals("Cámara fría 1", zone.name)
        assertEquals("Lácteos", zone.description)
        assertEquals(ZoneStatus.ACTIVE, zone.status)
        assertEquals(ZoneCondition.OUT_OF_RANGE, zone.condition)
        assertEquals(listOf(2, 1, 0), listOf(zone.sensorsOnline, zone.sensorsOffline, zone.sensorsInactive))
        assertEquals(OffsetDateTime.parse("2026-11-10T11:58:00Z"), zone.lastUpdatedAt)
        assertEquals("Puerta", zone.pointName("p-1"))
        assertNull(zone.pointName("otro"))
    }

    @Test
    fun `decimal readings keep their exact value`() = runBlocking {
        server.enqueue(MockResponse().setBody(zonesJson))

        val reading = (zones.getZones() as DataResult.Success).data.first().latestMeasurements.single()

        assertEquals("9.50", reading.value.toPlainString())
        assertEquals(EnvironmentalVariable.TEMPERATURE, reading.variable)
        assertEquals(Freshness.FRESH, reading.freshness)
        assertEquals("p-1", reading.pointId)
    }

    @Test
    fun `readings with an unknown variable or without a value are dropped instead of invented`() = runBlocking {
        server.enqueue(MockResponse().setBody(zonesJson))

        val readings = (zones.getZones() as DataResult.Success).data.first().latestMeasurements

        assertEquals(1, readings.size)
        assertTrue(readings.none { it.value == BigDecimal.ZERO })
    }

    @Test
    fun `unknown states do not break parsing`() = runBlocking {
        server.enqueue(MockResponse().setBody(zonesJson))

        val unknown = (zones.getZones() as DataResult.Success).data.last()

        assertEquals(ZoneStatus.UNKNOWN, unknown.status)
        assertEquals(ZoneCondition.UNKNOWN, unknown.condition)
        assertTrue(unknown.points.isEmpty())
        assertTrue(unknown.latestMeasurements.isEmpty())
    }

    @Test
    fun `without connection the last saved copy is returned and marked as cache`() = runBlocking {
        server.enqueue(MockResponse().setBody(zonesJson))
        zones.getZones()
        server.shutdown()

        val result = zones.getZones() as DataResult.Success

        assertEquals(DataSource.CACHE, result.source)
        assertTrue(result.isFromCache)
        assertEquals(FIXED_OFFSET_NOW, result.fetchedAt)
        assertEquals("Cámara fría 1", result.data.first().name)
    }

    @Test
    fun `a server error also falls back to the saved copy`() = runBlocking {
        server.enqueue(MockResponse().setBody(zonesJson))
        server.enqueue(MockResponse().setResponseCode(503))
        zones.getZones()

        assertEquals(DataSource.CACHE, (zones.getZones() as DataResult.Success).source)
    }

    @Test
    fun `offline without any saved copy is an error`() = runBlocking {
        server.shutdown()

        assertEquals(AppError.Offline, (zones.getZones() as DataResult.Failure).error)
    }

    @Test
    fun `authorization and not found errors are never hidden by the cache`() = runBlocking {
        server.enqueue(MockResponse().setBody(zonesJson))
        zones.getZones()
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse().setResponseCode(403))

        assertTrue((zones.getZone("z-9") as DataResult.Failure).error is AppError.NotFound)
        assertTrue((zones.getZones() as DataResult.Failure).error is AppError.Forbidden)
    }

    @Test
    fun `a response that is not valid json is an error and is not cached`() = runBlocking {
        server.enqueue(MockResponse().setBody("<html>proxy</html>"))
        server.enqueue(MockResponse().setResponseCode(503))

        assertTrue((zones.getZones() as DataResult.Failure).error is AppError.Unknown)
        assertTrue(zones.getZones() is DataResult.Failure)
    }

    @Test
    fun `latest measurements are requested for a zone`() = runBlocking {
        server.enqueue(MockResponse().setBody("""[{"id":"m-1","monitoringZoneId":"z-1","monitoringPointId":"p-1",
            "environmentalVariable":"HUMIDITY","measuredValue":55.20,"recordedAt":"2026-11-10T11:58:00Z"}]"""))

        val result = MeasurementsRepositoryImpl(env.remote).getLatest("z-1") as DataResult.Success

        assertEquals("/api/v1/environmental-monitoring/measurements/latest?monitoringZoneId=z-1", server.takeRequest().path)
        assertEquals("55.20", result.data.single().value.toPlainString())
    }
}
