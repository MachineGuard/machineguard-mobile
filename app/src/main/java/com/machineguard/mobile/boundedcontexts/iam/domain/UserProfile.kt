package com.machineguard.mobile.boundedcontexts.iam.domain

import java.time.OffsetDateTime

enum class UserRole {
    /** Puede reconocer alertas, registrar acciones correctivas y resolver incidentes. */
    ADMIN,

    /** Solo consulta. El Core API responde 403 si intenta una acción de escritura. */
    VIEWER,

    /** Rol que esta versión de la app no conoce: se trata como solo lectura. */
    UNKNOWN,
    ;

    companion object {
        fun fromApi(value: String?): UserRole = entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

/** Usuario autenticado (`GET /api/v1/users/me`). Los identificadores son UUID en texto. */
data class UserProfile(
    val id: String,
    val organizationId: String,
    val organizationName: String?,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val lastLoginAt: OffsetDateTime?,
) {
    val canManageIncidents: Boolean get() = role == UserRole.ADMIN
}
