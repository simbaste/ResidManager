package com.resid.manager.features.residences.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext

@Composable
fun EditResidenceDialog(
    residence: ResidenceContext,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(residence.residenceName) }
    var address by remember { mutableStateOf(residence.residenceAddress) }
    var kWhPrice by remember { mutableStateOf("150.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier la résidence") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = kWhPrice.toDoubleOrNull() ?: 150.0
                    onSubmit(name, address, price)
                },
                enabled = name.isNotBlank() && address.isNotBlank()
            ) {
                Text("Sauvegarder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
