package com.machineguard.mobile.boundedcontexts.iam.application

import com.machineguard.mobile.boundedcontexts.iam.domain.UserProfile

sealed interface SessionState {
    /** Se está comprobando si hay una sesión guardada. */
    data object Checking : SessionState

    data object SignedOut : SessionState

    data class SignedIn(val profile: UserProfile) : SessionState
}
