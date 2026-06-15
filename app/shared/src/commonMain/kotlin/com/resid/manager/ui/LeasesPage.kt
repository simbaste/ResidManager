package com.resid.manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.resid.manager.dto.*
import com.resid.manager.network.ApiClient
import com.resid.manager.ui.i18n.LocalStrings
import com.resid.manager.viewmodel.LoginViewModel
import io.ktor.client.request.*
import io.ktor.client.call.body
import kotlinx.coroutines.launch

@Composable
fun LeasesPage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activeResidence = uiState.selectedResidenceContext
    val isAuthorized = activeResidence != null && (activeResidence.userRoleInResidence == UserRole.ADMIN || activeResidence.userRoleInResidence == UserRole.RESIDENCE_MANAGER)

    var showWizard by remember { mutableStateOf(false) }
    var selectedLeaseForDetail by remember { mutableStateOf<LeaseDto?>(null) }

    if (selectedLeaseForDetail != null) {
        val lease = selectedLeaseForDetail!!
        val matchedLogement = uiState.logements.firstOrNull { it.id == lease.logementId }
        val matchedLogementName = matchedLogement?.name ?: "Logement ${lease.logementId.take(5)}"
        
        var paymentAmountText by remember { mutableStateOf("") }
        var localError by remember { mutableStateOf<String?>(null) }
        var paymentCategorySelected by remember { mutableStateOf(if (lease.depositAmount > 0.0) "CAUTION" else "LOYER") }
        var showTerminateConfirmation by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { selectedLeaseForDetail = null }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null,
                                tint = Color(0xFF006948),
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Retour à la liste")
                        }
                    }
                    Text(text = "Détails du Contrat de Bail", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }

                // Header status badge
                val (badgeBg, badgeColor, badgeText) = when (lease.status) {
                    LeaseStatus.SIGNED_ACTIVE -> Triple(Color(0xFFE6F7F0), Color(0xFF006948), "ACTIF / LOGEMENT OCCUPÉ")
                    LeaseStatus.PENDING_SIGNATURE -> Triple(Color(0xFFE0E7FF), Color(0xFF1E3A8A), "ATTENTE SIGNATURE")
                    LeaseStatus.DOWN_PAYMENT_PAID -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "ACOMPTE ENREGISTRÉ (PARTIAL)")
                    else -> Triple(Color(0xFFFDE8E8), Color(0xFFBA1A1A), "ATTENTE DE VERSEMENT")
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = badgeBg),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(9999.dp)
                ) {
                    Text(text = badgeText, style = MaterialTheme.typography.bodyMedium, color = badgeColor, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Column: Lease Details & Payments History
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Fiche d'Informations", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            HorizontalDivider()

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Unité rattachée : $matchedLogementName", style = MaterialTheme.typography.bodyLarge)
                                Text("Étage : ${matchedLogement?.floor ?: "Non spécifié"}", style = MaterialTheme.typography.bodyMedium)
                                Text("Type d'unité : ${matchedLogement?.type ?: "Non spécifié"}", style = MaterialTheme.typography.bodyMedium)
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                val matchedTenant = uiState.members.firstOrNull { it.userId == lease.tenantId }
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
                                Text("Caution requise : ${lease.depositAmount} XOF", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    // Payments History Ledger Card
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
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
                                                        containerColor = if (pay.category == "CAUTION") Color(0xFFFEF3C7) else Color(0xFFE0E7FF)
                                                    ),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = pay.category, 
                                                        style = MaterialTheme.typography.labelSmall, 
                                                        color = if (pay.category == "CAUTION") Color(0xFFD97706) else Color(0xFF1E3A8A),
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

                // Right Column: Payments & State Machine Signing Actions
                Card(
                    modifier = Modifier.weight(0.8f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Actions d'Administration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider()

                        val isTerminated = lease.status == LeaseStatus.TERMINATED
                        val isLocked = lease.status == LeaseStatus.SIGNED_ACTIVE
                        val isCautionExempt = lease.depositAmount == 0.0

                        val isAnnual = lease.paymentFrequency == "ANNUAL"
                        val rentMonthsRequired = if (isAnnual) 12 else lease.advanceMonths
                        val totalRequiredRent = rentMonthsRequired * lease.monthlyRentAtSign

                        val totalPaidCaution = lease.payments.filter { it.category == "CAUTION" }.sumOf { it.amount }
                        val totalPaidRent = lease.payments.filter { it.category == "LOYER" }.sumOf { it.amount }

                        val remainingCaution = maxOf(0.0, lease.depositAmount - totalPaidCaution)
                        val remainingRent = maxOf(0.0, totalRequiredRent - totalPaidRent)
                        val totalRemaining = remainingCaution + remainingRent

                        val isFullyPaid = lease.status == LeaseStatus.PENDING_SIGNATURE || lease.status == LeaseStatus.SIGNED_ACTIVE || lease.status == LeaseStatus.TERMINATED || totalRemaining <= 0.0
                        val isReadyToSign = lease.status == LeaseStatus.PENDING_SIGNATURE || totalRemaining <= 0.0

                        // Display contract locked or current state progress
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
                            
                            // Terminate contract button
                            Button(
                                onClick = { showTerminateConfirmation = true },
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
                            // Ledger explanation card
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isFullyPaid) Color(0xFFE0E7FF) else Color(0xFFFDE8E8)
                                ),
                                border = BorderStroke(1.dp, (if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A)).copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (isFullyPaid) "✓ TOUTES LES SOMMES SONT RÉGLÉES" else "⚠ VERSEMENT DE SOLDE ATTENDU",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A)
                                    )
                                    HorizontalDivider(color = (if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A)).copy(alpha = 0.2f))
                                    
                                    // Caution breakdown
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Dépôt de Garantie (Caution) :", style = MaterialTheme.typography.titleSmall, color = if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("• Requis :", style = MaterialTheme.typography.bodySmall, color = if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A))
                                            Text(if (isCautionExempt) "0 XOF" else "${lease.depositAmount} XOF", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("• Réglé :", style = MaterialTheme.typography.bodySmall, color = if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A))
                                            Text("$totalPaidCaution XOF", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("• Restant :", style = MaterialTheme.typography.bodySmall, color = if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A))
                                            Text("$remainingCaution XOF", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    HorizontalDivider(color = (if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A)).copy(alpha = 0.1f))

                                    // Rent breakdown
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Loyer d'Avance (${lease.advanceMonths} mois) :", style = MaterialTheme.typography.titleSmall, color = if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("• Requis :", style = MaterialTheme.typography.bodySmall, color = if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A))
                                            Text("$totalRequiredRent XOF", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("• Réglé :", style = MaterialTheme.typography.bodySmall, color = if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A))
                                            Text("$totalPaidRent XOF", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("• Restant :", style = MaterialTheme.typography.bodySmall, color = if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A))
                                            Text("$remainingRent XOF", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    HorizontalDivider(color = (if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A)).copy(alpha = 0.2f))

                                    // Grand Total Remaining
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("RESTE À VERSER AVANT SIGNATURE :", style = MaterialTheme.typography.titleMedium, color = if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A))
                                        Text("$totalRemaining XOF", style = MaterialTheme.typography.titleLarge, color = if (isFullyPaid) Color(0xFF1E3A8A) else Color(0xFFBA1A1A))
                                    }
                                }
                            }
                        }

                        // Rule 2.1: If PENDING_PAYMENT or PARTIALLY_PAID, display Saisir règlement allowing dynamic category selection
                        if (!isFullyPaid && !isLocked && !isTerminated && isAuthorized) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Sélectionnez la catégorie à créditer :", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (!isCautionExempt) {
                                        Button(
                                            onClick = { paymentCategorySelected = "CAUTION" },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (paymentCategorySelected == "CAUTION") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Text("Dépôt de Garantie (Caution)")
                                        }
                                    }
                                    Button(
                                        onClick = { paymentCategorySelected = "LOYER" },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (paymentCategorySelected == "LOYER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text("Loyer / Avance")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = paymentAmountText,
                                        onValueChange = { paymentAmountText = it },
                                        label = { Text("Montant du versement (XOF)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            val amount = paymentAmountText.toDoubleOrNull()
                                            if (amount == null || amount <= 0.0) {
                                                localError = "Veuillez saisir un montant valide."
                                                return@Button
                                            }
                                            viewModel.recordLeasePayment(lease.id, amount, paymentCategorySelected) { res ->
                                                res.onSuccess {
                                                    selectedLeaseForDetail = it
                                                    paymentAmountText = ""
                                                    localError = null
                                                }.onFailure { ex ->
                                                    localError = ex.message
                                                }
                                            }
                                        },
                                        enabled = !uiState.isLoading
                                    ) {
                                        Text("Enregistrer")
                                    }
                                }
                            }
                        }

                        // Rule 2.2: If ready to sign (all sums paid), show sign contract
                        if (isReadyToSign && !isLocked && !isTerminated && isAuthorized) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = "L'ensemble des sommes (caution et loyers exigibles) a été réglé avec succès. Le contrat de bail est prêt à être signé et activé :",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = {
                                        viewModel.updateLeaseStatus(lease.id, LeaseStatus.SIGNED_ACTIVE) { res ->
                                            res.onSuccess {
                                                selectedLeaseForDetail = it
                                                localError = null
                                            }.onFailure { ex ->
                                                localError = ex.message
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("✎ Valider la Signature du Contrat", color = Color.White)
                                }
                            }
                        }

                        val error = localError ?: uiState.errorMessage
                        if (error != null) {
                            Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        
        if (showTerminateConfirmation) {
            AlertDialog(
                onDismissRequest = { showTerminateConfirmation = false },
                title = { Text("Clôturer / Résilier le Contrat de Bail") },
                text = { Text("Êtes-vous sûr de vouloir définitivement résilier et clôturer ce contrat de bail ? Le logement rattaché sera automatiquement libéré et repassé à l'état disponible pour une nouvelle location. Cette action est irréversible.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateLeaseStatus(lease.id, LeaseStatus.TERMINATED) { res ->
                                res.onSuccess {
                                    selectedLeaseForDetail = it
                                    localError = null
                                }.onFailure { ex ->
                                    localError = ex.message
                                }
                            }
                            showTerminateConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirmer la clôture")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTerminateConfirmation = false }) {
                        Text("Annuler")
                    }
                }
            )
        }
    } else {
        // State variables for filter capsule selection
        var activeFilter by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE", "TERMINATED", "PENDING"
        val strings = LocalStrings.current

        val filteredLeases = remember(uiState.leases, activeFilter) {
            val list = uiState.leases
            when (activeFilter) {
                "ACTIVE" -> list.filter { it.status == LeaseStatus.SIGNED_ACTIVE }
                "PENDING" -> list.filter { it.status == LeaseStatus.PENDING_PAYMENT || it.status == LeaseStatus.PENDING_SIGNATURE || it.status == LeaseStatus.DOWN_PAYMENT_PAID }
                else -> list
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Header (Spans full width)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Gestion des Contrats de Bail (Baux)",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color(0xFF006948) // Brand deep primary green
                        )
                        Text(
                            text = "Consultez et gérez l'ensemble des contrats actifs et passés de la résidence.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    
                    if (isAuthorized) {
                        Button(
                            onClick = { showWizard = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                Text("Nouveau contrat de bail", color = Color.White)
                            }
                        }
                    }
                }
            }

            // 2. Filter Bar (Spans full width)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                        .width(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterCapsuleItem(label = "Tous les contrats", isSelected = activeFilter == "ALL", onClick = { activeFilter = "ALL" })
                    FilterCapsuleItem(label = "En cours", isSelected = activeFilter == "ACTIVE", onClick = { activeFilter = "ACTIVE" })
                    FilterCapsuleItem(label = "En attente", isSelected = activeFilter == "PENDING", onClick = { activeFilter = "PENDING" })
                }
            }

            // 3. Grid List of leases
            if (filteredLeases.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun contrat ne correspond à ce filtre.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                items(filteredLeases) { lease ->
                    val matchedLogement = uiState.logements.firstOrNull { it.id == lease.logementId }?.name ?: "Logement ${lease.logementId.take(5)}"
                    val matchedTenant = uiState.members.firstOrNull { it.userId == lease.tenantId }
                    val tenantName = matchedTenant?.let { "${it.firstName} ${it.lastName}" } ?: "Inconnu"

                    // Custom colors based on lease status
                    val (badgeBg, badgeColor, badgeText) = when (lease.status) {
                        LeaseStatus.SIGNED_ACTIVE -> Triple(Color(0xFFE0E7FF), Color(0xFF1E3A8A), "EN COURS")
                        LeaseStatus.PENDING_SIGNATURE -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "EN ATTENTE SIGNATURE")
                        LeaseStatus.PENDING_PAYMENT -> Triple(Color(0xFFFDE8E8), Color(0xFFBA1A1A), "EN ATTENTE PAIEMENT")
                        LeaseStatus.DOWN_PAYMENT_PAID -> Triple(Color(0xFFE6F7F0), Color(0xFF006948), "DOWN_PAYMENT_PAID")
                        LeaseStatus.TERMINATED -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "TERMINÉ")
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLeaseForDetail = lease },
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
                                        text = matchedLogement, 
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
            }
        }
    }

    if (showWizard) {
        LeaseWizardDialog(
            viewModel = viewModel,
            onDismiss = { showWizard = false }
        )
    }
}

@Composable
fun FilterCapsuleItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
            contentColor = if (isSelected) Color(0xFF006948) else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
        shape = RoundedCornerShape(9999.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Box(modifier = Modifier.fillMaxHeight().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
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
            style = MaterialTheme.typography.titleMedium, 
            color = if (isGreen) Color(0xFF006948) else MaterialTheme.colorScheme.onSurface
        )
    }
}

enum class WizardStep { TENANT, UNIT, FINANCIALS, TIMELINE }

@Composable
fun LeaseWizardDialog(
    viewModel: LoginViewModel,
    onDismiss: () -> Unit,
    initialLogementId: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableStateOf(WizardStep.TENANT) }

    // Unified draft form state to gather all inputs
    var draftTenantId by remember { mutableStateOf<String?>(null) }
    var draftTenantName by remember { mutableStateOf("") }
    var isInlineTenant by remember { mutableStateOf(false) }
    var inlineFirstName by remember { mutableStateOf("") }
    var inlineLastName by remember { mutableStateOf("") }
    var inlineEmail by remember { mutableStateOf("") }
    var inlinePhone by remember { mutableStateOf("") }

    var draftLogementId by remember { mutableStateOf(initialLogementId ?: "") }
    var draftDepositAmount by remember { mutableStateOf("0.0") }
    var draftPaymentFrequency by remember { mutableStateOf("MONTHLY") }
    var draftStartDate by remember { mutableStateOf("") }
    var draftEndDate by remember { mutableStateOf("") }
    var draftAdvanceMonths by remember { mutableStateOf("12") }
    var draftAdvancePaymentAmount by remember { mutableStateOf("0.0") }
    var draftPaymentMethod by remember { mutableStateOf("CASH") } // State for selector

    // User lookup variables
    var userQuery by remember { mutableStateOf("") }
    var userResults by remember { mutableStateOf<List<UserSearchDto>>(emptyList()) }
    var isSearchingUsers by remember { mutableStateOf(false) }

    // Search Job Coroutine
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Assistant Nouveau Contrat de Bail", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onDismiss) {
                    Text("✕", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step Progress tracker
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WizardStep.entries.forEachIndexed { index, step ->
                        val isActive = step == currentStep
                        val stepNum = index + 1
                        val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Badge(containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                                Text(text = stepNum.toString(), modifier = Modifier.padding(4.dp), color = color)
                            }
                            Text(text = step.name, style = MaterialTheme.typography.bodySmall, color = color)
                        }
                    }
                }

                HorizontalDivider()

                when (currentStep) {
                    WizardStep.TENANT -> {
                        Text("Étape 1 sur 4 : Sélection du Locataire", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        
                        if (isInlineTenant) {
                            // Inline creation form
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Nouveau locataire inline", style = MaterialTheme.typography.titleSmall)
                                OutlinedTextField(value = inlineFirstName, onValueChange = { inlineFirstName = it }, label = { Text("Prénom *") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = inlineLastName, onValueChange = { inlineLastName = it }, label = { Text("Nom *") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = inlineEmail, onValueChange = { inlineEmail = it }, label = { Text("Email *") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = inlinePhone, onValueChange = { inlinePhone = it }, label = { Text("Téléphone") }, modifier = Modifier.fillMaxWidth())
                                
                                Button(
                                    onClick = { isInlineTenant = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("Retour à la recherche")
                                }
                            }
                        } else {
                            // Search as you type
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = userQuery,
                                    onValueChange = { q ->
                                        userQuery = q
                                        // Real-time API query
                                        if (q.length >= 2) {
                                            isSearchingUsers = true
                                            coroutineScope.launch {
                                                try {
                                                    val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/users/search") {
                                                        parameter("q", q)
                                                        header(io.ktor.http.HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                                    }
                                                    if (response.status == io.ktor.http.HttpStatusCode.OK) {
                                                        userResults = response.body<List<UserSearchDto>>()
                                                    }
                                                } catch (e: Exception) {}
                                                isSearchingUsers = false
                                            }
                                        } else {
                                            userResults = emptyList()
                                        }
                                        userQuery = q
                                    },
                                    label = { Text("Saisissez le nom d'un locataire...") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (isSearchingUsers) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }

                                if (userResults.isNotEmpty()) {
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column {
                                            userResults.forEach { user ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            draftTenantId = user.id
                                                            draftTenantName = user.name
                                                            userResults = emptyList()
                                                            userQuery = user.name
                                                        }
                                                        .padding(12.dp)
                                                ) {
                                                    Text(text = "${user.name} (${user.email})")
                                                }
                                            }
                                        }
                                    }
                                }

                                if (draftTenantId != null) {
                                    Text("Sélectionné : $draftTenantName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }

                                Button(
                                    onClick = { 
                                        isInlineTenant = true 
                                        draftTenantId = null
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("+ Ajouter un nouveau locataire")
                                }
                            }
                        }
                    }

                    WizardStep.UNIT -> {
                        Text("Étape 2 sur 4 : Sélection du Logement", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        
                        val availableLogements = uiState.logements.filter { it.status == "AVAILABLE" }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (availableLogements.isEmpty()) {
                                Text("Aucun logement de type AVAILABLE (libre) n'est disponible dans cette résidence.")
                            } else {
                                Text("Sélectionnez l'unité libre à attribuer :")
                                
                                availableLogements.forEach { logement ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { draftLogementId = logement.id }
                                            .background(if (draftLogementId == logement.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(text = logement.name, style = MaterialTheme.typography.titleSmall)
                                            Text(text = "Étage : ${logement.floor} | Loyer : ${logement.nominalRent} XOF")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    WizardStep.FINANCIALS -> {
                        Text("Étape 3 sur 4 : Conditions Financières", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        
                        val selectedLogement = uiState.logements.firstOrNull { it.id == draftLogementId }
                        val isAnnual = draftPaymentFrequency == "ANNUAL"

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = draftDepositAmount,
                                onValueChange = { draftDepositAmount = it },
                                label = { Text("Montant du dépôt de garantie (Caution) *") },
                                isError = (draftDepositAmount.toDoubleOrNull() ?: 0.0) < 0.0,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Fréquence de paiement :")
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = { draftPaymentFrequency = "MONTHLY" },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (draftPaymentFrequency == "MONTHLY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("Mensuelle (MONTHLY)")
                                }
                                Button(
                                    onClick = { draftPaymentFrequency = "ANNUAL" },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (draftPaymentFrequency == "ANNUAL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("Annuelle (ANNUAL)")
                                }
                            }

                            // Show Annual specific inputs if frequency is ANNUAL
                            if (isAnnual) {
                                OutlinedTextField(
                                    value = draftAdvanceMonths,
                                    onValueChange = { draftAdvanceMonths = it },
                                    label = { Text("Nombre de mois d'avance payés (Défaut : 12) *") },
                                    isError = (draftAdvanceMonths.toIntOrNull() ?: 0) <= 0,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Rule 1: Acompte field is always visible to dynamically calculate target status
                            OutlinedTextField(
                                value = draftAdvancePaymentAmount,
                                onValueChange = { draftAdvancePaymentAmount = it },
                                label = { Text("Montant de l'acompte immédiat versé (XOF)") },
                                isError = (draftAdvancePaymentAmount.toDoubleOrNull() ?: 0.0) < 0.0,
                                modifier = Modifier.fillMaxWidth()
                            )

                            val deposit = draftDepositAmount.toDoubleOrNull() ?: 0.0
                            val acompteVal = draftAdvancePaymentAmount.toDoubleOrNull() ?: 0.0

                            // Rule 2.3: Conditional Payment Method selector only visible if acompte > 0
                            if (acompteVal > 0.0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Mode de paiement de l'acompte :", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { draftPaymentMethod = "CASH" },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (draftPaymentMethod == "CASH") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text("Espèces")
                                    }
                                    Button(
                                        onClick = { draftPaymentMethod = "TRANSFER" },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (draftPaymentMethod == "TRANSFER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text("Virement")
                                    }
                                    Button(
                                        onClick = { draftPaymentMethod = "MOBILE" },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (draftPaymentMethod == "MOBILE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text("Mobile Money")
                                    }
                                }
                            }

                            if (selectedLogement != null) {
                                val rent = selectedLogement.nominalRent
                                val charges = selectedLogement.serviceCharges
                                
                                val months = if (isAnnual) (draftAdvanceMonths.toIntOrNull() ?: 12) else 1
                                val firstRent = months * (rent + charges)
                                
                                val totalRequiredToPay = deposit + firstRent
                                val remainingToPay = totalRequiredToPay - acompteVal

                                // Rule 1.1, 1.2, 1.3: Real-time dynamic target status badge
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
                                            Text("RESTE À PAYER :", style = MaterialTheme.typography.titleMedium, color = if (remainingToPay > 0.0) Color(0xFFBA1A1A) else Color(0xFF006948))
                                            Text("${if (remainingToPay > 0.0) remainingToPay else 0.0} XOF", style = MaterialTheme.typography.titleMedium, color = if (remainingToPay > 0.0) Color(0xFFBA1A1A) else Color(0xFF006948))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    WizardStep.TIMELINE -> {
                        Text("Étape 4 sur 4 : Calendrier & Durée", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = draftStartDate,
                                onValueChange = { draftStartDate = it },
                                label = { Text("Date de début (AAAA-MM-JJ) *") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = draftEndDate,
                                onValueChange = { draftEndDate = it },
                                label = { Text("Date de fin (AAAA-MM-JJ) *") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Safe duration calculation using platform-agnostic remember (compiles on both JVM and JS targets)
                            val computedDuration = remember(draftStartDate, draftEndDate) {
                                try {
                                    val startParts = draftStartDate.split("-")
                                    val endParts = draftEndDate.split("-")
                                    val startYear = startParts[0].toInt()
                                    val startMonth = startParts[1].toInt()
                                    val endYear = endParts[0].toInt()
                                    val endMonth = endParts[1].toInt()
                                    val totalMonths = (endYear - startYear) * 12 + (endMonth - startMonth)
                                    if (totalMonths > 0) totalMonths else -1
                                } catch (ex: Exception) {
                                    -1
                                }
                            }

                            if (computedDuration > 0) {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                    Text(text = "Durée calculée du contrat : $computedDuration mois", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }

                val error = uiState.errorMessage
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            if (currentStep == WizardStep.TIMELINE) {
                Button(
                    onClick = {
                        val rentVal = uiState.logements.firstOrNull { it.id == draftLogementId }?.nominalRent ?: 0.0
                        val req = LeaseCreateRequest(
                            tenantId = draftTenantId,
                            inlineTenant = if (isInlineTenant) InlineTenantCreateRequest(inlineFirstName, inlineLastName, inlineEmail, inlinePhone.ifBlank { null }) else null,
                            logementId = draftLogementId,
                            depositAmount = draftDepositAmount.toDoubleOrNull() ?: 0.0,
                            paymentFrequency = draftPaymentFrequency,
                            startDate = draftStartDate,
                            endDate = draftEndDate,
                            monthlyRentAtSign = rentVal,
                            advanceMonths = if (draftPaymentFrequency == "ANNUAL") (draftAdvanceMonths.toIntOrNull() ?: 12) else 1,
                            advancePaymentAmount = (draftAdvancePaymentAmount.toDoubleOrNull() ?: 0.0)
                        )
                        viewModel.createLease(draftLogementId, req) {
                            onDismiss()
                        }
                    },
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Générer le contrat")
                    }
                }
            } else {
                Button(onClick = {
                    currentStep = when (currentStep) {
                        WizardStep.TENANT -> WizardStep.UNIT
                        WizardStep.UNIT -> WizardStep.FINANCIALS
                        WizardStep.FINANCIALS -> WizardStep.TIMELINE
                        WizardStep.TIMELINE -> WizardStep.TIMELINE
                    }
                }) {
                    Text("Suivant")
                }
            }
        },
        dismissButton = {
            if (currentStep != WizardStep.TENANT) {
                TextButton(onClick = {
                    currentStep = when (currentStep) {
                        WizardStep.TENANT -> WizardStep.TENANT
                        WizardStep.UNIT -> WizardStep.TENANT
                        WizardStep.FINANCIALS -> WizardStep.UNIT
                        WizardStep.TIMELINE -> WizardStep.FINANCIALS
                    }
                }) {
                    Text("Retour")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Annuler")
                }
            }
        }
    )
}
