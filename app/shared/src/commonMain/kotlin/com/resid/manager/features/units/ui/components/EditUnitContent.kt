package com.resid.manager.features.units.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.EquipmentDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.ui.components.AmountTextField
import com.resid.manager.ui.components.AppTextField
import com.resid.manager.ui.theme.ResidTheme

@Composable
fun EditUnitContent(
    residenceUnit: ResidenceUnitDto,
    modifier: Modifier = Modifier,
    availableEquipments: List<EquipmentDto> = emptyList(),
    currencySymbol: String = "XOF",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Double, Double, Double, List<String>) -> Unit,
) {
    var name by remember { mutableStateOf(residenceUnit.name) }
    var floor by remember { mutableStateOf(residenceUnit.floor) }
    var type by remember { mutableStateOf(residenceUnit.type) }
    var nominalRent by remember { mutableStateOf(residenceUnit.nominalRent.toString()) }
    var serviceCharges by remember { mutableStateOf(residenceUnit.serviceCharges.toString()) }
    var initialElectricityIndex by remember { mutableStateOf(residenceUnit.initialElectricityIndex.toString()) }
    var selectedEquipments by remember { mutableStateOf(residenceUnit.equipments.map { it.id }.toSet()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Modifier le logement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Fermer")
            }
        }

        AppTextField(
            value = name,
            onValueChange = {
                name = it
                validationError = null
            },
            label = "Nom / Numéro d'unité *",
            isError = (name.isBlank() && validationError != null),
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            modifier = Modifier.fillMaxWidth()
        )

        AppTextField(
            value = floor,
            onValueChange = {
                floor = it
                validationError = null
            },
            label = "Étage / Bloc *",
            isError = (floor.isBlank() && validationError != null),
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            modifier = Modifier.fillMaxWidth()
        )

        AppTextField(
            value = type,
            onValueChange = {
                type = it
                validationError = null
            },
            label = "Typologie de logement *",
            placeholder = "Studio...",
            isError = (type.isBlank() && validationError != null),
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            modifier = Modifier.fillMaxWidth()
        )

        AmountTextField(
            value = nominalRent,
            onValueChange = {
                nominalRent = it
                validationError = null
            },
            label = "Loyer Mensuel Principal *",
            currencySymbol = currencySymbol,
            isError = ((nominalRent.toDoubleOrNull() == null || (nominalRent.toDoubleOrNull() ?: 0.0) < 0.0) && validationError != null),
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            modifier = Modifier.fillMaxWidth()
        )

        AmountTextField(
            value = serviceCharges,
            onValueChange = {
                serviceCharges = it
                validationError = null
            },
            label = "Charges fixes *",
            currencySymbol = currencySymbol,
            isError = ((serviceCharges.toDoubleOrNull() == null || (serviceCharges.toDoubleOrNull() ?: 0.0) < 0.0) && validationError != null),
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            modifier = Modifier.fillMaxWidth()
        )

        AppTextField(
            value = initialElectricityIndex,
            onValueChange = {
                initialElectricityIndex = it
                validationError = null
            },
            label = "Index Compteur Initial (kWh) *",
            isError = ((initialElectricityIndex.toDoubleOrNull() == null || (initialElectricityIndex.toDoubleOrNull() ?: 0.0) < 0.0) && validationError != null),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            imeAction = ImeAction.Done,
            onImeAction = { focusManager.clearFocus() },
            modifier = Modifier.fillMaxWidth()
        )

        // Predefined Equipments list picker
        val equipmentsList = if (availableEquipments.isNotEmpty()) availableEquipments else residenceUnit.equipments
        if (equipmentsList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Équipements inclus :", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                equipmentsList.forEach { eq ->
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

        val err = validationError ?: errorMessage
        if (err != null) {
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Annuler")
            }
            Button(
                onClick = {
                    val rentVal = nominalRent.toDoubleOrNull()
                    val chargesVal = serviceCharges.toDoubleOrNull() ?: 0.0
                    val indexVal = initialElectricityIndex.toDoubleOrNull() ?: 0.0

                    if (name.isBlank() || floor.isBlank() || rentVal == null || rentVal < 0.0 || chargesVal < 0.0 || indexVal < 0.0) {
                        validationError = "Veuillez renseigner un nom, un étage, un loyer et des charges valides."
                        return@Button
                    }

                    onSubmit(name, floor, type.ifBlank { "Standard" }, rentVal, chargesVal, indexVal, selectedEquipments.toList())
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(if (isLoading) "Enregistrement..." else "Sauvegarder")
            }
        }
    }
}

@PreviewLightDark
@Composable
fun EditUnitContentPreview() {
    ResidTheme {
        EditUnitContent(
            residenceUnit = ResidenceUnitDto(
                id = "1",
                residenceId = "res1",
                name = "A1",
                floor = "1",
                type = "Studio",
                nominalRent = 60000.0,
                serviceCharges = 12000.0,
                initialElectricityIndex = 0.0,
                status = com.resid.manager.dto.UnitStatusDto.AVAILABLE
            ),
            onDismiss = {},
            onSubmit = { _, _, _, _, _, _, _ -> }
        )
    }
}
