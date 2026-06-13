package com.resid.manager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.resid.manager.ui.i18n.LocalStrings
import com.resid.manager.viewmodel.AppScreen

@Composable
fun SidebarContent(
    selectedScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = strings.appName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
        )

        val menuItems = AppScreen.entries.toTypedArray()
        for (item in menuItems) {
            val isSelected = item == selectedScreen
            val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

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
                colors = CardDefaults.cardColors(containerColor = containerColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
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
}
