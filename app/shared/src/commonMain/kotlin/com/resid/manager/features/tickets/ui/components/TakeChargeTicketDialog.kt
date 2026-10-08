package com.resid.manager.features.tickets.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TakeChargeTicketDialog(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (Double, String?) -> Unit
) {
    var formInterventionCostText by remember { mutableStateOf("0.0") }
    var formCommentText by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val costVal = formInterventionCostText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prendre en charge l'incident (Mise en cours)", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.widthIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Vous pouvez renseigner un montant d'intervention initial ou estimé (optionnel), ainsi qu'un commentaire de suivi :",
                    style = MaterialTheme.typography.bodyMedium
                )

                com.resid.manager.ui.components.AmountTextField(
                    value = formInterventionCostText,
                    onValueChange = { formInterventionCostText = it },
                    label = "Frais engagés immédiatement - Optionnel",
                    currencySymbol = "XOF",
                    placeholder = "0.0",
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                com.resid.manager.ui.components.AppTextField(
                    value = formCommentText,
                    onValueChange = { formCommentText = it },
                    label = "Commentaire de prise en charge - Optionnel",
                    singleLine = false,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    modifier = Modifier.fillMaxWidth()
                )

                if (costVal > 0.0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("⚠ IMPACT FINANCIER DÉBIT :", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(
                                text = "Le montant renseigné étant supérieur à 0, la validation de ce coût générera automatiquement un débit d'entretien de $costVal XOF dans la comptabilité de la résidence.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                val err = localError ?: errorMessage
                if (err != null) {
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = formInterventionCostText.toDoubleOrNull()
                    if (cost == null || cost < 0.0) {
                        localError = "Veuillez saisir un coût valide supérieur ou égal à 0."
                        return@Button
                    }
                    localError = null
                    onSubmit(cost, formCommentText.ifBlank { null })
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948)),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text("Prendre en charge")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
