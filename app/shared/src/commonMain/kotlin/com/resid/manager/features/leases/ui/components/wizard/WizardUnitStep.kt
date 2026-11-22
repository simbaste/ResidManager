package com.resid.manager.features.leases.ui.components.wizard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.AppCurrency
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.UnitStatusDto
import com.resid.manager.ui.components.AppTextField
import com.resid.manager.ui.theme.ResidTheme

@Composable
fun WizardUnitStep(
    modifier: Modifier = Modifier,
    residenceUnits: List<ResidenceUnitDto>,
    draftResidenceUnitId: String,
    currencySymbol: String = AppCurrency.DEFAULT.symbol,
    onUnitSelected: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    val availableResidenceUnits = remember(residenceUnits) {
        residenceUnits.filter { it.status == UnitStatusDto.AVAILABLE }
    }

    val filteredUnits = remember(availableResidenceUnits, searchQuery, draftResidenceUnitId) {
        val query = searchQuery.trim()
        val matched = if (query.isBlank()) {
            availableResidenceUnits
        } else {
            availableResidenceUnits.filter { unit ->
                unit.name.contains(query, ignoreCase = true) ||
                    unit.floor.contains(query, ignoreCase = true) ||
                    unit.type.contains(query, ignoreCase = true)
            }
        }

        // Si une unité est déjà sélectionnée, la placer en premier pour qu'elle reste toujours visible
        matched.sortedByDescending { it.id == draftResidenceUnitId }
    }

    // Affichage limité à 2 logements à la fois
    val displayedUnits = filteredUnits.take(2)
    val remainingCount = (filteredUnits.size - displayedUnits.size).coerceAtLeast(0)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Étape 2 sur 4 : Sélection du Logement",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (availableResidenceUnits.isEmpty()) {
            Text(
                text = "Aucun logement de type AVAILABLE (libre) n'est disponible dans cette résidence.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            // Champ de recherche
            AppTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Rechercher un logement",
                placeholder = "Nom, étage ou type de logement...",
                imeAction = ImeAction.Search,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Effacer",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (filteredUnits.isEmpty()) {
                Text(
                    text = "Aucun logement ne correspond à votre recherche.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Text(
                    text = "Sélectionnez l'unité libre à attribuer :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                displayedUnits.forEach { unit ->
                    val isSelected = draftResidenceUnitId == unit.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUnitSelected(unit.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = unit.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Étage : ${unit.floor} • Type : ${unit.type} • Loyer : ${unit.nominalRent} $currencySymbol",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Sélectionné",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (remainingCount > 0) {
                    Text(
                        text = "+ $remainingCount autre(s) logement(s) disponible(s). Affinez votre recherche pour les afficher.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun WizardUnitStepPreview() {
    val sampleUnits = listOf(
        ResidenceUnitDto(
            id = "unit-1",
            residenceId = "res-1",
            name = "Appartement 101",
            floor = "1er étage",
            nominalRent = 150000.0,
            serviceCharges = 15000.0,
            initialElectricityIndex = 0.0,
            type = "T3",
            status = UnitStatusDto.AVAILABLE
        ),
        ResidenceUnitDto(
            id = "unit-2",
            residenceId = "res-1",
            name = "Studio 204",
            floor = "2ème étage",
            nominalRent = 80000.0,
            serviceCharges = 10000.0,
            initialElectricityIndex = 0.0,
            type = "Studio",
            status = UnitStatusDto.AVAILABLE
        ),
        ResidenceUnitDto(
            id = "unit-3",
            residenceId = "res-1",
            name = "Duplex 301",
            floor = "3ème étage",
            nominalRent = 250000.0,
            serviceCharges = 25000.0,
            initialElectricityIndex = 0.0,
            type = "T4",
            status = UnitStatusDto.AVAILABLE
        )
    )

    ResidTheme {
        WizardUnitStep(
            residenceUnits = sampleUnits,
            draftResidenceUnitId = "unit-1",
            currencySymbol = "FCFA",
            onUnitSelected = {}
        )
    }
}
