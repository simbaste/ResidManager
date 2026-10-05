package com.resid.manager.features.residences.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.features.residences.ResidencesViewModel
import com.resid.manager.features.residences.mvi.ResidencesEffect
import com.resid.manager.features.residences.mvi.ResidencesIntent
import com.resid.manager.features.residences.mvi.ResidencesUiState
import com.resid.manager.features.residences.ui.components.AssociatedPropertyCard
import com.resid.manager.features.residences.ui.components.CreateResidenceDialog
import com.resid.manager.features.residences.ui.components.DeleteResidenceDialog
import com.resid.manager.features.residences.ui.components.EditResidenceDialog
import com.resid.manager.features.residences.ui.components.JoinResidenceDialog
import com.resid.manager.features.residences.ui.components.OwnedPropertyCard
import com.resid.manager.features.residences.ui.components.ResidencesEmptyState
import com.resid.manager.ui.components.ResidAppBarAction
import com.resid.manager.ui.components.ResidTopAppBar
import com.resid.manager.ui.theme.ResidTheme
import org.koin.compose.koinInject

@Composable
fun ResidencesScreen(
    jwtToken: String?,
    userId: String?,
    viewModel: ResidencesViewModel = koinInject(),
    onNavigateToDashboard: (ResidenceContext) -> Unit = {},
    onResidenceCreated: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(jwtToken) {
        val token = jwtToken ?: return@LaunchedEffect
        viewModel.onIntent(ResidencesIntent.LoadResidences(token))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ResidencesEffect.NavigateToDashboard -> onNavigateToDashboard(effect.residence)
                is ResidencesEffect.ResidenceCreated -> onResidenceCreated(effect.newResidenceId)
                is ResidencesEffect.ShowToast -> {}
            }
        }
    }

    ResidencesContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onResidenceSelected = { residence ->
            viewModel.onIntent(ResidencesIntent.SelectResidence(residence))
        }
    )

    // Modales
    val token = jwtToken ?: ""

    if (uiState.showCreateDialog) {
        CreateResidenceDialog(
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.onIntent(ResidencesIntent.SetShowCreateDialog(false)) },
            onSubmit = { name, address, currency, price ->
                viewModel.onIntent(ResidencesIntent.CreateResidence(token, name, address, currency, price))
            }
        )
    }

    if (uiState.editingResidence != null) {
        val target = uiState.editingResidence!!
        EditResidenceDialog(
            residence = target,
            onDismiss = { viewModel.onIntent(ResidencesIntent.SetEditingResidence(null)) },
            onSubmit = { name, address, price ->
                viewModel.onIntent(ResidencesIntent.UpdateResidence(token, target.residenceId, name, address, price))
            }
        )
    }

    if (uiState.deletingResidence != null) {
        val target = uiState.deletingResidence!!
        DeleteResidenceDialog(
            residenceName = target.residenceName,
            onDismiss = { viewModel.onIntent(ResidencesIntent.SetDeletingResidence(null)) },
            onConfirm = {
                viewModel.onIntent(ResidencesIntent.DeleteResidence(token, target.residenceId))
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
                viewModel.onIntent(ResidencesIntent.SearchResidences(token, query))
            },
            onDismiss = { viewModel.onIntent(ResidencesIntent.SetShowJoinDialog(false)) },
            onSubmit = { residenceId ->
                userId?.let { uid ->
                    viewModel.onIntent(ResidencesIntent.JoinResidence(token, uid, residenceId))
                }
            }
        )
    }
}

@Composable
fun ResidencesContent(
    uiState: ResidencesUiState,
    onIntent: (ResidencesIntent) -> Unit,
    onResidenceSelected: (ResidenceContext) -> Unit,
    modifier: Modifier = Modifier
) {
    val ownedResidences = uiState.residences.filter { it.userRoleInResidence == UserRole.OWNER }
    val associatedResidences = uiState.residences.filter { it.userRoleInResidence != UserRole.OWNER }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val currentResidences = if (selectedTabIndex == 0) ownedResidences else associatedResidences

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isSmallScreen = maxWidth < 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isSmallScreen) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResidTopAppBar(
                title = "Annuaire de mes Résidences",
                actions = listOf(
                    ResidAppBarAction(
                        title = "Créer une résidence",
                        icon = Icons.Default.Add,
                        onClick = { onIntent(ResidencesIntent.SetShowCreateDialog(true)) },
                    ),
                ),
            )

            if (uiState.residences.isEmpty()) {
                ResidencesEmptyState(
                    onCreateClick = { onIntent(ResidencesIntent.SetShowCreateDialog(true)) },
                    onJoinClick = { onIntent(ResidencesIntent.SetShowJoinDialog(true)) }
                )
            } else {
                // Segmented button to switch between Owned and Associated residences
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("Propriétés gérées (${ownedResidences.size})") }
                    )
                    SegmentedButton(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("Propriétés associées (${associatedResidences.size})") }
                    )
                }

                if (currentResidences.isEmpty()) {
                    Text(
                        text = if (selectedTabIndex == 0) {
                            "Aucune propriété gérée pour le moment."
                        } else {
                            "Aucune propriété associée pour le moment."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else if (isSmallScreen) {
                    // Small screens: Single vertical scrolling list
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(currentResidences, key = { it.residenceId }) { residence ->
                            if (selectedTabIndex == 0) {
                                OwnedPropertyCard(
                                    residence = residence,
                                    onClick = { onResidenceSelected(residence) },
                                    onEditClick = { onIntent(ResidencesIntent.SetEditingResidence(residence)) },
                                    onDeleteClick = { onIntent(ResidencesIntent.SetDeletingResidence(residence)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                AssociatedPropertyCard(
                                    residence = residence,
                                    onClick = { onResidenceSelected(residence) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    // Big screens: Responsive grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 280.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(currentResidences, key = { it.residenceId }) { residence ->
                            if (selectedTabIndex == 0) {
                                OwnedPropertyCard(
                                    residence = residence,
                                    onClick = { onResidenceSelected(residence) },
                                    onEditClick = { onIntent(ResidencesIntent.SetEditingResidence(residence)) },
                                    onDeleteClick = { onIntent(ResidencesIntent.SetDeletingResidence(residence)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                AssociatedPropertyCard(
                                    residence = residence,
                                    onClick = { onResidenceSelected(residence) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
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

