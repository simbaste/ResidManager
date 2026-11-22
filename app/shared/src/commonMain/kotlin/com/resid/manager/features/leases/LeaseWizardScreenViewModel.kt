package com.resid.manager.features.leases

import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.base.MviViewModel
import com.resid.manager.dto.GuarantorDto
import com.resid.manager.dto.InlineTenantCreateRequest
import com.resid.manager.dto.LeaseDraftStepUpdateRequest
import com.resid.manager.features.leases.mvi.LeaseWizardEffect
import com.resid.manager.features.leases.mvi.LeaseWizardIntent
import com.resid.manager.features.leases.mvi.LeaseWizardUiState
import com.resid.manager.features.leases.mvi.WizardStepStatus
import com.resid.manager.features.leases.ui.components.wizard.WizardStep
import com.resid.manager.features.leases.usecase.FetchTenantGuarantorsUseCase
import com.resid.manager.features.leases.usecase.FinalizeLeaseUseCase
import com.resid.manager.features.leases.usecase.GetAvailableUnitsUseCase
import com.resid.manager.features.leases.usecase.InitLeaseDraftUseCase
import com.resid.manager.features.leases.usecase.LoadLeaseDraftUseCase
import com.resid.manager.features.leases.usecase.SaveLeaseStepUseCase
import com.resid.manager.features.leases.usecase.SearchUsersUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class LeaseWizardScreenViewModel(
    private val searchUsersUseCase: SearchUsersUseCase,
    private val fetchTenantGuarantorsUseCase: FetchTenantGuarantorsUseCase,
    private val getAvailableUnitsUseCase: GetAvailableUnitsUseCase,
    private val initLeaseDraftUseCase: InitLeaseDraftUseCase,
    private val saveLeaseStepUseCase: SaveLeaseStepUseCase,
    private val finalizeLeaseUseCase: FinalizeLeaseUseCase,
    private val loadLeaseDraftUseCase: LoadLeaseDraftUseCase,
    private val sessionStorage: SessionStorage? = null
) : MviViewModel<LeaseWizardUiState, LeaseWizardIntent, LeaseWizardEffect>(LeaseWizardUiState()) {

    private var userSearchJob: Job? = null

    override fun onIntent(intent: LeaseWizardIntent) {
        when (intent) {
            is LeaseWizardIntent.Initialize -> handleInitialize(intent)
            is LeaseWizardIntent.SetExpandedStep -> updateState { it.copy(currentStep = intent.step) }

            // Step 1: Tenant
            is LeaseWizardIntent.SetUserQuery -> handleUserQuery(intent.query)
            is LeaseWizardIntent.SelectUser -> handleSelectUser(intent)
            is LeaseWizardIntent.ToggleInlineTenant -> updateState {
                it.copy(
                    isInlineTenant = intent.isInline,
                    draftTenantId = if (intent.isInline) null else it.draftTenantId
                )
            }
            is LeaseWizardIntent.SetInlineFirstName -> updateState { it.copy(inlineFirstName = intent.value) }
            is LeaseWizardIntent.SetInlineLastName -> updateState { it.copy(inlineLastName = intent.value) }
            is LeaseWizardIntent.SetInlineEmail -> updateState { it.copy(inlineEmail = intent.value) }
            is LeaseWizardIntent.SetInlinePhone -> updateState { it.copy(inlinePhone = intent.value) }
            is LeaseWizardIntent.SubmitTenantStep -> submitTenantStep()

            // Step 2: Guarantor
            is LeaseWizardIntent.SelectExistingGuarantor -> handleSelectExistingGuarantor(intent.guarantor)
            is LeaseWizardIntent.ToggleReuseGuarantor -> handleToggleReuseGuarantor(intent.reuse)
            is LeaseWizardIntent.SetGuarantorLastName -> updateState { it.copy(guarantorLastName = intent.value) }
            is LeaseWizardIntent.SetGuarantorFirstName -> updateState { it.copy(guarantorFirstName = intent.value) }
            is LeaseWizardIntent.SetGuarantorPhone -> updateState { it.copy(guarantorPhone = intent.value) }
            is LeaseWizardIntent.SetGuarantorMonthlySalary -> updateState { it.copy(guarantorMonthlySalary = intent.value) }
            is LeaseWizardIntent.SetGuarantorRelationship -> updateState { it.copy(guarantorRelationship = intent.value) }
            is LeaseWizardIntent.SetGuarantorJobTitle -> updateState { it.copy(guarantorJobTitle = intent.value) }
            is LeaseWizardIntent.SetGuarantorEmployer -> updateState { it.copy(guarantorEmployer = intent.value) }
            is LeaseWizardIntent.SkipGuarantorStep -> skipGuarantorStep()
            is LeaseWizardIntent.SubmitGuarantorStep -> submitGuarantorStep()

            // Step 3: Unit
            is LeaseWizardIntent.SelectUnit -> handleSelectUnit(intent.unitId)
            is LeaseWizardIntent.SubmitUnitStep -> submitUnitStep()

            // Step 4: Financials
            is LeaseWizardIntent.SetDepositAmount -> updateState { it.copy(draftDepositAmount = intent.value) }
            is LeaseWizardIntent.SetPaymentFrequency -> updateState { it.copy(draftPaymentFrequency = intent.value) }
            is LeaseWizardIntent.SetAdvanceMonths -> updateState { it.copy(draftAdvanceMonths = intent.value) }
            is LeaseWizardIntent.SetAdvancePaymentAmount -> updateState { it.copy(draftAdvancePaymentAmount = intent.value) }
            is LeaseWizardIntent.SetPaymentMethod -> updateState { it.copy(draftPaymentMethod = intent.value) }
            is LeaseWizardIntent.SubmitFinancialsStep -> submitFinancialsStep()

            // Step 5: Timeline
            is LeaseWizardIntent.SetStartDate -> updateTimeline(startDate = intent.value)
            is LeaseWizardIntent.SetEndDate -> updateTimeline(endDate = intent.value)
            is LeaseWizardIntent.SetDateRange -> updateTimeline(startDate = intent.startDate, endDate = intent.endDate)
            is LeaseWizardIntent.SubmitTimelineStep -> submitTimelineStep()

            // Step 6: Inventory
            is LeaseWizardIntent.SkipInventoryStep -> skipInventoryStep()

            // Step 7: Signature
            is LeaseWizardIntent.FinalizeSignature -> finalizeSignature()

            is LeaseWizardIntent.DismissError -> updateState { it.copy(errorMessage = null) }
        }
    }

    private fun handleInitialize(intent: LeaseWizardIntent.Initialize) {
        val residenceId = intent.residenceId ?: sessionStorage?.loadLastSelectedResidenceId()
        updateState {
            it.copy(
                residenceId = residenceId,
                currencySymbol = intent.currencySymbol ?: it.currencySymbol,
                draftResidenceUnitId = intent.initialResidenceUnitId ?: it.draftResidenceUnitId,
                draftLeaseId = intent.draftLeaseId ?: it.draftLeaseId
            )
        }

        val session = sessionStorage?.loadSession() ?: return
        if (residenceId != null) {
            viewModelScope.launch {
                updateState { it.copy(isLoading = true, errorMessage = null) }
                getAvailableUnitsUseCase(session.token, residenceId)
                    .onSuccess { units ->
                        val initialUnit = units.firstOrNull { u -> u.id == (intent.initialResidenceUnitId ?: uiState.value.draftResidenceUnitId) }
                        updateState {
                            it.copy(
                                isLoading = false,
                                availableUnits = units,
                                selectedUnit = initialUnit,
                                totalMonthlyRentAndCharges = initialUnit?.let { u -> u.nominalRent + u.serviceCharges } ?: 0.0
                            )
                        }
                    }
                    .onFailure { error ->
                        updateState { it.copy(isLoading = false, errorMessage = error.message) }
                    }
            }
        }

        if (intent.draftLeaseId != null && residenceId != null) {
            loadExistingDraft(session.token, residenceId, intent.draftLeaseId)
        }
    }

    private fun loadExistingDraft(token: String, residenceId: String, draftLeaseId: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            loadLeaseDraftUseCase(token, residenceId, draftLeaseId)
                .onSuccess { lease ->
                    updateState { state ->
                        val unit = state.availableUnits.firstOrNull { it.id == lease.residenceUnitId }
                        val rentAndCharges = unit?.let { u -> u.nominalRent + u.serviceCharges } ?: 0.0
                        val updated = state.copy(
                            isLoading = false,
                            draftLeaseId = lease.id,
                            leaseStatus = lease.status,
                            draftTenantId = lease.tenantId,
                            draftResidenceUnitId = lease.residenceUnitId,
                            selectedUnit = unit,
                            draftDepositAmount = (lease.depositAmount ?: 0.0).toString(),
                            draftPaymentFrequency = lease.paymentFrequencyDto?.name ?: "MONTHLY",
                            draftAdvanceMonths = (lease.advanceMonths ?: 1).toString(),
                            draftStartDate = lease.startDate ?: "",
                            draftEndDate = lease.endDate ?: "",
                            totalMonthlyRentAndCharges = rentAndCharges
                        )
                        val withCalculations = recalculateCompletionAndStatuses(updated)
                        // Smart Resume: Open first uncompleted required step, or SIGNATURE if all prerequisite steps are done
                        val smartStep = determineInitialStep(withCalculations)
                        withCalculations.copy(currentStep = smartStep)
                    }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun determineInitialStep(state: LeaseWizardUiState): WizardStep {
        // If lease is already signed, open SIGNATURE or INVENTORY
        if (state.leaseStatus == com.resid.manager.dto.LeaseStatusDto.SIGNED_ACTIVE) {
            return WizardStep.INVENTORY
        }
        val hasTenant = state.draftTenantId != null || (state.isInlineTenant && state.inlineFirstName.isNotBlank())
        if (!hasTenant) return WizardStep.TENANT

        val hasUnit = state.draftResidenceUnitId.isNotBlank()
        if (!hasUnit) return WizardStep.UNIT

        val hasFinancials = (state.draftDepositAmount.toDoubleOrNull() ?: 0.0) >= 0.0 && state.draftPaymentFrequency.isNotBlank()
        if (!hasFinancials) return WizardStep.FINANCIALS

        val hasDates = state.draftStartDate.isNotBlank() && state.draftEndDate.isNotBlank()
        if (!hasDates) return WizardStep.TIMELINE

        return WizardStep.SIGNATURE
    }

    fun isStepEditable(step: WizardStep, leaseStatus: com.resid.manager.dto.LeaseStatusDto?): Boolean {
        if (leaseStatus == null || leaseStatus == com.resid.manager.dto.LeaseStatusDto.DRAFT ||
            leaseStatus == com.resid.manager.dto.LeaseStatusDto.PENDING_PAYMENT ||
            leaseStatus == com.resid.manager.dto.LeaseStatusDto.DOWN_PAYMENT_PAID ||
            leaseStatus == com.resid.manager.dto.LeaseStatusDto.PARTIALLY_PAID ||
            leaseStatus == com.resid.manager.dto.LeaseStatusDto.PENDING_SIGNATURE) {
            // Unsigned lease: Steps 1 to 6 are editable. Step 7 (SIGNATURE) is dynamic
            return step != WizardStep.SIGNATURE
        }

        if (leaseStatus == com.resid.manager.dto.LeaseStatusDto.SIGNED_ACTIVE) {
            // Already signed lease: UNIT, TENANT, FINANCIALS, TIMELINE are strictly locked.
            // INVENTORY is editable (état des lieux d'entrée/sortie, relevés de compteur, remise des clés).
            // GUARANTOR is editable only if no guarantor was previously attached.
            return when (step) {
                WizardStep.TENANT, WizardStep.UNIT, WizardStep.FINANCIALS, WizardStep.TIMELINE, WizardStep.SIGNATURE -> false
                WizardStep.INVENTORY -> true
                WizardStep.GUARANTOR -> uiState.value.existingGuarantors.isEmpty() && uiState.value.selectedGuarantorId == null
            }
        }

        return false
    }

    private fun handleUserQuery(query: String) {
        updateState { it.copy(userQuery = query) }
        userSearchJob?.cancel()

        if (query.trim().length < 2) {
            updateState { it.copy(userResults = emptyList(), isSearchingUsers = false) }
            return
        }

        val session = sessionStorage?.loadSession() ?: return
        userSearchJob = viewModelScope.launch {
            delay(350.milliseconds)
            updateState { it.copy(isSearchingUsers = true) }
            searchUsersUseCase(session.token, query.trim(), uiState.value.residenceId)
                .onSuccess { results ->
                    updateState { it.copy(userResults = results, isSearchingUsers = false) }
                }
                .onFailure {
                    updateState { it.copy(userResults = emptyList(), isSearchingUsers = false) }
                }
        }
    }

    private fun handleSelectUser(intent: LeaseWizardIntent.SelectUser) {
        val user = intent.user
        updateState {
            it.copy(
                draftTenantId = user.id,
                draftTenantName = user.name,
                userQuery = user.name,
                userResults = emptyList()
            )
        }

        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            fetchTenantGuarantorsUseCase(session.token, user.id)
                .onSuccess { guarantors ->
                    updateState { state ->
                        val firstG = guarantors.firstOrNull()
                        state.copy(
                            existingGuarantors = guarantors,
                            selectedGuarantorId = firstG?.id,
                            isReusingGuarantor = guarantors.isNotEmpty(),
                            guarantorLastName = firstG?.lastName ?: "",
                            guarantorFirstName = firstG?.firstName ?: "",
                            guarantorPhone = firstG?.phone ?: "",
                            guarantorMonthlySalary = firstG?.monthlySalary?.toString() ?: "",
                            guarantorRelationship = firstG?.relationship ?: "",
                            guarantorJobTitle = firstG?.jobTitle ?: "",
                            guarantorEmployer = firstG?.employer ?: ""
                        )
                    }
                }
        }
    }

    private fun submitTenantStep() {
        val state = uiState.value
        val hasTenant = if (state.isInlineTenant) {
            state.inlineFirstName.isNotBlank() && state.inlineLastName.isNotBlank() && state.inlineEmail.isNotBlank()
        } else {
            state.draftTenantId != null
        }

        if (!hasTenant) {
            updateState { it.copy(errorMessage = "Veuillez sélectionner ou renseigner un locataire.") }
            return
        }

        val inlineReq = if (state.isInlineTenant) {
            InlineTenantCreateRequest(
                firstName = state.inlineFirstName.trim(),
                lastName = state.inlineLastName.trim(),
                email = state.inlineEmail.trim(),
                phone = state.inlinePhone.trim().ifBlank { null }
            )
        } else null

        val payload = LeaseDraftStepUpdateRequest(
            step = "TENANT",
            tenantId = state.draftTenantId,
            inlineTenant = inlineReq
        )

        saveStep(payload, nextStep = WizardStep.GUARANTOR, completedStep = WizardStep.TENANT)
    }

    private fun handleSelectExistingGuarantor(guarantor: GuarantorDto?) {
        if (guarantor != null) {
            updateState {
                it.copy(
                    selectedGuarantorId = guarantor.id,
                    guarantorLastName = guarantor.lastName,
                    guarantorFirstName = guarantor.firstName,
                    guarantorPhone = guarantor.phone,
                    guarantorMonthlySalary = guarantor.monthlySalary?.toString() ?: "",
                    guarantorRelationship = guarantor.relationship ?: "",
                    guarantorJobTitle = guarantor.jobTitle ?: "",
                    guarantorEmployer = guarantor.employer ?: ""
                )
            }
        } else {
            updateState { it.copy(selectedGuarantorId = null) }
        }
    }

    private fun handleToggleReuseGuarantor(reuse: Boolean) {
        updateState {
            it.copy(
                isReusingGuarantor = reuse,
                selectedGuarantorId = if (!reuse) null else it.selectedGuarantorId,
                guarantorLastName = if (!reuse) "" else it.guarantorLastName,
                guarantorFirstName = if (!reuse) "" else it.guarantorFirstName,
                guarantorPhone = if (!reuse) "" else it.guarantorPhone,
                guarantorMonthlySalary = if (!reuse) "" else it.guarantorMonthlySalary,
                guarantorRelationship = if (!reuse) "" else it.guarantorRelationship,
                guarantorJobTitle = if (!reuse) "" else it.guarantorJobTitle,
                guarantorEmployer = if (!reuse) "" else it.guarantorEmployer
            )
        }
    }

    private fun skipGuarantorStep() {
        val payload = LeaseDraftStepUpdateRequest(
            step = "GUARANTOR",
            guarantorId = null,
            guarantor = null
        )
        updateState { it.copy(isGuarantorSkipped = true) }
        saveStep(payload, nextStep = WizardStep.UNIT, completedStep = WizardStep.GUARANTOR, markSkipped = true)
    }

    private fun submitGuarantorStep() {
        val state = uiState.value
        val payload = if (state.isReusingGuarantor && state.selectedGuarantorId != null) {
            LeaseDraftStepUpdateRequest(step = "GUARANTOR", guarantorId = state.selectedGuarantorId)
        } else {
            val g = GuarantorDto(
                lastName = state.guarantorLastName.trim(),
                firstName = state.guarantorFirstName.trim(),
                phone = state.guarantorPhone.trim(),
                monthlySalary = state.guarantorMonthlySalary.toDoubleOrNull(),
                relationship = state.guarantorRelationship.trim().ifBlank { null },
                jobTitle = state.guarantorJobTitle.trim().ifBlank { null },
                employer = state.guarantorEmployer.trim().ifBlank { null }
            )
            LeaseDraftStepUpdateRequest(step = "GUARANTOR", guarantor = g)
        }
        updateState { it.copy(isGuarantorSkipped = false) }
        saveStep(payload, nextStep = WizardStep.UNIT, completedStep = WizardStep.GUARANTOR)
    }

    private fun handleSelectUnit(unitId: String) {
        val unit = uiState.value.availableUnits.firstOrNull { it.id == unitId }
        val rentAndCharges = (unit?.nominalRent ?: 0.0) + (unit?.serviceCharges ?: 0.0)
        val defaultDeposit = unit?.nominalRent ?: 0.0
        updateState {
            it.copy(
                draftResidenceUnitId = unitId,
                selectedUnit = unit,
                draftDepositAmount = defaultDeposit.toString(),
                totalMonthlyRentAndCharges = rentAndCharges
            )
        }
    }

    private fun submitUnitStep() {
        val state = uiState.value
        if (state.draftResidenceUnitId.isBlank()) {
            updateState { it.copy(errorMessage = "Veuillez sélectionner un logement.") }
            return
        }

        // If draft is not yet initialized on backend, initialize it
        val session = sessionStorage?.loadSession()
        if (state.draftLeaseId == null && session != null) {
            viewModelScope.launch {
                updateState { it.copy(isSavingStep = true, errorMessage = null) }
                initLeaseDraftUseCase(session.token, state.draftResidenceUnitId)
                    .onSuccess { lease ->
                        updateState { s ->
                            val updated = s.copy(
                                isSavingStep = false,
                                draftLeaseId = lease.id,
                                currentStep = WizardStep.FINANCIALS
                            )
                            recalculateCompletionAndStatuses(updated, completed = WizardStep.UNIT)
                        }
                    }
                    .onFailure { error ->
                        updateState { it.copy(isSavingStep = false, errorMessage = error.message) }
                    }
            }
        } else {
            val payload = LeaseDraftStepUpdateRequest(
                step = "UNIT",
                residenceUnitId = state.draftResidenceUnitId
            )
            saveStep(payload, nextStep = WizardStep.FINANCIALS, completedStep = WizardStep.UNIT)
        }
    }

    private fun submitFinancialsStep() {
        val state = uiState.value
        val payload = LeaseDraftStepUpdateRequest(
            step = "FINANCIALS",
            depositAmount = state.draftDepositAmount.toDoubleOrNull() ?: 0.0,
            paymentFrequency = state.draftPaymentFrequency,
            advanceMonths = if (state.draftPaymentFrequency == "ANNUAL") (state.draftAdvanceMonths.toIntOrNull() ?: 12) else 1,
            advancePaymentAmount = state.draftAdvancePaymentAmount.toDoubleOrNull() ?: 0.0
        )
        saveStep(payload, nextStep = WizardStep.TIMELINE, completedStep = WizardStep.FINANCIALS)
    }

    private fun updateTimeline(startDate: String = uiState.value.draftStartDate, endDate: String = uiState.value.draftEndDate) {
        val duration = calculateDurationMonths(startDate, endDate)
        updateState {
            it.copy(
                draftStartDate = startDate,
                draftEndDate = endDate,
                computedDurationMonths = duration
            )
        }
    }

    private fun submitTimelineStep() {
        val state = uiState.value
        if (state.draftStartDate.isBlank() || state.draftEndDate.isBlank()) {
            updateState { it.copy(errorMessage = "Veuillez sélectionner la période du bail.") }
            return
        }
        val payload = LeaseDraftStepUpdateRequest(
            step = "TIMELINE",
            startDate = state.draftStartDate,
            endDate = state.draftEndDate
        )
        saveStep(payload, nextStep = WizardStep.INVENTORY, completedStep = WizardStep.TIMELINE)
    }

    private fun skipInventoryStep() {
        updateState { s ->
            val updated = s.copy(
                isInventorySkipped = true,
                currentStep = WizardStep.SIGNATURE
            )
            recalculateCompletionAndStatuses(updated, completed = WizardStep.INVENTORY, isSkipped = true)
        }
    }

    private fun finalizeSignature() {
        val state = uiState.value
        val leaseId = state.draftLeaseId ?: return
        if (!state.canSign) {
            updateState { it.copy(errorMessage = "Toutes les étapes obligatoires doivent être complétées.") }
            return
        }

        val session = sessionStorage?.loadSession() ?: return
        viewModelScope.launch {
            updateState { it.copy(isSavingStep = true, errorMessage = null) }
            finalizeLeaseUseCase(session.token, leaseId)
                .onSuccess { finalized ->
                    updateState { s ->
                        val updated = s.copy(isSavingStep = false)
                        recalculateCompletionAndStatuses(updated, completed = WizardStep.SIGNATURE)
                    }
                    emitEffect(LeaseWizardEffect.LeaseFinalizedSuccess(finalized))
                }
                .onFailure { error ->
                    updateState { it.copy(isSavingStep = false, errorMessage = error.message) }
                }
        }
    }

    private fun saveStep(
        payload: LeaseDraftStepUpdateRequest,
        nextStep: WizardStep,
        completedStep: WizardStep,
        markSkipped: Boolean = false
    ) {
        val session = sessionStorage?.loadSession()
        val draftId = uiState.value.draftLeaseId

        if (session == null) {
            updateState { it.copy(errorMessage = "Session expirée. Veuillez vous reconnecter.") }
            return
        }

        if (draftId == null) {
            // If draftId doesn't exist yet, we cache in state and proceed, or init draft if unit is selected
            val unitId = uiState.value.draftResidenceUnitId
            if (unitId.isNotBlank()) {
                viewModelScope.launch {
                    updateState { it.copy(isSavingStep = true, errorMessage = null) }
                    initLeaseDraftUseCase(session.token, unitId)
                        .onSuccess { lease ->
                            saveLeaseStepUseCase(session.token, lease.id, payload)
                                .onSuccess {
                                    updateState { s ->
                                        val updated = s.copy(
                                            isSavingStep = false,
                                            draftLeaseId = lease.id,
                                            currentStep = nextStep
                                        )
                                        recalculateCompletionAndStatuses(updated, completed = completedStep, isSkipped = markSkipped)
                                    }
                                }
                                .onFailure { err ->
                                    updateState { it.copy(isSavingStep = false, errorMessage = err.message) }
                                }
                        }
                        .onFailure { err ->
                            updateState { it.copy(isSavingStep = false, errorMessage = err.message) }
                        }
                }
            } else {
                // Just move state forward in memory; will save when unitId is assigned or initialized
                updateState { s ->
                    val updated = s.copy(currentStep = nextStep)
                    recalculateCompletionAndStatuses(updated, completed = completedStep, isSkipped = markSkipped)
                }
            }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isSavingStep = true, errorMessage = null) }
            saveLeaseStepUseCase(session.token, draftId, payload)
                .onSuccess {
                    updateState { s ->
                        val updated = s.copy(
                            isSavingStep = false,
                            currentStep = nextStep
                        )
                        recalculateCompletionAndStatuses(updated, completed = completedStep, isSkipped = markSkipped)
                    }
                }
                .onFailure { error ->
                    updateState { it.copy(isSavingStep = false, errorMessage = error.message) }
                }
        }
    }

    private fun recalculateCompletionAndStatuses(
        state: LeaseWizardUiState,
        completed: WizardStep? = null,
        isSkipped: Boolean = false
    ): LeaseWizardUiState {
        val newStatuses = state.stepStatuses.toMutableMap()
        if (completed != null) {
            newStatuses[completed] = if (isSkipped) WizardStepStatus.SKIPPED else WizardStepStatus.COMPLETED
        }

        val hasTenant = (state.draftTenantId != null) || (state.isInlineTenant && state.inlineFirstName.isNotBlank())
        val hasUnit = state.draftResidenceUnitId.isNotBlank()
        val hasFinancials = (state.draftDepositAmount.toDoubleOrNull() ?: 0.0) >= 0.0 && state.draftPaymentFrequency.isNotBlank()
        val hasDates = state.draftStartDate.isNotBlank() && state.draftEndDate.isNotBlank()
        
        // Dynamic Lock on SIGNATURE: Only unlock if required steps (TENANT, UNIT, FINANCIALS, TIMELINE) are complete
        // and for already signed leases, canSign is false.
        val isAlreadySigned = state.leaseStatus == com.resid.manager.dto.LeaseStatusDto.SIGNED_ACTIVE
        val canSign = !isAlreadySigned && hasTenant && hasUnit && hasFinancials && hasDates && (state.draftLeaseId != null)

        newStatuses[WizardStep.SIGNATURE] = when {
            isAlreadySigned -> WizardStepStatus.COMPLETED
            canSign -> WizardStepStatus.ACTIVE
            else -> WizardStepStatus.LOCKED
        }

        val completedCount = newStatuses.values.count { it == WizardStepStatus.COMPLETED || it == WizardStepStatus.SKIPPED }
        val percentage = ((completedCount.toDouble() / WizardStep.entries.size) * 100).toInt()

        return state.copy(
            stepStatuses = newStatuses,
            completionPercentage = percentage,
            canSign = canSign
        )
    }

    private fun calculateDurationMonths(startDate: String, endDate: String): Int {
        return try {
            val startParts = startDate.split("-")
            val endParts = endDate.split("-")
            val startYear = startParts[0].toInt()
            val startMonth = startParts[1].toInt()
            val endYear = endParts[0].toInt()
            val endMonth = endParts[1].toInt()
            val totalMonths = (endYear - startYear) * 12 + (endMonth - startMonth)
            if (totalMonths > 0) totalMonths else -1
        } catch (_: Exception) {
            -1
        }
    }
}
