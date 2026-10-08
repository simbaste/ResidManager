package com.resid.manager.features.units.usecase

import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.repository.ResidenceUnitRepository

class FetchUnitsUseCase(
    private val residenceUnitRepository: ResidenceUnitRepository
) {
    suspend operator fun invoke(token: String, residenceId: String): Result<List<ResidenceUnitDto>> {
        if (token.isBlank() || residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("Token ou ID de résidence manquant"))
        }
        return residenceUnitRepository.fetchResidenceUnits(token, residenceId)
    }
}
