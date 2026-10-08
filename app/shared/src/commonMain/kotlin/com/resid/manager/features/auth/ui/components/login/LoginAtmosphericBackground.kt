package com.resid.manager.features.auth.ui.components.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resid.manager.ui.theme.residColors

/**
 * Atmospheric background canvas drawing luxury glow orbs and subtle grid pattern.
 */
@Composable
fun LoginAtmosphericBackground(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val residColors = MaterialTheme.residColors
    val brandPrimary = colorScheme.primary
    val brandPrimaryContainer = colorScheme.primaryContainer
    val textColorPrimary = colorScheme.onSurface

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Top-left primary glow orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(brandPrimary.copy(alpha = residColors.ambientGlowAlpha), Color.Transparent),
                center = Offset(w * 0.05f, h * 0.05f),
                radius = (w * 0.55f).coerceAtLeast(350f)
            ),
            radius = (w * 0.55f).coerceAtLeast(350f),
            center = Offset(w * 0.05f, h * 0.05f)
        )

        // Bottom-right primary glow orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(brandPrimaryContainer.copy(alpha = residColors.secondaryGlowAlpha), Color.Transparent),
                center = Offset(w * 0.95f, h * 0.95f),
                radius = (w * 0.6f).coerceAtLeast(400f)
            ),
            radius = (w * 0.6f).coerceAtLeast(400f),
            center = Offset(w * 0.95f, h * 0.95f)
        )

        // Center-right subtle ambient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorScheme.secondary.copy(alpha = residColors.secondaryGlowAlpha * 0.7f), Color.Transparent),
                center = Offset(w * 0.8f, h * 0.45f),
                radius = (w * 0.4f).coerceAtLeast(250f)
            ),
            radius = (w * 0.4f).coerceAtLeast(250f),
            center = Offset(w * 0.8f, h * 0.45f)
        )

        // Subtle Architectural Grid Pattern
        val spacing = 36.dp.toPx()
        var x = 0f
        while (x < w) {
            var y = 0f
            while (y < h) {
                drawCircle(
                    color = textColorPrimary.copy(alpha = residColors.gridAlpha),
                    radius = 1.2f,
                    center = Offset(x, y)
                )
                y += spacing
            }
            x += spacing
        }
    }
}
