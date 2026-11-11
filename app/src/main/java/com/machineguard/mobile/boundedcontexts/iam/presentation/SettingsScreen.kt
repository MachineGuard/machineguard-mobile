package com.machineguard.mobile.boundedcontexts.iam.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.machineguard.mobile.BuildConfig
import com.machineguard.mobile.R
import com.machineguard.mobile.boundedcontexts.iam.domain.SessionState
import com.machineguard.mobile.boundedcontexts.iam.application.SessionViewModel
import com.machineguard.mobile.boundedcontexts.iam.domain.UserRole
import com.machineguard.mobile.boundedcontexts.iam.domain.UserProfile
import com.machineguard.mobile.core.theme.MachineGuardTheme

@Composable
fun SettingsScreen(
    viewModel: SessionViewModel = viewModel(factory = IamViewModelFactory.session),
) {
    val session by viewModel.state.collectAsState()
    val isSigningOut by viewModel.isSigningOut.collectAsState()

    SettingsContent(
        profile = (session as? SessionState.SignedIn)?.user,
        isSigningOut = isSigningOut,
        onSignOut = viewModel::signOut,
    )
}

/** Versión sin estado de la pantalla, para previews y pruebas de interfaz. */
@Composable
fun SettingsContent(
    profile: UserProfile?,
    isSigningOut: Boolean,
    onSignOut: () -> Unit,
) {
    var showConfirmation by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.iam_settings_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        if (profile != null) {
            Text(
                text = stringResource(R.string.iam_settings_account),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AccountCard(profile)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            InfoRow(
                label = stringResource(R.string.iam_settings_app),
                value = stringResource(R.string.iam_settings_version, BuildConfig.VERSION_NAME),
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showConfirmation = true },
            enabled = !isSigningOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        ) {
            if (isSigningOut) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.iam_settings_sign_out))
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text(stringResource(R.string.iam_settings_sign_out_title)) },
            text = { Text(stringResource(R.string.iam_settings_sign_out_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmation = false
                        onSignOut()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.iam_settings_sign_out),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text(stringResource(R.string.iam_settings_cancel))
                }
            },
        )
    }
}

@Composable
private fun AccountCard(profile: UserProfile) {
    val displayName = profile.fullName.ifBlank { profile.email }
    val initial = displayName.trim().firstOrNull()?.uppercase() ?: "?"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = profile.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            InfoRow(
                label = stringResource(R.string.iam_settings_role),
                value = stringResource(
                    if (profile.role == UserRole.ADMIN) R.string.iam_role_admin else R.string.iam_role_viewer,
                ),
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun SettingsContentPreview() {
    MachineGuardTheme {
        SettingsContent(
            profile = UserProfile(
                id = "00000000-0000-0000-0000-000000000001",
                organizationId = "00000000-0000-0000-0000-000000000000",
                organizationName = "Demo",
                email = "demo@machineguard.com",
                fullName = "Demo Warehouse Manager",
                role = UserRole.ADMIN,
                lastLoginAt = null,
            ),
            isSigningOut = false,
            onSignOut = {},
        )
    }
}
