package com.machineguard.mobile.boundedcontexts.iam.presentation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.machineguard.mobile.appContainer
import com.machineguard.mobile.boundedcontexts.iam.application.LoginViewModel
import com.machineguard.mobile.boundedcontexts.iam.application.SessionViewModel
import com.machineguard.mobile.boundedcontexts.iam.domain.AuthRepository

/** Fábricas de ViewModel. Toman el repositorio de autenticación del `AppContainer` de la aplicación. */
internal object IamViewModelFactory {
    val session: ViewModelProvider.Factory = viewModelFactory {
        initializer { SessionViewModel(authRepository()) }
    }

    val login: ViewModelProvider.Factory = viewModelFactory {
        initializer { LoginViewModel(authRepository()) }
    }

    private fun CreationExtras.authRepository(): AuthRepository =
        checkNotNull(this[APPLICATION_KEY]).appContainer.authRepository
}
