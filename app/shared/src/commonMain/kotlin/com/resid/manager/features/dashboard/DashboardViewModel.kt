package com.resid.manager.features.dashboard

import androidx.lifecycle.viewModelScope
import com.resid.manager.base.MviViewModel
import com.resid.manager.features.dashboard.mvi.DashboardEffect
import com.resid.manager.features.dashboard.mvi.DashboardIntent
import com.resid.manager.features.dashboard.mvi.DashboardUiState
import com.resid.manager.features.dashboard.usecase.GetDashboardDataUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val getDashboardDataUseCase: GetDashboardDataUseCase
) : MviViewModel<DashboardUiState, DashboardIntent, DashboardEffect>(DashboardUiState()) {

    private var loadJob: Job? = null

    override fun onIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadData -> {
                fetchData(intent.token, intent.residenceId, uiState.value.periodFilter, uiState.value.customStartText, uiState.value.customEndText)
            }
            is DashboardIntent.ChangePeriodFilter -> {
                updateState { it.copy(periodFilter = intent.filter) }
                fetchData(intent.token, intent.residenceId, intent.filter, uiState.value.customStartText, uiState.value.customEndText)
            }
            is DashboardIntent.UpdateCustomStart -> {
                updateState { it.copy(customStartText = intent.startDate) }
                fetchData(intent.token, intent.residenceId, uiState.value.periodFilter, intent.startDate, uiState.value.customEndText)
            }
            is DashboardIntent.UpdateCustomEnd -> {
                updateState { it.copy(customEndText = intent.endDate) }
                fetchData(intent.token, intent.residenceId, uiState.value.periodFilter, uiState.value.customStartText, intent.endDate)
            }
        }
    }

    private fun fetchData(token: String, residenceId: String, periodFilter: String, startDate: String, endDate: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            getDashboardDataUseCase(token, residenceId, periodFilter, startDate, endDate)
                .onSuccess { data ->
                    updateState { it.copy(isLoading = false, dashboardData = data, errorMessage = null) }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}
