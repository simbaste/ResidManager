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
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Membres & Habilitations",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
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
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Header: Name
                        Row(
                            modifier = Modifier
                                .weight(2f)
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
                                text = "Nom Complet",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (sortBy == "name") (if (sortAscending) "▲" else "▼") else "↕",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        }

                        // Header: Email
                        Text(
                            text = "Email",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(2f)
                        )

                        // Header: Phone
                        Text(
                            text = "Téléphone",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1.5f)
                        )

                        // Header: Role
                        Row(
                            modifier = Modifier
                                .weight(1.5f)
                                .clickable {
                                    if (sortBy == "role") {
                                        sortAscending = !sortAscending
                                    } else {
                                        sortBy = "role"
                                        sortAscending = true
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rôle",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (sortBy == "role") (if (sortAscending) "▲" else "▼") else "↕",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        }

                        // Header: Status
                        Row(
                            modifier = Modifier
                                .weight(1.5f)
                                .clickable {
                                    if (sortBy == "status") {
                                        sortAscending = !sortAscending
                                    } else {
                                        sortBy = "status"
                                        sortAscending = true
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Statut",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (sortBy == "status") (if (sortAscending) "▲" else "▼") else "↕",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Table Body Rows
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sortedMembers.forEach { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${member.firstName} ${member.lastName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(2f)
                                )

                                Text(
                                    text = member.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(2f)
                                )

                                Text(
                                    text = member.phone ?: "Non spécifié",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1.5f)
                                )

                                Badge(
                                    containerColor = when (member.role) {
                                        "OWNER", "ADMIN" -> MaterialTheme.colorScheme.primaryContainer
                                        "RESIDENCE_MANAGER", "MANAGER" -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                    },
                                    modifier = Modifier.weight(1.5f).padding(end = 12.dp)
                                ) {
                                    Text(
                                        text = member.role,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }

                                Badge(
                                    containerColor = when (member.status) {
                                        "ACCEPTED" -> MaterialTheme.colorScheme.primaryContainer
                                        "PENDING_APPROVAL" -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.weight(1.5f).padding(end = 12.dp)
                                ) {
                                    Text(
                                        text = member.status,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}
