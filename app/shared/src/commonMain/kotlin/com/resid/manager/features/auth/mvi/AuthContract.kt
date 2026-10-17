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
    val rememberMe: Boolean = true,
    // --- Nouveaux champs pour l'avatar ---
    val avatarBytes: ByteArray? = null,
    val avatarMimeType: String = "image/jpeg",
    val avatarExtension: String = "jpg"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as AuthUiState
        if (!avatarBytes.contentEquals(other.avatarBytes)) return false
        return email == other.email &&
                passwordPlain == other.passwordPlain &&
                passwordVisible == other.passwordVisible &&
                firstName == other.firstName &&
                lastName == other.lastName &&
                birthDate == other.birthDate &&
                phone == other.phone &&
                isLoading == other.isLoading &&
                errorMessage == other.errorMessage &&
                loggedInUser == other.loggedInUser &&
                jwtToken == other.jwtToken &&
                darkMode == other.darkMode &&
                language == other.language &&
                rememberMe == other.rememberMe &&
                avatarMimeType == other.avatarMimeType &&
                avatarExtension == other.avatarExtension
    }

    override fun hashCode(): Int {
        var result = email.hashCode()
        result = 31 * result + (avatarBytes?.contentHashCode() ?: 0)
        result = 31 * result + avatarMimeType.hashCode()
        result = 31 * result + avatarExtension.hashCode()
        result = 31 * result + isLoading.hashCode()
        return result
    }
}

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
    
    data class AvatarSelected(
        val bytes: ByteArray,
        val mimeType: String = "image/jpeg",
        val extension: String = "jpg"
    ) : AuthIntent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as AvatarSelected
            if (!bytes.contentEquals(other.bytes)) return false
            return mimeType == other.mimeType && extension == other.extension
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            result = 31 * result + extension.hashCode()
            return result
        }
    }
    data object RemoveAvatar : AuthIntent

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
