package com.resid.manager.features.profile.mvi

import com.resid.manager.dto.TicketCategoryDto
import com.resid.manager.dto.UserDto

data class ProfileUiState(
    val activeTab: Int = 0, // 0: Mon Compte, 1: Configuration Résidence

    // Profile form
    val isEditingProfile: Boolean = false,
    val editFirstName: String = "",
    val editLastName: String = "",
    val editPhone: String = "",
    val editAvatarUrl: String? = null,
    val editAvatarBytes: ByteArray? = null,
    val editAvatarMimeType: String = "image/jpeg",
    val editAvatarExtension: String = "jpg",
    val isAvatarRemoved: Boolean = false,
    val isSavingProfile: Boolean = false,
    val profileError: String? = null,

    // Categories
    val categories: List<TicketCategoryDto> = emptyList(),
    val isLoadingCategories: Boolean = false,
    val formCategoryKey: String = "",
    val formCategoryLabel: String = "",
    val isSubmittingCategory: Boolean = false,
    val categoryError: String? = null,
    val editingCategory: TicketCategoryDto? = null,
    val editCategoryLabel: String = "",
    val isSubmittingEdit: Boolean = false,
    val editError: String? = null,

    // Currency
    val selectedCurrency: String = "XOF",
    val isSubmittingCurrency: Boolean = false,
    val currencySuccess: Boolean = false,
    val currencyErrorMsg: String? = null,

    val errorMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ProfileUiState
        if (!editAvatarBytes.contentEquals(other.editAvatarBytes)) return false
        return activeTab == other.activeTab &&
                isEditingProfile == other.isEditingProfile &&
                editFirstName == other.editFirstName &&
                editLastName == other.editLastName &&
                editPhone == other.editPhone &&
                editAvatarUrl == other.editAvatarUrl &&
                editAvatarMimeType == other.editAvatarMimeType &&
                editAvatarExtension == other.editAvatarExtension &&
                isAvatarRemoved == other.isAvatarRemoved &&
                isSavingProfile == other.isSavingProfile &&
                profileError == other.profileError &&
                categories == other.categories &&
                isLoadingCategories == other.isLoadingCategories &&
                formCategoryKey == other.formCategoryKey &&
                formCategoryLabel == other.formCategoryLabel &&
                isSubmittingCategory == other.isSubmittingCategory &&
                categoryError == other.categoryError &&
                editingCategory == other.editingCategory &&
                editCategoryLabel == other.editCategoryLabel &&
                isSubmittingEdit == other.isSubmittingEdit &&
                editError == other.editError &&
                selectedCurrency == other.selectedCurrency &&
                isSubmittingCurrency == other.isSubmittingCurrency &&
                currencySuccess == other.currencySuccess &&
                currencyErrorMsg == other.currencyErrorMsg &&
                errorMessage == other.errorMessage
    }

    override fun hashCode(): Int {
        var result = activeTab
        result = 31 * result + isEditingProfile.hashCode()
        result = 31 * result + editFirstName.hashCode()
        result = 31 * result + editLastName.hashCode()
        result = 31 * result + editPhone.hashCode()
        result = 31 * result + (editAvatarUrl?.hashCode() ?: 0)
        result = 31 * result + (editAvatarBytes?.contentHashCode() ?: 0)
        result = 31 * result + isAvatarRemoved.hashCode()
        result = 31 * result + isSavingProfile.hashCode()
        return result
    }
}

sealed interface ProfileIntent {
    data class SetTab(val tab: Int, val token: String, val residenceId: String?) : ProfileIntent

    // User profile actions
    data class StartEditingProfile(val firstName: String, val lastName: String, val phone: String, val avatarUrl: String?) : ProfileIntent
    data object CancelEditingProfile : ProfileIntent
    data class SetEditFirstName(val firstName: String) : ProfileIntent
    data class SetEditLastName(val lastName: String) : ProfileIntent
    data class SetEditPhone(val phone: String) : ProfileIntent
    data class SetEditAvatar(val bytes: ByteArray, val mimeType: String, val extension: String) : ProfileIntent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as SetEditAvatar
            return bytes.contentEquals(other.bytes) && mimeType == other.mimeType && extension == other.extension
        }
        override fun hashCode(): Int = bytes.contentHashCode()
    }
    data object RemoveEditAvatar : ProfileIntent
    data class SaveProfile(val token: String) : ProfileIntent

    // Categories actions
    data class LoadCategories(val token: String, val residenceId: String) : ProfileIntent
    data class SetCategoryKey(val key: String) : ProfileIntent
    data class SetCategoryLabel(val label: String) : ProfileIntent
    data class SubmitCategory(val token: String, val residenceId: String) : ProfileIntent
    data class SetEditingCategory(val category: TicketCategoryDto?) : ProfileIntent
    data class SetEditCategoryLabel(val label: String) : ProfileIntent
    data class SaveCategoryEdit(val token: String) : ProfileIntent

    // Currency actions
    data class SelectCurrency(val currency: String) : ProfileIntent
    data class SubmitCurrency(val token: String, val residenceId: String) : ProfileIntent
}

sealed interface ProfileEffect {
    data class ShowToast(val message: String) : ProfileEffect
    data class UserProfileUpdated(val user: UserDto) : ProfileEffect
}
