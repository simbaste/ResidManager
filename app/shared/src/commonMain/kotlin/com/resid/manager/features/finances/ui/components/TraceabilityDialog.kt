package com.resid.manager.features.finances.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TraceabilityDialog(
    payload: Pair<String, String>?,
    onDismiss: () -> Unit
) {
    if (payload == null) return
    val (type, id) = payload

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Traçabilité de la Transaction", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Cette transaction a été générée automatiquement par un processus métier lié.")
                Text("• Entité d'Origine : $type")
                Text("• Identifiant Technique (UUID) : $id")

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (type.uppercase()) {
                        "BAIL" -> "Ce versement correspond à la régulation de la caution ou d'un loyer d'avance lié à un contrat de bail actif."
                        "ELECTRICITY_STATEMENT" -> "Cette écriture correspond au relevé d'indexation d'électricité saisi par un agent."
                        "TICKET" -> "Cette dépense correspond à la résolution comptable d'un incident de maintenance résolu par le gestionnaire."
                        else -> "Écriture automatisée du système."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
