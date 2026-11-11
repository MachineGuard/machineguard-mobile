package com.machineguard.mobile.boundedcontexts.iam.infrastructure

import android.content.Context
import com.machineguard.mobile.boundedcontexts.iam.application.SessionManager
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthRepository
import com.machineguard.mobile.core.config.AppConfig

/**
 * Punto único donde se elige la implementación de autenticación.
 *
 * Lunes 9/11: cambiar [USE_FAKE_REPOSITORY] a `false` para usar el Core API real.
 */
object IamGraph {
    private const val USE_FAKE_REPOSITORY = true

    private var appContext: Context? = null

    /** Se llama una vez desde `MainActivity`. */
    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    val authRepository: AuthRepository by lazy {
        if (USE_FAKE_REPOSITORY) {
            FakeAuthRepository()
        } else {
            val context = checkNotNull(appContext) { "IamGraph.init(context) no fue llamado" }
            CoreApiAuthRepository(
                baseUrl = AppConfig.apiBaseUrl,
                tokenStore = EncryptedTokenStore(context),
            )
        }
    }

    val sessionManager: SessionManager by lazy { SessionManager(authRepository) }
}
