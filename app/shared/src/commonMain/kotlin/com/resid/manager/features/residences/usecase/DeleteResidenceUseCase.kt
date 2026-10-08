package com.resid.manager.features.residences.usecase

import com.resid.manager.repository.ResidenceRepository

class DeleteResidenceUseCase(
    private val residenceRepository: ResidenceRepository
) {
    suspend operator fun invoke(token: String, residenceId: String): Result<Unit> {
        if (token.isBlank() || residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("Paramètres de suppression manquants"))
        }
        return residenceRepository.deleteResidence(token, residenceId)
    }
}
