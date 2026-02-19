package com.volumecontrol.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6DB3FF),        // Lighter blue for dark mode
    onPrimary = Color(0xFF003584),
    primaryContainer = Color(0xFF004BAD),
    onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = Color(0xFF00D9B8),
    onSecondary = Color(0xFF003D35),
    secondaryContainer = Color(0xFF00574F),
    onSecondaryContainer = Color(0xFFB1EFDA),
    tertiary = Color(0xFF52E5D4),
    onTertiary = Color(0xFF003D38),
    tertiaryContainer = Color(0xFF005652),
    onTertiaryContainer = Color(0xFFA2F4E9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0A0E1A),
    onBackground = Color(0xFFF0F1FF),
    surface = Color(0xFF0A0E1A),
    onSurface = Color(0xFFF0F1FF),
    surfaceVariant = Color(0xFF48525F),
    onSurfaceVariant = Color(0xFFC8D1DB),
    outlineVariant = Color(0xFF48525F),
)

@Composable
fun VolumeControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
