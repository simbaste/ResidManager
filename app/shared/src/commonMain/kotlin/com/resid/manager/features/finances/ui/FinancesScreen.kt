package com.resid.manager.features.finances.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.features.finances.FinancesViewModel
import com.resid.manager.features.finances.mvi.FinancesIntent
import com.resid.manager.features.finances.mvi.FinancesUiState
import com.resid.manager.features.finances.ui.components.FinancesFilterBar
import com.resid.manager.features.finances.ui.components.QuickExpenseForm
import com.resid.manager.features.finances.ui.components.TraceabilityDialog
import com.resid.manager.features.finances.ui.components.TransactionsLedger
import org.koin.compose.koinInject

@Composable
fun FinancesScreen(
    activeResidence: ResidenceContext?,
    jwtToken: String?,
    viewModel: FinancesViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val token = jwtToken ?: ""
    val residenceId = activeResidence?.residenceId ?: ""
    val isAuthorized = activeResidence != null && (activeResidence.userRoleInResidence == UserRole.ADMIN || activeResidence.userRoleInResidence == UserRole.MANAGER)

    LaunchedEffect(token, residenceId) {
        if (token.isNotBlank() && residenceId.isNotBlank()) {
            viewModel.onIntent(FinancesIntent.LoadTransactions(token, residenceId))
        }
    }

    FinancesContent(
        uiState = uiState,
        isAuthorized = isAuthorized,
        onCategoryChanged = { viewModel.onIntent(FinancesIntent.SetFormCategory(it)) },
        onAmountChanged = { viewModel.onIntent(FinancesIntent.SetFormAmount(it)) },
        onDescriptionChanged = { viewModel.onIntent(FinancesIntent.SetFormDescription(it)) },
        onDateChanged = { viewModel.onIntent(FinancesIntent.SetFormDate(it)) },
        onSubmitExpense = { viewModel.onIntent(FinancesIntent.SubmitExpense(token, residenceId)) },
        onFilterTypeChanged = { viewModel.onIntent(FinancesIntent.SetFilterType(it, token, residenceId)) },
        onFilterQueryChanged = { viewModel.onIntent(FinancesIntent.SetFilterQuery(it, token, residenceId)) },
        onTraceabilityClick = { viewModel.onIntent(FinancesIntent.SetTraceabilityPayload(it)) }
    )

    if (uiState.traceabilityPayload != null) {
        TraceabilityDialog(
            payload = uiState.traceabilityPayload,
            onDismiss = { viewModel.onIntent(FinancesIntent.SetTraceabilityPayload(null)) }
        )
    }
}

@Composable
fun FinancesContent(
    uiState: FinancesUiState,
    isAuthorized: Boolean,
    onCategoryChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onDateChanged: (String) -> Unit,
    onSubmitExpense: () -> Unit,
    onFilterTypeChanged: (String) -> Unit,
    onFilterQueryChanged: (String) -> Unit,
    onTraceabilityClick: (Pair<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Zone A: Formulaire de Saisie Rapide (Left Column, fixed width)
        QuickExpenseForm(
            isAuthorized = isAuthorized,
            formCategory = uiState.formCategory,
            formAmountText = uiState.formAmountText,
            formDescription = uiState.formDescription,
            formDate = uiState.formDate,
            isSubmittingExpense = uiState.isSubmittingExpense,
            formError = uiState.formError,
            formSuccess = uiState.formSuccess,
            onCategoryChanged = onCategoryChanged,
            onAmountChanged = onAmountChanged,
            onDescriptionChanged = onDescriptionChanged,
            onDateChanged = onDateChanged,
            onSubmit = onSubmitExpense
        )

        // Zone B: Le Grand Livre des Transactions (Right Column, expands)
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FinancesFilterBar(
                filterType = uiState.filterType,
                filterQueryText = uiState.filterQueryText,
                onFilterTypeChanged = onFilterTypeChanged,
                onFilterQueryChanged = onFilterQueryChanged
            )

            TransactionsLedger(
                transactions = uiState.transactions,
                isLoading = uiState.isLoading,
                onTraceabilityClick = onTraceabilityClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
