package com.resid.manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext

@Composable
fun HeaderBar(
    title: String,
    userName: String,
    userRole: String?,
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
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Side: Navigation Title and Active Residence Badge Capsule
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

            // Global Residence Dropdown Selector Styled as a Capsule Badge
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(Color(0xFF006948).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFF006948).copy(alpha = 0.2f), RoundedCornerShape(9999.dp))
                        .clickable { showDropdown = !showDropdown }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = Color(0xFF006948),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = selectedResidence?.residenceName ?: "No Active Residence",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF006948)
                    )
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

        // Right Side: Utility Icons, Divider, Profile Column, and Logout Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Toggle Button (using core Material Icons to prevent display bugs)
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.Star else Icons.Default.Favorite,
                    contentDescription = "Toggle Theme",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Notification Bell
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Help Question Mark Icon (using info)
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Aide",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // User Segment Profile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clickable { onProfileClick() }
                    .padding(4.dp)
            ) {
                // Circular Initials Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF006948).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = if (userName.length >= 2) userName.take(2).uppercase() else "AD"
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF006948)
                    )
                }

                // Profile Column (Name + Role)
                if (isDesktop) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Rôle: ${userRole ?: "Non spécifié"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Logout CTA Button
            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Déconnexion", color = Color.White)
            }
        }
    }
}
