package com.resid.manager.features.leases.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.LeaseCategory
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.LeaseStatusDto
import com.resid.manager.dto.PaymentFrequencyDto
import com.resid.manager.dto.ResidenceUnitDto

@Composable
fun LeaseDetailAdminActions(
    lease: LeaseDto,
    matchedUnit: ResidenceUnitDto?,
    onRecordPayment: (Double, String) -> Unit,
    onSignContract: () -> Unit,
    onTerminateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var paymentAmountText by remember { mutableStateOf("") }
    var paymentCategorySelected by remember { mutableStateOf(if (lease.depositAmount > 0.0) "CAUTION" else "LOYER") }
    var localError by remember { mutableStateOf<String?>(null) }

    val isTerminated = lease.status == LeaseStatusDto.TERMINATED
    val isLocked = lease.status == LeaseStatusDto.SIGNED_ACTIVE

    val isAnnual = lease.paymentFrequencyDto == PaymentFrequencyDto.ANNUAL
    val rentMonthsRequired = if (isAnnual) 12 else lease.advanceMonths
    val monthlyCharges = matchedUnit?.serviceCharges ?: 0.0
    val totalRequiredRent = rentMonthsRequired * (lease.monthlyRentAtSign + monthlyCharges)

    val totalPaidCaution = lease.payments.filter { it.category == LeaseCategory.DEPOSIT }.sumOf { it.amount }
    val totalPaidRent = lease.payments.filter { it.category == LeaseCategory.RENT }.sumOf { it.amount }

    val remainingCaution = maxOf(0.0, lease.depositAmount - totalPaidCaution)
    val remainingRent = maxOf(0.0, totalRequiredRent - totalPaidRent)
    val totalRemaining = remainingCaution + remainingRent

    val isFullyPaid = isLocked || isTerminated || totalRemaining <= 0.0
    val isReadyToSign = !isLocked && !isTerminated && totalRemaining <= 0.0

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Actions d'Administration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider()

            if (isTerminated) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    border = BorderStroke(1.dp, Color(0xFF475569).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("✕ CONTRAT RÉSILIÉ / CLÔTURÉ", style = MaterialTheme.typography.titleSmall, color = Color(0xFF475569))
                        Text("Ce contrat de bail est définitivement clos. Le logement rattaché est repassé automatiquement à l'état disponible pour une nouvelle location.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                    }
                }
            } else if (isLocked) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F7F0)),
                    border = BorderStroke(1.dp, Color(0xFF006948).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("✓ CONTRAT VERROUILLÉ / ACTIF", style = MaterialTheme.typography.titleSmall, color = Color(0xFF006948))
                        Text("Ce bail est actuellement actif. Le logement associé passe automatiquement à l'état OCCUPIED. Les modifications financières et de caution sont closes.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF006948))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onTerminateClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = Color.White)
                        Text("Résilier / Clôturer le Contrat", color = Color.White)
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFullyPaid) Color(0xFFE0E7FF) else Color(0xFFFDE8E8)
                    ),
                    border = BorderStroke(1.dp, (if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A)).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isFullyPaid) "Exigences Financières Soldées" else "Reste à verser avant signature",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A)
                        )
                        Text("Caution payée : $totalPaidCaution / ${lease.depositAmount} XOF (Reste: $remainingCaution XOF)", style = MaterialTheme.typography.bodySmall)
                        Text("Loyers initiaux payés : $totalPaidRent / $totalRequiredRent XOF (Reste: $remainingRent XOF)", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = if (totalRemaining <= 0.0) "Total soldé ! Prêt pour signature." else "Reliquat total restant : $totalRemaining XOF",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (totalRemaining <= 0.0) Color(0xFF006948) else Color(0xFFBA1A1A)
                        )
                    }
                }

                // Enregistrement d'un versement
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enregistrer un versement financier :", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = paymentCategorySelected == "CAUTION",
                            onClick = { paymentCategorySelected = "CAUTION" },
                            label = { Text("Caution") }
                        )
                        FilterChip(
                            selected = paymentCategorySelected == "LOYER",
                            onClick = { paymentCategorySelected = "LOYER" },
                            label = { Text("Loyer d'avance") }
                        )
                    }

                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it },
                        label = { Text("Montant reçu (XOF)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val amount = paymentAmountText.toDoubleOrNull()
                            if (amount == null || amount <= 0.0) {
                                localError = "Veuillez saisir un montant numérique valide."
                            } else {
                                localError = null
                                onRecordPayment(amount, paymentCategorySelected)
                                paymentAmountText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("Enregistrer le Versement", color = Color.White)
                    }

                    if (localError != null) {
                        Text(text = localError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                HorizontalDivider()

                Button(
                    onClick = onSignContract,
                    enabled = isReadyToSign,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReadyToSign) Color(0xFF006948) else Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Text(
                            text = if (isReadyToSign) "Signer & Valider le Contrat (Actif)" else "Signature verrouillée (Solde requis)",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
