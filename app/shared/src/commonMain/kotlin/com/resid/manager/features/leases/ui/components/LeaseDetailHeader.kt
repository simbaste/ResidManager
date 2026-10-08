package com.resid.manager.features.leases.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.LeaseStatusDto

@Composable
fun LeaseDetailHeader(
    lease: LeaseDto,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeColor, badgeText) = when (lease.status) {
        LeaseStatusDto.SIGNED_ACTIVE -> Triple(Color(0xFFE6F7F0), Color(0xFF006948), "ACTIF / LOGEMENT OCCUPÉ")
        LeaseStatusDto.PENDING_SIGNATURE -> Triple(Color(0xFFE0E7FF), Color(0xFF1E3A8A), "ATTENTE SIGNATURE")
        LeaseStatusDto.DOWN_PAYMENT_PAID, LeaseStatusDto.PARTIALLY_PAID -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "ACOMPTE ENREGISTRÉ (PARTIAL)")
        LeaseStatusDto.TERMINATED -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "TERMINÉ")
        else -> Triple(Color(0xFFFDE8E8), Color(0xFFBA1A1A), "ATTENTE DE VERSEMENT")
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBackClick) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color(0xFF006948),
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Retour à la liste", color = Color(0xFF006948))
                }
            }
            Text(text = "Détails du Contrat de Bail", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = badgeBg),
            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(9999.dp)
        ) {
            Text(text = badgeText, style = MaterialTheme.typography.bodyMedium, color = badgeColor, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}
