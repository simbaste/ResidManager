package com.resid.manager.features.leases.usecase

import com.resid.manager.dto.LeaseDto
import com.resid.manager.repository.LeaseRepository

class FetchLeasesUseCase(
    private val leaseRepository: LeaseRepository
) {
    suspend operator fun invoke(token: String, residenceId: String): Result<List<LeaseDto>> {
        if (token.isBlank() || residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("Token ou ID de résidence manquant"))
        }
        return leaseRepository.fetchLeases(token, residenceId)
    }
}
