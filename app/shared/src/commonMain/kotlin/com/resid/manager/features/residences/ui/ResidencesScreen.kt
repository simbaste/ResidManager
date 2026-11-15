package com.resid.manager.features.residences.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.features.residences.ResidencesViewModel
import com.resid.manager.features.residences.mvi.ResidenceSortOption
import com.resid.manager.features.residences.mvi.ResidenceViewMode
import com.resid.manager.features.residences.mvi.ResidencesEffect
import com.resid.manager.features.residences.mvi.ResidencesIntent
import com.resid.manager.features.residences.mvi.ResidencesUiState
import com.resid.manager.features.residences.ui.components.CreateResidenceContent
import com.resid.manager.features.residences.ui.components.DeleteResidenceDialog
import com.resid.manager.features.residences.ui.components.EditResidenceDialog
import com.resid.manager.features.residences.ui.components.JoinResidenceDialog
import com.resid.manager.features.residences.ui.components.ResidenceFilterBar
import com.resid.manager.features.residences.ui.components.ResidenceGalleryContent
import com.resid.manager.features.residences.ui.components.ResidenceGridCard
import com.resid.manager.features.residences.ui.components.ResidenceListRow
import com.resid.manager.features.residences.ui.components.ResidenceMetricCard
import com.resid.manager.features.residences.ui.components.ResidenceSkeletonGrid
import com.resid.manager.features.residences.ui.components.ResidencesEmptyState
import com.resid.manager.features.residences.usecase.PhotoUploadItem
import com.resid.manager.navigation.AdaptiveModalSceneStrategy
import com.resid.manager.navigation.PlatformBackHandler
import com.resid.manager.navigation.adaptiveModal
import com.resid.manager.ui.components.GalleryPhotoItem
import com.resid.manager.ui.components.ResidAppBarAction
import com.resid.manager.ui.components.ResidTopAppBar
import com.resid.manager.ui.theme.ResidTheme
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
sealed interface ResidencesNavKey : NavKey {
    @Serializable
    data object Directory : ResidencesNavKey

    @Serializable
    data class Detail(val residenceId: String) : ResidencesNavKey

    @Serializable
    data object Create : ResidencesNavKey

    @Serializable
    data class Gallery(val residenceId: String) : ResidencesNavKey
}

@OptIn(
    ExperimentalMaterial3WindowSizeClassApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
@Composable
fun ResidencesScreen(
    activeResidenceId: String? = null,
    viewModel: ResidencesViewModel = koinInject(),
    onNavigateToDashboard: (ResidenceContext) -> Unit = {},
    onResidenceActivated: (ResidenceContext) -> Unit = onNavigateToDashboard,
    onResidenceCreated: (String) -> Unit = {},
    onNavigateToUnits: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(activeResidenceId) {
        if (activeResidenceId != null) {
            viewModel.onIntent(ResidencesIntent.SetSelectedResidenceId(activeResidenceId))
        }
    }

    val navigator = com.resid.manager.navigation.rememberNavigator(
        startEntry = ResidencesNavKey.Directory
    )

    // Synchronize navigator if selectedResidenceForDetail is restored or changed in ViewModel
    LaunchedEffect(uiState.selectedResidenceForDetail) {
        val detailRes = uiState.selectedResidenceForDetail
        if (detailRes != null) {
            val targetKey = ResidencesNavKey.Detail(detailRes.residenceId)
            if (navigator.current() != targetKey) {
                navigator.push(targetKey)
            }
        } else {
            // Returning to directory
            if (navigator.current() is ResidencesNavKey.Detail) {
                navigator.popBack()
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ResidencesEffect.NavigateToDashboard -> onNavigateToDashboard(effect.residence)
                is ResidencesEffect.NavigateToDetail -> {
                    viewModel.onIntent(ResidencesIntent.SelectResidenceForDetail(effect.residence))
                    val targetKey = ResidencesNavKey.Detail(effect.residence.residenceId)
                    if (navigator.current() != targetKey) {
                        navigator.push(targetKey)
                    }
                }
                is ResidencesEffect.ResidenceCreated -> onResidenceCreated(effect.newResidenceId)
                is ResidencesEffect.ResidenceActivated -> onResidenceActivated(effect.residence)
                is ResidencesEffect.ShowToast -> {}
            }
        }
    }

    // Platform-native back action (interacts with browser history popstate / Android system back)
    PlatformBackHandler(enabled = navigator.backStack().size > 1) {
        navigator.popBack()
        val currentKey = navigator.current()
        if (currentKey is ResidencesNavKey.Detail) {
            viewModel.onIntent(
                ResidencesIntent.SelectResidenceForDetail(
                    uiState.residences.firstOrNull { it.residenceId == currentKey.residenceId }
                )
            )
        } else {
            viewModel.onIntent(ResidencesIntent.SelectResidenceForDetail(null))
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
                    if (currentKey is ResidencesNavKey.Detail) {
                        viewModel.onIntent(
                            ResidencesIntent.SelectResidenceForDetail(
                                uiState.residences.firstOrNull { it.residenceId == currentKey.residenceId }
                            )
                        )
                    } else {
                        viewModel.onIntent(ResidencesIntent.SelectResidenceForDetail(null))
                    }
                }
            },
            entryProvider = { key: NavKey ->
                when (key) {
                    ResidencesNavKey.Directory -> NavEntry(key) {
                        ResidencesContent(
                            uiState = uiState,
                            onIntent = viewModel::onIntent,
                            onResidenceSelected = { residence ->
                                viewModel.onIntent(ResidencesIntent.SelectResidenceForDetail(residence))
                                navigator.push(ResidencesNavKey.Detail(residence.residenceId))
                            },
                            onActivateResidence = { residence ->
                                viewModel.onIntent(ResidencesIntent.SelectResidence(residence))
                                onResidenceActivated(residence)
                            }
                        )
                    }

                    is ResidencesNavKey.Detail -> NavEntry(key) {
                        val residence = uiState.residences.firstOrNull { it.residenceId == key.residenceId }
                            ?: uiState.selectedResidenceForDetail

                        if (residence != null) {
                            LaunchedEffect(residence.residenceId) {
                                viewModel.onIntent(ResidencesIntent.LoadResidencePhotos(residence.residenceId))
                            }

                            ResidenceDetailScreen(
                                residence = residence,
                                isActive = residence.residenceId == uiState.selectedResidenceId,
                                galleryPhotos = uiState.detailResidencePhotos,
                                onBackClick = {
                                    navigator.popBack()
                                    viewModel.onIntent(ResidencesIntent.SelectResidenceForDetail(null))
                                },
                                onActivateResidence = {
                                    viewModel.onIntent(ResidencesIntent.SelectResidence(residence))
                                    onResidenceActivated(residence)
                                },
                                onEditClick = { viewModel.onIntent(ResidencesIntent.SetEditingResidence(residence)) },
                                onDeleteClick = { viewModel.onIntent(ResidencesIntent.SetDeletingResidence(residence)) },
                                onNavigateToUnits = onNavigateToUnits,
                                onEditGalleryClick = {
                                    navigator.push(ResidencesNavKey.Gallery(residence.residenceId))
                                }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Résidence introuvable",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    ResidencesNavKey.Create -> NavEntry(
                        key = key,
                        metadata = adaptiveModal()
                    ) {
                        CreateResidenceContent(
                            isLoading = uiState.isLoading,
                            errorMessage = uiState.errorMessage,
                            onDismiss = {
                                navigator.popBack()
                                viewModel.onIntent(ResidencesIntent.SetShowCreateDialog(false))
                            },
                            onSubmit = { name, address, currency, price, imageBytes ->
                                viewModel.onIntent(ResidencesIntent.CreateResidence(name, address, currency, price, imageBytes))
                            }
                        )
                    }

                    is ResidencesNavKey.Gallery -> NavEntry(
                        key = key,
                        metadata = adaptiveModal()
                    ) {
                        val targetResidence = uiState.residences.firstOrNull { it.residenceId == key.residenceId }
                            ?: uiState.selectedResidenceForDetail

                        LaunchedEffect(key.residenceId) {
                            viewModel.onIntent(ResidencesIntent.LoadResidencePhotos(key.residenceId))
                        }

                        val photos = uiState.detailResidencePhotos
                        val galleryItems = remember(photos, targetResidence) {
                            if (photos.isNotEmpty()) {
                                photos.mapIndexed { index, p ->
                                    GalleryPhotoItem(
                                        id = "photo_$index",
                                        url = p.url,
                                        description = p.description
                                    )
                                }
                            } else if (!targetResidence?.photoUrl.isNullOrBlank()) {
                                listOf(
                                    GalleryPhotoItem(
                                        id = "primary",
                                        url = targetResidence.photoUrl,
                                        description = "Photo principale"
                                    )
                                )
                            } else {
                                emptyList()
                            }
                        }

                        ResidenceGalleryContent(
                            initialPhotos = galleryItems,
                            isLoading = uiState.isSavingPhotos,
                            errorMessage = uiState.errorMessage,
                            onDismiss = {
                                navigator.popBack()
                            },
                            onSave = { items ->
                                val uploadItems = items.map {
                                    PhotoUploadItem(
                                        url = it.url,
                                        bytes = it.bytes,
                                        description = it.description
                                    )
                                }
                                viewModel.onIntent(ResidencesIntent.UpdateResidencePhotos(key.residenceId, uploadItems))
                                navigator.popBack()
                            }
                        )
                    }

                    else -> NavEntry(key) {}
                }
            }
        )
    }

    // Synchronisation si le state déclenche l'ouverture de création
    LaunchedEffect(uiState.showCreateDialog) {
        if (uiState.showCreateDialog) {
            if (navigator.current() != ResidencesNavKey.Create) {
                navigator.push(ResidencesNavKey.Create)
            }
        } else {
            if (navigator.current() == ResidencesNavKey.Create) {
                navigator.popBack()
            }
        }
    }

    if (uiState.editingResidence != null) {
        val target = uiState.editingResidence!!
        EditResidenceDialog(
            residence = target,
            onDismiss = { viewModel.onIntent(ResidencesIntent.SetEditingResidence(null)) },
            onSubmit = { name, address, price, imageBytes ->
                viewModel.onIntent(ResidencesIntent.UpdateResidence(target.residenceId, name, address, price, imageBytes))
            },
            onOpenGallery = {
                viewModel.onIntent(ResidencesIntent.SetEditingResidence(null))
                navigator.push(ResidencesNavKey.Gallery(target.residenceId))
            }
        )
    }

    if (uiState.deletingResidence != null) {
        val target = uiState.deletingResidence!!
        DeleteResidenceDialog(
            residenceName = target.residenceName,
            onDismiss = { viewModel.onIntent(ResidencesIntent.SetDeletingResidence(null)) },
            onConfirm = {
                viewModel.onIntent(ResidencesIntent.DeleteResidence(target.residenceId))
            }
        )
    }

    if (uiState.showJoinDialog) {
        JoinResidenceDialog(
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            isSearching = uiState.isSearching,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            existingResidenceIds = remember(uiState.residences) {
                uiState.residences.map { it.residenceId }.toSet()
            },
            onSearchQueryChanged = { query ->
                viewModel.onIntent(ResidencesIntent.SearchResidences(query))
            },
            onDismiss = { viewModel.onIntent(ResidencesIntent.SetShowJoinDialog(false)) },
            onSubmit = { residenceId ->
                viewModel.onIntent(ResidencesIntent.JoinResidence(residenceId))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ResidencesContent(
    uiState: ResidencesUiState,
    onIntent: (ResidencesIntent) -> Unit,
    onResidenceSelected: (ResidenceContext) -> Unit,
    onActivateResidence: (ResidenceContext) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedId = uiState.selectedResidenceId

    // 1. Sort residences according to sortOption
    val sortedResidences = remember(uiState.residences, uiState.sortOption, uiState.consultedTimestamps, selectedId) {
        val list = uiState.residences
        when (uiState.sortOption) {
            ResidenceSortOption.SELECTED_FIRST -> {
                list.sortedWith(
                    compareByDescending<ResidenceContext> { it.residenceId == selectedId }
                        .thenBy { it.residenceName.lowercase() }
                )
            }
            ResidenceSortOption.LAST_CONSULTED -> {
                list.sortedWith(
                    compareByDescending<ResidenceContext> { it.residenceId == selectedId }
                        .thenByDescending { uiState.consultedTimestamps[it.residenceId] ?: 0L }
                        .thenBy { it.residenceName.lowercase() }
                )
            }
            ResidenceSortOption.NAME_ASC -> {
                list.sortedWith(
                    compareByDescending<ResidenceContext> { it.residenceId == selectedId }
                        .thenBy { it.residenceName.lowercase() }
                )
            }
        }
    }

    val ownedResidences = sortedResidences.filter { it.userRoleInResidence == UserRole.OWNER }
    val associatedResidences = sortedResidences.filter { it.userRoleInResidence != UserRole.OWNER }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val currentResidences = if (selectedTabIndex == 0) ownedResidences else associatedResidences

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
        val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
        val isMedium = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompact) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Top Bar
            ResidTopAppBar(
                title = "Annuaire des Résidences",
                actions = listOf(
                    ResidAppBarAction(
                        title = "Créer une résidence",
                        icon = Icons.Default.Add,
                        onClick = { onIntent(ResidencesIntent.SetShowCreateDialog(true)) },
                    ),
                ),
            )

            if (uiState.isLoading && uiState.residences.isEmpty()) {
                ResidencesMetricsBanner(
                    residences = uiState.residences,
                    ownedResidencesCount = ownedResidences.size,
                    associatedResidencesCount = associatedResidences.size,
                    isCompact = isCompact
                )
                ResidenceSkeletonGrid(modifier = Modifier.weight(1f))
            } else if (uiState.residences.isEmpty()) {
                ResidencesMetricsBanner(
                    residences = uiState.residences,
                    ownedResidencesCount = ownedResidences.size,
                    associatedResidencesCount = associatedResidences.size,
                    isCompact = isCompact
                )
                ResidencesEmptyState(
                    onCreateClick = { onIntent(ResidencesIntent.SetShowCreateDialog(true)) },
                    onJoinClick = { onIntent(ResidencesIntent.SetShowJoinDialog(true)) }
                )
            } else {
                if (uiState.viewMode == ResidenceViewMode.LIST && !isCompact) {
                    // List View on Desktop & Tablet
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(key = "metrics_banner") {
                            ResidencesMetricsBanner(
                                residences = uiState.residences,
                                ownedResidencesCount = ownedResidences.size,
                                associatedResidencesCount = associatedResidences.size,
                                isCompact = isCompact
                            )
                        }
                        item(key = "filter_bar") {
                            ResidenceFilterBar(
                                selectedTabIndex = selectedTabIndex,
                                ownedCount = ownedResidences.size,
                                associatedCount = associatedResidences.size,
                                onTabSelected = { selectedTabIndex = it },
                                sortOption = uiState.sortOption,
                                onSortOptionSelected = { onIntent(ResidencesIntent.SetSortOption(it)) },
                                viewMode = uiState.viewMode,
                                onViewModeSelected = { onIntent(ResidencesIntent.SetViewMode(it)) },
                                isCompact = isCompact
                            )
                        }
                        if (currentResidences.isEmpty()) {
                            item(key = "empty_tab") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (selectedTabIndex == 0) {
                                            "Aucune propriété gérée pour le moment."
                                        } else {
                                            "Aucune propriété associée pour le moment."
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(currentResidences, key = { it.residenceId }) { residence ->
                                val isSelected = residence.residenceId == selectedId
                                ResidenceListRow(
                                    residence = residence,
                                    isActive = isSelected,
                                    onClick = { onResidenceSelected(residence) },
                                    onManageClick = {
                                        if (isSelected) {
                                            onResidenceSelected(residence)
                                        } else {
                                            onActivateResidence(residence)
                                        }
                                    },
                                    onEditClick = { onIntent(ResidencesIntent.SetEditingResidence(residence)) },
                                    onDeleteClick = { onIntent(ResidencesIntent.SetDeletingResidence(residence)) }
                                )
                            }
                        }
                    }
                } else {
                    // Responsive Grid View: 1 column on compact mobile, 2 on tablet/medium, 3 on expanded desktop
                    val minColSize = if (isCompact) 280.dp else if (isMedium) 320.dp else 360.dp
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = minColSize),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "metrics_banner") {
                            ResidencesMetricsBanner(
                                residences = uiState.residences,
                                ownedResidencesCount = ownedResidences.size,
                                associatedResidencesCount = associatedResidences.size,
                                isCompact = isCompact
                            )
                        }
                        item(span = { GridItemSpan(maxLineSpan) }, key = "filter_bar") {
                            ResidenceFilterBar(
                                selectedTabIndex = selectedTabIndex,
                                ownedCount = ownedResidences.size,
                                associatedCount = associatedResidences.size,
                                onTabSelected = { selectedTabIndex = it },
                                sortOption = uiState.sortOption,
                                onSortOptionSelected = { onIntent(ResidencesIntent.SetSortOption(it)) },
                                viewMode = uiState.viewMode,
                                onViewModeSelected = { onIntent(ResidencesIntent.SetViewMode(it)) },
                                isCompact = isCompact
                            )
                        }
                        if (currentResidences.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "empty_tab") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (selectedTabIndex == 0) {
                                            "Aucune propriété gérée pour le moment."
                                        } else {
                                            "Aucune propriété associée pour le moment."
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(currentResidences, key = { it.residenceId }) { residence ->
                                val isSelected = residence.residenceId == selectedId
                                ResidenceGridCard(
                                    residence = residence,
                                    isActive = isSelected,
                                    onClick = { onResidenceSelected(residence) },
                                    onManageClick = {
                                        if (isSelected) {
                                            onResidenceSelected(residence)
                                        } else {
                                            onActivateResidence(residence)
                                        }
                                    },
                                    onEditClick = { onIntent(ResidencesIntent.SetEditingResidence(residence)) },
                                    onDeleteClick = { onIntent(ResidencesIntent.SetDeletingResidence(residence)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResidencesMetricsBanner(
    residences: List<ResidenceContext>,
    ownedResidencesCount: Int,
    associatedResidencesCount: Int,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val totalUnitsCount = remember(residences) { residences.sumOf { it.totalUnits } }

    if (isCompact) {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ResidenceMetricCard(
                    label = "Logements",
                    value = "$totalUnitsCount unités",
                    icon = Icons.Default.MeetingRoom,
                    subText = "100% opérationnel",
                    modifier = Modifier.width(160.dp)
                )
            }
            item {
                ResidenceMetricCard(
                    label = "Occupation",
                    value = "90%",
                    icon = Icons.Default.Star,
                    subText = "Taux moyen",
                    modifier = Modifier.width(160.dp)
                )
            }
            item {
                ResidenceMetricCard(
                    label = "Propriétés",
                    value = "${residences.size}",
                    icon = Icons.Default.Add,
                    subText = "Parc géré",
                    modifier = Modifier.width(160.dp)
                )
            }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResidenceMetricCard(
                label = "Total Logements",
                value = "$totalUnitsCount unités",
                icon = Icons.Default.MeetingRoom,
                subText = "100% opérationnel",
                modifier = Modifier.weight(1f)
            )
            ResidenceMetricCard(
                label = "Taux d'occupation",
                value = "92.5%",
                icon = Icons.Default.Star,
                subText = "Performance globale",
                modifier = Modifier.weight(1f)
            )
            ResidenceMetricCard(
                label = "Propriétés gérées",
                value = "$ownedResidencesCount",
                icon = Icons.Default.Check,
                subText = "$associatedResidencesCount associées",
                modifier = Modifier.weight(1f)
            )
        }
    }
}




class ResidencesUiStatePreviewProvider : PreviewParameterProvider<ResidencesUiState> {
    override val values: Sequence<ResidencesUiState> = sequenceOf(
        // View with no residence
        ResidencesUiState(residences = emptyList()),

        // View with one owned residence
        ResidencesUiState(
            residences = listOf(
                ResidenceContext(
                    residenceId = "1",
                    residenceName = "Résidence Les Palmiers",
                    residenceAddress = "Cocody Danga, Abidjan",
                    userRoleInResidence = UserRole.OWNER,
                    totalUnits = 12,
                    currencySymbol = "FCFA",
                    currencyCode = "XOF"
                )
            )
        ),

        // View with one non-owned residences
        ResidencesUiState(
            residences = listOf(
                ResidenceContext(
                    residenceId = "2",
                    residenceName = "Villa Emeraude",
                    residenceAddress = "Riviera Golf, Abidjan",
                    userRoleInResidence = UserRole.TENANT,
                    totalUnits = 4,
                    currencySymbol = "FCFA",
                    currencyCode = "XOF"
                )
            )
        ),

        // View with multiple residence (owned and not owned)
        ResidencesUiState(
            residences = listOf(
                ResidenceContext(
                    residenceId = "1",
                    residenceName = "Résidence Les Palmiers",
                    residenceAddress = "Cocody Danga, Abidjan",
                    userRoleInResidence = UserRole.OWNER,
                    totalUnits = 12,
                    currencySymbol = "FCFA",
                    currencyCode = "XOF"
                ),
                ResidenceContext(
                    residenceId = "2",
                    residenceName = "Villa Emeraude",
                    residenceAddress = "Riviera Golf, Abidjan",
                    userRoleInResidence = UserRole.TENANT,
                    totalUnits = 4,
                    currencySymbol = "FCFA",
                    currencyCode = "XOF"
                )
            )
        ),

        // View with multiple residences (only owned residences)
        ResidencesUiState(
            residences = listOf(
                ResidenceContext(
                    residenceId = "1",
                    residenceName = "Résidence Les Palmiers",
                    residenceAddress = "Cocody Danga, Abidjan",
                    userRoleInResidence = UserRole.OWNER,
                    totalUnits = 12,
                    currencySymbol = "FCFA",
                    currencyCode = "XOF"
                ),
                ResidenceContext(
                    residenceId = "3",
                    residenceName = "Résidence Oasis",
                    residenceAddress = "Marcory Zone 4, Abidjan",
                    userRoleInResidence = UserRole.OWNER,
                    totalUnits = 8,
                    currencySymbol = "FCFA",
                    currencyCode = "XOF"
                )
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
fun ResidencesContentPreview(
    @PreviewParameter(ResidencesUiStatePreviewProvider::class) uiState: ResidencesUiState
) {
    ResidTheme(darkTheme = false) {
        ResidencesContent(
            uiState = uiState,
            onIntent = {},
            onResidenceSelected = {}
        )
    }
}

