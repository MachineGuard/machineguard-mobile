package com.machineguard.mobile.boundedcontexts.iam.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.machineguard.mobile.R
import com.machineguard.mobile.boundedcontexts.iam.domain.SessionState
import com.machineguard.mobile.boundedcontexts.iam.application.SessionViewModel

/**
 * Navegación según la sesión: login si no hay sesión, la app si la hay.
 * Se usa en `MainActivity` envolviendo a `AppShell`.
 */
@Composable
fun AuthGate(
    viewModel: SessionViewModel = viewModel(factory = IamViewModelFactory.session),
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    when (state) {
        SessionState.Restoring -> CheckingSession()
        is SessionState.SignedOut -> LoginScreen(sessionExpired = (state as SessionState.SignedOut).expired)
        is SessionState.SignedIn -> content()
    }
}

@Composable
private fun CheckingSession() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.iam_checking_session),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
