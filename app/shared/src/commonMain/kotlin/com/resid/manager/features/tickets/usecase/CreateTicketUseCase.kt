package com.resid.manager.features.tickets.usecase

import com.resid.manager.dto.TicketCreateRequest
import com.resid.manager.dto.TicketDto
import com.resid.manager.features.tickets.data.TicketRepository

class CreateTicketUseCase(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(
        token: String,
        unitId: String,
        request: TicketCreateRequest
    ): Result<TicketDto> {
        if (token.isBlank() || unitId.isBlank()) {
            return Result.failure(IllegalArgumentException("Paramètres obligatoires manquants"))
        }
        if (request.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Le titre du ticket est obligatoire"))
        }
        if (request.description.isBlank()) {
            return Result.failure(IllegalArgumentException("La description de la panne est obligatoire"))
        }
        return ticketRepository.createTicket(token, unitId, request)
    }
}
