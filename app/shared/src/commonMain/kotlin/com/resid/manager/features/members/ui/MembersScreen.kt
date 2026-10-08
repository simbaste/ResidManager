package com.resid.manager.features.members.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.features.members.MembersViewModel
import com.resid.manager.features.members.mvi.MembersIntent
import com.resid.manager.features.members.mvi.MembersUiState
import com.resid.manager.features.members.ui.components.InviteMemberDialog
import com.resid.manager.features.members.ui.components.MembersEmptyState
import com.resid.manager.features.members.ui.components.MembersTable
import com.resid.manager.ui.components.ResidAppBarAction
import com.resid.manager.ui.components.ResidTopAppBar
import org.koin.compose.koinInject

@Composable
fun MembersScreen(
    activeResidence: ResidenceContext?,
    jwtToken: String?,
    viewModel: MembersViewModel = koinInject(),
    onCreateResidenceClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val token = jwtToken ?: ""
    val residenceId = activeResidence?.residenceId ?: ""
    val isAuthorized = activeResidence != null && (
        activeResidence.userRoleInResidence == UserRole.OWNER ||
        activeResidence.userRoleInResidence == UserRole.ADMIN ||
        activeResidence.userRoleInResidence == UserRole.MANAGER
    )

    LaunchedEffect(token, residenceId) {
        if (token.isNotBlank() && residenceId.isNotBlank()) {
            viewModel.onIntent(MembersIntent.LoadMembers(token, residenceId))
        }
    }

    if (activeResidence == null) {
        MembersEmptyState(onCreateResidenceClick = onCreateResidenceClick)
    } else if (uiState.isLoading && uiState.members.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF006948))
        }
    } else {
        MembersContent(
            uiState = uiState,
            isAuthorized = isAuthorized,
            onInviteClick = { viewModel.onIntent(MembersIntent.SetShowInviteDialog(true)) },
            onSortChanged = { column -> viewModel.onIntent(MembersIntent.ChangeSort(column)) }
        )
    }

    if (uiState.showInviteDialog) {
        InviteMemberDialog(
            userSearchQuery = uiState.userSearchQuery,
            userSearchResults = uiState.userSearchResults,
            isSearchingUsers = uiState.isSearchingUsers,
            selectedUserEmail = uiState.selectedUserEmail,
            selectedUserName = uiState.selectedUserName,
            selectedRole = uiState.selectedRole,
            isInviteLoading = uiState.isInviteLoading,
            inviteErrorMessage = uiState.inviteErrorMessage,
            onUserSearchQueryChanged = { q -> viewModel.onIntent(MembersIntent.SearchUsers(token, q)) },
            onUserSelected = { u -> viewModel.onIntent(MembersIntent.SelectUser(u)) },
            onRoleSelected = { r -> viewModel.onIntent(MembersIntent.SelectRole(r)) },
            onDismiss = { viewModel.onIntent(MembersIntent.SetShowInviteDialog(false)) },
            onSubmit = { viewModel.onIntent(MembersIntent.SubmitInvite(token, residenceId)) }
        )
    }
}

@Composable
fun MembersContent(
    uiState: MembersUiState,
    isAuthorized: Boolean,
    onInviteClick: () -> Unit,
    onSortChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedMembers = remember(uiState.members, uiState.sortBy, uiState.sortAscending) {
        val list = uiState.members
        when (uiState.sortBy) {
            "name" -> if (uiState.sortAscending) list.sortedBy { "${it.firstName} ${it.lastName}".lowercase() } else list.sortedByDescending { "${it.firstName} ${it.lastName}".lowercase() }
            "role" -> if (uiState.sortAscending) list.sortedBy { it.roleDto.name.lowercase() } else list.sortedByDescending { it.roleDto.name.lowercase() }
            "status" -> if (uiState.sortAscending) list.sortedBy { it.status.name.lowercase() } else list.sortedByDescending { it.status.name.lowercase() }
            else -> list
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ResidTopAppBar(
            title = "Membres & Habilitations",
            actions = if (isAuthorized) {
                listOf(
                    ResidAppBarAction(
                        title = "Inviter un membre",
                        icon = Icons.Default.PersonAdd,
                        onClick = onInviteClick
                    )
                )
            } else emptyList()
        )

        MembersTable(
            members = sortedMembers,
            sortBy = uiState.sortBy,
            sortAscending = uiState.sortAscending,
            onSortChanged = onSortChanged,
            modifier = Modifier.weight(1f)
        )
    }
}
