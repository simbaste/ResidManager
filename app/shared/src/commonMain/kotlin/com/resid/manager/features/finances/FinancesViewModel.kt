package com.resid.manager.features.finances

import androidx.lifecycle.viewModelScope
import com.resid.manager.base.MviViewModel
import com.resid.manager.dto.TransactionCategoryDto
import com.resid.manager.features.finances.mvi.FinancesEffect
import com.resid.manager.features.finances.mvi.FinancesIntent
import com.resid.manager.features.finances.mvi.FinancesUiState
import com.resid.manager.features.finances.usecase.FetchTransactionsUseCase
import com.resid.manager.features.finances.usecase.RecordExpenseUseCase
import com.resid.manager.ui.components.getTodayIsoDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FinancesViewModel(
    private val fetchTransactionsUseCase: FetchTransactionsUseCase,
    private val recordExpenseUseCase: RecordExpenseUseCase
) : MviViewModel<FinancesUiState, FinancesIntent, FinancesEffect>(FinancesUiState()) {

    private var loadJob: Job? = null

    override fun onIntent(intent: FinancesIntent) {
        when (intent) {
            is FinancesIntent.LoadTransactions -> loadTransactions(
                token = intent.token,
                residenceId = intent.residenceId,
                type = uiState.value.filterType,
                category = uiState.value.filterCategory,
                startDate = uiState.value.filterStartDateText,
                endDate = uiState.value.filterEndDateText,
                query = uiState.value.filterQueryText
            )
            is FinancesIntent.SetFilterType -> {
                updateState { it.copy(filterType = intent.type) }
                loadTransactions(intent.token, intent.residenceId, intent.type, uiState.value.filterCategory, uiState.value.filterStartDateText, uiState.value.filterEndDateText, uiState.value.filterQueryText)
            }
            is FinancesIntent.SetFilterCategory -> {
                updateState { it.copy(filterCategory = intent.category) }
                loadTransactions(intent.token, intent.residenceId, uiState.value.filterType, intent.category, uiState.value.filterStartDateText, uiState.value.filterEndDateText, uiState.value.filterQueryText)
            }
            is FinancesIntent.SetFilterStartDate -> {
                updateState { it.copy(filterStartDateText = intent.date) }
                loadTransactions(intent.token, intent.residenceId, uiState.value.filterType, uiState.value.filterCategory, intent.date, uiState.value.filterEndDateText, uiState.value.filterQueryText)
            }
            is FinancesIntent.SetFilterEndDate -> {
                updateState { it.copy(filterEndDateText = intent.date) }
                loadTransactions(intent.token, intent.residenceId, uiState.value.filterType, uiState.value.filterCategory, uiState.value.filterStartDateText, intent.date, uiState.value.filterQueryText)
            }
            is FinancesIntent.SetFilterQuery -> {
                updateState { it.copy(filterQueryText = intent.query) }
                loadTransactions(intent.token, intent.residenceId, uiState.value.filterType, uiState.value.filterCategory, uiState.value.filterStartDateText, uiState.value.filterEndDateText, intent.query)
            }
            is FinancesIntent.SetFormCategory -> updateState { it.copy(formCategory = intent.category) }
            is FinancesIntent.SetFormAmount -> updateState { it.copy(formAmountText = intent.amount, formError = null) }
            is FinancesIntent.SetFormDescription -> updateState { it.copy(formDescription = intent.description, formError = null) }
            is FinancesIntent.SetFormDate -> updateState { it.copy(formDate = intent.date, formError = null) }
            is FinancesIntent.SubmitExpense -> submitExpense(intent.token, intent.residenceId)
            is FinancesIntent.SetTraceabilityPayload -> updateState { it.copy(traceabilityPayload = intent.payload) }
        }
    }

    private fun loadTransactions(
        token: String,
        residenceId: String,
        type: String,
        category: String,
        startDate: String,
        endDate: String,
        query: String
    ) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            fetchTransactionsUseCase(token, residenceId, type, category, startDate, endDate, query)
                .onSuccess { list ->
                    updateState { it.copy(isLoading = false, transactions = list, errorMessage = null) }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun submitExpense(token: String, residenceId: String) {
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

            recordExpenseUseCase(token, residenceId, catEnum, amount, state.formDescription, state.formDate)
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
                        token,
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
