package com.resid.manager.features.dashboard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DashboardDelinquencyAlert(
    delinquencyRate: Double,
    modifier: Modifier = Modifier
) {
    if (delinquencyRate > 10.0) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8)),
            border = BorderStroke(1.dp, Color(0xFFBA1A1A).copy(alpha = 0.3f)),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFBA1A1A),
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    val roundedDelinquency = (delinquencyRate * 10.0).toInt() / 10.0
                    Text(
                        text = "Alerte Délinquance Critique : Taux d'Impayés à $roundedDelinquency%",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFBA1A1A)
                    )
                    Text(
                        text = "Le taux d'impayés dépasse le seuil critique de 10%. Veuillez relancer les locataires débiteurs immédiatement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFBA1A1A)
                    )
                }
            }
        }
    }
}
