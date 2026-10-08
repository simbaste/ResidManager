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
)

sealed interface ProfileIntent {
    data class SetTab(val tab: Int, val token: String, val residenceId: String?) : ProfileIntent

    // User profile actions
    data class StartEditingProfile(val firstName: String, val lastName: String, val phone: String) : ProfileIntent
    data object CancelEditingProfile : ProfileIntent
    data class SetEditFirstName(val firstName: String) : ProfileIntent
    data class SetEditLastName(val lastName: String) : ProfileIntent
    data class SetEditPhone(val phone: String) : ProfileIntent
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
