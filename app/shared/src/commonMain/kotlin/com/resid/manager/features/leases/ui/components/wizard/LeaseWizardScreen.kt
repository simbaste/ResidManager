package com.resid.manager.features.leases.ui.components.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.LeaseDraftStepUpdateRequest
import com.resid.manager.dto.UserSearchDto
import com.resid.manager.dto.GuarantorDto
import com.resid.manager.dto.InlineTenantCreateRequest
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.network.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch

@Composable
fun LeaseWizardScreen(
    jwtToken: String?,
    residenceUnits: List<ResidenceUnitDto>,
    isLoading: Boolean,
    errorMessage: String?,
    currencySymbol: String = com.resid.manager.dto.AppCurrency.DEFAULT.symbol,
    draftLeaseId: String?,
    onNavigateBack: () -> Unit,
    onSaveDraftStep: (LeaseDraftStepUpdateRequest) -> Unit,
    onFinalizeSignature: (String) -> Unit,
    initialResidenceUnitId: String? = null
) {
    var expandedStep by remember { mutableStateOf(WizardStep.TENANT) }

    // Step 1: TENANT
    var draftTenantId by remember { mutableStateOf<String?>(null) }
    var draftTenantName by remember { mutableStateOf("") }
    var isInlineTenant by remember { mutableStateOf(false) }
    var inlineFirstName by remember { mutableStateOf("") }
    var inlineLastName by remember { mutableStateOf("") }
    var inlineEmail by remember { mutableStateOf("") }
    var inlinePhone by remember { mutableStateOf("") }
    var userQuery by remember { mutableStateOf("") }
    var userResults by remember { mutableStateOf<List<UserSearchDto>>(emptyList()) }
    var isSearchingUsers by remember { mutableStateOf(false) }

    // Step 2: GUARANTOR
    var existingGuarantors by remember { mutableStateOf<List<GuarantorDto>>(emptyList()) }
    var selectedGuarantorId by remember { mutableStateOf<String?>(null) }
    var isReusingGuarantor by remember { mutableStateOf(false) }
    var guarantorLastName by remember { mutableStateOf("") }
    var guarantorFirstName by remember { mutableStateOf("") }
    var guarantorPhone by remember { mutableStateOf("") }
    var guarantorMonthlySalary by remember { mutableStateOf("") }
    var guarantorRelationship by remember { mutableStateOf("") }
    var guarantorJobTitle by remember { mutableStateOf("") }
    var guarantorEmployer by remember { mutableStateOf("") }

    // Step 3: UNIT
    var draftResidenceUnitId by remember { mutableStateOf(initialResidenceUnitId ?: "") }

    // Step 4: FINANCIALS
    var draftDepositAmount by remember { mutableStateOf("0.0") }
    var draftPaymentFrequency by remember { mutableStateOf("MONTHLY") }
    var draftAdvanceMonths by remember { mutableStateOf("1") }
    var draftAdvancePaymentAmount by remember { mutableStateOf("0.0") }
    var draftPaymentMethod by remember { mutableStateOf("CASH") }

    // Step 5: TIMELINE
    var draftStartDate by remember { mutableStateOf("") }
    var draftEndDate by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Création de contrat de bail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            WizardStep.entries.forEach { step ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedStep = step }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isComplete = false // TODO: Determine completion status based on step
                                if (isComplete) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                        Text(step.stepNumber.toString(), modifier = Modifier.padding(4.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = step.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (step.isOptional) {
                                    Text(
                                        text = " (Optionnel)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (expandedStep == step) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(
                            visible = expandedStep == step,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            ) {
                                when (step) {
                                    WizardStep.TENANT -> {
                                        WizardTenantStep(
                                            isInlineTenant = isInlineTenant,
                                            inlineFirstName = inlineFirstName,
                                            inlineLastName = inlineLastName,
                                            inlineEmail = inlineEmail,
                                            inlinePhone = inlinePhone,
                                            userQuery = userQuery,
                                            userResults = userResults,
                                            isSearchingUsers = isSearchingUsers,
                                            draftTenantId = draftTenantId,
                                            draftTenantName = draftTenantName,
                                            onInlineFirstNameChanged = { inlineFirstName = it },
                                            onInlineLastNameChanged = { inlineLastName = it },
                                            onInlineEmailChanged = { inlineEmail = it },
                                            onInlinePhoneChanged = { inlinePhone = it },
                                            onToggleInlineTenant = { isInlineTenant = it; if (it) draftTenantId = null },
                                            onUserQueryChanged = { q ->
                                                userQuery = q
                                                if (q.length >= 2) {
                                                    isSearchingUsers = true
                                                    coroutineScope.launch {
                                                        try {
                                                            val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/users/search") {
                                                                parameter("q", q)
                                                                header(HttpHeaders.Authorization, "Bearer $jwtToken")
                                                            }
                                                            if (response.status == HttpStatusCode.OK) {
                                                                userResults = response.body<List<UserSearchDto>>()
                                                            }
                                                        } catch (_: Exception) {}
                                                        isSearchingUsers = false
                                                    }
                                                } else {
                                                    userResults = emptyList()
                                                }
                                            },
                                            onUserSelected = { user ->
                                                draftTenantId = user.id
                                                draftTenantName = user.name
                                                userResults = emptyList()
                                                userQuery = user.name
                                                coroutineScope.launch {
                                                    try {
                                                        val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/tenants/${user.id}/guarantors") {
                                                            header(HttpHeaders.Authorization, "Bearer $jwtToken")
                                                        }
                                                        if (response.status == HttpStatusCode.OK) {
                                                            val list = response.body<List<GuarantorDto>>()
                                                            existingGuarantors = list
                                                            if (list.isNotEmpty()) {
                                                                isReusingGuarantor = true
                                                                val firstG = list.first()
                                                                selectedGuarantorId = firstG.id
                                                                guarantorLastName = firstG.lastName
                                                                guarantorFirstName = firstG.firstName
                                                                guarantorPhone = firstG.phone
                                                                guarantorMonthlySalary = firstG.monthlySalary?.toString() ?: ""
                                                                guarantorRelationship = firstG.relationship ?: ""
                                                                guarantorJobTitle = firstG.jobTitle ?: ""
                                                                guarantorEmployer = firstG.employer ?: ""
                                                            }
                                                        }
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                val inlineReq = if (isInlineTenant) InlineTenantCreateRequest(inlineFirstName, inlineLastName, inlineEmail, inlinePhone.ifBlank { null }) else null
                                                onSaveDraftStep(LeaseDraftStepUpdateRequest(step = "TENANT", tenantId = draftTenantId, inlineTenant = inlineReq))
                                                expandedStep = WizardStep.GUARANTOR
                                            },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Valider & Continuer")
                                        }
                                    }
                                    WizardStep.GUARANTOR -> {
                                        WizardGuarantorStep(
                                            existingGuarantors = existingGuarantors,
                                            selectedGuarantorId = selectedGuarantorId,
                                            isReusingGuarantor = isReusingGuarantor,
                                            guarantorLastName = guarantorLastName,
                                            guarantorFirstName = guarantorFirstName,
                                            guarantorPhone = guarantorPhone,
                                            guarantorMonthlySalary = guarantorMonthlySalary,
                                            guarantorRelationship = guarantorRelationship,
                                            guarantorJobTitle = guarantorJobTitle,
                                            guarantorEmployer = guarantorEmployer,
                                            currencySymbol = currencySymbol,
                                            onSelectExistingGuarantor = { g ->
                                                if (g != null) {
                                                    selectedGuarantorId = g.id
                                                    guarantorLastName = g.lastName
                                                    guarantorFirstName = g.firstName
                                                    guarantorPhone = g.phone
                                                    guarantorMonthlySalary = g.monthlySalary?.toString() ?: ""
                                                    guarantorRelationship = g.relationship ?: ""
                                                    guarantorJobTitle = g.jobTitle ?: ""
                                                    guarantorEmployer = g.employer ?: ""
                                                } else {
                                                    selectedGuarantorId = null
                                                }
                                            },
                                            onToggleReuseGuarantor = { isReusing ->
                                                isReusingGuarantor = isReusing
                                                if (!isReusing) {
                                                    selectedGuarantorId = null
                                                    guarantorLastName = ""
                                                    guarantorFirstName = ""
                                                    guarantorPhone = ""
                                                    guarantorMonthlySalary = ""
                                                    guarantorRelationship = ""
                                                    guarantorJobTitle = ""
                                                    guarantorEmployer = ""
                                                }
                                            },
                                            onGuarantorLastNameChanged = { guarantorLastName = it },
                                            onGuarantorFirstNameChanged = { guarantorFirstName = it },
                                            onGuarantorPhoneChanged = { guarantorPhone = it },
                                            onGuarantorMonthlySalaryChanged = { guarantorMonthlySalary = it },
                                            onGuarantorRelationshipChanged = { guarantorRelationship = it },
                                            onGuarantorJobTitleChanged = { guarantorJobTitle = it },
                                            onGuarantorEmployerChanged = { guarantorEmployer = it },
                                            onSkip = {
                                                onSaveDraftStep(LeaseDraftStepUpdateRequest(step = "GUARANTOR", guarantorId = null, guarantor = null))
                                                expandedStep = WizardStep.UNIT
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                val payload = if (isReusingGuarantor && selectedGuarantorId != null) {
                                                    LeaseDraftStepUpdateRequest(step = "GUARANTOR", guarantorId = selectedGuarantorId)
                                                } else {
                                                    val g = GuarantorDto(
                                                        lastName = guarantorLastName.trim(),
                                                        firstName = guarantorFirstName.trim(),
                                                        phone = guarantorPhone.trim(),
                                                        monthlySalary = guarantorMonthlySalary.toDoubleOrNull(),
                                                        relationship = guarantorRelationship.trim().ifBlank { null },
                                                        jobTitle = guarantorJobTitle.trim().ifBlank { null },
                                                        employer = guarantorEmployer.trim().ifBlank { null }
                                                    )
                                                    LeaseDraftStepUpdateRequest(step = "GUARANTOR", guarantor = g)
                                                }
                                                onSaveDraftStep(payload)
                                                expandedStep = WizardStep.UNIT
                                            },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Valider & Continuer")
                                        }
                                    }
                                    WizardStep.UNIT -> {
                                        WizardUnitStep(
                                            residenceUnits = residenceUnits,
                                            draftResidenceUnitId = draftResidenceUnitId,
                                            currencySymbol = currencySymbol,
                                            onUnitSelected = { draftResidenceUnitId = it }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                // Unit is handled via initial creation or a specific update, but we trigger next
                                                expandedStep = WizardStep.FINANCIALS
                                            },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Valider & Continuer")
                                        }
                                    }
                                    WizardStep.FINANCIALS -> {
                                        val selectedUnit = residenceUnits.firstOrNull { it.id == draftResidenceUnitId }
                                        WizardFinancialsStep(
                                            selectedResidenceUnit = selectedUnit,
                                            draftDepositAmount = draftDepositAmount,
                                            draftPaymentFrequency = draftPaymentFrequency,
                                            draftAdvanceMonths = draftAdvanceMonths,
                                            draftAdvancePaymentAmount = draftAdvancePaymentAmount,
                                            draftPaymentMethod = draftPaymentMethod,
                                            currencySymbol = currencySymbol,
                                            onDepositAmountChanged = { draftDepositAmount = it },
                                            onPaymentFrequencyChanged = { draftPaymentFrequency = it },
                                            onAdvanceMonthsChanged = { draftAdvanceMonths = it },
                                            onAdvancePaymentAmountChanged = { draftAdvancePaymentAmount = it },
                                            onPaymentMethodChanged = { draftPaymentMethod = it }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                onSaveDraftStep(LeaseDraftStepUpdateRequest(
                                                    step = "FINANCIALS",
                                                    depositAmount = draftDepositAmount.toDoubleOrNull() ?: 0.0,
                                                    paymentFrequency = draftPaymentFrequency,
                                                    advanceMonths = if (draftPaymentFrequency == "ANNUAL") (draftAdvanceMonths.toIntOrNull() ?: 12) else 1,
                                                    advancePaymentAmount = draftAdvancePaymentAmount.toDoubleOrNull() ?: 0.0
                                                ))
                                                expandedStep = WizardStep.TIMELINE
                                            },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Valider & Continuer")
                                        }
                                    }
                                    WizardStep.TIMELINE -> {
                                        WizardTimelineStep(
                                            draftStartDate = draftStartDate,
                                            draftEndDate = draftEndDate,
                                            onStartDateChanged = { draftStartDate = it },
                                            onEndDateChanged = { draftEndDate = it }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                onSaveDraftStep(LeaseDraftStepUpdateRequest(
                                                    step = "TIMELINE",
                                                    startDate = draftStartDate,
                                                    endDate = draftEndDate
                                                ))
                                                expandedStep = WizardStep.INVENTORY
                                            },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Valider & Continuer")
                                        }
                                    }
                                    WizardStep.INVENTORY -> {
                                        WizardInventoryStep(
                                            onSkip = {
                                                expandedStep = WizardStep.SIGNATURE
                                            }
                                        )
                                    }
                                    WizardStep.SIGNATURE -> {
                                        val canSign = draftTenantId != null && draftStartDate.isNotBlank() && draftEndDate.isNotBlank()
                                        WizardSignatureStep(canSign = canSign)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                if (draftLeaseId != null && canSign) {
                                                    onFinalizeSignature(draftLeaseId)
                                                }
                                            },
                                            enabled = !isLoading && draftLeaseId != null && canSign,
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            if (isLoading) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                            } else {
                                                Text("Finaliser et Signer")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
