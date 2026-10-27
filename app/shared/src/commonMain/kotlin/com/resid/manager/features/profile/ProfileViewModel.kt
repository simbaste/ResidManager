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
    private val updateResidenceCurrencyUseCase: UpdateResidenceCurrencyUseCase,
    private val storageRepository: com.resid.manager.repository.StorageRepository,
    private val sessionStorage: com.resid.manager.SessionStorage? = null
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
                        editAvatarUrl = intent.avatarUrl,
                        editAvatarBytes = null,
                        isAvatarRemoved = false,
                        profileError = null
                    )
                }
            }
            is ProfileIntent.CancelEditingProfile -> updateState {
                it.copy(
                    isEditingProfile = false,
                    editAvatarBytes = null,
                    isAvatarRemoved = false,
                    profileError = null
                )
            }
            is ProfileIntent.SetEditFirstName -> updateState { it.copy(editFirstName = intent.firstName) }
            is ProfileIntent.SetEditLastName -> updateState { it.copy(editLastName = intent.lastName) }
            is ProfileIntent.SetEditPhone -> updateState { it.copy(editPhone = intent.phone) }
            is ProfileIntent.SetEditAvatar -> updateState {
                it.copy(
                    editAvatarBytes = intent.bytes,
                    editAvatarMimeType = intent.mimeType,
                    editAvatarExtension = intent.extension,
                    isAvatarRemoved = false,
                    profileError = null
                )
            }
            is ProfileIntent.RemoveEditAvatar -> updateState {
                it.copy(
                    editAvatarBytes = null,
                    editAvatarUrl = null,
                    isAvatarRemoved = true,
                    profileError = null
                )
            }
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
            is ProfileIntent.InitResidenceCurrency -> updateState {
                it.copy(selectedCurrency = intent.currency.ifBlank { "XOF" }, currencySuccess = false, currencyErrorMsg = null)
            }
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

            // 1. Déterminer l'URL d'avatar final :
            // - Si une nouvelle image a été sélectionnée : upload vers R2
            // - Si l'avatar a été explicitement supprimé : "" (chaîne vide pour effacer en base)
            // - Sinon : conserver editAvatarUrl existant
            var targetAvatarUrl: String? = state.editAvatarUrl
            if (state.editAvatarBytes != null && state.editAvatarBytes.isNotEmpty()) {
                val uploadResult = storageRepository.uploadAvatarToR2(
                    bytes = state.editAvatarBytes,
                    mimeType = state.editAvatarMimeType,
                    extension = state.editAvatarExtension
                )
                if (uploadResult.isFailure) {
                    updateState {
                        it.copy(
                            isSavingProfile = false,
                            profileError = uploadResult.exceptionOrNull()?.message ?: "Échec de l'envoi de la photo de profil"
                        )
                    }
                    return@launch
                }
                targetAvatarUrl = uploadResult.getOrNull()
            } else if (state.isAvatarRemoved) {
                targetAvatarUrl = ""
            }

            updateUserProfileUseCase(
                token = token,
                firstName = state.editFirstName,
                lastName = state.editLastName,
                phone = state.editPhone,
                avatarUrl = targetAvatarUrl
            )
                .onSuccess { updatedUser ->
                    sessionStorage?.updateAvatarUrl(updatedUser.avatarUrl)
                    updateState {
                        it.copy(
                            isSavingProfile = false,
                            isEditingProfile = false,
                            editAvatarBytes = null,
                            editAvatarUrl = updatedUser.avatarUrl,
                            isAvatarRemoved = false
                        )
                    }
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
                    emitEffect(ProfileEffect.ResidenceCurrencyUpdated(residenceId, currency))
                }
                .onFailure { error ->
                    updateState { it.copy(isSubmittingCurrency = false, currencyErrorMsg = error.message) }
                }
        }
    }
}
