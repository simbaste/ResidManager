package com.resid.manager.features.auth.usecase

import com.resid.manager.SessionStorage
import com.resid.manager.dto.AuthResponse
import com.resid.manager.repository.AuthRepository
import com.resid.manager.validation.AuthValidator

class RegisterUseCase(
    private val authRepository: AuthRepository,
    private val sessionStorage: SessionStorage? = null
) {
    suspend operator fun invoke(
        firstName: String,
        lastName: String,
        birthDate: String?,
        phone: String?,
        email: String,
        passwordPlain: String
    ): Result<AuthResponse> {
        val validation = AuthValidator.validateRegister(firstName, lastName, email, passwordPlain)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull() ?: IllegalArgumentException("Données d'inscription invalides"))
        }

        val result = authRepository.register(
            firstName.trim(),
            lastName.trim(),
            birthDate?.trim()?.ifBlank { null },
            phone?.trim()?.ifBlank { null },
            email.trim(),
            passwordPlain
        )
        result.onSuccess { response ->
            val fName = response.user.firstName ?: "Utilisateur"
            val lName = response.user.lastName ?: ""
            sessionStorage?.saveSession(response.token, fName, lName)
        }
        return result
    }
}
