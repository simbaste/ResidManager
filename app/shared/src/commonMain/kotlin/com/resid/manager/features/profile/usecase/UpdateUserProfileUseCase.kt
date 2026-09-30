package com.resid.manager.features.profile.usecase

import com.resid.manager.dto.UserDto
import com.resid.manager.dto.UserUpdateRequest
import com.resid.manager.features.profile.data.ProfileRepository

class UpdateUserProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(
        token: String,
        firstName: String,
        lastName: String,
        phone: String
    ): Result<UserDto> {
        if (token.isBlank()) {
            return Result.failure(IllegalArgumentException("Token d'authentification manquant"))
        }
        val request = UserUpdateRequest(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            phone = phone.trim().ifBlank { null }
        )
        return profileRepository.updateProfile(token, request)
    }
}
