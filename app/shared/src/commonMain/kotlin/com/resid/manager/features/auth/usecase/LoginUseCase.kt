package com.resid.manager.features.auth.usecase

import com.resid.manager.SessionStorage
import com.resid.manager.dto.AuthResponse
import com.resid.manager.repository.AuthRepository
import com.resid.manager.validation.AuthValidator

class LoginUseCase(
    private val authRepository: AuthRepository,
    private val sessionStorage: SessionStorage? = null
) {
    suspend operator fun invoke(email: String, passwordPlain: String): Result<AuthResponse> {
        val validation = AuthValidator.validateLogin(email, passwordPlain)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull() ?: IllegalArgumentException("Identifiants invalides"))
        }

        val result = authRepository.login(email.trim(), passwordPlain)
        result.onSuccess { response ->
            val fName = response.user.firstName ?: "Utilisateur"
            val lName = response.user.lastName ?: ""
            sessionStorage?.saveSession(response.token, fName, lName)
        }
        return result
    }
}
