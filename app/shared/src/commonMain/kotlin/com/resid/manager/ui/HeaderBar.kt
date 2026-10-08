package com.resid.manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole
import com.resid.manager.ui.components.ResidenceSplitButton
import com.resid.manager.ui.components.ThemeToggleButton
import com.resid.manager.ui.theme.ResidTheme

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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp)
    ) {
        val showLogout = maxWidth >= 880.dp
        val showUserInfo = maxWidth >= 750.dp
        val showTitle = (maxWidth >= 960.dp) && isDesktop

        Row(
            modifier = Modifier.fillMaxSize(),
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
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (showTitle) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }

                // Global Residence Dropdown Selector Styled as a Split Button
                ResidenceSplitButton(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    selectedResidence = selectedResidence,
                    residences = residences,
                    onResidenceSelected = onResidenceSelected,
                    onAddResidenceClick = onAddResidenceClick,
                    onJoinResidenceClick = onJoinResidenceClick
                )
            }

            // Right Side: Utility Icons, Divider, Profile Column, and Logout Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ThemeToggleButton(
                    isDarkMode = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                )

                // Notification Bell
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Help Question Mark Icon (using info) - hidden on mobile
                if (isDesktop) {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Aide",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

                    // Profile Column (Name + Role) - hidden when width < 750dp
                    if (showUserInfo) {
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

                // Logout CTA Button - hidden when width < 880dp
                if (showLogout) {
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
    }
}

@Preview(showBackground = true)
@Composable
fun HeaderBarPreview() {
    val sampleResidences = listOf(
        ResidenceContext(
            residenceId = "1",
            residenceName = "Les Jardins d'Ivoire",
            residenceAddress = "Cocody Danga, Abidjan",
            userRoleInResidence = UserRole.OWNER,
            totalUnits = 24,
            currencySymbol = "FCFA",
            currencyCode = "XOF"
        ),
        ResidenceContext(
            residenceId = "2",
            residenceName = "Palais de la Marina",
            residenceAddress = "Boulevard de la Marina, Cotonou",
            userRoleInResidence = UserRole.ADMIN,
            totalUnits = 12,
            currencySymbol = "FCFA",
            currencyCode = "XOF"
        )
    )

    ResidTheme(darkTheme = false) {
        HeaderBar(
            title = "Tableau de bord",
            userName = "Amadou Diallo",
            userRole = "Administrateur",
            residences = sampleResidences,
            selectedResidence = sampleResidences.first(),
            onResidenceSelected = {},
            isDesktop = true,
            onMenuClick = {},
            onLogoutClick = {},
            onProfileClick = {},
            onAddResidenceClick = {},
            onJoinResidenceClick = {},
            isDarkTheme = false,
            onToggleTheme = {}
        )
    }
}

