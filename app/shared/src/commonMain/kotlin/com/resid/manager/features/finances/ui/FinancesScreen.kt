package com.resid.manager.features.finances.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.AppCurrency
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.features.finances.FinancesViewModel
import com.resid.manager.features.finances.mvi.FinancesIntent
import com.resid.manager.features.finances.mvi.FinancesUiState
import com.resid.manager.features.finances.ui.components.CashflowKpiCard
import com.resid.manager.features.finances.ui.components.FinancesFilterBar
import com.resid.manager.features.finances.ui.components.QuickExpenseForm
import com.resid.manager.features.finances.ui.components.TraceabilityDialog
import com.resid.manager.features.finances.ui.components.TransactionsLedger
import com.resid.manager.ui.components.ResidTopAppBar
import com.resid.manager.ui.theme.ResidTheme
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancesContent(
    modifier: Modifier = Modifier,
    uiState: FinancesUiState,
    isAuthorized: Boolean,
    currencySymbol: String = AppCurrency.DEFAULT.symbol,
    onCategoryChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onDateChanged: (String) -> Unit,
    onSubmitExpense: () -> Unit,
    onFilterTypeChanged: (String) -> Unit,
    onFilterQueryChanged: (String) -> Unit,
    onTraceabilityClick: (Pair<String, String>) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isSmallScreen = maxWidth < 900.dp
        
        // État pour afficher/masquer le BottomSheet sur Mobile
        var showBottomSheet by remember { mutableStateOf(false) }

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
                    CashflowKpiCard(
                        transactions = uiState.transactions,
                        currencySymbol = currencySymbol
                    )

                    // Boutons d'action rapides pour mobile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { 
                                // TODO: Ajuster pour initialiser comme recette si besoin
                                showBottomSheet = true 
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("+ Recette", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { 
                                // TODO: Ajuster pour initialiser comme dépense si besoin
                                showBottomSheet = true 
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("- Dépense", fontWeight = FontWeight.Bold)
                        }
                    }

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
                        isSmallScreen = isSmallScreen,
                        onTraceabilityClick = onTraceabilityClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ModalBottomSheet pour le formulaire sur Mobile
                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
                            onSubmit = {
                                onSubmitExpense()
                                showBottomSheet = false
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(48.dp)) // Padding inférieur pour le sheet
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Zone A: Panneau Gauche (KPI + Formulaire rapide)
                    Column(
                        modifier = Modifier.width(360.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        CashflowKpiCard(
                            transactions = uiState.transactions,
                            currencySymbol = currencySymbol
                        )
                        
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
                    }

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
                            isSmallScreen = isSmallScreen,
                            currencySymbol = currencySymbol,
                            onTraceabilityClick = onTraceabilityClick,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun FinancesContentPreview() {
    ResidTheme {
        FinancesContent(
            uiState = FinancesUiState(),
            isAuthorized = true,
            currencySymbol = AppCurrency.DEFAULT.symbol,
            onCategoryChanged = {},
            onAmountChanged = {},
            onDescriptionChanged = {},
            onDateChanged = {},
            onSubmitExpense = {},
            onFilterTypeChanged = {},
            onFilterQueryChanged = {},
            onTraceabilityClick = {}
        )
    }
}
