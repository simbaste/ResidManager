package com.resid.manager.features.electricity.usecase

import com.resid.manager.features.electricity.data.ElectricityRepository

class FetchPreviousIndexUseCase(
    private val electricityRepository: ElectricityRepository
) {
    suspend operator fun invoke(token: String, unitId: String): Result<Double> {
        if (token.isBlank() || unitId.isBlank()) {
            return Result.failure(IllegalArgumentException("Token ou ID de logement manquant"))
        }
        return electricityRepository.fetchPreviousIndex(token, unitId)
    }
}
