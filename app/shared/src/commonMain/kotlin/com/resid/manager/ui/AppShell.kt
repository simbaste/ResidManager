package com.resid.manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.LogementDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.viewmodel.AppScreen
import com.resid.manager.viewmodel.LoginViewModel

@Composable
fun AppShell(
    viewModel: LoginViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var isMobileSidebarVisible by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDesktop = maxWidth >= 768.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Left Sidebar (Desktop version, fixed)
            if (isDesktop) {
                SidebarContent(
                    selectedScreen = uiState.currentAppScreen,
                    onScreenSelected = { screen ->
                        viewModel.navigateToAppScreen(screen)
                    },
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Central Content Pane (Header + Selected Page Content)
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Top Header Section containing Residence dropdown selector
                HeaderBar(
                    title = uiState.currentAppScreen.title,
                    userName = "${uiState.firstName} ${uiState.lastName}",
                    residences = uiState.residences,
                    selectedResidence = uiState.selectedResidenceContext,
                    onResidenceSelected = { viewModel.selectResidence(it) },
                    isDesktop = isDesktop,
                    onMenuClick = { isMobileSidebarVisible = !isMobileSidebarVisible },
                    onLogoutClick = { viewModel.logout() },
                    onProfileClick = { viewModel.navigateToAppScreen(AppScreen.PROFILE) },
                    onAddResidenceClick = { viewModel.setShowCreateResidenceDialog(true) },
                    onJoinResidenceClick = { viewModel.setShowJoinResidenceDialog(true) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Central Dynamic Panel Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (uiState.currentAppScreen) {
                        AppScreen.DASHBOARD -> DashboardPage(viewModel = viewModel)
                        AppScreen.RESIDENCES -> ResidencesPage(viewModel = viewModel)
                        AppScreen.LOGEMENTS -> LogementsPage(viewModel = viewModel)
                        AppScreen.BAUX -> LeasesPage()
                        AppScreen.ELECTRICITY -> ElectricityPage()
                        AppScreen.TICKETS -> TicketsPage()
                        AppScreen.FINANCES -> FinancesPage()
                        AppScreen.PROFILE -> ProfilePage(viewModel = viewModel)
                    }
                }
            }
        }

        // Left Sidebar Overlay (Mobile version, drawer equivalent)
        if (!isDesktop && isMobileSidebarVisible) {
            // Dismissible background scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                    .clickable { isMobileSidebarVisible = false }
            )

            // Sidebar drawer sliding overlay
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(260.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .align(Alignment.CenterStart)
            ) {
                SidebarContent(
                    selectedScreen = uiState.currentAppScreen,
                    onScreenSelected = { screen ->
                        viewModel.navigateToAppScreen(screen)
                        isMobileSidebarVisible = false // Close sidebar on selection
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Modal Dialogs for Onboarding
    if (uiState.showCreateResidenceDialog) {
        CreateResidenceDialog(
            onDismiss = { viewModel.setShowCreateResidenceDialog(false) },
            onSubmit = { name, address, kWhPrice ->
                viewModel.createResidence(name, address, kWhPrice)
            }
        )
    }

    if (uiState.showJoinResidenceDialog) {
        JoinResidenceDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowJoinResidenceDialog(false) }
        )
    }

    if (uiState.showCreateLogementDialog) {
        CreateLogementDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowCreateLogementDialog(false) },
            onSubmit = { name, floor, type, rent, charges, initialIndex ->
                viewModel.createLogement(name, floor, type, rent, charges, initialIndex)
            }
        )
    }
}

@Composable
fun SidebarContent(
    selectedScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Resid Manager",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
        )

        val menuItems = AppScreen.entries.toTypedArray()
        for (item in menuItems) {
            val isSelected = item == selectedScreen
            val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

            Card(
                onClick = { onScreenSelected(item) },
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderBar(
    title: String,
    userName: String,
    residences: List<ResidenceContext>,
    selectedResidence: ResidenceContext?,
    onResidenceSelected: (ResidenceContext) -> Unit,
    isDesktop: Boolean,
    onMenuClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAddResidenceClick: () -> Unit,
    onJoinResidenceClick: () -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isDesktop) {
                IconButton(onClick = onMenuClick) {
                    Column(
                        modifier = Modifier.size(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.width(18.dp).height(2.dp).background(MaterialTheme.colorScheme.onSurface))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.width(18.dp).height(2.dp).background(MaterialTheme.colorScheme.onSurface))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.width(18.dp).height(2.dp).background(MaterialTheme.colorScheme.onSurface))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Global Residence Dropdown Selector
            Box {
                Button(
                    onClick = { showDropdown = !showDropdown },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text(selectedResidence?.residenceName ?: "No Active Residence")
                }

                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false }
                ) {
                    if (residences.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Créer une résidence...") },
                            onClick = {
                                showDropdown = false
                                onAddResidenceClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rejoindre une résidence...") },
                            onClick = {
                                showDropdown = false
                                onJoinResidenceClick()
                            }
                        )
                    } else {
                        residences.forEach { residence ->
                            DropdownMenuItem(
                                text = { Text(residence.residenceName) },
                                onClick = {
                                    onResidenceSelected(residence)
                                    showDropdown = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Créer une résidence") },
                            onClick = {
                                showDropdown = false
                                onAddResidenceClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🔍 Rejoindre une résidence") },
                            onClick = {
                                showDropdown = false
                                onJoinResidenceClick()
                            }
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile link showing User Name
            Text(
                text = userName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onProfileClick() }
                    .padding(4.dp)
            )

            // Simple sign out option
            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Déconnexion")
            }
        }
    }
}

// -----------------------------------------------------------------
// INTERACTIVE DASHBOARD PAGE & ROLE SCOPING
// -----------------------------------------------------------------

@Composable
fun DashboardPage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activeResidence = uiState.selectedResidenceContext

    if (activeResidence == null) {
        // No Residence / Empty State Onboarding
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.widthIn(max = 550.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Bienvenue dans Resid Manager",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Pour commencer, vous devez soit créer votre propre résidence, soit en rechercher une existante pour envoyer une demande d'adhésion.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setShowCreateResidenceDialog(true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Créer une résidence")
                        }

                        Button(
                            onClick = { viewModel.setShowJoinResidenceDialog(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Rejoindre une résidence")
                        }
                    }
                }
            }
        }
    } else {
        // Active Residence - Display personalized metrics and scoped widgets
        val role = activeResidence.userRoleInResidence
        val isManagement = role == UserRole.ADMIN || role == UserRole.RESIDENCE_MANAGER // elevated management tier

        var showActionMessage by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Personalized Greeting
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Bonjour, ${uiState.firstName} !",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Vous consultez actuellement la résidence : ${activeResidence.residenceName}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Rôle : ${role.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Core Metric Cards Row (Visibility scoped by role)
            if (isManagement) {
                // Show elevated Tier Cards (Financial & Operational metrics)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(title = "Cashflow Net", value = "1 250 000 XOF", desc = "Ce mois-ci", modifier = Modifier.weight(1f))
                    MetricCard(title = "Taux d'occupation", value = "82.5 %", desc = "Unités louées", modifier = Modifier.weight(1f))
                    MetricCard(title = "Impayés / Délinquance", value = "14.2 %", desc = "Loyers dus", modifier = Modifier.weight(1f))
                    MetricCard(title = "Tickets Actifs", value = "3", desc = "En cours d'intervention", modifier = Modifier.weight(1f))
                }
            } else {
                // Hide financial metrics for regular users. Show operational metrics instead!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(title = "Tickets Actifs", value = "3", desc = "En attente de traitement", modifier = Modifier.weight(1f))
                    MetricCard(title = "Dernier relevé élec.", value = "Relevé OK", desc = "Consulter l'historique", modifier = Modifier.weight(1f))
                }
            }

            // Quick Actions Section (Dynamic visibility scoped by role)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = "Actions rapides", style = MaterialTheme.typography.titleLarge)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (role) {
                            UserRole.ADMIN -> {
                                // Creator / Admin Quick Actions (full suite)
                                Button(onClick = { showActionMessage = "Formulaire de création de bail ouvert" }, modifier = Modifier.weight(1f)) {
                                    Text("Nouveau Bail")
                                }
                                Button(onClick = { viewModel.setShowCreateLogementDialog(true) }, modifier = Modifier.weight(1f)) {
                                    Text("Nouveau Logement")
                                }
                                Button(onClick = { showActionMessage = "Modal d'enregistrement de dépense ouvert" }, modifier = Modifier.weight(1f)) {
                                    Text("Enregistrer Dépense")
                                }
                            }
                            UserRole.RESIDENCE_MANAGER -> {
                                // Manager / Staff Quick Actions
                                Button(onClick = { viewModel.setShowCreateLogementDialog(true) }, modifier = Modifier.weight(1f)) {
                                    Text("Nouveau Logement")
                                }
                                Button(onClick = { showActionMessage = "Formulaire d'ouverture de ticket de maintenance ouvert" }, modifier = Modifier.weight(1f)) {
                                    Text("Ouvrir un Ticket")
                                }
                            }
                            UserRole.TENANT -> {
                                // Tenant Quick Actions
                                Button(
                                    onClick = { showActionMessage = "Formulaire de signalement d'incident (Maintenance) ouvert" },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text("Signaler un Incident / Ouvrir un Ticket", color = MaterialTheme.colorScheme.onError)
                                }
                            }
                        }
                    }

                    if (showActionMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Action exécutée: ${showActionMessage!!} (Bientôt disponible à l'étape 3 !)",
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}

// -----------------------------------------------------------------
// RESIDENCES DIRECTORY & PROPERTY CARDS
// -----------------------------------------------------------------

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
                    Text(
                        text = "✎",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .clickable { onEditClick() }
                            .padding(4.dp)
                    )
                    Text(
                        text = "✕",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .clickable { onDeleteClick() }
                            .padding(4.dp)
                    )
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

// -----------------------------------------------------------------
// HOUSING UNITS (LOGEMENTS) DIRECTORY & CREATION FORM
// -----------------------------------------------------------------

@Composable
fun LogementsPage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activeResidence = uiState.selectedResidenceContext
    val isAuthorized = activeResidence != null && (activeResidence.userRoleInResidence == UserRole.ADMIN || activeResidence.userRoleInResidence == UserRole.RESIDENCE_MANAGER)

    var showDeleteConfirmationId by remember { mutableStateOf<String?>(null) }
    var editingLogement by remember { mutableStateOf<LogementDto?>(null) }

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
                            isAuthorized = isAuthorized,
                            onDeleteClick = { showDeleteConfirmationId = logement.id },
                            onEditClick = { editingLogement = logement }
                        )
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
            }
        )
    }
}

@Composable
fun LogementCard(
    logement: LogementDto,
    isAuthorized: Boolean,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val statusColor = if (logement.status == "AVAILABLE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val statusText = if (logement.status == "AVAILABLE") "AVAILABLE (Libre)" else logement.status

    Card(
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = logement.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    // Show Edit & Delete Icons only if the user has MANAGEMENT permissions (OWNER, ADMIN, MANAGER)
                    if (isAuthorized) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✎",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .clickable { onEditClick() }
                                    .padding(4.dp)
                            )
                            Text(
                                text = "✕",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .clickable { onDeleteClick() }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

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

// -----------------------------------------------------------------
// DIALOG MODALS
// -----------------------------------------------------------------

@Composable
fun CreateResidenceDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var kWhPrice by remember { mutableStateOf("150.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer une résidence") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom de la résidence *") })
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Adresse *") })
                OutlinedTextField(value = kWhPrice, onValueChange = { kWhPrice = it }, label = { Text("Prix du kWh (XOF) *") })
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(name, address, kWhPrice.toDoubleOrNull() ?: 150.0) }) {
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun EditResidenceDialog(
    residence: ResidenceContext,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(residence.residenceName) }
    var address by remember { mutableStateOf(residence.residenceAddress) }
    var kWhPrice by remember { mutableStateOf("150.0") } // default fallback

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier la résidence") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom de la résidence *") })
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Adresse *") })
                OutlinedTextField(value = kWhPrice, onValueChange = { kWhPrice = it }, label = { Text("Prix du kWh (XOF) *") })
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(name, address, kWhPrice.toDoubleOrNull() ?: 150.0) }) {
                Text("Enregistrer les modifications")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun JoinResidenceDialog(
    viewModel: LoginViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rejoindre une résidence") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.widthIn(max = 450.dp).heightIn(max = 400.dp)
            ) {
                Text(
                    text = "Saisissez au moins 2 caractères pour rechercher une résidence existante et envoyer votre demande d'adhésion.",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    label = { Text("Rechercher par nom de résidence...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.isSearching) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    if (uiState.searchResults.isEmpty() && uiState.searchQuery.length >= 2) {
                        Text(
                            text = "Aucune résidence trouvée correspondant à votre recherche.",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else if (uiState.searchResults.isNotEmpty()) {
                        Text(
                            text = "Résultats de recherche (${uiState.searchResults.size}) :",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            uiState.searchResults.forEach { residence ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = residence.name, style = MaterialTheme.typography.titleMedium)
                                            Text(text = residence.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Button(
                                            onClick = { viewModel.joinResidence(residence.id) },
                                            enabled = !uiState.isLoading
                                        ) {
                                            if (uiState.isLoading) {
                                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                            } else {
                                                Text("Rejoindre")
                                            }
                                        }
                                    }
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
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun CreateLogementDialog(
    viewModel: LoginViewModel,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Double, Double, Double) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("RDC") }
    var type by remember { mutableStateOf("Studio") }
    var nominalRent by remember { mutableStateOf("") }
    var serviceCharges by remember { mutableStateOf("0.0") }
    var initialIndex by remember { mutableStateOf("0.0") }

    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un logement") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom / Numéro (ex: Appart A1) *") })
                
                // Floor Field
                OutlinedTextField(value = floor, onValueChange = { floor = it }, label = { Text("Étage (RDC, 1st, 2nd, etc.) *") })
                
                // Type Field
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (Chambre, Studio, T2, etc.) *") })

                OutlinedTextField(
                    value = nominalRent,
                    onValueChange = { nominalRent = it },
                    label = { Text("Loyer mensuel de base (XOF) *") }
                )

                OutlinedTextField(
                    value = serviceCharges,
                    onValueChange = { serviceCharges = it },
                    label = { Text("Charges fixes d'entretien (XOF) *") }
                )

                OutlinedTextField(
                    value = initialIndex,
                    onValueChange = { initialIndex = it },
                    label = { Text("Index initial élec. (kWh) *") }
                )

                val error = uiState.errorMessage ?: localError
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rentVal = nominalRent.toDoubleOrNull()
                    val chargesVal = serviceCharges.toDoubleOrNull()
                    val indexVal = initialIndex.toDoubleOrNull()

                    if (name.isBlank() || floor.isBlank() || type.isBlank() || rentVal == null || chargesVal == null || indexVal == null) {
                        localError = "Veuillez remplir tous les champs et saisir des valeurs numériques valides."
                        return@Button
                    }
                    if (rentVal < 0.0 || chargesVal < 0.0 || indexVal < 0.0) {
                        localError = "Les valeurs numériques doivent être supérieures ou égales à 0."
                        return@Button
                    }

                    onSubmit(name, floor, type, rentVal, chargesVal, indexVal)
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun EditLogementDialog(
    viewModel: LoginViewModel,
    logement: LogementDto,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Double, Double, Double) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf(logement.name) }
    var floor by remember { mutableStateOf(logement.floor) }
    var type by remember { mutableStateOf(logement.type) }
    var nominalRent by remember { mutableStateOf(logement.nominalRent.toString()) }
    var serviceCharges by remember { mutableStateOf(logement.serviceCharges.toString()) }
    var initialIndex by remember { mutableStateOf(logement.initialElectricityIndex.toString()) }

    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le logement") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom / Numéro (ex: Appart A1) *") })
                OutlinedTextField(value = floor, onValueChange = { floor = it }, label = { Text("Étage (RDC, 1st, 2nd, etc.) *") })
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (Chambre, Studio, T2, etc.) *") })

                OutlinedTextField(
                    value = nominalRent,
                    onValueChange = { nominalRent = it },
                    label = { Text("Loyer mensuel de base (XOF) *") }
                )

                OutlinedTextField(
                    value = serviceCharges,
                    onValueChange = { serviceCharges = it },
                    label = { Text("Charges fixes d'entretien (XOF) *") }
                )

                OutlinedTextField(
                    value = initialIndex,
                    onValueChange = { initialIndex = it },
                    label = { Text("Index initial élec. (kWh) *") }
                )

                val error = uiState.errorMessage ?: localError
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rentVal = nominalRent.toDoubleOrNull()
                    val chargesVal = serviceCharges.toDoubleOrNull()
                    val indexVal = initialIndex.toDoubleOrNull()

                    if (name.isBlank() || floor.isBlank() || type.isBlank() || rentVal == null || chargesVal == null || indexVal == null) {
                        localError = "Veuillez remplir tous les champs et saisir des valeurs numériques valides."
                        return@Button
                    }
                    if (rentVal < 0.0 || chargesVal < 0.0 || indexVal < 0.0) {
                        localError = "Les valeurs numériques doivent être supérieures ou égales à 0."
                        return@Button
                    }

                    onSubmit(name, floor, type, rentVal, chargesVal, indexVal)
                }
            ) {
                Text("Enregistrer les modifications")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

// -----------------------------------------------------------------
// PAGE TEMPLATE PLACEHOLDERS
// -----------------------------------------------------------------

@Composable
fun LeasesPage() {
    PagePlaceholder(title = "Contrats de bail (Baux)", description = "Suivez le cycle de vie de vos baux locataires : de la signature à la facturation du dépôt de garantie.")
}

@Composable
fun ElectricityPage() {
    PagePlaceholder(title = "Facturation Électricité", description = "Enregistrez les relevés de compteurs d'électricité de vos logements. L'application calcule automatiquement les montants dus d'après la formule d'Eco-Print.")
}

@Composable
fun TicketsPage() {
    PagePlaceholder(title = "Tickets d'Intervention", description = "Consultez, modifiez ou résolvez les rapports d'incidents techniques (plomberie, électricité, etc.) signalés par les résidents.")
}

@Composable
fun FinancesPage() {
    PagePlaceholder(title = "Finances & Cashflow", description = "Analysez en temps réel les flux d'entrées et de dépenses de chaque résidence, filtrez par période et suivez les impayés.")
}

@Composable
fun ProfilePage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Mon Compte", style = MaterialTheme.typography.headlineMedium)
                HorizontalDivider()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Prénom :", style = MaterialTheme.typography.bodyLarge)
                    Text(uiState.firstName, style = MaterialTheme.typography.bodyLarge)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Nom :", style = MaterialTheme.typography.bodyLarge)
                    Text(uiState.lastName, style = MaterialTheme.typography.bodyLarge)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Email :", style = MaterialTheme.typography.bodyLarge)
                    Text(uiState.email, style = MaterialTheme.typography.bodyLarge)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Téléphone :", style = MaterialTheme.typography.bodyLarge)
                    Text(uiState.phone.ifBlank { "Non renseigné" }, style = MaterialTheme.typography.bodyLarge)
                }

                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Se déconnecter")
                }
            }
        }
    }
}

@Composable
fun PagePlaceholder(
    title: String,
    description: String
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.widthIn(max = 500.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text(
                    text = "Prêt pour le raccordement de l'API de l'étape 3 !",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
