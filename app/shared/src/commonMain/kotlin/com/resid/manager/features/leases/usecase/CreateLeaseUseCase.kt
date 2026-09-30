package com.resid.manager.features.leases.usecase

import com.resid.manager.dto.LeaseCreateRequest
import com.resid.manager.dto.LeaseDto
import com.resid.manager.repository.LeaseRepository

class CreateLeaseUseCase(
    private val leaseRepository: LeaseRepository
) {
    suspend operator fun invoke(token: String, residenceUnitId: String, request: LeaseCreateRequest): Result<LeaseDto> {
        if (token.isBlank() || residenceUnitId.isBlank()) {
            return Result.failure(IllegalArgumentException("Token ou ID de logement manquant"))
        }
        return leaseRepository.createLease(token, residenceUnitId, request)
    }
}
