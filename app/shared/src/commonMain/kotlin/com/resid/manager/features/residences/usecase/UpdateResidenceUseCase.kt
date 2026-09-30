package com.resid.manager.features.residences.usecase

import com.resid.manager.dto.CurrencyCodeDto
import com.resid.manager.dto.ResidenceCreateRequest
import com.resid.manager.dto.ResidenceSummaryItemDto
import com.resid.manager.repository.ResidenceRepository

class UpdateResidenceUseCase(
    private val residenceRepository: ResidenceRepository
) {
    suspend operator fun invoke(
        token: String,
        residenceId: String,
        name: String,
        address: String,
        kWhPrice: Double
    ): Result<ResidenceSummaryItemDto> {
        if (residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID de résidence manquant"))
        }
        val request = ResidenceCreateRequest(
            name = name.trim(),
            address = address.trim(),
            currency = CurrencyCodeDto.XOF,
            kWhPrice = kWhPrice
        )
        return residenceRepository.updateResidence(token, residenceId, request)
    }
}
