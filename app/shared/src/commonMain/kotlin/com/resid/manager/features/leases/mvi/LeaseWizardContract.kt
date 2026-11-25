package com.resid.manager.features.leases.mvi

import com.resid.manager.dto.AppCurrency
import com.resid.manager.dto.GuarantorDto
import com.resid.manager.dto.LeaseDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.UserSearchDto
import com.resid.manager.features.leases.ui.components.wizard.WizardStep

enum class WizardStepStatus {
    COMPLETED,
    SKIPPED,
    ACTIVE,
    UPCOMING,
    LOCKED
}

data class LeaseWizardUiState(
    val currentStep: WizardStep = WizardStep.UNIT,
    val stepStatuses: Map<WizardStep, WizardStepStatus> = WizardStep.entries.associateWith { step ->
        if (step == WizardStep.UNIT) WizardStepStatus.ACTIVE else WizardStepStatus.LOCKED
    },
    val completionPercentage: Int = 0,

    // Residence context & Units
    val residenceId: String? = null,
    val currencySymbol: String = AppCurrency.DEFAULT.symbol,
    val availableUnits: List<ResidenceUnitDto> = emptyList(),
    val draftLeaseId: String? = null,
    val leaseStatus: com.resid.manager.dto.LeaseStatusDto? = null,

    // Step 1: UNIT
    val draftResidenceUnitId: String = "",
    val selectedUnit: ResidenceUnitDto? = null,

    // Step 2: TENANT
    val isInlineTenant: Boolean = false,
    val draftTenantId: String? = null,
    val draftTenantName: String = "",
    val draftTenantEmail: String = "",
    val draftTenantPhone: String = "",
    val inlineFirstName: String = "",
    val inlineLastName: String = "",
    val inlineEmail: String = "",
    val inlinePhone: String = "",
    val userQuery: String = "",
    val userResults: List<UserSearchDto> = emptyList(),
    val isSearchingUsers: Boolean = false,
    val inlineFirstNameError: String? = null,
    val inlineLastNameError: String? = null,
    val inlineEmailError: String? = null,

    // Step 3: GUARANTOR
    val existingGuarantors: List<GuarantorDto> = emptyList(),
    val selectedGuarantorId: String? = null,
    val isReusingGuarantor: Boolean = false,
    val guarantorLastName: String = "",
    val guarantorFirstName: String = "",
    val guarantorPhone: String = "",
    val guarantorMonthlySalary: String = "",
    val guarantorRelationship: String = "",
    val guarantorJobTitle: String = "",
    val guarantorEmployer: String = "",
    val isGuarantorSkipped: Boolean = false,
    val guarantorLastNameError: String? = null,
    val guarantorFirstNameError: String? = null,
    val guarantorPhoneError: String? = null,

    // Step 4: FINANCIALS
    val draftDepositAmount: String = "0.0",
    val draftPaymentFrequency: String = "MONTHLY",
    val draftAdvanceMonths: String = "1",
    val draftAdvancePaymentAmount: String = "0.0",
    val draftPaymentMethod: String = "CASH",
    val totalMonthlyRentAndCharges: Double = 0.0,

    // Step 5: TIMELINE
    val draftStartDate: String = "",
    val draftEndDate: String = "",
    val computedDurationMonths: Int = -1,

    // Step 6: INVENTORY
    val isInventorySkipped: Boolean = false,

    // Step 7: SIGNATURE
    val canSign: Boolean = false,

    // Global loading & errors
    val isLoading: Boolean = false,
    val isSavingStep: Boolean = false,
    val errorMessage: String? = null
)

sealed interface LeaseWizardIntent {
    data class Initialize(
        val initialResidenceUnitId: String? = null,
        val draftLeaseId: String? = null,
        val residenceId: String? = null,
        val currencySymbol: String? = null
    ) : LeaseWizardIntent

    data class SetExpandedStep(val step: WizardStep) : LeaseWizardIntent

    // Step 1: Unit Intents
    data class SelectUnit(val unitId: String) : LeaseWizardIntent
    data object SubmitUnitStep : LeaseWizardIntent

    // Step 2: Tenant Intents
    data class SetUserQuery(val query: String) : LeaseWizardIntent
    data class SelectUser(val user: UserSearchDto) : LeaseWizardIntent
    data class ToggleInlineTenant(val isInline: Boolean) : LeaseWizardIntent
    data class SetInlineFirstName(val value: String) : LeaseWizardIntent
    data class SetInlineLastName(val value: String) : LeaseWizardIntent
    data class SetInlineEmail(val value: String) : LeaseWizardIntent
    data class SetInlinePhone(val value: String) : LeaseWizardIntent
    data object SubmitTenantStep : LeaseWizardIntent

    // Step 3: Guarantor Intents
    data class SelectExistingGuarantor(val guarantor: GuarantorDto?) : LeaseWizardIntent
    data class ToggleReuseGuarantor(val reuse: Boolean) : LeaseWizardIntent
    data class SetGuarantorLastName(val value: String) : LeaseWizardIntent
    data class SetGuarantorFirstName(val value: String) : LeaseWizardIntent
    data class SetGuarantorPhone(val value: String) : LeaseWizardIntent
    data class SetGuarantorMonthlySalary(val value: String) : LeaseWizardIntent
    data class SetGuarantorRelationship(val value: String) : LeaseWizardIntent
    data class SetGuarantorJobTitle(val value: String) : LeaseWizardIntent
    data class SetGuarantorEmployer(val value: String) : LeaseWizardIntent
    data object SkipGuarantorStep : LeaseWizardIntent
    data object SubmitGuarantorStep : LeaseWizardIntent

    // Step 4: Financials Intents
    data class SetDepositAmount(val value: String) : LeaseWizardIntent
    data class SetPaymentFrequency(val value: String) : LeaseWizardIntent
    data class SetAdvanceMonths(val value: String) : LeaseWizardIntent
    data class SetAdvancePaymentAmount(val value: String) : LeaseWizardIntent
    data class SetPaymentMethod(val value: String) : LeaseWizardIntent
    data object SubmitFinancialsStep : LeaseWizardIntent

    // Step 5: Timeline Intents
    data class SetStartDate(val value: String) : LeaseWizardIntent
    data class SetEndDate(val value: String) : LeaseWizardIntent
    data class SetDateRange(val startDate: String, val endDate: String) : LeaseWizardIntent
    data object SubmitTimelineStep : LeaseWizardIntent

    // Step 6: Inventory Intents
    data object SkipInventoryStep : LeaseWizardIntent

    // Step 7: Signature Intents
    data object FinalizeSignature : LeaseWizardIntent

    data object DismissError : LeaseWizardIntent
}

sealed interface LeaseWizardEffect {
    data object NavigateBack : LeaseWizardEffect
    data class LeaseFinalizedSuccess(val lease: LeaseDto) : LeaseWizardEffect
    data class ShowToast(val message: String) : LeaseWizardEffect
}
