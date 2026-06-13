package com.resid.manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

                // Central Dynamic Panel Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (uiState.currentAppScreen) {
                        AppScreen.DASHBOARD -> DashboardPage(viewModel = viewModel)
                        AppScreen.RESIDENCES -> ResidencesPage(viewModel = viewModel)
                        AppScreen.LOGEMENTS -> LogementsPage(viewModel = viewModel)
                        AppScreen.BAUX -> LeasesPage(viewModel = viewModel)
                        AppScreen.MEMBERS -> MembersPage(viewModel = viewModel)
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
                    onAddResidenceClick = { viewModel.setShowCreateResidenceDialog(true) },
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
            onDismiss = { viewModel.setShowJoinResidenceDialog(false) },
            onSubmit = { residenceId ->
                viewModel.joinResidence(residenceId)
            }
        )
    }

    if (uiState.showCreateLogementDialog) {
        CreateLogementDialog(
            onDismiss = { viewModel.setShowCreateLogementDialog(false) },
            onSubmit = { name, floor, type, rent, charges, initialIndex ->
                viewModel.createLogement(name, floor, type, rent, charges, initialIndex)
            }
        )
    }
}
