package com.resid.manager.session

import com.resid.manager.SessionStorage
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserDto
import com.resid.manager.getCurrentEpochMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserSessionManager(
    private val sessionStorage: SessionStorage? = null
) {
    // 1. Statuts principaux
    private val _sessionStatus = MutableStateFlow<SessionStatus>(SessionStatus.Restoring)
    val sessionStatus: StateFlow<SessionStatus> = _sessionStatus.asStateFlow()

    private val _residenceState = MutableStateFlow<ActiveResidenceState>(ActiveResidenceState.Loading)
    val residenceState: StateFlow<ActiveResidenceState> = _residenceState.asStateFlow()

    // 2. Projections synchrones exposées en StateFlow
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private val _activeResidence = MutableStateFlow<ResidenceContext?>(null)
    val activeResidence: StateFlow<ResidenceContext?> = _activeResidence.asStateFlow()

    private val _allResidence = MutableStateFlow<List<ResidenceContext>>(emptyList())
    val allResidence: StateFlow<List<ResidenceContext>> = _allResidence.asStateFlow()

    init {
        initializeSession()
    }

    private fun applySession(status: SessionStatus) {
        _sessionStatus.value = status
        when (status) {
            is SessionStatus.Authenticated -> {
                _token.value = status.token
                _currentUser.value = status.user
            }
            else -> {
                _token.value = null
                _currentUser.value = null
            }
        }
    }

    private fun applyResidenceState(state: ActiveResidenceState) {
        _residenceState.value = state
        when (state) {
            is ActiveResidenceState.Selected -> {
                _activeResidence.value = state.current
                _allResidence.value = state.all
            }
            else -> {
                _activeResidence.value = null
                _allResidence.value = emptyList()
            }
        }
    }

    fun initializeSession() {
        val session = sessionStorage?.loadSession()
        if (session != null && session.token.isNotBlank()) {
            val user = UserDto(
                id = "",
                email = "",
                firstName = session.firstName,
                lastName = session.lastName,
                phone = null,
                birthDate = null,
                avatarUrl = session.avatarUrl,
                createdAt = "",
                updatedAt = ""
            )
            applySession(SessionStatus.Authenticated(session.token, user))
        } else {
            applySession(SessionStatus.Unauthenticated)
            applyResidenceState(ActiveResidenceState.NoResidence)
        }
    }

    fun setAuthenticated(token: String, user: UserDto) {
        sessionStorage?.saveSession(
            token = token,
            refreshToken = null,
            fName = user.firstName.orEmpty(),
            lName = user.lastName.orEmpty(),
            avatarUrl = user.avatarUrl
        )
        applySession(SessionStatus.Authenticated(token, user))
    }

    fun updateResidences(residences: List<ResidenceContext>, preferredResidenceId: String? = null) {
        if (residences.isEmpty()) {
            applyResidenceState(ActiveResidenceState.NoResidence)
            return
        }

        val savedResidenceId = preferredResidenceId ?: sessionStorage?.loadLastSelectedResidenceId()
        val targetSelection = if (savedResidenceId != null) {
            residences.find { it.residenceId == savedResidenceId } ?: residences.first()
        } else {
            residences.first()
        }

        sessionStorage?.saveLastSelectedResidenceId(targetSelection.residenceId)
        applyResidenceState(
            ActiveResidenceState.Selected(
                current = targetSelection,
                all = residences
            )
        )
    }

    fun selectResidence(residence: ResidenceContext) {
        val allList = when (val currentState = _residenceState.value) {
            is ActiveResidenceState.Selected -> currentState.all
            else -> listOf(residence)
        }
        val now = getCurrentEpochMillis()
        sessionStorage?.saveLastSelectedResidenceId(residence.residenceId)
        sessionStorage?.saveResidenceConsulted(residence.residenceId, now)

        applyResidenceState(
            ActiveResidenceState.Selected(
                current = residence,
                all = allList
            )
        )
    }

    fun clearSession() {
        sessionStorage?.clearSession()
        applySession(SessionStatus.Unauthenticated)
        applyResidenceState(ActiveResidenceState.NoResidence)
    }
}
