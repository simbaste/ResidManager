package com.resid.manager.features.units.usecase

import com.resid.manager.dto.ResidenceUnitCreateRequest
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.repository.ResidenceUnitRepository

class CreateUnitUseCase(
    private val residenceUnitRepository: ResidenceUnitRepository
) {
    suspend operator fun invoke(
        token: String,
        residenceId: String,
        name: String,
        floor: String,
        type: String,
        nominalRent: Double,
        serviceCharges: Double,
        initialElectricityIndex: Double,
        equipementIds: List<String> = emptyList()
    ): Result<ResidenceUnitDto> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Le nom du logement est obligatoire"))
        }
        val request = ResidenceUnitCreateRequest(
            name = name.trim(),
            floor = floor.trim(),
            type = type.trim(),
            nominalRent = nominalRent,
            serviceCharges = serviceCharges,
            initialElectricityIndex = initialElectricityIndex,
            equipementIds = equipementIds
        )
        return residenceUnitRepository.createResidenceUnit(token, residenceId, request)
    }
}
