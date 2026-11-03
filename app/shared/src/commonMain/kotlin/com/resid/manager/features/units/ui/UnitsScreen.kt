package com.resid.manager.features.units.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.UserRole
import com.resid.manager.features.residences.mvi.ResidenceViewMode
import com.resid.manager.features.units.UnitsViewModel
import com.resid.manager.features.units.mvi.UnitsIntent
import com.resid.manager.features.units.mvi.UnitsUiState
import com.resid.manager.features.units.ui.components.ResidenceUnitCard
import com.resid.manager.features.units.ui.components.ResidenceUnitListRow
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
                            currencySymbol = activeResidence?.currencySymbol ?: com.resid.manager.dto.AppCurrency.DEFAULT.symbol,
                            onAddUnitClick = onCreateUnitClick,
                            onDetailClick = { unit ->
                                viewModel.onIntent(UnitsIntent.SelectUnitForDetail(unit))
                                subBackStack.add(UnitsNavKey.Detail(unit.id))
                            },
                            onEditClick = onEditUnitClick,
                            onViewModeSelected = { mode ->
                                viewModel.onIntent(UnitsIntent.SetViewMode(mode))
                            }
                        )
                    }

                    is UnitsNavKey.Detail -> NavEntry(key) {
                        val residenceUnit = uiState.units.firstOrNull { it.id == key.unitId }
                            ?: uiState.selectedUnitForDetail
                        if (residenceUnit != null) {
                            LaunchedEffect(residenceUnit.id, token, residenceId) {
                                if (token.isNotBlank() && residenceId.isNotBlank()) {
                                    viewModel.onIntent(UnitsIntent.LoadUnitPhotos(token, residenceId, residenceUnit.id))
                                }
                            }

                            UnitDetailScreen(
                                residenceUnit = residenceUnit,
                                activeResidence = activeResidence,
                                isAuthorized = isAuthorized,
                                leases = leases,
                                members = members,
                                photos = uiState.detailUnitPhotos,
                                isSavingPhotos = uiState.isSavingPhotos,
                                errorMessage = uiState.errorMessage,
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
                                },
                                onSavePhotos = { photosToUpload ->
                                    viewModel.onIntent(
                                        UnitsIntent.UpdateUnitPhotos(
                                            token = token,
                                            residenceId = residenceId,
                                            unitId = residenceUnit.id,
                                            photos = photosToUpload
                                        )
                                    )
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

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun UnitsGridContent(
    uiState: UnitsUiState,
    isAuthorized: Boolean,
    currencySymbol: String = com.resid.manager.dto.AppCurrency.DEFAULT.symbol,
    onAddUnitClick: () -> Unit,
    onDetailClick: (ResidenceUnitDto) -> Unit,
    onEditClick: (ResidenceUnitDto) -> Unit,
    onViewModeSelected: (ResidenceViewMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(maxWidth, maxHeight)
        )
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
        val isMedium = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium
        val viewMode = uiState.viewMode

        // Header TopAppBar
        val topBarItem: @Composable () -> Unit = {
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

        // Stats Row
        val statsItem: @Composable () -> Unit = {
            UnitsStatsRow(units = uiState.units)
        }

        // Control / ViewMode Bar
        val controlBarItem: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.units.size} logement" + if (uiState.units.size > 1) "s" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Toggle Grille / Liste
                if (!isCompact) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(2.dp)
                    ) {
                        IconButton(
                            onClick = { onViewModeSelected(ResidenceViewMode.GRID) },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (viewMode == ResidenceViewMode.GRID) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Vue grille",
                                tint = if (viewMode == ResidenceViewMode.GRID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { onViewModeSelected(ResidenceViewMode.LIST) },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (viewMode == ResidenceViewMode.LIST) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
                                )
                        ) {
                            @Suppress("DEPRECATION")
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Vue liste",
                                tint = if (viewMode == ResidenceViewMode.LIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (viewMode == ResidenceViewMode.LIST && !isCompact) {
            // Vue Liste (Desktop / Tablette)
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(if (isCompact) 16.dp else 24.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "top_bar") { topBarItem() }
                item(key = "stats_row") { statsItem() }
                item(key = "control_bar") { controlBarItem() }

                items(uiState.units, key = { it.id }) { unit ->
                    ResidenceUnitListRow(
                        residenceUnit = unit,
                        isAuthorized = isAuthorized,
                        currencySymbol = currencySymbol,
                        onDetailClick = { onDetailClick(unit) },
                        onEditClick = { onEditClick(unit) }
                    )
                }
            }
        } else {
            // Vue Grille Responsive (1 colonne mobile, 2 colonnes tablette, 3-4 colonnes desktop)
            val minColSize = if (isCompact) 280.dp else if (isMedium) 320.dp else 360.dp

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = minColSize),
                modifier = Modifier.fillMaxSize().padding(if (isCompact) 16.dp else 24.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // 1. En-tête TopAppBar
                item(span = { GridItemSpan(maxLineSpan) }, key = "top_bar") {
                    topBarItem()
                }

                // 2. Bento Stats
                item(span = { GridItemSpan(maxLineSpan) }, key = "stats_row") {
                    statsItem()
                }

                // 3. Barre de contrôle (Compteur & Toggle)
                item(span = { GridItemSpan(maxLineSpan) }, key = "control_bar") {
                    controlBarItem()
                }

                // 4. Cartes des unités
                items(uiState.units, key = { it.id }) { unit ->
                    ResidenceUnitCard(
                        residenceUnit = unit,
                        isAuthorized = isAuthorized,
                        currencySymbol = currencySymbol,
                        onDetailClick = { onDetailClick(unit) },
                        onEditClick = { onEditClick(unit) }
                    )
                }

                // 5. Carte d'ajout
                if (isAuthorized) {
                    item(key = "add_card") {
                        UnitAddCard(onClick = onAddUnitClick)
                    }
                }
            }
        }
    }
}
