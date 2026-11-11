package com.machineguard.mobile.boundedcontexts.environmentalmonitoring.presentation

import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.Freshness
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.Measurement
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.MeasurementSource
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.MonitoringPoint
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.Zone
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.ZoneStatus
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.ZonesRepository
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.ZoneCondition as DomainZoneCondition
import com.machineguard.mobile.boundedcontexts.iam.domain.SessionState
import com.machineguard.mobile.boundedcontexts.iam.domain.UserProfile
import com.machineguard.mobile.boundedcontexts.iam.domain.UserRole
import com.machineguard.mobile.core.data.AppError
import com.machineguard.mobile.core.data.DataResult
import com.machineguard.mobile.core.data.DataSource
import com.machineguard.mobile.shared.domain.EnvironmentalVariable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.OffsetDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val signedIn = SessionState.SignedIn(
        UserProfile("user-1", "org-1", "Org", "ana@example.com", "Ana", UserRole.VIEWER, null),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signed in session loads zones sorted by condition`() {
        val repo = FakeZonesRepository(
            DataResult.Success(listOf(zone("normal", DomainZoneCondition.NORMAL), zone("critical", DomainZoneCondition.OUT_OF_RANGE))),
        )

        val viewModel = DashboardViewModel(repo, MutableStateFlow(signedIn))

        val content = viewModel.state.value as DashboardUiState.Content
        assertEquals(listOf("critical", "normal"), content.zones.map { it.id })
    }

    @Test
    fun `empty repository result shows the empty state`() {
        val viewModel = DashboardViewModel(FakeZonesRepository(DataResult.Success(emptyList())), MutableStateFlow(signedIn))

        assertEquals(DashboardUiState.Empty, viewModel.state.value)
    }

    @Test
    fun `cached zones still render as content`() {
        val repo = FakeZonesRepository(
            DataResult.Success(listOf(zone("cached", DomainZoneCondition.STALE)), source = DataSource.CACHE),
        )

        val viewModel = DashboardViewModel(repo, MutableStateFlow(signedIn))

        val content = viewModel.state.value as DashboardUiState.Content
        assertEquals(ZoneCondition.STALE, content.zones.single().condition)
    }

    @Test
    fun `readings keep their point name and variable and unknown conditions show as no data`() {
        val point = MonitoringPoint("p-1", "Puerta", null, active = true)
        val reading = Measurement(
            id = "m-1", zoneId = "z-1", pointId = "p-1", variable = EnvironmentalVariable.HUMIDITY,
            value = BigDecimal("65.00"), recordedAt = OffsetDateTime.parse("2026-11-10T11:58:00Z"),
            source = MeasurementSource.SENSOR, freshness = Freshness.FRESH,
        )
        val repo = FakeZonesRepository(
            DataResult.Success(listOf(zone("z-1", DomainZoneCondition.UNKNOWN, points = listOf(point), readings = listOf(reading)))),
        )

        val card = (DashboardViewModel(repo, MutableStateFlow(signedIn)).state.value as DashboardUiState.Content).zones.single()

        assertEquals(ZoneCondition.NO_DATA, card.condition)
        assertEquals(listOf("Puerta"), card.readings.map { it.pointName })
        assertEquals(ReadingVariable.HUMIDITY, card.readings.single().variable)
        assertEquals(BigDecimal("65.00"), card.readings.single().value)
    }

    @Test
    fun `a reading whose point is not in the zone has no point name for the UI to localize`() {
        val reading = Measurement(
            id = "m-2", zoneId = "z-1", pointId = "p-missing", variable = EnvironmentalVariable.TEMPERATURE,
            value = BigDecimal("4.20"), recordedAt = OffsetDateTime.parse("2026-11-10T11:58:00Z"),
            source = MeasurementSource.SENSOR, freshness = Freshness.FRESH,
        )
        val repo = FakeZonesRepository(DataResult.Success(listOf(zone("z-1", DomainZoneCondition.NORMAL, readings = listOf(reading)))))

        val card = (DashboardViewModel(repo, MutableStateFlow(signedIn)).state.value as DashboardUiState.Content).zones.single()

        assertNull(card.readings.single().pointName)
    }

    @Test
    fun `failure shows the error state and retry loads the zones`() {
        val repo = FakeZonesRepository(DataResult.Failure(AppError.Offline))
        val viewModel = DashboardViewModel(repo, MutableStateFlow(signedIn))
        assertEquals(DashboardUiState.Error, viewModel.state.value)

        repo.result = DataResult.Success(listOf(zone("z-1", DomainZoneCondition.NORMAL)))
        viewModel.retry()

        assertTrue(viewModel.state.value is DashboardUiState.Content)
    }

    @Test
    fun `signing out clears the zones and signing in loads them again`() {
        val repo = FakeZonesRepository(DataResult.Success(listOf(zone("z-1", DomainZoneCondition.NORMAL))))
        val session = MutableStateFlow<SessionState>(signedIn)
        val viewModel = DashboardViewModel(repo, session)
        assertTrue(viewModel.state.value is DashboardUiState.Content)

        session.value = SessionState.SignedOut()
        assertEquals(DashboardUiState.Loading, viewModel.state.value)

        session.value = signedIn
        assertTrue(viewModel.state.value is DashboardUiState.Content)
        assertEquals(2, repo.calls)
    }

    @Test
    fun `refresh keeps the zones visible and replaces them with the new result`() {
        val repo = FakeZonesRepository(DataResult.Success(listOf(zone("a", DomainZoneCondition.NORMAL))))
        val viewModel = DashboardViewModel(repo, MutableStateFlow(signedIn))

        repo.result = DataResult.Success(listOf(zone("b", DomainZoneCondition.NORMAL)))
        viewModel.refresh()

        assertEquals(listOf("b"), (viewModel.state.value as DashboardUiState.Content).zones.map { it.id })
        assertEquals(false, viewModel.isRefreshing.value)
        assertEquals(2, repo.calls)
    }

    @Test
    fun `failed refresh keeps the previous zones`() {
        val repo = FakeZonesRepository(DataResult.Success(listOf(zone("a", DomainZoneCondition.NORMAL))))
        val viewModel = DashboardViewModel(repo, MutableStateFlow(signedIn))

        repo.result = DataResult.Failure(AppError.Timeout)
        viewModel.refresh()

        assertEquals(listOf("a"), (viewModel.state.value as DashboardUiState.Content).zones.map { it.id })
    }

    @Test
    fun `refresh does nothing without a session`() {
        val repo = FakeZonesRepository(DataResult.Success(listOf(zone("a", DomainZoneCondition.NORMAL))))
        val viewModel = DashboardViewModel(repo, MutableStateFlow(SessionState.SignedOut()))

        viewModel.refresh()

        assertEquals(0, repo.calls)
        assertEquals(DashboardUiState.Loading, viewModel.state.value)
    }

    private fun zone(
        id: String,
        condition: DomainZoneCondition,
        points: List<MonitoringPoint> = emptyList(),
        readings: List<Measurement> = emptyList(),
    ) = Zone(
        id = id,
        name = "Zona $id",
        description = null,
        status = ZoneStatus.ACTIVE,
        condition = condition,
        sensorsOnline = 1,
        sensorsOffline = 0,
        sensorsInactive = 0,
        lastUpdatedAt = null,
        points = points,
        latestMeasurements = readings,
    )
}

private class FakeZonesRepository(var result: DataResult<List<Zone>>) : ZonesRepository {
    var calls = 0
        private set

    override suspend fun getZones(): DataResult<List<Zone>> {
        calls++
        return result
    }

    override suspend fun getZone(zoneId: String): DataResult<Zone> = error("not used by these tests")
}
