package com.resid.manager.features.electricity.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.features.electricity.ElectricityViewModel
import com.resid.manager.features.electricity.mvi.ElectricityIntent
import com.resid.manager.features.electricity.mvi.ElectricityUiState
import com.resid.manager.features.electricity.ui.components.CreateStatementDialog
import com.resid.manager.features.electricity.ui.components.ElectricityFilterBar
import com.resid.manager.features.electricity.ui.components.ElectricityHeader
import com.resid.manager.features.electricity.ui.components.ElectricityStatementsGrid
import com.resid.manager.features.electricity.ui.components.StatementPaymentDialog
import com.resid.manager.network.ApiClient
import com.resid.manager.ui.theme.ResidTheme
import org.koin.compose.koinInject

@Composable
fun ElectricityScreen(
    activeResidence: ResidenceContext? = null,
    viewModel: ElectricityViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(activeResidence?.residenceId) {
        if (activeResidence != null) {
            viewModel.updateActiveResidence(activeResidence)
        }
    }

    val currentResidence = uiState.activeResidence ?: activeResidence
    val effectiveCurrencySymbol = uiState.currencySymbol
    val residenceId = currentResidence?.residenceId.orEmpty()

    ElectricityContent(
        uiState = uiState,
        residenceUnits = uiState.residenceUnits,
        currencySymbol = effectiveCurrencySymbol,
        isAuthorized = uiState.isAuthorized,
        onAddStatementClick = { viewModel.onIntent(ElectricityIntent.SetShowCreateDialog(true)) },
        onEcoPrintClick = {
            val selectedKeys = uiState.selectedStatementIds.filter { it.value }.keys.joinToString(",")
            val url = "${ApiClient.BASE_URL}/api/residences/$residenceId/electricity/export-pdf" + 
                if (selectedKeys.isNotBlank()) "?ids=$selectedKeys" else ""
            uriHandler.openUri(url)
        },
        onStatusFilterChanged = { viewModel.onIntent(ElectricityIntent.SetStatusFilter(it)) },
        onFloorFilterChanged = { viewModel.onIntent(ElectricityIntent.SetFloorFilter(it)) },
        onTenantFilterChanged = { viewModel.onIntent(ElectricityIntent.SetTenantFilter(it)) },
        onToggleSelection = { id, selected -> viewModel.onIntent(ElectricityIntent.ToggleStatementSelection(id, selected)) },
        onMarkPaidClick = { stmt -> viewModel.onIntent(ElectricityIntent.SetConfirmingPaymentStatement(stmt)) }
    )

    if (uiState.showCreateDialog) {
        CreateStatementDialog(
            residenceUnits = uiState.residenceUnits,
            formPreviousIndex = uiState.formPreviousIndex,
            isLoadingPreviousIndex = uiState.isLoadingPreviousIndex,
            errorMessage = uiState.errorMessage,
            defaultKWhPrice = currentResidence?.kWhPrice ?: 150.0,
            currencySymbol = effectiveCurrencySymbol,
            onSelectUnitForPreviousIndex = { unitId ->
                viewModel.onIntent(ElectricityIntent.LoadPreviousIndex(unitId))
            },
            onDismiss = { viewModel.onIntent(ElectricityIntent.SetShowCreateDialog(false)) },
            onSubmit = { unitId, oldIdx, newIdx, price, date ->
                viewModel.onIntent(
                    ElectricityIntent.CreateStatement(
                        unitId = unitId,
                        previousIndex = oldIdx,
                        newIndex = newIdx,
                        kWhPrice = price,
                        statementDate = date
                    )
                )
            }
        )
    }

    if (uiState.confirmingPaymentStatement != null) {
        val stmt = uiState.confirmingPaymentStatement!!
        StatementPaymentDialog(
            statement = stmt,
            currencySymbol = effectiveCurrencySymbol,
            onDismiss = { viewModel.onIntent(ElectricityIntent.SetConfirmingPaymentStatement(null)) },
            onConfirm = {
                viewModel.onIntent(ElectricityIntent.MarkPaid(stmt.id))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ElectricityContent(
    uiState: ElectricityUiState,
    residenceUnits: List<ResidenceUnitDto>,
    currencySymbol: String,
    isAuthorized: Boolean,
    onAddStatementClick: () -> Unit,
    onEcoPrintClick: () -> Unit,
    onStatusFilterChanged: (String) -> Unit,
    onFloorFilterChanged: (String) -> Unit,
    onTenantFilterChanged: (String) -> Unit,
    onToggleSelection: (String, Boolean) -> Unit,
    onMarkPaidClick: (ElectricityStatementDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredStatements = remember(
        uiState.statements,
        uiState.statusFilter,
        uiState.floorFilterText,
        uiState.tenantFilterText,
        residenceUnits
    ) {
        uiState.statements.filter { stmt ->
            // Filtre par statut (ALL, UNPAID, PAID)
            val matchStatus = when (uiState.statusFilter) {
                "UNPAID" -> stmt.status.name == "UNPAID"
                "PAID" -> stmt.status.name == "PAID"
                else -> true
            }

            val matchedUnit = residenceUnits.firstOrNull { it.id == stmt.unitId }

            // Filtre par étage
            val matchFloor = if (uiState.floorFilterText.isNotBlank()) {
                matchedUnit?.floor?.contains(uiState.floorFilterText.trim(), ignoreCase = true) == true
            } else true

            // Filtre par locataire
            val matchTenant = if (uiState.tenantFilterText.isNotBlank()) {
                matchedUnit?.currentTenantName?.contains(uiState.tenantFilterText.trim(), ignoreCase = true) == true
            } else true

            matchStatus && matchFloor && matchTenant
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(maxWidth, maxHeight)
        )
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact || maxWidth < 600.dp
        ElectricityStatementsGrid(
            statements = filteredStatements,
            residenceUnits = residenceUnits,
            currencySymbol = currencySymbol,
            selectedStatementIds = uiState.selectedStatementIds,
            isAuthorized = isAuthorized,
            isLoading = uiState.isLoading,
            onToggleSelection = onToggleSelection,
            onMarkPaidClick = onMarkPaidClick,
            modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
            headerContent = {
                ElectricityHeader(
                    isAuthorized = isAuthorized,
                    selectedCount = uiState.selectedStatementIds.count { it.value },
                    onAddStatementClick = onAddStatementClick,
                    onEcoPrintClick = onEcoPrintClick
                )
            },
            filterContent = {
                ElectricityFilterBar(
                    statusFilter = uiState.statusFilter,
                    floorFilterText = uiState.floorFilterText,
                    tenantFilterText = uiState.tenantFilterText,
                    isCompact = isCompact,
                    onStatusFilterChanged = onStatusFilterChanged,
                    onFloorFilterChanged = onFloorFilterChanged,
                    onTenantFilterChanged = onTenantFilterChanged
                )
            }
        )
    }
}

@PreviewLightDark
@Composable
fun ElectricityContentPreview() {
    ResidTheme {
        ElectricityContent(
            uiState = ElectricityUiState(),
            residenceUnits = emptyList(),
            currencySymbol = "FCFA",
            isAuthorized = true,
            onAddStatementClick = {},
            onEcoPrintClick = {},
            onStatusFilterChanged = {},
            onFloorFilterChanged = {},
            onTenantFilterChanged = {},
            onToggleSelection = { _, _ -> },
            onMarkPaidClick = {}
        )
    }
}