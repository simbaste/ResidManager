package com.resid.manager.features.electricity.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ElectricityStatusDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.UserRole
import com.resid.manager.features.electricity.ElectricityViewModel
import com.resid.manager.features.electricity.mvi.ElectricityIntent
import com.resid.manager.features.electricity.mvi.ElectricityUiState
import com.resid.manager.features.electricity.ui.components.CreateStatementDialog
import com.resid.manager.features.electricity.ui.components.ElectricityFilterBar
import com.resid.manager.features.electricity.ui.components.ElectricityHeader
import com.resid.manager.features.electricity.ui.components.ElectricityStatementsGrid
import com.resid.manager.features.electricity.ui.components.StatementPaymentDialog
import com.resid.manager.network.ApiClient
import org.koin.compose.koinInject

@Composable
fun ElectricityScreen(
    activeResidence: ResidenceContext?,
    jwtToken: String?,
    residenceUnits: List<ResidenceUnitDto>,
    viewModel: ElectricityViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val token = jwtToken ?: ""
    val residenceId = activeResidence?.residenceId ?: ""
    val isAuthorized = activeResidence != null && (activeResidence.userRoleInResidence == UserRole.ADMIN || activeResidence.userRoleInResidence == UserRole.MANAGER)
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(token, residenceId) {
        if (token.isNotBlank() && residenceId.isNotBlank()) {
            viewModel.onIntent(ElectricityIntent.LoadStatements(token, residenceId))
        }
    }

    ElectricityContent(
        uiState = uiState,
        residenceUnits = residenceUnits,
        currencySymbol = activeResidence?.currencySymbol ?: "XOF",
        isAuthorized = isAuthorized,
        onAddStatementClick = { viewModel.onIntent(ElectricityIntent.SetShowCreateDialog(true)) },
        onEcoPrintClick = {
            val selectedKeys = uiState.selectedStatementIds.filter { it.value }.keys.joinToString(",")
            val url = "${ApiClient.BASE_URL}/api/residences/$residenceId/electricity/statements/pdf-batch?ids=$selectedKeys"
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
            residenceUnits = residenceUnits,
            formPreviousIndex = uiState.formPreviousIndex,
            isLoadingPreviousIndex = uiState.isLoadingPreviousIndex,
            errorMessage = uiState.errorMessage,
            onSelectUnitForPreviousIndex = { unitId ->
                viewModel.onIntent(ElectricityIntent.LoadPreviousIndex(token, unitId))
            },
            onDismiss = { viewModel.onIntent(ElectricityIntent.SetShowCreateDialog(false)) },
            onSubmit = { unitId, oldIdx, newIdx, price, date ->
                viewModel.onIntent(
                    ElectricityIntent.CreateStatement(
                        token = token,
                        residenceId = residenceId,
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
            currencySymbol = activeResidence?.currencySymbol ?: "XOF",
            onDismiss = { viewModel.onIntent(ElectricityIntent.SetConfirmingPaymentStatement(null)) },
            onConfirm = {
                viewModel.onIntent(ElectricityIntent.MarkPaid(token, residenceId, stmt.id))
            }
        )
    }
}

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
    onMarkPaidClick: (com.resid.manager.dto.ElectricityStatementDto) -> Unit,
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
            val matchStatus = when (uiState.statusFilter) {
                "PAID" -> stmt.status == ElectricityStatusDto.PAID
                "UNPAID" -> stmt.status == ElectricityStatusDto.UNPAID
                else -> true
            }

            val matchedUnit = residenceUnits.firstOrNull { it.id == stmt.unitId }
            val matchFloor = if (uiState.floorFilterText.isNotEmpty()) {
                matchedUnit?.floor?.contains(uiState.floorFilterText, ignoreCase = true) == true
            } else true

            val matchTenant = if (uiState.tenantFilterText.isNotEmpty()) {
                matchedUnit?.name?.contains(uiState.tenantFilterText, ignoreCase = true) == true
            } else true

            matchStatus && matchFloor && matchTenant
        }
    }

    val selectedCount = uiState.selectedStatementIds.count { it.value }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ElectricityHeader(
            isAuthorized = isAuthorized,
            selectedCount = selectedCount,
            onAddStatementClick = onAddStatementClick,
            onEcoPrintClick = onEcoPrintClick
        )

        ElectricityFilterBar(
            statusFilter = uiState.statusFilter,
            floorFilterText = uiState.floorFilterText,
            tenantFilterText = uiState.tenantFilterText,
            onStatusFilterChanged = onStatusFilterChanged,
            onFloorFilterChanged = onFloorFilterChanged,
            onTenantFilterChanged = onTenantFilterChanged
        )

        ElectricityStatementsGrid(
            statements = filteredStatements,
            residenceUnits = residenceUnits,
            currencySymbol = currencySymbol,
            selectedStatementIds = uiState.selectedStatementIds,
            isAuthorized = isAuthorized,
            isLoading = uiState.isLoading,
            onToggleSelection = onToggleSelection,
            onMarkPaidClick = onMarkPaidClick,
            modifier = Modifier.weight(1f)
        )
    }
}
