package com.resid.manager.features.units.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.resid.manager.dto.AppCurrency
import com.resid.manager.dto.EquipmentDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.UnitStatusDto
import com.resid.manager.features.residences.mvi.ResidenceViewMode
import com.resid.manager.features.residences.usecase.PhotoUploadItem
import com.resid.manager.features.units.UnitsViewModel
import com.resid.manager.features.units.mvi.UnitSortOption
import com.resid.manager.features.units.mvi.UnitStatusFilter
import com.resid.manager.features.units.mvi.UnitsIntent
import com.resid.manager.features.units.mvi.UnitsUiState
import com.resid.manager.features.units.ui.components.CreateUnitContent
import com.resid.manager.features.units.ui.components.EditUnitContent
import com.resid.manager.features.units.ui.components.ResidenceUnitCard
import com.resid.manager.features.units.ui.components.ResidenceUnitListRow
import com.resid.manager.features.units.ui.components.UnitAddCard
import com.resid.manager.features.units.ui.components.UnitDeleteDialog
import com.resid.manager.features.units.ui.components.UnitsStatsRow
import com.resid.manager.navigation.AdaptiveModalSceneStrategy
import com.resid.manager.navigation.PlatformBackHandler
import com.resid.manager.navigation.adaptiveModal
import com.resid.manager.navigation.rememberNavigator
import com.resid.manager.ui.components.AppFilterBar
import com.resid.manager.ui.components.AppTextField
import com.resid.manager.ui.components.FilterTabItem
import com.resid.manager.ui.components.ResidAppBarAction
import com.resid.manager.ui.components.ResidTopAppBar
import com.resid.manager.ui.components.SortOptionItem
import com.resid.manager.ui.theme.ResidTheme
import org.koin.compose.koinInject

@OptIn(
    ExperimentalMaterial3WindowSizeClassApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun UnitsScreen(
    activeResidence: ResidenceContext? = null,
    viewModel: UnitsViewModel = koinInject(),
    onAssignTenantClick: (String) -> Unit = {},
    onCreateUnitClick: () -> Unit = { viewModel.onIntent(UnitsIntent.SetShowCreateDialog(true)) },
    onEditUnitClick: (ResidenceUnitDto) -> Unit = {},
    onNavigateToLease: (String) -> Unit = {},
    onUnitCreated: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(activeResidence?.residenceId) {
        if (activeResidence != null) {
            viewModel.updateActiveResidence(activeResidence)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is com.resid.manager.features.units.mvi.UnitsEffect.UnitCreated -> onUnitCreated(effect.newUnitId)
                is com.resid.manager.features.units.mvi.UnitsEffect.ShowToast -> {}
            }
        }
    }

    val navigator = rememberNavigator(startEntry = UnitsNavKey.List)

    PlatformBackHandler(enabled = navigator.backStack().size > 1) {
        navigator.popBack()
        val currentKey = navigator.current()
        if (currentKey is UnitsNavKey.Detail) {
            viewModel.onIntent(UnitsIntent.SelectUnitForDetail(uiState.units.firstOrNull { it.id == currentKey.unitId }))
        } else {
            viewModel.onIntent(UnitsIntent.SelectUnitForDetail(null))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(maxWidth, maxHeight)
        )
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact || maxWidth < 600.dp

        NavDisplay(
            backStack = navigator.backStack(),
            sceneStrategies = listOf(
                remember(isCompact) {
                    AdaptiveModalSceneStrategy(isCompact = isCompact)
                }
            ),
            onBack = {
                if (navigator.popBack()) {
                    val currentKey = navigator.current()
                    if (currentKey is UnitsNavKey.Detail) {
                        viewModel.onIntent(UnitsIntent.SelectUnitForDetail(uiState.units.firstOrNull { it.id == currentKey.unitId }))
                    } else {
                        viewModel.onIntent(UnitsIntent.SelectUnitForDetail(null))
                    }
                }
            },
            entryProvider = { key: NavKey ->
                when (key) {
                    UnitsNavKey.List -> NavEntry(key) {
                        UnitsGridContent(
                            uiState = uiState,
                            isAuthorized = uiState.isAuthorized,
                            currencySymbol = uiState.currencySymbol,
                            onAddUnitClick = onCreateUnitClick,
                            onDetailClick = { unit ->
                                viewModel.onIntent(UnitsIntent.SelectUnitForDetail(unit))
                                navigator.push(UnitsNavKey.Detail(unit.id))
                            },
                            onPrimaryActionClick = { unit ->
                                if (unit.status == UnitStatusDto.AVAILABLE) {
                                    onAssignTenantClick(unit.id)
                                } else {
                                    val matchedLease = uiState.leases.firstOrNull { it.residenceUnitId == unit.id }
                                    if (matchedLease != null) {
                                        onNavigateToLease(matchedLease.id)
                                    } else {
                                        viewModel.onIntent(UnitsIntent.SelectUnitForDetail(unit))
                                        navigator.push(UnitsNavKey.Detail(unit.id))
                                    }
                                }
                            },
                            onEditClick = { unit ->
                                viewModel.onIntent(UnitsIntent.SetEditingUnit(unit))
                            },
                            onDeleteClick = { unit ->
                                viewModel.onIntent(UnitsIntent.SetDeletingUnitId(unit.id))
                            },
                            onSearchQueryChanged = { query ->
                                viewModel.onIntent(UnitsIntent.SetSearchQuery(query))
                            },
                            onStatusFilterSelected = { filter ->
                                viewModel.onIntent(UnitsIntent.SetStatusFilter(filter))
                            },
                            onSortOptionSelected = { sort ->
                                viewModel.onIntent(UnitsIntent.SetSortOption(sort))
                            },
                            onViewModeSelected = { mode ->
                                viewModel.onIntent(UnitsIntent.SetViewMode(mode))
                            }
                        )
                    }

                    UnitsNavKey.Create -> NavEntry(
                        key = key,
                        metadata = adaptiveModal()
                    ) {
                        CreateUnitContent(
                            currencySymbol = uiState.currencySymbol,
                            isLoading = uiState.isLoading,
                            errorMessage = uiState.errorMessage,
                            onDismiss = {
                                navigator.popBack()
                                viewModel.onIntent(UnitsIntent.SetShowCreateDialog(false))
                            },
                            onSubmit = { name, floor, type, rent, charges, initialIndex, equipmentIds, photos ->
                                val uploadItems = photos.map {
                                    PhotoUploadItem(
                                        url = it.url,
                                        bytes = it.bytes,
                                        description = it.description
                                    )
                                }
                                viewModel.onIntent(
                                    UnitsIntent.CreateUnit(
                                        name = name,
                                        floor = floor,
                                        type = type,
                                        rent = rent,
                                        charges = charges,
                                        initialIndex = initialIndex,
                                        equipmentIds = equipmentIds,
                                        photos = uploadItems
                                    )
                                )
                            }
                        )
                    }

                    is UnitsNavKey.Detail -> NavEntry(key) {
                        val residenceUnit = uiState.units.firstOrNull { it.id == key.unitId }
                            ?: uiState.selectedUnitForDetail
                        if (residenceUnit != null) {
                            LaunchedEffect(residenceUnit.id) {
                                viewModel.onIntent(UnitsIntent.LoadUnitPhotos(residenceUnit.id))
                            }

                            UnitDetailScreen(
                                residenceUnit = residenceUnit,
                                activeResidence = uiState.activeResidence ?: activeResidence,
                                isAuthorized = uiState.isAuthorized,
                                leases = uiState.leases,
                                members = uiState.members,
                                photos = uiState.detailUnitPhotos,
                                isSavingPhotos = uiState.isSavingPhotos,
                                errorMessage = uiState.errorMessage,
                                onBackClick = {
                                    viewModel.onIntent(UnitsIntent.SelectUnitForDetail(null))
                                    navigator.popBack()
                                },
                                onAssignTenantClick = onAssignTenantClick,
                                onEditClick = onEditUnitClick,
                                onDeleteClick = { unitId ->
                                    viewModel.onIntent(UnitsIntent.SetDeletingUnitId(unitId))
                                },
                                onSavePhotos = { photosToUpload ->
                                    viewModel.onIntent(
                                        UnitsIntent.UpdateUnitPhotos(
                                            unitId = residenceUnit.id,
                                            photos = photosToUpload
                                        )
                                    )
                                }
                            )
                        } else {
                            LaunchedEffect(key.unitId) {
                                navigator.popBack()
                            }
                        }
                    }

                    is UnitsNavKey.Edit -> NavEntry(
                        key = key,
                        metadata = adaptiveModal()
                    ) {
                        val targetUnit = uiState.units.firstOrNull { it.id == key.unitId } ?: uiState.editingUnit
                        if (targetUnit != null) {
                            EditUnitContent(
                                residenceUnit = targetUnit,
                                availableEquipments = uiState.availableEquipments,
                                currencySymbol = uiState.currencySymbol,
                                isLoading = uiState.isLoading,
                                errorMessage = uiState.errorMessage,
                                onDismiss = {
                                    navigator.popBack()
                                    viewModel.onIntent(UnitsIntent.SetEditingUnit(null))
                                },
                                onSubmit = { name, floor, type, rent, charges, initialIndex, equipmentIds ->
                                    viewModel.onIntent(
                                        UnitsIntent.UpdateUnit(
                                            unitId = targetUnit.id,
                                            name = name,
                                            floor = floor,
                                            type = type,
                                            rent = rent,
                                            charges = charges,
                                            initialIndex = initialIndex,
                                            equipmentIds = equipmentIds
                                        )
                                    )
                                    navigator.popBack()
                                }
                            )
                        } else {
                            LaunchedEffect(key.unitId) {
                                navigator.popBack()
                            }
                        }
                    }

                    else -> NavEntry(key) {}
                }
            }
        )
    }

    // Synchronisation si le state déclenche l'ouverture de création
    LaunchedEffect(uiState.showCreateDialog) {
        if (uiState.showCreateDialog) {
            if (navigator.current() != UnitsNavKey.Create) {
                navigator.push(UnitsNavKey.Create)
            }
        } else {
            if (navigator.current() == UnitsNavKey.Create) {
                navigator.popBack()
            }
        }
    }

    // Synchronisation si le state déclenche l'ouverture d'édition
    LaunchedEffect(uiState.editingUnit) {
        val editing = uiState.editingUnit
        if (editing != null) {
            val targetKey = UnitsNavKey.Edit(editing.id)
            if (navigator.current() != targetKey) {
                navigator.push(targetKey)
            }
        } else {
            if (navigator.current() is UnitsNavKey.Edit) {
                navigator.popBack()
            }
        }
    }

    // Dialogue de confirmation de suppression
    if (uiState.deletingUnitId != null) {
        val targetId = uiState.deletingUnitId!!
        val unitName = uiState.units.firstOrNull { it.id == targetId }?.name ?: "Ce logement"

        UnitDeleteDialog(
            unitName = unitName,
            onDismiss = { viewModel.onIntent(UnitsIntent.SetDeletingUnitId(null)) },
            onConfirm = {
                viewModel.onIntent(UnitsIntent.DeleteUnit(targetId))
                navigator.popBack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun UnitsGridContent(
    modifier: Modifier = Modifier,
    uiState: UnitsUiState,
    isAuthorized: Boolean,
    currencySymbol: String = AppCurrency.DEFAULT.symbol,
    onAddUnitClick: () -> Unit,
    onDetailClick: (ResidenceUnitDto) -> Unit,
    onPrimaryActionClick: (ResidenceUnitDto) -> Unit = {},
    onEditClick: (ResidenceUnitDto) -> Unit,
    onDeleteClick: (ResidenceUnitDto) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onStatusFilterSelected: (UnitStatusFilter) -> Unit = {},
    onSortOptionSelected: (UnitSortOption) -> Unit = {},
    onViewModeSelected: (ResidenceViewMode) -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            DpSize(maxWidth, maxHeight)
        )
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
        val isMedium = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium
        val viewMode = uiState.viewMode

        // 1. Filtered & Sorted units
        val availableCount = remember(uiState.units) {
            uiState.units.count { it.status == UnitStatusDto.AVAILABLE }
        }
        val occupiedCount = remember(uiState.units) {
            uiState.units.count { it.status == UnitStatusDto.OCCUPIED }
        }

        val filteredUnits = remember(
            uiState.units,
            uiState.searchQuery,
            uiState.statusFilter,
            uiState.sortOption
        ) {
            var result = uiState.units

            // Filter by search query
            if (uiState.searchQuery.isNotBlank()) {
                val q = uiState.searchQuery.trim().lowercase()
                result = result.filter { unit ->
                    unit.name.lowercase().contains(q) ||
                        unit.type.lowercase().contains(q) ||
                        unit.floor.lowercase().contains(q) ||
                        unit.equipments.any { it.label.lowercase().contains(q) }
                }
            }

            // Filter by status
            result = when (uiState.statusFilter) {
                UnitStatusFilter.ALL -> result
                UnitStatusFilter.AVAILABLE -> result.filter { it.status == UnitStatusDto.AVAILABLE }
                UnitStatusFilter.OCCUPIED -> result.filter { it.status == UnitStatusDto.OCCUPIED }
                UnitStatusFilter.OVERDUE -> result.filter { it.status == UnitStatusDto.OCCUPIED } // placeholder for late payment if applicable
            }

            // Sort
            when (uiState.sortOption) {
                UnitSortOption.NAME_ASC -> result.sortedBy { it.name.lowercase() }
                UnitSortOption.RENT_ASC -> result.sortedBy { it.nominalRent }
                UnitSortOption.RENT_DESC -> result.sortedByDescending { it.nominalRent }
            }
        }

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
                } else emptyList()
            )
        }

        // Stats Row
        val statsRowItem: @Composable () -> Unit = {
            UnitsStatsRow(
                isCompact = isCompact,
                units = uiState.units
            )
        }

        // Search & Filter Bar
        val filterBarItem: @Composable () -> Unit = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search field
                AppTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    label = if (isCompact) "Rechercher une unité (A1, Studio)..." else "Rechercher une unité (ex: A1, Studio, Étage 1)...",
                    modifier = Modifier.fillMaxWidth()
                )

                // Shared AppFilterBar with Tabs, Dropdown & View Mode Switcher
                val filterTabs = listOf(
                    FilterTabItem(
                        label = "Tous",
                        count = uiState.units.size,
                        shortLabel = "Tous"
                    ),
                    FilterTabItem(
                        label = "Disponibles",
                        count = availableCount,
                        shortLabel = "Dispo."
                    ),
                    FilterTabItem(
                        label = "Occupés",
                        count = occupiedCount,
                        shortLabel = "Occupés"
                    )
                )

                val selectedFilterIndex = when (uiState.statusFilter) {
                    UnitStatusFilter.ALL -> 0
                    UnitStatusFilter.AVAILABLE -> 1
                    UnitStatusFilter.OCCUPIED -> 2
                    UnitStatusFilter.OVERDUE -> 0
                }

                val sortOptionItems = UnitSortOption.entries.map {
                    SortOptionItem(value = it, label = it.label)
                }

                AppFilterBar(
                    tabs = filterTabs,
                    selectedTabIndex = selectedFilterIndex,
                    onTabSelected = { idx ->
                        val selectedFilter = when (idx) {
                            1 -> UnitStatusFilter.AVAILABLE
                            2 -> UnitStatusFilter.OCCUPIED
                            else -> UnitStatusFilter.ALL
                        }
                        onStatusFilterSelected(selectedFilter)
                    },
                    sortOptions = sortOptionItems,
                    selectedSortOption = uiState.sortOption,
                    onSortOptionSelected = onSortOptionSelected,
                    viewMode = viewMode,
                    onViewModeSelected = onViewModeSelected,
                    isCompact = isCompact
                )
            }
        }

        // Unit count row
        val unitCountItem: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredUnits.size} Logement(s)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        val padding = if (isCompact) 16.dp else 24.dp

        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                topBarItem()
                statsRowItem()
                filterBarItem()
                unitCountItem()
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (uiState.units.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                topBarItem()
                statsRowItem()
                Box(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = "Aucun logement répertorié",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Ajoutez des unités locatives pour suivre l'occupation, les loyers et l'électricité.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (isAuthorized) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onAddUnitClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Créer le premier logement")
                            }
                        }
                    }
                }
            }
        } else if (viewMode == ResidenceViewMode.LIST && !isCompact) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = padding, top = padding, end = padding, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "top_bar") {
                    topBarItem()
                }
                item(key = "stats_row") {
                    statsRowItem()
                }
                item(key = "filter_bar") {
                    filterBarItem()
                }
                item(key = "unit_count") {
                    unitCountItem()
                }
                if (filteredUnits.isEmpty()) {
                    item(key = "empty_filter") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucun logement ne correspond aux filtres sélectionnés.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    items(filteredUnits, key = { it.id }) { unit ->
                        ResidenceUnitListRow(
                            residenceUnit = unit,
                            isAuthorized = isAuthorized,
                            currencySymbol = currencySymbol,
                            onDetailClick = { onDetailClick(unit) },
                            onEditClick = { onEditClick(unit) }
                        )
                    }
                }
            }
        } else {
            val gridColumns = when {
                isCompact -> GridCells.Fixed(1)
                isMedium -> GridCells.Fixed(2)
                else -> GridCells.Adaptive(minSize = 320.dp)
            }

            LazyVerticalGrid(
                columns = gridColumns,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = padding, top = padding, end = padding, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "top_bar") {
                    topBarItem()
                }
                item(span = { GridItemSpan(maxLineSpan) }, key = "stats_row") {
                    statsRowItem()
                }
                item(span = { GridItemSpan(maxLineSpan) }, key = "filter_bar") {
                    filterBarItem()
                }
                item(span = { GridItemSpan(maxLineSpan) }, key = "unit_count") {
                    unitCountItem()
                }

                if (filteredUnits.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "empty_filter") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucun logement ne correspond aux filtres sélectionnés.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    items(filteredUnits, key = { it.id }) { unit ->
                        val matchedLease = uiState.leases.firstOrNull { it.residenceUnitId == unit.id }
                        val tenant = matchedLease?.let { lease ->
                            uiState.members.firstOrNull { it.userId == lease.tenantId }
                        }
                        val tenantDisplayName = tenant?.let { "${it.firstName} ${it.lastName}".trim() }

                        ResidenceUnitCard(
                            residenceUnit = unit,
                            isAuthorized = isAuthorized,
                            currencySymbol = currencySymbol,
                            tenantName = tenantDisplayName,
                            onCardClick = { onDetailClick(unit) },
                            onPrimaryActionClick = { onPrimaryActionClick(unit) },
                            onEditClick = { onEditClick(unit) },
                            onDeleteClick = { onDeleteClick(unit) }
                        )
                    }

                    if (isAuthorized) {
                        item(span = { GridItemSpan(1) }, key = "add_unit_card") {
                            UnitAddCard(
                                onClick = onAddUnitClick,
                                modifier = Modifier.height(280.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun UnitsGridContentPreview() {
    ResidTheme {
        UnitsGridContent(
            uiState = UnitsUiState(
                units = listOf(
                    ResidenceUnitDto(
                        id = "123",
                        residenceId = "12345",
                        name = "Logement 1",
                        floor = "1",
                        type = "Appartement",
                        nominalRent = 500.0,
                        serviceCharges = 100.0,
                        initialElectricityIndex = 0.0,
                        equipments = listOf(
                            EquipmentDto(
                                id = "12",
                                key = "clim",
                                label = "Climatisé"
                            ),
                            EquipmentDto(
                                id = "34",
                                key = "Fibre",
                                label = "Fibre incluse"
                            ),
                            EquipmentDto(
                                id = "56",
                                key = "eau",
                                label = "Eau autonome"
                            ),
                        ),
                        status = UnitStatusDto.OCCUPIED
                    ),
                )
            ),
            isAuthorized = true,
            currencySymbol = AppCurrency.DEFAULT.symbol,
            onAddUnitClick = {},
            onDetailClick = {},
            onEditClick = {}
        )
    }
}