package com.machineguard.mobile.core.di

import android.content.Context
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.AlertsRepository
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.domain.IncidentsRepository
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.infrastructure.AlertsRepositoryImpl
import com.machineguard.mobile.boundedcontexts.alertincidentmanagement.infrastructure.IncidentsRepositoryImpl
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.MeasurementsRepository
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.ZonesRepository
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.infrastructure.MeasurementsRepositoryImpl
import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.infrastructure.ZonesRepositoryImpl
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthRepository
import com.machineguard.mobile.boundedcontexts.iam.infrastructure.AuthRepositoryImpl
import com.machineguard.mobile.core.cache.FileLocalCache
import com.machineguard.mobile.core.config.AppConfig
import com.machineguard.mobile.core.data.RemoteSource
import com.machineguard.mobile.core.network.ApiClient
import com.machineguard.mobile.core.network.HttpClientFactory
import com.machineguard.mobile.core.session.EncryptedSessionStorage
import com.machineguard.mobile.core.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock

/**
 * Contenedor de dependencias de la app, armado a mano (sin framework de inyección). Las pantallas y los ViewModels
 * obtienen los repositorios de aquí, siempre como interfaces de `domain`:
 *
 * ```kotlin
 * val zones = context.appContainer.zonesRepository
 * ```
 */
class AppContainer(
    context: Context,
    baseUrl: String = AppConfig.apiBaseUrl,
    clock: Clock = Clock.systemUTC(),
) {
    private val cache = FileLocalCache(File(context.noBackupFilesDir, "api-cache"))

    /** Estado de la sesión y renovación de tokens. Los repositorios de datos lo usan de forma transparente. */
    val sessionManager = SessionManager(EncryptedSessionStorage(context.applicationContext)) { cache.clear() }

    private val api = ApiClient(HttpClientFactory.create(baseUrl, sessionManager, clock), baseUrl)
    private val remote = RemoteSource(api, cache, clock)

    val authRepository: AuthRepository = AuthRepositoryImpl(api, sessionManager, clock)
    val zonesRepository: ZonesRepository = ZonesRepositoryImpl(remote)
    val measurementsRepository: MeasurementsRepository = MeasurementsRepositoryImpl(remote)
    val alertsRepository: AlertsRepository = AlertsRepositoryImpl(remote)
    val incidentsRepository: IncidentsRepository = IncidentsRepositoryImpl(remote)

    /** Restaura la sesión guardada. Se llama una vez al iniciar la app; mientras tanto `session` es `Restoring`. */
    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) { sessionManager.restore() }
    }
}
