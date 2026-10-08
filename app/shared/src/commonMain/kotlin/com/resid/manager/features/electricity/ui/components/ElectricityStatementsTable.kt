package com.resid.manager.features.electricity.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ElectricityStatementDto
import com.resid.manager.dto.ElectricityStatusDto
import com.resid.manager.dto.ResidenceUnitDto

@Composable
fun ElectricityStatementsGrid(
    statements: List<ElectricityStatementDto>,
    residenceUnits: List<ResidenceUnitDto>,
    currencySymbol: String,
    selectedStatementIds: Map<String, Boolean>,
    isAuthorized: Boolean,
    isLoading: Boolean,
    onToggleSelection: (String, Boolean) -> Unit,
    onMarkPaidClick: (ElectricityStatementDto) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(modifier = modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF006948))
        }
    } else if (statements.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Aucun relevé d'électricité ne correspond à vos filtres.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            items(statements) { stmt ->
                val matchedUnit = residenceUnits.firstOrNull { it.id == stmt.unitId }
                val matchedUnitName = matchedUnit?.name ?: "Logement ${stmt.unitId.take(5)}"
                val isChecked = selectedStatementIds[stmt.id] ?: false

                ElectricityStatementCard(
                    statement = stmt,
                    unitName = matchedUnitName,
                    currencySymbol = currencySymbol,
                    isChecked = isChecked,
                    isAuthorized = isAuthorized,
                    onToggleSelection = { onToggleSelection(stmt.id, it) },
                    onMarkPaidClick = { onMarkPaidClick(stmt) }
                )
            }
        }
    }
}

@Composable
fun ElectricityStatementCard(
    statement: ElectricityStatementDto,
    unitName: String,
    currencySymbol: String,
    isChecked: Boolean,
    isAuthorized: Boolean,
    onToggleSelection: (Boolean) -> Unit,
    onMarkPaidClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggleSelection(it) }
                    )
                    Text(text = unitName, style = MaterialTheme.typography.titleMedium, color = Color(0xFF006948))
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (statement.status == ElectricityStatusDto.PAID) Color(0xFFE6F7F0) else Color(0xFFFDE8E8)
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (statement.status == ElectricityStatusDto.PAID) "PAYÉ" else "IMPAYÉ",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (statement.status == ElectricityStatusDto.PAID) Color(0xFF006948) else Color(0xFFBA1A1A),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Période / Date :", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    Text(statement.statementDate, style = MaterialTheme.typography.bodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ancien Index :", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    Text("${statement.previousIndex} kWh", style = MaterialTheme.typography.bodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Nouveau Index :", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    Text("${statement.newIndex} kWh", style = MaterialTheme.typography.bodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Consommation :", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    Text("${statement.newIndex - statement.previousIndex} kWh", style = MaterialTheme.typography.bodyMedium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tarif appliqué :", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    Text("${statement.kWhPriceApplied} $currencySymbol / kWh", style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MONTANT DÛ :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("${statement.amountDue} $currencySymbol", style = MaterialTheme.typography.titleLarge, color = Color(0xFF006948))
                }

                if (statement.status == ElectricityStatusDto.UNPAID && isAuthorized) {
                    Button(
                        onClick = onMarkPaidClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Marquer comme Payé")
                        }
                    }
                }
            }
        }
    }
}
