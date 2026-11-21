package com.resid.manager.features.leases.usecase

import com.resid.manager.dto.LeaseDraftStepUpdateRequest
import com.resid.manager.dto.LeaseDto
import com.resid.manager.repository.LeaseRepository

class SaveLeaseStepUseCase(
    private val leaseRepository: LeaseRepository
) {
    suspend operator fun invoke(
        token: String,
        leaseId: String,
        request: LeaseDraftStepUpdateRequest
    ): Result<LeaseDto> {
        if (token.isBlank() || leaseId.isBlank()) {
            return Result.failure(IllegalArgumentException("Paramètres invalides"))
        }
        return leaseRepository.updateDraftStep(token, leaseId, request)
    }
}
