package com.resid.manager.session

import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserDto

sealed interface SessionStatus {
    data object Restoring : SessionStatus
    data class Authenticated(val token: String, val user: UserDto) : SessionStatus
    data object Unauthenticated : SessionStatus
}

sealed interface ActiveResidenceState {
    data object Loading : ActiveResidenceState
    data class Selected(val current: ResidenceContext, val all: List<ResidenceContext>) : ActiveResidenceState
    data object NoResidence : ActiveResidenceState
}
