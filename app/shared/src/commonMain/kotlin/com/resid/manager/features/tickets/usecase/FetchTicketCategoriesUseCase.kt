package com.resid.manager.features.tickets.usecase

import com.resid.manager.dto.TicketCategoryDto
import com.resid.manager.features.tickets.data.TicketRepository

class FetchTicketCategoriesUseCase(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(token: String, residenceId: String): Result<List<TicketCategoryDto>> {
        if (token.isBlank() || residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("Token ou ID de résidence manquant"))
        }
        return ticketRepository.fetchCategories(token, residenceId)
    }
}
