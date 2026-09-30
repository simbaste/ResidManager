package com.resid.manager.features.residences.usecase

import com.resid.manager.dto.CurrencyCodeDto
import com.resid.manager.dto.ResidenceCreateRequest
import com.resid.manager.dto.ResidenceSummaryItemDto
import com.resid.manager.repository.ResidenceRepository

class CreateResidenceUseCase(
    private val residenceRepository: ResidenceRepository
) {
    suspend operator fun invoke(
        token: String,
        name: String,
        address: String,
        defaultCurrency: String,
        kWhPrice: Double
    ): Result<ResidenceSummaryItemDto> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Le nom de la résidence est obligatoire"))
        }
        if (address.isBlank()) {
            return Result.failure(IllegalArgumentException("L'adresse de la résidence est obligatoire"))
        }
        val currencyEnum = try {
            CurrencyCodeDto.valueOf(defaultCurrency)
        } catch (_: Exception) {
            CurrencyCodeDto.XOF
        }

        val request = ResidenceCreateRequest(
            name = name.trim(),
            address = address.trim(),
            currency = currencyEnum,
            kWhPrice = kWhPrice
        )
        return residenceRepository.createResidence(token, request)
    }
}
