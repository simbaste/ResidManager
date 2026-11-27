package com.resid.manager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.resid.manager.AppEnvironment
import com.resid.manager.getAppEnvironment

/**
 * Affiche deux barres obliques discrètes et élégantes dans le coin supérieur gauche
 * pour identifier immédiatement l'environnement courant :
 * - DEV (Local) : Rouge (#EF4444)
 * - TEST (Staging / Develop) : Violet (#8B5CF6)
 * - PROD : Rien
 */
@Composable
fun EnvironmentCornerRibbon(
    modifier: Modifier = Modifier,
    environment: AppEnvironment = getAppEnvironment()
) {
    val stripeColor = when (environment) {
        AppEnvironment.DEV -> Color(0xFFEF4444) // Rouge
        AppEnvironment.TEST -> Color(0xFF8B5CF6) // Violet
        AppEnvironment.PROD -> return
    }

    Canvas(modifier = modifier.size(28.dp)) {
        val strokeWidth = 3.5.dp.toPx()

        // 1ère barre oblique (la plus proche du coin)
        drawLine(
            color = stripeColor,
            start = Offset(0f, 10.dp.toPx()),
            end = Offset(10.dp.toPx(), 0f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // 2ème barre oblique (parallèle, légèrement plus bas)
        drawLine(
            color = stripeColor,
            start = Offset(0f, 20.dp.toPx()),
            end = Offset(20.dp.toPx(), 0f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
