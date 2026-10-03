package com.resid.manager.features.residences.usecase

import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.dto.toUserRole
import com.resid.manager.repository.ResidenceRepository

class FetchResidencesUseCase(
    private val residenceRepository: ResidenceRepository,
) {
    suspend operator fun invoke(token: String): Result<List<ResidenceContext>> {
        if (token.isBlank()) {
            return Result.failure(IllegalArgumentException("Token d'authentification manquant"))
        }

        return residenceRepository.fetchResidences(token).map { directory ->
            val owned = directory.ownedResidences.map {
                ResidenceContext(
                    residenceId = it.id,
                    residenceName = it.name,
                    residenceAddress = it.address,
                    userRoleInResidence = UserRole.OWNER,
                    totalUnits = it.totalUnits,
                    currencySymbol = it.currencySymbol.label,
                    currencyCode = it.currencyCode.name,
                )
            }
            val associated = directory.associatedResidences.map {
                ResidenceContext(
                    residenceId = it.id,
                    residenceName = it.name,
                    residenceAddress = it.address,
                    userRoleInResidence = it.roleDto.toUserRole(),
                    totalUnits = it.totalUnits,
                    currencySymbol = it.currencySymbol.label,
                    currencyCode = it.currencyCode.name,
                )
            }
            owned + associated
        }
    }
}
