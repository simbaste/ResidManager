package com.resid.manager.features.electricity.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.resid.manager.dto.ElectricityStatementDto

@Composable
fun StatementPaymentDialog(
    statement: ElectricityStatementDto,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmer le Règlement de la Facture") },
        text = {
            Text("Voulez-vous marquer la facture d'électricité de ${statement.amountDue} $currencySymbol comme payée ? Cette action passera son statut à PAID.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006948))
            ) {
                Text("Confirmer le Paiement", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
