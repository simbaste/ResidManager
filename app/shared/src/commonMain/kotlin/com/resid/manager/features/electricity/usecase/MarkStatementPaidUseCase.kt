package com.resid.manager.features.electricity.usecase

import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.features.electricity.data.ElectricityRepository

class MarkStatementPaidUseCase(
    private val electricityRepository: ElectricityRepository
) {
    suspend operator fun invoke(token: String, statementId: String): Result<ElectricityStatementDto> {
        if (token.isBlank() || statementId.isBlank()) {
            return Result.failure(IllegalArgumentException("Token ou ID de relevé manquant"))
        }
        return electricityRepository.markStatementPaid(token, statementId)
    }
}
