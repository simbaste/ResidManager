package com.resid.manager.features.residences.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CreateResidenceDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var defaultCurrency by remember { mutableStateOf("XOF") }
    var kWhPrice by remember { mutableStateOf("150.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer une nouvelle résidence") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de la résidence *") }
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Adresse *") }
                )
                OutlinedTextField(
                    value = kWhPrice,
                    onValueChange = { kWhPrice = it },
                    label = { Text("Prix du kWh (XOF) *") }
                )

                Text("Devise principale :")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("XOF", "EUR", "USD").forEach { curr ->
                        FilterChip(
                            selected = defaultCurrency == curr,
                            onClick = { defaultCurrency = curr },
                            label = { Text(curr) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = kWhPrice.toDoubleOrNull() ?: 150.0
                    onSubmit(name, address, defaultCurrency, price)
                },
                enabled = name.isNotBlank() && address.isNotBlank()
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
