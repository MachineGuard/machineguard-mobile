package com.machineguard.mobile.boundedcontexts.iam.domain

/** Rol del usuario dentro de su organización (Capítulo IV, IAM). */
enum class Role {
    /** Configura zonas y umbrales. */
    ADMIN,

    /** Solo consulta información. */
    VIEWER;

    companion object {
        /** Si el API envía un rol desconocido se aplica el de menor privilegio. */
        fun fromApi(value: String?): Role =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: VIEWER
    }
}
