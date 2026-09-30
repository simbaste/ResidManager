package com.resid.manager.features.residences.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import org.koin.compose.koinInject

@Composable
fun ResidencesScreen(
    jwtToken: String?,
    userId: String?,
    viewModel: ResidencesViewModel = koinInject(),
    onNavigateToDashboard: (ResidenceContext) -> Unit = {}
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
                else -> {}
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
    val ownedResidences = uiState.residences.filter { it.userRoleInResidence == UserRole.ADMIN }
    val associatedResidences = uiState.residences.filter { it.userRoleInResidence != UserRole.ADMIN }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Top Section with title and action button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Annuaire de mes Résidences",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Button(onClick = { onIntent(ResidencesIntent.SetShowCreateDialog(true)) }) {
                Text("+ Créer une résidence")
            }
        }

        if (uiState.residences.isEmpty()) {
            ResidencesEmptyState(
                onCreateClick = { onIntent(ResidencesIntent.SetShowCreateDialog(true)) },
                onJoinClick = { onIntent(ResidencesIntent.SetShowJoinDialog(true)) }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // "My Owned Properties" Section
                if (ownedResidences.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "My Owned Properties (${ownedResidences.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ownedResidences.forEach { residence ->
                                OwnedPropertyCard(
                                    residence = residence,
                                    onClick = { onResidenceSelected(residence) },
                                    onEditClick = { onIntent(ResidencesIntent.SetEditingResidence(residence)) },
                                    onDeleteClick = { onIntent(ResidencesIntent.SetDeletingResidence(residence)) }
                                )
                            }
                        }
                    }
                }

                // "Associated Properties" Section
                if (associatedResidences.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Associated Properties (${associatedResidences.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            associatedResidences.forEach { residence ->
                                AssociatedPropertyCard(
                                    residence = residence,
                                    onClick = { onResidenceSelected(residence) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
