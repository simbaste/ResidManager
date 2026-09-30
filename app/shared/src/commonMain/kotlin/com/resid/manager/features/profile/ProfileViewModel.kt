package com.resid.manager.features.profile

import androidx.lifecycle.viewModelScope
import com.resid.manager.base.MviViewModel
import com.resid.manager.features.profile.mvi.ProfileEffect
import com.resid.manager.features.profile.mvi.ProfileIntent
import com.resid.manager.features.profile.mvi.ProfileUiState
import com.resid.manager.features.profile.usecase.AddTicketCategoryUseCase
import com.resid.manager.features.profile.usecase.FetchResidenceCategoriesUseCase
import com.resid.manager.features.profile.usecase.UpdateResidenceCurrencyUseCase
import com.resid.manager.features.profile.usecase.UpdateTicketCategoryUseCase
import com.resid.manager.features.profile.usecase.UpdateUserProfileUseCase
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val fetchResidenceCategoriesUseCase: FetchResidenceCategoriesUseCase,
    private val addTicketCategoryUseCase: AddTicketCategoryUseCase,
    private val updateTicketCategoryUseCase: UpdateTicketCategoryUseCase,
    private val updateResidenceCurrencyUseCase: UpdateResidenceCurrencyUseCase
) : MviViewModel<ProfileUiState, ProfileIntent, ProfileEffect>(ProfileUiState()) {

    override fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.SetTab -> {
                updateState { it.copy(activeTab = intent.tab) }
                if (intent.tab == 1 && intent.residenceId != null) {
                    loadCategories(intent.token, intent.residenceId)
                }
            }
            is ProfileIntent.StartEditingProfile -> {
                updateState {
                    it.copy(
                        isEditingProfile = true,
                        editFirstName = intent.firstName,
                        editLastName = intent.lastName,
                        editPhone = intent.phone,
                        profileError = null
                    )
                }
            }
            is ProfileIntent.CancelEditingProfile -> updateState { it.copy(isEditingProfile = false, profileError = null) }
            is ProfileIntent.SetEditFirstName -> updateState { it.copy(editFirstName = intent.firstName) }
            is ProfileIntent.SetEditLastName -> updateState { it.copy(editLastName = intent.lastName) }
            is ProfileIntent.SetEditPhone -> updateState { it.copy(editPhone = intent.phone) }
            is ProfileIntent.SaveProfile -> saveProfile(intent.token)
            is ProfileIntent.LoadCategories -> loadCategories(intent.token, intent.residenceId)
            is ProfileIntent.SetCategoryKey -> updateState { it.copy(formCategoryKey = intent.key) }
            is ProfileIntent.SetCategoryLabel -> updateState { it.copy(formCategoryLabel = intent.label) }
            is ProfileIntent.SubmitCategory -> submitCategory(intent.token, intent.residenceId)
            is ProfileIntent.SetEditingCategory -> {
                updateState {
                    it.copy(
                        editingCategory = intent.category,
                        editCategoryLabel = intent.category?.label.orEmpty(),
                        editError = null
                    )
                }
            }
            is ProfileIntent.SetEditCategoryLabel -> updateState { it.copy(editCategoryLabel = intent.label) }
            is ProfileIntent.SaveCategoryEdit -> saveCategoryEdit(intent.token)
            is ProfileIntent.SelectCurrency -> updateState { it.copy(selectedCurrency = intent.currency, currencySuccess = false, currencyErrorMsg = null) }
            is ProfileIntent.SubmitCurrency -> submitCurrency(intent.token, intent.residenceId)
        }
    }

    private fun saveProfile(token: String) {
        val state = uiState.value
        if (state.editFirstName.isBlank() || state.editLastName.isBlank()) {
            updateState { it.copy(profileError = "Le prénom et le nom sont obligatoires.") }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isSavingProfile = true, profileError = null) }
            updateUserProfileUseCase(token, state.editFirstName, state.editLastName, state.editPhone)
                .onSuccess { updatedUser ->
                    updateState { it.copy(isSavingProfile = false, isEditingProfile = false) }
                    emitEffect(ProfileEffect.UserProfileUpdated(updatedUser))
                }
                .onFailure { error ->
                    updateState { it.copy(isSavingProfile = false, profileError = error.message) }
                }
        }
    }

    private fun loadCategories(token: String, residenceId: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoadingCategories = true) }
            fetchResidenceCategoriesUseCase(token, residenceId)
                .onSuccess { list ->
                    updateState { it.copy(isLoadingCategories = false, categories = list) }
                }
                .onFailure {
                    updateState { it.copy(isLoadingCategories = false) }
                }
        }
    }

    private fun submitCategory(token: String, residenceId: String) {
        val state = uiState.value
        if (state.formCategoryKey.isBlank() || state.formCategoryLabel.isBlank()) {
            updateState { it.copy(categoryError = "Veuillez renseigner le code et le libellé.") }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isSubmittingCategory = true, categoryError = null) }
            addTicketCategoryUseCase(token, residenceId, state.formCategoryKey, state.formCategoryLabel)
                .onSuccess {
                    updateState { it.copy(isSubmittingCategory = false, formCategoryKey = "", formCategoryLabel = "") }
                    loadCategories(token, residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isSubmittingCategory = false, categoryError = error.message) }
                }
        }
    }

    private fun saveCategoryEdit(token: String) {
        val state = uiState.value
        val catId = state.editingCategory?.id ?: return
        if (state.editCategoryLabel.isBlank()) {
            updateState { it.copy(editError = "Le libellé ne peut pas être vide.") }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isSubmittingEdit = true, editError = null) }
            updateTicketCategoryUseCase(token, catId, state.editCategoryLabel)
                .onSuccess {
                    val residenceId = state.editingCategory?.residenceId
                    updateState { it.copy(isSubmittingEdit = false, editingCategory = null) }
                    if (residenceId != null) {
                        loadCategories(token, residenceId)
                    }
                }
                .onFailure { error ->
                    updateState { it.copy(isSubmittingEdit = false, editError = error.message) }
                }
        }
    }

    private fun submitCurrency(token: String, residenceId: String) {
        val currency = uiState.value.selectedCurrency
        viewModelScope.launch {
            updateState { it.copy(isSubmittingCurrency = true, currencySuccess = false, currencyErrorMsg = null) }
            updateResidenceCurrencyUseCase(token, residenceId, currency)
                .onSuccess {
                    updateState { it.copy(isSubmittingCurrency = false, currencySuccess = true) }
                }
                .onFailure { error ->
                    updateState { it.copy(isSubmittingCurrency = false, currencyErrorMsg = error.message) }
                }
        }
    }
}
