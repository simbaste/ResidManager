package com.resid.manager.features.residences.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.resid.manager.ui.components.AmountTextField
import com.resid.manager.ui.components.AppTextField
import com.resid.manager.ui.components.rememberImagePickerLauncher

@Composable
fun CreateResidenceDialog(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Double, ByteArray?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var defaultCurrency by remember { mutableStateOf(com.resid.manager.dto.AppCurrency.DEFAULT.code) }
    var kWhPrice by remember { mutableStateOf("150.0") }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

    val imagePicker = rememberImagePickerLauncher { bytes ->
        selectedImageBytes = bytes
    }

    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer une nouvelle résidence") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nom de la résidence *",
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    modifier = Modifier.fillMaxWidth()
                )
                AppTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "Adresse *",
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    modifier = Modifier.fillMaxWidth()
                )
                val currentCurrency = com.resid.manager.dto.AppCurrency.fromCode(defaultCurrency)
                AmountTextField(
                    value = kWhPrice,
                    onValueChange = { kWhPrice = it },
                    label = "Prix du kWh *",
                    currencySymbol = currentCurrency.symbol,
                    placeholder = "150.0",
                    imeAction = ImeAction.Done,
                    onImeAction = { focusManager.clearFocus() },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Devise principale :")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    com.resid.manager.dto.AppCurrency.entries.forEach { appCurrency ->
                        FilterChip(
                            selected = defaultCurrency == appCurrency.code,
                            onClick = { defaultCurrency = appCurrency.code },
                            label = { Text(appCurrency.code) },
                        )
                    }
                }

                Text("Photo de la résidence (optionnelle) :", style = MaterialTheme.typography.bodyMedium)
                if (selectedImageBytes != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Image sélectionnée (${selectedImageBytes!!.size / 1024} Ko)", color = MaterialTheme.colorScheme.primary)
                        IconButton(
                            onClick = { selectedImageBytes = null },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Supprimer l'image", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { imagePicker.launch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("+ Ajouter une photo", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = kWhPrice.toDoubleOrNull() ?: 150.0
                    onSubmit(name, address, defaultCurrency, price, selectedImageBytes)
                },
                enabled = name.isNotBlank() && address.isNotBlank() && !isLoading,
            ) {
                Text(if (isLoading) "Création..." else "Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Annuler") }
        },
    )
}
