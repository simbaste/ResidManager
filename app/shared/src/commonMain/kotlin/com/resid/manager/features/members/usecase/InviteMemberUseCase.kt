package com.resid.manager.features.members.usecase

import com.resid.manager.repository.MemberRepository

class InviteMemberUseCase(
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(token: String, residenceId: String, email: String, role: String): Result<Unit> {
        if (token.isBlank() || residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("Token ou ID de résidence manquant"))
        }
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Veuillez sélectionner une adresse email"))
        }
        return memberRepository.inviteMember(token, residenceId, email.trim(), role)
    }
}
