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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.resid.manager.dto.AppCurrency
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.features.dashboard.ui.DashboardScreen
import com.resid.manager.features.electricity.ui.ElectricityScreen
import com.resid.manager.features.finances.ui.FinancesScreen
import com.resid.manager.features.leases.ui.LeasesScreen
import com.resid.manager.features.leases.ui.components.wizard.LeaseWizardScreen
import com.resid.manager.features.members.ui.MembersScreen
import com.resid.manager.features.profile.ui.ProfileScreen
import com.resid.manager.features.residences.ui.ResidencesScreen
import com.resid.manager.features.residences.ui.components.CreateResidenceContent
import com.resid.manager.features.residences.ui.components.JoinResidenceContent
import com.resid.manager.features.residences.usecase.PhotoUploadItem
import com.resid.manager.features.tickets.ui.TicketsScreen
import com.resid.manager.features.units.ui.UnitsScreen
import com.resid.manager.features.units.ui.components.CreateUnitContent
import com.resid.manager.navigation.AdaptiveModalSceneStrategy
import com.resid.manager.navigation.AppNavKey
import com.resid.manager.navigation.PlatformBackHandler
import com.resid.manager.navigation.adaptiveModal
import com.resid.manager.ui.theme.ResidTheme
import com.resid.manager.viewmodel.AppScreen
import com.resid.manager.viewmodel.LoginViewModel
import org.koin.compose.koinInject

@OptIn(
    ExperimentalMaterial3WindowSizeClassApi::class,
    ExperimentalMaterial3Api::class
)
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

        Row(modifier = Modifier.fillMaxSize()) {
            // Left Sidebar (Desktop version, fixed)
            if (isDesktop) {
                SidebarContent(
                    selectedScreen = uiState.currentAppScreen,
                    onScreenSelected = { screen ->
                        viewModel.navigateToAppScreen(screen)
                    },
                    onAddResidenceClick = {
                        if (innerBackStack.lastOrNull() != AppNavKey.CreateResidence) {
                            innerBackStack.add(AppNavKey.CreateResidence)
                        }
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
                    userRole = uiState.selectedResidenceContext?.userRoleInResidence?.name,
                    residences = uiState.residences,
                    selectedResidence = uiState.selectedResidenceContext,
                    onResidenceSelected = { viewModel.selectResidence(it) },
                    onMenuClick = { isMobileSidebarVisible = !isMobileSidebarVisible },
                    onLogoutClick = { viewModel.logout() },
                    onProfileClick = { viewModel.navigateToAppScreen(AppScreen.PROFILE) },
                    onAddResidenceClick = {
                        if (innerBackStack.lastOrNull() != AppNavKey.CreateResidence) {
                            innerBackStack.add(AppNavKey.CreateResidence)
                        }
                    },
                    onJoinResidenceClick = {
                        if (innerBackStack.lastOrNull() != AppNavKey.JoinResidence) {
                            innerBackStack.add(AppNavKey.JoinResidence)
                        }
                    },
                    isDarkTheme = uiState.darkMode,
                    onToggleTheme = { viewModel.toggleTheme() },
                    avatarUrl = uiState.loggedInUser?.avatarUrl
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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

                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val windowSizeClass = WindowSizeClass.calculateFromSize(
                        DpSize(maxWidth, maxHeight)
                    )
                    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact || maxWidth < 600.dp

                    NavDisplay(
                        backStack = innerBackStack,
                        sceneStrategies = listOf(
                            remember(isCompact) {
                                AdaptiveModalSceneStrategy(isCompact = isCompact)
                            }
                        ),
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
                                        onCreateResidenceClick = {
                                            if (innerBackStack.lastOrNull() != AppNavKey.CreateResidence) {
                                                innerBackStack.add(AppNavKey.CreateResidence)
                                            }
                                        },
                                        onJoinResidenceClick = {
                                            if (innerBackStack.lastOrNull() != AppNavKey.JoinResidence) {
                                                innerBackStack.add(AppNavKey.JoinResidence)
                                            }
                                        },
                                        onNewUnitClick = {
                                            if (innerBackStack.lastOrNull() != AppNavKey.CreateUnit) {
                                                innerBackStack.add(AppNavKey.CreateUnit)
                                            }
                                        },
                                        onNewLeaseClick = { viewModel.navigateToAppScreen(AppScreen.LEASES) },
                                        onNewExpenseClick = { viewModel.navigateToAppScreen(AppScreen.FINANCES) },
                                        onViewReportsClick = { viewModel.navigateToAppScreen(AppScreen.FINANCES) }
                                    )
                                }
                                AppNavKey.Residences -> NavEntry(key) {
                                    ResidencesScreen(
                                        activeResidenceId = uiState.selectedResidenceContext?.residenceId,
                                        onNavigateToDashboard = { residence ->
                                            viewModel.selectResidence(residence)
                                        },
                                        onResidenceActivated = { residence ->
                                            viewModel.selectResidence(residence)
                                        },
                                        onResidenceCreated = { newResidenceId ->
                                            viewModel.fetchResidences(autoSelectResidenceId = newResidenceId)
                                        },
                                        onNavigateToUnits = {
                                            viewModel.navigateToAppScreen(AppScreen.UNITS)
                                        }
                                    )
                                }
                                AppNavKey.Units -> NavEntry(key) {
                                    UnitsScreen(
                                        activeResidence = uiState.selectedResidenceContext,
                                        onAssignTenantClick = { unitId -> unitIdToAssignLease = unitId },
                                        onNavigateToLease = { _ ->
                                            viewModel.navigateToAppScreen(AppScreen.LEASES)
                                        },
                                        onEditUnitClick = { unitToEdit = it }
                                    )
                                }
                                AppNavKey.Leases -> NavEntry(key) {
                                    LeasesScreen(
                                        activeResidence = uiState.selectedResidenceContext
                                    )
                                }
                                AppNavKey.Members -> NavEntry(key) {
                                    MembersScreen(
                                        activeResidence = uiState.selectedResidenceContext,
                                        onCreateResidenceClick = { viewModel.setShowCreateResidenceDialog(true) }
                                    )
                                }
                                AppNavKey.Electricity -> NavEntry(key) {
                                    ElectricityScreen(
                                        activeResidence = uiState.selectedResidenceContext
                                    )
                                }
                                AppNavKey.Tickets -> NavEntry(key) {
                                    TicketsScreen(
                                        activeResidence = uiState.selectedResidenceContext
                                    )
                                }
                                AppNavKey.Finances -> NavEntry(key) {
                                    FinancesScreen(
                                        activeResidence = uiState.selectedResidenceContext
                                    )
                                }
                                AppNavKey.Profile -> NavEntry(key) {
                                    ProfileScreen(
                                        user = uiState.loggedInUser,
                                        activeResidence = uiState.selectedResidenceContext,
                                        onLogout = { viewModel.logout() },
                                        onUserUpdated = { viewModel.updateUserProfile(it) },
                                        onResidenceCurrencyUpdated = { resId, curr -> viewModel.onResidenceCurrencyUpdated(resId, curr) }
                                    )
                                }
                                AppNavKey.CreateUnit -> NavEntry(
                                    key = key,
                                    metadata = adaptiveModal()
                                ) {
                                    CreateUnitContent(
                                        currencySymbol = uiState.selectedResidenceContext?.currencySymbol ?: "XOF",
                                        isLoading = uiState.isLoading,
                                        errorMessage = uiState.errorMessage,
                                        onDismiss = {
                                            if (innerBackStack.size > 1) {
                                                innerBackStack.removeLastOrNull()
                                            }
                                        },
                                        onSubmit = { name, floor, type, rent, charges, initialIndex, equipmentIds, photos ->
                                            val uploadItems = photos.map {
                                                PhotoUploadItem(
                                                    url = it.url,
                                                    bytes = it.bytes,
                                                    description = it.description
                                                )
                                            }
                                            viewModel.createResidenceUnit(
                                                name = name,
                                                floor = floor,
                                                type = type,
                                                nominalRent = rent,
                                                serviceCharges = charges,
                                                initialElectricityIndex = initialIndex,
                                                equipmentIds = equipmentIds,
                                                photos = uploadItems,
                                                onSuccess = {
                                                    if (innerBackStack.size > 1) {
                                                        innerBackStack.removeLastOrNull()
                                                    }
                                                }
                                            )
                                        }
                                    )
                                }
                                AppNavKey.CreateResidence -> NavEntry(
                                    key = key,
                                    metadata = adaptiveModal()
                                ) {
                                    CreateResidenceContent(
                                        isLoading = uiState.isLoading,
                                        errorMessage = uiState.errorMessage,
                                        onDismiss = {
                                            if (innerBackStack.size > 1) {
                                                innerBackStack.removeLastOrNull()
                                            }
                                        },
                                        onSubmit = { name, address, defaultCurrency, kWhPrice, photos ->
                                            val uploadItems = photos.map {
                                                PhotoUploadItem(
                                                    url = it.url,
                                                    bytes = it.bytes,
                                                    description = it.description
                                                )
                                            }
                                            viewModel.createResidence(name, address, defaultCurrency, kWhPrice, uploadItems)
                                            if (innerBackStack.size > 1) {
                                                innerBackStack.removeLastOrNull()
                                            }
                                        }
                                    )
                                }
                                AppNavKey.JoinResidence -> NavEntry(
                                    key = key,
                                    metadata = adaptiveModal()
                                ) {
                                    val existingResidenceIds = remember(uiState.residences) {
                                        uiState.residences.map { it.residenceId }.toSet()
                                    }
                                    JoinResidenceContent(
                                        searchQuery = uiState.searchQuery,
                                        searchResults = uiState.searchResults,
                                        isSearching = uiState.isSearching,
                                        isLoading = uiState.isLoading,
                                        errorMessage = uiState.errorMessage,
                                        existingResidenceIds = existingResidenceIds,
                                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                                        onDismiss = {
                                            if (innerBackStack.size > 1) {
                                                innerBackStack.removeLastOrNull()
                                            }
                                        },
                                        onSubmit = { residenceId ->
                                            viewModel.joinResidence(residenceId)
                                            if (innerBackStack.size > 1) {
                                                innerBackStack.removeLastOrNull()
                                            }
                                        }
                                    )
                                }
                                else -> NavEntry(key) {
                                    DashboardScreen(
                                        activeResidence = uiState.selectedResidenceContext
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
                    onAddResidenceClick = {
                        isMobileSidebarVisible = false
                        if (innerBackStack.lastOrNull() != AppNavKey.CreateResidence) {
                            innerBackStack.add(AppNavKey.CreateResidence)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Modal Dialogs for Onboarding
    unitToEdit?.let { unit ->
        EditUnitDialog(
            residenceUnit = unit,
            availableEquipments = uiState.availableEquipments,
            currencySymbol = uiState.selectedResidenceContext?.currencySymbol ?: "XOF",
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onDismiss = { unitToEdit = null },
            onSubmit = { name, floor, type, rent, charges, initialIndex, equipmentIds ->
                viewModel.updateResidenceUnit(unit.id, name, floor, type, rent, charges, initialIndex, equipmentIds)
                unitToEdit = null
            }
        )
    }

    unitIdToAssignLease?.let { targetUnitId ->
        // the app shell is not handling draft ID for now, let's keep it simple
        LeaseWizardScreen(
            jwtToken = uiState.jwtToken,
            residenceUnits = uiState.residenceUnits,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            currencySymbol = uiState.selectedResidenceContext?.currencySymbol ?: AppCurrency.DEFAULT.symbol,
            initialResidenceUnitId = targetUnitId,
            draftLeaseId = null,
            onNavigateBack = { unitIdToAssignLease = null },
            onSaveDraftStep = { request ->
                // Draft save logic from appshell
            },
            onFinalizeSignature = { leaseId ->
                // Finalize logic
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

