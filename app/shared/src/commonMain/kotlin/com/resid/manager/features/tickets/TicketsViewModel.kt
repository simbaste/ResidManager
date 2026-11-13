package com.resid.manager.features.tickets

import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.base.MviViewModel
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.features.residences.usecase.FetchResidencesUseCase
import com.resid.manager.features.tickets.mvi.TicketsEffect
import com.resid.manager.features.tickets.mvi.TicketsIntent
import com.resid.manager.features.tickets.mvi.TicketsUiState
import com.resid.manager.features.tickets.usecase.CreateTicketUseCase
import com.resid.manager.features.tickets.usecase.FetchTicketCategoriesUseCase
import com.resid.manager.features.tickets.usecase.FetchTicketsUseCase
import com.resid.manager.features.tickets.usecase.UpdateTicketStatusUseCase
import com.resid.manager.features.units.usecase.FetchUnitsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class TicketsViewModel(
    private val fetchTicketsUseCase: FetchTicketsUseCase,
    private val fetchTicketCategoriesUseCase: FetchTicketCategoriesUseCase,
    private val createTicketUseCase: CreateTicketUseCase,
    private val updateTicketStatusUseCase: UpdateTicketStatusUseCase,
    private val fetchResidencesUseCase: FetchResidencesUseCase,
    private val fetchUnitsUseCase: FetchUnitsUseCase,
    private val sessionStorage: SessionStorage? = null
) : MviViewModel<TicketsUiState, TicketsIntent, TicketsEffect>(TicketsUiState()) {

    init {
        val lastSelectedId = sessionStorage?.loadLastSelectedResidenceId()
        if (!lastSelectedId.isNullOrBlank()) {
            loadResidenceAndTickets(lastSelectedId)
        }
    }

    override fun onIntent(intent: TicketsIntent) {
        when (intent) {
            is TicketsIntent.Refresh -> {
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (!residenceId.isNullOrBlank()) {
                    loadData(residenceId)
                }
            }
            is TicketsIntent.LoadData -> loadResidenceAndTickets(intent.residenceId)
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

    fun updateActiveResidence(residence: ResidenceContext?) {
        if (residence == null) {
            updateState {
                it.copy(
                    activeResidence = null,
                    isAuthorized = false,
                    currencySymbol = com.resid.manager.dto.AppCurrency.DEFAULT.symbol,
                    residenceUnits = emptyList(),
                    tickets = emptyList(),
                    categories = emptyList()
                )
            }
            return
        }
        val isAuth = residence.userRoleInResidence == UserRole.OWNER ||
            residence.userRoleInResidence == UserRole.ADMIN ||
            residence.userRoleInResidence == UserRole.MANAGER
        updateState {
            it.copy(
                activeResidence = residence,
                isAuthorized = isAuth,
                currencySymbol = residence.currencySymbol
            )
        }
        loadData(residence.residenceId)
    }

    private fun loadResidenceAndTickets(residenceId: String) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            if (uiState.value.activeResidence == null || uiState.value.activeResidence?.residenceId != residenceId) {
                fetchResidencesUseCase(session.token).onSuccess { list ->
                    val matched = list.firstOrNull { it.residenceId == residenceId }
                    if (matched != null) {
                        val isAuth = matched.userRoleInResidence == UserRole.OWNER ||
                            matched.userRoleInResidence == UserRole.ADMIN ||
                            matched.userRoleInResidence == UserRole.MANAGER
                        updateState {
                            it.copy(
                                activeResidence = matched,
                                isAuthorized = isAuth,
                                currencySymbol = matched.currencySymbol
                            )
                        }
                    }
                }
            }
            loadData(residenceId)
        }
    }

    private fun loadData(residenceId: String) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val ticketsDeferred = async { fetchTicketsUseCase(session.token, residenceId) }
                val categoriesDeferred = async { fetchTicketCategoriesUseCase(session.token, residenceId) }
                val unitsDeferred = async { fetchUnitsUseCase(session.token, residenceId) }

                val ticketsResult = ticketsDeferred.await()
                val categoriesResult = categoriesDeferred.await()
                val unitsResult = unitsDeferred.await()

                val tickets = ticketsResult.getOrElse { emptyList() }
                val categories = categoriesResult.getOrElse { emptyList() }
                val units = unitsResult.getOrElse { emptyList() }

                updateState {
                    it.copy(
                        isLoading = false,
                        tickets = tickets,
                        categories = categories,
                        residenceUnits = units,
                        errorMessage = ticketsResult.exceptionOrNull()?.message ?: categoriesResult.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    private fun createTicket(intent: TicketsIntent.CreateTicket) {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isSubmitting = true, errorMessage = null) }
            createTicketUseCase(
                token = session.token,
                unitId = intent.unitId,
                request = intent.request
            )
                .onSuccess {
                    updateState { it.copy(isSubmitting = false, showCreateDialog = false) }
                    emitEffect(TicketsEffect.TicketCreatedSuccess)
                    loadData(residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isSubmitting = false, errorMessage = error.message) }
                }
        }
    }

    private fun updateTicketStatus(intent: TicketsIntent.UpdateTicketStatus) {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isSubmitting = true, errorMessage = null) }
            updateTicketStatusUseCase(
                token = session.token,
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
                    loadData(residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isSubmitting = false, errorMessage = error.message) }
                }
        }
    }
}
