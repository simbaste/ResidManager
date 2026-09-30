package com.resid.manager.features.tickets.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceUnitDto

@Composable
fun TicketsHeader(
    onOpenTicketClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Tickets de Maintenance",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFF006948)
            )
            Text(
                text = "Suivez les pannes, coordonnez les réparations sur le terrain et pilotez les coûts opérationnels.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Button(
            onClick = onOpenTicketClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Text("Ouvrir un ticket", color = Color.White)
            }
        }
    }
}

@Composable
fun TicketsFilterPanel(
    statusFilter: String,
    urgencyFilter: String,
    unitFilterId: String,
    residenceUnits: List<ResidenceUnitDto>,
    onStatusFilterChanged: (String) -> Unit,
    onUrgencyFilterChanged: (String) -> Unit,
    onUnitFilterChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Filtrer les incidents", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status filters
                Column(modifier = Modifier.weight(1.5f)) {
                    Text("Statut", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        listOf("ALL" to "Tous", "OPEN" to "Ouverts", "IN_PROGRESS" to "En cours", "CLOSED" to "Clôturés").forEach { (key, label) ->
                            FilterChip(
                                selected = statusFilter == key,
                                onClick = { onStatusFilterChanged(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // Urgency Filter
                Column(modifier = Modifier.weight(1.5f)) {
                    Text("Urgence", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        listOf("ALL" to "Tous", "LOW" to "Faible", "MEDIUM" to "Moyenne", "CRITICAL" to "Critique").forEach { (key, label) ->
                            FilterChip(
                                selected = urgencyFilter == key,
                                onClick = { onUrgencyFilterChanged(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // Unit Filter dropdown
                Column(modifier = Modifier.weight(1f)) {
                    Text("Unité rattachée", style = MaterialTheme.typography.labelMedium)
                    var expandedLogDropdown by remember { mutableStateOf(false) }
                    val selectedUnit = residenceUnits.firstOrNull { it.id == unitFilterId }

                    Box(modifier = Modifier.padding(top = 4.dp)) {
                        OutlinedButton(
                            onClick = { expandedLogDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedUnit?.name ?: "Toutes les unités")
                        }
                        DropdownMenu(
                            expanded = expandedLogDropdown,
                            onDismissRequest = { expandedLogDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Toutes les unités") },
                                onClick = {
                                    onUnitFilterChanged("")
                                    expandedLogDropdown = false
                                }
                            )
                            residenceUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.name) },
                                    onClick = {
                                        onUnitFilterChanged(unit.id)
                                        expandedLogDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
