package com.resid.manager.features.leases.usecase

import com.resid.manager.repository.LeaseRepository

class DeleteLeaseUseCase(
    private val leaseRepository: LeaseRepository
) {
    suspend operator fun invoke(token: String, leaseId: String): Result<Unit> {
        return leaseRepository.deleteLease(token, leaseId)
    }
}
