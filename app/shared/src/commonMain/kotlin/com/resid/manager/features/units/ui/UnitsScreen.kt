package com.resid.manager.features.units.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.UserRole
import com.resid.manager.features.units.UnitsViewModel
import com.resid.manager.features.units.mvi.UnitsIntent
import com.resid.manager.features.units.mvi.UnitsUiState
import com.resid.manager.features.units.ui.components.ResidenceUnitCard
import com.resid.manager.features.units.ui.components.UnitAddCard
import com.resid.manager.features.units.ui.components.UnitDeleteDialog
import com.resid.manager.features.units.ui.components.UnitDetailHeader
import com.resid.manager.features.units.ui.components.UnitDetailMediaGallery
import com.resid.manager.features.units.ui.components.UnitDetailTechSpecs
import com.resid.manager.features.units.ui.components.UnitDetailTenantAndStats
import com.resid.manager.features.units.ui.components.UnitsGridHeader
import com.resid.manager.features.units.ui.components.UnitsStatsRow
import org.koin.compose.koinInject

@Composable
fun UnitsScreen(
    activeResidence: ResidenceContext?,
    jwtToken: String?,
    leases: List<LeaseDto>,
    members: List<ResidenceMemberSummaryDto>,
    viewModel: UnitsViewModel = koinInject(),
    onAssignTenantClick: (String) -> Unit = {},
    onCreateUnitClick: () -> Unit = {},
    onEditUnitClick: (ResidenceUnitDto) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val token = jwtToken ?: ""
    val residenceId = activeResidence?.residenceId ?: ""
    val isAuthorized = activeResidence != null && (activeResidence.userRoleInResidence == UserRole.ADMIN || activeResidence.userRoleInResidence == UserRole.MANAGER)

    LaunchedEffect(token, residenceId) {
        if (token.isNotBlank() && residenceId.isNotBlank()) {
            viewModel.onIntent(UnitsIntent.LoadUnits(token, residenceId))
        }
    }

    if (uiState.selectedUnitForDetail != null) {
        val residenceUnit = uiState.selectedUnitForDetail!!
        val activeLease = leases.firstOrNull { it.residenceUnitId == residenceUnit.id }
        val activeTenant = activeLease?.let { lease -> members.firstOrNull { it.userId == lease.tenantId } }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            UnitDetailHeader(
                residenceUnit = residenceUnit,
                residenceName = activeResidence?.residenceName,
                onBackClick = { viewModel.onIntent(UnitsIntent.SelectUnitForDetail(null)) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                UnitDetailTechSpecs(
                    residenceUnit = residenceUnit,
                    modifier = Modifier.weight(7f)
                )

                UnitDetailTenantAndStats(
                    residenceUnit = residenceUnit,
                    activeLease = activeLease,
                    activeTenant = activeTenant,
                    isAuthorized = isAuthorized,
                    onAssignTenantClick = { onAssignTenantClick(residenceUnit.id) },
                    onEditClick = { onEditUnitClick(residenceUnit) },
                    onDeleteClick = { viewModel.onIntent(UnitsIntent.SetDeletingUnitId(residenceUnit.id)) },
                    modifier = Modifier.weight(5f)
                )
            }

            UnitDetailMediaGallery()
        }
    } else {
        UnitsGridContent(
            uiState = uiState,
            isAuthorized = isAuthorized,
            onAddUnitClick = onCreateUnitClick,
            onDetailClick = { unit -> viewModel.onIntent(UnitsIntent.SelectUnitForDetail(unit)) },
            onEditClick = onEditUnitClick
        )
    }

    // Dialogue de confirmation de suppression
    if (uiState.deletingUnitId != null) {
        val targetId = uiState.deletingUnitId!!
        val unitName = uiState.units.firstOrNull { it.id == targetId }?.name ?: "Ce logement"

        UnitDeleteDialog(
            unitName = unitName,
            onDismiss = { viewModel.onIntent(UnitsIntent.SetDeletingUnitId(null)) },
            onConfirm = {
                viewModel.onIntent(UnitsIntent.DeleteUnit(token, residenceId, targetId))
            }
        )
    }
}

@Composable
fun UnitsGridContent(
    uiState: UnitsUiState,
    isAuthorized: Boolean,
    onAddUnitClick: () -> Unit,
    onDetailClick: (ResidenceUnitDto) -> Unit,
    onEditClick: (ResidenceUnitDto) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. En-tête & Bouton
        item(span = { GridItemSpan(maxLineSpan) }) {
            UnitsGridHeader(
                isAuthorized = isAuthorized,
                onAddUnitClick = onAddUnitClick
            )
        }

        // 2. Bento Stats
        item(span = { GridItemSpan(maxLineSpan) }) {
            UnitsStatsRow(units = uiState.units)
        }

        // 3. Cartes des unités
        items(uiState.units) { unit ->
            ResidenceUnitCard(
                residenceUnit = unit,
                isAuthorized = isAuthorized,
                onDetailClick = { onDetailClick(unit) },
                onEditClick = { onEditClick(unit) }
            )
        }

        // 4. Carte d'ajout
        if (isAuthorized) {
            item {
                UnitAddCard(onClick = onAddUnitClick)
            }
        }
    }
}
