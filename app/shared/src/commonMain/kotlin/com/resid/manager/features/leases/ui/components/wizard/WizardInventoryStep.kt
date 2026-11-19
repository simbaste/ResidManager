package com.resid.manager.features.leases.ui.components.wizard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WizardInventoryStep(
    modifier: Modifier = Modifier,
    onSkip: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "L'inventaire complet (équipements, mobilier, compteurs) sera ajouté dans une version ultérieure. Pour l'instant, vous pouvez passer cette étape.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("Passer l'état des lieux d'entrée")
        }
    }
}
