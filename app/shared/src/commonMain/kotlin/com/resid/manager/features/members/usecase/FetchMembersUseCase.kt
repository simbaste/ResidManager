package com.resid.manager.features.members.usecase

import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.repository.MemberRepository

class FetchMembersUseCase(
    private val memberRepository: MemberRepository
) {
    suspend operator fun invoke(token: String, residenceId: String): Result<List<ResidenceMemberSummaryDto>> {
        if (token.isBlank() || residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("Token ou ID de résidence manquant"))
        }
        return memberRepository.fetchMembers(token, residenceId)
    }
}
