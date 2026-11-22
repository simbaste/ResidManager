package com.resid.manager.features.leases.mvi

import com.resid.manager.dto.AppCurrency
import com.resid.manager.dto.LeaseCreateRequest
import com.resid.manager.dto.LeaseDraftStepUpdateRequest
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.LeaseStatusDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.dto.ResidenceUnitDto

data class LeasesUiState(
    val isLoading: Boolean = false,
    val activeResidence: ResidenceContext? = null,
    val isAuthorized: Boolean = false,
    val currencySymbol: String = AppCurrency.DEFAULT.symbol,
    val units: List<ResidenceUnitDto> = emptyList(),
    val members: List<ResidenceMemberSummaryDto> = emptyList(),
    val leases: List<LeaseDto> = emptyList(),
    val selectedLeaseForDetail: LeaseDto? = null,
    val activeFilter: String = "ALL", // "ALL", "ACTIVE", "PENDING"
    val showWizard: Boolean = false,
    val draftLeaseId: String? = null, // Stores the ID of the current draft
    val showTerminateConfirmation: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null
)

sealed interface LeasesIntent {
    data object Refresh : LeasesIntent
    data class LoadLeases(val residenceId: String) : LeasesIntent
    data class SelectLeaseForDetail(val lease: LeaseDto?) : LeasesIntent
    data class SetFilter(val filter: String) : LeasesIntent
    data class SetShowWizard(val show: Boolean) : LeasesIntent
    data class SetShowTerminateConfirmation(val show: Boolean) : LeasesIntent

    data class RecordPayment(
        val leaseId: String,
        val amount: Double,
        val category: String
    ) : LeasesIntent

    data class UpdateStatus(
        val leaseId: String,
        val status: LeaseStatusDto
    ) : LeasesIntent

    data class DeleteLease(
        val leaseId: String
    ) : LeasesIntent

    data class CreateLease(
        val residenceUnitId: String,
        val request: LeaseCreateRequest
    ) : LeasesIntent
    
    // New Intents for Draft Wizard
    data class InitDraft(val residenceUnitId: String) : LeasesIntent
    data class SaveDraftStep(val leaseId: String, val request: LeaseDraftStepUpdateRequest) : LeasesIntent
    data class FinalizeDraft(val leaseId: String) : LeasesIntent
}

sealed interface LeasesEffect {
    data class ShowToast(val message: String) : LeasesEffect
    data object LeaseCreatedSuccess : LeasesEffect
    data object LeaseDeletedSuccess : LeasesEffect
}
