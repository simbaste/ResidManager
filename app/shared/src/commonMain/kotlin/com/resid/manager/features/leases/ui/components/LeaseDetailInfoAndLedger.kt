package com.resid.manager.features.leases.ui.components

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
import com.resid.manager.dto.LeaseCategory
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.ResidenceUnitDto

@Composable
fun LeaseDetailInfoAndLedger(
    lease: LeaseDto,
    matchedUnit: ResidenceUnitDto?,
    matchedTenant: ResidenceMemberSummaryDto?,
    modifier: Modifier = Modifier
) {
    val matchedResidenceUnitName = matchedUnit?.name ?: "Logement ${lease.residenceUnitId.take(5)}"

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Fiche d'Informations Card
        Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Fiche d'Informations", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Unité rattachée : $matchedResidenceUnitName", style = MaterialTheme.typography.bodyLarge)
                    Text("Étage : ${matchedUnit?.floor ?: "Non spécifié"}", style = MaterialTheme.typography.bodyMedium)
                    Text("Type d'unité : ${matchedUnit?.type ?: "Non spécifié"}", style = MaterialTheme.typography.bodyMedium)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    if (matchedTenant != null) {
                        Text("Locataire : ${matchedTenant.firstName} ${matchedTenant.lastName}", style = MaterialTheme.typography.bodyLarge)
                        Text("Email : ${matchedTenant.email}", style = MaterialTheme.typography.bodyMedium)
                        Text("Téléphone : ${matchedTenant.phone ?: "Non spécifié"}", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Locataire (ID) : ${lease.tenantId}", style = MaterialTheme.typography.bodyLarge)
                    }

                    Text("Date de début : ${lease.startDate}", style = MaterialTheme.typography.bodyMedium)
                    Text("Date de fin : ${lease.endDate}", style = MaterialTheme.typography.bodyMedium)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text("Loyer de base : ${lease.monthlyRentAtSign} XOF / mois", style = MaterialTheme.typography.bodyLarge)
                    if ((matchedUnit?.serviceCharges ?: 0.0) > 0.0) {
                        Text("Charges fixes d'entretien : ${matchedUnit?.serviceCharges} XOF / mois", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("Caution requise : ${lease.depositAmount} XOF", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // Payments History Ledger Card
        Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Historique des Règlements", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider()

                if (lease.payments.isEmpty()) {
                    Text(
                        text = "Aucun versement n'a encore été enregistré pour ce bail.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        lease.payments.forEach { pay ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(pay.description, style = MaterialTheme.typography.bodyMedium)
                                    Text(pay.transactionDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (pay.category == LeaseCategory.DEPOSIT) Color(0xFFFEF3C7) else Color(0xFFE0E7FF)
                                        ),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = pay.category.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (pay.category == LeaseCategory.DEPOSIT) Color(0xFFD97706) else Color(0xFF1E3A8A),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "${pay.amount} XOF",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFF006948)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}
