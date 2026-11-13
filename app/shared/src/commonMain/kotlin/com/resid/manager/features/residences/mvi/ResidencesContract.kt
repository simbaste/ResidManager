package com.resid.manager.features.residences.mvi

import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceSummaryItemDto

enum class ResidenceSortOption(val label: String) {
    SELECTED_FIRST("Sélectionnée en premier"),
    LAST_CONSULTED("Dernière consultation"),
    NAME_ASC("Nom (A - Z)")
}

enum class ResidenceViewMode {
    GRID,
    LIST
}

data class ResidencesUiState(
    val isLoading: Boolean = false,
    val residences: List<ResidenceContext> = emptyList(),
    val selectedResidence: ResidenceContext? = null,
    val selectedResidenceId: String? = null,
    val selectedResidenceForDetail: ResidenceContext? = null,
    val detailResidencePhotos: List<com.resid.manager.dto.ResidencePhotoDto> = emptyList(),
    val isSavingPhotos: Boolean = false,
    val consultedTimestamps: Map<String, Long> = emptyMap(),
    val sortOption: ResidenceSortOption = ResidenceSortOption.SELECTED_FIRST,
    val viewMode: ResidenceViewMode = ResidenceViewMode.GRID,
    val errorMessage: String? = null,
    val isUploadingPhoto: Boolean = false,

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
    data object Refresh : ResidencesIntent
    data object LoadResidences : ResidencesIntent
    data class SelectResidence(val residence: ResidenceContext) : ResidencesIntent
    data class SelectResidenceForDetail(val residence: ResidenceContext?) : ResidencesIntent
    data class LoadResidencePhotos(val residenceId: String) : ResidencesIntent
    data class CreateResidence(val name: String, val address: String, val defaultCurrency: String, val kWhPrice: Double, val imageBytes: ByteArray? = null) : ResidencesIntent
    data class UpdateResidence(val residenceId: String, val name: String, val address: String, val kWhPrice: Double, val imageBytes: ByteArray? = null) : ResidencesIntent
    data class UpdateResidencePhotos(val residenceId: String, val photos: List<com.resid.manager.features.residences.usecase.PhotoUploadItem>) : ResidencesIntent
    data class DeleteResidence(val residenceId: String) : ResidencesIntent
    data class SearchResidences(val query: String) : ResidencesIntent
    data class JoinResidence(val residenceId: String) : ResidencesIntent

    // Dialog toggles
    data class SetShowCreateDialog(val show: Boolean) : ResidencesIntent
    data class SetShowJoinDialog(val show: Boolean) : ResidencesIntent
    data class SetEditingResidence(val residence: ResidenceContext?) : ResidencesIntent
    data class SetDeletingResidence(val residence: ResidenceContext?) : ResidencesIntent

    // View configs
    data class SetViewMode(val mode: ResidenceViewMode) : ResidencesIntent
    data class SetSortOption(val option: ResidenceSortOption) : ResidencesIntent
    data class SetSelectedResidenceId(val residenceId: String?) : ResidencesIntent
}

sealed interface ResidencesEffect {
    data class NavigateToDashboard(val residence: ResidenceContext) : ResidencesEffect
    data class NavigateToDetail(val residence: ResidenceContext) : ResidencesEffect
    data class ResidenceCreated(val newResidenceId: String) : ResidencesEffect
    data class ResidenceActivated(val residence: ResidenceContext) : ResidencesEffect
    data class ShowToast(val message: String) : ResidencesEffect
}
