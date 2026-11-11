package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation

import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.Alert
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertQuery
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertSeverity as DomainAlertSeverity
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertStatus as DomainAlertStatus
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertsRepository
import com.machineguard.mobile.boundedcontexts.iam.domain.SessionState
import com.machineguard.mobile.boundedcontexts.iam.domain.UserProfile
import com.machineguard.mobile.boundedcontexts.iam.domain.UserRole
import com.machineguard.mobile.core.data.AppError
import com.machineguard.mobile.core.data.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class AlertsViewModelTest {

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
    fun `alerts keep the repository order and map unknown values to unknown`() {
        val repo = FakeAlertsRepository(
            DataResult.Success(
                listOf(
                    alert("a-2", DomainAlertStatus.PENDING, DomainAlertSeverity.HIGH),
                    alert("a-1", DomainAlertStatus.UNKNOWN, DomainAlertSeverity.UNKNOWN),
                ),
            ),
        )

        val viewModel = AlertsViewModel(repo, MutableStateFlow(signedIn))

        val content = viewModel.state.value as AlertsUiState.Content
        assertEquals(listOf("a-2", "a-1"), content.alerts.map { it.id })
        assertEquals(AlertStatus.UNKNOWN, content.alerts.last().status)
        assertEquals(AlertSeverity.UNKNOWN, content.alerts.last().severity)
        assertEquals(AlertQuery(), repo.lastQuery)
    }

    @Test
    fun `empty repository result shows the empty state`() {
        val viewModel = AlertsViewModel(FakeAlertsRepository(DataResult.Success(emptyList())), MutableStateFlow(signedIn))

        assertEquals(AlertsUiState.Empty, viewModel.state.value)
    }

    @Test
    fun `failure shows the error state and retry loads the alerts`() {
        val repo = FakeAlertsRepository(DataResult.Failure(AppError.Timeout))
        val viewModel = AlertsViewModel(repo, MutableStateFlow(signedIn))
        assertEquals(AlertsUiState.Error, viewModel.state.value)

        repo.result = DataResult.Success(listOf(alert("a-1", DomainAlertStatus.PENDING, DomainAlertSeverity.LOW)))
        viewModel.retry()

        assertTrue(viewModel.state.value is AlertsUiState.Content)
    }

    @Test
    fun `signing out clears the alerts`() {
        val session = MutableStateFlow<SessionState>(signedIn)
        val viewModel = AlertsViewModel(
            FakeAlertsRepository(DataResult.Success(listOf(alert("a-1", DomainAlertStatus.PENDING, DomainAlertSeverity.LOW)))),
            session,
        )
        assertTrue(viewModel.state.value is AlertsUiState.Content)

        session.value = SessionState.SignedOut()

        assertEquals(AlertsUiState.Loading, viewModel.state.value)
    }

    @Test
    fun `zone id in the message is replaced by the zone name when known`() {
        val raw = alert("a-1", DomainAlertStatus.PENDING, DomainAlertSeverity.HIGH).copy(message = "HUMIDITY fuera de rango en la zona zone-1")

        assertEquals("HUMIDITY fuera de rango en la zona Cámara fría A", raw.toCardUiModel(mapOf("zone-1" to "Cámara fría A")).message)
        assertEquals("HUMIDITY fuera de rango en la zona zone-1", raw.toCardUiModel().message)
    }

    @Test
    fun `failed refresh keeps the previous alerts`() {
        val repo = FakeAlertsRepository(DataResult.Success(listOf(alert("a-1", DomainAlertStatus.PENDING, DomainAlertSeverity.LOW))))
        val viewModel = AlertsViewModel(repo, MutableStateFlow(signedIn))

        repo.result = DataResult.Failure(AppError.Timeout)
        viewModel.refresh()

        assertEquals(listOf("a-1"), (viewModel.state.value as AlertsUiState.Content).alerts.map { it.id })
        assertEquals(false, viewModel.isRefreshing.value)
    }

    private fun alert(id: String, status: DomainAlertStatus, severity: DomainAlertSeverity) = Alert(
        id = id,
        incidentId = "incident-$id",
        monitoringZoneId = "zone-1",
        severity = severity,
        status = status,
        message = "Alerta $id",
        raisedAt = OffsetDateTime.parse("2026-11-10T10:15:00-05:00"),
        acknowledgedAt = null,
        escalatedAt = null,
        historical = false,
    )
}

private class FakeAlertsRepository(var result: DataResult<List<Alert>>) : AlertsRepository {
    var lastQuery: AlertQuery? = null
        private set

    override suspend fun getAlerts(query: AlertQuery): DataResult<List<Alert>> {
        lastQuery = query
        return result
    }

    override suspend fun getAlert(alertId: String): DataResult<Alert> = error("not used by these tests")
}
