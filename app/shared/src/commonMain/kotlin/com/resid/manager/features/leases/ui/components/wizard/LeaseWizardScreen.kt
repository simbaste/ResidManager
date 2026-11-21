package com.resid.manager.features.leases.ui.components.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resid.manager.features.leases.wizard.LeaseWizardEffect
import com.resid.manager.features.leases.wizard.LeaseWizardIntent
import com.resid.manager.features.leases.wizard.LeaseWizardScreenViewModel
import com.resid.manager.features.leases.wizard.LeaseWizardUiState
import com.resid.manager.features.leases.wizard.WizardStepStatus
import org.koin.compose.koinInject

@Composable
fun LeaseWizardScreen(
    onNavigateBack: () -> Unit,
    initialResidenceUnitId: String? = null,
    draftLeaseId: String? = null,
    residenceId: String? = null,
    currencySymbol: String? = null,
    viewModel: LeaseWizardScreenViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialResidenceUnitId, draftLeaseId, residenceId) {
        viewModel.onIntent(
            LeaseWizardIntent.Initialize(
                initialResidenceUnitId = initialResidenceUnitId,
                draftLeaseId = draftLeaseId,
                residenceId = residenceId,
                currencySymbol = currencySymbol
            )
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LeaseWizardEffect.NavigateBack -> onNavigateBack()
                is LeaseWizardEffect.LeaseFinalizedSuccess -> onNavigateBack()
                is LeaseWizardEffect.ShowToast -> {
                    // Handled if snackbar host is attached
                }
            }
        }
    }

    LeaseWizardContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaseWizardContent(
    uiState: LeaseWizardUiState,
    onIntent: (LeaseWizardIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Création de contrat de bail")
                        if (uiState.completionPercentage > 0) {
                            Text(
                                text = "Progression : ${uiState.completionPercentage}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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

            if (uiState.completionPercentage > 0) {
                LinearProgressIndicator(
                    progress = { uiState.completionPercentage / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = { onIntent(LeaseWizardIntent.DismissError) }) {
                            Text("Fermer", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            WizardStep.entries.forEach { step ->
                val stepStatus = uiState.stepStatuses[step] ?: WizardStepStatus.UPCOMING
                val isCurrent = uiState.currentStep == step
                val isCompleted = stepStatus == WizardStepStatus.COMPLETED
                val isSkipped = stepStatus == WizardStepStatus.SKIPPED

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onIntent(LeaseWizardIntent.SetExpandedStep(step)) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when {
                                    isCompleted -> {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    isSkipped -> {
                                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                            Text("Sautée", modifier = Modifier.padding(4.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    else -> {
                                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                            Text(step.stepNumber.toString(), modifier = Modifier.padding(4.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
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
                                imageVector = if (isCurrent) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(
                            visible = isCurrent,
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
                                            isInlineTenant = uiState.isInlineTenant,
                                            inlineFirstName = uiState.inlineFirstName,
                                            inlineLastName = uiState.inlineLastName,
                                            inlineEmail = uiState.inlineEmail,
                                            inlinePhone = uiState.inlinePhone,
                                            userQuery = uiState.userQuery,
                                            userResults = uiState.userResults,
                                            isSearchingUsers = uiState.isSearchingUsers,
                                            draftTenantId = uiState.draftTenantId,
                                            draftTenantName = uiState.draftTenantName,
                                            onInlineFirstNameChanged = { onIntent(LeaseWizardIntent.SetInlineFirstName(it)) },
                                            onInlineLastNameChanged = { onIntent(LeaseWizardIntent.SetInlineLastName(it)) },
                                            onInlineEmailChanged = { onIntent(LeaseWizardIntent.SetInlineEmail(it)) },
                                            onInlinePhoneChanged = { onIntent(LeaseWizardIntent.SetInlinePhone(it)) },
                                            onToggleInlineTenant = { onIntent(LeaseWizardIntent.ToggleInlineTenant(it)) },
                                            onUserQueryChanged = { onIntent(LeaseWizardIntent.SetUserQuery(it)) },
                                            onUserSelected = { onIntent(LeaseWizardIntent.SelectUser(it)) }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { onIntent(LeaseWizardIntent.SubmitTenantStep) },
                                            enabled = !uiState.isSavingStep,
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            if (uiState.isSavingStep) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Text("Valider & Continuer")
                                            }
                                        }
                                    }
                                    WizardStep.GUARANTOR -> {
                                        WizardGuarantorStep(
                                            existingGuarantors = uiState.existingGuarantors,
                                            selectedGuarantorId = uiState.selectedGuarantorId,
                                            isReusingGuarantor = uiState.isReusingGuarantor,
                                            guarantorLastName = uiState.guarantorLastName,
                                            guarantorFirstName = uiState.guarantorFirstName,
                                            guarantorPhone = uiState.guarantorPhone,
                                            guarantorMonthlySalary = uiState.guarantorMonthlySalary,
                                            guarantorRelationship = uiState.guarantorRelationship,
                                            guarantorJobTitle = uiState.guarantorJobTitle,
                                            guarantorEmployer = uiState.guarantorEmployer,
                                            currencySymbol = uiState.currencySymbol,
                                            onSelectExistingGuarantor = { onIntent(LeaseWizardIntent.SelectExistingGuarantor(it)) },
                                            onToggleReuseGuarantor = { onIntent(LeaseWizardIntent.ToggleReuseGuarantor(it)) },
                                            onGuarantorLastNameChanged = { onIntent(LeaseWizardIntent.SetGuarantorLastName(it)) },
                                            onGuarantorFirstNameChanged = { onIntent(LeaseWizardIntent.SetGuarantorFirstName(it)) },
                                            onGuarantorPhoneChanged = { onIntent(LeaseWizardIntent.SetGuarantorPhone(it)) },
                                            onGuarantorMonthlySalaryChanged = { onIntent(LeaseWizardIntent.SetGuarantorMonthlySalary(it)) },
                                            onGuarantorRelationshipChanged = { onIntent(LeaseWizardIntent.SetGuarantorRelationship(it)) },
                                            onGuarantorJobTitleChanged = { onIntent(LeaseWizardIntent.SetGuarantorJobTitle(it)) },
                                            onGuarantorEmployerChanged = { onIntent(LeaseWizardIntent.SetGuarantorEmployer(it)) },
                                            onSkip = { onIntent(LeaseWizardIntent.SkipGuarantorStep) }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { onIntent(LeaseWizardIntent.SubmitGuarantorStep) },
                                            enabled = !uiState.isSavingStep,
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            if (uiState.isSavingStep) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Text("Valider & Continuer")
                                            }
                                        }
                                    }
                                    WizardStep.UNIT -> {
                                        WizardUnitStep(
                                            residenceUnits = uiState.availableUnits,
                                            draftResidenceUnitId = uiState.draftResidenceUnitId,
                                            currencySymbol = uiState.currencySymbol,
                                            onUnitSelected = { onIntent(LeaseWizardIntent.SelectUnit(it)) }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { onIntent(LeaseWizardIntent.SubmitUnitStep) },
                                            enabled = !uiState.isSavingStep && uiState.draftResidenceUnitId.isNotBlank(),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            if (uiState.isSavingStep) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Text("Valider & Continuer")
                                            }
                                        }
                                    }
                                    WizardStep.FINANCIALS -> {
                                        WizardFinancialsStep(
                                            selectedResidenceUnit = uiState.selectedUnit,
                                            draftDepositAmount = uiState.draftDepositAmount,
                                            draftPaymentFrequency = uiState.draftPaymentFrequency,
                                            draftAdvanceMonths = uiState.draftAdvanceMonths,
                                            draftAdvancePaymentAmount = uiState.draftAdvancePaymentAmount,
                                            draftPaymentMethod = uiState.draftPaymentMethod,
                                            currencySymbol = uiState.currencySymbol,
                                            onDepositAmountChanged = { onIntent(LeaseWizardIntent.SetDepositAmount(it)) },
                                            onPaymentFrequencyChanged = { onIntent(LeaseWizardIntent.SetPaymentFrequency(it)) },
                                            onAdvanceMonthsChanged = { onIntent(LeaseWizardIntent.SetAdvanceMonths(it)) },
                                            onAdvancePaymentAmountChanged = { onIntent(LeaseWizardIntent.SetAdvancePaymentAmount(it)) },
                                            onPaymentMethodChanged = { onIntent(LeaseWizardIntent.SetPaymentMethod(it)) }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { onIntent(LeaseWizardIntent.SubmitFinancialsStep) },
                                            enabled = !uiState.isSavingStep,
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            if (uiState.isSavingStep) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Text("Valider & Continuer")
                                            }
                                        }
                                    }
                                    WizardStep.TIMELINE -> {
                                        WizardTimelineStep(
                                            draftStartDate = uiState.draftStartDate,
                                            draftEndDate = uiState.draftEndDate,
                                            onStartDateChanged = { onIntent(LeaseWizardIntent.SetStartDate(it)) },
                                            onEndDateChanged = { onIntent(LeaseWizardIntent.SetEndDate(it)) }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { onIntent(LeaseWizardIntent.SubmitTimelineStep) },
                                            enabled = !uiState.isSavingStep,
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            if (uiState.isSavingStep) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Text("Valider & Continuer")
                                            }
                                        }
                                    }
                                    WizardStep.INVENTORY -> {
                                        WizardInventoryStep(
                                            onSkip = {
                                                onIntent(LeaseWizardIntent.SkipInventoryStep)
                                            }
                                        )
                                    }
                                    WizardStep.SIGNATURE -> {
                                        WizardSignatureStep(canSign = uiState.canSign)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { onIntent(LeaseWizardIntent.FinalizeSignature) },
                                            enabled = !uiState.isSavingStep && uiState.canSign,
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            if (uiState.isSavingStep) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
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
