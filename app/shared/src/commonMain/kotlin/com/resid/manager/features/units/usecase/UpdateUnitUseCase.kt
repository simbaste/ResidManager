package com.resid.manager.features.units.usecase

import com.resid.manager.dto.ResidenceUnitCreateRequest
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.repository.ResidenceUnitRepository

class UpdateUnitUseCase(
    private val residenceUnitRepository: ResidenceUnitRepository
) {
    suspend operator fun invoke(
        token: String,
        residenceId: String,
        unitId: String,
        name: String,
        floor: String,
        type: String,
        nominalRent: Double,
        serviceCharges: Double,
        initialElectricityIndex: Double,
        equipementIds: List<String> = emptyList()
    ): Result<ResidenceUnitDto> {
        if (unitId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID de logement manquant"))
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
        return residenceUnitRepository.updateResidenceUnit(token, residenceId, unitId, request)
    }
}
