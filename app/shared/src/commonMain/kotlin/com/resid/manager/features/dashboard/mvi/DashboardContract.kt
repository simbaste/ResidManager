package com.resid.manager.features.dashboard.mvi

import com.resid.manager.dto.DashboardDataDto
import com.resid.manager.dto.ResidenceContext

data class DashboardUiState(
    val isLoading: Boolean = false,
    val firstName: String = "",
    val activeResidence: ResidenceContext? = null,
    val dashboardData: DashboardDataDto? = null,
    val periodFilter: String = "MONTH", // "MONTH", "QUARTER", "YEAR", "CUSTOM"
    val customStartText: String = "2025-02-01",
    val customEndText: String = "2025-02-28",
    val errorMessage: String? = null
)

sealed interface DashboardIntent {
    data object Refresh : DashboardIntent
    data class LoadData(val residenceId: String) : DashboardIntent
    data class ChangePeriodFilter(val filter: String) : DashboardIntent
    data class UpdateCustomStart(val startDate: String) : DashboardIntent
    data class UpdateCustomEnd(val endDate: String) : DashboardIntent
}

sealed interface DashboardEffect {
    data class ShowToast(val message: String) : DashboardEffect
}
