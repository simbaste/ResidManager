package com.resid.manager.features.members

import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.base.MviViewModel
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.dto.UserSearchDto
import com.resid.manager.features.members.mvi.MembersEffect
import com.resid.manager.features.members.mvi.MembersIntent
import com.resid.manager.features.members.mvi.MembersUiState
import com.resid.manager.features.members.usecase.FetchMembersUseCase
import com.resid.manager.features.members.usecase.InviteMemberUseCase
import com.resid.manager.features.residences.usecase.FetchResidencesUseCase
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
import kotlin.time.Duration.Companion.milliseconds

class MembersViewModel(
    private val fetchMembersUseCase: FetchMembersUseCase,
    private val inviteMemberUseCase: InviteMemberUseCase,
    private val fetchResidencesUseCase: FetchResidencesUseCase,
    private val sessionStorage: SessionStorage? = null
) : MviViewModel<MembersUiState, MembersIntent, MembersEffect>(MembersUiState()) {

    private var searchJob: Job? = null

    init {
        val lastSelectedId = sessionStorage?.loadLastSelectedResidenceId()
        if (!lastSelectedId.isNullOrBlank()) {
            loadResidenceAndMembers(lastSelectedId)
        }
    }

    override fun onIntent(intent: MembersIntent) {
        when (intent) {
            is MembersIntent.Refresh -> {
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (!residenceId.isNullOrBlank()) {
                    loadMembers(residenceId)
                }
            }
            is MembersIntent.LoadMembers -> loadResidenceAndMembers(intent.residenceId)
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
            is MembersIntent.SearchUsers -> searchUsers(intent.query)
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
            is MembersIntent.SubmitInvite -> submitInvite()
        }
    }

    fun updateActiveResidence(residence: ResidenceContext?) {
        if (residence == null) {
            updateState {
                it.copy(
                    activeResidence = null,
                    isAuthorized = false,
                    members = emptyList()
                )
            }
            return
        }
        val isAuth = residence.userRoleInResidence == UserRole.OWNER ||
            residence.userRoleInResidence == UserRole.ADMIN ||
            residence.userRoleInResidence == UserRole.MANAGER
        updateState {
            it.copy(
                activeResidence = residence,
                isAuthorized = isAuth
            )
        }
        loadMembers(residence.residenceId)
    }

    private fun loadResidenceAndMembers(residenceId: String) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            if (uiState.value.activeResidence == null || uiState.value.activeResidence?.residenceId != residenceId) {
                fetchResidencesUseCase(session.token).onSuccess { list ->
                    val matched = list.firstOrNull { it.residenceId == residenceId }
                    if (matched != null) {
                        val isAuth = matched.userRoleInResidence == UserRole.OWNER ||
                            matched.userRoleInResidence == UserRole.ADMIN ||
                            matched.userRoleInResidence == UserRole.MANAGER
                        updateState {
                            it.copy(
                                activeResidence = matched,
                                isAuthorized = isAuth
                            )
                        }
                    }
                }
            }
            loadMembers(residenceId)
        }
    }

    private fun loadMembers(residenceId: String) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            fetchMembersUseCase(session.token, residenceId)
                .onSuccess { list ->
                    updateState { it.copy(isLoading = false, members = list, errorMessage = null) }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun searchUsers(query: String) {
        val session = sessionStorage?.loadSession() ?: return
        updateState { it.copy(userSearchQuery = query, isSearchingUsers = true, inviteErrorMessage = null) }
        searchJob?.cancel()

        if (query.length < 2) {
            updateState { it.copy(userSearchResults = emptyList(), isSearchingUsers = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350.milliseconds)
            try {
                val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/users/search") {
                    parameter("q", query)
                    header(HttpHeaders.Authorization, "Bearer ${session.token}")
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

    private fun submitInvite() {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        val state = uiState.value
        if (state.selectedUserEmail.isBlank()) {
            updateState { it.copy(inviteErrorMessage = "Veuillez rechercher et sélectionner un destinataire.") }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isInviteLoading = true, inviteErrorMessage = null) }
            inviteMemberUseCase(session.token, residenceId, state.selectedUserEmail, state.selectedRole)
                .onSuccess {
                    updateState { it.copy(isInviteLoading = false, showInviteDialog = false) }
                    emitEffect(MembersEffect.MemberInvitedSuccess)
                    loadMembers(residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isInviteLoading = false, inviteErrorMessage = error.message) }
                }
        }
    }
}
