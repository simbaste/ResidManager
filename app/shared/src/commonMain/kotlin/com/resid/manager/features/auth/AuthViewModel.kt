package com.resid.manager.features.auth

import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.base.MviViewModel
import com.resid.manager.features.auth.mvi.AuthEffect
import com.resid.manager.features.auth.mvi.AuthIntent
import com.resid.manager.features.auth.mvi.AuthUiState
import com.resid.manager.features.auth.usecase.LoginUseCase
import com.resid.manager.features.auth.usecase.LogoutUseCase
import com.resid.manager.features.auth.usecase.RegisterUseCase
import kotlinx.coroutines.launch

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val sessionStorage: SessionStorage? = null
) : MviViewModel<AuthUiState, AuthIntent, AuthEffect>(AuthUiState()) {

    override fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.EmailChanged -> updateState { it.copy(email = intent.email, errorMessage = null) }
            is AuthIntent.PasswordChanged -> updateState { it.copy(passwordPlain = intent.password, errorMessage = null) }
            is AuthIntent.FirstNameChanged -> updateState { it.copy(firstName = intent.firstName, errorMessage = null) }
            is AuthIntent.LastNameChanged -> updateState { it.copy(lastName = intent.lastName, errorMessage = null) }
            is AuthIntent.BirthDateChanged -> updateState { it.copy(birthDate = intent.birthDate, errorMessage = null) }
            is AuthIntent.PhoneChanged -> updateState { it.copy(phone = intent.phone, errorMessage = null) }
            is AuthIntent.TogglePasswordVisibility -> updateState { it.copy(passwordVisible = !it.passwordVisible) }
            is AuthIntent.ToggleTheme -> updateState { it.copy(darkMode = !it.darkMode) }
            is AuthIntent.SetLanguage -> updateState { it.copy(language = intent.language) }
            is AuthIntent.SubmitLogin -> login()
            is AuthIntent.SubmitRegister -> register()
            is AuthIntent.Logout -> logout()
        }
    }

    private fun login() {
        val state = uiState.value
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            loginUseCase(state.email, state.passwordPlain)
                .onSuccess { response ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            jwtToken = response.token,
                            loggedInUser = response.user,
                            errorMessage = null
                        )
                    }
                    emitEffect(AuthEffect.NavigateToMain(response.token, response.user))
                }
                .onFailure { exception ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "Identifiants invalides"
                        )
                    }
                }
        }
    }

    private fun register() {
        val state = uiState.value
        updateState { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            registerUseCase(
                firstName = state.firstName,
                lastName = state.lastName,
                birthDate = state.birthDate,
                phone = state.phone,
                email = state.email,
                passwordPlain = state.passwordPlain
            )
                .onSuccess { response ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            jwtToken = response.token,
                            loggedInUser = response.user,
                            errorMessage = null
                        )
                    }
                    emitEffect(AuthEffect.NavigateToMain(response.token, response.user))
                }
                .onFailure { exception ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "Erreur d'inscription"
                        )
                    }
                }
        }
    }

    private fun logout() {
        logoutUseCase()
        updateState {
            it.copy(
                email = "",
                passwordPlain = "",
                firstName = "",
                lastName = "",
                birthDate = "",
                phone = "",
                jwtToken = null,
                loggedInUser = null,
                errorMessage = null
            )
        }
        emitEffect(AuthEffect.NavigateToLogin)
    }
}
