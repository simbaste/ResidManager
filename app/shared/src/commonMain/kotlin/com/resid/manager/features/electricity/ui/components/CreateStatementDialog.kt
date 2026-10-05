package com.resid.manager.features.electricity.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceUnitDto

@Composable
fun CreateStatementDialog(
    residenceUnits: List<ResidenceUnitDto>,
    formPreviousIndex: Double?,
    isLoadingPreviousIndex: Boolean,
    errorMessage: String?,
    onSelectUnitForPreviousIndex: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String, Double, Double, Double, String) -> Unit
) {
    var residenceUnitSearchQuery by remember { mutableStateOf("") }
    var selectedResidenceUnitName by remember { mutableStateOf("") }
    var selectedResidenceUnitId by remember { mutableStateOf("") }

    var formNewIndexText by remember { mutableStateOf("") }
    var formKWhPriceText by remember { mutableStateOf("120.0") }
    var formStatementDate by remember { mutableStateOf("2025-02-17") }
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

    val calculatedOldIndex = formPreviousIndex ?: 0.0
    val calculatedNewIndex = formNewIndexText.toDoubleOrNull() ?: calculatedOldIndex
    val calculatedPrice = formKWhPriceText.toDoubleOrNull() ?: 0.0
    val calculatedAmountDue = maxOf(0.0, (calculatedNewIndex - calculatedOldIndex) * calculatedPrice)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enregistrer un Relevé d'Électricité", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.widthIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Rechercher l'unité concernée * :", style = MaterialTheme.typography.titleSmall)

                com.resid.manager.ui.components.AppTextField(
                    value = residenceUnitSearchQuery,
                    onValueChange = { residenceUnitSearchQuery = it },
                    label = "Saisissez le nom ou l'étage...",
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                    modifier = Modifier.fillMaxWidth()
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
                                            onSelectUnitForPreviousIndex(unit.id)
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

                if (isLoadingPreviousIndex) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                } else if (formPreviousIndex != null) {
                    com.resid.manager.ui.components.AppTextField(
                        value = "${formPreviousIndex} kWh",
                        onValueChange = {},
                        label = "Ancien Index Compteur (Lecture Seule)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                com.resid.manager.ui.components.AppTextField(
                    value = formNewIndexText,
                    onValueChange = { formNewIndexText = it },
                    label = "Nouveau Relevé d'Index (kWh) *",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                com.resid.manager.ui.components.AmountTextField(
                    value = formKWhPriceText,
                    onValueChange = { formKWhPriceText = it },
                    label = "Prix Unitaire Appliqué du kWh *",
                    currencySymbol = "XOF",
                    placeholder = "150.0",
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                com.resid.manager.ui.components.AppTextField(
                    value = formStatementDate,
                    onValueChange = { formStatementDate = it },
                    label = "Date du Relevé (AAAA-MM-JJ) *",
                    placeholder = "AAAA-MM-JJ",
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    modifier = Modifier.fillMaxWidth()
                )

                // Résumé du montant calculé
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Consommation : ${(calculatedNewIndex - calculatedOldIndex).coerceAtLeast(0.0)} kWh", style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider()
                        Text("MONTANT CALCULÉ : $calculatedAmountDue XOF", style = MaterialTheme.typography.titleMedium, color = Color(0xFF006948))
                    }
                }

                val err = localError ?: errorMessage
                if (err != null) {
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedResidenceUnitId.isBlank()) {
                        localError = "Veuillez sélectionner un logement."
                        return@Button
                    }
                    val newIdx = formNewIndexText.toDoubleOrNull()
                    val price = formKWhPriceText.toDoubleOrNull()
                    val oldIdx = formPreviousIndex ?: 0.0

                    if (newIdx == null || newIdx < oldIdx) {
                        localError = "Le nouvel index doit être un nombre supérieur ou égal au précédent ($oldIdx kWh)."
                        return@Button
                    }
                    if (price == null || price <= 0.0) {
                        localError = "Veuillez saisir un tarif kWh valide."
                        return@Button
                    }

                    localError = null
                    onSubmit(selectedResidenceUnitId, oldIdx, newIdx, price, formStatementDate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948))
            ) {
                Text("Générer la facture")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
