package com.machineguard.mobile.boundedcontexts.iam.domain

/** Almacén seguro de los tokens de la sesión (JWT de acceso y refresh token rotatorio). */
interface TokenStore {
    fun accessToken(): String?
    fun refreshToken(): String?
    fun save(accessToken: String, refreshToken: String)
    fun clear()
}
