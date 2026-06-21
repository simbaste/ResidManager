package com.resid.manager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
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
fun MembersPage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activeResidence = uiState.selectedResidenceContext
    val isAuthorized = activeResidence != null && (activeResidence.userRoleInResidence == UserRole.ADMIN || activeResidence.userRoleInResidence == UserRole.RESIDENCE_MANAGER)

    var sortBy by remember { mutableStateOf("name") }
    var sortAscending by remember { mutableStateOf(true) }

    // State variables for invite popup
    var showInviteDialog by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("MANAGER") } // MANAGER, STAFF, TENANT
    var isInviteLoading by remember { mutableStateOf(false) }
    var inviteErrorMessage by remember { mutableStateOf<String?>(null) }

    // States for live user search in dialog
    var userSearchQuery by remember { mutableStateOf("") }
    var userSearchResults by remember { mutableStateOf<List<UserSearchDto>>(emptyList()) }
    var isSearchingUsers by remember { mutableStateOf(false) }
    var selectedUserEmail by remember { mutableStateOf("") }
    var selectedUserName by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    val sortedMembers = remember(uiState.members, sortBy, sortAscending) {
        val list = uiState.members
        when (sortBy) {
            "name" -> if (sortAscending) list.sortedBy { "${it.firstName} ${it.lastName}".lowercase() } else list.sortedByDescending { "${it.firstName} ${it.lastName}".lowercase() }
            "role" -> if (sortAscending) list.sortedBy { it.role.lowercase() } else list.sortedByDescending { it.role.lowercase() }
            "status" -> if (sortAscending) list.sortedBy { it.status.lowercase() } else list.sortedByDescending { it.status.lowercase() }
            else -> list
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
            Text(
                text = "Membres & Habilitations",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFF006948) // Brand deep primary green
            )

            if (isAuthorized) {
                Button(
                    onClick = {
                        userSearchQuery = ""
                        userSearchResults = emptyList()
                        selectedUserEmail = ""
                        selectedUserName = ""
                        selectedRole = "MANAGER"
                        inviteErrorMessage = null
                        showInviteDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Text("Inviter un membre", color = Color.White)
                    }
                }
            }
        }

        if (activeResidence == null) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.widthIn(max = 500.dp).padding(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFBA1A1A),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Aucune résidence active",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFBA1A1A)
                        )
                        Text(
                            text = "Pour gérer les membres et les habilitations d'accès, vous devez d'abord créer ou rejoindre une résidence d'exploitation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.setShowCreateResidenceDialog(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Créer une résidence maintenant")
                        }
                    }
                }
            }
        } else if (uiState.members.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF006948))
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Table Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF34D399)) // Vivid Flat Emerald background from design
                            .padding(vertical = 14.dp, horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Header: Name
                        Row(
                            modifier = Modifier
                                .weight(2.5f)
                                .clickable {
                                    if (sortBy == "name") {
                                        sortAscending = !sortAscending
                                    } else {
                                        sortBy = "name"
                                        sortAscending = true
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NOM COMPLET",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF0F172A), // Dark slate text
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = if (sortBy == "name") (if (sortAscending) "▲" else "▼") else "↕",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF0F172A).copy(alpha = 0.6f)
                            )
                        }

                        // Header: Email
                        Text(
                            text = "EMAIL",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(2.5f)
                        )

                        // Header: Phone
                        Text(
                            text = "TÉLÉPHONE",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(2f)
                        )

                        // Header: Role
                        Row(
                            modifier = Modifier
                                .weight(2.5f)
                                .clickable {
                                    if (sortBy == "role") {
                                        sortAscending = !sortAscending
                                    } else {
                                        sortBy = "role"
                                        sortAscending = true
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "RÔLE",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = if (sortBy == "role") (if (sortAscending) "▲" else "▼") else "↕",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF0F172A).copy(alpha = 0.6f)
                            )
                        }

                        // Header: Status
                        Row(
                            modifier = Modifier
                                .weight(2f)
                                .clickable {
                                    if (sortBy == "status") {
                                        sortAscending = !sortAscending
                                    } else {
                                        sortBy = "status"
                                        sortAscending = true
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "STATUT",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = if (sortBy == "status") (if (sortAscending) "▲" else "▼") else "↕",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF0F172A).copy(alpha = 0.6f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Table Body Rows
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        sortedMembers.forEach { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(vertical = 12.dp, horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cell: Name
                                Text(
                                    text = "${member.firstName} ${member.lastName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(2.5f)
                                )

                                // Cell: Email
                                Text(
                                    text = member.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(2.5f)
                                )

                                // Cell: Phone
                                val phoneText = member.phone
                                if (phoneText.isNullOrBlank()) {
                                    Text(
                                        text = "Non spécifié",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.weight(2f)
                                    )
                                } else {
                                    Text(
                                        text = phoneText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(2f)
                                    )
                                }

                                // Cell: Role (Styled Capsule pill)
                                Box(
                                    modifier = Modifier.weight(2.5f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val (roleBg, roleColor) = when (member.role) {
                                        "OWNER" -> Pair(Color(0xFFA7F3D0), Color(0xFF065F46)) // Light emerald, dark emerald
                                        "ADMIN" -> Pair(Color(0xFFA7F3D0), Color(0xFF065F46))
                                        "TENANT" -> Pair(Color(0xFFE0E7FF), Color(0xFF3730A3)) // Light indigo, dark indigo
                                        "MANAGER" -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B)) // Light red, dark red
                                        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = roleBg),
                                        shape = RoundedCornerShape(9999.dp),
                                        modifier = Modifier.width(130.dp).height(30.dp),
                                        border = BorderStroke(1.dp, roleColor.copy(alpha = 0.15f))
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = member.role,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = roleColor,
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            )
                                        }
                                    }
                                }

                                // Cell: Status (Styled Capsule pill)
                                Box(
                                    modifier = Modifier.weight(2f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val (statusBg, statusColor) = when (member.status) {
                                        "ACCEPTED" -> Pair(Color(0xFFD1FAE5), Color(0xFF065F46)) // Light green, dark green
                                        else -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B)) // Light red, dark red
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = statusBg),
                                        shape = RoundedCornerShape(9999.dp),
                                        modifier = Modifier.width(110.dp).height(30.dp),
                                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.15f))
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = member.status,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = statusColor,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }

    // Modal AlertDialog for Inviting Members
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Inviter un nouveau membre", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier.widthIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Recherchez le membre de la plateforme par son nom ou son adresse email pour lui envoyer une invitation :", style = MaterialTheme.typography.bodyMedium)
                    
                    // Live user search text field
                    OutlinedTextField(
                        value = userSearchQuery,
                        onValueChange = { q ->
                            userSearchQuery = q
                            if (q.length >= 2) {
                                isSearchingUsers = true
                                coroutineScope.launch {
                                    try {
                                        val resp = ApiClient.httpClient.get("${ApiClient.BASE_URL}/api/users/search") {
                                            parameter("q", q)
                                            header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                        }
                                        if (resp.status == io.ktor.http.HttpStatusCode.OK) {
                                            userSearchResults = resp.body<List<UserSearchDto>>()
                                        }
                                    } catch (e: Exception) {}
                                    isSearchingUsers = false
                                }
                            } else {
                                userSearchResults = emptyList()
                            }
                        },
                        label = { Text("Rechercher un utilisateur...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (isSearchingUsers) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                    }

                    // Display search results
                    if (userSearchResults.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.heightIn(max = 150.dp).verticalScroll(rememberScrollState())) {
                                userSearchResults.forEach { user ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedUserEmail = user.email
                                                selectedUserName = user.name
                                                userSearchResults = emptyList()
                                                userSearchQuery = user.name
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(user.name, style = MaterialTheme.typography.titleSmall)
                                            Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selected user card feedback
                    if (selectedUserEmail.isNotEmpty()) {
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
                                    Text("Destinataire sélectionné :", style = MaterialTheme.typography.labelSmall, color = Color(0xFF006948))
                                    Text("$selectedUserName ($selectedUserEmail)", style = MaterialTheme.typography.titleSmall, color = Color(0xFF006948))
                                }
                            }
                        }
                    }

                    // Dropdown Role selection
                    Text("Rôle rattaché * :", style = MaterialTheme.typography.titleSmall)
                    var expandedRoleDropdown by remember { mutableStateOf(false) }
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedRoleDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedRole)
                        }
                        DropdownMenu(
                            expanded = expandedRoleDropdown,
                            onDismissRequest = { expandedRoleDropdown = false },
                            modifier = Modifier.width(312.dp)
                        ) {
                            listOf("MANAGER", "STAFF", "TENANT").forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r) },
                                    onClick = {
                                        selectedRole = r
                                        expandedRoleDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    inviteErrorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedUserEmail.isBlank()) {
                            inviteErrorMessage = "Veuillez rechercher et sélectionner un destinataire d'invitation."
                            return@Button
                        }

                        isInviteLoading = true
                        inviteErrorMessage = null

                        coroutineScope.launch {
                            try {
                                val req = InviteMemberRequest(email = selectedUserEmail, role = selectedRole)
                                val resp = ApiClient.httpClient.post("${ApiClient.BASE_URL}/api/residences/${activeResidence?.residenceId}/members/invite") {
                                    contentType(ContentType.Application.Json)
                                    header(HttpHeaders.Authorization, "Bearer ${uiState.jwtToken}")
                                    setBody(req)
                                }
                                if (resp.status == io.ktor.http.HttpStatusCode.Created || resp.status == io.ktor.http.HttpStatusCode.OK) {
                                    viewModel.fetchMembers()
                                    showInviteDialog = false
                                } else {
                                    val err = resp.body<ErrorResponse>()
                                    inviteErrorMessage = err.message
                                }
                            } catch (e: Exception) {
                                inviteErrorMessage = "Erreur de connexion : ${e.message}"
                            }
                            isInviteLoading = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                    enabled = !isInviteLoading
                ) {
                    if (isInviteLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Envoyer l'invitation")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
