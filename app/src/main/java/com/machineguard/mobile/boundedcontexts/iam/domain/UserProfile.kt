package com.machineguard.mobile.boundedcontexts.iam.domain

/** Perfil del usuario autenticado (`GET /api/v1/users/me`). */
data class UserProfile(
    val fullName: String,
    val email: String,
    val role: Role,
)
