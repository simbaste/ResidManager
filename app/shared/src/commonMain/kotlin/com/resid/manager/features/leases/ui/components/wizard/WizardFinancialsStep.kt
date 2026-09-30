package com.resid.manager.features.leases.ui.components.wizard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceUnitDto

@Composable
fun WizardFinancialsStep(
    selectedResidenceUnit: ResidenceUnitDto?,
    draftDepositAmount: String,
    draftPaymentFrequency: String,
    draftAdvanceMonths: String,
    draftAdvancePaymentAmount: String,
    draftPaymentMethod: String,
    onDepositAmountChanged: (String) -> Unit,
    onPaymentFrequencyChanged: (String) -> Unit,
    onAdvanceMonthsChanged: (String) -> Unit,
    onAdvancePaymentAmountChanged: (String) -> Unit,
    onPaymentMethodChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAnnual = draftPaymentFrequency == "ANNUAL"

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Étape 3 sur 4 : Conditions Financières", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        OutlinedTextField(
            value = draftDepositAmount,
            onValueChange = onDepositAmountChanged,
            label = { Text("Montant du dépôt de garantie (Caution) *") },
            isError = (draftDepositAmount.toDoubleOrNull() ?: 0.0) < 0.0,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Fréquence de paiement :")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { onPaymentFrequencyChanged("MONTHLY") },
                colors = ButtonDefaults.buttonColors(containerColor = if (draftPaymentFrequency == "MONTHLY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Mensuelle (MONTHLY)")
            }
            Button(
                onClick = { onPaymentFrequencyChanged("ANNUAL") },
                colors = ButtonDefaults.buttonColors(containerColor = if (draftPaymentFrequency == "ANNUAL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Annuelle (ANNUAL)")
            }
        }

        if (isAnnual) {
            OutlinedTextField(
                value = draftAdvanceMonths,
                onValueChange = onAdvanceMonthsChanged,
                label = { Text("Nombre de mois d'avance payés (Défaut : 12) *") },
                isError = (draftAdvanceMonths.toIntOrNull() ?: 0) <= 0,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = draftAdvancePaymentAmount,
            onValueChange = onAdvancePaymentAmountChanged,
            label = { Text("Montant de l'acompte immédiat versé (XOF)") },
            isError = (draftAdvancePaymentAmount.toDoubleOrNull() ?: 0.0) < 0.0,
            modifier = Modifier.fillMaxWidth()
        )

        val deposit = draftDepositAmount.toDoubleOrNull() ?: 0.0
        val acompteVal = draftAdvancePaymentAmount.toDoubleOrNull() ?: 0.0

        if (acompteVal > 0.0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Mode de paiement de l'acompte :", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onPaymentMethodChanged("CASH") },
                    colors = ButtonDefaults.buttonColors(containerColor = if (draftPaymentMethod == "CASH") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Espèces")
                }
                Button(
                    onClick = { onPaymentMethodChanged("TRANSFER") },
                    colors = ButtonDefaults.buttonColors(containerColor = if (draftPaymentMethod == "TRANSFER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Virement")
                }
                Button(
                    onClick = { onPaymentMethodChanged("MOBILE") },
                    colors = ButtonDefaults.buttonColors(containerColor = if (draftPaymentMethod == "MOBILE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Mobile Money")
                }
            }
        }

        if (selectedResidenceUnit != null) {
            val rent = selectedResidenceUnit.nominalRent
            val charges = selectedResidenceUnit.serviceCharges

            val months = if (isAnnual) (draftAdvanceMonths.toIntOrNull() ?: 12) else 1
            val firstRent = months * (rent + charges)

            val totalRequiredToPay = deposit + firstRent
            val remainingToPay = totalRequiredToPay - acompteVal

            val (probadgeBg, probadgeColor, probadgeText) = when {
                acompteVal <= 0.0 -> Triple(Color(0xFFFDE8E8), Color(0xFFBA1A1A), "[État cible : PENDING_PAYMENT] (Attente)")
                acompteVal < totalRequiredToPay -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "[État cible : PARTIALLY_PAID] (Acompte)")
                else -> Triple(Color(0xFFE0E7FF), Color(0xFF1E3A8A), "[État cible : PENDING_SIGNATURE] (Solder / Prêt à signer)")
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calculateur de Cycle de Vie :", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = probadgeBg),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, probadgeColor.copy(alpha = 0.2f))
                        ) {
                            Text(text = probadgeText, style = MaterialTheme.typography.bodySmall, color = probadgeColor, modifier = Modifier.padding(6.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Loyer mensuel :", style = MaterialTheme.typography.bodyMedium)
                        Text("$rent XOF", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Charges fixes d'entretien :", style = MaterialTheme.typography.bodyMedium)
                        Text("$charges XOF", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Dépôt de garantie (Caution) :", style = MaterialTheme.typography.bodyMedium)
                        Text("$deposit XOF", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = if (!isAnnual) "Premier loyer échu (1 mois) :" else "Versement d'avance échu ($months mois) :",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("$firstRent XOF", style = MaterialTheme.typography.bodyMedium)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL REQUIS (Caution + Loyers) :", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("$totalRequiredToPay XOF", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Acompte versé :", style = MaterialTheme.typography.bodyMedium)
                        Text("$acompteVal XOF", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("RESTE À PAYER :", style = MaterialTheme.typography.titleMedium, color = if (remainingToPay > 0.0) Color(0xFFBA1A1A) else Color(0xFF006948))
                        Text("${if (remainingToPay > 0.0) remainingToPay else 0.0} XOF", style = MaterialTheme.typography.titleMedium, color = if (remainingToPay > 0.0) Color(0xFFBA1A1A) else Color(0xFF006948))
                    }
                }
            }
        }
    }
}
