package com.resid.manager.features.residences

import androidx.lifecycle.viewModelScope
import com.resid.manager.base.MviViewModel
import com.resid.manager.features.residences.mvi.ResidencesEffect
import com.resid.manager.features.residences.mvi.ResidencesIntent
import com.resid.manager.features.residences.mvi.ResidencesUiState
import com.resid.manager.features.residences.usecase.CreateResidenceUseCase
import com.resid.manager.features.residences.usecase.DeleteResidenceUseCase
import com.resid.manager.features.residences.usecase.FetchResidencesUseCase
import com.resid.manager.features.residences.usecase.JoinResidenceUseCase
import com.resid.manager.features.residences.usecase.UpdateResidenceUseCase
import com.resid.manager.usecase.SearchResidencesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ResidencesViewModel(
    private val fetchResidencesUseCase: FetchResidencesUseCase,
    private val createResidenceUseCase: CreateResidenceUseCase,
    private val updateResidenceUseCase: UpdateResidenceUseCase,
    private val deleteResidenceUseCase: DeleteResidenceUseCase,
    private val searchResidencesUseCase: SearchResidencesUseCase,
    private val joinResidenceUseCase: JoinResidenceUseCase
) : MviViewModel<ResidencesUiState, ResidencesIntent, ResidencesEffect>(ResidencesUiState()) {

    private var searchJob: Job? = null

    override fun onIntent(intent: ResidencesIntent) {
        when (intent) {
            is ResidencesIntent.LoadResidences -> loadResidences(intent.token)
            is ResidencesIntent.SelectResidence -> {
                updateState { it.copy(selectedResidence = intent.residence) }
                emitEffect(ResidencesEffect.NavigateToDashboard(intent.residence))
            }
            is ResidencesIntent.CreateResidence -> createResidence(intent.token, intent.name, intent.address, intent.defaultCurrency, intent.kWhPrice)
            is ResidencesIntent.UpdateResidence -> updateResidence(intent.token, intent.residenceId, intent.name, intent.address, intent.kWhPrice)
            is ResidencesIntent.DeleteResidence -> deleteResidence(intent.token, intent.residenceId)
            is ResidencesIntent.SearchResidences -> searchResidences(intent.token, intent.query)
            is ResidencesIntent.JoinResidence -> joinResidence(intent.token, intent.userId, intent.residenceId)
            is ResidencesIntent.SetShowCreateDialog -> updateState { it.copy(showCreateDialog = intent.show, errorMessage = null) }
            is ResidencesIntent.SetShowJoinDialog -> updateState { it.copy(showJoinDialog = intent.show, searchQuery = "", searchResults = emptyList(), isSearching = false, errorMessage = null) }
            is ResidencesIntent.SetEditingResidence -> updateState { it.copy(editingResidence = intent.residence, errorMessage = null) }
            is ResidencesIntent.SetDeletingResidence -> updateState { it.copy(deletingResidence = intent.residence, errorMessage = null) }
        }
    }

    private fun loadResidences(token: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            fetchResidencesUseCase(token)
                .onSuccess { list ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            residences = list,
                            selectedResidence = it.selectedResidence ?: list.firstOrNull(),
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun createResidence(token: String, name: String, address: String, defaultCurrency: String, kWhPrice: Double) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            createResidenceUseCase(token, name, address, defaultCurrency, kWhPrice)
                .onSuccess { createdDto ->
                    updateState { it.copy(isLoading = false, showCreateDialog = false, errorMessage = null) }
                    emitEffect(ResidencesEffect.ResidenceCreated(createdDto.id))
                    loadResidences(token)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun updateResidence(token: String, residenceId: String, name: String, address: String, kWhPrice: Double) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            updateResidenceUseCase(token, residenceId, name, address, kWhPrice)
                .onSuccess {
                    updateState { it.copy(editingResidence = null) }
                    loadResidences(token)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun deleteResidence(token: String, residenceId: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            deleteResidenceUseCase(token, residenceId)
                .onSuccess {
                    updateState { it.copy(deletingResidence = null) }
                    loadResidences(token)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun searchResidences(token: String, query: String) {
        updateState { it.copy(searchQuery = query, isSearching = true, errorMessage = null) }
        searchJob?.cancel()

        if (query.length < 2) {
            updateState { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(400)
            searchResidencesUseCase(token, query)
                .onSuccess { results ->
                    updateState { it.copy(searchResults = results, isSearching = false) }
                }
                .onFailure { error ->
                    updateState { it.copy(isSearching = false, errorMessage = error.message) }
                }
        }
    }

    private fun joinResidence(token: String, userId: String, residenceId: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            joinResidenceUseCase(token, userId, residenceId)
                .onSuccess {
                    updateState { it.copy(isLoading = false, showJoinDialog = false, errorMessage = null) }
                    loadResidences(token)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}
