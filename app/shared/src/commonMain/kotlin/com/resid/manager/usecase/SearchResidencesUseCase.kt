package com.resid.manager.usecase

import com.resid.manager.dto.ResidenceSummaryItem
import com.resid.manager.repository.ResidenceRepository

class SearchResidencesUseCase(
    private val residenceRepository: ResidenceRepository
) {
    suspend operator fun invoke(token: String, query: String): Result<List<ResidenceSummaryItem>> {
        if (query.length < 2) {
            return Result.success(emptyList())
        }
        return residenceRepository.searchResidences(token, query)
    }
}
