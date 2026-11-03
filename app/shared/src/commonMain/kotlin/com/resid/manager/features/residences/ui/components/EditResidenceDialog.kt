package com.resid.manager.features.residences.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.ui.components.AmountTextField
import com.resid.manager.ui.components.AppTextField

@Composable
fun EditResidenceDialog(
    residence: ResidenceContext,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Double, ByteArray?) -> Unit,
    onOpenGallery: () -> Unit
) {
    var name by remember { mutableStateOf(residence.residenceName) }
    var address by remember { mutableStateOf(residence.residenceAddress) }
    var kWhPrice by remember { mutableStateOf(residence.kWhPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier la résidence") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nom de la résidence *",
                    modifier = Modifier.fillMaxWidth()
                )
                AppTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "Adresse *",
                    modifier = Modifier.fillMaxWidth()
                )
                AmountTextField(
                    value = kWhPrice,
                    onValueChange = { kWhPrice = it },
                    label = "Prix du kWh *",
                    currencySymbol = residence.currencySymbol,
                    placeholder = "150.0",
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Photos de la résidence :", style = MaterialTheme.typography.bodyMedium)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .clickable { onOpenGallery() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Modifier les photos",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = kWhPrice.toDoubleOrNull() ?: 150.0
                    onSubmit(name, address, price, null)
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
