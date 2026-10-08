package com.resid.manager.features.residences.mvi

import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceSummaryItemDto

data class ResidencesUiState(
    val isLoading: Boolean = false,
    val residences: List<ResidenceContext> = emptyList(),
    val selectedResidence: ResidenceContext? = null,
    val errorMessage: String? = null,

    // Dialog states
    val showCreateDialog: Boolean = false,
    val showJoinDialog: Boolean = false,
    val editingResidence: ResidenceContext? = null,
    val deletingResidence: ResidenceContext? = null,

    // Real-time search states for joining
    val searchQuery: String = "",
    val searchResults: List<ResidenceSummaryItemDto> = emptyList(),
    val isSearching: Boolean = false
)

sealed interface ResidencesIntent {
    data class LoadResidences(val token: String) : ResidencesIntent
    data class SelectResidence(val residence: ResidenceContext) : ResidencesIntent
    data class CreateResidence(val token: String, val name: String, val address: String, val defaultCurrency: String, val kWhPrice: Double) : ResidencesIntent
    data class UpdateResidence(val token: String, val residenceId: String, val name: String, val address: String, val kWhPrice: Double) : ResidencesIntent
    data class DeleteResidence(val token: String, val residenceId: String) : ResidencesIntent
    data class SearchResidences(val token: String, val query: String) : ResidencesIntent
    data class JoinResidence(val token: String, val userId: String, val residenceId: String) : ResidencesIntent

    // Dialog toggles
    data class SetShowCreateDialog(val show: Boolean) : ResidencesIntent
    data class SetShowJoinDialog(val show: Boolean) : ResidencesIntent
    data class SetEditingResidence(val residence: ResidenceContext?) : ResidencesIntent
    data class SetDeletingResidence(val residence: ResidenceContext?) : ResidencesIntent
}

sealed interface ResidencesEffect {
    data class NavigateToDashboard(val residence: ResidenceContext) : ResidencesEffect
    data class ResidenceCreated(val newResidenceId: String) : ResidencesEffect
    data class ShowToast(val message: String) : ResidencesEffect
}
