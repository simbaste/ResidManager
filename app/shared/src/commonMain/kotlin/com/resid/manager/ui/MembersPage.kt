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
import com.resid.manager.viewmodel.LoginViewModel

@Composable
fun MembersPage(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    var sortBy by remember { mutableStateOf("name") }
    var sortAscending by remember { mutableStateOf(true) }

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
        Text(
            text = "Membres & Habilitations",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFF006948) // Brand deep primary green
        )

        if (uiState.members.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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
                                .weight(2f)
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
                                    modifier = Modifier.weight(2f),
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
}
