package com.resid.manager.features.electricity.usecase

import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.features.electricity.data.ElectricityRepository

class FetchElectricityStatementsUseCase(
    private val electricityRepository: ElectricityRepository
) {
    suspend operator fun invoke(token: String, residenceId: String): Result<List<ElectricityStatementDto>> {
        if (token.isBlank() || residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("Token ou ID de résidence manquant"))
        }
        return electricityRepository.fetchStatements(token, residenceId)
    }
}
