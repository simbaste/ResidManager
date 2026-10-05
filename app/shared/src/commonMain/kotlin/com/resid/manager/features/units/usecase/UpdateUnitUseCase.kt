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
        equipmentIds: List<String> = emptyList()
    ): Result<ResidenceUnitDto> {
        val trimmedName = name.trim()
        val trimmedFloor = floor.trim()
        val trimmedType = type.trim()

        if (unitId.isBlank()) {
            return Result.failure(IllegalArgumentException("ID de logement manquant."))
        }
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Le nom du logement est obligatoire."))
        }
        if (trimmedFloor.isBlank()) {
            return Result.failure(IllegalArgumentException("L'étage ou le bloc est obligatoire."))
        }
        if (trimmedType.isBlank()) {
            return Result.failure(IllegalArgumentException("Le type de logement est obligatoire."))
        }
        if (nominalRent < 0.0) {
            return Result.failure(IllegalArgumentException("Le loyer nominal ne peut pas être négatif."))
        }
        if (serviceCharges < 0.0) {
            return Result.failure(IllegalArgumentException("Les charges fixes ne peuvent pas être négatives."))
        }
        if (initialElectricityIndex < 0.0) {
            return Result.failure(IllegalArgumentException("L'index initial d'électricité ne peut pas être négatif."))
        }

        val request = ResidenceUnitCreateRequest(
            name = trimmedName,
            floor = trimmedFloor,
            type = trimmedType,
            nominalRent = nominalRent,
            serviceCharges = serviceCharges,
            initialElectricityIndex = initialElectricityIndex,
            equipmentIds = equipmentIds
        )
        return residenceUnitRepository.updateResidenceUnit(token, residenceId, unitId, request)
    }
}
