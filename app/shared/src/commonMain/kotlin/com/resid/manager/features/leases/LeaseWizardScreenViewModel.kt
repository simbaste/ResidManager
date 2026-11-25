package com.resid.manager.features.leases

import androidx.lifecycle.viewModelScope
import com.resid.manager.SessionStorage
import com.resid.manager.base.MviViewModel
import com.resid.manager.dto.GuarantorDto
import com.resid.manager.dto.InlineTenantCreateRequest
import com.resid.manager.dto.LeaseDraftStepUpdateRequest
import com.resid.manager.dto.LeaseStatusDto
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
            is LeaseWizardIntent.SetExpandedStep -> handleSetExpandedStep(intent.step)

            // Step 1: Unit
            is LeaseWizardIntent.SelectUnit -> handleSelectUnit(intent.unitId)
            is LeaseWizardIntent.SubmitUnitStep -> submitUnitStep()

            // Step 2: Tenant
            is LeaseWizardIntent.SetUserQuery -> handleUserQuery(intent.query)
            is LeaseWizardIntent.SelectUser -> handleSelectUser(intent)
            is LeaseWizardIntent.ToggleInlineTenant -> updateState {
                it.copy(
                    isInlineTenant = intent.isInline,
                    draftTenantId = if (intent.isInline) null else it.draftTenantId,
                    inlineFirstNameError = null,
                    inlineLastNameError = null,
                    inlineEmailError = null
                )
            }
            is LeaseWizardIntent.SetInlineFirstName -> updateState {
                it.copy(inlineFirstName = intent.value, inlineFirstNameError = null)
            }
            is LeaseWizardIntent.SetInlineLastName -> updateState {
                it.copy(inlineLastName = intent.value, inlineLastNameError = null)
            }
            is LeaseWizardIntent.SetInlineEmail -> updateState {
                it.copy(inlineEmail = intent.value, inlineEmailError = null)
            }
            is LeaseWizardIntent.SetInlinePhone -> updateState { it.copy(inlinePhone = intent.value) }
            is LeaseWizardIntent.SubmitTenantStep -> submitTenantStep()

            // Step 3: Guarantor
            is LeaseWizardIntent.SelectExistingGuarantor -> handleSelectExistingGuarantor(intent.guarantor)
            is LeaseWizardIntent.ToggleReuseGuarantor -> handleToggleReuseGuarantor(intent.reuse)
            is LeaseWizardIntent.SetGuarantorLastName -> updateState {
                it.copy(guarantorLastName = intent.value, guarantorLastNameError = null)
            }
            is LeaseWizardIntent.SetGuarantorFirstName -> updateState {
                it.copy(guarantorFirstName = intent.value, guarantorFirstNameError = null)
            }
            is LeaseWizardIntent.SetGuarantorPhone -> updateState {
                it.copy(guarantorPhone = intent.value, guarantorPhoneError = null)
            }
            is LeaseWizardIntent.SetGuarantorMonthlySalary -> updateState { it.copy(guarantorMonthlySalary = intent.value) }
            is LeaseWizardIntent.SetGuarantorRelationship -> updateState { it.copy(guarantorRelationship = intent.value) }
            is LeaseWizardIntent.SetGuarantorJobTitle -> updateState { it.copy(guarantorJobTitle = intent.value) }
            is LeaseWizardIntent.SetGuarantorEmployer -> updateState { it.copy(guarantorEmployer = intent.value) }
            is LeaseWizardIntent.SkipGuarantorStep -> skipGuarantorStep()
            is LeaseWizardIntent.SubmitGuarantorStep -> submitGuarantorStep()

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

    private fun handleSetExpandedStep(step: WizardStep) {
        if (!isStepAccessible(step)) {
            updateState { it.copy(errorMessage = "Veuillez d'abord choisir et valider le logement (Étape 1).") }
            return
        }
        updateState { it.copy(currentStep = step) }
    }

    private fun handleInitialize(intent: LeaseWizardIntent.Initialize) {
        val residenceId = intent.residenceId ?: sessionStorage?.loadLastSelectedResidenceId()
        val initUnitId = intent.initialResidenceUnitId ?: uiState.value.draftResidenceUnitId

        updateState {
            it.copy(
                residenceId = residenceId,
                currencySymbol = intent.currencySymbol ?: it.currencySymbol,
                draftResidenceUnitId = initUnitId,
                draftLeaseId = intent.draftLeaseId ?: it.draftLeaseId
            )
        }

        val session = sessionStorage?.loadSession() ?: return
        if (residenceId != null) {
            viewModelScope.launch {
                updateState { it.copy(isLoading = true, errorMessage = null) }
                getAvailableUnitsUseCase(session.token, residenceId)
                    .onSuccess { units ->
                        val initialUnit = units.firstOrNull { u -> u.id == initUnitId }
                        val rentAndCharges = initialUnit?.let { u -> u.nominalRent + u.serviceCharges } ?: 0.0
                        val defaultDeposit = initialUnit?.nominalRent ?: 0.0
                        updateState {
                            it.copy(
                                isLoading = false,
                                availableUnits = units,
                                selectedUnit = initialUnit,
                                draftDepositAmount = if (it.draftDepositAmount == "0.0" && defaultDeposit > 0.0) defaultDeposit.toString() else it.draftDepositAmount,
                                totalMonthlyRentAndCharges = rentAndCharges
                            )
                        }

                        // If opened with pre-selected unit and no draft exists yet, initialize draft on backend
                        if (intent.draftLeaseId == null && !initUnitId.isNullOrBlank()) {
                            initLeaseDraftUseCase(session.token, initUnitId)
                                .onSuccess { lease ->
                                    updateState { s ->
                                        val updated = s.copy(draftLeaseId = lease.id)
                                        recalculateCompletionAndStatuses(updated, completed = WizardStep.UNIT)
                                    }
                                }
                                .onFailure { err ->
                                    updateState { it.copy(errorMessage = err.message) }
                                }
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
                    val tenantUser = lease.tenant
                    val tenantFullName = tenantUser?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim() } ?: ""
                    val tenantEmail = tenantUser?.email ?: ""
                    val tenantPhone = tenantUser?.phone ?: ""

                    // Also populate guarantor if present
                    val existingG = lease.guarantor

                    updateState { state ->
                        val unit = state.availableUnits.firstOrNull { it.id == lease.residenceUnitId }
                        val rentAndCharges = unit?.let { u -> u.nominalRent + u.serviceCharges } ?: 0.0
                        val updated = state.copy(
                            isLoading = false,
                            draftLeaseId = lease.id,
                            leaseStatus = lease.status,
                            draftTenantId = lease.tenantId,
                            draftTenantName = tenantFullName,
                            draftTenantEmail = tenantEmail,
                            draftTenantPhone = tenantPhone,
                            userQuery = tenantFullName,
                            isInlineTenant = false,
                            draftResidenceUnitId = lease.residenceUnitId,
                            selectedUnit = unit,
                            draftDepositAmount = (lease.depositAmount ?: 0.0).toString(),
                            draftPaymentFrequency = lease.paymentFrequencyDto?.name ?: "MONTHLY",
                            draftAdvanceMonths = (lease.advanceMonths ?: 1).toString(),
                            draftStartDate = lease.startDate ?: "",
                            draftEndDate = lease.endDate ?: "",
                            totalMonthlyRentAndCharges = rentAndCharges,
                            // Guarantor state
                            selectedGuarantorId = existingG?.id,
                            isReusingGuarantor = existingG != null,
                            guarantorLastName = existingG?.lastName ?: state.guarantorLastName,
                            guarantorFirstName = existingG?.firstName ?: state.guarantorFirstName,
                            guarantorPhone = existingG?.phone ?: state.guarantorPhone,
                            guarantorMonthlySalary = existingG?.monthlySalary?.toString() ?: state.guarantorMonthlySalary,
                            guarantorRelationship = existingG?.relationship ?: state.guarantorRelationship,
                            guarantorJobTitle = existingG?.jobTitle ?: state.guarantorJobTitle,
                            guarantorEmployer = existingG?.employer ?: state.guarantorEmployer
                        )
                        val withCalculations = recalculateCompletionAndStatuses(updated)
                        // Smart Resume: Open first uncompleted required step, or SIGNATURE if all prerequisite steps are done
                        val smartStep = determineInitialStep(withCalculations)
                        withCalculations.copy(currentStep = smartStep)
                    }

                    // If tenant exists, also fetch potential guarantors list for selection
                    if (lease.tenantId != null) {
                        fetchTenantGuarantorsUseCase(token, lease.tenantId)
                            .onSuccess { guarantors ->
                                updateState { s ->
                                    s.copy(existingGuarantors = guarantors)
                                }
                            }
                    }
                }
                .onFailure { error ->
                    updateState { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    private fun determineInitialStep(state: LeaseWizardUiState): WizardStep {
        // If lease is already signed, open SIGNATURE or INVENTORY
        if (state.leaseStatus == LeaseStatusDto.SIGNED_ACTIVE) {
            return WizardStep.INVENTORY
        }
        val hasUnit = state.draftResidenceUnitId.isNotBlank() && state.draftLeaseId != null
        if (!hasUnit) return WizardStep.UNIT

        val hasTenant = state.draftTenantId != null || (state.isInlineTenant && state.inlineFirstName.isNotBlank())
        if (!hasTenant) return WizardStep.TENANT

        val hasFinancials = (state.draftDepositAmount.toDoubleOrNull() ?: 0.0) >= 0.0 && state.draftPaymentFrequency.isNotBlank()
        if (!hasFinancials) return WizardStep.FINANCIALS

        val hasDates = state.draftStartDate.isNotBlank() && state.draftEndDate.isNotBlank()
        if (!hasDates) return WizardStep.TIMELINE

        return WizardStep.SIGNATURE
    }

    fun isStepAccessible(step: WizardStep): Boolean {
        // Step 1 (UNIT) is always accessible
        if (step == WizardStep.UNIT) return true
        // Steps 2 to 7 are strictly locked until UNIT is selected and saved on backend (draftLeaseId != null)
        val state = uiState.value
        return state.draftLeaseId != null && state.draftResidenceUnitId.isNotBlank()
    }

    fun isStepEditable(step: WizardStep, leaseStatus: LeaseStatusDto?): Boolean {
        if (!isStepAccessible(step)) return false

        if (leaseStatus == null || leaseStatus == LeaseStatusDto.DRAFT ||
            leaseStatus == LeaseStatusDto.PENDING_PAYMENT ||
            leaseStatus == LeaseStatusDto.DOWN_PAYMENT_PAID ||
            leaseStatus == LeaseStatusDto.PARTIALLY_PAID ||
            leaseStatus == LeaseStatusDto.PENDING_SIGNATURE) {
            // Unsigned lease: Steps 1 to 6 are editable. Step 7 (SIGNATURE) is dynamic
            return step != WizardStep.SIGNATURE
        }

        if (leaseStatus == LeaseStatusDto.SIGNED_ACTIVE) {
            // Already signed lease: UNIT, TENANT, FINANCIALS, TIMELINE are strictly locked.
            // INVENTORY is editable.
            // GUARANTOR is editable only if no guarantor was previously attached.
            return when (step) {
                WizardStep.UNIT, WizardStep.TENANT, WizardStep.FINANCIALS, WizardStep.TIMELINE, WizardStep.SIGNATURE -> false
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

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.isNotBlank() && emailRegex.matches(email)
    }

    private fun submitTenantStep() {
        val state = uiState.value
        if (state.isInlineTenant) {
            var hasError = false
            var firstNameErr: String? = null
            var lastNameErr: String? = null
            var emailErr: String? = null

            if (state.inlineFirstName.isBlank()) {
                firstNameErr = "Le prénom est obligatoire."
                hasError = true
            }
            if (state.inlineLastName.isBlank()) {
                lastNameErr = "Le nom est obligatoire."
                hasError = true
            }
            if (state.inlineEmail.isBlank()) {
                emailErr = "L'adresse email est obligatoire."
                hasError = true
            } else if (!isValidEmail(state.inlineEmail.trim())) {
                emailErr = "Format d'adresse email invalide."
                hasError = true
            }

            if (hasError) {
                updateState {
                    it.copy(
                        inlineFirstNameError = firstNameErr,
                        inlineLastNameError = lastNameErr,
                        inlineEmailError = emailErr,
                        errorMessage = "Veuillez corriger les erreurs du formulaire."
                    )
                }
                return
            }

            val inlineReq = InlineTenantCreateRequest(
                firstName = state.inlineFirstName.trim(),
                lastName = state.inlineLastName.trim(),
                email = state.inlineEmail.trim(),
                phone = state.inlinePhone.trim().ifBlank { null }
            )
            val payload = LeaseDraftStepUpdateRequest(
                step = "TENANT",
                inlineTenant = inlineReq
            )
            saveStep(payload, nextStep = WizardStep.GUARANTOR, completedStep = WizardStep.TENANT)
        } else {
            if (state.draftTenantId == null) {
                updateState { it.copy(errorMessage = "Veuillez sélectionner un locataire existant.") }
                return
            }
            val payload = LeaseDraftStepUpdateRequest(
                step = "TENANT",
                tenantId = state.draftTenantId
            )
            saveStep(payload, nextStep = WizardStep.GUARANTOR, completedStep = WizardStep.TENANT)
        }
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
        saveStep(payload, nextStep = WizardStep.FINANCIALS, completedStep = WizardStep.GUARANTOR, markSkipped = true)
    }

    private fun submitGuarantorStep() {
        val state = uiState.value
        if (state.isReusingGuarantor && state.selectedGuarantorId != null) {
            val payload = LeaseDraftStepUpdateRequest(step = "GUARANTOR", guarantorId = state.selectedGuarantorId)
            updateState { it.copy(isGuarantorSkipped = false) }
            saveStep(payload, nextStep = WizardStep.FINANCIALS, completedStep = WizardStep.GUARANTOR)
        } else {
            var hasError = false
            var lNameErr: String? = null
            var fNameErr: String? = null
            var phoneErr: String? = null

            if (state.guarantorLastName.isBlank()) {
                lNameErr = "Le nom du garant est obligatoire."
                hasError = true
            }
            if (state.guarantorFirstName.isBlank()) {
                fNameErr = "Le prénom du garant est obligatoire."
                hasError = true
            }
            if (state.guarantorPhone.isBlank()) {
                phoneErr = "Le téléphone du garant est obligatoire."
                hasError = true
            }

            if (hasError) {
                updateState {
                    it.copy(
                        guarantorLastNameError = lNameErr,
                        guarantorFirstNameError = fNameErr,
                        guarantorPhoneError = phoneErr,
                        errorMessage = "Veuillez renseigner tous les champs obligatoires du garant."
                    )
                }
                return
            }

            val g = GuarantorDto(
                lastName = state.guarantorLastName.trim(),
                firstName = state.guarantorFirstName.trim(),
                phone = state.guarantorPhone.trim(),
                monthlySalary = state.guarantorMonthlySalary.toDoubleOrNull(),
                relationship = state.guarantorRelationship.trim().ifBlank { null },
                jobTitle = state.guarantorJobTitle.trim().ifBlank { null },
                employer = state.guarantorEmployer.trim().ifBlank { null }
            )
            val payload = LeaseDraftStepUpdateRequest(step = "GUARANTOR", guarantor = g)
            updateState { it.copy(isGuarantorSkipped = false) }
            saveStep(payload, nextStep = WizardStep.FINANCIALS, completedStep = WizardStep.GUARANTOR)
        }
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

        val session = sessionStorage?.loadSession()
        if (session == null) {
            updateState { it.copy(errorMessage = "Session expirée. Veuillez vous reconnecter.") }
            return
        }

        // If draft is not yet initialized on backend, initialize it
        if (state.draftLeaseId == null) {
            viewModelScope.launch {
                updateState { it.copy(isSavingStep = true, errorMessage = null) }
                initLeaseDraftUseCase(session.token, state.draftResidenceUnitId)
                    .onSuccess { lease ->
                        updateState { s ->
                            val updated = s.copy(
                                isSavingStep = false,
                                draftLeaseId = lease.id,
                                currentStep = WizardStep.TENANT
                            )
                            recalculateCompletionAndStatuses(updated, completed = WizardStep.UNIT)
                        }
                    }
                    .onFailure { error ->
                        updateState { it.copy(isSavingStep = false, errorMessage = error.message) }
                    }
            }
        } else {
            // Update residence unit on existing draft
            val payload = LeaseDraftStepUpdateRequest(
                step = "UNIT",
                residenceUnitId = state.draftResidenceUnitId
            )
            saveStep(payload, nextStep = WizardStep.TENANT, completedStep = WizardStep.UNIT)
        }
    }

    private fun submitFinancialsStep() {
        val state = uiState.value
        val depositVal = state.draftDepositAmount.toDoubleOrNull()
        if (depositVal == null || depositVal < 0.0) {
            updateState { it.copy(errorMessage = "Le montant de la caution doit être supérieur ou égal à 0.") }
            return
        }

        val advanceVal = state.draftAdvancePaymentAmount.toDoubleOrNull()
        if (advanceVal == null || advanceVal < 0.0) {
            updateState { it.copy(errorMessage = "Le montant de l'acompte doit être supérieur ou égal à 0.") }
            return
        }

        val payload = LeaseDraftStepUpdateRequest(
            step = "FINANCIALS",
            depositAmount = depositVal,
            paymentFrequency = state.draftPaymentFrequency,
            advanceMonths = if (state.draftPaymentFrequency == "ANNUAL") (state.draftAdvanceMonths.toIntOrNull() ?: 12) else 1,
            advancePaymentAmount = advanceVal
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
            updateState { it.copy(errorMessage = "Veuillez sélectionner la date de début et la date de fin du bail.") }
            return
        }

        val duration = calculateDurationMonths(state.draftStartDate, state.draftEndDate)
        if (duration <= 0) {
            updateState { it.copy(errorMessage = "La date de fin doit être postérieure à la date de début.") }
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
            updateState { it.copy(errorMessage = "Le contrat de bail brouillon n'a pas encore été initialisé (Logement requis).") }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isSavingStep = true, errorMessage = null) }
            saveLeaseStepUseCase(session.token, draftId, payload)
                .onSuccess { updatedLease ->
                    val tenantUser = updatedLease.tenant
                    val tenantFullName = tenantUser?.let { "${it.firstName ?: ""} ${it.lastName ?: ""}".trim() }
                    val tenantEmail = tenantUser?.email
                    val tenantPhone = tenantUser?.phone

                    updateState { s ->
                        val updated = s.copy(
                            isSavingStep = false,
                            currentStep = nextStep,
                            draftTenantId = updatedLease.tenantId ?: s.draftTenantId,
                            draftTenantName = tenantFullName ?: s.draftTenantName,
                            draftTenantEmail = tenantEmail ?: s.draftTenantEmail,
                            draftTenantPhone = tenantPhone ?: s.draftTenantPhone,
                            userQuery = tenantFullName ?: s.userQuery
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

        val isUnitSaved = state.draftLeaseId != null && state.draftResidenceUnitId.isNotBlank()
        val hasTenant = (state.draftTenantId != null) || (state.isInlineTenant && state.inlineFirstName.isNotBlank())
        val hasFinancials = (state.draftDepositAmount.toDoubleOrNull() ?: -1.0) >= 0.0 && state.draftPaymentFrequency.isNotBlank()
        val hasDates = state.draftStartDate.isNotBlank() && state.draftEndDate.isNotBlank() && state.computedDurationMonths > 0
        
        // If unit is not saved yet, steps 2 to 7 are locked
        WizardStep.entries.forEach { step ->
            if (step != WizardStep.UNIT) {
                if (!isUnitSaved) {
                    newStatuses[step] = WizardStepStatus.LOCKED
                } else if (newStatuses[step] == WizardStepStatus.LOCKED) {
                    newStatuses[step] = WizardStepStatus.UPCOMING
                }
            }
        }

        // Auto-mark completed steps if data exists
        if (isUnitSaved && newStatuses[WizardStep.UNIT] != WizardStepStatus.SKIPPED) {
            newStatuses[WizardStep.UNIT] = WizardStepStatus.COMPLETED
        }
        if (hasTenant && newStatuses[WizardStep.TENANT] != WizardStepStatus.SKIPPED) {
            newStatuses[WizardStep.TENANT] = WizardStepStatus.COMPLETED
        }
        if (hasFinancials && newStatuses[WizardStep.FINANCIALS] != WizardStepStatus.SKIPPED) {
            newStatuses[WizardStep.FINANCIALS] = WizardStepStatus.COMPLETED
        }
        if (hasDates && newStatuses[WizardStep.TIMELINE] != WizardStepStatus.SKIPPED) {
            newStatuses[WizardStep.TIMELINE] = WizardStepStatus.COMPLETED
        }

        // Dynamic Lock on SIGNATURE: Only unlock if required steps (UNIT, TENANT, FINANCIALS, TIMELINE) are complete
        val isAlreadySigned = state.leaseStatus == LeaseStatusDto.SIGNED_ACTIVE
        val canSign = !isAlreadySigned && isUnitSaved && hasTenant && hasFinancials && hasDates

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
