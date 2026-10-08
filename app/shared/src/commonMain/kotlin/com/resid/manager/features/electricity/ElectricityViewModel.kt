package com.resid.manager.features.electricity

import androidx.lifecycle.viewModelScope
import com.resid.manager.base.MviViewModel
import com.resid.manager.features.electricity.mvi.ElectricityEffect
import com.resid.manager.features.electricity.mvi.ElectricityIntent
import com.resid.manager.features.electricity.mvi.ElectricityUiState
import com.resid.manager.features.electricity.usecase.CreateElectricityStatementUseCase
import com.resid.manager.features.electricity.usecase.FetchElectricityStatementsUseCase
import com.resid.manager.features.electricity.usecase.FetchPreviousIndexUseCase
import com.resid.manager.features.electricity.usecase.MarkStatementPaidUseCase
import kotlinx.coroutines.launch

class ElectricityViewModel(
    private val fetchElectricityStatementsUseCase: FetchElectricityStatementsUseCase,
    private val fetchPreviousIndexUseCase: FetchPreviousIndexUseCase,
    private val createElectricityStatementUseCase: CreateElectricityStatementUseCase,
    private val markStatementPaidUseCase: MarkStatementPaidUseCase
) : MviViewModel<ElectricityUiState, ElectricityIntent, ElectricityEffect>(ElectricityUiState()) {

    override fun onIntent(intent: ElectricityIntent) {
        when (intent) {
            is ElectricityIntent.LoadStatements -> loadStatements(intent.token, intent.residenceId)
            is ElectricityIntent.SetStatusFilter -> updateState { it.copy(statusFilter = intent.filter) }
            is ElectricityIntent.SetUnitFilter -> updateState { it.copy(selectedUnitFilterId = intent.unitId) }
            is ElectricityIntent.SetFloorFilter -> updateState { it.copy(floorFilterText = intent.floor) }
            is ElectricityIntent.SetTenantFilter -> updateState { it.copy(tenantFilterText = intent.tenant) }
            is ElectricityIntent.ToggleStatementSelection -> {
                val updated = uiState.value.selectedStatementIds.toMutableMap()
                if (intent.isSelected) updated[intent.statementId] = true else updated.remove(intent.statementId)
                updateState { it.copy(selectedStatementIds = updated) }
            }
            is ElectricityIntent.SetShowCreateDialog -> updateState { it.copy(showCreateDialog = intent.show, errorMessage = null) }
            is ElectricityIntent.SetConfirmingPaymentStatement -> updateState { it.copy(confirmingPaymentStatement = intent.statement) }
            is ElectricityIntent.LoadPreviousIndex -> loadPreviousIndex(intent.token, intent.unitId)
            is ElectricityIntent.CreateStatement -> createStatement(intent)
            is ElectricityIntent.MarkPaid -> markPaid(intent)
        }
    }

    private fun loadStatements(token: String, residenceId: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            fetchElectricityStatementsUseCase(token, residenceId)
                .onSuccess { list ->
                    updateState { it.copy(isLoading = false, statements = list, errorMessage = null) }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun loadPreviousIndex(token: String, unitId: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoadingPreviousIndex = true) }
            fetchPreviousIndexUseCase(token, unitId)
                .onSuccess { lastIdx ->
                    updateState { it.copy(isLoadingPreviousIndex = false, formPreviousIndex = lastIdx) }
                }
                .onFailure {
                    updateState { it.copy(isLoadingPreviousIndex = false, formPreviousIndex = 0.0) }
                }
        }
    }

    private fun createStatement(intent: ElectricityIntent.CreateStatement) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            createElectricityStatementUseCase(
                token = intent.token,
                residenceId = intent.residenceId,
                unitId = intent.unitId,
                previousIndex = intent.previousIndex,
                newIndex = intent.newIndex,
                kWhPrice = intent.kWhPrice,
                statementDate = intent.statementDate
            )
                .onSuccess {
                    updateState { it.copy(showCreateDialog = false, formPreviousIndex = null) }
                    loadStatements(intent.token, intent.residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun markPaid(intent: ElectricityIntent.MarkPaid) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            markStatementPaidUseCase(intent.token, intent.statementId)
                .onSuccess {
                    updateState { it.copy(confirmingPaymentStatement = null) }
                    loadStatements(intent.token, intent.residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}
