package com.resid.manager.features.tickets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.TicketStatusDto
import com.resid.manager.dto.UserRole
import com.resid.manager.features.tickets.TicketsViewModel
import com.resid.manager.features.tickets.mvi.TicketsIntent
import com.resid.manager.features.tickets.mvi.TicketsUiState
import com.resid.manager.features.tickets.ui.components.CloseTicketDialog
import com.resid.manager.features.tickets.ui.components.CreateTicketDialog
import com.resid.manager.features.tickets.ui.components.TakeChargeTicketDialog
import com.resid.manager.features.tickets.ui.components.TicketCard
import com.resid.manager.features.tickets.ui.components.TicketsFilterPanel
import com.resid.manager.features.tickets.ui.components.TicketsHeader
import org.koin.compose.koinInject

@Composable
fun TicketsScreen(
    activeResidence: ResidenceContext?,
    jwtToken: String?,
    residenceUnits: List<ResidenceUnitDto>,
    viewModel: TicketsViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val token = jwtToken ?: ""
    val residenceId = activeResidence?.residenceId ?: ""
    val isAuthorized = activeResidence != null && (activeResidence.userRoleInResidence == UserRole.ADMIN || activeResidence.userRoleInResidence == UserRole.MANAGER)

    LaunchedEffect(token, residenceId) {
        if (token.isNotBlank() && residenceId.isNotBlank()) {
            viewModel.onIntent(TicketsIntent.LoadData(token, residenceId))
        }
    }

    TicketsContent(
        uiState = uiState,
        residenceUnits = residenceUnits,
        isAuthorized = isAuthorized,
        onOpenTicketClick = { viewModel.onIntent(TicketsIntent.SetShowCreateDialog(true)) },
        onStatusFilterChanged = { viewModel.onIntent(TicketsIntent.SetStatusFilter(it)) },
        onUrgencyFilterChanged = { viewModel.onIntent(TicketsIntent.SetUrgencyFilter(it)) },
        onUnitFilterChanged = { viewModel.onIntent(TicketsIntent.SetUnitFilter(it)) },
        onTakeChargeClick = { ticketId -> viewModel.onIntent(TicketsIntent.SetTakeChargeTicketId(ticketId)) },
        onCloseClick = { ticketId -> viewModel.onIntent(TicketsIntent.SetCloseTicketId(ticketId)) }
    )

    if (uiState.showCreateDialog) {
        CreateTicketDialog(
            residenceUnits = residenceUnits,
            categories = uiState.categories,
            isSubmitting = uiState.isSubmitting,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.onIntent(TicketsIntent.SetShowCreateDialog(false)) },
            onSubmit = { unitId, request ->
                viewModel.onIntent(TicketsIntent.CreateTicket(token, residenceId, unitId, request))
            }
        )
    }

    if (uiState.takeChargeTicketId != null) {
        val targetTicketId = uiState.takeChargeTicketId!!
        TakeChargeTicketDialog(
            isSubmitting = uiState.isSubmitting,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.onIntent(TicketsIntent.SetTakeChargeTicketId(null)) },
            onSubmit = { cost, comment ->
                viewModel.onIntent(
                    TicketsIntent.UpdateTicketStatus(
                        token = token,
                        residenceId = residenceId,
                        ticketId = targetTicketId,
                        status = TicketStatusDto.IN_PROGRESS,
                        interventionCost = cost,
                        comment = comment
                    )
                )
            }
        )
    }

    if (uiState.closeTicketId != null) {
        val targetTicketId = uiState.closeTicketId!!
        CloseTicketDialog(
            isSubmitting = uiState.isSubmitting,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.onIntent(TicketsIntent.SetCloseTicketId(null)) },
            onSubmit = { cost, comment ->
                viewModel.onIntent(
                    TicketsIntent.UpdateTicketStatus(
                        token = token,
                        residenceId = residenceId,
                        ticketId = targetTicketId,
                        status = TicketStatusDto.CLOSED,
                        interventionCost = cost,
                        comment = comment
                    )
                )
            }
        )
    }
}

@Composable
fun TicketsContent(
    uiState: TicketsUiState,
    residenceUnits: List<ResidenceUnitDto>,
    isAuthorized: Boolean,
    onOpenTicketClick: () -> Unit,
    onStatusFilterChanged: (String) -> Unit,
    onUrgencyFilterChanged: (String) -> Unit,
    onUnitFilterChanged: (String) -> Unit,
    onTakeChargeClick: (String) -> Unit,
    onCloseClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredTickets = remember(uiState.tickets, uiState.statusFilter, uiState.urgencyFilter, uiState.unitFilterId) {
        uiState.tickets.filter { t ->
            val matchStatus = if (uiState.statusFilter != "ALL") t.status.name == uiState.statusFilter else true
            val matchUrgency = if (uiState.urgencyFilter != "ALL") t.urgency.name == uiState.urgencyFilter else true
            val matchUnit = if (uiState.unitFilterId.isNotEmpty()) t.unitId == uiState.unitFilterId else true
            matchStatus && matchUrgency && matchUnit
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        TicketsHeader(onOpenTicketClick = onOpenTicketClick)

        TicketsFilterPanel(
            statusFilter = uiState.statusFilter,
            urgencyFilter = uiState.urgencyFilter,
            unitFilterId = uiState.unitFilterId,
            residenceUnits = residenceUnits,
            onStatusFilterChanged = onStatusFilterChanged,
            onUrgencyFilterChanged = onUrgencyFilterChanged,
            onUnitFilterChanged = onUnitFilterChanged
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF006948))
            }
        } else if (filteredTickets.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Aucun ticket d'incident ne correspond aux filtres.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(filteredTickets) { ticket ->
                    val matchedUnit = residenceUnits.firstOrNull { it.id == ticket.unitId }
                    val matchedUnitName = matchedUnit?.name ?: "Logement ${ticket.unitId.take(5)}"

                    TicketCard(
                        ticket = ticket,
                        unitName = matchedUnitName,
                        isAuthorized = isAuthorized,
                        onTakeChargeClick = { onTakeChargeClick(ticket.id) },
                        onCloseClick = { onCloseClick(ticket.id) }
                    )
                }
            }
        }
    }
}
