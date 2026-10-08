package com.resid.manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.features.dashboard.ui.DashboardScreen
import com.resid.manager.features.electricity.ui.ElectricityScreen
import com.resid.manager.features.finances.ui.FinancesScreen
import com.resid.manager.features.leases.ui.LeasesScreen
import com.resid.manager.features.leases.ui.components.wizard.LeaseWizardDialog
import com.resid.manager.features.members.ui.MembersScreen
import com.resid.manager.features.profile.ui.ProfileScreen
import com.resid.manager.features.residences.ui.ResidencesScreen
import com.resid.manager.features.tickets.ui.TicketsScreen
import com.resid.manager.features.units.ui.UnitsScreen
import com.resid.manager.navigation.AppNavKey
import com.resid.manager.navigation.PlatformBackHandler
import com.resid.manager.ui.theme.ResidTheme
import com.resid.manager.viewmodel.AppScreen
import com.resid.manager.viewmodel.LoginViewModel
import org.koin.compose.koinInject

@Composable
fun AppShell(
    viewModel: LoginViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isMobileSidebarVisible by remember { mutableStateOf(false) }
    var unitToEdit by remember { mutableStateOf<ResidenceUnitDto?>(null) }
    var unitIdToAssignLease by remember { mutableStateOf<String?>(null) }

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
                    onAddResidenceClick = { viewModel.setShowCreateResidenceDialog(true) },
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
                    userRole = uiState.selectedResidenceContext?.userRoleInResidence?.name,
                    residences = uiState.residences,
                    selectedResidence = uiState.selectedResidenceContext,
                    onResidenceSelected = { viewModel.selectResidence(it) },
                    isDesktop = isDesktop,
                    onMenuClick = { isMobileSidebarVisible = !isMobileSidebarVisible },
                    onLogoutClick = { viewModel.logout() },
                    onProfileClick = { viewModel.navigateToAppScreen(AppScreen.PROFILE) },
                    onAddResidenceClick = { viewModel.setShowCreateResidenceDialog(true) },
                    onJoinResidenceClick = { viewModel.setShowJoinResidenceDialog(true) },
                    isDarkTheme = uiState.darkMode,
                    onToggleTheme = { viewModel.toggleTheme() }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Central Dynamic Panel Content powered by Navigation 3
                val innerBackStack = remember {
                    val initialNav: AppNavKey = when (uiState.currentAppScreen) {
                        AppScreen.DASHBOARD -> AppNavKey.Dashboard
                        AppScreen.RESIDENCES -> AppNavKey.Residences
                        AppScreen.UNITS -> AppNavKey.Units
                        AppScreen.LEASES -> AppNavKey.Leases
                        AppScreen.MEMBERS -> AppNavKey.Members
                        AppScreen.ELECTRICITY -> AppNavKey.Electricity
                        AppScreen.TICKETS -> AppNavKey.Tickets
                        AppScreen.FINANCES -> AppNavKey.Finances
                        AppScreen.PROFILE -> AppNavKey.Profile
                    }
                    NavBackStack(initialNav)
                }

                // Synchronize innerBackStack when sidebar or header triggers navigation
                LaunchedEffect(uiState.currentAppScreen) {
                    val targetNav: AppNavKey = when (uiState.currentAppScreen) {
                        AppScreen.DASHBOARD -> AppNavKey.Dashboard
                        AppScreen.RESIDENCES -> AppNavKey.Residences
                        AppScreen.UNITS -> AppNavKey.Units
                        AppScreen.LEASES -> AppNavKey.Leases
                        AppScreen.MEMBERS -> AppNavKey.Members
                        AppScreen.ELECTRICITY -> AppNavKey.Electricity
                        AppScreen.TICKETS -> AppNavKey.Tickets
                        AppScreen.FINANCES -> AppNavKey.Finances
                        AppScreen.PROFILE -> AppNavKey.Profile
                    }
                    if (innerBackStack.lastOrNull() != targetNav) {
                        innerBackStack.add(targetNav)
                    }
                }

                // Handle inner navigation backstack on web / platform
                PlatformBackHandler(enabled = innerBackStack.size > 1) {
                    innerBackStack.removeLastOrNull()
                    when (innerBackStack.lastOrNull()) {
                        AppNavKey.Dashboard -> viewModel.navigateToAppScreen(AppScreen.DASHBOARD)
                        AppNavKey.Residences -> viewModel.navigateToAppScreen(AppScreen.RESIDENCES)
                        AppNavKey.Units -> viewModel.navigateToAppScreen(AppScreen.UNITS)
                        AppNavKey.Leases -> viewModel.navigateToAppScreen(AppScreen.LEASES)
                        AppNavKey.Members -> viewModel.navigateToAppScreen(AppScreen.MEMBERS)
                        AppNavKey.Electricity -> viewModel.navigateToAppScreen(AppScreen.ELECTRICITY)
                        AppNavKey.Tickets -> viewModel.navigateToAppScreen(AppScreen.TICKETS)
                        AppNavKey.Finances -> viewModel.navigateToAppScreen(AppScreen.FINANCES)
                        AppNavKey.Profile -> viewModel.navigateToAppScreen(AppScreen.PROFILE)
                        else -> {}
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    NavDisplay(
                        backStack = innerBackStack,
                        onBack = {
                            if (innerBackStack.size > 1) {
                                innerBackStack.removeLastOrNull()
                            }
                        },
                        entryProvider = { key: AppNavKey ->
                            when (key) {
                                AppNavKey.Dashboard -> NavEntry(key) {
                                    DashboardScreen(
                                        activeResidence = uiState.selectedResidenceContext,
                                        firstName = uiState.firstName,
                                        jwtToken = uiState.jwtToken,
                                        onCreateResidenceClick = { viewModel.setShowCreateResidenceDialog(true) },
                                        onJoinResidenceClick = { viewModel.setShowJoinResidenceDialog(true) },
                                        onNewUnitClick = { viewModel.setShowCreateUnitDialog(true) },
                                        onNewLeaseClick = { viewModel.navigateToAppScreen(AppScreen.LEASES) },
                                        onNewExpenseClick = { viewModel.navigateToAppScreen(AppScreen.FINANCES) },
                                        onViewReportsClick = { viewModel.navigateToAppScreen(AppScreen.FINANCES) }
                                    )
                                }
                                AppNavKey.Residences -> NavEntry(key) {
                                    ResidencesScreen(
                                        jwtToken = uiState.jwtToken,
                                        userId = uiState.loggedInUser?.id,
                                        onNavigateToDashboard = { residence ->
                                            viewModel.selectResidence(residence)
                                            viewModel.navigateToAppScreen(AppScreen.DASHBOARD)
                                        },
                                        onResidenceCreated = { newResidenceId ->
                                            viewModel.fetchResidences(autoSelectResidenceId = newResidenceId)
                                        }
                                    )
                                }
                                AppNavKey.Units -> NavEntry(key) {
                                    UnitsScreen(
                                        activeResidence = uiState.selectedResidenceContext,
                                        jwtToken = uiState.jwtToken,
                                        leases = uiState.leases,
                                        members = uiState.members,
                                        residenceUnits = uiState.residenceUnits,
                                        onAssignTenantClick = { unitId -> unitIdToAssignLease = unitId },
                                        onCreateUnitClick = { viewModel.setShowCreateUnitDialog(true) },
                                        onEditUnitClick = { unitToEdit = it }
                                    )
                                }
                                AppNavKey.Leases -> NavEntry(key) {
                                    LeasesScreen(
                                        activeResidence = uiState.selectedResidenceContext,
                                        jwtToken = uiState.jwtToken,
                                        residenceUnits = uiState.residenceUnits,
                                        members = uiState.members
                                    )
                                }
                                AppNavKey.Members -> NavEntry(key) {
                                    MembersScreen(
                                        activeResidence = uiState.selectedResidenceContext,
                                        jwtToken = uiState.jwtToken,
                                        onCreateResidenceClick = { viewModel.setShowCreateResidenceDialog(true) }
                                    )
                                }
                                AppNavKey.Electricity -> NavEntry(key) {
                                    ElectricityScreen(
                                        activeResidence = uiState.selectedResidenceContext,
                                        jwtToken = uiState.jwtToken,
                                        residenceUnits = uiState.residenceUnits
                                    )
                                }
                                AppNavKey.Tickets -> NavEntry(key) {
                                    TicketsScreen(
                                        activeResidence = uiState.selectedResidenceContext,
                                        jwtToken = uiState.jwtToken,
                                        residenceUnits = uiState.residenceUnits
                                    )
                                }
                                AppNavKey.Finances -> NavEntry(key) {
                                    FinancesScreen(
                                        activeResidence = uiState.selectedResidenceContext,
                                        jwtToken = uiState.jwtToken
                                    )
                                }
                                AppNavKey.Profile -> NavEntry(key) {
                                    ProfileScreen(
                                        user = uiState.loggedInUser,
                                        firstName = uiState.firstName,
                                        lastName = uiState.lastName,
                                        phone = uiState.phone,
                                        activeResidence = uiState.selectedResidenceContext,
                                        jwtToken = uiState.jwtToken,
                                        onLogout = { viewModel.logout() },
                                        onUserUpdated = { viewModel.updateUserProfile(it) }
                                    )
                                }
                                else -> NavEntry(key) {
                                    DashboardScreen(
                                        activeResidence = uiState.selectedResidenceContext,
                                        firstName = uiState.firstName,
                                        jwtToken = uiState.jwtToken
                                    )
                                }
                            }
                        }
                    )
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
                    onAddResidenceClick = { viewModel.setShowCreateResidenceDialog(true) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Modal Dialogs for Onboarding
    if (uiState.showCreateResidenceDialog) {
        CreateResidenceDialog(
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.setShowCreateResidenceDialog(false) },
            onSubmit = { name, address, defaultCurrency, kWhPrice ->
                viewModel.createResidence(name, address, defaultCurrency, kWhPrice)
            }
        )
    }

    if (uiState.showJoinResidenceDialog) {
        JoinResidenceDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowJoinResidenceDialog(false) },
            onSubmit = { residenceId ->
                viewModel.joinResidence(residenceId)
            }
        )
    }

    if (uiState.showCreateUnitDialog) {
        CreateUnitDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowCreateUnitDialog(false) },
            onSubmit = { name, floor, type, rent, charges, initialIndex, equipmentIds ->
                viewModel.createResidenceUnit(name, floor, type, rent, charges, initialIndex, equipmentIds)
            }
        )
    }

    unitToEdit?.let { unit ->
        EditUnitDialog(
            viewModel = viewModel,
            residenceUnit = unit,
            onDismiss = { unitToEdit = null },
            onSubmit = { name, floor, type, rent, charges, initialIndex, equipmentIds ->
                viewModel.updateResidenceUnit(unit.id, name, floor, type, rent, charges, initialIndex, equipmentIds)
                unitToEdit = null
            }
        )
    }

    unitIdToAssignLease?.let { targetUnitId ->
        LeaseWizardDialog(
            jwtToken = uiState.jwtToken,
            residenceUnits = uiState.residenceUnits,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            initialResidenceUnitId = targetUnitId,
            onDismiss = { unitIdToAssignLease = null },
            onSubmit = { unitId, request ->
                viewModel.createLease(unitId, request) {
                    unitIdToAssignLease = null
                }
            }
        )
    }
}

@Preview
@Composable
fun AppShellPreview() {
    ResidTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Text(
                text = "Resid Manager - App Shell (Preview)",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

