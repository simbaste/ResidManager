package com.resid.manager.features.leases

import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.base.MviViewModel
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.features.leases.mvi.LeasesEffect
import com.resid.manager.features.leases.mvi.LeasesIntent
import com.resid.manager.features.leases.mvi.LeasesUiState
import com.resid.manager.features.leases.usecase.CreateLeaseUseCase
import com.resid.manager.features.leases.usecase.DeleteLeaseUseCase
import com.resid.manager.features.leases.usecase.DraftLeaseUseCases
import com.resid.manager.features.leases.usecase.FetchLeasesUseCase
import com.resid.manager.features.leases.usecase.RecordLeasePaymentUseCase
import com.resid.manager.features.leases.usecase.UpdateLeaseStatusUseCase
import com.resid.manager.features.members.usecase.FetchMembersUseCase
import com.resid.manager.features.residences.usecase.FetchResidencesUseCase
import com.resid.manager.features.units.usecase.FetchUnitsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class LeasesViewModel(
    private val fetchLeasesUseCase: FetchLeasesUseCase,
    private val createLeaseUseCase: CreateLeaseUseCase,
    private val deleteLeaseUseCase: DeleteLeaseUseCase,
    private val recordLeasePaymentUseCase: RecordLeasePaymentUseCase,
    private val updateLeaseStatusUseCase: UpdateLeaseStatusUseCase,
    private val fetchResidencesUseCase: FetchResidencesUseCase,
    private val fetchUnitsUseCase: FetchUnitsUseCase,
    private val fetchMembersUseCase: FetchMembersUseCase,
    private val draftLeaseUseCases: DraftLeaseUseCases,
    private val sessionStorage: SessionStorage? = null
) : MviViewModel<LeasesUiState, LeasesIntent, LeasesEffect>(LeasesUiState()) {

    init {
        val lastSelectedId = sessionStorage?.loadLastSelectedResidenceId()
        if (!lastSelectedId.isNullOrBlank()) {
            loadResidenceAndLeases(lastSelectedId)
        }
    }

    override fun onIntent(intent: LeasesIntent) {
        when (intent) {
            is LeasesIntent.Refresh -> {
                val residenceId = uiState.value.activeResidence?.residenceId
                    ?: sessionStorage?.loadLastSelectedResidenceId()
                if (!residenceId.isNullOrBlank()) {
                    loadLeases(residenceId)
                }
            }
            is LeasesIntent.LoadLeases -> loadResidenceAndLeases(intent.residenceId)
            is LeasesIntent.SelectLeaseForDetail -> updateState { it.copy(selectedLeaseForDetail = intent.lease) }
            is LeasesIntent.SetFilter -> updateState { it.copy(activeFilter = intent.filter) }
            is LeasesIntent.SetShowWizard -> {
                updateState { it.copy(showWizard = intent.show, errorMessage = null) }
                if (!intent.show) {
                    updateState { it.copy(draftLeaseId = null) } // reset draft ID when closing
                }
            }
            is LeasesIntent.SetShowTerminateConfirmation -> updateState { it.copy(showTerminateConfirmation = intent.show) }
            is LeasesIntent.RecordPayment -> recordPayment(intent)
            is LeasesIntent.UpdateStatus -> updateStatus(intent)
            is LeasesIntent.DeleteLease -> deleteLease(intent.leaseId)
            is LeasesIntent.CreateLease -> createLease(intent)
            is LeasesIntent.InitDraft -> initDraft(intent)
            is LeasesIntent.SaveDraftStep -> saveDraftStep(intent)
            is LeasesIntent.FinalizeDraft -> finalizeDraft(intent)
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
        loadLeases(residence.residenceId)
    }

    private fun loadResidenceAndLeases(residenceId: String) {
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
            loadLeases(residenceId)
        }
    }

    private fun loadLeases(residenceId: String) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            coroutineScope {
                val leasesDeferred = async { fetchLeasesUseCase(session.token, residenceId) }
                val unitsDeferred = async { fetchUnitsUseCase(session.token, residenceId) }
                val membersDeferred = async { fetchMembersUseCase(session.token, residenceId) }

                val leasesResult = leasesDeferred.await()
                val unitsResult = unitsDeferred.await()
                val membersResult = membersDeferred.await()

                val units = unitsResult.getOrDefault(emptyList())
                val members = membersResult.getOrDefault(emptyList())

                leasesResult
                    .onSuccess { list ->
                        updateState {
                            it.copy(
                                isLoading = false,
                                leases = list,
                                units = units,
                                members = members,
                                selectedLeaseForDetail = it.selectedLeaseForDetail?.let { selected -> list.firstOrNull { l -> l.id == selected.id } },
                                errorMessage = null
                            )
                        }
                    }
                    .onFailure { error ->
                        updateState {
                            it.copy(
                                isLoading = false,
                                units = units,
                                members = members,
                                errorMessage = error.message
                            )
                        }
                    }
            }
        }
    }

    private fun recordPayment(intent: LeasesIntent.RecordPayment) {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            recordLeasePaymentUseCase(session.token, intent.leaseId, intent.amount, intent.category)
                .onSuccess { updated ->
                    updateState {
                        it.copy(
                            selectedLeaseForDetail = updated
                        )
                    }
                    loadLeases(residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun updateStatus(intent: LeasesIntent.UpdateStatus) {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null, showTerminateConfirmation = false) }
            updateLeaseStatusUseCase(session.token, intent.leaseId, intent.status)
                .onSuccess { updated ->
                    updateState {
                        it.copy(
                            selectedLeaseForDetail = updated
                        )
                    }
                    loadLeases(residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun createLease(intent: LeasesIntent.CreateLease) {
        val session = sessionStorage?.loadSession() ?: return
        val residenceId = uiState.value.activeResidence?.residenceId
            ?: sessionStorage.loadLastSelectedResidenceId() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            createLeaseUseCase(session.token, intent.residenceUnitId, intent.request)
                .onSuccess {
                    updateState { it.copy(showWizard = false) }
                    emitEffect(LeasesEffect.LeaseCreatedSuccess)
                    loadLeases(residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun initDraft(intent: LeasesIntent.InitDraft) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            draftLeaseUseCases.initDraft(session.token, com.resid.manager.dto.LeaseDraftInitRequest(intent.residenceUnitId))
                .onSuccess { lease ->
                    updateState { it.copy(isLoading = false, draftLeaseId = lease.id) }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message ?: "Erreur d'initialisation du brouillon") }
                }
        }
    }

    private fun saveDraftStep(intent: LeasesIntent.SaveDraftStep) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            draftLeaseUseCases.updateDraftStep(session.token, intent.leaseId, intent.request)
                .onSuccess { lease ->
                    updateState { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message ?: "Erreur lors de la sauvegarde de l'étape") }
                }
        }
    }

    private fun finalizeDraft(intent: LeasesIntent.FinalizeDraft) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            draftLeaseUseCases.finalizeDraft(session.token, intent.leaseId)
                .onSuccess { lease ->
                    updateState { it.copy(isLoading = false, showWizard = false, draftLeaseId = null) }
                    emitEffect(LeasesEffect.LeaseCreatedSuccess)
                    val actId = uiState.value.activeResidence?.residenceId
                    if (actId != null) {
                        loadLeases(actId)
                    }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message ?: "Erreur de finalisation du bail") }
                }
        }
    }

    private fun deleteLease(leaseId: String) {
        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isDeleting = true, errorMessage = null) }
            deleteLeaseUseCase(session.token, leaseId)
                .onSuccess {
                    updateState { it.copy(isDeleting = false, selectedLeaseForDetail = null) }
                    emitEffect(LeasesEffect.LeaseDeletedSuccess)
                    emitEffect(LeasesEffect.ShowToast("Contrat de bail supprimé avec succès."))
                    val actId = uiState.value.activeResidence?.residenceId
                    if (actId != null) {
                        loadLeases(actId)
                    }
                }
                .onFailure { error ->
                    updateState { it.copy(isDeleting = false, errorMessage = error.message ?: "Erreur lors de la suppression du bail.") }
                    emitEffect(LeasesEffect.ShowToast(error.message ?: "Erreur lors de la suppression du bail."))
                }
        }
    }
}
