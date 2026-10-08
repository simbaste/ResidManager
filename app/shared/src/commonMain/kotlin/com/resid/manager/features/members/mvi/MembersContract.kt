package com.resid.manager.features.members.mvi

import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.UserSearchDto

data class MembersUiState(
    val isLoading: Boolean = false,
    val members: List<ResidenceMemberSummaryDto> = emptyList(),
    val sortBy: String = "name", // "name", "role", "status"
    val sortAscending: Boolean = true,
    val showInviteDialog: Boolean = false,
    val isInviteLoading: Boolean = false,
    val inviteErrorMessage: String? = null,
    val userSearchQuery: String = "",
    val userSearchResults: List<UserSearchDto> = emptyList(),
    val isSearchingUsers: Boolean = false,
    val selectedUserEmail: String = "",
    val selectedUserName: String = "",
    val selectedRole: String = "MANAGER",
    val errorMessage: String? = null
)

sealed interface MembersIntent {
    data class LoadMembers(val token: String, val residenceId: String) : MembersIntent
    data class ChangeSort(val column: String) : MembersIntent
    data class SetShowInviteDialog(val show: Boolean) : MembersIntent
    data class SearchUsers(val token: String, val query: String) : MembersIntent
    data class SelectUser(val user: UserSearchDto) : MembersIntent
    data class SelectRole(val role: String) : MembersIntent
    data class SubmitInvite(val token: String, val residenceId: String) : MembersIntent
}

sealed interface MembersEffect {
    data class ShowToast(val message: String) : MembersEffect
    data object MemberInvitedSuccess : MembersEffect
}
