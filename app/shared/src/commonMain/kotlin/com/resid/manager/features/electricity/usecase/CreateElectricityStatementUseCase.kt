package com.resid.manager.features.electricity.usecase

import com.resid.manager.dto.ElectricityStatementCreateRequest
import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.features.electricity.data.ElectricityRepository

class CreateElectricityStatementUseCase(
    private val electricityRepository: ElectricityRepository
) {
    suspend operator fun invoke(
        token: String,
        residenceId: String,
        unitId: String,
        previousIndex: Double,
        newIndex: Double,
        kWhPrice: Double,
        statementDate: String
    ): Result<ElectricityStatementDto> {
        if (token.isBlank() || residenceId.isBlank() || unitId.isBlank()) {
            return Result.failure(IllegalArgumentException("Paramètres obligatoires manquants"))
        }
        if (newIndex < previousIndex) {
            return Result.failure(IllegalArgumentException("Le nouvel index ($newIndex) ne peut pas être inférieur au précédent ($previousIndex)"))
        }
        if (kWhPrice <= 0.0) {
            return Result.failure(IllegalArgumentException("Le prix du kWh doit être supérieur à zéro"))
        }

        val request = ElectricityStatementCreateRequest(
            unitId = unitId,
            previousIndex = previousIndex,
            newIndex = newIndex,
            kWhPriceApplied = kWhPrice,
            statementDate = statementDate
        )
        return electricityRepository.createStatement(token, residenceId, request)
    }
}
