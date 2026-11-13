package com.resid.manager.features.electricity.mvi

import com.resid.manager.dto.AppCurrency
import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceUnitDto

data class ElectricityUiState(
    val isLoading: Boolean = false,
    val activeResidence: ResidenceContext? = null,
    val isAuthorized: Boolean = false,
    val currencySymbol: String = AppCurrency.DEFAULT.symbol,
    val residenceUnits: List<ResidenceUnitDto> = emptyList(),
    val statements: List<ElectricityStatementDto> = emptyList(),
    val statusFilter: String = "ALL", // "ALL", "UNPAID", "PAID"
    val selectedUnitFilterId: String = "",
    val floorFilterText: String = "",
    val tenantFilterText: String = "",
    val selectedStatementIds: Map<String, Boolean> = emptyMap(),
    val showCreateDialog: Boolean = false,
    val confirmingPaymentStatement: ElectricityStatementDto? = null,
    val formPreviousIndex: Double? = null,
    val isLoadingPreviousIndex: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ElectricityIntent {
    data object Refresh : ElectricityIntent
    data class LoadStatements(val residenceId: String) : ElectricityIntent
    data class SetStatusFilter(val filter: String) : ElectricityIntent
    data class SetUnitFilter(val unitId: String) : ElectricityIntent
    data class SetFloorFilter(val floor: String) : ElectricityIntent
    data class SetTenantFilter(val tenant: String) : ElectricityIntent
    data class ToggleStatementSelection(val statementId: String, val isSelected: Boolean) : ElectricityIntent
    data class SetShowCreateDialog(val show: Boolean) : ElectricityIntent
    data class SetConfirmingPaymentStatement(val statement: ElectricityStatementDto?) : ElectricityIntent
    data class LoadPreviousIndex(val unitId: String) : ElectricityIntent

    data class CreateStatement(
        val unitId: String,
        val previousIndex: Double,
        val newIndex: Double,
        val kWhPrice: Double,
        val statementDate: String
    ) : ElectricityIntent

    data class MarkPaid(
        val statementId: String
    ) : ElectricityIntent
}

sealed interface ElectricityEffect {
    data class ShowToast(val message: String) : ElectricityEffect
    data class OpenPdfUrl(val url: String) : ElectricityEffect
}
