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
fun TicketsPage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activeResidence = uiState.selectedResidenceContext
    val isAuthorized = activeResidence != null && (activeResidence.userRoleInResidence == UserRole.ADMIN || activeResidence.userRoleInResidence == UserRole.RESIDENCE_MANAGER)

    var tickets by remember { mutableStateOf<List<TicketDto>>(emptyList()) }
    var isLoadingList by remember { mutableStateOf(false) }

    // Search and filter states
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "OPEN", "IN_PROGRESS", "CLOSED"
    var urgencyFilter by remember { mutableStateOf("ALL") } // "ALL", "LOW", "MEDIUM", "CRITICAL"
    var logementFilterId by remember { mutableStateOf("") }

    // Dialog form states (Declaration)
    var showCreateDialog by remember { mutableStateOf(false) }
    var formLogementId by remember { mutableStateOf("") }
    var formCategory by remember { mutableStateOf(TicketCategory.OTHER) }
    var formUrgency by remember { mutableStateOf(TicketUrgency.MEDIUM) }
    var formTitle by remember { mutableStateOf("") }
    var formDescription by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var isSubmittingCreate by remember { mutableStateOf(false) }

    // State Machine dialog states
    var showTakeChargeDialogForTicketId by remember { mutableStateOf<String?>(null) }
    var showClosureDialogForTicketId by remember { mutableStateOf<String?>(null) }
    var formInterventionCostText by remember { mutableStateOf("0.0") }
    var formCommentText by remember { mutableStateOf("") }
    
    var isSubmittingTransition by remember { mutableStateOf(false) }
    var transitionError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Load tickets from server
    fun fetchTickets() {
        if (activeResidence == null) return
        isLoadingList = true
        coroutineScope.launch {
            try {
                val response = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/residences/${activeResidence.residenceId}/tickets") {
                    header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                }
                if (response.status == io.ktor.http.HttpStatusCode.OK) {
                    tickets = response.body()
                }
            } catch (e: Exception) {}
            isLoadingList = false
        }
    }

    LaunchedEffect(activeResidence) {
        fetchTickets()
    }

    // Client-side real-time filtering for absolute mobile snappiness
    val filteredTickets = remember(tickets, statusFilter, urgencyFilter, logementFilterId) {
        tickets.filter { t ->
            val matchStatus = if (statusFilter != "ALL") t.status.name == statusFilter else true
            val matchUrgency = if (urgencyFilter != "ALL") t.urgency.name == urgencyFilter else true
            val matchLogement = if (logementFilterId.isNotEmpty()) t.logementId == logementFilterId else true
            matchStatus && matchUrgency && matchLogement
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Page Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Tickets de Maintenance",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color(0xFF006948)
                )
                Text(
                    text = "Suivez les pannes, coordonnez les réparations sur le terrain et pilotez les coûts opérationnels.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Button(
                onClick = { 
                    formLogementId = uiState.logements.firstOrNull()?.id ?: ""
                    showCreateDialog = true 
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Text("Ouvrir un ticket", color = Color.White)
                }
            }
        }

        // Search & Filters panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Filtrer les incidents", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status filters
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("Statut", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("ALL" to "Tous", "OPEN" to "Ouverts", "IN_PROGRESS" to "En cours", "CLOSED" to "Clôturés").forEach { (key, label) ->
                                FilterChip(
                                    selected = statusFilter == key,
                                    onClick = { statusFilter = key },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    // Urgency Filter
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("Urgence", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("ALL" to "Tous", "LOW" to "Faible", "MEDIUM" to "Moyenne", "CRITICAL" to "Critique").forEach { (key, label) ->
                                FilterChip(
                                    selected = urgencyFilter == key,
                                    onClick = { urgencyFilter = key },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    // Logement Filter dropdown
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Unité rattachée", style = MaterialTheme.typography.labelMedium)
                        var expandedLogDropdown by remember { mutableStateOf(false) }
                        val selectedLogementObj = uiState.logements.firstOrNull { it.id == logementFilterId }
                        
                        Box(modifier = Modifier.padding(top = 4.dp)) {
                            OutlinedButton(
                                onClick = { expandedLogDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedLogementObj?.name ?: "Toutes les unités")
                            }
                            DropdownMenu(
                                expanded = expandedLogDropdown,
                                onDismissRequest = { expandedLogDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Toutes les unités") },
                                    onClick = {
                                        logementFilterId = ""
                                        expandedLogDropdown = false
                                    }
                                )
                                uiState.logements.forEach { logement ->
                                    DropdownMenuItem(
                                        text = { Text(logement.name) },
                                        onClick = {
                                            logementFilterId = logement.id
                                            expandedLogDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Grid list of tickets
        if (isLoadingList) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF006948))
            }
        } else if (filteredTickets.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("Aucun ticket d'incident ne correspond aux filtres.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(filteredTickets) { ticket ->
                    val matchedLogement = uiState.logements.firstOrNull { it.id == ticket.logementId }
                    val matchedLogementName = matchedLogement?.name ?: "Logement ${ticket.logementId.take(5)}"
                    
                    val (statusColor, statusBg) = when (ticket.status) {
                        TicketStatus.OPEN -> Color(0xFFBA1A1A) to Color(0xFFFDE8E8)
                        TicketStatus.IN_PROGRESS -> Color(0xFFD97706) to Color(0xFFFEF3C7)
                        TicketStatus.CLOSED -> Color(0xFF006948) to Color(0xFFE6F7F0)
                    }

                    val (urgencyColor, urgencyBg) = when (ticket.urgency) {
                        TicketUrgency.LOW -> Color(0xFF475569) to Color(0xFFF1F5F9)
                        TicketUrgency.MEDIUM -> Color(0xFFD97706) to Color(0xFFFEF3C7)
                        TicketUrgency.CRITICAL -> Color(0xFFBA1A1A) to Color(0xFFFDE8E8)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = statusBg),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = ticket.status.name, 
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = urgencyBg),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, urgencyColor.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = "Urgence: ${ticket.urgency.name}", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = urgencyColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(ticket.title, style = MaterialTheme.typography.titleLarge, color = Color(0xFF006948))
                                Text("Unité : $matchedLogementName | Catégorie : ${ticket.category.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }

                            Text(
                                ticket.description, 
                                style = MaterialTheme.typography.bodyMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (ticket.status == TicketStatus.CLOSED || ticket.interventionCost > 0.0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Coût cumulé facturé :", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    Text("${ticket.interventionCost} XOF", style = MaterialTheme.typography.titleLarge, color = Color(0xFF006948))
                                }
                            }

                            // State machine actions
                            if (ticket.status != TicketStatus.CLOSED && isAuthorized) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (ticket.status == TicketStatus.OPEN) {
                                        Button(
                                            onClick = {
                                                formInterventionCostText = "0.0"
                                                formCommentText = ""
                                                showTakeChargeDialogForTicketId = ticket.id
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Text("Prendre en charge")
                                            }
                                        }
                                    } else if (ticket.status == TicketStatus.IN_PROGRESS) {
                                        Button(
                                            onClick = {
                                                formInterventionCostText = "0.0"
                                                formCommentText = ""
                                                showClosureDialogForTicketId = ticket.id
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948))
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Text("Clôturer l'incident")
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

    // Modal creation dialog (Mobile-declaration form)
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Déclarer un Incident / Ticket", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier.widthIn(max = 500.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Unité concernée * :", style = MaterialTheme.typography.titleSmall)
                    uiState.logements.forEach { logement ->
                        val isSelected = formLogementId == logement.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { formLogementId = logement.id }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(logement.name, style = MaterialTheme.typography.titleSmall)
                        }
                    }

                    HorizontalDivider()

                    Text("Catégorie de panne * :", style = MaterialTheme.typography.titleSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TicketCategory.entries.forEach { cat ->
                            val isSelected = formCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { formCategory = cat },
                                label = { Text(cat.name) }
                            )
                        }
                    }

                    Text("Degré d'urgence * :", style = MaterialTheme.typography.titleSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TicketUrgency.entries.forEach { urg ->
                            val isSelected = formUrgency == urg
                            FilterChip(
                                selected = isSelected,
                                onClick = { formUrgency = urg },
                                label = { Text(urg.name) }
                            )
                        }
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = formTitle,
                        onValueChange = { formTitle = it },
                        label = { Text("Titre de l'incident *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = formDescription,
                        onValueChange = { formDescription = it },
                        label = { Text("Description complète de la panne *") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    formError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (formLogementId.isEmpty() || formTitle.isEmpty() || formDescription.isEmpty()) {
                            formError = "Veuillez remplir tous les champs obligatoires."
                            return@Button
                        }
                        
                        isSubmittingCreate = true
                        coroutineScope.launch {
                            try {
                                val req = TicketCreateRequest(
                                    logementId = formLogementId,
                                    category = formCategory,
                                    title = formTitle,
                                    description = formDescription,
                                    urgency = formUrgency
                                )
                                val resp = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/logements/$formLogementId/tickets") {
                                    contentType(ContentType.Application.Json)
                                    header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                    setBody(req)
                                }
                                if (resp.status == io.ktor.http.HttpStatusCode.Created || resp.status == io.ktor.http.HttpStatusCode.OK) {
                                    fetchTickets()
                                    formTitle = ""
                                    formDescription = ""
                                    formError = null
                                    showCreateDialog = false
                                } else {
                                    val err = resp.body<ErrorResponse>()
                                    formError = err.message
                                }
                            } catch (e: Exception) {
                                formError = "Erreur de connexion : ${e.message}"
                            }
                            isSubmittingCreate = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                    enabled = !isSubmittingCreate
                ) {
                    if (isSubmittingCreate) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Déclarer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal TAKE CHARGE Dialog
    if (showTakeChargeDialogForTicketId != null) {
        val targetTicketId = showTakeChargeDialogForTicketId!!
        val costVal = formInterventionCostText.toDoubleOrNull() ?: 0.0

        AlertDialog(
            onDismissRequest = { showTakeChargeDialogForTicketId = null },
            title = { Text("Prendre en charge l'incident (Mise en cours)", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier.widthIn(max = 500.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Vous pouvez renseigner un montant d'intervention initial ou estimé (optionnel), ainsi qu'un commentaire de suivi :", style = MaterialTheme.typography.bodyMedium)
                    
                    OutlinedTextField(
                        value = formInterventionCostText,
                        onValueChange = { formInterventionCostText = it },
                        label = { Text("Frais engagés immédiatement (XOF) - Optionnel") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = formCommentText,
                        onValueChange = { formCommentText = it },
                        label = { Text("Commentaire de prise en charge - Optionnel") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    if (costVal > 0.0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("⚠ IMPACT FINANCIER DÉBIT :", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text(
                                    text = "Le montant renseigné étant supérieur à 0, la validation de ce coût générera automatiquement un débit d'entretien de $costVal XOF dans la comptabilité de la résidence.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    transitionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = formInterventionCostText.toDoubleOrNull()
                        if (cost == null || cost < 0.0) {
                            transitionError = "Veuillez saisir un coût valide supérieur ou égal à 0."
                            return@Button
                        }

                        isSubmittingTransition = true
                        coroutineScope.launch {
                            try {
                                val req = TicketUpdateRequest(
                                    status = TicketStatus.IN_PROGRESS, 
                                    interventionCost = cost,
                                    comment = formCommentText.ifBlank { null }
                                )
                                val resp = ApiClient.httpClient.put("${ApiClient.BASE_URL}/api/tickets/$targetTicketId/status") {
                                    contentType(ContentType.Application.Json)
                                    header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                    setBody(req)
                                }
                                if (resp.status == io.ktor.http.HttpStatusCode.OK) {
                                    fetchTickets()
                                    transitionError = null
                                    showTakeChargeDialogForTicketId = null
                                } else {
                                    val err = resp.body<ErrorResponse>()
                                    transitionError = err.message
                                }
                            } catch (e: Exception) {
                                transitionError = "Erreur de connexion : ${e.message}"
                            }
                            isSubmittingTransition = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                    enabled = !isSubmittingTransition
                ) {
                    if (isSubmittingTransition) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Prendre en charge")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTakeChargeDialogForTicketId = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal CLOSURE Dialog
    if (showClosureDialogForTicketId != null) {
        val targetTicketId = showClosureDialogForTicketId!!
        val costVal = formInterventionCostText.toDoubleOrNull() ?: 0.0

        AlertDialog(
            onDismissRequest = { showClosureDialogForTicketId = null },
            title = { Text("Clôturer le Ticket d'Incident (Résolution)", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier.widthIn(max = 500.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Saisissez le coût de clôture éventuel et le rapport d'intervention final :", style = MaterialTheme.typography.bodyMedium)
                    
                    OutlinedTextField(
                        value = formInterventionCostText,
                        onValueChange = { formInterventionCostText = it },
                        label = { Text("Coût supplémentaire de clôture (XOF) - Optionnel") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = formCommentText,
                        onValueChange = { formCommentText = it },
                        label = { Text("Rapport / Commentaire final de clôture - Optionnel") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    if (costVal > 0.0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("⚠ IMPACT FINANCIER DÉBIT :", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text(
                                    text = "La validation de ce coût générera automatiquement un débit d'entretien supplémentaire de $costVal XOF dans la comptabilité de la résidence.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    transitionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = formInterventionCostText.toDoubleOrNull()
                        if (cost == null || cost < 0.0) {
                            transitionError = "Veuillez saisir un coût valide supérieur ou égal à 0."
                            return@Button
                        }

                        isSubmittingTransition = true
                        coroutineScope.launch {
                            try {
                                val req = TicketUpdateRequest(
                                    status = TicketStatus.CLOSED, 
                                    interventionCost = cost,
                                    comment = formCommentText.ifBlank { null }
                                )
                                val resp = ApiClient.httpClient.put("${ApiClient.BASE_URL}/api/tickets/$targetTicketId/status") {
                                    contentType(ContentType.Application.Json)
                                    header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                    setBody(req)
                                }
                                if (resp.status == io.ktor.http.HttpStatusCode.OK) {
                                    fetchTickets()
                                    transitionError = null
                                    showClosureDialogForTicketId = null
                                } else {
                                    val err = resp.body<ErrorResponse>()
                                    transitionError = err.message
                                }
                            } catch (e: Exception) {
                                transitionError = "Erreur de connexion : ${e.message}"
                            }
                            isSubmittingTransition = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                    enabled = !isSubmittingTransition
                ) {
                    if (isSubmittingTransition) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Confirmer la clôture")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showClosureDialogForTicketId = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
