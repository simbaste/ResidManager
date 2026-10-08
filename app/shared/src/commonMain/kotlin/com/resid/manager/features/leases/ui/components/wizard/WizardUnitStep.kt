package com.resid.manager.features.leases.ui.components.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resid.manager.dto.ResidenceUnitDto
import com.resid.manager.dto.UnitStatusDto

@Composable
fun WizardUnitStep(
    residenceUnits: List<ResidenceUnitDto>,
    draftResidenceUnitId: String,
    onUnitSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val availableResidenceUnits = residenceUnits.filter { it.status == UnitStatusDto.AVAILABLE }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Étape 2 sur 4 : Sélection du Logement", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        if (availableResidenceUnits.isEmpty()) {
            Text("Aucun logement de type AVAILABLE (libre) n'est disponible dans cette résidence.")
        } else {
            Text("Sélectionnez l'unité libre à attribuer :")

            availableResidenceUnits.forEach { unit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUnitSelected(unit.id) }
                        .background(
                            if (draftResidenceUnitId == unit.id) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Text(text = unit.name, style = MaterialTheme.typography.titleSmall)
                        Text(text = "Étage : ${unit.floor} | Loyer : ${unit.nominalRent} XOF")
                    }
                }
            }
        }
    }
}
