package com.machineguard.mobile.boundedcontexts.iam.infrastructure

import com.machineguard.mobile.boundedcontexts.iam.domain.UserProfile
import com.machineguard.mobile.boundedcontexts.iam.domain.UserRole
import com.machineguard.mobile.core.network.parseApiDate
import kotlinx.serialization.Serializable

/** Formas JSON del Core API para IAM (`/api/v1/auth` y `/api/v1/users`). Solo se declaran los campos que usa la app. */
@Serializable
internal data class LoginRequestDto(val email: String, val password: String)

@Serializable
internal data class LogoutRequestDto(val refreshToken: String)

@Serializable
internal data class UserDto(
    val id: String,
    val organizationId: String,
    val email: String,
    val fullName: String = "",
    val role: String? = null,
    val lastLoginAt: String? = null,
)

@Serializable
internal data class OrganizationDto(val id: String, val name: String = "")

@Serializable
internal data class AuthenticationDto(
    val accessToken: String,
    val refreshToken: String,
    /** Segundos de vida del access token. */
    val expiresIn: Long = 0,
    val user: UserDto,
    val organization: OrganizationDto? = null,
)

internal fun UserDto.toDomain(organizationName: String?): UserProfile = UserProfile(
    id = id,
    organizationId = organizationId,
    organizationName = organizationName,
    email = email,
    fullName = fullName,
    role = UserRole.fromApi(role),
    lastLoginAt = parseApiDate(lastLoginAt),
)
