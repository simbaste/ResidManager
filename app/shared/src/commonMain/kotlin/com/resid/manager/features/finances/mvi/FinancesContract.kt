package com.resid.manager.features.finances.mvi

import com.resid.manager.dto.FinanceTransactionDto
import com.resid.manager.ui.components.getTodayIsoDate

data class FinancesUiState(
    val isLoading: Boolean = false,
    val transactions: List<FinanceTransactionDto> = emptyList(),

    // Formulaire de saisie rapide
    val formCategory: String = "Cleaning",
    val formAmountText: String = "",
    val formDescription: String = "",
    val formDate: String = getTodayIsoDate(),
    val isSubmittingExpense: Boolean = false,
    val formError: String? = null,
    val formSuccess: Boolean = false,

    // Filtres du grand livre
    val filterType: String = "ALL", // "ALL", "INCOME", "EXPENSE"
    val filterCategory: String = "ALL",
    val filterStartDateText: String = "",
    val filterEndDateText: String = "",
    val filterQueryText: String = "",

    // Traçabilité
    val traceabilityPayload: Pair<String, String>? = null,
    val errorMessage: String? = null
)

sealed interface FinancesIntent {
    data class LoadTransactions(val token: String, val residenceId: String) : FinancesIntent
    data class SetFilterType(val type: String, val token: String, val residenceId: String) : FinancesIntent
    data class SetFilterCategory(val category: String, val token: String, val residenceId: String) : FinancesIntent
    data class SetFilterStartDate(val date: String, val token: String, val residenceId: String) : FinancesIntent
    data class SetFilterEndDate(val date: String, val token: String, val residenceId: String) : FinancesIntent
    data class SetFilterQuery(val query: String, val token: String, val residenceId: String) : FinancesIntent

    // Form state updates
    data class SetFormCategory(val category: String) : FinancesIntent
    data class SetFormAmount(val amount: String) : FinancesIntent
    data class SetFormDescription(val description: String) : FinancesIntent
    data class SetFormDate(val date: String) : FinancesIntent
    data class SubmitExpense(val token: String, val residenceId: String) : FinancesIntent

    // Traçabilité
    data class SetTraceabilityPayload(val payload: Pair<String, String>?) : FinancesIntent
}

sealed interface FinancesEffect {
    data class ShowToast(val message: String) : FinancesEffect
    data object ExpenseRecordedSuccess : FinancesEffect
}
