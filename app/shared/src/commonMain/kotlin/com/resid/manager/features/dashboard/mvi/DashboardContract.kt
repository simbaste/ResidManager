package com.resid.manager.features.dashboard.mvi

import com.resid.manager.dto.DashboardDataDto

data class DashboardUiState(
    val isLoading: Boolean = false,
    val dashboardData: DashboardDataDto? = null,
    val periodFilter: String = "MONTH", // "MONTH", "QUARTER", "YEAR", "CUSTOM"
    val customStartText: String = "2025-02-01",
    val customEndText: String = "2025-02-28",
    val errorMessage: String? = null
)

sealed interface DashboardIntent {
    data class LoadData(val token: String, val residenceId: String) : DashboardIntent
    data class ChangePeriodFilter(val filter: String, val token: String, val residenceId: String) : DashboardIntent
    data class UpdateCustomStart(val startDate: String, val token: String, val residenceId: String) : DashboardIntent
    data class UpdateCustomEnd(val endDate: String, val token: String, val residenceId: String) : DashboardIntent
}

sealed interface DashboardEffect {
    data class ShowToast(val message: String) : DashboardEffect
}
