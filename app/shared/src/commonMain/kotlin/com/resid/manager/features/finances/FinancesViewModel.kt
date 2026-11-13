package com.resid.manager.features.finances

import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.base.MviViewModel
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.TransactionCategoryDto
import com.resid.manager.dto.UserRole
import com.resid.manager.features.finances.mvi.FinancesEffect
import com.resid.manager.features.finances.mvi.FinancesIntent
import com.resid.manager.features.finances.mvi.FinancesUiState
import com.resid.manager.features.finances.usecase.FetchTransactionsUseCase
import com.resid.manager.features.finances.usecase.RecordExpenseUseCase
import com.resid.manager.features.residences.usecase.FetchResidencesUseCase
import com.resid.manager.ui.components.getTodayIsoDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FinancesViewModel(
    private val fetchTransactionsUseCase: FetchTransactionsUseCase,
    private val recordExpenseUseCase: RecordExpenseUseCase,
    private val fetchResidencesUseCase: FetchResidencesUseCase,
    private val sessionStorage: SessionStorage? = null
) : MviViewModel<FinancesUiState, FinancesIntent, FinancesEffect>(FinancesUiState()) {

    private var loadJob: Job? = null

    init {
        val lastSelectedId = sessionStorage?.loadLastSelectedResidenceId()
        if (!lastSelectedId.isNullOrBlank()) {
            loadResidenceAndTransactions(lastSelectedId)
        }
    }

    override fun onIntent(intent: FinancesIntent) {
        when (intent) {
            is FinancesIntent.Refresh -> {
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (!residenceId.isNullOrBlank()) {
                    loadTransactions(
                        residenceId = residenceId,
                        type = uiState.value.filterType,
                        category = uiState.value.filterCategory,
                        startDate = uiState.value.filterStartDateText,
                        endDate = uiState.value.filterEndDateText,
                        query = uiState.value.filterQueryText
                    )
                }
            }
            is FinancesIntent.LoadTransactions -> loadResidenceAndTransactions(intent.residenceId)
            is FinancesIntent.SetFilterType -> {
                updateState { it.copy(filterType = intent.type) }
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (!residenceId.isNullOrBlank()) {
                    loadTransactions(residenceId, intent.type, uiState.value.filterCategory, uiState.value.filterStartDateText, uiState.value.filterEndDateText, uiState.value.filterQueryText)
                }
            }
            is FinancesIntent.SetFilterCategory -> {
                updateState { it.copy(filterCategory = intent.category) }
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (!residenceId.isNullOrBlank()) {
                    loadTransactions(residenceId, uiState.value.filterType, intent.category, uiState.value.filterStartDateText, uiState.value.filterEndDateText, uiState.value.filterQueryText)
                }
            }
            is FinancesIntent.SetFilterStartDate -> {
                updateState { it.copy(filterStartDateText = intent.date) }
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (!residenceId.isNullOrBlank()) {
                    loadTransactions(residenceId, uiState.value.filterType, uiState.value.filterCategory, intent.date, uiState.value.filterEndDateText, uiState.value.filterQueryText)
                }
            }
            is FinancesIntent.SetFilterEndDate -> {
                updateState { it.copy(filterEndDateText = intent.date) }
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (!residenceId.isNullOrBlank()) {
                    loadTransactions(residenceId, uiState.value.filterType, uiState.value.filterCategory, uiState.value.filterStartDateText, intent.date, uiState.value.filterQueryText)
                }
            }
            is FinancesIntent.SetFilterQuery -> {
                updateState { it.copy(filterQueryText = intent.query) }
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (!residenceId.isNullOrBlank()) {
                    loadTransactions(residenceId, uiState.value.filterType, uiState.value.filterCategory, uiState.value.filterStartDateText, uiState.value.filterEndDateText, intent.query)
                }
            }
            is FinancesIntent.SetFormCategory -> updateState { it.copy(formCategory = intent.category) }
            is FinancesIntent.SetFormAmount -> updateState { it.copy(formAmountText = intent.amount, formError = null) }
            is FinancesIntent.SetFormDescription -> updateState { it.copy(formDescription = intent.description, formError = null) }
            is FinancesIntent.SetFormDate -> updateState { it.copy(formDate = intent.date, formError = null) }
            is FinancesIntent.SubmitExpense -> submitExpense()
            is FinancesIntent.SetTraceabilityPayload -> updateState { it.copy(traceabilityPayload = intent.payload) }
        }
    }

    fun updateActiveResidence(residence: ResidenceContext?) {
        if (residence == null) {
            updateState {
                it.copy(
                    activeResidence = null,
                    isAuthorized = false,
                    currencySymbol = com.resid.manager.dto.AppCurrency.DEFAULT.symbol,
                    transactions = emptyList()
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
        loadTransactions(
            residenceId = residence.residenceId,
            type = uiState.value.filterType,
            category = uiState.value.filterCategory,
            startDate = uiState.value.filterStartDateText,
            endDate = uiState.value.filterEndDateText,
            query = uiState.value.filterQueryText
        )
    }

    private fun loadResidenceAndTransactions(residenceId: String) {
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
            loadTransactions(
                residenceId = residenceId,
                type = uiState.value.filterType,
                category = uiState.value.filterCategory,
                startDate = uiState.value.filterStartDateText,
                endDate = uiState.value.filterEndDateText,
                query = uiState.value.filterQueryText
            )
        }
    }

    private fun loadTransactions(
        residenceId: String,
        type: String,
        category: String,
        startDate: String,
        endDate: String,
        query: String
    ) {
        val session = sessionStorage?.loadSession() ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            fetchTransactionsUseCase(session.token, residenceId, type, category, startDate, endDate, query)
                .onSuccess { list ->
                    updateState { it.copy(isLoading = false, transactions = list, errorMessage = null) }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun submitExpense() {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        val state = uiState.value
        val amount = state.formAmountText.toDoubleOrNull()
        if (amount == null || amount <= 0.0 || state.formDescription.isBlank() || state.formDate.isBlank()) {
            updateState {
                it.copy(
                    formError = "Veuillez remplir tous les champs obligatoires avec des valeurs valides.",
                    formSuccess = false
                )
            }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isSubmittingExpense = true, formError = null, formSuccess = false) }
            val catEnum = try {
                TransactionCategoryDto.valueOf(state.formCategory)
            } catch (_: Exception) {
                TransactionCategoryDto.MAINTENANCE
            }

            recordExpenseUseCase(session.token, residenceId, catEnum, amount, state.formDescription, state.formDate)
                .onSuccess {
                    updateState {
                        it.copy(
                            isSubmittingExpense = false,
                            formAmountText = "",
                            formDescription = "",
                            formDate = getTodayIsoDate(),
                            formSuccess = true
                        )
                    }
                    emitEffect(FinancesEffect.ExpenseRecordedSuccess)
                    loadTransactions(
                        residenceId,
                        state.filterType,
                        state.filterCategory,
                        state.filterStartDateText,
                        state.filterEndDateText,
                        state.filterQueryText
                    )
                }
                .onFailure { error ->
                    updateState {
                        it.copy(
                            isSubmittingExpense = false,
                            formError = error.message,
                            formSuccess = false
                        )
                    }
                }
        }
    }
}
