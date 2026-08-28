package com.resid.manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ElectricityStatementCreateRequest
import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.dto.ElectricityStatementUpdateRequest
import com.resid.manager.dto.ElectricityStatusDto
import com.resid.manager.dto.ErrorResponse
import com.resid.manager.dto.LeaseStatusDto
import com.resid.manager.dto.UserRole
import com.resid.manager.network.ApiClient
import com.resid.manager.viewmodel.LoginViewModel
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.launch

@Composable
fun ElectricityPage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activeResidence = uiState.selectedResidenceContext
    val isAuthorized = activeResidence != null && (activeResidence.userRoleInResidence == UserRole.ADMIN || activeResidence.userRoleInResidence == UserRole.MANAGER)

    var statements by remember { mutableStateOf<List<ElectricityStatementDto>>(emptyList()) }
    var isLoadingList by remember { mutableStateOf(false) }

    // Filter states
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "UNPAID", "PAID"
    var selectedResidenceUnitFilterId by remember { mutableStateOf("") }
    var floorFilterText by remember { mutableStateOf("") }
    var tenantFilterText by remember { mutableStateOf("") }

    // Form / dialog states
    var showFormDialog by remember { mutableStateOf(false) }
    var formSelectedResidenceUnitId by remember { mutableStateOf("") }
    var formPreviousIndex by remember { mutableStateOf<Double?>(null) }
    var formNewIndexText by remember { mutableStateOf("") }
    var formKWhPriceText by remember { mutableStateOf("120.0") }
    var formStatementDate by remember { mutableStateOf("2025-02-17") }
    
    var isLoadingPreviousIndex by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Selection checkbox map for Eco-Print export
    val selectedStatementIds = remember { mutableStateMapOf<String, Boolean>() }
    var showPaymentConfirmationForStatement by remember { mutableStateOf<ElectricityStatementDto?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    // Helper function to fetch statements
    fun fetchStatements() {
        if (activeResidence == null) return
        isLoadingList = true
        coroutineScope.launch {
            try {
                val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/residences/${activeResidence.residenceId}/electricity/statements") {
                    header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                }
                if (response.status == io.ktor.http.HttpStatusCode.OK) {
                    statements = response.body()
                }
            } catch (e: Exception) {
                // Graceful error handling
            }
            isLoadingList = false
        }
    }

    // Trigger loading on startup or active residence change
    LaunchedEffect(activeResidence) {
        fetchStatements()
    }

    // Client-side real-time filtering for maximum mobile-first snappiness
    val filteredStatements = remember(statements, statusFilter, selectedResidenceUnitFilterId, floorFilterText, tenantFilterText, uiState.leases, uiState.members) {
        statements.filter { stmt ->
            // 1. Status Filter
            val matchStatus = when (statusFilter) {
                "PAID" -> stmt.status == ElectricityStatusDto.PAID
                "UNPAID" -> stmt.status == ElectricityStatusDto.UNPAID
                else -> true
            }

            // 2. ResidenceUnit Filter
            val matchResidenceUnit = if (selectedResidenceUnitFilterId.isNotEmpty()) stmt.unitId == selectedResidenceUnitFilterId else true

            // 3. Floor Filter
            val matchedResidenceUnitObj = uiState.residenceUnits.firstOrNull { it.id == stmt.unitId }
            val matchFloor = if (floorFilterText.isNotEmpty() && matchedResidenceUnitObj != null) {
                matchedResidenceUnitObj.floor.contains(floorFilterText, ignoreCase = true)
            } else true

            // 4. Tenant Name Filter (join via active lease)
            val matchTenant = if (tenantFilterText.isNotEmpty()) {
                val activeLease = uiState.leases.firstOrNull { it.residenceUnitId == stmt.unitId && it.status == LeaseStatusDto.SIGNED_ACTIVE }
                val tenant = activeLease?.let { lease -> uiState.members.firstOrNull { it.userId == lease.tenantId } }
                val fullName = "${tenant?.firstName ?: ""} ${tenant?.lastName ?: ""}"
                fullName.contains(tenantFilterText, ignoreCase = true)
            } else true

            matchStatus && matchResidenceUnit && matchFloor && matchTenant
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Page Header with Title & Add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Facturation Électricité",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color(0xFF006948)
                )
                Text(
                    text = "Saisissez les index, facturez automatiquement et générez les reçus Eco-Print 2x2.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Eco-Print Export Button
                val checkedIds = selectedStatementIds.filter { it.value }.keys.toList()
                val exportUrl = "${ApiClient.BASE_URL}/api/residences/${activeResidence?.residenceId}/electricity/export-pdf?ids=${checkedIds.joinToString(",")}"
                
                Button(
                    onClick = {
                        // Open export URL directly to trigger PDF download / view via platform browser
                        uriHandler.openUri(exportUrl)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Text(if (checkedIds.isEmpty()) "Eco-Print (Derniers 4)" else "Eco-Print (${checkedIds.size} sélec.)", color = Color.White)
                    }
                }

                if (isAuthorized) {
                    Button(
                        onClick = { 
                            formStatementDate = "2025-02-17"
                            formKWhPriceText = "120.0"
                            showFormDialog = true 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Text("Nouveau Relevé", color = Color.White)
                        }
                    }
                }
            }
        }

        // Search & Filters panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Recherche & Filtres Cumulatifs en Temps Réel", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Filter
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Statut", style = MaterialTheme.typography.labelMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            listOf("ALL" to "Tous", "UNPAID" to "Impayés", "PAID" to "Payés").forEach { (key, label) ->
                                FilterChip(
                                    selected = statusFilter == key,
                                    onClick = { statusFilter = key },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    // Floor filter input
                    OutlinedTextField(
                        value = floorFilterText,
                        onValueChange = { floorFilterText = it },
                        label = { Text("Filtre par étage") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // Tenant filter input
                    OutlinedTextField(
                        value = tenantFilterText,
                        onValueChange = { tenantFilterText = it },
                        label = { Text("Nom du locataire") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        }

        // Statements List
        if (isLoadingList) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF006948))
            }
        } else if (filteredStatements.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Aucun relevé d'électricité ne correspond à vos filtres.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(filteredStatements) { stmt ->
                    val matchedResidenceUnit = uiState.residenceUnits.firstOrNull { it.id == stmt.unitId }
                    val matchedResidenceUnitName = matchedResidenceUnit?.name ?: "Logement ${stmt.unitId.take(5)}"
                    val isChecked = selectedStatementIds[stmt.id] ?: false
                    val currencySymbol = activeResidence?.currencySymbol ?: "XOF"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { selectedStatementIds[stmt.id] = it ?: false }
                                    )
                                    Text(text = matchedResidenceUnitName, style = MaterialTheme.typography.titleMedium, color = Color(0xFF006948))
                                }

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (stmt.status == ElectricityStatusDto.PAID) Color(0xFFE6F7F0) else Color(0xFFFDE8E8)
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (stmt.status == ElectricityStatusDto.PAID) "PAYÉ" else "IMPAYÉ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (stmt.status == ElectricityStatusDto.PAID) Color(0xFF006948) else Color(0xFFBA1A1A),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Période / Date :", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(stmt.statementDate, style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Ancien Index :", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    Text("${stmt.previousIndex} kWh", style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Nouveau Index :", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    Text("${stmt.newIndex} kWh", style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Consommation :", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    Text("${stmt.newIndex - stmt.previousIndex} kWh", style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tarif appliqué :", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    Text("${stmt.kWhPriceApplied} $currencySymbol / kWh", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("MONTANT DÛ :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("${stmt.amountDue} $currencySymbol", style = MaterialTheme.typography.titleLarge, color = Color(0xFF006948))
                                }

                                if (stmt.status == ElectricityStatusDto.UNPAID && isAuthorized) {
                                    Button(
                                        onClick = { showPaymentConfirmationForStatement = stmt },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text("Marquer comme Payé")
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

    // Input Form Dialog (Mobile-First responsive modal)
    if (showFormDialog) {
        var residenceUnitSearchQuery by remember { mutableStateOf("") }
        var selectedResidenceUnitName by remember { mutableStateOf("") }

        val filteredResidenceUnits = remember(uiState.residenceUnits, residenceUnitSearchQuery, selectedResidenceUnitName) {
            if (residenceUnitSearchQuery.isBlank() || residenceUnitSearchQuery == selectedResidenceUnitName) {
                emptyList()
            } else {
                uiState.residenceUnits.filter {
                    it.name.contains(residenceUnitSearchQuery, ignoreCase = true) ||
                    it.floor.contains(residenceUnitSearchQuery, ignoreCase = true)
                }
            }
        }

        val calculatedOldIndex = formPreviousIndex ?: 0.0
        val calculatedNewIndex = formNewIndexText.toDoubleOrNull() ?: calculatedOldIndex
        val calculatedPrice = formKWhPriceText.toDoubleOrNull() ?: 0.0
        val calculatedAmountDue = maxOf(0.0, (calculatedNewIndex - calculatedOldIndex) * calculatedPrice)

        AlertDialog(
            onDismissRequest = { showFormDialog = false },
            title = { Text("Enregistrer un Relevé d'Électricité", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier.widthIn(max = 500.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Rechercher l'unité concernée * :", style = MaterialTheme.typography.titleSmall)
                    
                    // Compact autocomplete search input
                    OutlinedTextField(
                        value = residenceUnitSearchQuery,
                        onValueChange = { residenceUnitSearchQuery = it },
                        label = { Text("Saisissez le nom ou l'étage...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Results dropdown list
                    if (filteredResidenceUnits.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.heightIn(max = 140.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                filteredResidenceUnits.forEach { residenceUnit ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                formSelectedResidenceUnitId = residenceUnit.id
                                                selectedResidenceUnitName = residenceUnit.name
                                                residenceUnitSearchQuery = residenceUnit.name // collapses list
                                                
                                                // Dynamically fetch previous index for read-only display!
                                                isLoadingPreviousIndex = true
                                                coroutineScope.launch {
                                                    try {
                                                        val resp = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/logements/${residenceUnit.id}/electricity/previous") {
                                                            header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                                        }
                                                        if (resp.status == io.ktor.http.HttpStatusCode.OK) {
                                                            val bodyMap = resp.body<Map<String, Double>>()
                                                            formPreviousIndex = bodyMap["previousIndex"]
                                                        }
                                                    } catch (e: Exception) {}
                                                    isLoadingPreviousIndex = false
                                                }
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(residenceUnit.name, style = MaterialTheme.typography.titleSmall)
                                            Text("Étage : ${residenceUnit.floor} | Type : ${residenceUnit.type}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selected residenceUnit feedback card
                    if (formSelectedResidenceUnitId.isNotEmpty() && selectedResidenceUnitName.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF006948))
                                Column {
                                    Text("Unité sélectionnée avec succès :", style = MaterialTheme.typography.labelSmall, color = Color(0xFF006948))
                                    Text(selectedResidenceUnitName, style = MaterialTheme.typography.titleSmall, color = Color(0xFF006948))
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Locked Read-only previous index field
                    if (formSelectedResidenceUnitId.isNotEmpty()) {
                        if (isLoadingPreviousIndex) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                        } else {
                            OutlinedTextField(
                                value = "${formPreviousIndex ?: 0.0} kWh",
                                onValueChange = {},
                                label = { Text("Index précédent (Lect. seule / Locked) *") },
                                enabled = false, // Read-only / Locked as specified!
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // New Index input field
                    OutlinedTextField(
                        value = formNewIndexText,
                        onValueChange = { formNewIndexText = it },
                        label = { Text("Nouveau relevé d'index (kWh) *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Pre-filled kWh Price applied (modifiable)
                    OutlinedTextField(
                        value = formKWhPriceText,
                        onValueChange = { formKWhPriceText = it },
                        label = { Text("Tarif du kWh appliqué (XOF) *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Statement Date
                    OutlinedTextField(
                        value = formStatementDate,
                        onValueChange = { formStatementDate = it },
                        label = { Text("Date du relevé (AAAA-MM-JJ) *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Real-time calculation feedback card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Aperçu de la Facturation en Direct :", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Consommation calculée : ${maxOf(0.0, calculatedNewIndex - calculatedOldIndex)} kWh", style = MaterialTheme.typography.bodyMedium)
                            Text("Montant total généré : $calculatedAmountDue XOF", style = MaterialTheme.typography.titleMedium, color = Color(0xFF006948))
                        }
                    }

                    formError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (formSelectedResidenceUnitId.isEmpty() || formNewIndexText.isEmpty() || formStatementDate.isEmpty()) {
                            formError = "Veuillez remplir tous les champs obligatoires."
                            return@Button
                        }
                        if (calculatedNewIndex < calculatedOldIndex) {
                            formError = "Le nouvel index ($calculatedNewIndex) doit être supérieur ou égal à l'index précédent ($calculatedOldIndex)."
                            return@Button
                        }
                        
                        isSubmitting = true
                        coroutineScope.launch {
                            try {
                                val req = ElectricityStatementCreateRequest(
                                    unitId = formSelectedResidenceUnitId,
                                    previousIndex = calculatedOldIndex,
                                    newIndex = calculatedNewIndex,
                                    kWhPriceApplied = calculatedPrice,
                                    statementDate = formStatementDate
                                )
                                val resp = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/logements/$formSelectedResidenceUnitId/electricity") {
                                    contentType(ContentType.Application.Json)
                                    header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                    setBody(req)
                                }
                                if (resp.status == io.ktor.http.HttpStatusCode.Created || resp.status == io.ktor.http.HttpStatusCode.OK) {
                                    fetchStatements()
                                    // Reset input form
                                    formSelectedResidenceUnitId = ""
                                    formPreviousIndex = null
                                    formNewIndexText = ""
                                    formError = null
                                    showFormDialog = false
                                } else {
                                    val err = resp.body<ErrorResponse>()
                                    formError = err.message
                                }
                            } catch (e: Exception) {
                                formError = "Erreur de connexion : ${e.message}"
                            }
                            isSubmitting = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Facturer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFormDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal Payment Confirmation Dialog
    if (showPaymentConfirmationForStatement != null) {
        val stmt = showPaymentConfirmationForStatement!!
        val currencySymbol = activeResidence?.currencySymbol ?: "XOF"
        
        AlertDialog(
            onDismissRequest = { showPaymentConfirmationForStatement = null },
            title = { Text("Confirmer le règlement", style = MaterialTheme.typography.titleLarge) },
            text = { 
                Text("Êtes-vous sûr de vouloir enregistrer le paiement de cette facture d'électricité d'un montant de ${stmt.amountDue} $currencySymbol ? Cette opération va marquer la facture comme PAYÉE et mettre à jour le grand livre de caisse de manière irréversible.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val req = ElectricityStatementUpdateRequest(status = ElectricityStatusDto.PAID)
                                val resp = ApiClient.httpClient.put("${ApiClient.BASE_URL}/api/electricity/statements/${stmt.id}/status") {
                                    contentType(ContentType.Application.Json)
                                    header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                    setBody(req)
                                }
                                if (resp.status == io.ktor.http.HttpStatusCode.OK) {
                                    fetchStatements() // Refresh list dynamically!
                                }
                            } catch (e: Exception) {}
                        }
                        showPaymentConfirmationForStatement = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948))
                ) {
                    Text("Confirmer le paiement")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentConfirmationForStatement = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
