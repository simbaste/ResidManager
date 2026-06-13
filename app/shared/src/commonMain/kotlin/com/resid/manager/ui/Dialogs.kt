package com.resid.manager.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.LogementDto
import com.resid.manager.dto.ResidenceContext
import com.resid.manager.dto.ResidenceSummaryItem
import com.resid.manager.viewmodel.LoginViewModel

@Composable
fun CreateResidenceDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var kWhPrice by remember { mutableStateOf("150.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer une résidence") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom *") })
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Adresse *") })
                OutlinedTextField(value = kWhPrice, onValueChange = { kWhPrice = it }, label = { Text("Prix kWh Électricité (XOF) *") })
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
    var selectedItem by remember { mutableStateOf<ResidenceSummaryItem?>(null) }

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
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("STUDIO") }
    var nominalRent by remember { mutableStateOf("0.0") }
    var serviceCharges by remember { mutableStateOf("0.0") }
    var initialElectricityIndex by remember { mutableStateOf("0.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un logement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom / Numéro d'unité *") })
                OutlinedTextField(value = floor, onValueChange = { floor = it }, label = { Text("Étage / Bloc *") })
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type d'unité *") })
                OutlinedTextField(value = nominalRent, onValueChange = { nominalRent = it }, label = { Text("Loyer mensuel nominal (XOF) *") })
                OutlinedTextField(value = serviceCharges, onValueChange = { serviceCharges = it }, label = { Text("Charges fixes d'entretien *") })
                OutlinedTextField(value = initialElectricityIndex, onValueChange = { initialElectricityIndex = it }, label = { Text("Index Électricité initial (kWh)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rent = nominalRent.toDoubleOrNull() ?: 0.0
                    val charges = serviceCharges.toDoubleOrNull() ?: 0.0
                    val index = initialElectricityIndex.toDoubleOrNull() ?: 0.0
                    onSubmit(name, floor, type, rent, charges, index)
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
    logement: LogementDto,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Double, Double, Double) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf(logement.name) }
    var floor by remember { mutableStateOf(logement.floor) }
    var type by remember { mutableStateOf(logement.type) }
    var nominalRent by remember { mutableStateOf(logement.nominalRent.toString()) }
    var serviceCharges by remember { mutableStateOf(logement.serviceCharges.toString()) }
    var initialElectricityIndex by remember { mutableStateOf(logement.initialElectricityIndex.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le logement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom d'unité *") })
                OutlinedTextField(value = floor, onValueChange = { floor = it }, label = { Text("Étage *") })
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type d'unité *") })
                OutlinedTextField(value = nominalRent, onValueChange = { nominalRent = it }, label = { Text("Loyer de base (XOF) *") })
                OutlinedTextField(value = serviceCharges, onValueChange = { serviceCharges = it }, label = { Text("Charges fixes *") })
                OutlinedTextField(value = initialElectricityIndex, onValueChange = { initialElectricityIndex = it }, label = { Text("Index Elec initial *") })
                
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
                    onSubmit(name, floor, type, rent, charges, index)
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
