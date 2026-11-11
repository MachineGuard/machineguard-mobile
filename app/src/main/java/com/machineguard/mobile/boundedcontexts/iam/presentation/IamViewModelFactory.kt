package com.machineguard.mobile.boundedcontexts.iam.presentation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.machineguard.mobile.boundedcontexts.iam.application.LoginViewModel
import com.machineguard.mobile.boundedcontexts.iam.application.SessionViewModel
import com.machineguard.mobile.boundedcontexts.iam.infrastructure.IamGraph

/** Fábricas de ViewModel. Toman las dependencias de [IamGraph]. */
internal object IamViewModelFactory {
    val session: ViewModelProvider.Factory = viewModelFactory {
        initializer { SessionViewModel(IamGraph.sessionManager) }
    }

    val login: ViewModelProvider.Factory = viewModelFactory {
        initializer { LoginViewModel(IamGraph.sessionManager) }
    }
}
