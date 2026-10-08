package com.resid.manager.features.leases.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.LeaseStatusDto
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.ResidenceUnitDto

@Composable
fun LeaseCard(
    lease: LeaseDto,
    matchedUnit: ResidenceUnitDto?,
    matchedTenant: ResidenceMemberSummaryDto?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matchedResidenceUnit = matchedUnit?.name ?: "Logement ${lease.residenceUnitId.take(5)}"
    val tenantName = matchedTenant?.let { "${it.firstName} ${it.lastName}" } ?: "Inconnu"

    val (badgeBg, badgeColor, badgeText) = when (lease.status) {
        LeaseStatusDto.SIGNED_ACTIVE -> Triple(Color(0xFFE0E7FF), Color(0xFF1E3A8A), "EN COURS")
        LeaseStatusDto.PENDING_SIGNATURE -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "EN ATTENTE SIGNATURE")
        LeaseStatusDto.PENDING_PAYMENT -> Triple(Color(0xFFFDE8E8), Color(0xFFBA1A1A), "EN ATTENTE PAIEMENT")
        LeaseStatusDto.DOWN_PAYMENT_PAID, LeaseStatusDto.PARTIALLY_PAID -> Triple(Color(0xFFE6F7F0), Color(0xFF006948), "ACOMPTE ENREGISTRÉ")
        LeaseStatusDto.TERMINATED -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "TERMINÉ")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = matchedResidenceUnit,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF006948)
                    )
                    Text(
                        text = "Locataire: $tenantName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = badgeBg),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Key-Value Rows
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LeaseInfoRow(label = "Du :", value = lease.startDate)
                LeaseInfoRow(label = "Au :", value = lease.endDate)
                LeaseInfoRow(label = "Loyer mensuel :", value = "${lease.monthlyRentAtSign} XOF", isGreen = true)
                LeaseInfoRow(label = "Caution :", value = "${lease.depositAmount} XOF")
            }
        }
    }
}

@Composable
fun LeaseInfoRow(
    label: String,
    value: String,
    isGreen: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isGreen) Color(0xFF006948) else MaterialTheme.colorScheme.onSurface
        )
    }
}
