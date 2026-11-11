package com.machineguard.mobile.boundedcontexts.alertincidentmanagement

import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AcknowledgementResult
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.Acknowledgement
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.Alert
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertSeverity
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertStatus
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.CorrectiveAction
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.Incident
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentDetail
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentQuery
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentStatus
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentsRepository
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation.incidentdetail.ActionKind
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation.incidentdetail.IncidentDetailViewModel
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation.incidentdetail.TimelineStep
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation.incidentdetail.elapsedLabel
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation.incidentdetail.message
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation.incidentdetail.timeline
import com.machineguard.mobile.core.data.AppError
import com.machineguard.mobile.core.data.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

private val T0: OffsetDateTime = OffsetDateTime.of(2026, 11, 10, 12, 0, 0, 0, ZoneOffset.UTC)

private fun alert(status: AlertStatus = AlertStatus.PENDING, escalatedAt: OffsetDateTime? = null) = Alert(
    id = "a1",
    incidentId = "i1",
    monitoringZoneId = "z1",
    severity = AlertSeverity.CRITICAL,
    status = status,
    message = "Temperatura fuera de rango",
    raisedAt = T0,
    acknowledgedAt = null,
    escalatedAt = escalatedAt,
    historical = false,
)

private fun incident(status: IncidentStatus = IncidentStatus.OPEN) = Incident(
    id = "i1",
    originMeasurementId = null,
    monitoringZoneId = "z1",
    environmentalVariable = null,
    status = status,
    openedAt = T0,
    resolvedAt = if (status == IncidentStatus.RESOLVED) T0.plusHours(2) else null,
    resolvedBy = null,
    resolutionNotes = if (status == IncidentStatus.RESOLVED) "Ventilación restablecida" else null,
    historical = false,
)

private fun action(at: OffsetDateTime, description: String) =
    CorrectiveAction("c-$description", "i1", description, "user", at, null)

private fun detail(
    incident: Incident = incident(),
    alert: Alert? = alert(),
    acknowledgement: Acknowledgement? = null,
    actions: List<CorrectiveAction> = emptyList(),
    resolutionAllowed: Boolean = false,
) = IncidentDetail(
    incident = incident,
    alert = alert,
    acknowledgement = acknowledgement,
    correctiveActions = actions,
    withinSafeRange = resolutionAllowed,
    resolutionAllowed = resolutionAllowed,
    resolutionBlockReasons = if (resolutionAllowed) emptyList() else listOf("Falta una acción correctiva"),
)

/** Repositorio falso: guarda las llamadas y devuelve lo que se le configure. */
private class FakeIncidents(var current: DataResult<IncidentDetail>) : IncidentsRepository {
    val calls = mutableListOf<String>()
    var writeResult: DataResult<Any>? = null

    override suspend fun getIncidents(query: IncidentQuery) = DataResult.Success(emptyList<Incident>())

    override suspend fun getIncident(incidentId: String) = current

    override suspend fun acknowledgeAlert(alertId: String, note: String?): DataResult<AcknowledgementResult> {
        calls += "ack:$alertId:$note"
        @Suppress("UNCHECKED_CAST")
        return (writeResult ?: DataResult.Success(
            AcknowledgementResult(alertId, "i1", "user", T0, note, AlertStatus.ACKNOWLEDGED, IncidentStatus.ACKNOWLEDGED),
        )) as DataResult<AcknowledgementResult>
    }

    override suspend fun registerCorrectiveAction(
        incidentId: String,
        description: String,
        notes: String?,
    ): DataResult<CorrectiveAction> {
        calls += "action:$description:$notes"
        @Suppress("UNCHECKED_CAST")
        return (writeResult ?: DataResult.Success(action(T0, description))) as DataResult<CorrectiveAction>
    }

    override suspend fun resolveIncident(incidentId: String, notes: String?): DataResult<Incident> {
        calls += "resolve:$notes"
        @Suppress("UNCHECKED_CAST")
        return (writeResult ?: DataResult.Success(incident(IncidentStatus.RESOLVED))) as DataResult<Incident>
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentDetailTest {

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repo: FakeIncidents, canManage: Boolean = true) =
        IncidentDetailViewModel("i1", repo, canManage)

    @Test
    fun `la linea de tiempo marca lo hecho y lo pendiente`() {
        val steps = detail().timeline()
        assertEquals(
            listOf(
                TimelineStep.Kind.RAISED,
                TimelineStep.Kind.ACKNOWLEDGED,
                TimelineStep.Kind.CORRECTIVE_ACTION,
                TimelineStep.Kind.RESOLVED,
            ),
            steps.map { it.kind },
        )
        assertEquals(listOf(true, false, false, false), steps.map { it.done })
    }

    @Test
    fun `la linea de tiempo ordena las acciones, incluye escalamiento y resolucion`() {
        val d = detail(
            incident = incident(IncidentStatus.RESOLVED),
            alert = alert(AlertStatus.CLOSED, escalatedAt = T0.plusMinutes(30)),
            acknowledgement = Acknowledgement("admin", T0.plusMinutes(5), "Voy en camino"),
            actions = listOf(action(T0.plusHours(1), "segunda"), action(T0.plusMinutes(40), "primera")),
        )
        val steps = d.timeline()
        assertEquals(
            listOf("Alerta detectada", "Alerta reconocida", "Alerta escalada", "Acción correctiva", "Acción correctiva", "Incidente resuelto"),
            steps.map { it.title },
        )
        assertEquals(listOf("primera", "segunda"), steps.filter { it.kind == TimelineStep.Kind.CORRECTIVE_ACTION }.map { it.detail })
        assertTrue(steps.all { it.done })
        assertEquals("admin · Voy en camino", steps[1].detail)
    }

    @Test
    fun `el tiempo transcurrido se escribe en minutos, horas y dias`() {
        assertEquals("5 min", elapsedLabel(T0, T0.plusMinutes(5)))
        assertEquals("2 h 10 min", elapsedLabel(T0, T0.plusMinutes(130)))
        assertEquals("1 d 3 h", elapsedLabel(T0, T0.plusHours(27)))
        assertEquals("0 min", elapsedLabel(T0, T0.minusMinutes(3)))
        assertEquals("—", elapsedLabel(null, T0))
    }

    @Test
    fun `carga el detalle al iniciar`() = runTest {
        val vm = viewModel(FakeIncidents(DataResult.Success(detail())))
        advanceUntilIdle()
        assertFalse(vm.state.value.loading)
        assertNotNull(vm.state.value.detail)
    }

    @Test
    fun `sin conexion y sin copia muestra el error de carga`() = runTest {
        val vm = viewModel(FakeIncidents(DataResult.Failure(AppError.Offline)))
        advanceUntilIdle()
        assertNull(vm.state.value.detail)
        assertEquals(AppError.Offline, vm.state.value.loadError)
    }

    @Test
    fun `datos de la copia local se marcan como tal`() = runTest {
        val vm = viewModel(
            FakeIncidents(
                DataResult.Success(detail(), com.machineguard.mobile.core.data.DataSource.CACHE),
            ),
        )
        advanceUntilIdle()
        assertTrue(vm.state.value.fromCache)
    }

    @Test
    fun `reconocer envia la nota recortada y recarga el detalle`() = runTest {
        val repo = FakeIncidents(DataResult.Success(detail()))
        val vm = viewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.state.value.canAcknowledge)

        vm.acknowledge("  voy para alla  ")
        advanceUntilIdle()
        assertEquals(listOf("ack:a1:voy para alla"), repo.calls)
        assertEquals(ActionKind.ACKNOWLEDGE, vm.state.value.lastSuccess)
        assertFalse(vm.state.value.submitting)
    }

    @Test
    fun `una nota vacia no se envia`() = runTest {
        val repo = FakeIncidents(DataResult.Success(detail()))
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.acknowledge("   ")
        advanceUntilIdle()
        assertEquals(listOf("ack:a1:null"), repo.calls)
    }

    @Test
    fun `no se puede reconocer una alerta ya reconocida`() = runTest {
        val repo = FakeIncidents(
            DataResult.Success(
                detail(incident(IncidentStatus.ACKNOWLEDGED), alert(AlertStatus.ACKNOWLEDGED), Acknowledgement("a", T0, null)),
            ),
        )
        val vm = viewModel(repo)
        advanceUntilIdle()
        assertFalse(vm.state.value.canAcknowledge)
        vm.acknowledge(null)
        advanceUntilIdle()
        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun `registrar una accion exige descripcion`() = runTest {
        val repo = FakeIncidents(DataResult.Success(detail()))
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.registerCorrectiveAction("   ", "nota")
        advanceUntilIdle()
        assertTrue(repo.calls.isEmpty())

        vm.registerCorrectiveAction(" Abrir ventilacion ", "")
        advanceUntilIdle()
        assertEquals(listOf("action:Abrir ventilacion:null"), repo.calls)
        assertEquals(ActionKind.CORRECTIVE_ACTION, vm.state.value.lastSuccess)
    }

    @Test
    fun `resolver solo se habilita si el servidor lo permite`() = runTest {
        val blocked = FakeIncidents(DataResult.Success(detail(resolutionAllowed = false)))
        val vmBlocked = viewModel(blocked)
        advanceUntilIdle()
        assertFalse(vmBlocked.state.value.canResolve)
        vmBlocked.resolve("listo")
        advanceUntilIdle()
        assertTrue(blocked.calls.isEmpty())

        val allowed = FakeIncidents(DataResult.Success(detail(resolutionAllowed = true)))
        val vm = viewModel(allowed)
        advanceUntilIdle()
        assertTrue(vm.state.value.canResolve)
        vm.resolve("Todo normal")
        advanceUntilIdle()
        assertEquals(listOf("resolve:Todo normal"), allowed.calls)
        assertEquals(ActionKind.RESOLVE, vm.state.value.lastSuccess)
    }

    @Test
    fun `un incidente resuelto ya no admite acciones`() = runTest {
        val vm = viewModel(
            FakeIncidents(DataResult.Success(detail(incident(IncidentStatus.RESOLVED), alert(AlertStatus.CLOSED), null, emptyList(), true))),
        )
        advanceUntilIdle()
        assertFalse(vm.state.value.canAcknowledge)
        assertFalse(vm.state.value.canRegisterAction)
        assertFalse(vm.state.value.canResolve)
    }

    @Test
    fun `un usuario de solo lectura no puede escribir`() = runTest {
        val repo = FakeIncidents(DataResult.Success(detail(resolutionAllowed = true)))
        val vm = viewModel(repo, canManage = false)
        advanceUntilIdle()
        assertFalse(vm.state.value.canAcknowledge)
        assertFalse(vm.state.value.canRegisterAction)
        assertFalse(vm.state.value.canResolve)
        vm.resolve("x")
        vm.acknowledge("x")
        advanceUntilIdle()
        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun `un conflicto muestra el motivo del servidor y recarga`() = runTest {
        val repo = FakeIncidents(DataResult.Success(detail(resolutionAllowed = true)))
        val vm = viewModel(repo)
        advanceUntilIdle()
        repo.writeResult = DataResult.Failure(AppError.Conflict("Incidente ya resuelto"))
        vm.resolve(null)
        advanceUntilIdle()
        val failure = vm.state.value.actionFailure
        assertNotNull(failure)
        assertEquals(ActionKind.RESOLVE, failure!!.action)
        assertEquals("Incidente ya resuelto", failure.error.message())
        assertNull(vm.state.value.lastSuccess)
        assertFalse(vm.state.value.submitting)
        assertNotNull(vm.state.value.detail)
    }

    @Test
    fun `un error de escritura sin conexion conserva los datos en pantalla`() = runTest {
        val repo = FakeIncidents(DataResult.Success(detail()))
        val vm = viewModel(repo)
        advanceUntilIdle()
        repo.writeResult = DataResult.Failure(AppError.Offline)
        repo.current = DataResult.Failure(AppError.Offline)
        vm.acknowledge(null)
        advanceUntilIdle()
        assertEquals(AppError.Offline, vm.state.value.actionFailure?.error)
        assertNotNull(vm.state.value.detail)
        assertNull(vm.state.value.loadError)
    }

    @Test
    fun `descartar el mensaje limpia el estado`() = runTest {
        val repo = FakeIncidents(DataResult.Success(detail()))
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.acknowledge(null)
        advanceUntilIdle()
        vm.dismissMessage()
        assertNull(vm.state.value.lastSuccess)
        assertNull(vm.state.value.actionFailure)
    }
}
