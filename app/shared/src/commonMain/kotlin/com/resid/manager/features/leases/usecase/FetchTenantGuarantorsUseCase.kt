package com.resid.manager.features.leases.usecase

import com.resid.manager.dto.GuarantorDto
import com.resid.manager.repository.LeaseRepository

class FetchTenantGuarantorsUseCase(
    private val leaseRepository: LeaseRepository
) {
    suspend operator fun invoke(token: String, tenantId: String): Result<List<GuarantorDto>> {
        if (token.isBlank() || tenantId.isBlank()) {
            return Result.failure(IllegalArgumentException("Paramètres invalides"))
        }
        return leaseRepository.fetchGuarantorsByTenant(token, tenantId)
    }
}
