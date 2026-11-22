package com.resid.manager.features.leases.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.LeaseStatusDto
import com.resid.manager.features.leases.mvi.LeasesUiState
import com.resid.manager.features.leases.ui.components.LeaseDetailAdminActions
import com.resid.manager.features.leases.ui.components.LeaseDetailHeader
import com.resid.manager.features.leases.ui.components.LeaseDetailInfoAndLedger
import com.resid.manager.ui.theme.ResidTheme

@Composable
fun LeaseDetailScreen(
    uiState: LeasesUiState,
    key: LeasesNavKey.Detail,
    onBackClick: () -> Unit = { },
    onEditClick: (String) -> Unit = { },
    onDeleteClick: (String) -> Unit = { },
    onRecordPayment: (Double, String, String) -> Unit = { _, _, _ -> },
    onSignContract: (String, LeaseStatusDto) -> Unit = { _, _ -> },
    onTerminateClick: () -> Unit = { },
    goBack: () -> Unit = { },
) {
    val lease = uiState.leases.firstOrNull { it.id == key.leaseId }
        ?: uiState.selectedLeaseForDetail

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (lease != null) {
        val matchedUnit = uiState.units.firstOrNull { it.id == lease.residenceUnitId }
        val matchedTenant = uiState.members.firstOrNull { it.userId == lease.tenantId }

        val isSignedOrActive = lease.status == LeaseStatusDto.SIGNED_ACTIVE
        val isTerminated = lease.status == LeaseStatusDto.TERMINATED

        // Delete rule: Only unsigned leases can be deleted
        val canDelete = !isSignedOrActive && !isTerminated

        // Edit rule: Can edit full wizard if unsigned, or inventory/details if signed/active; cannot edit if terminated
        val canEdit = !isTerminated

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            LeaseDetailHeader(
                lease = lease,
                onBackClick = onBackClick,
                canEdit = canEdit,
                canDelete = canDelete,
                isDeleting = uiState.isDeleting,
                onEditClick = { onEditClick(lease.id) },
                onDeleteClick = { showDeleteConfirmDialog = true }
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val currentCurrencySymbol = uiState.currencySymbol

                LeaseDetailInfoAndLedger(
                    lease = lease,
                    matchedUnit = matchedUnit,
                    matchedTenant = matchedTenant,
                    currencySymbol = currentCurrencySymbol,
                    modifier = Modifier.weight(1.2f)
                )

                LeaseDetailAdminActions(
                    lease = lease,
                    matchedUnit = matchedUnit,
                    currencySymbol = currentCurrencySymbol,
                    onRecordPayment = { amount, category ->
                        onRecordPayment(amount, lease.id, category)
                    },
                    onSignContract = {
                        onSignContract(lease.id, LeaseStatusDto.SIGNED_ACTIVE)
                    },
                    onTerminateClick = onTerminateClick,
                    errorMessage = uiState.errorMessage,
                    modifier = Modifier.weight(0.8f)
                )
            }

            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = {
                        Text("Supprimer le contrat de bail")
                    },
                    text = {
                        Text("Êtes-vous sûr de vouloir supprimer ce contrat de bail ? Cette action est irréversible et libérera le logement associé.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirmDialog = false
                                onDeleteClick(lease.id)
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Supprimer définitivement")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Annuler")
                        }
                    }
                )
            }
        }
    } else {
        LaunchedEffect(key.leaseId) { goBack() }
    }
}

@PreviewLightDark
@Composable
fun LeaseDetailScreenPreview() {
    ResidTheme {
        LeaseDetailScreen(
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
            key = LeasesNavKey.Detail("1")
        )
    }
}