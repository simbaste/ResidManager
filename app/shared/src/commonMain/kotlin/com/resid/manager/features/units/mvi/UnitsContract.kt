package com.resid.manager.features.units.mvi

import com.resid.manager.dto.EquipmentDto
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.ResidencePhotoDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.features.residences.mvi.ResidenceViewMode

data class UnitsUiState(
    val isLoading: Boolean = false,
    val activeResidence: ResidenceContext? = null,
    val isAuthorized: Boolean = false,
    val currencySymbol: String = com.resid.manager.dto.AppCurrency.DEFAULT.symbol,
    val units: List<ResidenceUnitDto> = emptyList(),
    val leases: List<LeaseDto> = emptyList(),
    val members: List<ResidenceMemberSummaryDto> = emptyList(),
    val selectedUnitForDetail: ResidenceUnitDto? = null,
    val editingUnit: ResidenceUnitDto? = null,
    val deletingUnitId: String? = null,
    val assigningLeaseUnitId: String? = null,
    val showCreateDialog: Boolean = false,
    val viewMode: ResidenceViewMode = ResidenceViewMode.GRID,
    val detailUnitPhotos: List<ResidencePhotoDto> = emptyList(),
    val isSavingPhotos: Boolean = false,
    val availableEquipments: List<EquipmentDto> = emptyList(),
    val errorMessage: String? = null
)

sealed interface UnitsIntent {
    data object Refresh : UnitsIntent
    data class LoadUnits(val residenceId: String) : UnitsIntent
    data class SelectUnitForDetail(val unit: ResidenceUnitDto?) : UnitsIntent
    data class SetViewMode(val mode: ResidenceViewMode) : UnitsIntent
    data class LoadUnitPhotos(val unitId: String) : UnitsIntent
    data class UpdateUnitPhotos(
        val unitId: String,
        val photos: List<com.resid.manager.features.residences.usecase.PhotoUploadItem>
    ) : UnitsIntent
    data class SetEditingUnit(val unit: ResidenceUnitDto?) : UnitsIntent
    data class SetDeletingUnitId(val unitId: String?) : UnitsIntent
    data class SetAssigningLeaseUnitId(val unitId: String?) : UnitsIntent
    data class SetShowCreateDialog(val show: Boolean) : UnitsIntent

    data class CreateUnit(
        val name: String,
        val floor: String,
        val type: String,
        val rent: Double,
        val charges: Double,
        val initialIndex: Double,
        val equipmentIds: List<String> = emptyList(),
        val photos: List<com.resid.manager.features.residences.usecase.PhotoUploadItem> = emptyList()
    ) : UnitsIntent

    data class UpdateUnit(
        val unitId: String,
        val name: String,
        val floor: String,
        val type: String,
        val rent: Double,
        val charges: Double,
        val initialIndex: Double,
        val equipmentIds: List<String> = emptyList()
    ) : UnitsIntent

    data class DeleteUnit(
        val unitId: String
    ) : UnitsIntent
}

sealed interface UnitsEffect {
    data class ShowToast(val message: String) : UnitsEffect
}
