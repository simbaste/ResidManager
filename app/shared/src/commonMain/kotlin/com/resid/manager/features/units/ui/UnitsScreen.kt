package com.resid.manager.features.units.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
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
import com.resid.manager.features.units.ui.components.UnitsStatsRow
import com.resid.manager.navigation.PlatformBackHandler
import com.resid.manager.ui.components.ResidAppBarAction
import com.resid.manager.ui.components.ResidTopAppBar
import org.koin.compose.koinInject

@Composable
fun UnitsScreen(
    activeResidence: ResidenceContext?,
    jwtToken: String?,
    leases: List<LeaseDto>,
    members: List<ResidenceMemberSummaryDto>,
    residenceUnits: List<ResidenceUnitDto> = emptyList(),
    viewModel: UnitsViewModel = koinInject(),
    onAssignTenantClick: (String) -> Unit = {},
    onCreateUnitClick: () -> Unit = {},
    onEditUnitClick: (ResidenceUnitDto) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val token = jwtToken ?: ""
    val residenceId = activeResidence?.residenceId ?: ""
    val isAuthorized = activeResidence != null && (
        activeResidence.userRoleInResidence == UserRole.ADMIN ||
        activeResidence.userRoleInResidence == UserRole.OWNER ||
        activeResidence.userRoleInResidence == UserRole.MANAGER
    )

    LaunchedEffect(token, residenceId, residenceUnits) {
        if (token.isNotBlank() && residenceId.isNotBlank()) {
            viewModel.onIntent(UnitsIntent.LoadUnits(token, residenceId))
        }
    }

    val subBackStack = remember { NavBackStack<UnitsNavKey>(UnitsNavKey.List) }

    PlatformBackHandler(enabled = subBackStack.size > 1) {
        subBackStack.removeLastOrNull()
        val currentKey = subBackStack.lastOrNull()
        if (currentKey is UnitsNavKey.Detail) {
            viewModel.onIntent(UnitsIntent.SelectUnitForDetail(uiState.units.firstOrNull { it.id == currentKey.unitId }))
        } else {
            viewModel.onIntent(UnitsIntent.SelectUnitForDetail(null))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = subBackStack,
            onBack = {
                if (subBackStack.size > 1) {
                    subBackStack.removeLastOrNull()
                    val currentKey = subBackStack.lastOrNull()
                    if (currentKey is UnitsNavKey.Detail) {
                        viewModel.onIntent(UnitsIntent.SelectUnitForDetail(uiState.units.firstOrNull { it.id == currentKey.unitId }))
                    } else {
                        viewModel.onIntent(UnitsIntent.SelectUnitForDetail(null))
                    }
                }
            },
            entryProvider = { key: UnitsNavKey ->
                when (key) {
                    UnitsNavKey.List -> NavEntry(key) {
                        UnitsGridContent(
                            uiState = uiState,
                            isAuthorized = isAuthorized,
                            onAddUnitClick = onCreateUnitClick,
                            onDetailClick = { unit ->
                                viewModel.onIntent(UnitsIntent.SelectUnitForDetail(unit))
                                subBackStack.add(UnitsNavKey.Detail(unit.id))
                            },
                            onEditClick = onEditUnitClick
                        )
                    }

                    is UnitsNavKey.Detail -> NavEntry(key) {
                        val residenceUnit = uiState.units.firstOrNull { it.id == key.unitId }
                            ?: uiState.selectedUnitForDetail
                        if (residenceUnit != null) {
                            UnitDetailScreen(
                                residenceUnit = residenceUnit,
                                activeResidence = activeResidence,
                                isAuthorized = isAuthorized,
                                leases = leases,
                                members = members,
                                onBackClick = {
                                    viewModel.onIntent(UnitsIntent.SelectUnitForDetail(null))
                                    if (subBackStack.size > 1) {
                                        subBackStack.removeLastOrNull()
                                    }
                                },
                                onAssignTenantClick = onAssignTenantClick,
                                onEditClick = onEditUnitClick,
                                onDeleteClick = { unitId ->
                                    viewModel.onIntent(UnitsIntent.SetDeletingUnitId(unitId))
                                }
                            )
                        } else {
                            LaunchedEffect(key.unitId) {
                                if (subBackStack.size > 1) {
                                    subBackStack.removeLastOrNull()
                                }
                            }
                        }
                    }
                }
            }
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
                if (subBackStack.size > 1) {
                    subBackStack.removeLastOrNull()
                }
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
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // 1. En-tête TopAppBar
        item(span = { GridItemSpan(maxLineSpan) }) {
            ResidTopAppBar(
                title = "Gestion des Logements",
                actions = if (isAuthorized) {
                    listOf(
                        ResidAppBarAction(
                            title = "Ajouter un logement",
                            icon = Icons.Default.Add,
                            onClick = onAddUnitClick,
                        )
                    )
                } else {
                    emptyList()
                }
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
