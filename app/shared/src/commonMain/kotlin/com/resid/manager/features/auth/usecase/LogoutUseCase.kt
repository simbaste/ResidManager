package com.resid.manager.features.auth.usecase

import com.resid.manager.SessionStorage

class LogoutUseCase(
    private val sessionStorage: SessionStorage? = null
) {
    operator fun invoke() {
        sessionStorage?.clearSession()
    }
}
