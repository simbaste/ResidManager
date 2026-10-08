package com.resid.manager.features.tickets.usecase

import com.resid.manager.dto.TicketDto
import com.resid.manager.dto.TicketStatusDto
import com.resid.manager.dto.TicketUpdateRequest
import com.resid.manager.features.tickets.data.TicketRepository

class UpdateTicketStatusUseCase(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(
        token: String,
        ticketId: String,
        status: TicketStatusDto,
        interventionCost: Double,
        comment: String?
    ): Result<TicketDto> {
        if (token.isBlank() || ticketId.isBlank()) {
            return Result.failure(IllegalArgumentException("Token ou ID de ticket manquant"))
        }
        if (interventionCost < 0.0) {
            return Result.failure(IllegalArgumentException("Le coût d'intervention ne peut pas être négatif"))
        }

        val request = TicketUpdateRequest(
            status = status,
            interventionCost = interventionCost,
            comment = comment?.ifBlank { null }
        )
        return ticketRepository.updateTicketStatus(token, ticketId, request)
    }
}
