package com.resid.manager.features.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
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
import org.koin.compose.koinInject

@Composable
fun DashboardScreen(
    activeResidence: ResidenceContext?,
    firstName: String,
    jwtToken: String?,
    viewModel: DashboardViewModel = koinInject(),
    onCreateResidenceClick: () -> Unit = {},
    onJoinResidenceClick: () -> Unit = {},
    onNewUnitClick: () -> Unit = {},
    onNewLeaseClick: () -> Unit = {},
    onNewExpenseClick: () -> Unit = {},
    onViewReportsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(activeResidence?.residenceId, jwtToken) {
        val token = jwtToken ?: return@LaunchedEffect
        val residenceId = activeResidence?.residenceId ?: return@LaunchedEffect
        viewModel.onIntent(DashboardIntent.LoadData(token, residenceId))
    }

    if (activeResidence == null) {
        DashboardEmptyState(
            onCreateResidence = onCreateResidenceClick,
            onJoinResidence = onJoinResidenceClick
        )
    } else {
        DashboardContent(
            uiState = uiState,
            activeResidence = activeResidence,
            firstName = firstName,
            onPeriodFilterChanged = { filter ->
                val token = jwtToken ?: return@DashboardContent
                viewModel.onIntent(DashboardIntent.ChangePeriodFilter(filter, token, activeResidence.residenceId))
            },
            onCustomStartChanged = { date ->
                val token = jwtToken ?: return@DashboardContent
                viewModel.onIntent(DashboardIntent.UpdateCustomStart(date, token, activeResidence.residenceId))
            },
            onCustomEndChanged = { date ->
                val token = jwtToken ?: return@DashboardContent
                viewModel.onIntent(DashboardIntent.UpdateCustomEnd(date, token, activeResidence.residenceId))
            },
            onNewUnitClick = onNewUnitClick,
            onNewLeaseClick = onNewLeaseClick,
            onNewExpenseClick = onNewExpenseClick,
            onViewReportsClick = onViewReportsClick
        )
    }
}

@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    activeResidence: ResidenceContext,
    firstName: String,
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
            firstName = firstName,
            residenceName = activeResidence.residenceName,
            roleName = activeResidence.userRoleInResidence.name
        )

        // 2. Real-Time Period Selection Bar
        DashboardPeriodFilterBar(
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
                data = data,
                currencySymbol = activeResidence.currencySymbol
            )
        }

        // 5. Quick Actions Panel
        DashboardQuickActions(
            onNewLeaseClick = onNewLeaseClick,
            onNewUnitClick = onNewUnitClick,
            onNewExpenseClick = onNewExpenseClick
        )

        // 6. Dynamic Chart Section & Optimisation Card
        DashboardChartsSection(
            data = data,
            onViewReportsClick = onViewReportsClick
        )
    }
}
