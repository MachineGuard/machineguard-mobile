package com.machineguard.mobile.boundedcontexts.iam.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.machineguard.mobile.R
import com.machineguard.mobile.boundedcontexts.iam.application.LoginFailure
import com.machineguard.mobile.boundedcontexts.iam.application.LoginUiState
import com.machineguard.mobile.boundedcontexts.iam.application.LoginViewModel
import com.machineguard.mobile.boundedcontexts.iam.domain.EmailError
import com.machineguard.mobile.boundedcontexts.iam.domain.PasswordError
import com.machineguard.mobile.core.theme.MachineGuardTheme

// TODO: reemplazar por las URL reales de Terms y Privacy del Landing Page.
private const val TERMS_URL = "https://machineguard.example/terms"
private const val PRIVACY_URL = "https://machineguard.example/privacy"

@Composable
fun LoginScreen(
    sessionExpired: Boolean = false,
    viewModel: LoginViewModel = viewModel(factory = IamViewModelFactory.login),
) {
    val state by viewModel.state.collectAsState()
    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onSubmit = viewModel::submit,
        sessionExpired = sessionExpired,
    )
}

/** Versión sin estado de la pantalla, para previews y pruebas de interfaz. */
@Composable
fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    sessionExpired: Boolean = false,
) {
    val emailMessage = state.emailError?.let { emailErrorRes(it) }
    val passwordMessage = state.passwordError?.let { passwordErrorRes(it) }
    val failureMessage = state.failure?.let { failureRes(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.iam_brand),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.iam_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.iam_sign_in_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.iam_sign_in_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.iam_email_label)) },
                    placeholder = { Text(stringResource(R.string.iam_email_placeholder)) },
                    singleLine = true,
                    enabled = !state.isLoading,
                    isError = emailMessage != null,
                    supportingText = if (emailMessage != null) {
                        { Text(stringResource(emailMessage)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.iam_password_label)) },
                    singleLine = true,
                    enabled = !state.isLoading,
                    isError = passwordMessage != null,
                    supportingText = if (passwordMessage != null) {
                        { Text(stringResource(passwordMessage)) }
                    } else {
                        null
                    },
                    visualTransformation = if (state.passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    trailingIcon = {
                        TextButton(
                            onClick = onTogglePasswordVisibility,
                            enabled = !state.isLoading,
                        ) {
                            Text(
                                stringResource(
                                    if (state.passwordVisible) R.string.iam_hide_password
                                    else R.string.iam_show_password,
                                ),
                            )
                        }
                    },
                )

                if (sessionExpired && failureMessage == null) {
                    Text(
                        text = stringResource(R.string.iam_session_expired),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (failureMessage != null) {
                    // Color + icono + texto: el error no depende solo del color.
                    Row(
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = stringResource(failureMessage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Button(
                    onClick = onSubmit,
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.iam_signing_in))
                    } else {
                        Text(stringResource(R.string.iam_sign_in_button))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        LegalFooter()
    }
}

@Composable
private fun LegalFooter() {
    val uriHandler = LocalUriHandler.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { runCatching { uriHandler.openUri(TERMS_URL) } }) {
            Text(stringResource(R.string.iam_terms))
        }
        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { runCatching { uriHandler.openUri(PRIVACY_URL) } }) {
            Text(stringResource(R.string.iam_privacy))
        }
    }
}

@StringRes
private fun emailErrorRes(error: EmailError): Int = when (error) {
    EmailError.Empty -> R.string.iam_error_email_empty
    EmailError.Invalid -> R.string.iam_error_email_invalid
}

@StringRes
private fun passwordErrorRes(error: PasswordError): Int = when (error) {
    PasswordError.Empty -> R.string.iam_error_password_empty
}

@StringRes
private fun failureRes(failure: LoginFailure): Int = when (failure) {
    LoginFailure.InvalidCredentials -> R.string.iam_error_invalid_credentials
    LoginFailure.TooManyRequests -> R.string.iam_error_too_many_requests
    LoginFailure.AccountDisabled -> R.string.iam_error_account_disabled
    LoginFailure.Network -> R.string.iam_error_network
    LoginFailure.Server -> R.string.iam_error_server
    LoginFailure.Unknown -> R.string.iam_error_unknown
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun LoginContentPreview() {
    MachineGuardTheme {
        LoginContent(
            state = LoginUiState(email = "demo@machineguard.com", failure = LoginFailure.InvalidCredentials),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onSubmit = {},
        )
    }
}
