package com.resid.manager.features.residences

import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.base.MviViewModel
import com.resid.manager.features.residences.mvi.ResidencesEffect
import com.resid.manager.features.residences.mvi.ResidencesIntent
import com.resid.manager.features.residences.mvi.ResidencesUiState
import com.resid.manager.features.residences.usecase.CreateResidenceUseCase
import com.resid.manager.features.residences.usecase.DeleteResidenceUseCase
import com.resid.manager.features.residences.usecase.FetchResidencePhotosUseCase
import com.resid.manager.features.residences.usecase.FetchResidencesUseCase
import com.resid.manager.features.residences.usecase.JoinResidenceUseCase
import com.resid.manager.features.residences.usecase.PhotoUploadItem
import com.resid.manager.features.residences.usecase.UpdateResidencePhotosUseCase
import com.resid.manager.features.residences.usecase.UpdateResidenceUseCase
import com.resid.manager.usecase.SearchResidencesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ResidencesViewModel(
    private val fetchResidencesUseCase: FetchResidencesUseCase,
    private val fetchResidencePhotosUseCase: FetchResidencePhotosUseCase,
    private val createResidenceUseCase: CreateResidenceUseCase,
    private val updateResidenceUseCase: UpdateResidenceUseCase,
    private val updateResidencePhotosUseCase: UpdateResidencePhotosUseCase,
    private val deleteResidenceUseCase: DeleteResidenceUseCase,
    private val searchResidencesUseCase: SearchResidencesUseCase,
    private val joinResidenceUseCase: JoinResidenceUseCase,
    private val sessionStorage: SessionStorage? = null
) : MviViewModel<ResidencesUiState, ResidencesIntent, ResidencesEffect>(ResidencesUiState()) {

    private var searchJob: Job? = null

    init {
        val lastSelectedId = sessionStorage?.loadLastSelectedResidenceId()
        val timestamps = sessionStorage?.loadResidenceConsultedTimestamps().orEmpty()
        updateState {
            it.copy(
                selectedResidenceId = lastSelectedId,
                consultedTimestamps = timestamps
            )
        }
        loadResidences()
    }

    override fun onIntent(intent: ResidencesIntent) {
        when (intent) {
            is ResidencesIntent.Refresh,
            is ResidencesIntent.LoadResidences -> loadResidences()
            is ResidencesIntent.SelectResidence -> {
                val now = com.resid.manager.getCurrentEpochMillis()
                sessionStorage?.saveLastSelectedResidenceId(intent.residence.residenceId)
                sessionStorage?.saveResidenceConsulted(intent.residence.residenceId, now)
                updateState {
                    it.copy(
                        selectedResidence = intent.residence,
                        selectedResidenceId = intent.residence.residenceId,
                        consultedTimestamps = it.consultedTimestamps + (intent.residence.residenceId to now)
                    )
                }
            }
            is ResidencesIntent.SelectResidenceForDetail -> {
                updateState { it.copy(selectedResidenceForDetail = intent.residence, detailResidencePhotos = emptyList()) }
            }
            is ResidencesIntent.LoadResidencePhotos -> loadResidencePhotos(intent.residenceId)
            is ResidencesIntent.SetViewMode -> updateState { it.copy(viewMode = intent.mode) }
            is ResidencesIntent.SetSortOption -> updateState { it.copy(sortOption = intent.option) }
            is ResidencesIntent.SetSelectedResidenceId -> updateState { it.copy(selectedResidenceId = intent.residenceId) }
            is ResidencesIntent.CreateResidence -> createResidence(intent.name, intent.address, intent.defaultCurrency, intent.kWhPrice, intent.imageBytes)
            is ResidencesIntent.UpdateResidence -> updateResidence(intent.residenceId, intent.name, intent.address, intent.kWhPrice, intent.imageBytes)
            is ResidencesIntent.UpdateResidencePhotos -> updateResidencePhotos(intent.residenceId, intent.photos)
            is ResidencesIntent.DeleteResidence -> deleteResidence(intent.residenceId)
            is ResidencesIntent.SearchResidences -> searchResidences(intent.query)
            is ResidencesIntent.JoinResidence -> joinResidence(intent.residenceId)
            is ResidencesIntent.SetShowCreateDialog -> updateState { it.copy(showCreateDialog = intent.show, errorMessage = null) }
            is ResidencesIntent.SetShowJoinDialog -> updateState { it.copy(showJoinDialog = intent.show, searchQuery = "", searchResults = emptyList(), isSearching = false, errorMessage = null) }
            is ResidencesIntent.SetEditingResidence -> updateState { it.copy(editingResidence = intent.residence, errorMessage = null) }
            is ResidencesIntent.SetDeletingResidence -> updateState { it.copy(deletingResidence = intent.residence, errorMessage = null) }
        }
    }

    private fun loadResidences() {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            val lastSelectedId = sessionStorage.loadLastSelectedResidenceId()
            val timestamps = sessionStorage.loadResidenceConsultedTimestamps().orEmpty()
            fetchResidencesUseCase(session.token)
                .onSuccess { list ->
                    updateState { currentState ->
                        val currentSelectedId = currentState.selectedResidenceId ?: lastSelectedId
                        currentState.copy(
                            isLoading = false,
                            residences = list,
                            selectedResidence = list.find { r -> r.residenceId == currentSelectedId } ?: currentState.selectedResidence ?: list.firstOrNull(),
                            selectedResidenceId = currentSelectedId ?: list.firstOrNull()?.residenceId,
                            consultedTimestamps = timestamps,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun createResidence(name: String, address: String, defaultCurrency: String, kWhPrice: Double, imageBytes: ByteArray? = null) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            createResidenceUseCase(session.token, name, address, defaultCurrency, kWhPrice)
                .onSuccess { createdDto ->
                    if (imageBytes != null && imageBytes.isNotEmpty()) {
                        updateState { it.copy(isUploadingPhoto = true) }
                    }
                    updateState { it.copy(isLoading = false, showCreateDialog = false, isUploadingPhoto = false, errorMessage = null) }
                    emitEffect(ResidencesEffect.ResidenceCreated(createdDto.id))
                    loadResidences()
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun updateResidence(residenceId: String, name: String, address: String, kWhPrice: Double, imageBytes: ByteArray? = null) {
        val session = sessionStorage?.loadSession() ?: return
        val existingCurrency = uiState.value.editingResidence?.currencyCode
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            updateResidenceUseCase(session.token, residenceId, name, address, kWhPrice, existingCurrency)
                .onSuccess {
                    updateState { it.copy(editingResidence = null) }
                    loadResidences()
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun loadResidencePhotos(residenceId: String) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            fetchResidencePhotosUseCase(session.token, residenceId)
                .onSuccess { list ->
                    updateState { it.copy(detailResidencePhotos = list) }
                }
                .onFailure { error ->
                    updateState { it.copy(errorMessage = error.message) }
                }
        }
    }

    private fun updateResidencePhotos(residenceId: String, photos: List<PhotoUploadItem>) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isSavingPhotos = true, errorMessage = null) }
            updateResidencePhotosUseCase(session.token, residenceId, photos)
                .onSuccess { updatedList ->
                    updateState { it.copy(isSavingPhotos = false, detailResidencePhotos = updatedList) }
                    emitEffect(ResidencesEffect.ShowToast("Photos de la résidence enregistrées avec succès !"))
                    loadResidences()
                }
                .onFailure { error ->
                    updateState { it.copy(isSavingPhotos = false, errorMessage = error.message) }
                }
        }
    }

    private fun deleteResidence(residenceId: String) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            deleteResidenceUseCase(session.token, residenceId)
                .onSuccess {
                    updateState { it.copy(deletingResidence = null) }
                    loadResidences()
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun searchResidences(query: String) {
        val session = sessionStorage?.loadSession() ?: return
        updateState { it.copy(searchQuery = query, isSearching = true, errorMessage = null) }
        searchJob?.cancel()

        if (query.length < 2) {
            updateState { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(400.milliseconds)
            searchResidencesUseCase(session.token, query)
                .onSuccess { results ->
                    updateState { it.copy(searchResults = results, isSearching = false) }
                }
                .onFailure { error ->
                    updateState { it.copy(isSearching = false, errorMessage = error.message) }
                }
        }
    }

    private fun joinResidence(residenceId: String) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            joinResidenceUseCase(session.token, residenceId)
                .onSuccess {
                    updateState { it.copy(isLoading = false, showJoinDialog = false, errorMessage = null) }
                    loadResidences()
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}
