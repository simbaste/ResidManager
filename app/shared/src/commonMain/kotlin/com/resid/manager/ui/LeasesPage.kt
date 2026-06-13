package com.resid.manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.*
import com.resid.manager.network.ApiClient
import com.resid.manager.viewmodel.LoginViewModel
import io.ktor.client.request.*
import io.ktor.client.call.body
import kotlinx.coroutines.launch

@Composable
fun LeasesPage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activeResidence = uiState.selectedResidenceContext
    val isAuthorized = activeResidence != null && (activeResidence.userRoleInResidence == UserRole.ADMIN || activeResidence.userRoleInResidence == UserRole.RESIDENCE_MANAGER)

    var showWizard by remember { mutableStateOf(false) }
    var selectedLeaseForDetail by remember { mutableStateOf<LeaseDto?>(null) }

    if (selectedLeaseForDetail != null) {
        val lease = selectedLeaseForDetail!!
        val matchedLogement = uiState.logements.firstOrNull { it.id == lease.logementId }
        val matchedLogementName = matchedLogement?.name ?: "Logement ${lease.logementId.take(5)}"
        
        var paymentAmountText by remember { mutableStateOf("") }
        var localError by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { selectedLeaseForDetail = null }) {
                        Text("← Retour à la liste")
                    }
                    Text(text = "Détails du Contrat de Bail", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                }
                Badge(
                    containerColor = when (lease.status) {
                        LeaseStatus.SIGNED_ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                        LeaseStatus.PENDING_SIGNATURE -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(text = lease.status.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(8.dp))
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Column: Lease Details
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Fiche d'Informations", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider()

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Unité rattachée : $matchedLogementName", style = MaterialTheme.typography.bodyLarge)
                            Text("Étage : ${matchedLogement?.floor ?: "Non spécifié"}", style = MaterialTheme.typography.bodyMedium)
                            Text("Type d'unité : ${matchedLogement?.type ?: "Non spécifié"}", style = MaterialTheme.typography.bodyMedium)
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            val matchedTenant = uiState.members.firstOrNull { it.userId == lease.tenantId }
                            if (matchedTenant != null) {
                                Text("Locataire : ${matchedTenant.firstName} ${matchedTenant.lastName}", style = MaterialTheme.typography.bodyLarge)
                                Text("Email : ${matchedTenant.email}", style = MaterialTheme.typography.bodyMedium)
                                Text("Téléphone : ${matchedTenant.phone ?: "Non spécifié"}", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Text("Locataire (ID) : ${lease.tenantId}", style = MaterialTheme.typography.bodyLarge)
                            }
                            
                            Text("Date de début : ${lease.startDate}", style = MaterialTheme.typography.bodyMedium)
                            Text("Date de fin : ${lease.endDate}", style = MaterialTheme.typography.bodyMedium)
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Text("Loyer de base : ${lease.monthlyRentAtSign} XOF / mois", style = MaterialTheme.typography.bodyLarge)
                            Text("Caution requise : ${lease.depositAmount} XOF", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                // Right Column: Payments & Signing Actions
                Card(
                    modifier = Modifier.weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Actions d'Administration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider()

                        // Display payment progress
                        val isFullyPaid = lease.status == LeaseStatus.PENDING_SIGNATURE || lease.status == LeaseStatus.SIGNED_ACTIVE
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFullyPaid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isFullyPaid) "✓ CAUTION ENTIÈREMENT PAYÉE" else "⚠ ATTENTE DE PAIEMENT DE LA CAUTION",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isFullyPaid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Montant de la caution : ${lease.depositAmount} XOF",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isFullyPaid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        // Action 1: Add Payment
                        if (!isFullyPaid && isAuthorized) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Saisir un versement pour la caution :", style = MaterialTheme.typography.bodyMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = paymentAmountText,
                                        onValueChange = { paymentAmountText = it },
                                        label = { Text("Montant du versement (XOF)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            val amount = paymentAmountText.toDoubleOrNull()
                                            if (amount == null || amount <= 0.0) {
                                                localError = "Veuillez saisir un montant valide supérieur à 0."
                                                return@Button
                                            }
                                            viewModel.recordLeasePayment(lease.id, amount) { res ->
                                                res.onSuccess {
                                                    selectedLeaseForDetail = it
                                                    paymentAmountText = ""
                                                    localError = null
                                                }.onFailure { ex ->
                                                    localError = ex.message
                                                }
                                            }
                                        },
                                        enabled = !uiState.isLoading
                                    ) {
                                        Text("Enregistrer")
                                    }
                                }
                            }
                        }

                        // Action 2: Sign and Activate Lease
                        if (lease.status == LeaseStatus.PENDING_SIGNATURE && isAuthorized) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                Text("Le dépôt de garantie a été entièrement réglé. Vous pouvez maintenant signer et activer le contrat de bail :", style = MaterialTheme.typography.bodyMedium)
                                Button(
                                    onClick = {
                                        viewModel.updateLeaseStatus(lease.id, LeaseStatus.SIGNED_ACTIVE) { res ->
                                            res.onSuccess {
                                                selectedLeaseForDetail = it
                                                localError = null
                                            }.onFailure { ex ->
                                                localError = ex.message
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("✎ Signer et Activer le Bail")
                                }
                            }
                        }

                        val error = localError ?: uiState.errorMessage
                        if (error != null) {
                            Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gestion des Contrats de Bail (Baux)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (isAuthorized) {
                    Button(onClick = { showWizard = true }) {
                        Text("+ Nouveau contrat de bail")
                    }
                }
            }

            if (uiState.leases.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 500.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Aucun contrat de bail",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Aucun bail n'a été enregistré pour le moment. Cliquez sur le bouton ci-dessus pour initier l'assistant de création étape par étape.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                // Leases Grid/List
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        uiState.leases.forEach { lease ->
                            val matchedLogement = uiState.logements.firstOrNull { it.id == lease.logementId }?.name ?: "Logement ${lease.logementId.take(5)}"
                            
                            Card(
                                modifier = Modifier.width(300.dp).wrapContentHeight().clickable { selectedLeaseForDetail = lease },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = matchedLogement, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Badge(
                                            containerColor = when (lease.status) {
                                                LeaseStatus.SIGNED_ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                                                LeaseStatus.PENDING_SIGNATURE -> MaterialTheme.colorScheme.tertiaryContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ) {
                                            Text(text = lease.status.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(4.dp))
                                        }
                                    }

                                    HorizontalDivider()

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(text = "Du : ${lease.startDate}", style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "Au : ${lease.endDate}", style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "Loyer mensuel : ${lease.monthlyRentAtSign} XOF", style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "Caution : ${lease.depositAmount} XOF", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWizard) {
        LeaseWizardDialog(
            viewModel = viewModel,
            onDismiss = { showWizard = false }
        )
    }
}

enum class WizardStep { TENANT, UNIT, FINANCIALS, TIMELINE }

@Composable
fun LeaseWizardDialog(
    viewModel: LoginViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableStateOf(WizardStep.TENANT) }

    // Unified draft form state to gather all inputs
    var draftTenantId by remember { mutableStateOf<String?>(null) }
    var draftTenantName by remember { mutableStateOf("") }
    var isInlineTenant by remember { mutableStateOf(false) }
    var inlineFirstName by remember { mutableStateOf("") }
    var inlineLastName by remember { mutableStateOf("") }
    var inlineEmail by remember { mutableStateOf("") }
    var inlinePhone by remember { mutableStateOf("") }

    var draftLogementId by remember { mutableStateOf("") }
    var draftDepositAmount by remember { mutableStateOf("0.0") }
    var draftPaymentFrequency by remember { mutableStateOf("MONTHLY") }
    var draftStartDate by remember { mutableStateOf("") }
    var draftEndDate by remember { mutableStateOf("") }
    var draftAdvanceMonths by remember { mutableStateOf("12") }
    var draftAdvancePaymentAmount by remember { mutableStateOf("0.0") }

    // User lookup variables
    var userQuery by remember { mutableStateOf("") }
    var userResults by remember { mutableStateOf<List<UserSearchDto>>(emptyList()) }
    var isSearchingUsers by remember { mutableStateOf(false) }

    // Search Job Coroutine
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
                TextButton(onClick = onDismiss) {
                    Text("✕", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).heightIn(max = 450.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step Progress tracker
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
                        Text("Étape 1 sur 4 : Sélection du Locataire", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        
                        if (isInlineTenant) {
                            // Inline creation form
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Nouveau locataire inline", style = MaterialTheme.typography.titleSmall)
                                OutlinedTextField(value = inlineFirstName, onValueChange = { inlineFirstName = it }, label = { Text("Prénom *") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = inlineLastName, onValueChange = { inlineLastName = it }, label = { Text("Nom *") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = inlineEmail, onValueChange = { inlineEmail = it }, label = { Text("Email *") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = inlinePhone, onValueChange = { inlinePhone = it }, label = { Text("Téléphone") }, modifier = Modifier.fillMaxWidth())
                                
                                Button(
                                    onClick = { isInlineTenant = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("Retour à la recherche")
                                }
                            }
                        } else {
                            // Search as you type
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = userQuery,
                                    onValueChange = { q ->
                                        userQuery = q
                                        // Real-time API query
                                        if (q.length >= 2) {
                                            isSearchingUsers = true
                                            coroutineScope.launch {
                                                try {
                                                    val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/users/search") {
                                                        parameter("q", q)
                                                        header(io.ktor.http.HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                                    }
                                                    if (response.status == io.ktor.http.HttpStatusCode.OK) {
                                                        userResults = response.body<List<UserSearchDto>>()
                                                    }
                                                } catch (e: Exception) {}
                                                isSearchingUsers = false
                                            }
                                        } else {
                                            userResults = emptyList()
                                        }
                                        userQuery = q
                                    },
                                    label = { Text("Saisissez le nom d'un locataire...") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (isSearchingUsers) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }

                                if (userResults.isNotEmpty()) {
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column {
                                            userResults.forEach { user ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            draftTenantId = user.id
                                                            draftTenantName = user.name
                                                            userResults = emptyList()
                                                            userQuery = user.name
                                                        }
                                                        .padding(12.dp)
                                                ) {
                                                    Text(text = "${user.name} (${user.email})")
                                                }
                                            }
                                        }
                                    }
                                }

                                if (draftTenantId != null) {
                                    Text("Sélectionné : $draftTenantName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }

                                Button(
                                    onClick = { 
                                        isInlineTenant = true 
                                        draftTenantId = null
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("+ Ajouter un nouveau locataire")
                                }
                            }
                        }
                    }

                    WizardStep.UNIT -> {
                        Text("Étape 2 sur 4 : Sélection du Logement", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        
                        val availableLogements = uiState.logements.filter { it.status == "AVAILABLE" }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (availableLogements.isEmpty()) {
                                Text("Aucun logement de type AVAILABLE (libre) n'est disponible dans cette résidence.")
                            } else {
                                Text("Sélectionnez l'unité libre à attribuer :")
                                
                                availableLogements.forEach { logement ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { draftLogementId = logement.id }
                                            .background(if (draftLogementId == logement.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(text = logement.name, style = MaterialTheme.typography.titleSmall)
                                            Text(text = "Étage : ${logement.floor} | Loyer : ${logement.nominalRent} XOF")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    WizardStep.FINANCIALS -> {
                        Text("Étape 3 sur 4 : Conditions Financières", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        
                        val selectedLogement = uiState.logements.firstOrNull { it.id == draftLogementId }
                        val isAnnual = draftPaymentFrequency == "ANNUAL"

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = draftDepositAmount,
                                onValueChange = { draftDepositAmount = it },
                                label = { Text("Montant du dépôt de garantie (Caution) *") },
                                isError = (draftDepositAmount.toDoubleOrNull() ?: 0.0) < 0.0,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Fréquence de paiement :")
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = { draftPaymentFrequency = "MONTHLY" },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (draftPaymentFrequency == "MONTHLY") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("Mensuelle (MONTHLY)")
                                }
                                Button(
                                    onClick = { draftPaymentFrequency = "ANNUAL" },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (draftPaymentFrequency == "ANNUAL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("Annuelle (ANNUAL)")
                                }
                            }

                            // Show Annual specific inputs if frequency is ANNUAL
                            if (isAnnual) {
                                OutlinedTextField(
                                    value = draftAdvanceMonths,
                                    onValueChange = { draftAdvanceMonths = it },
                                    label = { Text("Nombre de mois d'avance payés (Défaut : 12) *") },
                                    isError = (draftAdvanceMonths.toIntOrNull() ?: 0) <= 0,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = draftAdvancePaymentAmount,
                                    onValueChange = { draftAdvancePaymentAmount = it },
                                    label = { Text("Montant du versement d'avance fait immédiatement (XOF) *") },
                                    isError = (draftAdvancePaymentAmount.toDoubleOrNull() ?: 0.0) < 0.0,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (selectedLogement != null) {
                                val rent = selectedLogement.nominalRent
                                val charges = selectedLogement.serviceCharges
                                val deposit = draftDepositAmount.toDoubleOrNull() ?: 0.0
                                
                                val months = if (isAnnual) (draftAdvanceMonths.toIntOrNull() ?: 12) else 1
                                val firstRent = months * (rent + charges)
                                
                                val totalToPay = deposit + firstRent
                                val advancePaid = if (isAnnual) (draftAdvancePaymentAmount.toDoubleOrNull() ?: 0.0) else 0.0
                                val remainingToPay = totalToPay - advancePaid

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Récapitulatif Financier de l'Entrée :", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        HorizontalDivider()
                                        
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Loyer de base :", style = MaterialTheme.typography.bodyMedium)
                                            Text("$rent XOF / mois", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Charges fixes d'entretien :", style = MaterialTheme.typography.bodyMedium)
                                            Text("$charges XOF / mois", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Dépôt de garantie (Caution) :", style = MaterialTheme.typography.bodyMedium)
                                            Text("$deposit XOF", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(
                                                text = if (!isAnnual) "Premier loyer échu (1 mois) :" else "Versement d'avance échu ($months mois) :",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text("$firstRent XOF", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        
                                        if (isAnnual && advancePaid > 0.0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Acompte versé immédiatement :", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                                Text("- $advancePaid XOF", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }

                                        HorizontalDivider()
                                        
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("TOTAL À PAYER À L'ENTRÉE :", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                            Text("$totalToPay XOF", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("RESTE À PAYER :", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                            Text("$remainingToPay XOF", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    WizardStep.TIMELINE -> {
                        Text("Étape 4 sur 4 : Calendrier & Durée", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = draftStartDate,
                                onValueChange = { draftStartDate = it },
                                label = { Text("Date de début (AAAA-MM-JJ) *") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = draftEndDate,
                                onValueChange = { draftEndDate = it },
                                label = { Text("Date de fin (AAAA-MM-JJ) *") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Safe duration calculation using platform-agnostic remember (compiles on both JVM and JS targets)
                            val computedDuration = remember(draftStartDate, draftEndDate) {
                                try {
                                    val startParts = draftStartDate.split("-")
                                    val endParts = draftEndDate.split("-")
                                    val startYear = startParts[0].toInt()
                                    val startMonth = startParts[1].toInt()
                                    val endYear = endParts[0].toInt()
                                    val endMonth = endParts[1].toInt()
                                    val totalMonths = (endYear - startYear) * 12 + (endMonth - startMonth)
                                    if (totalMonths > 0) totalMonths else -1
                                } catch (ex: Exception) {
                                    -1
                                }
                            }

                            if (computedDuration > 0) {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                    Text(text = "Durée calculée du contrat : $computedDuration mois", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }

                val error = uiState.errorMessage
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            if (currentStep == WizardStep.TIMELINE) {
                Button(
                    onClick = {
                        val rentVal = uiState.logements.firstOrNull { it.id == draftLogementId }?.nominalRent ?: 0.0
                        val req = LeaseCreateRequest(
                            tenantId = draftTenantId,
                            inlineTenant = if (isInlineTenant) InlineTenantCreateRequest(inlineFirstName, inlineLastName, inlineEmail, inlinePhone.ifBlank { null }) else null,
                            logementId = draftLogementId,
                            depositAmount = draftDepositAmount.toDoubleOrNull() ?: 0.0,
                            paymentFrequency = draftPaymentFrequency,
                            startDate = draftStartDate,
                            endDate = draftEndDate,
                            monthlyRentAtSign = rentVal,
                            advanceMonths = if (draftPaymentFrequency == "ANNUAL") (draftAdvanceMonths.toIntOrNull() ?: 12) else 1,
                            advancePaymentAmount = if (draftPaymentFrequency == "ANNUAL") (draftAdvancePaymentAmount.toDoubleOrNull() ?: 0.0) else 0.0
                        )
                        viewModel.createLease(draftLogementId, req) {
                            onDismiss()
                        }
                    },
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
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
