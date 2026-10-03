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
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceSummaryItemDto
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

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

@Composable
fun EditResidenceDialog(
    residence: ResidenceContext,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(residence.residenceName) }
    var address by remember { mutableStateOf(residence.residenceAddress) }
    var kWhPrice by remember { mutableStateOf("150.0") } // default for update placeholder

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier la résidence") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom de la résidence *") })
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Adresse *") })
                OutlinedTextField(value = kWhPrice, onValueChange = { kWhPrice = it }, label = { Text("Prix du kWh (XOF) *") })
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
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.searchResults.forEach { item ->
                            Card(
                                onClick = { selectedItem = item },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedItem?.id == item.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = item.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
fun CreateLogementDialog(
    viewModel: LoginViewModel,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Double, Double, Double, List<String>) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("STUDIO") }
    var nominalRent by remember { mutableStateOf("0.0") }
    var serviceCharges by remember { mutableStateOf("0.0") }
    var initialElectricityIndex by remember { mutableStateOf("0.0") }
    var selectedEquipements by remember { mutableStateOf(emptySet<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un logement") },
        text = {
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()
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

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom / Numéro d'unité *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(nameRequester)
                        .onFocusEvent { if (it.isFocused) coroutineScope.launch { nameRequester.bringIntoView() } }
                )
                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it },
                    label = { Text("Étage / Bloc *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(floorRequester)
                        .onFocusEvent { if (it.isFocused) coroutineScope.launch { floorRequester.bringIntoView() } }
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Type d'unité *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(typeRequester)
                        .onFocusEvent { if (it.isFocused) coroutineScope.launch { typeRequester.bringIntoView() } }
                )
                OutlinedTextField(
                    value = nominalRent,
                    onValueChange = { nominalRent = it },
                    label = { Text("Loyer mensuel nominal ($currencySymbol) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(rentRequester)
                        .onFocusEvent { if (it.isFocused) coroutineScope.launch { rentRequester.bringIntoView() } }
                )
                OutlinedTextField(
                    value = serviceCharges,
                    onValueChange = { serviceCharges = it },
                    label = { Text("Charges fixes d'entretien *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(chargesRequester)
                        .onFocusEvent { if (it.isFocused) coroutineScope.launch { chargesRequester.bringIntoView() } }
                )
                OutlinedTextField(
                    value = initialElectricityIndex,
                    onValueChange = { initialElectricityIndex = it },
                    label = { Text("Index Électricité initial (kWh)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(electricityIndexRequester)
                        .onFocusEvent { if (it.isFocused) coroutineScope.launch { electricityIndexRequester.bringIntoView() } }
                )
                
                // Predefined Equipements list picker
                if (uiState.availableEquipements.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Équipements inclus :", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.availableEquipements.forEach { eq ->
                            val isChecked = selectedEquipements.contains(eq.id)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedEquipements = if (isChecked) selectedEquipements - eq.id else selectedEquipements + eq.id
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedEquipements = if (checked == true) selectedEquipements + eq.id else selectedEquipements - eq.id
                                    }
                                )
                                Text(eq.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rent = nominalRent.toDoubleOrNull() ?: 0.0
                    val charges = serviceCharges.toDoubleOrNull() ?: 0.0
                    val index = initialElectricityIndex.toDoubleOrNull() ?: 0.0
                    onSubmit(name, floor, type, rent, charges, index, selectedEquipements.toList())
                },
                enabled = name.isNotBlank() && floor.isNotBlank() && type.isNotBlank()
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
fun EditLogementDialog(
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
    var selectedEquipements by remember { mutableStateOf(residenceUnit.equipments.map { it.id }.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le logement") },
        text = {
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()
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

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom d'unité *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(nameRequester)
                        .onFocusEvent { if (it.isFocused) coroutineScope.launch { nameRequester.bringIntoView() } }
                )
                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it },
                    label = { Text("Étage *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(floorRequester)
                        .onFocusEvent { if (it.isFocused) coroutineScope.launch { floorRequester.bringIntoView() } }
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Type d'unité *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(typeRequester)
                        .onFocusEvent { if (it.isFocused) coroutineScope.launch { typeRequester.bringIntoView() } }
                )
                OutlinedTextField(
                    value = nominalRent,
                    onValueChange = { nominalRent = it },
                    label = { Text("Loyer de base ($currencySymbol) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(rentRequester)
                        .onFocusEvent { if (it.isFocused) coroutineScope.launch { rentRequester.bringIntoView() } }
                )
                OutlinedTextField(
                    value = serviceCharges,
                    onValueChange = { serviceCharges = it },
                    label = { Text("Charges fixes *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(chargesRequester)
                        .onFocusEvent { if (it.isFocused) coroutineScope.launch { chargesRequester.bringIntoView() } }
                )
                OutlinedTextField(
                    value = initialElectricityIndex,
                    onValueChange = { initialElectricityIndex = it },
                    label = { Text("Index Elec initial *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(electricityIndexRequester)
                        .onFocusEvent { if (it.isFocused) coroutineScope.launch { electricityIndexRequester.bringIntoView() } }
                )
                
                // Predefined Equipements list picker
                if (uiState.availableEquipements.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Équipements inclus :", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.availableEquipements.forEach { eq ->
                            val isChecked = selectedEquipements.contains(eq.id)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedEquipements = if (isChecked) selectedEquipements - eq.id else selectedEquipements + eq.id
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedEquipements = if (checked == true) selectedEquipements + eq.id else selectedEquipements - eq.id
                                    }
                                )
                                Text(eq.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
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
                onClick = {
                    val rent = nominalRent.toDoubleOrNull() ?: 0.0
                    val charges = serviceCharges.toDoubleOrNull() ?: 0.0
                    val index = initialElectricityIndex.toDoubleOrNull() ?: 0.0
                    onSubmit(name, floor, type, rent, charges, index, selectedEquipements.toList())
                },
                enabled = name.isNotBlank() && floor.isNotBlank() && type.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                } else {
                    Text("Sauvegarder")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
