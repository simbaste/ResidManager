package com.resid.manager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Scheme based on Emerald & Slate values, tuned for professional high-contrast SaaS
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006C4A), // inverse-primary from DESIGN.md as base primary for light mode
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF85F8C4), // primary-fixed from DESIGN.md
    onPrimaryContainer = Color(0xFF002114), // on-primary-fixed
    secondary = Color(0xFF3F465C), // secondary-container
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE2FD), // secondary-fixed
    onSecondaryContainer = Color(0xFF131B2E), // on-secondary-fixed
    surface = Color(0xFFF8FAFC), // Slate-based clear neutral
    onSurface = Color(0xFF0F172A), // Slate-900 background value
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

// Dark Scheme: The exact "Emerald Estate" Design tokens from DESIGN.md
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF68DBA9), // primary
    onPrimary = Color(0xFF003825), // on-primary
    primaryContainer = Color(0xFF25A475), // primary-container
    onPrimaryContainer = Color(0xFF00311F), // on-primary-container
    secondary = Color(0xFFBEC6E0), // secondary
    onSecondary = Color(0xFF283044), // on-secondary
    secondaryContainer = Color(0xFF3F465C), // secondary-container
    onSecondaryContainer = Color(0xFFADB4CE), // on-secondary-container
    tertiary = Color(0xFFC4C7C9), // tertiary
    onTertiary = Color(0xFF2D3133), // on-tertiary
    tertiaryContainer = Color(0xFF8E9193), // tertiary-container
    onTertiaryContainer = Color(0xFF272A2C), // on-tertiary-container
    surface = Color(0xFF031427), // surface / background from DESIGN.md
    onSurface = Color(0xFFD3E4FE), // on-surface
    surfaceVariant = Color(0xFF26364A), // surface-variant / container
    onSurfaceVariant = Color(0xFFBCCAC0), // on-surface-variant
    background = Color(0xFF031427), // background from DESIGN.md
    onBackground = Color(0xFFD3E4FE), // on-background
    outline = Color(0xFF87948B), // outline
    outlineVariant = Color(0xFF3D4A42), // outline-variant
    error = Color(0xFFFFB4AB), // error
    onError = Color(0xFF690005), // on-error
    errorContainer = Color(0xFF93000A), // error-container
    onErrorContainer = Color(0xFFFFDAD6) // on-error-container
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
