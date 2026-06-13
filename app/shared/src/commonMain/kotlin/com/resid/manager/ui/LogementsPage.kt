package com.resid.manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
    var showAssignLeaseWizardByLogementId by remember { mutableStateOf<String?>(null) }

    if (selectedLogementForDetail != null) {
        val logement = selectedLogementForDetail!!
        
        // Lookup active lease for this logement
        val activeLease = uiState.leases.firstOrNull { it.logementId == logement.id }
        val activeTenant = activeLease?.let { lease -> uiState.members.firstOrNull { it.userId == lease.tenantId } }
        val isAvailable = logement.status == "AVAILABLE"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Breadcrumb / Back Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { selectedLogementForDetail = null },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = Color(0xFF006948),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Retour à la liste",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF006948)
                    )
                }
            }

            // 2. Page Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Détails du Logement : ${logement.name}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${activeResidence?.residenceName ?: "Résidence"}, Zone A, Bâtiment C",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    
                    // State Capsule Badge
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAvailable) Color(0xFFE6F7F0) else Color(0xFFFDE8E8)
                        ),
                        shape = RoundedCornerShape(9999.dp),
                        border = BorderStroke(1.dp, (if (isAvailable) Color(0xFF006948) else Color(0xFFBA1A1A)).copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pulsing dot circle representation
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isAvailable) Color(0xFF006948) else Color(0xFFBA1A1A))
                            )
                            Text(
                                text = if (isAvailable) "AVAILABLE" else "OCCUPIED",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isAvailable) Color(0xFF006948) else Color(0xFFBA1A1A)
                            )
                        }
                    }
                }
            }

            // 3. Bento Grid Content (Two Column Layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Column (Technical Details)
                Card(
                    modifier = Modifier.weight(7f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = null,
                                tint = Color(0xFF006948)
                            )
                            Text(
                                text = "Fiche technique du Logement",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Multi-column Tech Grid
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Nom / Numéro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(logement.name, style = MaterialTheme.typography.titleLarge)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Étage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("Étage ${logement.floor}", style = MaterialTheme.typography.titleLarge)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Type d'appartement", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("Studio ${logement.type}", style = MaterialTheme.typography.titleLarge)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Loyer mensuel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = "${logement.nominalRent} XOF", 
                                        style = MaterialTheme.typography.headlineMedium, 
                                        color = Color(0xFF006948)
                                    )
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Charges fixes d'entretien", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("${logement.serviceCharges} XOF", style = MaterialTheme.typography.titleLarge)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Index Électricité initial", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("${logement.initialElectricityIndex} kWh", style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }

                        // Amenities Tags (Équipements inclus)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text(
                                text = "ÉQUIPEMENTS INCLUS", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.outline
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (logement.equipements.isEmpty()) {
                                    Text("Aucun équipement renseigné", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                } else {
                                    logement.equipements.forEach { eq ->
                                        val icon = when (eq.key) {
                                            "WIFI" -> Icons.Default.Share
                                            "CLIM" -> Icons.Default.Settings
                                            "EAU" -> Icons.Default.Star
                                            "PARKING" -> Icons.Default.LocationOn
                                            else -> Icons.Default.Home
                                        }
                                        ListOfAmenityBadge(icon = icon, label = eq.label)
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Column (Management & Tenant Section)
                Column(
                    modifier = Modifier.weight(5f),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Card 1: Tenant & Actions
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF006948)
                                )
                                Text(
                                    text = "Locataire & Actions",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            if (isAvailable) {
                                // Empty state matching screenshot
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF006948).copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF006948))
                                        }
                                        Text(text = "Logement disponible", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "Aucun contrat de bail actif n'est rattaché à cette unité pour le moment.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        OutlinedButton(
                                            onClick = { showAssignLeaseWizardByLogementId = logement.id },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Attribuer un nouveau locataire", color = Color(0xFF006948))
                                        }
                                    }
                                }
                            } else if (activeTenant != null && activeLease != null) {
                                // Active Tenant Profile Segment
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF006948).copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF006948).copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF006948))
                                            }
                                            Column {
                                                Text("${activeTenant.firstName} ${activeTenant.lastName}", style = MaterialTheme.typography.titleMedium)
                                                Text("Locataire Actif", style = MaterialTheme.typography.bodySmall, color = Color(0xFF006948))
                                            }
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                        Text("Email : ${activeTenant.email}", style = MaterialTheme.typography.bodyMedium)
                                        Text("Téléphone : ${activeTenant.phone ?: "Non spécifié"}", style = MaterialTheme.typography.bodyMedium)
                                        Text("Bail du ${activeLease.startDate} au ${activeLease.endDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }

                            // Administrative Control actions (Modifier, Supprimer)
                            if (isAuthorized) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { editingLogement = logement },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text("Modifier")
                                        }
                                    }

                                    Button(
                                        onClick = { showDeleteConfirmationId = logement.id },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text("Supprimer")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Card 2: Quick Insights (Statistiques Unité)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A2B)), // Dark background matching corporate SaaS
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "STATISTIQUES UNITÉ",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF68DBA9) // Brand green label
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Taux d'occupation (12m)", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFBEC6E0))
                                    Text("85%", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                }
                                HorizontalDivider(color = Color(0xFF1E2D3D))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Revenus bruts cumulés", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFBEC6E0))
                                    Text("480 000 XOF", style = MaterialTheme.typography.titleMedium, color = Color(0xFF68DBA9))
                                }
                            }
                        }
                    }
                }
            }

            // 4. Bottom Section: Photo Gallery & Floor Plan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Card 1: Gallery
                Card(
                    modifier = Modifier.weight(1f).height(200.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE2E8F0))) {
                        Column(
                            modifier = Modifier.padding(20.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("Galerie Photos", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            Text("Vue principale du studio", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }

                // Card 2: Floor Plan
                Card(
                    modifier = Modifier.weight(1f).height(200.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFCBD5E1))) {
                        Column(
                            modifier = Modifier.padding(20.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("Plan d'étage", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            Text("Configuration technique", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }

                // Card 3: Add Media Dashed
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(200.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Ajouter des médias", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Glissez-déposez vos photos ou documents ici", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Title & Add Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Gestion des Logements",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color(0xFF006948) // Brand deep primary green
                    )
                    Text(
                        text = "Visualisez et gérez l'état d'occupation de vos unités immobilières.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                if (isAuthorized) {
                    Button(
                        onClick = { viewModel.setShowCreateLogementDialog(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Text("Ajouter un logement", color = Color.White)
                        }
                    }
                }
            }

            // Stats Cards Row (Total, Available, Occupied, Late)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val total = uiState.logements.size
                val available = uiState.logements.count { it.status == "AVAILABLE" }
                val occupied = uiState.logements.count { it.status == "OCCUPIED" }

                BentoMiniStatCard(title = "Total Unités", value = total.toString().padStart(2, '0'), borderColor = Color(0xFF8E9193), modifier = Modifier.weight(1f))
                BentoMiniStatCard(title = "Disponibles", value = available.toString().padStart(2, '0'), borderColor = Color(0xFF006948), modifier = Modifier.weight(1f))
                BentoMiniStatCard(title = "Occupés", value = occupied.toString().padStart(2, '0'), borderColor = Color(0xFF3F465C), modifier = Modifier.weight(1f))
                BentoMiniStatCard(title = "En Retard", value = "00", borderColor = Color(0xFFBA1A1A), modifier = Modifier.weight(1f))
            }

            // Cards Bento Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // List housing units
                uiState.logements.forEach { logement ->
                    LogementCard(
                        logement = logement,
                        isAuthorized = isAuthorized,
                        onDetailClick = { selectedLogementForDetail = logement },
                        onEditClick = { editingLogement = logement }
                    )
                }

                // Plus / Add Action Card
                if (isAuthorized) {
                    Card(
                        onClick = { viewModel.setShowCreateLogementDialog(true) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF006948).copy(alpha = 0.04f)),
                        border = BorderStroke(1.dp, Color(0xFF006948).copy(alpha = 0.2f)),
                        modifier = Modifier
                            .width(300.dp)
                            .height(410.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF006948).copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF006948), modifier = Modifier.size(28.dp))
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(text = "Nouvelle Unité", style = MaterialTheme.typography.titleMedium, color = Color(0xFF006948))
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Configurez une nouvelle unité de logement dans cette résidence.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
            onSubmit = { name, floor, type, rent, charges, initialIndex, equipementIds ->
                viewModel.updateLogement(target.id, name, floor, type, rent, charges, initialIndex, equipementIds)
                editingLogement = null
                selectedLogementForDetail = target.copy(
                    name = name,
                    floor = floor,
                    type = type,
                    nominalRent = rent,
                    serviceCharges = charges,
                    initialElectricityIndex = initialIndex,
                    equipements = uiState.availableEquipements.filter { equipementIds.contains(it.id) }
                )
            }
        )
    }

    if (showAssignLeaseWizardByLogementId != null) {
        LeaseWizardDialog(
            viewModel = viewModel,
            onDismiss = { 
                showAssignLeaseWizardByLogementId = null
                selectedLogementForDetail = null // return to list to update and refresh
                viewModel.fetchLogements()
                viewModel.fetchLeases()
            },
            initialLogementId = showAssignLeaseWizardByLogementId
        )
    }
}

@Composable
fun BentoMiniStatCard(
    title: String,
    value: String,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(90.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left vertical border accent
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(borderColor)
            )
            
            // Content
            Column(
                modifier = Modifier.padding(16.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun LogementCard(
    logement: LogementDto,
    isAuthorized: Boolean,
    onDetailClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val isAvailable = logement.status == "AVAILABLE"
    val badgeBg = if (isAvailable) Color(0xFFE6F7F0) else Color(0xFFFDE8E8)
    val badgeColor = if (isAvailable) Color(0xFF006948) else Color(0xFFBA1A1A)
    val badgeText = if (isAvailable) "✓ AVAILABLE (Libre)" else "👤 OCCUPIED"

    Card(
        modifier = Modifier
            .width(300.dp)
            .height(410.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Abstract Image Representation Area with Badge overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color(0xFFE2E8F0)) // Clean slate gray image background placeholder
            ) {
                // Image Background abstract gradient to look gorgeous and hyper-realistic
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color(0xFFE2E8F0)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Overlaid Badge Capsule
                Card(
                    colors = CardDefaults.cardColors(containerColor = badgeBg),
                    shape = RoundedCornerShape(9999.dp),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Body Area
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Title and dropdown menu row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = logement.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDetailClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }

                    // Subtitle
                    Text(
                        text = "Studio ${logement.type} • Étage ${logement.floor}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Dynamic Value List rows
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LogementDetailRow(icon = Icons.Default.Star, label = "Loyer de base", value = "${logement.nominalRent} XOF / mois")
                        LogementDetailRow(icon = Icons.Default.Settings, label = "Index Elec initial", value = "${logement.initialElectricityIndex} kWh")
                        LogementDetailRow(icon = Icons.Default.Star, label = "Charges fixes", value = "${logement.serviceCharges} XOF / mois")
                    }
                }

                // Dynamic Action buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isAvailable) {
                        Button(
                            onClick = onDetailClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text("Louer maintenant", color = Color.White)
                        }
                        if (isAuthorized) {
                            OutlinedButton(
                                onClick = onEditClick,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDetailClick,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text("Voir le contrat", color = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedButton(
                            onClick = onDetailClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogementDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ListOfAmenityBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
