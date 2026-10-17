package com.resid.manager.features.auth.usecase

import com.resid.manager.SessionStorage
import com.resid.manager.dto.AuthResponse
import com.resid.manager.repository.AuthRepository
import com.resid.manager.repository.StorageRepository
import com.resid.manager.validation.AuthValidator

class RegisterUseCase(
    private val authRepository: AuthRepository,
    private val storageRepository: StorageRepository,
    private val sessionStorage: SessionStorage? = null
) {
    suspend operator fun invoke(
        firstName: String,
        lastName: String,
        birthDate: String?,
        phone: String?,
        email: String,
        passwordPlain: String,
        avatarBytes: ByteArray? = null,
        avatarMimeType: String = "image/jpeg",
        avatarExtension: String = "jpg"
    ): Result<AuthResponse> {
        val validation = AuthValidator.validateRegister(firstName, lastName, email, passwordPlain)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull() ?: IllegalArgumentException("Données d'inscription invalides"))
        }

        var publicAvatarUrl: String? = null

        // Upload préalable si une image a été sélectionnée
        if (avatarBytes != null && avatarBytes.isNotEmpty()) {
            val uploadResult = storageRepository.uploadAvatarToR2(avatarBytes, avatarMimeType, avatarExtension)
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Erreur lors de l'envoi de la photo de profil"))
            }
            publicAvatarUrl = uploadResult.getOrNull()
        }

        val result = authRepository.register(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            birthDate = birthDate?.trim()?.ifBlank { null },
            phone = phone?.trim()?.ifBlank { null },
            email = email.trim(),
            passwordPlain = passwordPlain,
            avatarUrl = publicAvatarUrl
        )

        result.onSuccess { response ->
            val fName = response.user.firstName ?: "Utilisateur"
            val lName = response.user.lastName ?: ""
            sessionStorage?.saveSession(
                token = response.token,
                refreshToken = response.refreshToken,
                fName = fName,
                lName = lName
            )
        }
        return result
    }
}
