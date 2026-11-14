package com.resid.manager.features.residences.usecase

import com.resid.manager.dto.AppCurrency
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
                val appCurrency = AppCurrency.fromCode(it.currencyCode.name)
                ResidenceContext(
                    residenceId = it.id,
                    residenceName = it.name,
                    residenceAddress = it.address,
                    userRoleInResidence = UserRole.OWNER,
                    totalUnits = it.totalUnits,
                    currencySymbol = appCurrency.symbol,
                    currencyCode = appCurrency.code,
                    kWhPrice = it.kWhPrice,
                    photoUrl = it.photoUrl
                )
            }
            val associated = directory.associatedResidences.map {
                val appCurrency = AppCurrency.fromCode(it.currencyCode.name)
                ResidenceContext(
                    residenceId = it.id,
                    residenceName = it.name,
                    residenceAddress = it.address,
                    userRoleInResidence = it.roleDto.toUserRole(),
                    totalUnits = it.totalUnits,
                    currencySymbol = appCurrency.symbol,
                    currencyCode = appCurrency.code,
                    kWhPrice = it.kWhPrice,
                    photoUrl = it.photoUrl
                )
            }
            owned + associated
        }
    }
}
