package com.machineguard.mobile.boundedcontexts.iam.domain

/** Estado de la sesión del dispositivo. La navegación principal debe decidir según este valor. */
sealed interface SessionState {

    /** La app arrancó y todavía se está leyendo el almacenamiento seguro. Mostrar una pantalla de carga. */
    data object Restoring : SessionState

    /**
     * No hay sesión. [expired] es verdadero cuando se perdió por una renovación fallida (el token venció o fue
     * revocado): la pantalla de login puede mostrar "Tu sesión terminó, inicia sesión de nuevo".
     */
    data class SignedOut(val expired: Boolean = false) : SessionState

    data class SignedIn(val user: UserProfile) : SessionState
}
