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
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.features.residences.mvi.ResidenceViewMode
import com.resid.manager.features.residences.usecase.PhotoUploadItem
import com.resid.manager.features.units.UnitsViewModel
import com.resid.manager.features.units.mvi.UnitsIntent
import com.resid.manager.features.units.mvi.UnitsUiState
import com.resid.manager.features.units.ui.components.CreateUnitContent
import com.resid.manager.features.units.ui.components.ResidenceUnitCard
import com.resid.manager.features.units.ui.components.ResidenceUnitListRow
import com.resid.manager.features.units.ui.components.UnitAddCard
import com.resid.manager.features.units.ui.components.UnitDeleteDialog
import com.resid.manager.features.units.ui.components.UnitsStatsRow
import com.resid.manager.navigation.AdaptiveModalSceneStrategy
import com.resid.manager.navigation.PlatformBackHandler
import com.resid.manager.navigation.adaptiveModal
import com.resid.manager.navigation.rememberNavigator
import com.resid.manager.ui.components.ResidAppBarAction
import com.resid.manager.ui.components.ResidTopAppBar
import org.koin.compose.koinInject

@OptIn(
    ExperimentalMaterial3WindowSizeClassApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
@Composable
fun UnitsScreen(
    activeResidence: ResidenceContext? = null,
    viewModel: UnitsViewModel = koinInject(),
    onAssignTenantClick: (String) -> Unit = {},
    onCreateUnitClick: () -> Unit = { viewModel.onIntent(UnitsIntent.SetShowCreateDialog(true)) },
    onEditUnitClick: (ResidenceUnitDto) -> Unit = {},
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
                            onEditClick = onEditUnitClick,
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
                } else emptyList()
            )
        }

        // Stats Row
        val statsRowItem: @Composable () -> Unit = {
            UnitsStatsRow(
                units = uiState.units
            )
        }

        // View Mode Switcher
        val viewModeSwitcherItem: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.units.size} Logement(s)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (!isCompact) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onViewModeSelected(ResidenceViewMode.GRID) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Vue Grille",
                                tint = if (viewMode == ResidenceViewMode.GRID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }

                        IconButton(
                            onClick = { onViewModeSelected(ResidenceViewMode.LIST) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = "Vue Liste",
                                tint = if (viewMode == ResidenceViewMode.LIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        val padding = if (isCompact) 16.dp else 24.dp

        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            topBarItem()
            statsRowItem()
            viewModeSwitcherItem()

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.units.isEmpty()) {
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
            } else if (viewMode == ResidenceViewMode.LIST && !isCompact) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                val gridColumns = when {
                    isCompact -> GridCells.Fixed(1)
                    isMedium -> GridCells.Fixed(2)
                    else -> GridCells.Adaptive(minSize = 320.dp)
                }

                LazyVerticalGrid(
                    columns = gridColumns,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.units, key = { it.id }) { unit ->
                        ResidenceUnitCard(
                            residenceUnit = unit,
                            isAuthorized = isAuthorized,
                            currencySymbol = currencySymbol,
                            onDetailClick = { onDetailClick(unit) },
                            onEditClick = { onEditClick(unit) }
                        )
                    }

                    if (isAuthorized) {
                        item(span = { GridItemSpan(1) }) {
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
