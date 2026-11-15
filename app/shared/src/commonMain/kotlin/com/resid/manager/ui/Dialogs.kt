package com.resid.manager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.ui.components.AmountTextField
import com.resid.manager.ui.components.AppTextField
import com.resid.manager.viewmodel.LoginViewModel

@Composable
fun EditUnitDialog(
    viewModel: LoginViewModel,
    residenceUnit: ResidenceUnitDto,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Double, Double, Double, List<String>) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf(residenceUnit.name) }
    var floor by remember { mutableStateOf(residenceUnit.floor) }
    var type by remember { mutableStateOf(residenceUnit.type) }
    var nominalRent by remember { mutableStateOf(residenceUnit.nominalRent.toString()) }
    var serviceCharges by remember { mutableStateOf(residenceUnit.serviceCharges.toString()) }
    var initialElectricityIndex by remember { mutableStateOf(residenceUnit.initialElectricityIndex.toString()) }
    var selectedEquipments by remember { mutableStateOf(residenceUnit.equipments.map { it.id }.toSet()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier l'unité") },
        text = {
            val scrollState = rememberScrollState()
            val currencySymbol = uiState.selectedResidenceContext?.currencySymbol ?: "XOF"
            val focusManager = LocalFocusManager.current

            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .heightIn(max = 450.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val nameRequester = remember { BringIntoViewRequester() }
                val floorRequester = remember { BringIntoViewRequester() }
                val typeRequester = remember { BringIntoViewRequester() }
                val rentRequester = remember { BringIntoViewRequester() }
                val chargesRequester = remember { BringIntoViewRequester() }
                val electricityIndexRequester = remember { BringIntoViewRequester() }

                AppTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = "Nom d'unité *",
                    isError = (name.isBlank() && validationError != null),
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    bringIntoViewRequester = nameRequester,
                    modifier = Modifier.fillMaxWidth()
                )
                AppTextField(
                    value = floor,
                    onValueChange = {
                        floor = it
                        validationError = null
                    },
                    label = "Étage *",
                    isError = (floor.isBlank() && validationError != null),
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    bringIntoViewRequester = floorRequester,
                    modifier = Modifier.fillMaxWidth()
                )
                AppTextField(
                    value = type,
                    onValueChange = {
                        type = it
                        validationError = null
                    },
                    label = "Type d'unité *",
                    isError = (type.isBlank() && validationError != null),
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    bringIntoViewRequester = typeRequester,
                    modifier = Modifier.fillMaxWidth()
                )
                AmountTextField(
                    value = nominalRent,
                    onValueChange = {
                        nominalRent = it
                        validationError = null
                    },
                    label = "Loyer de base *",
                    currencySymbol = currencySymbol,
                    placeholder = "0.0",
                    isError = ((nominalRent.toDoubleOrNull() == null || (nominalRent.toDoubleOrNull() ?: 0.0) < 0.0) && validationError != null),
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    bringIntoViewRequester = rentRequester,
                    modifier = Modifier.fillMaxWidth()
                )
                AmountTextField(
                    value = serviceCharges,
                    onValueChange = {
                        serviceCharges = it
                        validationError = null
                    },
                    label = "Charges fixes *",
                    placeholder = "0.0",
                    isError = ((serviceCharges.toDoubleOrNull() == null || (serviceCharges.toDoubleOrNull() ?: 0.0) < 0.0) && validationError != null),
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    bringIntoViewRequester = chargesRequester,
                    modifier = Modifier.fillMaxWidth()
                )
                AppTextField(
                    value = initialElectricityIndex,
                    onValueChange = {
                        initialElectricityIndex = it
                        validationError = null
                    },
                    label = "Index Elec initial *",
                    isError = ((initialElectricityIndex.toDoubleOrNull() == null || (initialElectricityIndex.toDoubleOrNull() ?: 0.0) < 0.0) && validationError != null),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    imeAction = ImeAction.Done,
                    onImeAction = { focusManager.clearFocus() },
                    bringIntoViewRequester = electricityIndexRequester,
                    modifier = Modifier.fillMaxWidth()
                )

                // Predefined Equipments list picker
                if (uiState.availableEquipments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Équipements inclus :", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.availableEquipments.forEach { eq ->
                            val isChecked = selectedEquipments.contains(eq.id)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedEquipments = if (isChecked) selectedEquipments - eq.id else selectedEquipments + eq.id
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedEquipments = if (checked == true) selectedEquipments + eq.id else selectedEquipments - eq.id
                                    }
                                )
                                Text(eq.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                val err = validationError ?: uiState.errorMessage
                if (err != null) {
                    Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    validateResidenceUnitInput(
                        name = name,
                        floor = floor,
                        type = type,
                        nominalRent = nominalRent,
                        serviceCharges = serviceCharges,
                        initialElectricityIndex = initialElectricityIndex,
                        selectedEquipments = selectedEquipments.toList(),
                        onError = { validationError = it },
                        onSuccess = { validName, validFloor, validType, rent, charges, index, equipments ->
                            validationError = null
                            onSubmit(validName, validFloor, validType, rent, charges, index, equipments)
                        }
                    )
                },
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                } else {
                    Text("Sauvegarder")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isLoading) { Text("Annuler") }
        }
    )
}

private fun validateResidenceUnitInput(
    name: String,
    floor: String,
    type: String,
    nominalRent: String,
    serviceCharges: String,
    initialElectricityIndex: String,
    selectedEquipments: List<String>,
    onError: (String) -> Unit,
    onSuccess: (name: String, floor: String, type: String, rent: Double, charges: Double, index: Double, equipments: List<String>) -> Unit
) {
    val trimmedName = name.trim()
    val trimmedFloor = floor.trim()
    val trimmedType = type.trim()
    val rent = nominalRent.toDoubleOrNull()
    val charges = serviceCharges.toDoubleOrNull()
    val index = initialElectricityIndex.toDoubleOrNull()

    when {
        trimmedName.isBlank() -> {
            onError("Le nom / numéro d'unité est obligatoire.")
        }
        trimmedFloor.isBlank() -> {
            onError("L'étage ou le bloc est obligatoire.")
        }
        trimmedType.isBlank() -> {
            onError("Le type d'unité est obligatoire.")
        }
        rent == null -> {
            onError("Le loyer mensuel doit être un nombre valide.")
        }
        rent < 0.0 -> {
            onError("Le loyer mensuel ne peut pas être négatif.")
        }
        charges == null -> {
            onError("Les charges fixes doivent être un nombre valide.")
        }
        charges < 0.0 -> {
            onError("Les charges fixes ne peuvent pas être négatives.")
        }
        index == null -> {
            onError("L'index d'électricité doit être un nombre valide.")
        }
        index < 0.0 -> {
            onError("L'index d'électricité ne peut pas être négatif.")
        }
        else -> {
            onSuccess(trimmedName, trimmedFloor, trimmedType, rent, charges, index, selectedEquipments)
        }
    }
}

