package com.resid.manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
fun ProfilePage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activeResidence = uiState.selectedResidenceContext
    val isManagement = activeResidence != null && (activeResidence.userRoleInResidence == UserRole.ADMIN || activeResidence.userRoleInResidence == UserRole.RESIDENCE_MANAGER)

    var activeTab by remember { mutableStateOf(0) } // 0: Profil, 1: Configuration Résidence

    // Categories management states
    var categories by remember { mutableStateOf<List<TicketCategoryDto>>(emptyList()) }
    var isLoadingCategories by remember { mutableStateOf(false) }
    
    var formCategoryKey by remember { mutableStateOf("") }
    var formCategoryLabel by remember { mutableStateOf("") }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var isSubmittingCategory by remember { mutableStateOf(false) }

    // Currency selection states
    var formCurrencyCodeSelected by remember { mutableStateOf(activeResidence?.currencyCode ?: "XOF") }
    var isSubmittingCurrency by remember { mutableStateOf(false) }
    var currencySuccess by remember { mutableStateOf(false) }
    var currencyErrorMsg by remember { mutableStateOf<String?>(null) }

    // Editing category states
    var editingCategory by remember { mutableStateOf<TicketCategoryDto?>(null) }
    var editCategoryLabel by remember { mutableStateOf("") }
    var isSubmittingEdit by remember { mutableStateOf(false) }
    var editError by remember { mutableStateOf<String?>(null) }

    // Profile editing states
    var isEditingProfile by remember { mutableStateOf(false) }
    var editFirstName by remember { mutableStateOf(uiState.firstName) }
    var editLastName by remember { mutableStateOf(uiState.lastName) }
    var editPhone by remember { mutableStateOf(uiState.phone) }
    var profileError by remember { mutableStateOf<String?>(null) }
    var isSavingProfile by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Helper to fetch categories
    fun fetchCategories() {
        if (activeResidence == null) return
        isLoadingCategories = true
        coroutineScope.launch {
            try {
                val resp = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/residences/${activeResidence.residenceId}/ticket-categories") {
                    header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                }
                if (resp.status == io.ktor.http.HttpStatusCode.OK) {
                    categories = resp.body()
                }
            } catch (e: Exception) {}
            isLoadingCategories = false
        }
    }

    // Trigger categories loading when switching to tab 1
    LaunchedEffect(activeTab, activeResidence) {
        if (activeTab == 1) {
            fetchCategories()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Page Title
        Text(
            text = "Mon Espace Personnel",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFF006948)
        )

        // Navigation Tabs (Profile vs Configuration)
        TabRow(
            selectedTabIndex = activeTab,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Mon Compte", style = MaterialTheme.typography.titleMedium) }
            )
            if (isManagement) {
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Configuration Résidence", style = MaterialTheme.typography.titleMedium) }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        if (activeTab == 0) {
            // MON COMPTE TAB
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.TopStart
            ) {
                Card(
                    modifier = Modifier.widthIn(max = 600.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Fiche d'Information Utilisateur",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF006948)
                            )
                            
                            if (!isEditingProfile) {
                                IconButton(
                                    onClick = {
                                        editFirstName = uiState.firstName
                                        editLastName = uiState.lastName
                                        editPhone = uiState.phone
                                        profileError = null
                                        isEditingProfile = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color(0xFF006948))
                                }
                            }
                        }
                        
                        HorizontalDivider()

                        if (isEditingProfile) {
                            // EDITING STATE FORM
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = editFirstName,
                                    onValueChange = { editFirstName = it },
                                    label = { Text("Prénom *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editLastName,
                                    onValueChange = { editLastName = it },
                                    label = { Text("Nom *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = editPhone,
                                    onValueChange = { editPhone = it },
                                    label = { Text("Téléphone") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                profileError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (editFirstName.isBlank() || editLastName.isBlank()) {
                                                profileError = "Le prénom et le nom sont obligatoires."
                                                return@Button
                                            }

                                            isSavingProfile = true
                                            profileError = null

                                            coroutineScope.launch {
                                                try {
                                                    val req = UserUpdateRequest(
                                                        firstName = editFirstName,
                                                        lastName = editLastName,
                                                        phone = editPhone.ifBlank { null }
                                                    )
                                                    val resp = ApiClient.httpClient.put("${ApiClient.BASE_URL}/api/profile") {
                                                        contentType(ContentType.Application.Json)
                                                        header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                                        setBody(req)
                                                    }
                                                    if (resp.status == io.ktor.http.HttpStatusCode.OK) {
                                                        val updatedUser = resp.body<UserDto>()
                                                        viewModel.updateUserProfile(updatedUser)
                                                        isEditingProfile = false
                                                    } else {
                                                        val err = resp.body<ErrorResponse>()
                                                        profileError = err.message
                                                    }
                                                } catch (e: Exception) {
                                                    profileError = "Erreur de connexion : ${e.message}"
                                                }
                                                isSavingProfile = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        enabled = !isSavingProfile,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isSavingProfile) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text("Enregistrer")
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { isEditingProfile = false },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Annuler")
                                    }
                                }
                            }
                        } else {
                            // READ-ONLY DISPLAY STATE
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = "Prénom : ${uiState.firstName}", style = MaterialTheme.typography.titleMedium)
                                Text(text = "Nom : ${uiState.lastName}", style = MaterialTheme.typography.titleMedium)
                                Text(text = "E-mail : ${uiState.loggedInUser?.email ?: "Non spécifié"}", style = MaterialTheme.typography.titleMedium)
                                Text(text = "Téléphone : ${uiState.phone.ifBlank { "Non spécifié" }}", style = MaterialTheme.typography.titleMedium)
                                Text(text = "ID Utilisateur : ${uiState.loggedInUser?.id ?: "Non spécifié"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }

                            HorizontalDivider()

                            Button(
                                onClick = { viewModel.logout() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Déconnexion")
                            }
                        }
                    }
                }
            }
        } else {
            // CONFIGURATION RESIDENCE TAB (Dynamic categories & currency settings subscreen!)
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Scrollable Left Column (Saisie Catégories & Saisie Devise)
                Column(
                    modifier = Modifier.width(320.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Card 1: Category Creation Form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Nouvelle Catégorie", style = MaterialTheme.typography.titleMedium, color = Color(0xFF006948))
                            Text("Ajoutez une catégorie de pannes de maintenance personnalisée.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            
                            HorizontalDivider()

                            OutlinedTextField(
                                value = formCategoryKey,
                                onValueChange = { formCategoryKey = it },
                                label = { Text("Clé Unique (ex: PAINTING) *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = formCategoryLabel,
                                onValueChange = { formCategoryLabel = it },
                                label = { Text("Libellé d'affichage (ex: Peinture) *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            categoryError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }

                            Button(
                                onClick = {
                                    if (formCategoryKey.isBlank() || formCategoryLabel.isBlank()) {
                                        categoryError = "Veuillez remplir tous les champs."
                                        return@Button
                                    }

                                    isSubmittingCategory = true
                                    categoryError = null

                                    coroutineScope.launch {
                                        try {
                                            val req = TicketCategoryDto(
                                                id = "",
                                                key = formCategoryKey.uppercase(),
                                                label = formCategoryLabel,
                                                residenceId = activeResidence?.residenceId
                                            )
                                            val resp = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/residences/${activeResidence?.residenceId}/ticket-categories") {
                                                contentType(ContentType.Application.Json)
                                                header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                                setBody(req)
                                            }
                                            if (resp.status == io.ktor.http.HttpStatusCode.Created) {
                                                fetchCategories()
                                                formCategoryKey = ""
                                                formCategoryLabel = ""
                                            } else {
                                                val err = resp.body<ErrorResponse>()
                                                categoryError = err.message
                                            }
                                        } catch (e: Exception) {
                                            categoryError = "Erreur de connexion : ${e.message}"
                                        }
                                        isSubmittingCategory = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                enabled = !isSubmittingCategory,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSubmittingCategory) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text("Ajouter la catégorie")
                                }
                            }
                        }
                    }

                    // Card 2: Currency Selection Form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Devise de la Résidence", style = MaterialTheme.typography.titleMedium, color = Color(0xFF006948))
                            Text("Sélectionnez la devise pour les loyers, l'électricité et les transactions financières.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            
                            HorizontalDivider()

                            // Dropdown selector
                            var expandedCurrencyDropdown by remember { mutableStateOf(false) }
                            val displayCodeName = when (formCurrencyCodeSelected) {
                                "XOF" -> "Franc CFA (XOF - FCFA)"
                                "EUR" -> "Euro (EUR - €)"
                                "USD" -> "US Dollar (USD - $)"
                                else -> formCurrencyCodeSelected
                            }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedCurrencyDropdown = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(displayCodeName)
                                }
                                DropdownMenu(
                                    expanded = expandedCurrencyDropdown,
                                    onDismissRequest = { expandedCurrencyDropdown = false },
                                    modifier = Modifier.width(260.dp)
                                ) {
                                    listOf("XOF" to "Franc CFA (XOF)", "EUR" to "Euro (EUR)", "USD" to "US Dollar (USD)").forEach { (code, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                formCurrencyCodeSelected = code
                                                expandedCurrencyDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (currencyErrorMsg != null) {
                                Text(currencyErrorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }

                            if (currencySuccess) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F7F0)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF006948))
                                        Text("Devise mise à jour !", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF006948))
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    isSubmittingCurrency = true
                                    currencyErrorMsg = null
                                    currencySuccess = false

                                    coroutineScope.launch {
                                        try {
                                            val req = mapOf("currencyCode" to formCurrencyCodeSelected)
                                            val resp = ApiClient.httpClient.put("${ApiClient.BASE_URL}/api/residences/${activeResidence?.residenceId}/currency") {
                                                contentType(ContentType.Application.Json)
                                                header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                                setBody(req)
                                            }
                                            if (resp.status == io.ktor.http.HttpStatusCode.OK) {
                                                currencySuccess = true
                                                viewModel.fetchResidences() // Globally update currency symbol instantly!
                                            } else {
                                                val err = resp.body<ErrorResponse>()
                                                currencyErrorMsg = err.message
                                            }
                                        } catch (e: Exception) {
                                            currencyErrorMsg = "Erreur de connexion : ${e.message}"
                                        }
                                        isSubmittingCurrency = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                enabled = !isSubmittingCurrency,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSubmittingCurrency) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Text("Sauvegarder la devise")
                                }
                            }
                        }
                    }
                }

                // Categories Listing Grid / Table (Right Column, expands)
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Catégories d'incidents existantes", style = MaterialTheme.typography.titleMedium, color = Color(0xFF006948))
                        HorizontalDivider()

                        if (isLoadingCategories) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF006948))
                            }
                        } else if (categories.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Aucune catégorie disponible.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { cat ->
                                    val isGlobal = cat.residenceId == null
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(cat.label, style = MaterialTheme.typography.titleMedium)
                                                if (isGlobal) {
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E7FF)),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text("Global", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E3A8A), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                            Text("Clé technique : ${cat.key}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }

                                        // Edit & Delete Actions (Only for custom ones! Global ones are protected)
                                        if (!isGlobal) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                IconButton(
                                                    onClick = { 
                                                        editCategoryLabel = cat.label
                                                        editingCategory = cat 
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                                }

                                                IconButton(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            try {
                                                                val resp = ApiClient.httpClient.delete("${ApiClient.BASE_URL}/api/ticket-categories/${cat.id}") {
                                                                    header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                                                }
                                                                if (resp.status == io.ktor.http.HttpStatusCode.OK) {
                                                                    fetchCategories()
                                                                }
                                                            } catch (e: Exception) {}
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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

    // Modal Edit Category Dialog
    if (editingCategory != null) {
        val target = editingCategory!!
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Modifier la catégorie de pannes", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Clé technique (Verrouillée) : ${target.key}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    OutlinedTextField(
                        value = editCategoryLabel,
                        onValueChange = { editCategoryLabel = it },
                        label = { Text("Libellé d'affichage *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    editError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editCategoryLabel.isBlank()) {
                            editError = "Le libellé ne peut pas être vide."
                            return@Button
                        }

                        isSubmittingEdit = true
                        editError = null

                        coroutineScope.launch {
                            try {
                                val req = TicketCategoryDto(id = target.id, key = target.key, label = editCategoryLabel, residenceId = target.residenceId)
                                val resp = ApiClient.httpClient.put("${ApiClient.BASE_URL}/api/ticket-categories/${target.id}") {
                                    contentType(ContentType.Application.Json)
                                    header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                    setBody(req)
                                }
                                if (resp.status == io.ktor.http.HttpStatusCode.OK) {
                                    fetchCategories()
                                    editingCategory = null
                                } else {
                                    val err = resp.body<ErrorResponse>()
                                    editError = err.message
                                }
                            } catch (e: Exception) {
                                editError = "Erreur de connexion : ${e.message}"
                            }
                            isSubmittingEdit = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                    enabled = !isSubmittingEdit
                ) {
                    if (isSubmittingEdit) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Enregistrer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
