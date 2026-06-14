package com.resid.manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.resid.manager.dto.*
import com.resid.manager.network.ApiClient
import com.resid.manager.viewmodel.LoginViewModel
import io.ktor.client.request.*
import io.ktor.client.call.body
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.launch

@Composable
fun FinancesPage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activeResidence = uiState.selectedResidenceContext
    val isAuthorized = activeResidence != null && (activeResidence.userRoleInResidence == UserRole.ADMIN || activeResidence.userRoleInResidence == UserRole.RESIDENCE_MANAGER)

    var transactions by remember { mutableStateOf<List<FinanceTransactionDto>>(emptyList()) }
    var isLoadingList by remember { mutableStateOf(false) }

    // Saisie Rapide Form States
    var formCategory by remember { mutableStateOf("Cleaning") }
    var formAmountText by remember { mutableStateOf("") }
    var formDescription by remember { mutableStateOf("") }
    var formDate by remember { mutableStateOf("2025-02-17") }
    var isSubmittingExpense by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var formSuccess by remember { mutableStateOf(false) }

    // Search & Filter States
    var filterType by remember { mutableStateOf("ALL") } // "ALL", "INCOME", "EXPENSE"
    var filterCategory by remember { mutableStateOf("ALL") }
    var filterStartDateText by remember { mutableStateOf("") }
    var filterEndDateText by remember { mutableStateOf("") }
    var filterQueryText by remember { mutableStateOf("") }

    // Traceability Dialog state
    var traceabilityDialogPayload by remember { mutableStateOf<Pair<String, String>?>(null) } // Type to ID

    val coroutineScope = rememberCoroutineScope()

    // Helper function to fetch transactions from server
    fun fetchTransactions() {
        if (activeResidence == null) return
        isLoadingList = true
        coroutineScope.launch {
            try {
                val url = "${ApiClient.BASE_URL}/api/residences/${activeResidence.residenceId}/transactions?" +
                        "type=${if (filterType != "ALL") filterType else ""}&" +
                        "category=${if (filterCategory != "ALL") filterCategory else ""}&" +
                        "start_date=$filterStartDateText&" +
                        "end_date=$filterEndDateText&" +
                        "q=$filterQueryText"
                
                val response = ApiClient.httpClient.get(url) {
                    header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                }
                if (response.status == io.ktor.http.HttpStatusCode.OK) {
                    transactions = response.body()
                }
            } catch (e: Exception) {}
            isLoadingList = false
        }
    }

    LaunchedEffect(activeResidence, filterType, filterCategory, filterStartDateText, filterEndDateText, filterQueryText) {
        fetchTransactions()
    }

    val validCategories = listOf(
        "Cleaning" to "Nettoyage / Entretien",
        "Fuel" to "Carburant Générateur",
        "Security" to "Sécurité",
        "Maintenance" to "Maintenance Technique",
        "Taxes" to "Impôts & Taxes",
        "Other" to "Autres Charges"
    )

    Row(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Zone A: Formulaire de Saisie Rapide (Left Column, fixed width)
        Card(
            modifier = Modifier.width(360.dp).fillMaxHeight(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Saisie de Dépense Opérationnelle", 
                    style = MaterialTheme.typography.titleLarge, 
                    color = Color(0xFF006948)
                )
                Text(
                    text = "Enregistrez manuellement une charge opérationnelle payée directement.", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.outline
                )
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                if (isAuthorized) {
                    // Category selector dropdown
                    Text("Catégorie de la Dépense * :", style = MaterialTheme.typography.titleSmall)
                    var expandedDropdown by remember { mutableStateOf(false) }
                    val selectedLabel = validCategories.firstOrNull { it.first == formCategory }?.second ?: formCategory
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedLabel)
                        }
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.width(312.dp)
                        ) {
                            validCategories.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        formCategory = key
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Amount input field
                    OutlinedTextField(
                        value = formAmountText,
                        onValueChange = { formAmountText = it },
                        label = { Text("Montant (XOF) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Date input field
                    OutlinedTextField(
                        value = formDate,
                        onValueChange = { formDate = it },
                        label = { Text("Date d'opération (AAAA-MM-JJ) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Description text input
                    OutlinedTextField(
                        value = formDescription,
                        onValueChange = { formDescription = it },
                        label = { Text("Description explicite de l'achat *") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    if (formError != null) {
                        Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    if (formSuccess) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F7F0)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF006948))
                                Text("Dépense enregistrée avec succès !", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF006948))
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val amount = formAmountText.toDoubleOrNull()
                            if (amount == null || amount <= 0.0 || formDescription.isBlank() || formDate.isBlank()) {
                                formError = "Veuillez remplir tous les champs obligatoires avec des valeurs valides."
                                formSuccess = false
                                return@Button
                            }

                            isSubmittingExpense = true
                            formError = null
                            formSuccess = false

                            coroutineScope.launch {
                                try {
                                    val req = ExpenseRecordRequest(
                                        category = formCategory,
                                        amount = amount,
                                        description = formDescription,
                                        transactionDate = formDate
                                    )
                                    val resp = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/residences/${activeResidence?.residenceId}/transactions") {
                                        contentType(ContentType.Application.Json)
                                        header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                        setBody(req)
                                    }
                                    if (resp.status == io.ktor.http.HttpStatusCode.Created || resp.status == io.ktor.http.HttpStatusCode.OK) {
                                        fetchTransactions()
                                        // Reset fields
                                        formAmountText = ""
                                        formDescription = ""
                                        formSuccess = true
                                    } else {
                                        val err = resp.body<ErrorResponse>()
                                        formError = err.message
                                    }
                                } catch (e: Exception) {
                                    formError = "Erreur de connexion : ${e.message}"
                                }
                                isSubmittingExpense = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        enabled = !isSubmittingExpense
                    ) {
                        if (isSubmittingExpense) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Enregistrer la dépense")
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Accès réservé aux administrateurs.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        // Zone B: Le Grand Livre des Transactions (Right Column, expands)
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filters row
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Recherche & Filtres Grand Livre", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flow toggles
                        Row(modifier = Modifier.weight(1f)) {
                            listOf("ALL" to "Tous", "INCOME" to "Revenus (+)", "EXPENSE" to "Dépenses (-)").forEach { (key, label) ->
                                FilterChip(
                                    selected = filterType == key,
                                    onClick = { filterType = key },
                                    label = { Text(label) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }

                        // Keyword description search
                        OutlinedTextField(
                            value = filterQueryText,
                            onValueChange = { filterQueryText = it },
                            label = { Text("Rechercher dans description...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            // Ledger ledger list
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                if (isLoadingList) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF006948))
                    }
                } else if (transactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucune transaction trouvée pour ces filtres.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                        Text("Grand Livre Chronologique des Flux", style = MaterialTheme.typography.titleMedium, color = Color(0xFF006948))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Header row of the ledger
                        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                            Text("Date", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text("Catégorie", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text("Description & Liens", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(2f))
                            Text("Montant", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1.2f))
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                            transactions.forEach { tx ->
                                val isIncome = tx.type == "INCOME"
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {}
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(tx.transactionDate, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(
                                        text = when (tx.category) {
                                            "Cleaning" -> "Nettoyage"
                                            "Fuel" -> "Carburant"
                                            "Security" -> "Sécurité"
                                            "Maintenance" -> "Maintenance"
                                            "Taxes" -> "Impôts"
                                            "Deposit" -> "Caution Recue"
                                            "Rent" -> "Loyer Recu"
                                            "Electricity" -> "Électricité"
                                            else -> tx.category
                                        }, 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(
                                        modifier = Modifier.weight(2f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(tx.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        
                                        // Traceability Link Badge
                                        if (tx.relatedEntityType != null && tx.relatedEntityId != null) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E7FF)),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.clickable {
                                                    traceabilityDialogPayload = tx.relatedEntityType to tx.relatedEntityId
                                                }
                                            ) {
                                                Text(
                                                    text = "Lien ${tx.relatedEntityType}", 
                                                    style = MaterialTheme.typography.labelSmall, 
                                                    color = Color(0xFF1E3A8A),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (isIncome) "+ ${tx.amount} XOF" else "- ${tx.amount} XOF",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isIncome) Color(0xFF006948) else Color(0xFFBA1A1A),
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Traceability Modal Dialog
    if (traceabilityDialogPayload != null) {
        val (type, id) = traceabilityDialogPayload!!
        AlertDialog(
            onDismissRequest = { traceabilityDialogPayload = null },
            title = { Text("Traçabilité de la Transaction", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cette transaction a été générée automatiquement par un processus métier lié.")
                    Text("• Entité d'Origine : $type")
                    Text("• Identifiant Technique (UUID) : $id")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (type.uppercase()) {
                            "BAIL" -> "Ce versement correspond à la régulation de la caution ou d'un loyer d'avance lié à un contrat de bail actif."
                            "ELECTRICITY_STATEMENT" -> "Cette écriture correspond au relevé d'indexation d'électricité saisi par un agent."
                            "TICKET" -> "Cette dépense correspond à la résolution comptable d'un incident de maintenance résolu par le gestionnaire."
                            else -> "Écriture automatisée du système."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(onClick = { traceabilityDialogPayload = null }) {
                    Text("Fermer")
                }
            }
        )
    }
}
