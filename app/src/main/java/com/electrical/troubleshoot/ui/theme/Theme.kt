package com.electrical.troubleshoot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF1565C0)
private val PrimaryDark = Color(0xFF0D47A1)
private val Secondary = Color(0xFF00897B)
private val Error = Color(0xFFD32F2F)
private val Success = Color(0xFF2E7D32)
private val Warning = Color(0xFFF57C00)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    secondary = Secondary,
    onSecondary = Color.White,
    error = Error,
    onError = Color.White,
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE8EEF4)
)

@Composable
fun ElectricalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content
    )
}

object AppColors {
    val Success = Success
    val Warning = Warning
    val Error = Error
    val Primary = Primary
}
