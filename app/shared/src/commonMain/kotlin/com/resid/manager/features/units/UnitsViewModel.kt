package com.resid.manager.features.units

import androidx.lifecycle.viewModelScope
import com.resid.manager.base.MviViewModel
import com.resid.manager.features.units.mvi.UnitsEffect
import com.resid.manager.features.units.mvi.UnitsIntent
import com.resid.manager.features.units.mvi.UnitsUiState
import com.resid.manager.features.units.usecase.CreateUnitUseCase
import com.resid.manager.features.units.usecase.DeleteUnitUseCase
import com.resid.manager.features.units.usecase.FetchUnitsUseCase
import com.resid.manager.features.units.usecase.UpdateUnitUseCase
import kotlinx.coroutines.launch

class UnitsViewModel(
    private val fetchUnitsUseCase: FetchUnitsUseCase,
    private val fetchUnitPhotosUseCase: com.resid.manager.features.units.usecase.FetchUnitPhotosUseCase,
    private val createUnitUseCase: CreateUnitUseCase,
    private val updateUnitUseCase: UpdateUnitUseCase,
    private val updateUnitPhotosUseCase: com.resid.manager.features.units.usecase.UpdateUnitPhotosUseCase,
    private val deleteUnitUseCase: DeleteUnitUseCase
) : MviViewModel<UnitsUiState, UnitsIntent, UnitsEffect>(UnitsUiState()) {

    override fun onIntent(intent: UnitsIntent) {
        when (intent) {
            is UnitsIntent.LoadUnits -> loadUnits(intent.token, intent.residenceId)
            is UnitsIntent.SelectUnitForDetail -> updateState { it.copy(selectedUnitForDetail = intent.unit, detailUnitPhotos = emptyList()) }
            is UnitsIntent.SetViewMode -> updateState { it.copy(viewMode = intent.mode) }
            is UnitsIntent.LoadUnitPhotos -> loadUnitPhotos(intent.token, intent.residenceId, intent.unitId)
            is UnitsIntent.UpdateUnitPhotos -> updateUnitPhotos(intent.token, intent.residenceId, intent.unitId, intent.photos)
            is UnitsIntent.SetEditingUnit -> updateState { it.copy(editingUnit = intent.unit) }
            is UnitsIntent.SetDeletingUnitId -> updateState { it.copy(deletingUnitId = intent.unitId) }
            is UnitsIntent.SetAssigningLeaseUnitId -> updateState { it.copy(assigningLeaseUnitId = intent.unitId) }
            is UnitsIntent.SetShowCreateDialog -> updateState { it.copy(showCreateDialog = intent.show, errorMessage = null) }
            is UnitsIntent.CreateUnit -> createUnit(intent)
            is UnitsIntent.UpdateUnit -> updateUnit(intent)
            is UnitsIntent.DeleteUnit -> deleteUnit(intent)
        }
    }

    private fun loadUnitPhotos(token: String, residenceId: String, unitId: String) {
        viewModelScope.launch {
            fetchUnitPhotosUseCase(token, residenceId, unitId)
                .onSuccess { list ->
                    updateState { it.copy(detailUnitPhotos = list) }
                }
                .onFailure { error ->
                    updateState { it.copy(errorMessage = error.message) }
                }
        }
    }

    private fun updateUnitPhotos(
        token: String,
        residenceId: String,
        unitId: String,
        photos: List<com.resid.manager.features.residences.usecase.PhotoUploadItem>
    ) {
        viewModelScope.launch {
            updateState { it.copy(isSavingPhotos = true, errorMessage = null) }
            updateUnitPhotosUseCase(token, residenceId, unitId, photos)
                .onSuccess { updatedPhotos ->
                    updateState { it.copy(isSavingPhotos = false, detailUnitPhotos = updatedPhotos) }
                    emitEffect(UnitsEffect.ShowToast("Photos du logement enregistrées avec succès !"))
                    loadUnits(token, residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isSavingPhotos = false, errorMessage = error.message) }
                }
        }
    }

    private fun loadUnits(token: String, residenceId: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            fetchUnitsUseCase(token, residenceId)
                .onSuccess { list ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            units = list,
                            selectedUnitForDetail = it.selectedUnitForDetail?.let { selected -> list.firstOrNull { u -> u.id == selected.id } },
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun createUnit(intent: UnitsIntent.CreateUnit) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            createUnitUseCase(
                token = intent.token,
                residenceId = intent.residenceId,
                name = intent.name,
                floor = intent.floor,
                type = intent.type,
                nominalRent = intent.rent,
                serviceCharges = intent.charges,
                initialElectricityIndex = intent.initialIndex,
                equipmentIds = intent.equipmentIds
            )
                .onSuccess {
                    updateState { it.copy(showCreateDialog = false) }
                    loadUnits(intent.token, intent.residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun updateUnit(intent: UnitsIntent.UpdateUnit) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            updateUnitUseCase(
                token = intent.token,
                residenceId = intent.residenceId,
                unitId = intent.unitId,
                name = intent.name,
                floor = intent.floor,
                type = intent.type,
                nominalRent = intent.rent,
                serviceCharges = intent.charges,
                initialElectricityIndex = intent.initialIndex,
                equipmentIds = intent.equipmentIds
            )
                .onSuccess { updated ->
                    updateState {
                        it.copy(
                            editingUnit = null,
                            selectedUnitForDetail = if (it.selectedUnitForDetail?.id == updated.id) updated else it.selectedUnitForDetail
                        )
                    }
                    loadUnits(intent.token, intent.residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun deleteUnit(intent: UnitsIntent.DeleteUnit) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            deleteUnitUseCase(intent.token, intent.residenceId, intent.unitId)
                .onSuccess {
                    updateState {
                        it.copy(
                            deletingUnitId = null,
                            selectedUnitForDetail = if (it.selectedUnitForDetail?.id == intent.unitId) null else it.selectedUnitForDetail
                        )
                    }
                    loadUnits(intent.token, intent.residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}
