package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation.incidentdetail

import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentDetail
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentStatus
import com.machineguard.mobile.core.data.AppError
import java.time.OffsetDateTime

/** Paso de la línea de tiempo del incidente: Detectada → Reconocida → Acción correctiva → Resuelta. */
data class TimelineStep(
    val kind: Kind,
    val detail: String?,
    val at: OffsetDateTime?,
    val done: Boolean,
) {
    enum class Kind { RAISED, ACKNOWLEDGED, ESCALATED, CORRECTIVE_ACTION, RESOLVED }
}

/** Construye la línea de tiempo con los datos que ya trae el detalle del incidente (sin pedir nada más al servidor). */
fun IncidentDetail.timeline(): List<TimelineStep> {
    val steps = mutableListOf<TimelineStep>()
    steps += TimelineStep(
        kind = TimelineStep.Kind.RAISED,
        detail = alert?.message,
        at = alert?.raisedAt ?: incident.openedAt,
        done = true,
    )
    steps += TimelineStep(
        kind = TimelineStep.Kind.ACKNOWLEDGED,
        detail = acknowledgement?.note,
        at = acknowledgement?.acknowledgedAt,
        done = acknowledgement != null,
    )
    alert?.escalatedAt?.let {
        steps += TimelineStep(TimelineStep.Kind.ESCALATED, null, it, done = true)
    }
    if (correctiveActions.isEmpty()) {
        steps += TimelineStep(TimelineStep.Kind.CORRECTIVE_ACTION, null, null, done = false)
    } else {
        correctiveActions.sortedBy { it.performedAt }.forEach {
            steps += TimelineStep(
                kind = TimelineStep.Kind.CORRECTIVE_ACTION,
                        detail = listOfNotNull(it.description, it.notes).joinToString(" · "),
                at = it.performedAt,
                done = true,
            )
        }
    }
    steps += TimelineStep(
        kind = TimelineStep.Kind.RESOLVED,
        detail = incident.resolutionNotes,
        at = incident.resolvedAt,
        done = incident.status == IncidentStatus.RESOLVED,
    )
    return steps
}

/** Qué falló al ejecutar una acción, listo para que la pantalla lo traduzca a un mensaje. */
enum class ActionKind { ACKNOWLEDGE, CORRECTIVE_ACTION, RESOLVE }

data class ActionFailure(val action: ActionKind, val error: AppError)

data class IncidentDetailUiState(
    val loading: Boolean = true,
    val detail: IncidentDetail? = null,
    /** Hay un error de carga y no hay datos que mostrar. */
    val loadError: AppError? = null,
    /** Los datos mostrados vienen de la copia local (sin conexión). */
    val fromCache: Boolean = false,
    val canManage: Boolean = false,
    /** Hay una escritura en curso: se bloquean los botones para no duplicarla. */
    val submitting: Boolean = false,
    val actionFailure: ActionFailure? = null,
    val lastSuccess: ActionKind? = null,
) {
    private val active get() = detail?.incident?.status?.isActive == true

    val canAcknowledge: Boolean
        get() = canManage && !submitting && active && detail?.acknowledgement == null &&
            detail?.alert != null && detail.incident.status == IncidentStatus.OPEN
    val canRegisterAction: Boolean get() = canManage && !submitting && active
    val canResolve: Boolean get() = canManage && !submitting && active && detail?.resolutionAllowed == true
}
