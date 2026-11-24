package com.resid.manager.features.finances.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resid.manager.ui.components.AmountTextField
import com.resid.manager.ui.components.AppDatePickerField
import com.resid.manager.ui.components.AppTextField

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
    currencySymbol: String = com.resid.manager.dto.AppCurrency.DEFAULT.symbol,
    onCategoryChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onDateChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Saisie d'Opération",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Enregistrez manuellement une opération financière.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (isAuthorized) {
                // Category selector dropdown
                Text("Catégorie * :", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                var expandedDropdown by remember { mutableStateOf(false) }
                val selectedLabel = validExpenseCategories.firstOrNull { it.first == formCategory }?.second ?: formCategory

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(horizontalArrangement = Arrangement.Center) {
                            Text(selectedLabel)
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                imageVector = if (expandedDropdown) Icons.Default.ArrowDropUp  else Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.width(312.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        validExpenseCategories.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp)
                                    ) {
                                        Text(label)
                                        if (key == formCategory) {
                                            Spacer(Modifier.width(20.dp))
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onCategoryChanged(key)
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                // Amount input field
                AmountTextField(
                    value = formAmountText,
                    onValueChange = onAmountChanged,
                    label = "Montant *",
                    currencySymbol = currencySymbol,
                    placeholder = "0.0",
                    modifier = Modifier.fillMaxWidth()
                )

                // Date input field with AppDatePickerField
                AppDatePickerField(
                    date = formDate,
                    onDateSelected = onDateChanged,
                    label = "Date d'opération *",
                    placeholder = "AAAA-MM-JJ",
                    modifier = Modifier.fillMaxWidth()
                )

                // Description text input
                AppTextField(
                    value = formDescription,
                    onValueChange = onDescriptionChanged,
                    label = "Description explicite de l'achat *",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                if (formError != null) {
                    Text(formError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (formSuccess) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Opération enregistrée avec succès !", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                Button(
                    onClick = onSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmittingExpense
                ) {
                    if (isSubmittingExpense) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Enregistrer", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Accès réservé aux administrateurs.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
