package com.resid.manager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.resid.manager.dto.ResidenceSummaryItemDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.ui.components.AmountTextField
import com.resid.manager.ui.components.AppTextField
import com.resid.manager.viewmodel.LoginViewModel

@Composable
fun CreateResidenceDialog(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var defaultCurrency by remember { mutableStateOf("XOF") }
    var kWhPrice by remember { mutableStateOf("150.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer une résidence") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom *") })
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Adresse *") })
                OutlinedTextField(value = kWhPrice, onValueChange = { kWhPrice = it }, label = { Text("Prix kWh Électricité *") })

                Text("Devise d'opération * :", style = MaterialTheme.typography.titleSmall)
                var expandedDropdown by remember { mutableStateOf(false) }
                val displayLabel = when (defaultCurrency) {
                    "XOF" -> "Franc CFA (XOF - FCFA)"
                    "EUR" -> "Euro (EUR - €)"
                    "USD" -> "US Dollar (USD - $)"
                    else -> defaultCurrency
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(displayLabel)
                    }
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        listOf("XOF" to "Franc CFA (XOF)", "EUR" to "Euro (EUR)", "USD" to "US Dollar (USD)").forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    defaultCurrency = code
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = kWhPrice.toDoubleOrNull() ?: 150.0
                    onSubmit(name, address, defaultCurrency, price)
                },
                enabled = name.isNotBlank() && address.isNotBlank() && !isLoading
            ) {
                Text(if (isLoading) "Création..." else "Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Annuler") }
        }
    )
}

@Composable
fun JoinResidenceDialog(
    viewModel: LoginViewModel,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedItem by remember { mutableStateOf<ResidenceSummaryItemDto?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rejoindre une résidence") },
        text = {
            Column(
                modifier = Modifier.widthIn(max = 450.dp).heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Recherchez le nom d'un bâtiment ou d'une résidence pour y souscrire un accès :")

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    label = { Text("Saisissez le nom...") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (uiState.searchResults.isNotEmpty()) {
                    val existingResidenceIds = remember(uiState.residences) {
                        uiState.residences.map { it.residenceId }.toSet()
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.searchResults.forEach { item ->
                            val isAlreadyMember = existingResidenceIds.contains(item.id)
                            Card(
                                onClick = {
                                    if (!isAlreadyMember) {
                                        selectedItem = item
                                    }
                                },
                                enabled = !isAlreadyMember,
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isAlreadyMember -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        selectedItem?.id == item.id -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isAlreadyMember) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
                                        )
                                        if (isAlreadyMember) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Déjà membre",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                    Text(text = "Adresse : ${item.address}", style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "Logements : ${item.totalUnits}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                } else if (uiState.searchQuery.isNotBlank() && uiState.searchQuery.length >= 2) {
                    Text("Aucun résultat ne correspond à votre recherche.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }

                if (selectedItem != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Text(
                            text = "Sélectionné : ${selectedItem!!.name}. Cliquez sur Rejoindre ci-dessous pour envoyer votre demande.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                val err = uiState.errorMessage
                if (err != null) {
                    Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedItem?.let { onSubmit(it.id) } },
                enabled = selectedItem != null && !uiState.isLoading
            ) {
                Text("Rejoindre")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun CreateUnitDialog(
    viewModel: LoginViewModel,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Double, Double, Double, List<String>) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var nominalRent by remember { mutableStateOf("") }
    var serviceCharges by remember { mutableStateOf("") }
    var initialElectricityIndex by remember { mutableStateOf("0.0") }
    var selectedEquipments by remember { mutableStateOf(emptySet<String>()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une unité") },
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
                    label = "Nom / Numéro d'unité *",
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
                    label = "Étage / Bloc *",
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
                    placeholder = "STUDIO",
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
                    label = "Loyer mensuel nominal *",
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
                    label = "Charges fixes d'entretien *",
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
                    label = "Index Électricité initial (kWh)",
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

                val activeError = validationError ?: uiState.errorMessage
                if (activeError != null) {
                    Text(
                        text = activeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                Text(if (uiState.isLoading) "Ajout en cours..." else "Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isLoading) { Text("Annuler") }
        }
    )
}

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

