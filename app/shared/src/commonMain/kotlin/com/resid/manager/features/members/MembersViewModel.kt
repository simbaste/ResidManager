package com.resid.manager.features.members

import androidx.lifecycle.viewModelScope
import com.resid.manager.base.MviViewModel
import com.resid.manager.dto.UserSearchDto
import com.resid.manager.features.members.mvi.MembersEffect
import com.resid.manager.features.members.mvi.MembersIntent
import com.resid.manager.features.members.mvi.MembersUiState
import com.resid.manager.features.members.usecase.FetchMembersUseCase
import com.resid.manager.features.members.usecase.InviteMemberUseCase
import com.resid.manager.network.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MembersViewModel(
    private val fetchMembersUseCase: FetchMembersUseCase,
    private val inviteMemberUseCase: InviteMemberUseCase
) : MviViewModel<MembersUiState, MembersIntent, MembersEffect>(MembersUiState()) {

    private var searchJob: Job? = null

    override fun onIntent(intent: MembersIntent) {
        when (intent) {
            is MembersIntent.LoadMembers -> loadMembers(intent.token, intent.residenceId)
            is MembersIntent.ChangeSort -> {
                val currentSort = uiState.value.sortBy
                val currentAsc = uiState.value.sortAscending
                if (currentSort == intent.column) {
                    updateState { it.copy(sortAscending = !currentAsc) }
                } else {
                    updateState { it.copy(sortBy = intent.column, sortAscending = true) }
                }
            }
            is MembersIntent.SetShowInviteDialog -> {
                updateState {
                    it.copy(
                        showInviteDialog = intent.show,
                        userSearchQuery = "",
                        userSearchResults = emptyList(),
                        selectedUserEmail = "",
                        selectedUserName = "",
                        selectedRole = "MANAGER",
                        inviteErrorMessage = null
                    )
                }
            }
            is MembersIntent.SearchUsers -> searchUsers(intent.token, intent.query)
            is MembersIntent.SelectUser -> {
                updateState {
                    it.copy(
                        selectedUserEmail = intent.user.email,
                        selectedUserName = intent.user.name,
                        userSearchQuery = intent.user.name,
                        userSearchResults = emptyList()
                    )
                }
            }
            is MembersIntent.SelectRole -> updateState { it.copy(selectedRole = intent.role) }
            is MembersIntent.SubmitInvite -> submitInvite(intent.token, intent.residenceId)
        }
    }

    private fun loadMembers(token: String, residenceId: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            fetchMembersUseCase(token, residenceId)
                .onSuccess { list ->
                    updateState { it.copy(isLoading = false, members = list, errorMessage = null) }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun searchUsers(token: String, query: String) {
        updateState { it.copy(userSearchQuery = query, isSearchingUsers = true, inviteErrorMessage = null) }
        searchJob?.cancel()

        if (query.length < 2) {
            updateState { it.copy(userSearchResults = emptyList(), isSearchingUsers = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350)
            try {
                val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/users/search") {
                    parameter("q", query)
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                if (response.status == HttpStatusCode.OK) {
                    val list = response.body<List<UserSearchDto>>()
                    updateState { it.copy(userSearchResults = list, isSearchingUsers = false) }
                } else {
                    updateState { it.copy(isSearchingUsers = false) }
                }
            } catch (_: Exception) {
                updateState { it.copy(isSearchingUsers = false) }
            }
        }
    }

    private fun submitInvite(token: String, residenceId: String) {
        val state = uiState.value
        if (state.selectedUserEmail.isBlank()) {
            updateState { it.copy(inviteErrorMessage = "Veuillez rechercher et sélectionner un destinataire.") }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isInviteLoading = true, inviteErrorMessage = null) }
            inviteMemberUseCase(token, residenceId, state.selectedUserEmail, state.selectedRole)
                .onSuccess {
                    updateState { it.copy(isInviteLoading = false, showInviteDialog = false) }
                    emitEffect(MembersEffect.MemberInvitedSuccess)
                    loadMembers(token, residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isInviteLoading = false, inviteErrorMessage = error.message) }
                }
        }
    }
}
