package com.resid.manager.features.units

import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.base.MviViewModel
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.features.leases.usecase.FetchLeasesUseCase
import com.resid.manager.features.members.usecase.FetchMembersUseCase
import com.resid.manager.features.residences.usecase.FetchResidencesUseCase
import com.resid.manager.features.residences.usecase.PhotoUploadItem
import com.resid.manager.features.units.mvi.UnitsEffect
import com.resid.manager.features.units.mvi.UnitsIntent
import com.resid.manager.features.units.mvi.UnitsUiState
import com.resid.manager.features.units.usecase.CreateUnitUseCase
import com.resid.manager.features.units.usecase.DeleteUnitUseCase
import com.resid.manager.features.units.usecase.FetchUnitPhotosUseCase
import com.resid.manager.features.units.usecase.FetchUnitsUseCase
import com.resid.manager.features.units.usecase.UpdateUnitPhotosUseCase
import com.resid.manager.features.units.usecase.UpdateUnitUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class UnitsViewModel(
    private val fetchUnitsUseCase: FetchUnitsUseCase,
    private val fetchUnitPhotosUseCase: FetchUnitPhotosUseCase,
    private val createUnitUseCase: CreateUnitUseCase,
    private val updateUnitUseCase: UpdateUnitUseCase,
    private val updateUnitPhotosUseCase: UpdateUnitPhotosUseCase,
    private val deleteUnitUseCase: DeleteUnitUseCase,
    private val fetchResidencesUseCase: FetchResidencesUseCase,
    private val fetchLeasesUseCase: FetchLeasesUseCase,
    private val fetchMembersUseCase: FetchMembersUseCase,
    private val sessionStorage: SessionStorage? = null
) : MviViewModel<UnitsUiState, UnitsIntent, UnitsEffect>(UnitsUiState()) {

    init {
        val lastSelectedId = sessionStorage?.loadLastSelectedResidenceId()
        if (!lastSelectedId.isNullOrBlank()) {
            loadResidenceAndUnits(lastSelectedId)
        }
    }

    override fun onIntent(intent: UnitsIntent) {
        when (intent) {
            is UnitsIntent.Refresh -> {
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (!residenceId.isNullOrBlank()) {
                    loadUnits(residenceId)
                }
            }
            is UnitsIntent.LoadUnits -> loadResidenceAndUnits(intent.residenceId)
            is UnitsIntent.SelectUnitForDetail -> updateState { it.copy(selectedUnitForDetail = intent.unit, detailUnitPhotos = emptyList()) }
            is UnitsIntent.SetViewMode -> updateState { it.copy(viewMode = intent.mode) }
            is UnitsIntent.LoadUnitPhotos -> loadUnitPhotos(intent.unitId)
            is UnitsIntent.UpdateUnitPhotos -> updateUnitPhotos(intent.unitId, intent.photos)
            is UnitsIntent.SetEditingUnit -> updateState { it.copy(editingUnit = intent.unit) }
            is UnitsIntent.SetDeletingUnitId -> updateState { it.copy(deletingUnitId = intent.unitId) }
            is UnitsIntent.SetAssigningLeaseUnitId -> updateState { it.copy(assigningLeaseUnitId = intent.unitId) }
            is UnitsIntent.SetShowCreateDialog -> updateState { it.copy(showCreateDialog = intent.show, errorMessage = null) }
            is UnitsIntent.CreateUnit -> createUnit(intent)
            is UnitsIntent.UpdateUnit -> updateUnit(intent)
            is UnitsIntent.DeleteUnit -> deleteUnit(intent.unitId)
        }
    }

    fun updateActiveResidence(residence: ResidenceContext?) {
        if (residence == null) {
            updateState {
                it.copy(
                    activeResidence = null,
                    isAuthorized = false,
                    currencySymbol = com.resid.manager.dto.AppCurrency.DEFAULT.symbol,
                    units = emptyList(),
                    leases = emptyList(),
                    members = emptyList()
                )
            }
            return
        }
        val isAuth = residence.userRoleInResidence == UserRole.ADMIN ||
            residence.userRoleInResidence == UserRole.OWNER ||
            residence.userRoleInResidence == UserRole.MANAGER
        updateState {
            it.copy(
                activeResidence = residence,
                isAuthorized = isAuth,
                currencySymbol = residence.currencySymbol
            )
        }
        loadUnits(residence.residenceId)
    }

    private fun loadResidenceAndUnits(residenceId: String) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            if (uiState.value.activeResidence == null || uiState.value.activeResidence?.residenceId != residenceId) {
                fetchResidencesUseCase(session.token).onSuccess { list ->
                    val matched = list.firstOrNull { it.residenceId == residenceId }
                    if (matched != null) {
                        val isAuth = matched.userRoleInResidence == UserRole.ADMIN ||
                            matched.userRoleInResidence == UserRole.OWNER ||
                            matched.userRoleInResidence == UserRole.MANAGER
                        updateState {
                            it.copy(
                                activeResidence = matched,
                                isAuthorized = isAuth,
                                currencySymbol = matched.currencySymbol
                            )
                        }
                    }
                }
            }
            loadUnits(residenceId)
        }
    }

    private fun loadUnits(residenceId: String) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val unitsDeferred = async { fetchUnitsUseCase(session.token, residenceId) }
                val leasesDeferred = async { fetchLeasesUseCase(session.token, residenceId) }
                val membersDeferred = async { fetchMembersUseCase(session.token, residenceId) }

                val unitsResult = unitsDeferred.await()
                val leasesResult = leasesDeferred.await()
                val membersResult = membersDeferred.await()

                val leases = leasesResult.getOrDefault(emptyList())
                val members = membersResult.getOrDefault(emptyList())

                unitsResult
                    .onSuccess { list ->
                        updateState {
                            it.copy(
                                isLoading = false,
                                units = list,
                                leases = leases,
                                members = members,
                                selectedUnitForDetail = it.selectedUnitForDetail?.let { selected -> list.firstOrNull { u -> u.id == selected.id } },
                                errorMessage = null
                            )
                        }
                    }
                    .onFailure { error ->
                        updateState {
                            it.copy(
                                isLoading = false,
                                leases = leases,
                                members = members,
                                errorMessage = error.message
                            )
                        }
                    }
            }
        }
    }

    private fun loadUnitPhotos(unitId: String) {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        viewModelScope.launch {
            fetchUnitPhotosUseCase(session.token, residenceId, unitId)
                .onSuccess { list ->
                    updateState { it.copy(detailUnitPhotos = list) }
                }
                .onFailure { error ->
                    updateState { it.copy(errorMessage = error.message) }
                }
        }
    }

    private fun updateUnitPhotos(
        unitId: String,
        photos: List<PhotoUploadItem>
    ) {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isSavingPhotos = true, errorMessage = null) }
            updateUnitPhotosUseCase(session.token, residenceId, unitId, photos)
                .onSuccess { updatedPhotos ->
                    updateState { it.copy(isSavingPhotos = false, detailUnitPhotos = updatedPhotos) }
                    emitEffect(UnitsEffect.ShowToast("Photos du logement enregistrées avec succès !"))
                    loadUnits(residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isSavingPhotos = false, errorMessage = error.message) }
                }
        }
    }

    private fun createUnit(intent: UnitsIntent.CreateUnit) {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            createUnitUseCase(
                token = session.token,
                residenceId = residenceId,
                name = intent.name,
                floor = intent.floor,
                type = intent.type,
                nominalRent = intent.rent,
                serviceCharges = intent.charges,
                initialElectricityIndex = intent.initialIndex,
                equipmentIds = intent.equipmentIds
            )
                .onSuccess { createdUnit ->
                    if (intent.photos.isNotEmpty()) {
                        updateUnitPhotosUseCase(
                            token = session.token,
                            residenceId = residenceId,
                            unitId = createdUnit.id,
                            photos = intent.photos
                        )
                    }
                    updateState { it.copy(showCreateDialog = false, isLoading = false) }
                    loadUnits(residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun updateUnit(intent: UnitsIntent.UpdateUnit) {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            updateUnitUseCase(
                token = session.token,
                residenceId = residenceId,
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
                    loadUnits(residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun deleteUnit(unitId: String) {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            deleteUnitUseCase(session.token, residenceId, unitId)
                .onSuccess {
                    updateState {
                        it.copy(
                            deletingUnitId = null,
                            selectedUnitForDetail = if (it.selectedUnitForDetail?.id == unitId) null else it.selectedUnitForDetail
                        )
                    }
                    loadUnits(residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}
