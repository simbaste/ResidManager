package com.resid.manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.resid.manager.ui.i18n.LocalStrings
import com.resid.manager.viewmodel.AppScreen

@Composable
fun SidebarContent(
    selectedScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    onAddResidenceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Column(
        modifier = modifier
            .background(Color(0xFF0E1A2B)) // Ultra deep dark slate background matching design
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // App Branding & Subtitle
            Column(modifier = Modifier.padding(bottom = 24.dp, start = 8.dp)) {
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF68DBA9) // Brand Emerald green
                )
                Text(
                    text = "Gestion Immobilière",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E9193) // soft adoucied subtitle grey
                )
            }

            // Menu Items List
            val menuItems = AppScreen.entries.toTypedArray()
            for (item in menuItems) {
                val isSelected = item == selectedScreen
                
                // Track hover interactions using standard Compose API
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                // High-end smooth states matching top SaaS platforms
                val containerColor = when {
                    isSelected -> Color(0xFF0F2D25) // selected brand emerald-dim capsule
                    isHovered -> Color(0xFF1E2D3D).copy(alpha = 0.5f) // extremely elegant and soft slate hover overlay
                    else -> Color.Transparent
                }
                val contentColor = when {
                    isSelected -> Color(0xFF68DBA9) // active vibrant brand emerald
                    isHovered -> Color(0xFFE2E8F0) // hovered brighter soft slate
                    else -> Color(0xFF94A3B8) // regular inactive slate-grey
                }

                val icon = when (item) {
                    AppScreen.DASHBOARD -> Icons.Default.Home
                    AppScreen.RESIDENCES -> Icons.Default.LocationOn
                    AppScreen.LOGEMENTS -> Icons.Default.Build
                    AppScreen.BAUX -> Icons.Default.DateRange
                    AppScreen.MEMBERS -> Icons.Default.Person
                    AppScreen.ELECTRICITY -> Icons.Default.Settings
                    AppScreen.TICKETS -> Icons.Default.Warning
                    AppScreen.FINANCES -> Icons.Default.Star
                    AppScreen.PROFILE -> Icons.Default.AccountCircle
                }

                val itemTitle = when (item) {
                    AppScreen.DASHBOARD -> strings.dashboard
                    AppScreen.RESIDENCES -> strings.residences
                    AppScreen.LOGEMENTS -> strings.logements
                    AppScreen.BAUX -> strings.leases
                    AppScreen.MEMBERS -> strings.members
                    AppScreen.ELECTRICITY -> strings.electricity
                    AppScreen.TICKETS -> strings.tickets
                    AppScreen.FINANCES -> strings.finances
                    AppScreen.PROFILE -> strings.profile
                }

                Card(
                    onClick = { onScreenSelected(item) },
                    colors = CardDefaults.cardColors(
                        containerColor = containerColor,
                        contentColor = contentColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    interactionSource = interactionSource
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = itemTitle,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = itemTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor
                        )
                    }
                }
            }
        }

        // Bottom CTA: Nouvelle Résidence Button Card
        val ctaInteractionSource = remember { MutableInteractionSource() }
        val isCtaHovered by ctaInteractionSource.collectIsHoveredAsState()
        
        Card(
            onClick = onAddResidenceClick,
            colors = CardDefaults.cardColors(
                containerColor = if (isCtaHovered) Color(0xFF005137) else Color(0xFF006948) // darker premium on hover
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            interactionSource = ctaInteractionSource
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nouvelle Résidence",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}
