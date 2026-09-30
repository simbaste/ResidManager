package com.resid.manager.features.electricity.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ElectricityHeader(
    isAuthorized: Boolean,
    selectedCount: Int,
    onAddStatementClick: () -> Unit,
    onEcoPrintClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        Text("+ Relevé d'index", color = Color.White)
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
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = floorFilterText,
                    onValueChange = onFloorFilterChanged,
                    placeholder = { Text("Filtrer par étage...") },
                    modifier = Modifier.height(50.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = tenantFilterText,
                    onValueChange = onTenantFilterChanged,
                    placeholder = { Text("Filtrer par locataire...") },
                    modifier = Modifier.height(50.dp),
                    singleLine = true
                )
            }
        }
    }
}
