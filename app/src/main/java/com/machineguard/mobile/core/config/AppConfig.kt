package com.machineguard.mobile.core.config

import com.machineguard.mobile.BuildConfig

object AppConfig {
    /**
     * URL base del Core API. En el emulador, 10.0.2.2 apunta al localhost del equipo.
     * Se sobrescribe con -PapiBaseUrl=... al compilar.
     */
    val apiBaseUrl: String = BuildConfig.API_BASE_URL
}
