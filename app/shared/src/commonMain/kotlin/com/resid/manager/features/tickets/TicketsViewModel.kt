package com.resid.manager.features.tickets

import androidx.lifecycle.viewModelScope
import com.resid.manager.base.MviViewModel
import com.resid.manager.features.tickets.mvi.TicketsEffect
import com.resid.manager.features.tickets.mvi.TicketsIntent
import com.resid.manager.features.tickets.mvi.TicketsUiState
import com.resid.manager.features.tickets.usecase.CreateTicketUseCase
import com.resid.manager.features.tickets.usecase.FetchTicketCategoriesUseCase
import com.resid.manager.features.tickets.usecase.FetchTicketsUseCase
import com.resid.manager.features.tickets.usecase.UpdateTicketStatusUseCase
import kotlinx.coroutines.launch

class TicketsViewModel(
    private val fetchTicketsUseCase: FetchTicketsUseCase,
    private val fetchTicketCategoriesUseCase: FetchTicketCategoriesUseCase,
    private val createTicketUseCase: CreateTicketUseCase,
    private val updateTicketStatusUseCase: UpdateTicketStatusUseCase
) : MviViewModel<TicketsUiState, TicketsIntent, TicketsEffect>(TicketsUiState()) {

    override fun onIntent(intent: TicketsIntent) {
        when (intent) {
            is TicketsIntent.LoadData -> loadData(intent.token, intent.residenceId)
            is TicketsIntent.SetStatusFilter -> updateState { it.copy(statusFilter = intent.filter) }
            is TicketsIntent.SetUrgencyFilter -> updateState { it.copy(urgencyFilter = intent.filter) }
            is TicketsIntent.SetUnitFilter -> updateState { it.copy(unitFilterId = intent.unitId) }
            is TicketsIntent.SetShowCreateDialog -> updateState { it.copy(showCreateDialog = intent.show, errorMessage = null) }
            is TicketsIntent.SetTakeChargeTicketId -> updateState { it.copy(takeChargeTicketId = intent.ticketId, errorMessage = null) }
            is TicketsIntent.SetCloseTicketId -> updateState { it.copy(closeTicketId = intent.ticketId, errorMessage = null) }
            is TicketsIntent.CreateTicket -> createTicket(intent)
            is TicketsIntent.UpdateTicketStatus -> updateTicketStatus(intent)
        }
    }

    private fun loadData(token: String, residenceId: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            val ticketsResult = fetchTicketsUseCase(token, residenceId)
            val categoriesResult = fetchTicketCategoriesUseCase(token, residenceId)

            val tickets = ticketsResult.getOrElse { emptyList() }
            val categories = categoriesResult.getOrElse { emptyList() }

            updateState {
                it.copy(
                    isLoading = false,
                    tickets = tickets,
                    categories = categories,
                    errorMessage = ticketsResult.exceptionOrNull()?.message ?: categoriesResult.exceptionOrNull()?.message
                )
            }
        }
    }

    private fun createTicket(intent: TicketsIntent.CreateTicket) {
        viewModelScope.launch {
            updateState { it.copy(isSubmitting = true, errorMessage = null) }
            createTicketUseCase(
                token = intent.token,
                unitId = intent.unitId,
                request = intent.request
            )
                .onSuccess {
                    updateState { it.copy(isSubmitting = false, showCreateDialog = false) }
                    emitEffect(TicketsEffect.TicketCreatedSuccess)
                    loadData(intent.token, intent.residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isSubmitting = false, errorMessage = error.message) }
                }
        }
    }

    private fun updateTicketStatus(intent: TicketsIntent.UpdateTicketStatus) {
        viewModelScope.launch {
            updateState { it.copy(isSubmitting = true, errorMessage = null) }
            updateTicketStatusUseCase(
                token = intent.token,
                ticketId = intent.ticketId,
                status = intent.status,
                interventionCost = intent.interventionCost,
                comment = intent.comment
            )
                .onSuccess {
                    updateState {
                        it.copy(
                            isSubmitting = false,
                            takeChargeTicketId = null,
                            closeTicketId = null
                        )
                    }
                    loadData(intent.token, intent.residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isSubmitting = false, errorMessage = error.message) }
                }
        }
    }
}
