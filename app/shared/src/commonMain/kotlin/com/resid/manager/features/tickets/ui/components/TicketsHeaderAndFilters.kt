package com.resid.manager.features.tickets.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.helpers.PreviewResidenceUnits
import com.resid.manager.ui.theme.ResidTheme

@Composable
fun TicketsFilterPanel(
    statusFilter: String,
    urgencyFilter: String,
    unitFilterId: String,
    isCompact: Boolean,
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

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (isCompact) {
                    StatusFilterCompact(statusFilter, onStatusFilterChanged)
                    UrgencyFilterCompact(urgencyFilter, onUrgencyFilterChanged)
                } else {
                    StatusFilterDefault(statusFilter, onStatusFilterChanged)
                    UrgencyFilterDefault(urgencyFilter, onUrgencyFilterChanged)
                }

                // Unit Filter dropdown
                Column(modifier = Modifier.wrapContentWidth()) {
                    Text("Unité rattachée", style = MaterialTheme.typography.labelMedium)
                    var expandedLogDropdown by remember { mutableStateOf(false) }
                    val selectedUnit = residenceUnits.firstOrNull { it.id == unitFilterId }

                    Box(modifier = Modifier.padding(top = 4.dp)) {
                        OutlinedButton(
                            onClick = { expandedLogDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(selectedUnit?.name ?: "Toutes les unités")
                                Spacer(Modifier.width(10.dp))
                                Icon(
                                    imageVector = if (expandedLogDropdown) {
                                        Icons.Default.ArrowDropUp
                                    } else {
                                        Icons.Default.ArrowDropDown
                                    },
                                    contentDescription = null
                                )
                            }
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

@Composable
private fun FlowRowScope.UrgencyFilterDefault(
    urgencyFilter: String,
    onUrgencyFilterChanged: (String) -> Unit
) {
    Column(modifier = Modifier.weight(1.5f)) {
        Text("Urgence", style = MaterialTheme.typography.labelMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            listOf(
                "ALL" to "Tous",
                "LOW" to "Faible",
                "MEDIUM" to "Moyenne",
                "CRITICAL" to "Critique"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = urgencyFilter == key,
                    onClick = { onUrgencyFilterChanged(key) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun UrgencyFilterCompact(
    urgencyFilter: String,
    onUrgencyFilterChanged: (String) -> Unit
) {
    Column(modifier = Modifier.wrapContentWidth()) {
        Text("Urgence", style = MaterialTheme.typography.labelMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp).horizontalScroll(rememberScrollState())
        ) {
            listOf(
                "ALL" to "Tous",
                "LOW" to "Faible",
                "MEDIUM" to "Moyenne",
                "CRITICAL" to "Critique"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = urgencyFilter == key,
                    onClick = { onUrgencyFilterChanged(key) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun FlowRowScope.StatusFilterDefault(
    statusFilter: String,
    onStatusFilterChanged: (String) -> Unit
) {
    Column(modifier = Modifier.weight(1.5f)) {
        Text("Statut", style = MaterialTheme.typography.labelMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            listOf(
                "ALL" to "Tous",
                "OPEN" to "Ouverts",
                "IN_PROGRESS" to "En cours",
                "CLOSED" to "Clôturés"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = statusFilter == key,
                    onClick = { onStatusFilterChanged(key) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun StatusFilterCompact(
    statusFilter: String,
    onStatusFilterChanged: (String) -> Unit
) {
    Column(modifier = Modifier.wrapContentWidth()) {
        Text("Statut", style = MaterialTheme.typography.labelMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp).horizontalScroll(rememberScrollState())
        ) {
            listOf(
                "ALL" to "Tous",
                "OPEN" to "Ouverts",
                "IN_PROGRESS" to "En cours",
                "CLOSED" to "Clôturés"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = statusFilter == key,
                    onClick = { onStatusFilterChanged(key) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
fun TicketsFilterPanelPreview() {
    ResidTheme {
        TicketsFilterPanel(
            statusFilter = "OPEN",
            urgencyFilter = "MEDIUM",
            unitFilterId = "1234",
            residenceUnits = listOf(
                PreviewResidenceUnits.unitB2,
                PreviewResidenceUnits.unitA1
            ),
            isCompact = true,
            onStatusFilterChanged = {},
            onUrgencyFilterChanged = {},
            onUnitFilterChanged = {}
        )
    }
}
