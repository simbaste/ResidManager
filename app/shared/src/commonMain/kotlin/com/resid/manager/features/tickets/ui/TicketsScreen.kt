package com.resid.manager.features.tickets.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.TicketStatusDto
import com.resid.manager.features.tickets.TicketsViewModel
import com.resid.manager.features.tickets.mvi.TicketsIntent
import com.resid.manager.features.tickets.mvi.TicketsUiState
import com.resid.manager.features.tickets.ui.components.CloseTicketDialog
import com.resid.manager.features.tickets.ui.components.CreateTicketDialog
import com.resid.manager.features.tickets.ui.components.TakeChargeTicketDialog
import com.resid.manager.features.tickets.ui.components.TicketCard
import com.resid.manager.features.tickets.ui.components.TicketsFilterPanel
import com.resid.manager.ui.components.ResidAppBarAction
import com.resid.manager.ui.components.ResidTopAppBar
import com.resid.manager.ui.theme.ResidTheme
import org.koin.compose.koinInject

@Composable
fun TicketsScreen(
    activeResidence: ResidenceContext? = null,
    viewModel: TicketsViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(activeResidence?.residenceId) {
        if (activeResidence != null) {
            viewModel.updateActiveResidence(activeResidence)
        }
    }

    val effectiveCurrencySymbol = uiState.currencySymbol

    TicketsContent(
        uiState = uiState,
        residenceUnits = uiState.residenceUnits,
        isAuthorized = uiState.isAuthorized,
        onOpenTicketClick = { viewModel.onIntent(TicketsIntent.SetShowCreateDialog(true)) },
        onStatusFilterChanged = { viewModel.onIntent(TicketsIntent.SetStatusFilter(it)) },
        onUrgencyFilterChanged = { viewModel.onIntent(TicketsIntent.SetUrgencyFilter(it)) },
        onUnitFilterChanged = { viewModel.onIntent(TicketsIntent.SetUnitFilter(it)) },
        onTakeChargeClick = { ticketId -> viewModel.onIntent(TicketsIntent.SetTakeChargeTicketId(ticketId)) },
        onCloseClick = { ticketId -> viewModel.onIntent(TicketsIntent.SetCloseTicketId(ticketId)) }
    )

    if (uiState.showCreateDialog) {
        CreateTicketDialog(
            residenceUnits = uiState.residenceUnits,
            categories = uiState.categories,
            isSubmitting = uiState.isSubmitting,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.onIntent(TicketsIntent.SetShowCreateDialog(false)) },
            onSubmit = { unitId, request ->
                viewModel.onIntent(TicketsIntent.CreateTicket(unitId, request))
            }
        )
    }

    if (uiState.takeChargeTicketId != null) {
        val targetTicketId = uiState.takeChargeTicketId!!
        TakeChargeTicketDialog(
            isSubmitting = uiState.isSubmitting,
            errorMessage = uiState.errorMessage,
            currencySymbol = effectiveCurrencySymbol,
            onDismiss = { viewModel.onIntent(TicketsIntent.SetTakeChargeTicketId(null)) },
            onSubmit = { cost, comment ->
                viewModel.onIntent(
                    TicketsIntent.UpdateTicketStatus(
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
            currencySymbol = effectiveCurrencySymbol,
            onDismiss = { viewModel.onIntent(TicketsIntent.SetCloseTicketId(null)) },
            onSubmit = { cost, comment ->
                viewModel.onIntent(
                    TicketsIntent.UpdateTicketStatus(
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

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
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
            val matchStatus = uiState.statusFilter == "ALL" || t.status.name == uiState.statusFilter
            val matchUrgency = uiState.urgencyFilter == "ALL" || t.urgency.name == uiState.urgencyFilter
            val matchUnit = uiState.unitFilterId.isEmpty() || t.unitId == uiState.unitFilterId
            matchStatus && matchUrgency && matchUnit
        }
    }

    BoxWithConstraints {
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(maxWidth, maxHeight)
        )
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact || maxWidth < 600.dp
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ResidTopAppBar(
                title = "Tickets d'Intervention",
                actions = listOf(
                    ResidAppBarAction(
                        title = "Nouveau Ticket",
                        icon = Icons.Default.Add,
                        onClick = onOpenTicketClick
                    )
                )
            )

            TicketsFilterPanel(
                statusFilter = uiState.statusFilter,
                urgencyFilter = uiState.urgencyFilter,
                unitFilterId = uiState.unitFilterId,
                residenceUnits = residenceUnits,
                isCompact = isCompact,
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
}

@PreviewLightDark
@Composable
fun TicketsContentPreview() {
    ResidTheme {
        TicketsContent(
            uiState = TicketsUiState(),
            residenceUnits = emptyList(),
            isAuthorized = true,
            onOpenTicketClick = {},
            onStatusFilterChanged = {},
            onUrgencyFilterChanged = {},
            onUnitFilterChanged = {},
            onTakeChargeClick = {},
            onCloseClick = {}
        )
    }
}