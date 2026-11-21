package com.resid.manager.features.leases.usecase

import com.resid.manager.dto.UserSearchDto
import com.resid.manager.repository.UserRepository

class SearchUsersUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(token: String, query: String, residenceId: String? = null): Result<List<UserSearchDto>> {
        if (token.isBlank()) {
            return Result.failure(IllegalArgumentException("Token d'authentification manquant"))
        }
        if (query.trim().length < 2) {
            return Result.success(emptyList())
        }
        return userRepository.searchUsers(token, query.trim(), residenceId)
    }
}
