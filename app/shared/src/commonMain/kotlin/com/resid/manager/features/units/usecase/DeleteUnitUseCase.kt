package com.resid.manager.features.units.usecase

import com.resid.manager.repository.ResidenceUnitRepository

class DeleteUnitUseCase(
    private val residenceUnitRepository: ResidenceUnitRepository
) {
    suspend operator fun invoke(token: String, residenceId: String, unitId: String): Result<Unit> {
        if (token.isBlank() || residenceId.isBlank() || unitId.isBlank()) {
            return Result.failure(IllegalArgumentException("Paramètres de suppression manquants"))
        }
        return residenceUnitRepository.deleteResidenceUnit(token, residenceId, unitId)
    }
}
