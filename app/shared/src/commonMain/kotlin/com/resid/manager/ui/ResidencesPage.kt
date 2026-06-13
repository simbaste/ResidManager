package com.resid.manager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.viewmodel.AppScreen
import com.resid.manager.viewmodel.LoginViewModel

@Composable
fun ResidencesPage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Split residences: ADMIN behaves as OWNER in our UI directories
    val ownedResidences = uiState.residences.filter { it.userRoleInResidence == UserRole.ADMIN }
    val associatedResidences = uiState.residences.filter { it.userRoleInResidence != UserRole.ADMIN }

    var editingResidence by remember { mutableStateOf<ResidenceContext?>(null) }
    var deletingResidenceId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Top Section with title and action button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Annuaire de mes Résidences",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Button(onClick = { viewModel.setShowCreateResidenceDialog(true) }) {
                Text("+ Créer une résidence")
            }
        }

        if (uiState.residences.isEmpty()) {
            // Empty State Placeholder
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.widthIn(max = 500.dp)
                ) {
                    Text(
                        text = "Aucune résidence enregistrée",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "You don't have any residences yet. Click the button above to register your first building or search for an existing one to join.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(onClick = { viewModel.setShowCreateResidenceDialog(true) }) {
                            Text("Créer ma première résidence")
                        }
                        Button(
                            onClick = { viewModel.setShowJoinResidenceDialog(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Rejoindre une résidence")
                        }
                    }
                }
            }
        } else {
            // Non-empty State Directories
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // "My Owned Properties" Section
                if (ownedResidences.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "My Owned Properties (${ownedResidences.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ownedResidences.forEach { residence ->
                                OwnedPropertyCard(
                                    residence = residence,
                                    onClick = {
                                        viewModel.selectResidence(residence)
                                        viewModel.navigateToAppScreen(AppScreen.DASHBOARD)
                                    },
                                    onEditClick = { editingResidence = residence },
                                    onDeleteClick = { deletingResidenceId = residence.residenceId }
                                )
                            }
                        }
                    }
                }

                // "Associated Properties" Section
                if (associatedResidences.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Associated Properties (${associatedResidences.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            associatedResidences.forEach { residence ->
                                AssociatedPropertyCard(
                                    residence = residence,
                                    onClick = {
                                        viewModel.selectResidence(residence)
                                        viewModel.navigateToAppScreen(AppScreen.DASHBOARD)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Residence Dialog
    if (editingResidence != null) {
        val target = editingResidence!!
        EditResidenceDialog(
            residence = target,
            onDismiss = { editingResidence = null },
            onSubmit = { name, address, kWhPrice ->
                viewModel.updateResidence(target.residenceId, name, address, kWhPrice)
                editingResidence = null
            }
        )
    }

    // Delete Residence Confirmation Dialog
    if (deletingResidenceId != null) {
        val targetId = deletingResidenceId!!
        val resName = uiState.residences.firstOrNull { it.residenceId == targetId }?.residenceName ?: "Cette résidence"

        AlertDialog(
            onDismissRequest = { deletingResidenceId = null },
            title = { Text("Supprimer la résidence") },
            text = { Text("Êtes-vous sûr de vouloir supprimer définitivement la résidence '$resName' ainsi que tous ses logements, contrats et données financières ? Cette action est irréversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteResidence(targetId)
                        deletingResidenceId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingResidenceId = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun OwnedPropertyCard(
    residence: ResidenceContext,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(280.dp).height(180.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = residence.residenceName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = residence.residenceAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${residence.totalUnits} logement" + (if (residence.totalUnits > 1) "s" else ""),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = "OWNER",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AssociatedPropertyCard(
    residence: ResidenceContext,
    onClick: () -> Unit
) {
    val role = residence.userRoleInResidence
    val badgeColor = when (role) {
        UserRole.ADMIN -> MaterialTheme.colorScheme.primaryContainer
        UserRole.RESIDENCE_MANAGER -> MaterialTheme.colorScheme.tertiaryContainer
        UserRole.TENANT -> MaterialTheme.colorScheme.secondaryContainer
    }
    val badgeContentColor = when (role) {
        UserRole.ADMIN -> MaterialTheme.colorScheme.onPrimaryContainer
        UserRole.RESIDENCE_MANAGER -> MaterialTheme.colorScheme.onTertiaryContainer
        UserRole.TENANT -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        onClick = onClick,
        modifier = Modifier.width(280.dp).height(180.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = residence.residenceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = residence.residenceAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${residence.totalUnits} logement" + (if (residence.totalUnits > 1) "s" else ""),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Badge(containerColor = badgeColor) {
                    Text(
                        text = role.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = badgeContentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
