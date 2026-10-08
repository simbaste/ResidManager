package com.resid.manager.features.auth.mvi

import com.resid.manager.dto.UserDto

data class AuthUiState(
    val email: String = "",
    val passwordPlain: String = "",
    val passwordVisible: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val phone: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedInUser: UserDto? = null,
    val jwtToken: String? = null,
    val darkMode: Boolean = false,
    val language: String = "fr",
    val rememberMe: Boolean = true
)

sealed interface AuthIntent {
    data class EmailChanged(val email: String) : AuthIntent
    data class PasswordChanged(val password: String) : AuthIntent
    data class FirstNameChanged(val firstName: String) : AuthIntent
    data class LastNameChanged(val lastName: String) : AuthIntent
    data class BirthDateChanged(val birthDate: String) : AuthIntent
    data class PhoneChanged(val phone: String) : AuthIntent
    data class RememberMeChanged(val rememberMe: Boolean) : AuthIntent
    data object TogglePasswordVisibility : AuthIntent
    data object ToggleTheme : AuthIntent
    data class SetLanguage(val language: String) : AuthIntent
    
    data object SubmitLogin : AuthIntent
    data object SubmitRegister : AuthIntent
    data object Logout : AuthIntent
}

sealed interface AuthEffect {
    data class NavigateToMain(val token: String, val user: UserDto) : AuthEffect
    data object NavigateToLogin : AuthEffect
    data object NavigateToRegister : AuthEffect
    data class ShowToast(val message: String) : AuthEffect
}
