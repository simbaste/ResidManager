package com.resid.manager.features.profile.usecase

import com.resid.manager.dto.TicketCategoryDto
import com.resid.manager.features.profile.data.ProfileRepository

class FetchResidenceCategoriesUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(token: String, residenceId: String): Result<List<TicketCategoryDto>> {
        if (token.isBlank() || residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("Paramètres obligatoires manquants"))
        }
        return profileRepository.fetchCategories(token, residenceId)
    }
}

class AddTicketCategoryUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(token: String, residenceId: String, key: String, label: String): Result<TicketCategoryDto> {
        if (token.isBlank() || residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("Paramètres obligatoires manquants"))
        }
        if (key.isBlank() || label.isBlank()) {
            return Result.failure(IllegalArgumentException("La clé et le libellé sont obligatoires"))
        }
        return profileRepository.addCategory(token, residenceId, key.trim().uppercase(), label.trim())
    }
}

class UpdateTicketCategoryUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(token: String, categoryId: String, label: String): Result<TicketCategoryDto> {
        if (token.isBlank() || categoryId.isBlank()) {
            return Result.failure(IllegalArgumentException("Paramètres obligatoires manquants"))
        }
        if (label.isBlank()) {
            return Result.failure(IllegalArgumentException("Le libellé ne peut pas être vide"))
        }
        return profileRepository.updateCategory(token, categoryId, label.trim())
    }
}

class UpdateResidenceCurrencyUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(token: String, residenceId: String, currencyCode: String): Result<Unit> {
        if (token.isBlank() || residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("Paramètres obligatoires manquants"))
        }
        return profileRepository.updateCurrency(token, residenceId, currencyCode)
    }
}
