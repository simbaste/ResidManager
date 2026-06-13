package com.resid.manager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00664F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9DF2D4),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF4B635A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCEE9DD),
    onSecondaryContainer = Color(0xFF082018),
    surface = Color(0xFFFBFDF9),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDBE5E0),
    onSurfaceVariant = Color(0xFF3F4945),
    background = Color(0xFFFBFDF9),
    onBackground = Color(0xFF191C1B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81D5B9),
    onPrimary = Color(0xFF003829),
    primaryContainer = Color(0xFF004F3B),
    onPrimaryContainer = Color(0xFF9DF2D4),
    secondary = Color(0xFFB2CCC1),
    onSecondary = Color(0xFF1D352D),
    secondaryContainer = Color(0xFF334C43),
    onSecondaryContainer = Color(0xFFCEE9DD),
    surface = Color(0xFF191C1B),
    onSurface = Color(0xFFE1E3E0),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFDBE5E0),
    background = Color(0xFF191C1B),
    onBackground = Color(0xFFE1E3E0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun ResidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
