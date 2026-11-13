package com.resid.manager.features.finances.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.features.finances.FinancesViewModel
import com.resid.manager.features.finances.mvi.FinancesIntent
import com.resid.manager.features.finances.mvi.FinancesUiState
import com.resid.manager.features.finances.ui.components.FinancesFilterBar
import com.resid.manager.features.finances.ui.components.QuickExpenseForm
import com.resid.manager.features.finances.ui.components.TraceabilityDialog
import com.resid.manager.features.finances.ui.components.TransactionsLedger
import com.resid.manager.ui.components.ResidTopAppBar
import org.koin.compose.koinInject

@Composable
fun FinancesScreen(
    activeResidence: ResidenceContext? = null,
    viewModel: FinancesViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(activeResidence?.residenceId) {
        if (activeResidence != null) {
            viewModel.updateActiveResidence(activeResidence)
        }
    }

    val effectiveCurrencySymbol = uiState.currencySymbol

    FinancesContent(
        uiState = uiState,
        isAuthorized = uiState.isAuthorized,
        currencySymbol = effectiveCurrencySymbol,
        onCategoryChanged = { viewModel.onIntent(FinancesIntent.SetFormCategory(it)) },
        onAmountChanged = { viewModel.onIntent(FinancesIntent.SetFormAmount(it)) },
        onDescriptionChanged = { viewModel.onIntent(FinancesIntent.SetFormDescription(it)) },
        onDateChanged = { viewModel.onIntent(FinancesIntent.SetFormDate(it)) },
        onSubmitExpense = { viewModel.onIntent(FinancesIntent.SubmitExpense) },
        onFilterTypeChanged = { viewModel.onIntent(FinancesIntent.SetFilterType(it)) },
        onFilterQueryChanged = { viewModel.onIntent(FinancesIntent.SetFilterQuery(it)) },
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
    currencySymbol: String = com.resid.manager.dto.AppCurrency.DEFAULT.symbol,
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
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isSmallScreen = maxWidth < 900.dp

        Column(
            modifier = Modifier.fillMaxSize().padding(if (isSmallScreen) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResidTopAppBar(
                title = "Cashflow & Finances"
            )

            if (isSmallScreen) {
                // Stack vertically on smaller screens
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    QuickExpenseForm(
                        isAuthorized = isAuthorized,
                        formCategory = uiState.formCategory,
                        formAmountText = uiState.formAmountText,
                        formDescription = uiState.formDescription,
                        formDate = uiState.formDate,
                        isSubmittingExpense = uiState.isSubmittingExpense,
                        formError = uiState.formError,
                        formSuccess = uiState.formSuccess,
                        currencySymbol = currencySymbol,
                        onCategoryChanged = onCategoryChanged,
                        onAmountChanged = onAmountChanged,
                        onDescriptionChanged = onDescriptionChanged,
                        onDateChanged = onDateChanged,
                        onSubmit = onSubmitExpense,
                        modifier = Modifier.fillMaxWidth()
                    )

                    FinancesFilterBar(
                        filterType = uiState.filterType,
                        filterQueryText = uiState.filterQueryText,
                        onFilterTypeChanged = onFilterTypeChanged,
                        onFilterQueryChanged = onFilterQueryChanged
                    )

                    TransactionsLedger(
                        transactions = uiState.transactions,
                        isLoading = uiState.isLoading,
                        currencySymbol = currencySymbol,
                        onTraceabilityClick = onTraceabilityClick,
                        modifier = Modifier.fillMaxWidth().height(450.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
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
                        currencySymbol = currencySymbol,
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
                            currencySymbol = currencySymbol,
                            onTraceabilityClick = onTraceabilityClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
