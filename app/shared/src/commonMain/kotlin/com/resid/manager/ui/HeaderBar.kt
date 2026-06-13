package com.resid.manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext

@Composable
fun HeaderBar(
    title: String,
    userName: String,
    residences: List<ResidenceContext>,
    selectedResidence: ResidenceContext?,
    onResidenceSelected: (ResidenceContext) -> Unit,
    isDesktop: Boolean,
    onMenuClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAddResidenceClick: () -> Unit,
    onJoinResidenceClick: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isDesktop) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Global Residence Dropdown Selector
            Box {
                Button(
                    onClick = { showDropdown = !showDropdown },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text(selectedResidence?.residenceName ?: "No Active Residence")
                }

                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false }
                ) {
                    if (residences.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Créer une résidence...") },
                            onClick = {
                                showDropdown = false
                                onAddResidenceClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rejoindre une résidence...") },
                            onClick = {
                                showDropdown = false
                                onJoinResidenceClick()
                            }
                        )
                    } else {
                        residences.forEach { residence ->
                            DropdownMenuItem(
                                text = { Text(residence.residenceName) },
                                onClick = {
                                    onResidenceSelected(residence)
                                    showDropdown = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Créer une résidence") },
                            onClick = {
                                showDropdown = false
                                onAddResidenceClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🔍 Rejoindre une résidence") },
                            onClick = {
                                showDropdown = false
                                onJoinResidenceClick()
                            }
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Toggle Button (Sun/Moon represented with ☼ and ☾ portable for Compose JS Canvas!)
            Text(
                text = if (isDarkTheme) "☼" else "☾",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clickable { onToggleTheme() }
                    .padding(8.dp)
            )

            // Profile link showing User Name
            Text(
                text = userName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onProfileClick() }
                    .padding(4.dp)
            )

            // Simple sign out option
            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Déconnexion")
            }
        }
    }
}
