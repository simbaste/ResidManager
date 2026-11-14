package com.resid.manager.features.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.DashboardDataDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.features.dashboard.DashboardViewModel
import com.resid.manager.features.dashboard.mvi.DashboardIntent
import com.resid.manager.features.dashboard.mvi.DashboardUiState
import com.resid.manager.features.dashboard.ui.components.DashboardBentoKpis
import com.resid.manager.features.dashboard.ui.components.DashboardChartsSection
import com.resid.manager.features.dashboard.ui.components.DashboardDelinquencyAlert
import com.resid.manager.features.dashboard.ui.components.DashboardEmptyState
import com.resid.manager.features.dashboard.ui.components.DashboardPeriodFilterBar
import com.resid.manager.features.dashboard.ui.components.DashboardQuickActions
import com.resid.manager.features.dashboard.ui.components.DashboardWelcomeBanner
import com.resid.manager.ui.theme.ResidTheme
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun DashboardScreen(
    activeResidence: ResidenceContext? = null,
    viewModel: DashboardViewModel = koinInject(),
    onCreateResidenceClick: () -> Unit = {},
    onJoinResidenceClick: () -> Unit = {},
    onNewUnitClick: () -> Unit = {},
    onNewLeaseClick: () -> Unit = {},
    onNewExpenseClick: () -> Unit = {},
    onViewReportsClick: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
        val isLarge = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(activeResidence?.residenceId) {
            if (activeResidence != null) {
                viewModel.updateActiveResidence(activeResidence)
            }
        }

        val currentResidence = uiState.activeResidence ?: activeResidence

        if (currentResidence == null) {
            DashboardEmptyState(
                onCreateResidence = onCreateResidenceClick,
                onJoinResidence = onJoinResidenceClick
            )
        } else {
            DashboardContent(
                uiState = uiState,
                activeResidence = currentResidence,
                isCompact = isCompact,
                onPeriodFilterChanged = { filter ->
                    viewModel.onIntent(DashboardIntent.ChangePeriodFilter(filter))
                },
                onCustomStartChanged = { date ->
                    viewModel.onIntent(DashboardIntent.UpdateCustomStart(date))
                },
                onCustomEndChanged = { date ->
                    viewModel.onIntent(DashboardIntent.UpdateCustomEnd(date))
                },
                onNewUnitClick = onNewUnitClick,
                onNewLeaseClick = onNewLeaseClick,
                onNewExpenseClick = onNewExpenseClick,
                onViewReportsClick = onViewReportsClick
            )
        }
    }

}

@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    activeResidence: ResidenceContext,
    isCompact: Boolean,
    onPeriodFilterChanged: (String) -> Unit,
    onCustomStartChanged: (String) -> Unit,
    onCustomEndChanged: (String) -> Unit,
    onNewUnitClick: () -> Unit,
    onNewLeaseClick: () -> Unit,
    onNewExpenseClick: () -> Unit,
    onViewReportsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data = uiState.dashboardData

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Welcome Banner Section
        DashboardWelcomeBanner(
            isCompact = isCompact,
            firstName = uiState.firstName.ifBlank { "Utilisateur" },
            residenceName = activeResidence.residenceName,
            roleName = activeResidence.userRoleInResidence.name
        )

        // 2. Real-Time Period Selection Bar
        DashboardPeriodFilterBar(
            isCompact = isCompact,
            periodFilter = uiState.periodFilter,
            customStartText = uiState.customStartText,
            customEndText = uiState.customEndText,
            onPeriodFilterChanged = onPeriodFilterChanged,
            onCustomStartChanged = onCustomStartChanged,
            onCustomEndChanged = onCustomEndChanged
        )

        // 3. High delinquency rate alert (> 10%)
        if (data != null) {
            DashboardDelinquencyAlert(delinquencyRate = data.delinquencyRate)
        }

        // 4. Loading or KPI Cards
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF006948))
            }
        } else if (data != null) {
            DashboardBentoKpis(
                isCompact = isCompact,
                data = data,
                currencySymbol = activeResidence.currencySymbol
            )
        }

        // 5. Quick Actions Panel
        DashboardQuickActions(
            isCompact = isCompact,
            onNewLeaseClick = onNewLeaseClick,
            onNewUnitClick = onNewUnitClick,
            onNewExpenseClick = onNewExpenseClick
        )

        // 6. Dynamic Chart Section & Optimisation Card
        DashboardChartsSection(
            isCompact = isCompact,
            data = data,
            onViewReportsClick = onViewReportsClick
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardContentPreview() {
    val sampleResidence = ResidenceContext(
        residenceId = "residence-1",
        residenceName = "Résidence Les Palmiers",
        residenceAddress = "Cocody Danga, Abidjan",
        userRoleInResidence = UserRole.OWNER,
        totalUnits = 12,
        currencySymbol = "FCFA",
        currencyCode = "XOF",
        kWhPrice = 150.0
    )

    val sampleUiState = DashboardUiState(
        isLoading = false,
        firstName = "Amadou",
        activeResidence = sampleResidence,
        dashboardData = DashboardDataDto(
            residenceId = "residence-1",
            totalRevenuesCollected = 1250000.0,
            totalExpensesIncurred = 450000.0,
            netCashflow = 800000.0,
            delinquencyRate = 4.5,
            occupancyRate = 92.0
        ),
        periodFilter = "MONTH",
        customStartText = "2025-02-01",
        customEndText = "2025-02-28"
    )

    ResidTheme(darkTheme = false) {
        DashboardContent(
            uiState = sampleUiState,
            activeResidence = sampleResidence,
            isCompact = true,
            onPeriodFilterChanged = {},
            onCustomStartChanged = {},
            onCustomEndChanged = {},
            onNewUnitClick = {},
            onNewLeaseClick = {},
            onNewExpenseClick = {},
            onViewReportsClick = {}
        )
    }
}

