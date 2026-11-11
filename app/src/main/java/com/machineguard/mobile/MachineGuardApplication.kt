package com.machineguard.mobile

import android.app.Application
import android.content.Context
import com.machineguard.mobile.core.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MachineGuardApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start(applicationScope)
    }
}

/** Acceso al [AppContainer] desde cualquier `Context` (actividad, composable o ViewModel con la aplicación). */
val Context.appContainer: AppContainer
    get() = (applicationContext as MachineGuardApplication).container
