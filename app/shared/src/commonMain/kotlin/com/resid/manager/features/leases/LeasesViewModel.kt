package com.resid.manager.features.leases

import androidx.lifecycle.viewModelScope
import com.resid.manager.base.MviViewModel
import com.resid.manager.features.leases.mvi.LeasesEffect
import com.resid.manager.features.leases.mvi.LeasesIntent
import com.resid.manager.features.leases.mvi.LeasesUiState
import com.resid.manager.features.leases.usecase.CreateLeaseUseCase
import com.resid.manager.features.leases.usecase.FetchLeasesUseCase
import com.resid.manager.features.leases.usecase.RecordLeasePaymentUseCase
import com.resid.manager.features.leases.usecase.UpdateLeaseStatusUseCase
import kotlinx.coroutines.launch

class LeasesViewModel(
    private val fetchLeasesUseCase: FetchLeasesUseCase,
    private val createLeaseUseCase: CreateLeaseUseCase,
    private val recordLeasePaymentUseCase: RecordLeasePaymentUseCase,
    private val updateLeaseStatusUseCase: UpdateLeaseStatusUseCase
) : MviViewModel<LeasesUiState, LeasesIntent, LeasesEffect>(LeasesUiState()) {

    override fun onIntent(intent: LeasesIntent) {
        when (intent) {
            is LeasesIntent.LoadLeases -> loadLeases(intent.token, intent.residenceId)
            is LeasesIntent.SelectLeaseForDetail -> updateState { it.copy(selectedLeaseForDetail = intent.lease) }
            is LeasesIntent.SetFilter -> updateState { it.copy(activeFilter = intent.filter) }
            is LeasesIntent.SetShowWizard -> updateState { it.copy(showWizard = intent.show, errorMessage = null) }
            is LeasesIntent.SetShowTerminateConfirmation -> updateState { it.copy(showTerminateConfirmation = intent.show) }
            is LeasesIntent.RecordPayment -> recordPayment(intent)
            is LeasesIntent.UpdateStatus -> updateStatus(intent)
            is LeasesIntent.CreateLease -> createLease(intent)
        }
    }

    private fun loadLeases(token: String, residenceId: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            fetchLeasesUseCase(token, residenceId)
                .onSuccess { list ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            leases = list,
                            selectedLeaseForDetail = it.selectedLeaseForDetail?.let { selected -> list.firstOrNull { l -> l.id == selected.id } },
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun recordPayment(intent: LeasesIntent.RecordPayment) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            recordLeasePaymentUseCase(intent.token, intent.leaseId, intent.amount, intent.category)
                .onSuccess { updated ->
                    updateState {
                        it.copy(
                            selectedLeaseForDetail = updated
                        )
                    }
                    loadLeases(intent.token, intent.residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun updateStatus(intent: LeasesIntent.UpdateStatus) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null, showTerminateConfirmation = false) }
            updateLeaseStatusUseCase(intent.token, intent.leaseId, intent.status)
                .onSuccess { updated ->
                    updateState {
                        it.copy(
                            selectedLeaseForDetail = updated
                        )
                    }
                    loadLeases(intent.token, intent.residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun createLease(intent: LeasesIntent.CreateLease) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            createLeaseUseCase(intent.token, intent.residenceUnitId, intent.request)
                .onSuccess {
                    updateState { it.copy(showWizard = false) }
                    emitEffect(LeasesEffect.LeaseCreatedSuccess)
                    loadLeases(intent.token, intent.residenceId)
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }
}
