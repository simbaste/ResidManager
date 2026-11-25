package com.resid.manager.features.leases.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.resid.manager.dto.AppCurrency
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.LeaseStatusDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.features.leases.LeasesViewModel
import com.resid.manager.features.leases.mvi.LeasesIntent
import com.resid.manager.features.leases.mvi.LeasesUiState
import com.resid.manager.features.leases.ui.components.LeaseAddFolderCard
import com.resid.manager.features.leases.ui.components.LeaseCard
import com.resid.manager.features.leases.ui.components.LeaseTerminateDialog
import com.resid.manager.features.leases.ui.components.LeasesFilterCapsuleBar
import com.resid.manager.navigation.PlatformBackHandler
import com.resid.manager.navigation.rememberNavigator
import com.resid.manager.ui.components.ResidAppBarAction
import com.resid.manager.ui.components.ResidTopAppBar
import com.resid.manager.ui.theme.ResidTheme
import org.koin.compose.koinInject

@Composable
fun LeasesScreen(
    activeResidence: ResidenceContext? = null,
    viewModel: LeasesViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(activeResidence?.residenceId) {
        if (activeResidence != null) {
            viewModel.updateActiveResidence(activeResidence)
        }
    }

    val navigator = rememberNavigator(startEntry = LeasesNavKey.List)

    PlatformBackHandler(enabled = navigator.backStack().size > 1) {
        navigator.popBack()
        val currentKey = navigator.current()
        if (currentKey is LeasesNavKey.Detail) {
            viewModel.onIntent(LeasesIntent.SelectLeaseForDetail(uiState.leases.firstOrNull { it.id == currentKey.leaseId }))
        } else {
            viewModel.onIntent(LeasesIntent.SelectLeaseForDetail(null))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = navigator.backStack(),
            onBack = {
                if (navigator.popBack()) {
                    val currentKey = navigator.current()
                    if (currentKey is LeasesNavKey.Detail) {
                        viewModel.onIntent(LeasesIntent.SelectLeaseForDetail(uiState.leases.firstOrNull { it.id == currentKey.leaseId }))
                    } else {
                        viewModel.onIntent(LeasesIntent.SelectLeaseForDetail(null))
                    }
                }
            },
            entryProvider = { key: NavKey ->
                when (key) {
                    LeasesNavKey.List -> NavEntry(key) {
                        LeasesGridContent(
                            uiState = uiState,
                            residenceUnits = uiState.units,
                            members = uiState.members,
                            isAuthorized = uiState.isAuthorized,
                            currencySymbol = uiState.currencySymbol,
                            onNewLeaseClick = { navigator.push(LeasesNavKey.CreateLease("")) },
                            onFilterSelected = { filter -> viewModel.onIntent(LeasesIntent.SetFilter(filter)) },
                            onLeaseClick = { lease ->
                                viewModel.onIntent(LeasesIntent.SelectLeaseForDetail(lease))
                                navigator.push(LeasesNavKey.Detail(lease.id))
                            }
                        )
                    }

                    is LeasesNavKey.Detail -> NavEntry(key) {
                        LeaseDetailScreen(
                            uiState,
                            key,
                            onEditClick = { leaseId ->
                                navigator.push(LeasesNavKey.CreateLease(draftLeaseId = leaseId))
                            },
                            onDeleteClick = { leaseId ->
                                viewModel.onIntent(LeasesIntent.DeleteLease(leaseId))
                                navigator.popBack()
                            },
                            onRecordPayment = { amount, leaseId, category ->
                                viewModel.onIntent(LeasesIntent.RecordPayment(leaseId, amount, category))
                            },
                            onSignContract = { leaseId, status ->
                                viewModel.onIntent(LeasesIntent.UpdateStatus(leaseId, status))
                            },
                            onTerminateClick = {
                                viewModel.onIntent(LeasesIntent.SetShowTerminateConfirmation(true))
                            },
                            goBack = { navigator.popBack() },
                            onBackClick = {
                                viewModel.onIntent(LeasesIntent.SelectLeaseForDetail(null))
                                navigator.popBack()
                            }
                        )
                    }

                    is LeasesNavKey.CreateLease -> NavEntry(key) {
                        LeaseWizardScreen(
                            residenceId = uiState.activeResidence?.residenceId,
                            currencySymbol = uiState.currencySymbol,
                            draftLeaseId = key.draftLeaseId ?: uiState.draftLeaseId,
                            initialResidenceUnitId = key.unitId.ifBlank { null },
                            onNavigateBack = {
                                viewModel.onIntent(LeasesIntent.Refresh)
                                navigator.popBack()
                            }
                        )
                    }

                    else -> NavEntry(key) {}
                }
            }
        )
    }

    // Dialogs
    if (uiState.showTerminateConfirmation && uiState.selectedLeaseForDetail != null) {
        val lease = uiState.selectedLeaseForDetail!!
        LeaseTerminateDialog(
            onDismiss = { viewModel.onIntent(LeasesIntent.SetShowTerminateConfirmation(false)) },
            onConfirm = {
                viewModel.onIntent(LeasesIntent.UpdateStatus(lease.id, LeaseStatusDto.TERMINATED))
            }
        )
    }
}

@Composable
fun LeasesGridContent(
    modifier: Modifier = Modifier,
    uiState: LeasesUiState,
    residenceUnits: List<ResidenceUnitDto>,
    members: List<ResidenceMemberSummaryDto>,
    isAuthorized: Boolean,
    currencySymbol: String = AppCurrency.DEFAULT.symbol,
    onNewLeaseClick: () -> Unit,
    onFilterSelected: (String) -> Unit,
    onLeaseClick: (LeaseDto) -> Unit,
) {
    val filteredLeases = remember(uiState.leases, uiState.activeFilter) {
        val list = uiState.leases
        when (uiState.activeFilter) {
            "DRAFT" -> list.filter { it.status == LeaseStatusDto.DRAFT }
            "ACTIVE" -> list.filter { it.status == LeaseStatusDto.SIGNED_ACTIVE }
            "PENDING" -> list.filter {
                it.status == LeaseStatusDto.PENDING_PAYMENT ||
                it.status == LeaseStatusDto.DOWN_PAYMENT_PAID ||
                it.status == LeaseStatusDto.PARTIALLY_PAID ||
                it.status == LeaseStatusDto.PENDING_SIGNATURE
            }
            "TERMINATED" -> list.filter { it.status == LeaseStatusDto.TERMINATED }
            else -> list
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 350.dp),
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        item(span = { GridItemSpan(maxLineSpan) }) {
            ResidTopAppBar(
                title = "Contrats de bail",
                actions = if (isAuthorized) {
                    listOf(
                        ResidAppBarAction(
                            title = "Nouveau contrat",
                            icon = Icons.Default.Add,
                            onClick = onNewLeaseClick
                        )
                    )
                } else emptyList()
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            LeasesFilterCapsuleBar(
                activeFilter = uiState.activeFilter,
                onFilterSelected = onFilterSelected
            )
        }

        if (uiState.isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredLeases.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aucun bail trouvé",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredLeases) { lease ->
                val unit = residenceUnits.firstOrNull { it.id == lease.residenceUnitId }
                val tenants = members.filter { it.userId == lease.tenantId }
                LeaseCard(
                    lease = lease,
                    matchedUnit = unit,
                    matchedTenant = tenants.firstOrNull(),
                    currencySymbol = currencySymbol,
                    onClick = { onLeaseClick(lease) }
                )
            }
        }

        // Add Folder Card
        if (isAuthorized) {
            item {
                LeaseAddFolderCard(
                    onClick = onNewLeaseClick
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
fun LeasesGridContentPreview() {
    ResidTheme {
        LeasesGridContent(
            uiState = LeasesUiState(
                leases = listOf(
                    LeaseDto(
                        id = "1",
                        residenceUnitId = "1",
                        tenantId = "1",
                        startDate = "2023-01-01",
                        endDate = "2023-12-31",
                        monthlyRentAtSign = 1000.0,
                        depositAmount = 500.0,
                        status = LeaseStatusDto.SIGNED_ACTIVE,
                        payments = emptyList(),
                        guarantor = null,
                        createdAt = "2023-01-01",
                        updatedAt = "2023-01-01"
                    )
                )
            ),
            residenceUnits = emptyList(),
            members = emptyList(),
            onNewLeaseClick = { },
            onFilterSelected = { },
            onLeaseClick = { },
            isAuthorized = true,
        )
    }
}
