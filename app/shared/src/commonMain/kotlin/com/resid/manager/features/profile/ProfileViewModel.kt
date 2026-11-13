package com.resid.manager.features.profile

import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.base.MviViewModel
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.features.profile.mvi.ProfileEffect
import com.resid.manager.features.profile.mvi.ProfileIntent
import com.resid.manager.features.profile.mvi.ProfileUiState
import com.resid.manager.features.profile.usecase.AddTicketCategoryUseCase
import com.resid.manager.features.profile.usecase.FetchResidenceCategoriesUseCase
import com.resid.manager.features.profile.usecase.UpdateResidenceCurrencyUseCase
import com.resid.manager.features.profile.usecase.UpdateTicketCategoryUseCase
import com.resid.manager.features.profile.usecase.UpdateUserProfileUseCase
import com.resid.manager.features.residences.usecase.FetchResidencesUseCase
import com.resid.manager.repository.StorageRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val fetchResidenceCategoriesUseCase: FetchResidenceCategoriesUseCase,
    private val addTicketCategoryUseCase: AddTicketCategoryUseCase,
    private val updateTicketCategoryUseCase: UpdateTicketCategoryUseCase,
    private val updateResidenceCurrencyUseCase: UpdateResidenceCurrencyUseCase,
    private val fetchResidencesUseCase: FetchResidencesUseCase,
    private val storageRepository: StorageRepository,
    private val sessionStorage: SessionStorage? = null
) : MviViewModel<ProfileUiState, ProfileIntent, ProfileEffect>(ProfileUiState()) {

    init {
        sessionStorage?.loadSession()?.let { session ->
            updateState {
                it.copy(
                    firstName = session.firstName,
                    lastName = session.lastName,
                    editFirstName = session.firstName,
                    editLastName = session.lastName,
                    editAvatarUrl = session.avatarUrl
                )
            }
            val lastResId = sessionStorage.loadLastSelectedResidenceId()
            if (!lastResId.isNullOrBlank()) {
                loadResidenceContext(session.token, lastResId)
            }
        }
    }

    override fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.SetTab -> {
                updateState { it.copy(activeTab = intent.tab) }
                if (intent.tab == 1) {
                    loadCategories()
                }
            }
            is ProfileIntent.StartEditingProfile -> {
                val state = uiState.value
                updateState {
                    it.copy(
                        isEditingProfile = true,
                        editFirstName = state.firstName,
                        editLastName = state.lastName,
                        editPhone = state.phone,
                        editAvatarUrl = state.user?.avatarUrl ?: state.editAvatarUrl,
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
            is ProfileIntent.SaveProfile -> saveProfile()
            is ProfileIntent.LoadCategories -> loadCategories()
            is ProfileIntent.SetCategoryKey -> updateState { it.copy(formCategoryKey = intent.key) }
            is ProfileIntent.SetCategoryLabel -> updateState { it.copy(formCategoryLabel = intent.label) }
            is ProfileIntent.SubmitCategory -> submitCategory()
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
            is ProfileIntent.SaveCategoryEdit -> saveCategoryEdit()
            is ProfileIntent.InitResidenceCurrency -> updateState {
                it.copy(selectedCurrency = intent.currency.ifBlank { "XOF" }, currencySuccess = false, currencyErrorMsg = null)
            }
            is ProfileIntent.SelectCurrency -> updateState { it.copy(selectedCurrency = intent.currency, currencySuccess = false, currencyErrorMsg = null) }
            is ProfileIntent.SubmitCurrency -> submitCurrency()
        }
    }

    fun updateActiveResidence(residence: ResidenceContext?) {
        if (residence == null) {
            updateState {
                it.copy(
                    activeResidence = null,
                    isManagement = false,
                    categories = emptyList()
                )
            }
            return
        }
        val isMgmt = residence.userRoleInResidence == UserRole.OWNER ||
            residence.userRoleInResidence == UserRole.ADMIN ||
            residence.userRoleInResidence == UserRole.MANAGER
        updateState {
            it.copy(
                activeResidence = residence,
                isManagement = isMgmt,
                selectedCurrency = residence.currencyCode.ifBlank { "XOF" }
            )
        }
        if (uiState.value.activeTab == 1) {
            loadCategories()
        }
    }

    fun updateUser(user: com.resid.manager.dto.UserDto?) {
        if (user != null) {
            updateState {
                it.copy(
                    user = user,
                    firstName = user.firstName.orEmpty(),
                    lastName = user.lastName.orEmpty(),
                    phone = user.phone.orEmpty(),
                    editFirstName = user.firstName.orEmpty(),
                    editLastName = user.lastName.orEmpty(),
                    editPhone = user.phone.orEmpty(),
                    editAvatarUrl = user.avatarUrl
                )
            }
        }
    }

    private fun loadResidenceContext(token: String, residenceId: String) {
        viewModelScope.launch {
            if (uiState.value.activeResidence == null || uiState.value.activeResidence?.residenceId != residenceId) {
                fetchResidencesUseCase(token).onSuccess { list ->
                    val matched = list.firstOrNull { it.residenceId == residenceId }
                    if (matched != null) {
                        val isMgmt = matched.userRoleInResidence == UserRole.OWNER ||
                            matched.userRoleInResidence == UserRole.ADMIN ||
                            matched.userRoleInResidence == UserRole.MANAGER
                        updateState {
                            it.copy(
                                activeResidence = matched,
                                isManagement = isMgmt,
                                selectedCurrency = matched.currencyCode.ifBlank { "XOF" }
                            )
                        }
                        if (uiState.value.activeTab == 1) {
                            loadCategories()
                        }
                    }
                }
            }
        }
    }

    private fun saveProfile() {
        val session = sessionStorage?.loadSession() ?: return
        val state = uiState.value
        if (state.editFirstName.isBlank() || state.editLastName.isBlank()) {
            updateState { it.copy(profileError = "Le prénom et le nom sont obligatoires.") }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isSavingProfile = true, profileError = null) }

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
                token = session.token,
                firstName = state.editFirstName,
                lastName = state.editLastName,
                phone = state.editPhone,
                avatarUrl = targetAvatarUrl
            )
                .onSuccess { updatedUser ->
                    sessionStorage.updateAvatarUrl(updatedUser.avatarUrl)
                    updateState {
                        it.copy(
                            isSavingProfile = false,
                            isEditingProfile = false,
                            editAvatarBytes = null,
                            editAvatarUrl = updatedUser.avatarUrl,
                            isAvatarRemoved = false,
                            user = updatedUser,
                            firstName = updatedUser.firstName.orEmpty(),
                            lastName = updatedUser.lastName.orEmpty(),
                            phone = updatedUser.phone.orEmpty()
                        )
                    }
                    emitEffect(ProfileEffect.UserProfileUpdated(updatedUser))
                }
                .onFailure { error ->
                    updateState { it.copy(isSavingProfile = false, profileError = error.message) }
                }
        }
    }

    private fun loadCategories() {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoadingCategories = true) }
            fetchResidenceCategoriesUseCase(session.token, residenceId)
                .onSuccess { list ->
                    updateState { it.copy(isLoadingCategories = false, categories = list) }
                }
                .onFailure {
                    updateState { it.copy(isLoadingCategories = false) }
                }
        }
    }

    private fun submitCategory() {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        val state = uiState.value
        if (state.formCategoryKey.isBlank() || state.formCategoryLabel.isBlank()) {
            updateState { it.copy(categoryError = "Veuillez renseigner le code et le libellé.") }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isSubmittingCategory = true, categoryError = null) }
            addTicketCategoryUseCase(session.token, residenceId, state.formCategoryKey, state.formCategoryLabel)
                .onSuccess {
                    updateState { it.copy(isSubmittingCategory = false, formCategoryKey = "", formCategoryLabel = "") }
                    loadCategories()
                }
                .onFailure { error ->
                    updateState { it.copy(isSubmittingCategory = false, categoryError = error.message) }
                }
        }
    }

    private fun saveCategoryEdit() {
        val session = sessionStorage?.loadSession() ?: return
        val state = uiState.value
        val catId = state.editingCategory?.id ?: return
        if (state.editCategoryLabel.isBlank()) {
            updateState { it.copy(editError = "Le libellé ne peut pas être vide.") }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isSubmittingEdit = true, editError = null) }
            updateTicketCategoryUseCase(session.token, catId, state.editCategoryLabel)
                .onSuccess {
                    updateState { it.copy(isSubmittingEdit = false, editingCategory = null) }
                    loadCategories()
                }
                .onFailure { error ->
                    updateState { it.copy(isSubmittingEdit = false, editError = error.message) }
                }
        }
    }

    private fun submitCurrency() {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        val currency = uiState.value.selectedCurrency
        viewModelScope.launch {
            updateState { it.copy(isSubmittingCurrency = true, currencySuccess = false, currencyErrorMsg = null) }
            updateResidenceCurrencyUseCase(session.token, residenceId, currency)
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
