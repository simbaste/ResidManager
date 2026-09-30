package com.resid.manager.features.leases.mvi

import com.resid.manager.dto.LeaseCreateRequest
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.LeaseStatusDto

data class LeasesUiState(
    val isLoading: Boolean = false,
    val leases: List<LeaseDto> = emptyList(),
    val selectedLeaseForDetail: LeaseDto? = null,
    val activeFilter: String = "ALL", // "ALL", "ACTIVE", "PENDING"
    val showWizard: Boolean = false,
    val showTerminateConfirmation: Boolean = false,
    val errorMessage: String? = null
)

sealed interface LeasesIntent {
    data class LoadLeases(val token: String, val residenceId: String) : LeasesIntent
    data class SelectLeaseForDetail(val lease: LeaseDto?) : LeasesIntent
    data class SetFilter(val filter: String) : LeasesIntent
    data class SetShowWizard(val show: Boolean) : LeasesIntent
    data class SetShowTerminateConfirmation(val show: Boolean) : LeasesIntent

    data class RecordPayment(
        val token: String,
        val residenceId: String,
        val leaseId: String,
        val amount: Double,
        val category: String
    ) : LeasesIntent

    data class UpdateStatus(
        val token: String,
        val residenceId: String,
        val leaseId: String,
        val status: LeaseStatusDto
    ) : LeasesIntent

    data class CreateLease(
        val token: String,
        val residenceId: String,
        val residenceUnitId: String,
        val request: LeaseCreateRequest
    ) : LeasesIntent
}

sealed interface LeasesEffect {
    data class ShowToast(val message: String) : LeasesEffect
    data object LeaseCreatedSuccess : LeasesEffect
}
