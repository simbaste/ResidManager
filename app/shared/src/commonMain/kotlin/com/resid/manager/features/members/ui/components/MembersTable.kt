package com.resid.manager.features.members.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceMemberSummaryDto
import com.resid.manager.ui.components.SortHeaderIcon

@Composable
fun MembersTable(
    members: List<ResidenceMemberSummaryDto>,
    sortBy: String,
    sortAscending: Boolean,
    onSortChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header: Name
                Row(
                    modifier = Modifier
                        .weight(2.5f)
                        .clickable { onSortChanged("name") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NOM COMPLET",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    SortHeaderIcon(
                        isCurrentSort = sortBy == "name",
                        sortAscending = sortAscending
                    )
                }

                // Header: Email
                Text(
                    text = "EMAIL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(2.5f)
                )

                // Header: Phone
                Text(
                    text = "TÉLÉPHONE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(2f)
                )

                // Header: Role
                Row(
                    modifier = Modifier
                        .weight(2.5f)
                        .clickable { onSortChanged("role") },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "RÔLE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    SortHeaderIcon(
                        isCurrentSort = sortBy == "role",
                        sortAscending = sortAscending
                    )
                }

                // Header: Status
                Row(
                    modifier = Modifier
                        .weight(2f)
                        .clickable { onSortChanged("status") },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "STATUT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    SortHeaderIcon(
                        isCurrentSort = sortBy == "status",
                        sortAscending = sortAscending
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
                members.forEach { member ->
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
                                fontStyle = FontStyle.Italic,
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
                            RoleBadge(role = member.roleDto)
                        }

                        // Cell: Status (Styled Capsule pill)
                        Box(
                            modifier = Modifier.weight(2f),
                            contentAlignment = Alignment.Center
                        ) {
                            StatusBadge(status = member.status)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}
