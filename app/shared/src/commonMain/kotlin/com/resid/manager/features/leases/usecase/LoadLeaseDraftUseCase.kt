package com.resid.manager.features.leases.usecase

import com.resid.manager.dto.LeaseDto
import com.resid.manager.repository.LeaseRepository

class LoadLeaseDraftUseCase(
    private val leaseRepository: LeaseRepository
) {
    suspend operator fun invoke(token: String, residenceId: String, leaseId: String): Result<LeaseDto> {
        if (token.isBlank() || residenceId.isBlank() || leaseId.isBlank()) {
            return Result.failure(IllegalArgumentException("Paramètres invalides"))
        }
        return leaseRepository.fetchLeaseById(token, residenceId, leaseId)
    }
}
