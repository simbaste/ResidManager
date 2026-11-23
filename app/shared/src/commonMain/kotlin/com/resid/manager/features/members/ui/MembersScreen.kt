package com.resid.manager.features.members.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.RoleDto
import com.resid.manager.features.members.MembersViewModel
import com.resid.manager.features.members.mvi.MembersIntent
import com.resid.manager.features.members.mvi.MembersUiState
import com.resid.manager.features.members.ui.components.InviteMemberDialog
import com.resid.manager.features.members.ui.components.MembersCardsList
import com.resid.manager.features.members.ui.components.MembersEmptyFilterState
import com.resid.manager.features.members.ui.components.MembersEmptyState
import com.resid.manager.features.members.ui.components.MembersFilterBar
import com.resid.manager.features.members.ui.components.MembersHeader
import com.resid.manager.features.members.ui.components.MembersSummaryBar
import com.resid.manager.features.members.ui.components.MembersTable
import com.resid.manager.helpers.PreviewResidenceMembersData
import com.resid.manager.ui.theme.ResidTheme
import org.koin.compose.koinInject

@Composable
fun MembersScreen(
    activeResidence: ResidenceContext? = null,
    viewModel: MembersViewModel = koinInject(),
    onCreateResidenceClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(activeResidence?.residenceId) {
        if (activeResidence != null) {
            viewModel.updateActiveResidence(activeResidence)
        }
    }

    val currentResidence = uiState.activeResidence ?: activeResidence

    if (currentResidence == null) {
        MembersEmptyState(onCreateResidenceClick = onCreateResidenceClick)
    } else if (uiState.isLoading && uiState.members.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        MembersContent(
            uiState = uiState,
            isAuthorized = uiState.isAuthorized,
            onInviteClick = { viewModel.onIntent(MembersIntent.SetShowInviteDialog(true)) },
            onSortChanged = { column -> viewModel.onIntent(MembersIntent.ChangeSort(column)) },
            onSearchQueryChange = { query -> viewModel.onIntent(MembersIntent.UpdateSearchQuery(query)) },
            onRoleSelected = { role -> viewModel.onIntent(MembersIntent.SelectRoleFilter(role)) },
            onResetFilters = { viewModel.onIntent(MembersIntent.ResetFilters) },
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
            onUserSearchQueryChanged = { q -> viewModel.onIntent(MembersIntent.SearchUsers(q)) },
            onUserSelected = { u -> viewModel.onIntent(MembersIntent.SelectUser(u)) },
            onRoleSelected = { r -> viewModel.onIntent(MembersIntent.SelectRole(r)) },
            onDismiss = { viewModel.onIntent(MembersIntent.SetShowInviteDialog(false)) },
            onSubmit = { viewModel.onIntent(MembersIntent.SubmitInvite) },
        )
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MembersContent(
    uiState: MembersUiState,
    isAuthorized: Boolean,
    onInviteClick: () -> Unit,
    onSortChanged: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRoleSelected: (RoleDto?) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
    forcedWindowWidthSizeClass: WindowWidthSizeClass? = null,
) {
    // 1. Dynamic role counts calculated from all loaded members
    val roleCounts = remember(uiState.members) {
        val map = mutableMapOf<RoleDto, Int>()
        uiState.members.forEach { m ->
            map[m.roleDto] = (map[m.roleDto] ?: 0) + 1
        }
        map
    }

    // 2. Filter & Sort pipeline
    val filteredSortedMembers = remember(
        uiState.members,
        uiState.searchQuery,
        uiState.selectedRoleFilter,
        uiState.sortBy,
        uiState.sortAscending,
    ) {
        val query = uiState.searchQuery.trim().lowercase()
        val roleFilter = uiState.selectedRoleFilter

        val filtered = uiState.members.filter { member ->
            val matchesRole = (roleFilter == null) || (member.roleDto == roleFilter)
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                val fullName = "${member.firstName} ${member.lastName}".lowercase()
                val reverseFullName = "${member.lastName} ${member.firstName}".lowercase()
                val email = member.email.lowercase()
                val phone = member.phone?.lowercase().orEmpty()

                member.firstName.lowercase().contains(query) ||
                    member.lastName.lowercase().contains(query) ||
                    fullName.contains(query) ||
                    reverseFullName.contains(query) ||
                    email.contains(query) ||
                    phone.contains(query)
            }
            matchesRole && matchesQuery
        }

        when (uiState.sortBy) {
            "name" -> if (uiState.sortAscending) {
                filtered.sortedBy { "${it.firstName} ${it.lastName}".lowercase() }
            } else {
                filtered.sortedByDescending { "${it.firstName} ${it.lastName}".lowercase() }
            }
            "role" -> if (uiState.sortAscending) {
                filtered.sortedBy { it.roleDto.name.lowercase() }
            } else {
                filtered.sortedByDescending { it.roleDto.name.lowercase() }
            }
            "status" -> if (uiState.sortAscending) {
                filtered.sortedBy { it.status.name.lowercase() }
            } else {
                filtered.sortedByDescending { it.status.name.lowercase() }
            }
            else -> filtered
        }
    }

    val isFilterActive = uiState.searchQuery.isNotBlank() || (uiState.selectedRoleFilter != null)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowWidthClass = forcedWindowWidthSizeClass ?: run {
            val sizeClass = WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
            sizeClass.widthSizeClass
        }
        val isExpanded = windowWidthClass == WindowWidthSizeClass.Expanded
        val isCompact = windowWidthClass == WindowWidthSizeClass.Compact || maxWidth < 600.dp
        val screenPadding = if (isCompact) 16.dp else 24.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            MembersHeader(
                isAuthorized = isAuthorized,
                onInviteClick = onInviteClick,
                isCompact = !isExpanded,
            )

            // Search & Filter controls
            MembersFilterBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                selectedRole = uiState.selectedRoleFilter,
                onRoleSelected = onRoleSelected,
                totalCount = uiState.members.size,
                roleCounts = roleCounts,
            )

            // Summary Bar
            MembersSummaryBar(
                displayedCount = filteredSortedMembers.size,
                totalCount = uiState.members.size,
                isFiltered = isFilterActive,
            )

            // Main Content: Table or Cards, or Empty Filter State
            if (uiState.members.isNotEmpty() && filteredSortedMembers.isEmpty()) {
                MembersEmptyFilterState(
                    onResetFilters = onResetFilters,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else if (isExpanded) {
                MembersTable(
                    members = filteredSortedMembers,
                    sortBy = uiState.sortBy,
                    sortAscending = uiState.sortAscending,
                    onSortChanged = onSortChanged,
                    modifier = Modifier.weight(1f),
                )
            } else {
                MembersCardsList(
                    members = filteredSortedMembers,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// =========================================================================
// PREVIEWS
// =========================================================================

private val previewMemberList: List<ResidenceMemberSummaryDto> = listOf(
    PreviewResidenceMembersData.ownerAdmin,
    PreviewResidenceMembersData.coAdmin,
    PreviewResidenceMembersData.tenantHerve,
    PreviewResidenceMembersData.propertyManager,
    PreviewResidenceMembersData.maintenanceStaffAccepted,
)

@PreviewLightDark
@Composable
private fun MembersContentExpandedPreview() {
    ResidTheme {
        MembersContent(
            uiState = MembersUiState(
                members = previewMemberList,
                isAuthorized = true,
            ),
            isAuthorized = true,
            onInviteClick = {},
            onSortChanged = {},
            onSearchQueryChange = {},
            onRoleSelected = {},
            onResetFilters = {},
            forcedWindowWidthSizeClass = WindowWidthSizeClass.Expanded,
        )
    }
}

@PreviewLightDark
@Composable
private fun MembersContentCompactPreview() {
    ResidTheme {
        MembersContent(
            uiState = MembersUiState(
                members = previewMemberList,
                isAuthorized = true,
            ),
            isAuthorized = true,
            onInviteClick = {},
            onSortChanged = {},
            onSearchQueryChange = {},
            onRoleSelected = {},
            onResetFilters = {},
            forcedWindowWidthSizeClass = WindowWidthSizeClass.Compact,
        )
    }
}

@PreviewLightDark
@Composable
private fun MembersContentEmptyFilterPreview() {
    ResidTheme {
        MembersContent(
            uiState = MembersUiState(
                members = previewMemberList,
                searchQuery = "introuvable",
                isAuthorized = true,
            ),
            isAuthorized = true,
            onInviteClick = {},
            onSortChanged = {},
            onSearchQueryChange = {},
            onRoleSelected = {},
            onResetFilters = {},
            forcedWindowWidthSizeClass = WindowWidthSizeClass.Compact,
        )
    }
}
