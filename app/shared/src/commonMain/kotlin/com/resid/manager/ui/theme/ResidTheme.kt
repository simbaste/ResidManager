package com.resid.manager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
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

// Luxury Estate Custom Theme Extension Tokens
data class ResidCustomColors(
    val cardBackground: Color,
    val cardBorder: Color,
    val inputBackground: Color,
    val inputBorder: Color,
    val dividerColor: Color,
    val ambientGlowAlpha: Float,
    val secondaryGlowAlpha: Float,
    val gridAlpha: Float,
    val cardShadowElevation: Float
)

val LocalResidColors = staticCompositionLocalOf {
    ResidCustomColors(
        cardBackground = Color(0xF2031427),
        cardBorder = Color(0x593D4A42),
        inputBackground = Color(0x8026364A),
        inputBorder = Color(0x7387948B),
        dividerColor = Color(0x4D3D4A42),
        ambientGlowAlpha = 0.22f,
        secondaryGlowAlpha = 0.18f,
        gridAlpha = 0.035f,
        cardShadowElevation = 0.6f
    )
}

val MaterialTheme.residColors: ResidCustomColors
    @Composable
    @ReadOnlyComposable
    get() = LocalResidColors.current

private val LightResidCustomColors = ResidCustomColors(
    cardBackground = Color(0xF5FFFFFF),
    cardBorder = Color(0xA6CBD5E1),
    inputBackground = Color(0x66E2E8F0),
    inputBorder = Color(0xA694A3B8),
    dividerColor = Color(0x80CBD5E1),
    ambientGlowAlpha = 0.12f,
    secondaryGlowAlpha = 0.10f,
    gridAlpha = 0.025f,
    cardShadowElevation = 0.15f
)

private val DarkResidCustomColors = ResidCustomColors(
    cardBackground = Color(0xE0031427),
    cardBorder = Color(0x593D4A42),
    inputBackground = Color(0x8026364A),
    inputBorder = Color(0x7387948B),
    dividerColor = Color(0x4D3D4A42),
    ambientGlowAlpha = 0.22f,
    secondaryGlowAlpha = 0.18f,
    gridAlpha = 0.035f,
    cardShadowElevation = 0.6f
)

@Composable
fun ResidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val customColors = if (darkTheme) DarkResidCustomColors else LightResidCustomColors

    CompositionLocalProvider(LocalResidColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
