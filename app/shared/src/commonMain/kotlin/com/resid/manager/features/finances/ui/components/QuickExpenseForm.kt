package com.resid.manager.features.finances.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

val validExpenseCategories = listOf(
    "Cleaning" to "Nettoyage / Entretien",
    "Fuel" to "Carburant Générateur",
    "Security" to "Sécurité",
    "Maintenance" to "Maintenance Technique",
    "Taxes" to "Impôts & Taxes",
    "Other" to "Autres Charges"
)

@Composable
fun QuickExpenseForm(
    isAuthorized: Boolean,
    formCategory: String,
    formAmountText: String,
    formDescription: String,
    formDate: String,
    isSubmittingExpense: Boolean,
    formError: String?,
    formSuccess: Boolean,
    onCategoryChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onDateChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(360.dp).fillMaxHeight(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Saisie de Dépense Opérationnelle",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF006948)
            )
            Text(
                text = "Enregistrez manuellement une charge opérationnelle payée directement.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (isAuthorized) {
                // Category selector dropdown
                Text("Catégorie de la Dépense * :", style = MaterialTheme.typography.titleSmall)
                var expandedDropdown by remember { mutableStateOf(false) }
                val selectedLabel = validExpenseCategories.firstOrNull { it.first == formCategory }?.second ?: formCategory

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedLabel)
                    }
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.width(312.dp)
                    ) {
                        validExpenseCategories.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    onCategoryChanged(key)
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                // Amount input field
                OutlinedTextField(
                    value = formAmountText,
                    onValueChange = onAmountChanged,
                    label = { Text("Montant (XOF) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Date input field
                OutlinedTextField(
                    value = formDate,
                    onValueChange = onDateChanged,
                    label = { Text("Date d'opération (AAAA-MM-JJ) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Description text input
                OutlinedTextField(
                    value = formDescription,
                    onValueChange = onDescriptionChanged,
                    label = { Text("Description explicite de l'achat *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                if (formError != null) {
                    Text(formError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (formSuccess) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F7F0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF006948))
                            Text("Dépense enregistrée avec succès !", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF006948))
                        }
                    }
                }

                Button(
                    onClick = onSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    enabled = !isSubmittingExpense
                ) {
                    if (isSubmittingExpense) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Enregistrer la dépense")
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Accès réservé aux administrateurs.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
