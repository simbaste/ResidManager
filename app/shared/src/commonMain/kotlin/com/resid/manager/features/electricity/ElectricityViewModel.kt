package com.resid.manager.features.electricity

import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.base.MviViewModel
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.features.electricity.mvi.ElectricityEffect
import com.resid.manager.features.electricity.mvi.ElectricityIntent
import com.resid.manager.features.electricity.mvi.ElectricityUiState
import com.resid.manager.features.electricity.usecase.CreateElectricityStatementUseCase
import com.resid.manager.features.electricity.usecase.FetchElectricityStatementsUseCase
import com.resid.manager.features.electricity.usecase.FetchPreviousIndexUseCase
import com.resid.manager.features.electricity.usecase.MarkStatementPaidUseCase
import com.resid.manager.features.residences.usecase.FetchResidencesUseCase
import com.resid.manager.features.units.usecase.FetchUnitsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ElectricityViewModel(
    private val fetchElectricityStatementsUseCase: FetchElectricityStatementsUseCase,
    private val fetchPreviousIndexUseCase: FetchPreviousIndexUseCase,
    private val createElectricityStatementUseCase: CreateElectricityStatementUseCase,
    private val markStatementPaidUseCase: MarkStatementPaidUseCase,
    private val fetchResidencesUseCase: FetchResidencesUseCase,
    private val fetchUnitsUseCase: FetchUnitsUseCase,
    private val sessionStorage: SessionStorage? = null
) : MviViewModel<ElectricityUiState, ElectricityIntent, ElectricityEffect>(ElectricityUiState()) {

    init {
        val lastSelectedId = sessionStorage?.loadLastSelectedResidenceId()
        if (!lastSelectedId.isNullOrBlank()) {
            loadResidenceAndStatements(lastSelectedId)
        }
    }

    override fun onIntent(intent: ElectricityIntent) {
        when (intent) {
            is ElectricityIntent.Refresh -> {
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (!residenceId.isNullOrBlank()) {
                    loadStatements(residenceId)
                }
            }
            is ElectricityIntent.LoadStatements -> loadResidenceAndStatements(intent.residenceId)
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
            is ElectricityIntent.LoadPreviousIndex -> loadPreviousIndex(intent.unitId)
            is ElectricityIntent.CreateStatement -> createStatement(intent)
            is ElectricityIntent.MarkPaid -> markPaid(intent.statementId)
        }
    }

    fun updateActiveResidence(residence: ResidenceContext?) {
        if (residence == null) {
            updateState {
                it.copy(
                    activeResidence = null,
                    isAuthorized = false,
                    currencySymbol = com.resid.manager.dto.AppCurrency.DEFAULT.symbol,
                    residenceUnits = emptyList(),
                    statements = emptyList()
                )
            }
            return
        }
        val isAuth = residence.userRoleInResidence == UserRole.OWNER ||
            residence.userRoleInResidence == UserRole.ADMIN ||
            residence.userRoleInResidence == UserRole.MANAGER
        updateState {
            it.copy(
                activeResidence = residence,
                isAuthorized = isAuth,
                currencySymbol = residence.currencySymbol
            )
        }
        loadStatements(residence.residenceId)
    }

    private fun loadResidenceAndStatements(residenceId: String) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            if (uiState.value.activeResidence == null || uiState.value.activeResidence?.residenceId != residenceId) {
                fetchResidencesUseCase(session.token).onSuccess { list ->
                    val matched = list.firstOrNull { it.residenceId == residenceId }
                    if (matched != null) {
                        val isAuth = matched.userRoleInResidence == UserRole.OWNER ||
                            matched.userRoleInResidence == UserRole.ADMIN ||
                            matched.userRoleInResidence == UserRole.MANAGER
                        updateState {
                            it.copy(
                                activeResidence = matched,
                                isAuthorized = isAuth,
                                currencySymbol = matched.currencySymbol
                            )
                        }
                    }
                }
            }
            loadStatements(residenceId)
        }
    }

    private fun loadStatements(residenceId: String) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val statementsDeferred = async { fetchElectricityStatementsUseCase(session.token, residenceId) }
                val unitsDeferred = async { fetchUnitsUseCase(session.token, residenceId) }

                val statementsResult = statementsDeferred.await()
                val unitsResult = unitsDeferred.await()

                val units = unitsResult.getOrDefault(emptyList())

                statementsResult
                    .onSuccess { list ->
                        updateState {
                            it.copy(
                                isLoading = false,
                                statements = list,
                                residenceUnits = units,
                                errorMessage = null
                            )
                        }
                    }
                    .onFailure { error ->
                        updateState {
                            it.copy(
                                isLoading = false,
                                residenceUnits = units,
                                errorMessage = error.message
                            )
                        }
                    }
            }
        }
    }

    private fun loadPreviousIndex(unitId: String) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoadingPreviousIndex = true) }
            fetchPreviousIndexUseCase(session.token, unitId)
                .onSuccess { lastIdx ->
                    updateState { it.copy(isLoadingPreviousIndex = false, formPreviousIndex = lastIdx) }
                }
                .onFailure {
                    updateState { it.copy(isLoadingPreviousIndex = false, formPreviousIndex = 0.0) }
                }
        }
    }

    private fun createStatement(intent: ElectricityIntent.CreateStatement) {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            createElectricityStatementUseCase(
                token = session.token,
                residenceId = residenceId,
                unitId = intent.unitId,
                previousIndex = intent.previousIndex,
                newIndex = intent.newIndex,
                kWhPrice = intent.kWhPrice,
                statementDate = intent.statementDate
            )
                .onSuccess {
                    updateState { it.copy(showCreateDialog = false, formPreviousIndex = null) }
                    loadStatements(residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun markPaid(statementId: String) {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            markStatementPaidUseCase(session.token, statementId)
                .onSuccess {
                    updateState { it.copy(confirmingPaymentStatement = null) }
                    loadStatements(residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}
