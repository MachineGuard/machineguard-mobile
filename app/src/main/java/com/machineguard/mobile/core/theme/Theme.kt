package com.machineguard.mobile.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = MgColors.Primary,
    onPrimary = Color.White,
    primaryContainer = MgColors.PrimaryContainer,
    onPrimaryContainer = MgColors.NightNavy,
    secondary = MgColors.Ok,
    tertiary = MgColors.Warn,
    error = MgColors.Error,
    background = MgColors.Cloud,
    onBackground = MgColors.NightNavy,
    surface = Color.White,
    onSurface = MgColors.NightNavy,
    onSurfaceVariant = MgColors.Slate,
)

private val DarkColors = darkColorScheme(
    primary = MgColors.PrimaryContainer,
    onPrimary = MgColors.NightNavy,
    primaryContainer = MgColors.Primary,
    onPrimaryContainer = Color.White,
    secondary = MgColors.Ok,
    tertiary = MgColors.Warn,
    error = MgColors.Error,
    background = MgColors.NightNavy,
    onBackground = Color.White,
    surface = MgColors.NightNavy,
    onSurface = Color.White,
)

@Composable
fun MachineGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
