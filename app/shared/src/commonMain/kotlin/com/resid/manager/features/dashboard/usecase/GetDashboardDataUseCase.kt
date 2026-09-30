package com.resid.manager.features.dashboard.usecase

import com.resid.manager.dto.DashboardDataDto
import com.resid.manager.features.dashboard.data.DashboardRepository

class GetDashboardDataUseCase(
    private val dashboardRepository: DashboardRepository
) {
    suspend operator fun invoke(
        token: String,
        residenceId: String,
        periodFilter: String,
        startDate: String,
        endDate: String
    ): Result<DashboardDataDto> {
        if (token.isBlank()) {
            return Result.failure(IllegalArgumentException("Token d'authentification manquant"))
        }
        if (residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID de résidence manquant"))
        }
        return dashboardRepository.fetchDashboardData(token, residenceId, periodFilter, startDate, endDate)
    }
}
