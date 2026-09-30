package com.resid.manager.features.leases.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.LeaseStatusDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.UserRole
import com.resid.manager.features.leases.LeasesViewModel
import com.resid.manager.features.leases.mvi.LeasesIntent
import com.resid.manager.features.leases.mvi.LeasesUiState
import com.resid.manager.features.leases.ui.components.LeaseCard
import com.resid.manager.features.leases.ui.components.LeaseDetailAdminActions
import com.resid.manager.features.leases.ui.components.LeaseDetailHeader
import com.resid.manager.features.leases.ui.components.LeaseDetailInfoAndLedger
import com.resid.manager.features.leases.ui.components.LeaseTerminateDialog
import com.resid.manager.features.leases.ui.components.LeasesFilterCapsuleBar
import com.resid.manager.features.leases.ui.components.LeasesGridHeader
import com.resid.manager.features.leases.ui.components.wizard.LeaseWizardDialog
import org.koin.compose.koinInject

@Composable
fun LeasesScreen(
    activeResidence: ResidenceContext?,
    jwtToken: String?,
    residenceUnits: List<ResidenceUnitDto>,
    members: List<ResidenceMemberSummaryDto>,
    viewModel: LeasesViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val token = jwtToken ?: ""
    val residenceId = activeResidence?.residenceId ?: ""
    val isAuthorized = activeResidence != null && (activeResidence.userRoleInResidence == UserRole.ADMIN || activeResidence.userRoleInResidence == UserRole.MANAGER)

    LaunchedEffect(token, residenceId) {
        if (token.isNotBlank() && residenceId.isNotBlank()) {
            viewModel.onIntent(LeasesIntent.LoadLeases(token, residenceId))
        }
    }

    if (uiState.selectedLeaseForDetail != null) {
        val lease = uiState.selectedLeaseForDetail!!
        val matchedUnit = residenceUnits.firstOrNull { it.id == lease.residenceUnitId }
        val matchedTenant = members.firstOrNull { it.userId == lease.tenantId }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LeaseDetailHeader(
                lease = lease,
                onBackClick = { viewModel.onIntent(LeasesIntent.SelectLeaseForDetail(null)) }
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                LeaseDetailInfoAndLedger(
                    lease = lease,
                    matchedUnit = matchedUnit,
                    matchedTenant = matchedTenant,
                    modifier = Modifier.weight(1.2f)
                )

                LeaseDetailAdminActions(
                    lease = lease,
                    matchedUnit = matchedUnit,
                    onRecordPayment = { amount, category ->
                        viewModel.onIntent(LeasesIntent.RecordPayment(token, residenceId, lease.id, amount, category))
                    },
                    onSignContract = {
                        viewModel.onIntent(LeasesIntent.UpdateStatus(token, residenceId, lease.id, LeaseStatusDto.SIGNED_ACTIVE))
                    },
                    onTerminateClick = {
                        viewModel.onIntent(LeasesIntent.SetShowTerminateConfirmation(true))
                    },
                    modifier = Modifier.weight(0.8f)
                )
            }
        }
    } else {
        LeasesGridContent(
            uiState = uiState,
            residenceUnits = residenceUnits,
            members = members,
            isAuthorized = isAuthorized,
            onNewLeaseClick = { viewModel.onIntent(LeasesIntent.SetShowWizard(true)) },
            onFilterSelected = { filter -> viewModel.onIntent(LeasesIntent.SetFilter(filter)) },
            onLeaseClick = { lease -> viewModel.onIntent(LeasesIntent.SelectLeaseForDetail(lease)) }
        )
    }

    // Dialogs
    if (uiState.showTerminateConfirmation && uiState.selectedLeaseForDetail != null) {
        val lease = uiState.selectedLeaseForDetail!!
        LeaseTerminateDialog(
            onDismiss = { viewModel.onIntent(LeasesIntent.SetShowTerminateConfirmation(false)) },
            onConfirm = {
                viewModel.onIntent(LeasesIntent.UpdateStatus(token, residenceId, lease.id, LeaseStatusDto.TERMINATED))
            }
        )
    }

    if (uiState.showWizard) {
        LeaseWizardDialog(
            jwtToken = jwtToken,
            residenceUnits = residenceUnits,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.onIntent(LeasesIntent.SetShowWizard(false)) },
            onSubmit = { unitId, request ->
                viewModel.onIntent(LeasesIntent.CreateLease(token, residenceId, unitId, request))
            }
        )
    }
}

@Composable
fun LeasesGridContent(
    uiState: LeasesUiState,
    residenceUnits: List<ResidenceUnitDto>,
    members: List<ResidenceMemberSummaryDto>,
    isAuthorized: Boolean,
    onNewLeaseClick: () -> Unit,
    onFilterSelected: (String) -> Unit,
    onLeaseClick: (LeaseDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredLeases = remember(uiState.leases, uiState.activeFilter) {
        val list = uiState.leases
        when (uiState.activeFilter) {
            "ACTIVE" -> list.filter { it.status == LeaseStatusDto.SIGNED_ACTIVE }
            "PENDING" -> list.filter {
                it.status == LeaseStatusDto.PENDING_PAYMENT ||
                it.status == LeaseStatusDto.PENDING_SIGNATURE ||
                it.status == LeaseStatusDto.DOWN_PAYMENT_PAID ||
                it.status == LeaseStatusDto.PARTIALLY_PAID
            }
            else -> list
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            LeasesGridHeader(
                isAuthorized = isAuthorized,
                onNewLeaseClick = onNewLeaseClick
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            LeasesFilterCapsuleBar(
                activeFilter = uiState.activeFilter,
                onFilterSelected = onFilterSelected
            )
        }

        if (filteredLeases.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun contrat ne correspond à ce filtre.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(filteredLeases) { lease ->
                val matchedUnit = residenceUnits.firstOrNull { it.id == lease.residenceUnitId }
                val matchedTenant = members.firstOrNull { it.userId == lease.tenantId }

                LeaseCard(
                    lease = lease,
                    matchedUnit = matchedUnit,
                    matchedTenant = matchedTenant,
                    onClick = { onLeaseClick(lease) }
                )
            }
        }
    }
}
