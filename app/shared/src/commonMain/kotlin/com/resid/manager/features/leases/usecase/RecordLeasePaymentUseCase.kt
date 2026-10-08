package com.resid.manager.features.leases.usecase

import com.resid.manager.dto.LeaseDto
import com.resid.manager.repository.LeaseRepository

class RecordLeasePaymentUseCase(
    private val leaseRepository: LeaseRepository
) {
    suspend operator fun invoke(token: String, leaseId: String, amount: Double, category: String): Result<LeaseDto> {
        if (token.isBlank() || leaseId.isBlank()) {
            return Result.failure(IllegalArgumentException("Token ou ID de contrat manquant"))
        }
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Le montant du versement doit être supérieur à zéro"))
        }
        return leaseRepository.recordLeasePayment(token, leaseId, amount, category)
    }
}
