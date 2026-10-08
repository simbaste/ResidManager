package com.resid.manager.features.leases.usecase

import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.LeaseStatusDto
import com.resid.manager.repository.LeaseRepository

class UpdateLeaseStatusUseCase(
    private val leaseRepository: LeaseRepository
) {
    suspend operator fun invoke(token: String, leaseId: String, status: LeaseStatusDto): Result<LeaseDto> {
        if (token.isBlank() || leaseId.isBlank()) {
            return Result.failure(IllegalArgumentException("Token ou ID de contrat manquant"))
        }
        return leaseRepository.updateLeaseStatus(token, leaseId, status)
    }
}
