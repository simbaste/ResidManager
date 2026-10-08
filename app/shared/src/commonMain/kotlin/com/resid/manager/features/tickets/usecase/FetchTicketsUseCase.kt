package com.resid.manager.features.tickets.usecase

import com.resid.manager.dto.TicketDto
import com.resid.manager.features.tickets.data.TicketRepository

class FetchTicketsUseCase(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(token: String, residenceId: String): Result<List<TicketDto>> {
        if (token.isBlank() || residenceId.isBlank()) {
            return Result.failure(IllegalArgumentException("Token ou ID de résidence manquant"))
        }
        return ticketRepository.fetchTickets(token, residenceId)
    }
}
