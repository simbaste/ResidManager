package com.resid.manager.features.leases.ui.components.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.InlineTenantCreateRequest
import com.resid.manager.dto.LeaseCreateRequest
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.UserSearchDto
import com.resid.manager.network.ApiClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch

@Composable
fun LeaseWizardDialog(
    jwtToken: String?,
    residenceUnits: List<ResidenceUnitDto>,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, LeaseCreateRequest) -> Unit,
    initialResidenceUnitId: String? = null
) {
    var currentStep by remember { mutableStateOf(WizardStep.TENANT) }

    var draftTenantId by remember { mutableStateOf<String?>(null) }
    var draftTenantName by remember { mutableStateOf("") }
    var isInlineTenant by remember { mutableStateOf(false) }
    var inlineFirstName by remember { mutableStateOf("") }
    var inlineLastName by remember { mutableStateOf("") }
    var inlineEmail by remember { mutableStateOf("") }
    var inlinePhone by remember { mutableStateOf("") }

    var draftResidenceUnitId by remember { mutableStateOf(initialResidenceUnitId ?: "") }
    var draftDepositAmount by remember { mutableStateOf("0.0") }
    var draftPaymentFrequency by remember { mutableStateOf("MONTHLY") }
    var draftStartDate by remember { mutableStateOf("") }
    var draftEndDate by remember { mutableStateOf("") }
    var draftAdvanceMonths by remember { mutableStateOf("12") }
    var draftAdvancePaymentAmount by remember { mutableStateOf("0.0") }
    var draftPaymentMethod by remember { mutableStateOf("CASH") }

    var userQuery by remember { mutableStateOf("") }
    var userResults by remember { mutableStateOf<List<UserSearchDto>>(emptyList()) }
    var isSearchingUsers by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Assistant Nouveau Contrat de Bail", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WizardStep.entries.forEachIndexed { index, step ->
                        val isActive = step == currentStep
                        val stepNum = index + 1
                        val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Badge(containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                                Text(text = stepNum.toString(), modifier = Modifier.padding(4.dp), color = color)
                            }
                            Text(text = step.name, style = MaterialTheme.typography.bodySmall, color = color)
                        }
                    }
                }

                HorizontalDivider()

                when (currentStep) {
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
                            onToggleInlineTenant = {
                                isInlineTenant = it
                                if (it) draftTenantId = null
                            },
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
                            }
                        )
                    }

                    WizardStep.UNIT -> {
                        WizardUnitStep(
                            residenceUnits = residenceUnits,
                            draftResidenceUnitId = draftResidenceUnitId,
                            onUnitSelected = { draftResidenceUnitId = it }
                        )
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
                            onDepositAmountChanged = { draftDepositAmount = it },
                            onPaymentFrequencyChanged = { draftPaymentFrequency = it },
                            onAdvanceMonthsChanged = { draftAdvanceMonths = it },
                            onAdvancePaymentAmountChanged = { draftAdvancePaymentAmount = it },
                            onPaymentMethodChanged = { draftPaymentMethod = it }
                        )
                    }

                    WizardStep.TIMELINE -> {
                        WizardTimelineStep(
                            draftStartDate = draftStartDate,
                            draftEndDate = draftEndDate,
                            onStartDateChanged = { draftStartDate = it },
                            onEndDateChanged = { draftEndDate = it }
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            if (currentStep == WizardStep.TIMELINE) {
                Button(
                    onClick = {
                        val rentVal = residenceUnits.firstOrNull { it.id == draftResidenceUnitId }?.nominalRent ?: 0.0
                        val req = LeaseCreateRequest(
                            tenantId = draftTenantId,
                            inlineTenant = if (isInlineTenant) InlineTenantCreateRequest(inlineFirstName, inlineLastName, inlineEmail, inlinePhone.ifBlank { null }) else null,
                            residenceUnitId = draftResidenceUnitId,
                            depositAmount = draftDepositAmount.toDoubleOrNull() ?: 0.0,
                            paymentFrequency = draftPaymentFrequency,
                            startDate = draftStartDate,
                            endDate = draftEndDate,
                            monthlyRentAtSign = rentVal,
                            advanceMonths = if (draftPaymentFrequency == "ANNUAL") (draftAdvanceMonths.toIntOrNull() ?: 12) else 1,
                            advancePaymentAmount = (draftAdvancePaymentAmount.toDoubleOrNull() ?: 0.0)
                        )
                        onSubmit(draftResidenceUnitId, req)
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Générer le contrat")
                    }
                }
            } else {
                Button(onClick = {
                    currentStep = when (currentStep) {
                        WizardStep.TENANT -> WizardStep.UNIT
                        WizardStep.UNIT -> WizardStep.FINANCIALS
                        WizardStep.FINANCIALS -> WizardStep.TIMELINE
                        WizardStep.TIMELINE -> WizardStep.TIMELINE
                    }
                }) {
                    Text("Suivant")
                }
            }
        },
        dismissButton = {
            if (currentStep != WizardStep.TENANT) {
                TextButton(onClick = {
                    currentStep = when (currentStep) {
                        WizardStep.TENANT -> WizardStep.TENANT
                        WizardStep.UNIT -> WizardStep.TENANT
                        WizardStep.FINANCIALS -> WizardStep.UNIT
                        WizardStep.TIMELINE -> WizardStep.FINANCIALS
                    }
                }) {
                    Text("Retour")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Annuler")
                }
            }
        }
    )
}
