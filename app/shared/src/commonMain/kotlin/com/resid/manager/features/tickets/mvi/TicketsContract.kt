package com.resid.manager.features.tickets.mvi

import com.resid.manager.dto.TicketCategoryDto
import com.resid.manager.dto.TicketCreateRequest
import com.resid.manager.dto.TicketDto
import com.resid.manager.dto.TicketStatusDto

data class TicketsUiState(
    val isLoading: Boolean = false,
    val tickets: List<TicketDto> = emptyList(),
    val categories: List<TicketCategoryDto> = emptyList(),
    val statusFilter: String = "ALL", // "ALL", "OPEN", "IN_PROGRESS", "CLOSED"
    val urgencyFilter: String = "ALL", // "ALL", "LOW", "MEDIUM", "CRITICAL"
    val unitFilterId: String = "",
    val showCreateDialog: Boolean = false,
    val takeChargeTicketId: String? = null,
    val closeTicketId: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

sealed interface TicketsIntent {
    data class LoadData(val token: String, val residenceId: String) : TicketsIntent
    data class SetStatusFilter(val filter: String) : TicketsIntent
    data class SetUrgencyFilter(val filter: String) : TicketsIntent
    data class SetUnitFilter(val unitId: String) : TicketsIntent
    data class SetShowCreateDialog(val show: Boolean) : TicketsIntent
    data class SetTakeChargeTicketId(val ticketId: String?) : TicketsIntent
    data class SetCloseTicketId(val ticketId: String?) : TicketsIntent

    data class CreateTicket(
        val token: String,
        val residenceId: String,
        val unitId: String,
        val request: TicketCreateRequest
    ) : TicketsIntent

    data class UpdateTicketStatus(
        val token: String,
        val residenceId: String,
        val ticketId: String,
        val status: TicketStatusDto,
        val interventionCost: Double,
        val comment: String?
    ) : TicketsIntent
}

sealed interface TicketsEffect {
    data class ShowToast(val message: String) : TicketsEffect
    data object TicketCreatedSuccess : TicketsEffect
}
