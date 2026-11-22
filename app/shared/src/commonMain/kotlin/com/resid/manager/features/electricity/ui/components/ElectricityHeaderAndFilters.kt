package com.resid.manager.features.electricity.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.resid.manager.ui.components.AppTextField
import com.resid.manager.ui.theme.ResidTheme

@Composable
fun ElectricityHeader(
    isAuthorized: Boolean,
    selectedCount: Int,
    onAddStatementClick: () -> Unit,
    onEcoPrintClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Facturation de l'Électricité",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFF006948)
            )
            Text(
                text = "Générez les relevés de consommation et éditez les factures pour chaque logement.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onEcoPrintClick,
                enabled = selectedCount > 0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Text("Eco-Print ($selectedCount)")
                }
            }

            if (isAuthorized) {
                Button(
                    onClick = onAddStatementClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Text("Relevé d'index", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ElectricityFilterBar(
    statusFilter: String,
    floorFilterText: String,
    tenantFilterText: String,
    isCompact: Boolean,
    onStatusFilterChanged: (String) -> Unit,
    onFloorFilterChanged: (String) -> Unit,
    onTenantFilterChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        FlowRow(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("État :", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                FilterChip(
                    selected = statusFilter == "ALL",
                    onClick = { onStatusFilterChanged("ALL") },
                    label = { Text("Tous") }
                )
                FilterChip(
                    selected = statusFilter == "UNPAID",
                    onClick = { onStatusFilterChanged("UNPAID") },
                    label = { Text("Non Payés") }
                )
                FilterChip(
                    selected = statusFilter == "PAID",
                    onClick = { onStatusFilterChanged("PAID") },
                    label = { Text("Payés") }
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                AppTextField(
                    value = floorFilterText,
                    onValueChange = onFloorFilterChanged,
                    label = "Étage",
                    placeholder = "Filtrer par étage...",
                    imeAction = ImeAction.Search,
                    modifier = Modifier.apply {
                        if (isCompact) {
                            fillMaxWidth()
                        } else {
                            width(180.dp)
                        }
                    }
                )
                AppTextField(
                    value = tenantFilterText,
                    onValueChange = onTenantFilterChanged,
                    label = "Locataire",
                    placeholder = "Filtrer par locataire...",
                    imeAction = ImeAction.Search,
                    modifier = Modifier.apply {
                        if (isCompact) {
                            fillMaxWidth()
                        } else {
                            width(200.dp)
                        }
                    }
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
fun ElectricityHeaderPreview() {
    ResidTheme {
        ElectricityHeader(
            isAuthorized = true,
            selectedCount = 2,
            onAddStatementClick = {},
            onEcoPrintClick = {}
        )
    }
}

@PreviewLightDark
@Composable
fun ElectricityFilterBarPreview() {
    ResidTheme {
        ElectricityFilterBar(
            statusFilter = "ALL",
            floorFilterText = "",
            tenantFilterText = "",
            isCompact = true,
            onStatusFilterChanged = {},
            onFloorFilterChanged = {},
            onTenantFilterChanged = {}
        )
    }
}
