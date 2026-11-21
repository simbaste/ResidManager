package com.resid.manager.features.leases.usecase

import com.resid.manager.dto.LeaseDto
import com.resid.manager.repository.LeaseRepository

class InitLeaseDraftUseCase(
    private val leaseRepository: LeaseRepository
) {
    suspend operator fun invoke(token: String, residenceUnitId: String): Result<LeaseDto> {
        if (token.isBlank() || residenceUnitId.isBlank()) {
            return Result.failure(IllegalArgumentException("Paramètres invalides"))
        }
        return leaseRepository.initDraft(token, residenceUnitId)
    }
}
