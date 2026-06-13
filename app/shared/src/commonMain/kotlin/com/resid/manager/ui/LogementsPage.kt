package com.resid.manager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.LogementDto
import com.resid.manager.dto.UserRole
import com.resid.manager.viewmodel.LoginViewModel

@Composable
fun LogementsPage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activeResidence = uiState.selectedResidenceContext
    val isAuthorized = activeResidence != null && (activeResidence.userRoleInResidence == UserRole.ADMIN || activeResidence.userRoleInResidence == UserRole.RESIDENCE_MANAGER)

    var showDeleteConfirmationId by remember { mutableStateOf<String?>(null) }
    var editingLogement by remember { mutableStateOf<LogementDto?>(null) }
    var selectedLogementForDetail by remember { mutableStateOf<LogementDto?>(null) }

    if (selectedLogementForDetail != null) {
        val logement = selectedLogementForDetail!!
        
        // Lookup active lease for this logement
        val activeLease = uiState.leases.firstOrNull { it.logementId == logement.id }
        val activeTenant = activeLease?.let { lease -> uiState.members.firstOrNull { it.userId == lease.tenantId } }

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
                    TextButton(onClick = { selectedLogementForDetail = null }) {
                        Text("← Retour à la liste")
                    }
                    Text(text = "Détails du Logement : ${logement.name}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }
                Badge(
                    containerColor = if (logement.status == "AVAILABLE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(text = logement.status, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(8.dp))
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Column: Logement details
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Fiche technique du Logement", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider()

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Nom / Numéro : ${logement.name}", style = MaterialTheme.typography.bodyLarge)
                            Text("Étage : ${logement.floor}", style = MaterialTheme.typography.bodyMedium)
                            Text("Type d'appartement : ${logement.type}", style = MaterialTheme.typography.bodyMedium)
                            Text("Loyer mensuel : ${logement.nominalRent} XOF", style = MaterialTheme.typography.bodyLarge)
                            Text("Charges fixes d'entretien : ${logement.serviceCharges} XOF", style = MaterialTheme.typography.bodyMedium)
                            Text("Index Électricité initial : ${logement.initialElectricityIndex} kWh", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Right Column: Active Lease & Actions
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Locataire & Actions de Gestion", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider()

                        if (logement.status == "OCCUPIED" && activeTenant != null && activeLease != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("✓ CONTRAT EN COURS", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    HorizontalDivider()
                                    Text("Locataire : ${activeTenant.firstName} ${activeTenant.lastName}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Email : ${activeTenant.email}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Téléphone : ${activeTenant.phone ?: "Non spécifié"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Période : du ${activeLease.startDate} au ${activeLease.endDate}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Logement disponible", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    Text("Aucun contrat de bail actif n'est rattaché à cette unité pour le moment.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        if (isAuthorized) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { editingLogement = logement },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("✎ Modifier")
                                }
                                Button(
                                    onClick = { showDeleteConfirmationId = logement.id },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("✕ Supprimer")
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gestion des Logements",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Only show Button if user is OWNER, ADMIN or RESIDENCE_MANAGER
                if (isAuthorized) {
                    Button(onClick = { viewModel.setShowCreateLogementDialog(true) }) {
                        Text("+ Ajouter un logement")
                    }
                }
            }

            if (uiState.logements.isEmpty()) {
                // Empty state template
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 500.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Aucun logement enregistré",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Aucun logement n'a été enregistré dans cette résidence pour le moment. Cliquez sur le bouton ci-dessus pour ajouter votre premier appartement.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                // List of housings (logements)
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        uiState.logements.forEach { logement ->
                            LogementCard(
                                logement = logement,
                                onClick = { selectedLogementForDetail = logement }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmationId != null) {
        val targetId = showDeleteConfirmationId!!
        val logementName = uiState.logements.firstOrNull { it.id == targetId }?.name ?: "Ce logement"

        AlertDialog(
            onDismissRequest = { showDeleteConfirmationId = null },
            title = { Text("Supprimer le logement") },
            text = { Text("Êtes-vous sûr de vouloir supprimer définitivement le logement '$logementName' ? Cette action est irréversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLogement(targetId)
                        showDeleteConfirmationId = null
                        selectedLogementForDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationId = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Edit Logement Dialog
    if (editingLogement != null) {
        val target = editingLogement!!
        EditLogementDialog(
            viewModel = viewModel,
            logement = target,
            onDismiss = { editingLogement = null },
            onSubmit = { name, floor, type, rent, charges, initialIndex ->
                viewModel.updateLogement(target.id, name, floor, type, rent, charges, initialIndex)
                editingLogement = null
                selectedLogementForDetail = target.copy(
                    name = name,
                    floor = floor,
                    type = type,
                    nominalRent = rent,
                    serviceCharges = charges,
                    initialElectricityIndex = initialIndex
                )
            }
        )
    }
}

@Composable
fun LogementCard(
    logement: LogementDto,
    onClick: () -> Unit
) {
    val statusColor = if (logement.status == "AVAILABLE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val statusText = if (logement.status == "AVAILABLE") "AVAILABLE (Libre)" else logement.status

    Card(
        onClick = onClick,
        modifier = Modifier.width(300.dp).wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = logement.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Badge(containerColor = statusColor) {
                    Text(text = statusText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(4.dp))
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Étage : ${logement.floor}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Type : ${logement.type}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Loyer de base : ${logement.nominalRent} XOF / mois", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Charges fixes : ${logement.serviceCharges} XOF / mois", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Index Elec initial : ${logement.initialElectricityIndex} kWh", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
