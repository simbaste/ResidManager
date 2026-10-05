package com.resid.manager.features.members.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.UserSearchDto

@Composable
fun InviteMemberDialog(
    userSearchQuery: String,
    userSearchResults: List<UserSearchDto>,
    isSearchingUsers: Boolean,
    selectedUserEmail: String,
    selectedUserName: String,
    selectedRole: String,
    isInviteLoading: Boolean,
    inviteErrorMessage: String?,
    onUserSearchQueryChanged: (String) -> Unit,
    onUserSelected: (UserSearchDto) -> Unit,
    onRoleSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    var expandedRoleDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inviter un nouveau membre", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.widthIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Recherchez le membre de la plateforme par son nom ou son adresse email pour lui envoyer une invitation :",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Champ de recherche en direct
                com.resid.manager.ui.components.AppTextField(
                    value = userSearchQuery,
                    onValueChange = onUserSearchQueryChanged,
                    label = "Rechercher un utilisateur...",
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isSearchingUsers) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                }

                // Résultats de recherche
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
                                        .clickable { onUserSelected(user) }
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

                // Feedback utilisateur sélectionné
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

                // Dropdown de rôle
                Text("Rôle rattaché * :", style = MaterialTheme.typography.titleSmall)

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
                                    onRoleSelected(r)
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
                onClick = onSubmit,
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
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
