package com.resid.manager.features.residences.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.UserRole

@Composable
fun AssociatedPropertyCard(
    residence: ResidenceContext,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val role = residence.userRoleInResidence
    val badgeColor = when (role) {
        UserRole.ADMIN, UserRole.OWNER -> MaterialTheme.colorScheme.primaryContainer
        UserRole.MANAGER, UserRole.STAFF -> MaterialTheme.colorScheme.tertiaryContainer
        UserRole.TENANT -> MaterialTheme.colorScheme.secondaryContainer
    }
    val badgeContentColor = when (role) {
        UserRole.ADMIN, UserRole.OWNER -> MaterialTheme.colorScheme.onPrimaryContainer
        UserRole.MANAGER, UserRole.STAFF -> MaterialTheme.colorScheme.onTertiaryContainer
        UserRole.TENANT -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        onClick = onClick,
        modifier = modifier.height(180.dp),
        border = if (isSelected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = residence.residenceName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sélectionnée",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Text(
                    text = residence.residenceAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${residence.totalUnits} logement" + (if (residence.totalUnits > 1) "s" else ""),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Badge(containerColor = badgeColor) {
                    Text(
                        text = role.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = badgeContentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
