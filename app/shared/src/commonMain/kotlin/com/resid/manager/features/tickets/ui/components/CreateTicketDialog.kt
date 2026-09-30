package com.resid.manager.features.tickets.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.resid.manager.dto.TicketCategoryDto
import com.resid.manager.dto.TicketCreateRequest
import com.resid.manager.dto.TicketUrgencyDto

@Composable
fun CreateTicketDialog(
    residenceUnits: List<ResidenceUnitDto>,
    categories: List<TicketCategoryDto>,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, TicketCreateRequest) -> Unit
) {
    var residenceUnitSearchQuery by remember { mutableStateOf("") }
    var selectedResidenceUnitName by remember { mutableStateOf("") }
    var selectedResidenceUnitId by remember { mutableStateOf(residenceUnits.firstOrNull()?.id ?: "") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }
    var selectedUrgency by remember { mutableStateOf(TicketUrgencyDto.MEDIUM) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val filteredResidenceUnits = remember(residenceUnits, residenceUnitSearchQuery, selectedResidenceUnitName) {
        if (residenceUnitSearchQuery.isBlank() || residenceUnitSearchQuery == selectedResidenceUnitName) {
            emptyList()
        } else {
            residenceUnits.filter {
                it.name.contains(residenceUnitSearchQuery, ignoreCase = true) ||
                it.floor.contains(residenceUnitSearchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Déclarer un Incident / Ticket", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.widthIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Rechercher l'unité concernée * :", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value = residenceUnitSearchQuery,
                    onValueChange = { residenceUnitSearchQuery = it },
                    label = { Text("Saisissez le nom ou l'étage...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (filteredResidenceUnits.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.heightIn(max = 140.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredResidenceUnits.forEach { unit ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedResidenceUnitId = unit.id
                                            selectedResidenceUnitName = unit.name
                                            residenceUnitSearchQuery = unit.name
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(unit.name, style = MaterialTheme.typography.titleSmall)
                                        Text("Étage : ${unit.floor} | Type : ${unit.type}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedResidenceUnitId.isNotEmpty() && selectedResidenceUnitName.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF006948))
                            Column {
                                Text("Unité rattachée sélectionnée :", style = MaterialTheme.typography.labelSmall, color = Color(0xFF006948))
                                Text(selectedResidenceUnitName, style = MaterialTheme.typography.titleSmall, color = Color(0xFF006948))
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text("Catégorie de panne * :", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.label) }
                        )
                    }
                }

                Text("Degré d'urgence * :", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TicketUrgencyDto.entries.forEach { urg ->
                        FilterChip(
                            selected = selectedUrgency == urg,
                            onClick = { selectedUrgency = urg },
                            label = { Text(urg.name) }
                        )
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de l'incident *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description complète de la panne *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                val err = localError ?: errorMessage
                if (err != null) {
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedResidenceUnitId.isEmpty() || selectedCategoryId.isEmpty() || title.isEmpty() || description.isEmpty()) {
                        localError = "Veuillez remplir tous les champs obligatoires."
                        return@Button
                    }
                    localError = null
                    onSubmit(
                        selectedResidenceUnitId,
                        TicketCreateRequest(
                            unitId = selectedResidenceUnitId,
                            categoryId = selectedCategoryId,
                            title = title,
                            description = description,
                            urgency = selectedUrgency
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Déclarer")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
