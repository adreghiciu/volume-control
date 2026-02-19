package com.volumecontrol.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1F7FFF),        // Vibrant blue matching system volume slider
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9FF),
    onPrimaryContainer = Color(0xFF001B47),
    secondary = Color(0xFF03DAC5),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFB1EFDA),
    onSecondaryContainer = Color(0xFF004D3F),
    tertiary = Color(0xFF0DB3A0),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA2F4E9),
    onTertiaryContainer = Color(0xFF00201D),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFAFCFF),
    onBackground = Color(0xFF0A1428),
    surface = Color(0xFFFAFCFF),
    onSurface = Color(0xFF0A1428),
    surfaceVariant = Color(0xFFEAEEF7),
    onSurfaceVariant = Color(0xFF48525F),
    outlineVariant = Color(0xFFC8D1DB),
)

@Composable
fun VolumeControlTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
